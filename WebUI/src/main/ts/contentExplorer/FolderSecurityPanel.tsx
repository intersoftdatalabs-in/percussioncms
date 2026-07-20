/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Folder security panel (US4 / T060). The server-driven surface that
 * lets an Admin user view + edit the {@code PSFolderPermission}
 * ACL on a folder; non-Admin users see the same panel read-only.
 *
 * <p>Components:</p>
 * <ul>
 *   <li>Loading state: shows the i18n {@code SECURITY_LOADING} key.</li>
 *   <li>Error state: surfaces the i18n {@code SECURITY_LOAD_ERROR} key with a retry button.</li>
 *   <li>Permission panel: renders the four principal lists
 *       ({@code adminPrincipals}, {@code writePrincipals},
 *       {@code readPrincipals}, {@code viewPrincipals}) with
 *       add / remove controls per row. Controls are disabled when
 *       {@link canEditSecurityPanel} returns false (FR-016).</li>
 *   <li>Self-lockout warning (FR-015): before invoking
 *       {@link FolderSecurityPanelProps.onSave}, the panel asks the
 *       host to confirm; the host may either pass
 *       {@code confirmLockout: true} (e.g. after a native confirm
 *       dialog) or abort the save.</li>
 *   <li>Read-only banner: shown when the user lacks ADMIN rights.</li>
 * </ul>
 *
 * <p>Server endpoints used (per the existing sitemanage path API):</p>
 * <ul>
 *   <li>{@code GET /Rhythmyx/rest/pathmanagement/path/folderproperties/{id}}</li>
 *   <li>{@code POST /Rhythmyx/rest/pathmanagement/path/saveFolderProperties}</li>
 * </ul>
 *
 * <p>This component is presentation-only: the host is responsible for
 * fetching the {@link PSFolderProperties}, calling save, surfacing
 * errors, and dispatching the lockout confirm dialog. The shared
 * helpers under {@link aclLockout} are pure functions and are
 * individually testable.</p>
 */

import React, { useMemo, useState } from "react";
import {
  folderProperties,
  saveFolderProperties,
} from "../api/contentExplorer/pathApi";
import type {
  PSFolderPermission,
  PSFolderProperties,
  PSPrincipal,
} from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import {
  canEditSecurityPanel,
  canViewSecurityPanel,
  detectSelfLockout,
  type PrincipalListKey,
} from "./aclLockout";

export interface FolderSecurityPanelProps {
  /** The id of the folder whose permission to view / edit. */
  folderId: string;
  /** Identities the current user holds (USER name + ROLE names). */
  currentUserIdentities: ReadonlyArray<string>;
  /**
   * Optional initial properties (e.g. from parent prefetch). When
   * omitted the panel fetches via {@link folderProperties} on mount.
   */
  initial?: PSFolderProperties;
  /**
   * Override the load + save transport. Tests pass a stub; the
   * default uses the sitemanage path API.
   */
  load?: (folderId: string) => Promise<PSFolderProperties>;
  save?: (props: PSFolderProperties) => Promise<void>;
  /**
   * Called after the panel detects a self-lockout risk; the host
   * must return a promise that resolves to {@code true} when the
   * user confirmed (continue) or {@code false} when the user
   * cancelled. The default uses {@code window.confirm}.
   */
  confirmLockout?: (
    level: PrincipalListKey,
    identities: ReadonlyArray<string>,
  ) => Promise<boolean>;
  /**
   * Called when a successful save completes. The host typically
   * refreshes the tree / list. Errors raised from {@link save} are
   * caught and surfaced in the panel state.
   */
  onSaved?: (props: PSFolderProperties) => void;
}

export type Status =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; props: PSFolderProperties; dirty: boolean };

interface PrincipalListDraft {
  level: PrincipalListKey;
  principals: PSPrincipal[];
  label: string;
}

const PRINCIPAL_LABELS: Record<PrincipalListKey, string> = {
  adminPrincipals: "Admin",
  writePrincipals: "Write",
  readPrincipals: "Read",
  viewPrincipals: "View",
};

export function FolderSecurityPanel(
  props: FolderSecurityPanelProps,
): React.JSX.Element {
  const {
    folderId,
    currentUserIdentities,
    initial,
    load = folderProperties,
    save = saveFolderProperties,
    confirmLockout,
    onSaved,
  } = props;

  const [status, setStatus] = useState<Status>(
    initial
      ? { kind: "ready", props: initial, dirty: false }
      : { kind: "loading" },
  );
  const [pendingSave, setPendingSave] = useState(false);
  /**
   * Snapshot of the permission object that was *originally loaded* from
   * the server. The lockout warning compares this against the
   * post-edit permission; without it, the `detectSelfLockout` check
   * always sees {@code before === after} (the current edited state)
   * and never fires. Reset on each successful load + whenever the
   * supplied {@code initial} prop changes.
   *
   * Mitigation for kilo-code-bot PR #1397 thread 3614415903.
   */
  const originalPermissionRef = React.useRef<PSFolderPermission | undefined>(
    initial?.permission,
  );

  React.useEffect(() => {
    if (initial) {
      originalPermissionRef.current = initial.permission;
      return;
    }
    let cancelled = false;
    setStatus({ kind: "loading" });
    load(folderId)
      .then((props) => {
        if (cancelled) return;
        originalPermissionRef.current = props.permission;
        setStatus({ kind: "ready", props, dirty: false });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const msg =
          err instanceof Error ? err.message : message(EXPLORER_MSG.ERROR_GENERIC);
        setStatus({ kind: "error", message: msg });
      });
    return () => {
      cancelled = true;
    };
  }, [folderId, initial, load]);

  function patchProps(updater: (p: PSFolderProperties) => PSFolderProperties): void {
    if (status.kind !== "ready") return;
    setStatus({ ...status, props: updater(status.props), dirty: true });
  }

  async function attemptSave(): Promise<void> {
    if (status.kind !== "ready" || pendingSave) return;
    const current = status.props;
    if (!current.permission) return;

    // Lockout check: compare the original loaded permission against the
    // current edited permission so removing the current user from any
    // level is detected. Without `originalPermissionRef`, `before`
    // and `after` collapse to the same object and the check never
    // fires. Mitigation for kilo-code-bot PR #1397 thread 3614415903.
    const lockoutLevels = detectSelfLockout(
      originalPermissionRef.current ?? current.permission,
      current.permission,
      currentUserIdentities,
    );

    if (lockoutLevels.length > 0) {
      try {
        const proceed = confirmLockout
          ? await confirmLockout(lockoutLevels[0]!.level, currentUserIdentities)
          : window.confirm(
              message(EXPLORER_MSG.SECURITY_LOCKOUT_WARNING_BODY),
            );
        if (!proceed) return;
      } catch (err: unknown) {
        // Host-supplied confirmLockout threw; treat rejection-from-confirm
        // as "user cancelled" so the save does NOT proceed with no
        // confirmation. Mitigation for kilo-code-bot PR #1397
        // thread 3614415910.
        // eslint-disable-next-line no-console -- surface to ops
        console.warn(
          `[FolderSecurityPanel] confirmLockout rejected; aborting save: ${err instanceof Error ? err.message : String(err)}`,
        );
        return;
      }
    }

    setPendingSave(true);
    try {
      await save(current);
      // Snapshot is now the new post-save state.
      originalPermissionRef.current = current.permission;
      setStatus({ ...status, dirty: false });
      onSaved?.(current);
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : message(EXPLORER_MSG.SECURITY_SAVE_ERROR);
      setStatus({ kind: "error", message: msg });
    } finally {
      setPendingSave(false);
    }
  }

  // Hooks MUST be called in the same order on every render. The
  // loading / error / no-access branches are early returns below;
  // useMemo must run before them or React throws "Rendered more
  // hooks than during the previous render" on the second render
  // (when status transitions loading → ready).
  const drafts: PrincipalListDraft[] = useMemo(() => {
    const permission =
      status.kind === "ready" ? status.props.permission : undefined;
    if (!permission) return [];
    return (Object.keys(PRINCIPAL_LABELS) as PrincipalListKey[]).map((k) => ({
      level: k,
      label: PRINCIPAL_LABELS[k],
      principals: permission[k] ?? [],
    }));
  }, [status]);

  if (status.kind === "loading") {
    return (
      <div role="status" data-testid="folder-security-loading">
        {message(EXPLORER_MSG.SECURITY_LOADING)}
      </div>
    );
  }

  if (status.kind === "error") {
    return (
      <div role="alert" data-testid="folder-security-error">
        <p>{status.message}</p>
        <button
          type="button"
          onClick={() => setStatus({ kind: "loading" })}
          data-testid="folder-security-retry"
        >
          {message(EXPLORER_MSG.RETRY)}
        </button>
      </div>
    );
  }

  const { props: current, dirty } = status;
  const permission = current.permission;
  if (!canViewSecurityPanel(permission)) {
    return (
      <div role="status" data-testid="folder-security-no-access">
        {message(EXPLORER_MSG.PERMISSION_DENIED)}
      </div>
    );
  }

  const editable = canEditSecurityPanel(permission);

  return (
    <section
      role="region"
      aria-label={message(EXPLORER_MSG.SECURITY_TITLE)}
      data-testid="folder-security-panel"
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.SECURITY_TITLE)} — {current.name || current.id}
      </h2>
      {!editable ? (
        <p
          role="status"
          data-testid="folder-security-readonly"
          style={{ color: "#a00" }}
        >
          {message(EXPLORER_MSG.SECURITY_READ_ONLY)}
        </p>
      ) : null}
      {drafts.map((d) => (
        <PrincipalListEditor
          key={d.level}
          draft={d}
          editable={editable}
          onChange={(next) =>
            patchProps((p) => ({
              ...p,
              permission: { ...p.permission!, [d.level]: next },
            }))
          }
        />
      ))}
      <div style={{ marginTop: 12 }}>
        <button
          type="button"
          disabled={!editable || !dirty || pendingSave}
          onClick={() => void attemptSave()}
          data-testid="folder-security-save"
        >
          {pendingSave
            ? message(EXPLORER_MSG.SECURITY_LOADING)
            : message(EXPLORER_MSG.CONFIRM_OK)}
        </button>
        <span
          role="status"
          data-testid="folder-security-dirty"
          style={{ marginLeft: 8, color: dirty ? "#a00" : "#888" }}
        >
          {dirty ? "●" : "○"}
        </span>
      </div>
    </section>
  );
}

function PrincipalListEditor(props: {
  draft: PrincipalListDraft;
  editable: boolean;
  onChange: (next: PSPrincipal[]) => void;
}): React.JSX.Element {
  const { draft, editable, onChange } = props;
  const [adding, setAdding] = useState(false);
  const [draftName, setDraftName] = useState("");

  return (
    <fieldset
      data-testid={`folder-security-list-${draft.level}`}
      style={{ border: "1px solid #ccc", padding: 8, marginBottom: 8 }}
    >
      <legend>{draft.label}</legend>
      <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
        {draft.principals.length === 0 ? (
          <li
            role="presentation"
            data-testid={`folder-security-list-${draft.level}-empty`}
            style={{ color: "#888" }}
          >
            (none)
          </li>
        ) : (
          draft.principals.map((p, idx) => (
            <li
              key={`${p.type}:${p.name}`}
              style={{ display: "flex", gap: 8, alignItems: "center" }}
            >
              <span>
                {p.name} <small>({p.type})</small>
              </span>
              <button
                type="button"
                disabled={!editable}
                onClick={() =>
                  onChange(
                    draft.principals.filter((_, i) => i !== idx),
                  )
                }
                data-testid={`folder-security-list-${draft.level}-remove-${p.name}`}
              >
                {message(EXPLORER_MSG.SECURITY_PRINCIPAL_REMOVE)}
              </button>
            </li>
          ))
        )}
      </ul>
      {editable ? (
        adding ? (
          <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
            <label
              htmlFor={`folder-security-add-${draft.level}`}
              style={{ display: "none" }}
            >
              {message(EXPLORER_MSG.SECURITY_PRINCIPAL_NAME_LABEL)}
            </label>
            <input
              id={`folder-security-add-${draft.level}`}
              type="text"
              value={draftName}
              onChange={(e) => setDraftName(e.target.value)}
              data-testid={`folder-security-list-${draft.level}-input`}
            />
            <button
              type="button"
              onClick={() => {
                const trimmed = draftName.trim();
                if (!trimmed) return;
                const principal: PSPrincipal = {
                  type: "USER",
                  name: trimmed,
                };
                if (
                  draft.principals.some(
                    (p) => p.name === principal.name && p.type === principal.type,
                  )
                ) {
                  setAdding(false);
                  setDraftName("");
                  return;
                }
                onChange([...draft.principals, principal]);
                setDraftName("");
                setAdding(false);
              }}
              data-testid={`folder-security-list-${draft.level}-add-confirm`}
            >
              {message(EXPLORER_MSG.SECURITY_PRINCIPAL_ADD)}
            </button>
            <button
              type="button"
              onClick={() => {
                setDraftName("");
                setAdding(false);
              }}
              data-testid={`folder-security-list-${draft.level}-add-cancel`}
            >
              {message(EXPLORER_MSG.CONFIRM_CANCEL)}
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => setAdding(true)}
            data-testid={`folder-security-list-${draft.level}-add`}
            style={{ marginTop: 8 }}
          >
            {message(EXPLORER_MSG.SECURITY_PRINCIPAL_ADD)}
          </button>
        )
      ) : null}
    </fieldset>
  );
}

