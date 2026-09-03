/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import React, { useEffect, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { extractRestErrorMessage, isApiError } from "../api/client";
import {
  ACTION_MENU_HANDLERS,
  ACTION_MENU_PROP,
  ACTION_MENU_REFRESH_HINTS,
  ACTION_MENU_TYPE_ITEM,
  ACTION_MENU_TYPES,
  ACTION_MENU_VISIBILITY_ALIASES,
  actionMenuRowsEqual,
  createActionMenu,
  deleteActionMenu,
  extraActionMenuProperties,
  getActionMenuDetail,
  isActionMenuWriteReady,
  mergeActionMenuProperties,
  listActionMenus,
  normalizeActionMenuName,
  normalizeActionMenuParameters,
  normalizeUiContexts,
  normalizeVisibilityContexts,
  propertyValue,
  saveActionMenu,
  saveActionMenuChildren,
  withoutStaleActionMenuWriteGap,
  type ActionMenuWriteBody,
} from "../api/developer/actionMenusApi";
import { resolveActionMenuObjectGuid } from "../api/displayFormatGuid";
import type {
  ActionMenu,
  ActionMenuModeUIContext,
  ActionMenuParameter,
  ActionMenuProperty,
  ActionMenuVisibilityContext,
} from "../api/developer/types";
import {
  addActionMenuChild,
  catalogsNotInChildren,
  childrenOrderEqual,
  isActionMenuChildrenWritable,
  isKnownSystemActionMenuName,
  isRestUserActionMenu,
  moveActionMenuChild,
  removeActionMenuChild,
  toChildWriteBody,
  type ActionMenuChildRef,
} from "./actionMenuChildren";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  marginBottom: "12px",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

const tabListStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "4px",
  borderBottom: `2px solid ${catalogColors.headerBorder}`,
  margin: "8px 0 16px",
};

function tabStyle(active: boolean): React.CSSProperties {
  return {
    background: "transparent",
    border: "none",
    borderBottom: active ? `2px solid ${catalogColors.accent}` : "2px solid transparent",
    marginBottom: "-2px",
    padding: "8px 12px",
    cursor: "pointer",
    color: active ? catalogColors.accent : catalogColors.text,
    font: "inherit",
  };
}

type AmTab = "usage" | "command" | "visibility";
const actionButton: React.CSSProperties = {
  padding: "4px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  background: catalogColors.surface,
  cursor: "pointer",
  font: "inherit",
};

function childrenFromDetail(detail: ActionMenu | null): ActionMenuChildRef[] {
  if (detail == null || !Array.isArray(detail.children)) {
    return [];
  }
  return detail.children.map((c) => {
    const row: ActionMenuChildRef = { name: (c.name || "").trim() };
    if (c.id != null && c.id > 0) {
      row.id = c.id;
    }
    if (c.guidString) {
      row.guidString = c.guidString;
    }
    return row;
  });
}
function typeFromDetail(detail: ActionMenu | null, fallback: string): string {
  if (detail?.menuType && detail.menuType.trim()) return detail.menuType.trim();
  return fallback;
}

function handlerFromDetail(detail: ActionMenu | null): string {
  const raw = (detail?.handler || "").trim().toUpperCase();
  return raw === "SERVER" ? "SERVER" : "CLIENT";
}

function applyLoaded(detail: ActionMenu, idOrName: string | null): {
  name: string;
  label: string;
  description: string;
  menuType: string;
  url: string;
  handler: string;
  accel: string;
  mnem: string;
  tooltip: string;
  icon: string;
  launch: string;
  multi: string;
  refresh: string;
  target: string;
  targetStyle: string;
  parameters: ActionMenuParameter[];
  extraProps: ActionMenuProperty[];
  visibility: ActionMenuVisibilityContext[];
  uiContexts: ActionMenuModeUIContext[];
} {
  const props = detail.properties || [];
  return {
    name: detail.name || idOrName || "",
    label: detail.label || "",
    description: detail.description || "",
    menuType: typeFromDetail(detail, ACTION_MENU_TYPE_ITEM),
    url: detail.url || "",
    handler: handlerFromDetail(detail),
    accel: propertyValue(props, ACTION_MENU_PROP.ACCEL),
    mnem: propertyValue(props, ACTION_MENU_PROP.MNEM),
    tooltip: propertyValue(props, ACTION_MENU_PROP.SHORT_DESC),
    icon: propertyValue(props, ACTION_MENU_PROP.ICON),
    launch: propertyValue(props, ACTION_MENU_PROP.LAUNCH),
    multi: propertyValue(props, ACTION_MENU_PROP.MULTI),
    refresh: propertyValue(props, ACTION_MENU_PROP.REFRESH),
    target: propertyValue(props, ACTION_MENU_PROP.TARGET),
    targetStyle: propertyValue(props, ACTION_MENU_PROP.TARGET_STYLE),
    parameters: normalizeActionMenuParameters(detail.parameters),
    extraProps: extraActionMenuProperties(props),
    visibility: normalizeVisibilityContexts(detail.visibilityContexts),
    uiContexts: normalizeUiContexts(detail.uiContexts),
  };
}

export function ActionMenuDetailPanel({
  idOrName,
  catalogGuid,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  /** GUID from catalog list row when detail wire omits stringValue (#3380). */
  catalogGuid?: string | null;
  onBack: () => void;
  onSaved?: (detail: ActionMenu) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<ActionMenu | null>(null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [menuType, setMenuType] = useState(ACTION_MENU_TYPE_ITEM);
  const [url, setUrl] = useState("");
  const [handler, setHandler] = useState<string>("CLIENT");
  const [accel, setAccel] = useState("");
  const [mnem, setMnem] = useState("");
  const [tooltip, setTooltip] = useState("");
  const [icon, setIcon] = useState("");
  const [launch, setLaunch] = useState("");
  const [multi, setMulti] = useState("");
  const [refresh, setRefresh] = useState("");
  const [target, setTarget] = useState("");
  const [targetStyle, setTargetStyle] = useState("");
  const [parameters, setParameters] = useState<ActionMenuParameter[]>([]);
  const [extraProps, setExtraProps] = useState<ActionMenuProperty[]>([]);
  const [visibility, setVisibility] = useState<ActionMenuVisibilityContext[]>([]);
  const [uiContexts, setUiContexts] = useState<ActionMenuModeUIContext[]>([]);
  const [tab, setTab] = useState<AmTab>("usage");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const [draftChildren, setDraftChildren] = useState<ActionMenuChildRef[]>([]);
  const [catalog, setCatalog] = useState<ActionMenu[]>([]);
  const [addChildName, setAddChildName] = useState("");
  const inflight = useRef(false);

  function hydrate(d: ActionMenu, key: string | null, preserveCollections = false): void {
    const loaded = applyLoaded(d, key);
    const keepCollections = preserveCollections || Boolean(d.partialOverlay);
    setName(loaded.name);
    setLabel(loaded.label);
    setDescription(loaded.description);
    setMenuType(loaded.menuType);
    setUrl(loaded.url);
    setHandler(loaded.handler);
    setAccel(loaded.accel);
    setMnem(loaded.mnem);
    setTooltip(loaded.tooltip);
    setIcon(loaded.icon);
    setLaunch(loaded.launch);
    setMulti(loaded.multi);
    setRefresh(loaded.refresh);
    setTarget(loaded.target);
    setTargetStyle(loaded.targetStyle);
    if (d.parameters != null && d.parameters.length > 0) {
      setParameters(loaded.parameters);
    } else if (d.parameters != null && d.parameters.length === 0 && !keepCollections) {
      setParameters([]);
    }
    setExtraProps(loaded.extraProps);
    if (d.visibilityContexts != null && d.visibilityContexts.length > 0) {
      setVisibility(loaded.visibility);
    } else if (
      d.visibilityContexts != null &&
      d.visibilityContexts.length === 0 &&
      !keepCollections
    ) {
      setVisibility([]);
    }
    if (d.uiContexts != null && d.uiContexts.length > 0) {
      setUiContexts(loaded.uiContexts);
    } else if (d.uiContexts != null && d.uiContexts.length === 0 && !keepCollections) {
      setUiContexts([]);
    }
  }

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setDraftChildren([]);
    setAddChildName("");
    setLoading(true);
    getActionMenuDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        hydrate(d, idOrName);
        setDraftChildren(childrenFromDetail(d));
        setAddChildName("");
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.AM_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loaded = detail != null ? applyLoaded(detail, idOrName) : null;
  const loadedName = normalizeActionMenuName(detail?.name || idOrName || "");
  const dirty =
    isNew ||
    loaded == null ||
    normalizeActionMenuName(name) !== loadedName ||
    label !== loaded.label ||
    description !== loaded.description ||
    menuType !== loaded.menuType ||
    url !== loaded.url ||
    handler !== loaded.handler ||
    accel !== loaded.accel ||
    mnem !== loaded.mnem ||
    tooltip !== loaded.tooltip ||
    icon !== loaded.icon ||
    launch !== loaded.launch ||
    multi !== loaded.multi ||
    refresh !== loaded.refresh ||
    target !== loaded.target ||
    targetStyle !== loaded.targetStyle ||
    !actionMenuRowsEqual(parameters, loaded.parameters) ||
    !actionMenuRowsEqual(visibility, loaded.visibility) ||
    !actionMenuRowsEqual(uiContexts, loaded.uiContexts);
  const canSave = !busy && dirty && isActionMenuWriteReady({ isNew, name });
  const writeKey = idOrName || createdKey || normalizeActionMenuName(name);
  const objectGuid = resolveActionMenuObjectGuid(detail, catalogGuid);
  const restUser =
    isRestUserActionMenu(detail) ||
    createdKey != null ||
    (detail != null && !isKnownSystemActionMenuName(detail.name || writeKey));
  const childrenWritable = isActionMenuChildrenWritable({
    isNew,
    isRestUser: restUser,
    menuType: typeFromDetail(detail, menuType),
    url: detail?.url || "",
  });
  const loadedChildren = childrenFromDetail(detail);
  const children = childrenWritable ? draftChildren : loadedChildren;
  const childrenDirty = childrenWritable && !childrenOrderEqual(draftChildren, loadedChildren);
  const canSaveChildren = !busy && childrenDirty && Boolean(writeKey);
  const availableChildren = catalogsNotInChildren(catalog, draftChildren, {
    name: writeKey,
    id: detail?.id,
    guidString: objectGuid,
  });

  useEffect(() => {
    if (isNew) {
      return;
    }
    let cancelled = false;
    listActionMenus()
      .then((rows) => {
        if (!cancelled) setCatalog(rows);
      })
      .catch(() => {
        if (!cancelled) setCatalog([]);
      });
    return () => {
      cancelled = true;
    };
  }, [isNew, idOrName, createdKey]);

  function identityBody(): ActionMenuWriteBody {
    const body: ActionMenuWriteBody = {
      name: isNew ? normalizeActionMenuName(name) : detail?.name || normalizeActionMenuName(name),
      label,
      description,
      menuType,
    };
    if (url.trim()) {
      body.url = url.trim();
    }
    return body;
  }

  function ui03Body(): ActionMenuWriteBody {
    const body = identityBody();
    body.handler = handler;
    body.url = url.trim();
    const overlayPartial = Boolean(detail?.partialOverlay);
    const nextParams = parameters
      .filter((p) => (p.name || "").trim())
      .map((p) => ({
        name: (p.name || "").trim(),
        value: p.value || "",
        description: p.description || "",
      }));
    if (!(overlayPartial && nextParams.length === 0)) {
      body.parameters = nextParams;
    }
    body.properties = mergeActionMenuProperties(extraProps, {
      [ACTION_MENU_PROP.ACCEL]: accel,
      [ACTION_MENU_PROP.MNEM]: mnem,
      [ACTION_MENU_PROP.SHORT_DESC]: tooltip,
      [ACTION_MENU_PROP.ICON]: icon,
      [ACTION_MENU_PROP.LAUNCH]: launch,
      [ACTION_MENU_PROP.MULTI]: multi,
      [ACTION_MENU_PROP.REFRESH]: refresh,
      [ACTION_MENU_PROP.TARGET]: target,
      [ACTION_MENU_PROP.TARGET_STYLE]: targetStyle,
    });
    const nextVis = visibility
      .filter((row) => (row.name || "").trim() && (row.value || "").trim())
      .map((row) => ({
        name: (row.name || "").trim(),
        value: (row.value || "").trim(),
        description: row.description || "",
      }));
    if (!(overlayPartial && nextVis.length === 0)) {
      body.visibilityContexts = nextVis;
    }
    const nextUi = uiContexts
      .filter((row) => (row.modeId || "").trim() && (row.contextId || "").trim())
      .map((row) => ({
        modeId: (row.modeId || "").trim(),
        contextId: (row.contextId || "").trim(),
        modeName: row.modeName || "",
        contextName: row.contextName || "",
        description: row.description || "",
      }));
    if (!(overlayPartial && nextUi.length === 0)) {
      body.uiContexts = nextUi;
    }
    return body;
  }

  function writeBody(): ActionMenuWriteBody {
    // POST create stays identity-only so JAXB unwrap (#4171) is unchanged.
    return isNew ? identityBody() : ui03Body();
  }

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409) {
      const fromBody = extractRestErrorMessage(err.body);
      if (fromBody) return fromBody;
      return isNew ? DEV_MSG.AM_DUPLICATE : DEV_MSG.AM_SYSTEM;
    }
    if (isApiError(err) && err.status === 400) {
      if (isNew) return DEV_MSG.AM_INVALID_NAME;
      const fromBody = extractRestErrorMessage(err.body);
      return fromBody || DEV_MSG.AM_SAVE_ERROR;
    }
    if (isApiError(err) && err.status === 403) return DEV_MSG.AM_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.AM_NOT_FOUND;
    return DEV_MSG.AM_SAVE_ERROR;
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const creating = isNew || !writeKey;
      let saved = creating
        ? await createActionMenu(identityBody())
        : await saveActionMenu(writeKey, ui03Body());
      const persistedName = (saved.name || "").trim();
      if (!persistedName) {
        setError(DEV_MSG.AM_MISSING_PERSISTED_NAME);
        return;
      }
      const persistKey =
        saved.id != null && saved.id > 0
          ? String(saved.id)
          : resolveActionMenuObjectGuid(saved) || persistedName;
      if (creating) {
        setCreatedKey(persistKey);
        const followUp = ui03Body();
        followUp.name = persistedName;
        const hasUi03 =
          followUp.handler === "SERVER" ||
          (followUp.parameters != null && followUp.parameters.length > 0) ||
          (followUp.visibilityContexts != null && followUp.visibilityContexts.length > 0) ||
          (followUp.uiContexts != null && followUp.uiContexts.length > 0) ||
          accel.trim() !== "" ||
          mnem.trim() !== "" ||
          tooltip.trim() !== "" ||
          icon.trim() !== "" ||
          launch.trim() !== "" ||
          multi.trim() !== "" ||
          refresh.trim() !== "" ||
          target.trim() !== "" ||
          targetStyle.trim() !== "";
        if (hasUi03) {
          try {
            saved = await saveActionMenu(persistKey, followUp);
          } catch (followUpErr: unknown) {
            try {
              await deleteActionMenu(persistKey);
            } catch (rollbackErr: unknown) {
              console.error("Action menu create rollback DELETE failed", rollbackErr);
              setNotice(DEV_MSG.AM_CREATE_ROLLBACK_FAILED);
            }
            throw followUpErr;
          }
        }
      }
      setDetail(saved);
      hydrate(saved, persistedName, true);
      setDraftChildren(childrenFromDetail(saved));
      setNotice(DEV_MSG.AM_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 409) {
        setError(saveFallback(err));
      } else {
        setError(panelErrMsg(err, saveFallback(err)));
      }
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || !writeKey || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current) return;
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteActionMenu(writeKey);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.AM_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.AM_NOT_FOUND
            : isApiError(err) && err.status === 409
              ? DEV_MSG.AM_SYSTEM
              : DEV_MSG.AM_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function childrenSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409) {
      const fromBody = extractRestErrorMessage(err.body);
      if (fromBody && /system/i.test(fromBody)) return DEV_MSG.AM_SYSTEM;
      return fromBody || DEV_MSG.AM_SYSTEM;
    }
    if (isApiError(err) && err.status === 400) {
      const fromBody = extractRestErrorMessage(err.body);
      return fromBody || DEV_MSG.AM_CHILDREN_INVALID;
    }
    if (isApiError(err) && err.status === 403) return DEV_MSG.AM_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.AM_NOT_FOUND;
    return DEV_MSG.AM_CHILDREN_SAVE_ERROR;
  }

  async function handleSaveChildren(): Promise<void> {
    if (!canSaveChildren || !writeKey || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await saveActionMenuChildren(writeKey, toChildWriteBody(draftChildren));
      setDetail(saved);
      setDraftChildren(childrenFromDetail(saved));
      setNotice(DEV_MSG.AM_CHILDREN_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 409) {
        setError(childrenSaveFallback(err));
      } else {
        setError(panelErrMsg(err, childrenSaveFallback(err)));
      }
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function handleAddChild(): void {
    const candidate = availableChildren.find(
      (row) => (row.name || "").trim() === addChildName.trim(),
    );
    if (!candidate) return;
    setDraftChildren(addActionMenuChild(draftChildren, candidate));
    setAddChildName("");
  }

  const title = isNew
    ? DEV_MSG.AM_NEW
    : detail?.label || detail?.name || idOrName || DEV_MSG.AM_EDIT;

  const gapList =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? withoutStaleActionMenuWriteGap(detail.designGaps)
      : [];

  function yesNoSelect(
    id: string,
    testId: string,
    labelText: string,
    value: string,
    onChange: (next: string) => void,
  ): React.ReactElement {
    return (
      <div style={fieldStyle}>
        <label htmlFor={id}>{labelText}</label>
        <select
          id={id}
          data-testid={testId}
          style={inputStyle}
          value={value}
          disabled={busy}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">{DEV_MSG.AM_UNSET}</option>
          <option value="yes">{DEV_MSG.AM_YES}</option>
          <option value="no">{DEV_MSG.AM_NO}</option>
        </select>
      </div>
    );
  }

  const childrenHint = !restUser
    ? DEV_MSG.AM_CHILDREN_READONLY
    : childrenWritable
      ? DEV_MSG.AM_CHILDREN_HINT
      : DEV_MSG.AM_CHILDREN_NEED_CASCADE;


  return (
    <div data-testid="developer-am-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-am-back"
        aria-label={DEV_MSG.AM_BACK}
        style={backButton}
      >
        ← {DEV_MSG.AM_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-am-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-am-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-am-detail-loading">{DEV_MSG.AM_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-am-detail-title">
              {title}
            </h2>
            {!isNew && detail?.description && !dirty ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            {!isNew && objectGuid ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.AM_COL_GUID}</dt>
                <dd
                  style={{ margin: 0, ...monoCell }}
                  data-testid="developer-am-detail-guid"
                >
                  {objectGuid}
                </dd>
                <dt>{DEV_MSG.AM_COL_HANDLER}</dt>
                <dd style={{ margin: 0 }} data-testid="developer-am-meta-handler">
                  {handler || "—"}
                </dd>
                <dt>{DEV_MSG.AM_COL_SORT}</dt>
                <dd style={{ margin: 0 }}>
                  {detail?.sortRank != null ? String(detail.sortRank) : "—"}
                </dd>
              </dl>
            ) : !isNew ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.AM_COL_GUID}</dt>
                <dd
                  style={{ margin: 0, ...monoCell }}
                  data-testid="developer-am-detail-guid"
                >
                  —
                </dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="am-name">{DEV_MSG.AM_FORM_NAME}</label>
            <input
              id="am-name"
              data-testid="developer-am-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.AM_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.AM_NAME_HINT}
              </span>
            )}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-label">{DEV_MSG.AM_FORM_LABEL}</label>
            <input
              id="am-label"
              data-testid="developer-am-label"
              style={inputStyle}
              value={label}
              disabled={busy}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-desc">{DEV_MSG.AM_FORM_DESCRIPTION}</label>
            <input
              id="am-desc"
              data-testid="developer-am-description"
              style={inputStyle}
              value={description}
              disabled={busy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-type">{DEV_MSG.AM_FORM_TYPE}</label>
            <select
              id="am-type"
              data-testid="developer-am-type"
              style={inputStyle}
              value={menuType}
              disabled={busy}
              onChange={(e) => setMenuType(e.target.value)}
            >
              {ACTION_MENU_TYPES.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>

          <div
            role="tablist"
            aria-label={DEV_MSG.AM_TAB_USAGE}
            data-testid="developer-am-tabs"
            style={tabListStyle}
          >
            {(
              [
                ["usage", DEV_MSG.AM_TAB_USAGE],
                ["command", DEV_MSG.AM_TAB_COMMAND],
                ["visibility", DEV_MSG.AM_TAB_VISIBILITY],
              ] as const
            ).map(([id, labelText]) => (
              <button
                key={id}
                type="button"
                role="tab"
                id={`developer-am-tab-${id}`}
                data-testid={`developer-am-tab-${id}`}
                aria-selected={tab === id}
                aria-controls={`developer-am-panel-${id}`}
                onClick={() => setTab(id)}
                style={tabStyle(tab === id)}
              >
                {labelText}
              </button>
            ))}
          </div>

          {tab === "usage" ? (
            <section
              role="tabpanel"
              id="developer-am-panel-usage"
              data-testid="developer-am-panel-usage"
              aria-labelledby="developer-am-tab-usage"
            >
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.AM_USAGE_HINT}</p>
              <div style={fieldStyle}>
                <label htmlFor="am-handler">{DEV_MSG.AM_FORM_HANDLER}</label>
                <select
                  id="am-handler"
                  data-testid="developer-am-handler"
                  style={inputStyle}
                  value={handler}
                  disabled={busy}
                  onChange={(e) => setHandler(e.target.value)}
                >
                  {ACTION_MENU_HANDLERS.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              </div>
              <div style={fieldStyle}>
                <label htmlFor="am-accel">{DEV_MSG.AM_FORM_ACCEL}</label>
                <input
                  id="am-accel"
                  data-testid="developer-am-accel"
                  style={inputStyle}
                  value={accel}
                  disabled={busy}
                  onChange={(e) => setAccel(e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div style={fieldStyle}>
                <label htmlFor="am-mnem">{DEV_MSG.AM_FORM_MNEM}</label>
                <input
                  id="am-mnem"
                  data-testid="developer-am-mnem"
                  style={inputStyle}
                  value={mnem}
                  disabled={busy}
                  onChange={(e) => setMnem(e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div style={fieldStyle}>
                <label htmlFor="am-tooltip">{DEV_MSG.AM_FORM_TOOLTIP}</label>
                <input
                  id="am-tooltip"
                  data-testid="developer-am-tooltip"
                  style={inputStyle}
                  value={tooltip}
                  disabled={busy}
                  onChange={(e) => setTooltip(e.target.value)}
                />
              </div>
              <div style={fieldStyle}>
                <label htmlFor="am-icon">{DEV_MSG.AM_FORM_ICON}</label>
                <input
                  id="am-icon"
                  data-testid="developer-am-icon"
                  style={{ ...inputStyle, fontFamily: "monospace" }}
                  value={icon}
                  disabled={busy}
                  onChange={(e) => setIcon(e.target.value)}
                  autoComplete="off"
                />
              </div>
              {yesNoSelect("am-launch", "developer-am-launch", DEV_MSG.AM_FORM_LAUNCH, launch, setLaunch)}
              {yesNoSelect("am-multi", "developer-am-multiselect", DEV_MSG.AM_FORM_MULTI, multi, setMulti)}
              <div style={fieldStyle}>
                <label htmlFor="am-refresh">{DEV_MSG.AM_FORM_REFRESH}</label>
                <select
                  id="am-refresh"
                  data-testid="developer-am-refresh"
                  style={inputStyle}
                  value={refresh}
                  disabled={busy}
                  onChange={(e) => setRefresh(e.target.value)}
                >
                  <option value="">{DEV_MSG.AM_UNSET}</option>
                  {ACTION_MENU_REFRESH_HINTS.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              </div>
            </section>
          ) : null}

          {tab === "command" ? (
            <section
              role="tabpanel"
              id="developer-am-panel-command"
              data-testid="developer-am-panel-command"
              aria-labelledby="developer-am-tab-command"
            >
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {DEV_MSG.AM_COMMAND_HINT}
              </p>
              <div style={fieldStyle}>
                <label htmlFor="am-url">{DEV_MSG.AM_FORM_URL}</label>
                <input
                  id="am-url"
                  data-testid="developer-am-url"
                  style={{ ...inputStyle, fontFamily: "monospace" }}
                  value={url}
                  disabled={busy}
                  onChange={(e) => setUrl(e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div style={fieldStyle}>
                <label htmlFor="am-target">{DEV_MSG.AM_FORM_TARGET}</label>
                <input
                  id="am-target"
                  data-testid="developer-am-target"
                  style={inputStyle}
                  value={target}
                  disabled={busy}
                  onChange={(e) => setTarget(e.target.value)}
                  autoComplete="off"
                />
              </div>
              <div style={fieldStyle}>
                <label htmlFor="am-target-style">{DEV_MSG.AM_FORM_TARGET_STYLE}</label>
                <input
                  id="am-target-style"
                  data-testid="developer-am-target-style"
                  style={inputStyle}
                  value={targetStyle}
                  disabled={busy}
                  onChange={(e) => setTargetStyle(e.target.value)}
                  autoComplete="off"
                />
              </div>
              <section data-testid="developer-am-params">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_PARAMS}</h3>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.AM_PARAMS_HINT}</p>
                {parameters.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-am-params-empty">
                    {DEV_MSG.AM_NONE}
                  </p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-am-params-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_PARAM}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_VALUE}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_ACTIONS}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {parameters.map((p, i) => (
                          <tr
                            key={`param-${i}`}
                            data-testid={`developer-am-param-row-${i}`}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px" }}>
                              <input
                                data-testid={`developer-am-param-name-${i}`}
                                aria-label={DEV_MSG.AM_COL_PARAM}
                                style={{ ...inputStyle, fontFamily: "monospace", width: "100%" }}
                                value={p.name || ""}
                                disabled={busy}
                                onChange={(e) => {
                                  const next = [...parameters];
                                  next[i] = { ...p, name: e.target.value };
                                  setParameters(next);
                                }}
                              />
                            </td>
                            <td style={{ padding: "8px" }}>
                              <input
                                data-testid={`developer-am-param-value-${i}`}
                                aria-label={DEV_MSG.AM_COL_VALUE}
                                style={{ ...inputStyle, width: "100%" }}
                                value={p.value || ""}
                                disabled={busy}
                                onChange={(e) => {
                                  const next = [...parameters];
                                  next[i] = { ...p, value: e.target.value };
                                  setParameters(next);
                                }}
                              />
                            </td>
                            <td style={{ padding: "8px" }}>
                              <button
                                type="button"
                                data-testid={`developer-am-param-remove-${i}`}
                                aria-label={DEV_MSG.AM_REMOVE_PARAM}
                                disabled={busy}
                                onClick={() => setParameters(parameters.filter((_, j) => j !== i))}
                              >
                                {DEV_MSG.AM_REMOVE_PARAM}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                <button
                  type="button"
                  data-testid="developer-am-param-add"
                  disabled={busy}
                  onClick={() => setParameters([...parameters, { name: "", value: "" }])}
                  style={{ marginTop: "8px" }}
                >
                  {DEV_MSG.AM_ADD_PARAM}
                </button>
              </section>
              {extraProps.length > 0 ? (
                <section style={{ marginTop: "16px" }} data-testid="developer-am-props">
                  <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_PROPS}</h3>
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-am-props-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_PROP}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_VALUE}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {extraProps.map((p, i) => (
                          <tr
                            key={`${p.name ?? "prop"}-${i}`}
                            data-testid={`developer-am-prop-row-${i}`}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {p.name || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{p.value || "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>
              ) : (
                <section style={{ marginTop: "16px" }} data-testid="developer-am-props">
                  <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_PROPS}</h3>
                  <p style={{ color: catalogColors.empty }} data-testid="developer-am-props-empty">
                    {DEV_MSG.AM_NONE}
                  </p>
                </section>
              )}
            </section>
          ) : null}

          {tab === "visibility" ? (
            <section
              role="tabpanel"
              id="developer-am-panel-visibility"
              data-testid="developer-am-panel-visibility"
              aria-labelledby="developer-am-tab-visibility"
            >
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_VIS_CONTEXTS}</h3>
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.AM_VIS_HINT}</p>
              {visibility.length === 0 ? (
                <p style={{ color: catalogColors.empty }} data-testid="developer-am-vis-empty">
                  {DEV_MSG.AM_NONE}
                </p>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table
                    data-testid="developer-am-vis-table"
                    style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                  >
                    <thead>
                      <tr style={tableHeaderRow}>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_CONTEXT}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_VALUE}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_ACTIONS}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visibility.map((row, i) => (
                        <tr
                          key={`vis-${i}`}
                          data-testid={`developer-am-vis-row-${i}`}
                          style={tableRow}
                        >
                          <td style={{ padding: "8px" }}>
                            <select
                              data-testid={`developer-am-vis-name-${i}`}
                              aria-label={DEV_MSG.AM_COL_CONTEXT}
                              style={inputStyle}
                              value={row.name || ""}
                              disabled={busy}
                              onChange={(e) => {
                                const next = [...visibility];
                                next[i] = { ...row, name: e.target.value };
                                setVisibility(next);
                              }}
                            >
                              <option value="">{DEV_MSG.AM_UNSET}</option>
                              {ACTION_MENU_VISIBILITY_ALIASES.map((alias) => (
                                <option key={alias} value={alias}>
                                  {alias}
                                </option>
                              ))}
                              {["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"].map(
                                (id) => (
                                  <option key={`vis-id-${id}`} value={id}>
                                    {id}
                                  </option>
                                ),
                              )}
                            </select>
                          </td>
                          <td style={{ padding: "8px" }}>
                            <input
                              data-testid={`developer-am-vis-value-${i}`}
                              aria-label={DEV_MSG.AM_COL_VALUE}
                              style={inputStyle}
                              value={row.value || ""}
                              disabled={busy}
                              onChange={(e) => {
                                const next = [...visibility];
                                next[i] = { ...row, value: e.target.value };
                                setVisibility(next);
                              }}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>
                            <button
                              type="button"
                              data-testid={`developer-am-vis-remove-${i}`}
                              aria-label={DEV_MSG.AM_REMOVE_VIS}
                              disabled={busy}
                              onClick={() => setVisibility(visibility.filter((_, j) => j !== i))}
                            >
                              {DEV_MSG.AM_REMOVE_VIS}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <button
                type="button"
                data-testid="developer-am-vis-add"
                disabled={busy}
                onClick={() => setVisibility([...visibility, { name: "community", value: "" }])}
                style={{ marginTop: "8px" }}
              >
                {DEV_MSG.AM_ADD_VIS}
              </button>

              <h3 style={{ fontSize: "1rem", marginTop: "16px" }}>{DEV_MSG.AM_UI_CONTEXTS}</h3>
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.AM_UI_HINT}</p>
              {uiContexts.length === 0 ? (
                <p style={{ color: catalogColors.empty }} data-testid="developer-am-uictx-empty">
                  {DEV_MSG.AM_NONE}
                </p>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table
                    data-testid="developer-am-uictx-table"
                    style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                  >
                    <thead>
                      <tr style={tableHeaderRow}>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_MODE_ID}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_MODE_NAME}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_CTX_ID}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_CTX_NAME}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_ACTIONS}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {uiContexts.map((row, i) => (
                        <tr
                          key={`uictx-${i}`}
                          data-testid={`developer-am-uictx-row-${i}`}
                          style={tableRow}
                        >
                          <td style={{ padding: "8px" }}>
                            <input
                              data-testid={`developer-am-uictx-mode-${i}`}
                              aria-label={DEV_MSG.AM_COL_MODE_ID}
                              style={{ ...inputStyle, fontFamily: "monospace" }}
                              value={row.modeId || ""}
                              disabled={busy}
                              onChange={(e) => {
                                const next = [...uiContexts];
                                next[i] = { ...row, modeId: e.target.value };
                                setUiContexts(next);
                              }}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>
                            <input
                              data-testid={`developer-am-uictx-mode-name-${i}`}
                              aria-label={DEV_MSG.AM_COL_MODE_NAME}
                              style={inputStyle}
                              value={row.modeName || ""}
                              disabled={busy}
                              onChange={(e) => {
                                const next = [...uiContexts];
                                next[i] = { ...row, modeName: e.target.value };
                                setUiContexts(next);
                              }}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>
                            <input
                              data-testid={`developer-am-uictx-context-${i}`}
                              aria-label={DEV_MSG.AM_COL_CTX_ID}
                              style={{ ...inputStyle, fontFamily: "monospace" }}
                              value={row.contextId || ""}
                              disabled={busy}
                              onChange={(e) => {
                                const next = [...uiContexts];
                                next[i] = { ...row, contextId: e.target.value };
                                setUiContexts(next);
                              }}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>
                            <input
                              data-testid={`developer-am-uictx-context-name-${i}`}
                              aria-label={DEV_MSG.AM_COL_CTX_NAME}
                              style={inputStyle}
                              value={row.contextName || ""}
                              disabled={busy}
                              onChange={(e) => {
                                const next = [...uiContexts];
                                next[i] = { ...row, contextName: e.target.value };
                                setUiContexts(next);
                              }}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>
                            <button
                              type="button"
                              data-testid={`developer-am-uictx-remove-${i}`}
                              aria-label={DEV_MSG.AM_REMOVE_UI}
                              disabled={busy}
                              onClick={() => setUiContexts(uiContexts.filter((_, j) => j !== i))}
                            >
                              {DEV_MSG.AM_REMOVE_UI}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <button
                type="button"
                data-testid="developer-am-uictx-add"
                disabled={busy}
                onClick={() =>
                  setUiContexts([...uiContexts, { modeId: "", contextId: "", modeName: "", contextName: "" }])
                }
                style={{ marginTop: "8px" }}
              >
                {DEV_MSG.AM_ADD_UI}
              </button>
            </section>
          ) : null}

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", margin: "16px 0" }}>
            <button
              type="button"
              data-testid="developer-am-save"
              aria-label={DEV_MSG.AM_SAVE}
              disabled={!canSave}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: canSave ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSave ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.AM_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-am-cancel"
              disabled={busy}
              onClick={onBack}
              style={{
                padding: "8px 16px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.AM_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-am-delete"
                aria-label={DEV_MSG.AM_DELETE}
                disabled={busy}
                onClick={requestDelete}
                style={{
                  padding: "8px 16px",
                  background: "#c53030",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy ? "wait" : "pointer",
                  marginLeft: "auto",
                }}
              >
                {DEV_MSG.AM_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
              <section data-testid="developer-am-children">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_CHILDREN}</h3>
                <p
                  style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
                  data-testid={
                    childrenWritable ? "developer-am-children-hint" : "developer-am-children-readonly"
                  }
                >
                  {childrenHint}
                </p>
                {children.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-am-children-empty">
                    {DEV_MSG.AM_CHILDREN_EMPTY}
                  </p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-am-children-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_CHILDREN_COL_NAME}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.AM_CHILDREN_COL_LABEL}</th>
                          {childrenWritable ? (
                            <th style={{ padding: "8px" }}>{DEV_MSG.AM_CHILDREN_COL_ACTIONS}</th>
                          ) : null}
                        </tr>
                      </thead>
                      <tbody>
                        {children.map((c, i) => (
                          <tr
                            key={`${c.name ?? "child"}-${i}`}
                            data-testid={`developer-am-child-row-${i}`}
                            data-am-child-name={c.name || ""}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {c.name || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>
                              {catalog.find(
                                (row) =>
                                  (row.name || "").trim().toLowerCase() ===
                                  (c.name || "").trim().toLowerCase(),
                              )?.label || "—"}
                            </td>
                            {childrenWritable ? (
                              <td style={{ padding: "8px" }}>
                                <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                                  <button
                                    type="button"
                                    data-testid={`developer-am-child-up-${i}`}
                                    aria-label={DEV_MSG.AM_CHILDREN_MOVE_UP}
                                    disabled={busy || i === 0}
                                    onClick={() =>
                                      setDraftChildren(moveActionMenuChild(draftChildren, i, -1))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.AM_CHILDREN_MOVE_UP}
                                  </button>
                                  <button
                                    type="button"
                                    data-testid={`developer-am-child-down-${i}`}
                                    aria-label={DEV_MSG.AM_CHILDREN_MOVE_DOWN}
                                    disabled={busy || i === children.length - 1}
                                    onClick={() =>
                                      setDraftChildren(moveActionMenuChild(draftChildren, i, 1))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.AM_CHILDREN_MOVE_DOWN}
                                  </button>
                                  <button
                                    type="button"
                                    data-testid={`developer-am-child-remove-${i}`}
                                    aria-label={DEV_MSG.AM_CHILDREN_REMOVE}
                                    disabled={busy}
                                    onClick={() =>
                                      setDraftChildren(removeActionMenuChild(draftChildren, i))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.AM_CHILDREN_REMOVE}
                                  </button>
                                </div>
                              </td>
                            ) : null}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                {childrenWritable ? (
                  <div
                    style={{
                      marginTop: "12px",
                      display: "flex",
                      gap: "8px",
                      flexWrap: "wrap",
                      alignItems: "flex-end",
                    }}
                    data-testid="developer-am-children-editor"
                  >
                    <label
                      htmlFor="am-child-add"
                      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
                    >
                      {DEV_MSG.AM_CHILDREN_ADD_PICKER}
                      <select
                        id="am-child-add"
                        data-testid="developer-am-child-source"
                        style={inputStyle}
                        value={addChildName}
                        disabled={busy || availableChildren.length === 0}
                        onChange={(e) => setAddChildName(e.target.value)}
                      >
                        <option value="">
                          {availableChildren.length ? "—" : DEV_MSG.AM_NONE}
                        </option>
                        {availableChildren.map((row) => (
                          <option key={row.name || String(row.id)} value={row.name || ""}>
                            {row.label ? `${row.label} (${row.name})` : row.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      data-testid="developer-am-child-add"
                      aria-label={DEV_MSG.AM_CHILDREN_ADD}
                      disabled={busy || !addChildName.trim()}
                      onClick={handleAddChild}
                      style={{
                        ...actionButton,
                        padding: "8px 12px",
                        background: addChildName.trim()
                          ? catalogColors.accent
                          : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        cursor: addChildName.trim() && !busy ? "pointer" : "not-allowed",
                      }}
                    >
                      {DEV_MSG.AM_CHILDREN_ADD}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-am-children-save"
                      aria-label={DEV_MSG.AM_CHILDREN_SAVE}
                      disabled={!canSaveChildren}
                      onClick={() => void handleSaveChildren()}
                      style={{
                        padding: "8px 16px",
                        background: canSaveChildren ? catalogColors.accent : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: canSaveChildren ? "pointer" : "not-allowed",
                      }}
                    >
                      {busy ? DEV_MSG.AM_CHILDREN_SAVING : DEV_MSG.AM_CHILDREN_SAVE}
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    data-testid="developer-am-children-save"
                    aria-label={DEV_MSG.AM_CHILDREN_SAVE}
                    disabled
                    style={{
                      marginTop: "12px",
                      padding: "8px 16px",
                      background: catalogColors.disabled,
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: "not-allowed",
                    }}
                  >
                    {DEV_MSG.AM_CHILDREN_SAVE}
                  </button>
                )}
              </section>
              <ObjectAclSection
                objectGuid={objectGuid}
                objectKind="action-menu"
                testIdPrefix="developer-am-acl"
              />

              <section style={{ marginTop: "16px" }} data-testid="developer-am-gaps">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_GAPS}</h3>
                <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {gapList.map((g) => (
                    <li key={g}>{g}</li>
                  ))}
                </ul>
              </section>
            </>
          ) : null}
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.AM_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
