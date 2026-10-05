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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useRef, useState } from "react";
import {
  associateContentList,
  copyEdition,
  createEdition,
  deleteEdition,
  disassociateContentList,
  reorderEditionContentList,
  listContentLists,
  listContexts,
  listEditionContentLists,
  updateEdition,
  type ContentListSummary,
  type ContextSummary,
  type EditionSummary,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import {
  mapEditionContentListAssociateError,
  mapEditionContentListDisassociateError,
  mapEditionContentListReorderError,
  mapEditionCopyError,
  mapEditionDeleteError,
  mapEditionSaveError,
} from "../editionSaveErrors";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  listItemStyle,
  listStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

/** Result of a successful copy, so Design can show the new row on the target site. */
export interface EditionCopiedInfo {
  targetSiteId: string;
  name?: string;
}

export interface EditionEditorProps {
  siteId: string;
  edition: EditionSummary | null;
  sites: Array<{ name: string; id: string }>;
  onSaved: () => void;
  /** When set, a successful copy uses this instead of {@link onSaved}. */
  onCopied?: (info: EditionCopiedInfo) => void;
  onCancel: () => void;
}

export function EditionEditor({
  siteId,
  edition,
  sites,
  onSaved,
  onCopied,
  onCancel,
}: EditionEditorProps): React.ReactElement {
  const [name, setName] = useState(edition?.name ?? "");
  const [comment, setComment] = useState(edition?.comment ?? "");
  const [priority, setPriority] = useState(edition?.priority ?? 3);
  const [assoc, setAssoc] = useState<ContentListSummary[]>([]);
  const [allLists, setAllLists] = useState<ContentListSummary[]>([]);
  const [contexts, setContexts] = useState<ContextSummary[]>([]);
  const [pickCl, setPickCl] = useState("");
  const [pickCtx, setPickCtx] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [copySiteId, setCopySiteId] = useState(siteId);
  const [copyName, setCopyName] = useState("");
  /** Bumps when an associate succeeds so an in-flight list reload cannot wipe the new row. */
  const assocLoadGen = useRef(0);

  useEffect(() => {
    setName(edition?.name ?? "");
    setComment(edition?.comment ?? "");
    setPriority(edition?.priority ?? 3);
  }, [edition]);

  function reloadAssoc(): void {
    if (!edition?.editionId) {
      setAssoc([]);
      return;
    }
    const gen = assocLoadGen.current;
    const editionId = edition.editionId;
    listEditionContentLists(editionId)
      .then((rows) => {
        if (assocLoadGen.current !== gen) {
          return;
        }
        setAssoc(rows);
      })
      .catch(() => {
        if (assocLoadGen.current !== gen) {
          return;
        }
        setAssoc([]);
      });
  }

  useEffect(() => {
    reloadAssoc();
  }, [edition?.editionId]);

  useEffect(() => {
    if (!edition?.editionId) {
      return;
    }
    listContentLists().then(setAllLists).catch(() => setAllLists([]));
    listContexts()
      .then(setContexts)
      .catch(() => setContexts([]));
  }, [edition?.editionId]);

  async function handleSave(): Promise<void> {
    const trimmedName = name.trim();
    if (!trimmedName) {
      setError("Name is required");
      return;
    }
    // RXEDITION.DISPLAYTITLE is VARCHAR(100). Reject before POST so the operator
    // sees the limit instead of a database 500.
    if (trimmedName.length > 100) {
      setError("Edition name must be 100 characters or fewer");
      return;
    }
    if (!siteId.trim()) {
      setError("name and siteId are required");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body: EditionSummary = {
        name: trimmedName,
        comment,
        priority,
        siteId,
      };
      if (edition?.editionId) {
        await updateEdition(edition.editionId, {
          ...body,
          editionId: edition.editionId,
        });
      } else {
        await createEdition(body);
      }
      onSaved();
    } catch (e) {
      setError(mapEditionSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (!edition?.editionId) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await deleteEdition(edition.editionId);
      onSaved();
    } catch (e) {
      setError(mapEditionDeleteError(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleCopy(): Promise<void> {
    if (!edition?.editionId) {
      return;
    }
    if (!copySiteId.trim()) {
      setError(
        mapEditionCopyError({
          status: 400,
          statusText: "Bad Request",
          body: { message: "sourceEditionId and targetSiteId are required" },
        }),
      );
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const copied = await copyEdition({
        sourceEditionId: edition.editionId,
        targetSiteId: copySiteId,
        newName: copyName.trim() || undefined,
        copyContentLists: true,
      });
      const shownName = copied.name?.trim() || copyName.trim() || undefined;
      if (onCopied) {
        onCopied({ targetSiteId: copySiteId, name: shownName });
      } else {
        onSaved();
      }
    } catch (e) {
      setError(mapEditionCopyError(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleAssociate(): Promise<void> {
    if (!edition?.editionId) {
      return;
    }
    if (!pickCl.trim() || !pickCtx.trim()) {
      setError(message(MSG.PUBLISH.DESIGN.EDITIONS.NEED_LIST_AND_CONTEXT));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await associateContentList(edition.editionId, {
        contentListId: pickCl,
        deliveryContextId: pickCtx,
      });
      assocLoadGen.current += 1;
      const id =
        (typeof created.contentListId === "string" &&
          created.contentListId.trim()) ||
        (typeof created.contentListId === "number" &&
        Number.isFinite(created.contentListId)
          ? String(created.contentListId)
          : pickCl);
      const known = allLists.find((row) => row.contentListId === pickCl);
      const createdName =
        typeof created.name === "string" ? created.name.trim() : "";
      setAssoc((prev) => {
        if (prev.some((row) => row.contentListId === id)) {
          return prev;
        }
        return [
          ...prev,
          {
            contentListId: id,
            name: createdName || known?.name,
            listType: created.listType ?? known?.listType,
            description: created.description,
            generator: created.generator,
            url: created.url,
          },
        ];
      });
      setPickCl("");
    } catch (e) {
      setError(mapEditionContentListAssociateError(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleDisassociate(clId: string): Promise<void> {
    if (!edition?.editionId || !clId.trim() || saving) {
      return;
    }
    if (
      !window.confirm(message(MSG.PUBLISH.DESIGN.EDITIONS.CONFIRM_REMOVE_LIST))
    ) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await disassociateContentList(edition.editionId, clId);
      // Drop only this row after DELETE succeeds. A list reload that fails must
      // not clear every association, and an in-flight list must not restore it.
      assocLoadGen.current += 1;
      setAssoc((prev) =>
        prev.filter((row) => String(row.contentListId ?? "") !== clId),
      );
    } catch (e) {
      setError(mapEditionContentListDisassociateError(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleReorder(
    clId: string,
    index: number,
    direction: -1 | 1,
  ): Promise<void> {
    if (!edition?.editionId || !clId.trim() || saving) {
      return;
    }
    const target = index + direction;
    if (target < 0 || target >= assoc.length) {
      return;
    }
    const confirmText = message(
      direction < 0
        ? MSG.PUBLISH.DESIGN.EDITIONS.CONFIRM_MOVE_UP
        : MSG.PUBLISH.DESIGN.EDITIONS.CONFIRM_MOVE_DOWN,
    );
    if (!window.confirm(confirmText)) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await reorderEditionContentList(edition.editionId, clId, target);
      // Swap only this pair after the write succeeds. A list reload that fails
      // must not restore the previous order.
      assocLoadGen.current += 1;
      setAssoc((prev) => {
        if (index < 0 || index >= prev.length) {
          return prev;
        }
        const nextIndex = index + direction;
        if (nextIndex < 0 || nextIndex >= prev.length) {
          return prev;
        }
        if (String(prev[index]?.contentListId ?? "") !== clId) {
          return prev;
        }
        const copy = prev.slice();
        const [row] = copy.splice(index, 1);
        copy.splice(nextIndex, 0, row);
        return copy;
      });
    } catch (e) {
      setError(mapEditionContentListReorderError(e));
    } finally {
      setSaving(false);
    }
  }

  const assocIds = new Set(assoc.map((a) => a.contentListId));
  const available = allLists.filter((l) => l.contentListId && !assocIds.has(l.contentListId));

  return (
    <div data-testid="edition-editor">
      <h3>{edition?.editionId ? "Edit edition" : "Create edition"}</h3>
      <div style={formRowStyle}>
        <label htmlFor="ed-name">* Name</label>
        <input
          id="ed-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="ed-comment">Comment</label>
        <input
          id="ed-comment"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="ed-priority">Priority (1–5)</label>
        <input
          id="ed-priority"
          type="number"
          min={1}
          max={5}
          value={priority}
          onChange={(e) => setPriority(Number(e.target.value))}
        />
      </div>

      {edition?.editionId && (
        <div style={{ marginTop: 12 }}>
          <h4>{message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATED_LISTS)}</h4>
          {assoc.length === 0 ? (
            <p style={{ color: "#666" }} data-testid="edition-assoc-empty">
              {message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATED_LISTS_NONE)}
            </p>
          ) : (
            <ul style={listStyle} data-testid="edition-assoc-list">
              {assoc.map((c, index) => (
                <li
                  key={c.contentListId ?? c.name}
                  style={listItemStyle}
                  data-testid={
                    c.contentListId
                      ? `edition-assoc-row-${c.contentListId}`
                      : undefined
                  }
                >
                  <span>
                    <span
                      data-testid={
                        c.contentListId
                          ? `edition-assoc-name-${c.contentListId}`
                          : undefined
                      }
                    >
                      {c.name}
                    </span>{" "}
                    <span style={{ color: "#888" }}>({c.listType})</span>
                  </span>
                  {c.contentListId && (
                    <span style={{ display: "flex", gap: 8 }}>
                      <button
                        type="button"
                        data-testid={`edition-move-up-${c.contentListId}`}
                        style={buttonStyle}
                        disabled={saving || index === 0}
                        aria-label={message(MSG.PUBLISH.DESIGN.EDITIONS.MOVE_UP)}
                        onClick={() =>
                          void handleReorder(String(c.contentListId), index, -1)
                        }
                      >
                        {message(MSG.PUBLISH.DESIGN.EDITIONS.MOVE_UP)}
                      </button>
                      <button
                        type="button"
                        data-testid={`edition-move-down-${c.contentListId}`}
                        style={buttonStyle}
                        disabled={saving || index === assoc.length - 1}
                        aria-label={message(
                          MSG.PUBLISH.DESIGN.EDITIONS.MOVE_DOWN,
                        )}
                        onClick={() =>
                          void handleReorder(String(c.contentListId), index, 1)
                        }
                      >
                        {message(MSG.PUBLISH.DESIGN.EDITIONS.MOVE_DOWN)}
                      </button>
                      <button
                        type="button"
                        data-testid={`edition-disassociate-${c.contentListId}`}
                        style={buttonStyle}
                        disabled={saving}
                        onClick={() =>
                          void handleDisassociate(String(c.contentListId))
                        }
                      >
                        {message(MSG.PUBLISH.DESIGN.EDITIONS.REMOVE)}
                      </button>
                    </span>
                  )}
                </li>
              ))}
            </ul>
          )}
          <div style={toolbarStyle}>
            <label htmlFor="ed-assoc-cl">
              {message(MSG.PUBLISH.DESIGN.EDITIONS.SELECT_LIST)}
            </label>
            <select
              id="ed-assoc-cl"
              data-testid="edition-assoc-content-list"
              value={pickCl}
              onChange={(e) => setPickCl(e.target.value)}
              aria-label={message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATE_LIST_ARIA)}
            >
              <option value="">
                {message(MSG.PUBLISH.DESIGN.EDITIONS.SELECT_LIST)}
              </option>
              {available.map((l) => (
                <option key={l.contentListId} value={l.contentListId}>
                  {l.name}
                </option>
              ))}
            </select>
            <label htmlFor="ed-assoc-ctx">
              {message(MSG.PUBLISH.DESIGN.EDITIONS.SELECT_CONTEXT)}
            </label>
            <select
              id="ed-assoc-ctx"
              data-testid="edition-assoc-context"
              value={pickCtx}
              onChange={(e) => setPickCtx(e.target.value)}
              aria-label={message(MSG.PUBLISH.DESIGN.EDITIONS.DELIVERY_CONTEXT_ARIA)}
            >
              <option value="">
                {message(MSG.PUBLISH.DESIGN.EDITIONS.SELECT_CONTEXT)}
              </option>
              {contexts.map((c) => (
                <option key={c.contextId} value={String(c.contextId ?? "")}>
                  {c.name}
                </option>
              ))}
            </select>
            <button
              type="button"
              data-testid="edition-associate"
              style={buttonStyle}
              disabled={saving}
              onClick={() => void handleAssociate()}
            >
              {message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATE)}
            </button>
          </div>
        </div>
      )}

      {edition?.editionId && (
        <div style={{ marginTop: 16, borderTop: "1px solid #eee", paddingTop: 12 }}>
          <h4>Copy to site</h4>
          <div style={formRowStyle}>
            <label htmlFor="copy-site">Target site</label>
            <select
              id="copy-site"
              value={copySiteId}
              onChange={(e) => setCopySiteId(e.target.value)}
            >
              {sites.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          <div style={formRowStyle}>
            <label htmlFor="copy-name">New name (optional)</label>
            <input
              id="copy-name"
              value={copyName}
              onChange={(e) => setCopyName(e.target.value)}
            />
          </div>
          <button
            type="button"
            data-testid="edition-copy"
            style={buttonStyle}
            disabled={saving}
            onClick={() => void handleCopy()}
          >
            Copy edition
          </button>
        </div>
      )}

      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <button
          type="button"
          data-testid="edition-save"
          style={primaryButtonStyle}
          disabled={saving}
          onClick={() => void handleSave()}
        >
          {message(MSG.PUBLISH_SAVE)}
        </button>
        <button type="button" style={buttonStyle} onClick={onCancel}>
          {message(MSG.PUBLISH_BACK)}
        </button>
        {edition?.editionId && (
          <button
            type="button"
            data-testid="edition-delete"
            style={buttonStyle}
            disabled={saving}
            onClick={() => void handleDelete()}
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}
