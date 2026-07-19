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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useState } from "react";
import {
  associateContentList,
  copyEdition,
  createEdition,
  deleteEdition,
  disassociateContentList,
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
  buttonStyle,
  errorStyle,
  formRowStyle,
  listItemStyle,
  listStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface EditionEditorProps {
  siteId: string;
  edition: EditionSummary | null;
  sites: Array<{ name: string; id: string }>;
  onSaved: () => void;
  onCancel: () => void;
}

export function EditionEditor({
  siteId,
  edition,
  sites,
  onSaved,
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
    listEditionContentLists(edition.editionId)
      .then(setAssoc)
      .catch(() => setAssoc([]));
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
      .then((c) => {
        setContexts(c);
        if (c.length > 0) {
          setPickCtx(String(c[0].contextId ?? ""));
        }
      })
      .catch(() => setContexts([]));
  }, [edition?.editionId]);

  async function handleSave(): Promise<void> {
    if (!name.trim()) {
      setError("Name is required");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body: EditionSummary = {
        name: name.trim(),
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
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
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
    try {
      await deleteEdition(edition.editionId);
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setSaving(false);
    }
  }

  async function handleCopy(): Promise<void> {
    if (!edition?.editionId) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await copyEdition({
        sourceEditionId: edition.editionId,
        targetSiteId: copySiteId,
        newName: copyName.trim() || undefined,
        copyContentLists: true,
      });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setSaving(false);
    }
  }

  async function handleAssociate(): Promise<void> {
    if (!edition?.editionId || !pickCl || !pickCtx) {
      setError("Select content list and delivery context");
      return;
    }
    setError(null);
    try {
      await associateContentList(edition.editionId, {
        contentListId: pickCl,
        deliveryContextId: pickCtx,
      });
      reloadAssoc();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  async function handleDisassociate(clId: string): Promise<void> {
    if (!edition?.editionId) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    try {
      await disassociateContentList(edition.editionId, clId);
      reloadAssoc();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
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
          <h4>Associated content lists</h4>
          {assoc.length === 0 ? (
            <p style={{ color: "#666" }}>None</p>
          ) : (
            <ul style={listStyle}>
              {assoc.map((c) => (
                <li key={c.contentListId ?? c.name} style={listItemStyle}>
                  <span>
                    {c.name}{" "}
                    <span style={{ color: "#888" }}>({c.listType})</span>
                  </span>
                  {c.contentListId && (
                    <button
                      type="button"
                      style={buttonStyle}
                      onClick={() => void handleDisassociate(c.contentListId!)}
                    >
                      Remove
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
          <div style={toolbarStyle}>
            <select
              value={pickCl}
              onChange={(e) => setPickCl(e.target.value)}
              aria-label="Content list to associate"
            >
              <option value="">Select content list</option>
              {available.map((l) => (
                <option key={l.contentListId} value={l.contentListId}>
                  {l.name}
                </option>
              ))}
            </select>
            <select
              value={pickCtx}
              onChange={(e) => setPickCtx(e.target.value)}
              aria-label="Delivery context"
            >
              {contexts.map((c) => (
                <option key={c.contextId} value={c.contextId}>
                  {c.name}
                </option>
              ))}
            </select>
            <button type="button" style={buttonStyle} onClick={() => void handleAssociate()}>
              Associate
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
