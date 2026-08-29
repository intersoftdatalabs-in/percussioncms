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

/**
 * React Content Editor host (995): checkout + content-type field form.
 * Rich controls persist through itemmanagement fields / binary APIs.
 */

import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router";
import { getContentTypeDetail } from "../api/developer/contentTypesApi";
import type {
  CommunitySummary,
  ContentTypeFieldSummary,
  KeywordSummary,
} from "../api/developer/types";
import type { ItemEditorBinaryMeta } from "./itemBinaryApi";
import { parsePositiveInt } from "../assembly/assemblyHostUrl";
import { message } from "../i18n/message";
import { mergeEditorRows, type EditorFieldRow } from "./controlKinds";
import {
  checkinEditorItem,
  checkoutEditorItem,
  fetchItemEditorFields,
  saveItemEditorFields,
  type ItemEditorFields,
} from "./itemFieldsApi";
import { uploadItemEditorBinary } from "./itemBinaryApi";
import styles from "./EditorHost.module.css";
import { normalizeEditorMode, type EditorHostMode } from "./editorHostUrl";
import { EDITOR_MSG } from "./messages";
import { CommunityFieldWidget } from "./widgets/CommunityFieldWidget";
import { FileFieldWidget } from "./widgets/FileFieldWidget";
import { HtmlFieldWidget } from "./widgets/HtmlFieldWidget";
import { ImageFieldWidget } from "./widgets/ImageFieldWidget";
import { KeywordFieldWidget } from "./widgets/KeywordFieldWidget";
import { PromoteForm } from "./widgets/PromoteForm";

export { mergeEditorRows } from "./controlKinds";

/** Coerce item-field JSON so controlled inputs never receive a number. */
export function fieldValueAsString(value: unknown): string {
  return value == null ? "" : String(value);
}

export interface EditorHostProps {
  loadFields?: (itemId: string) => Promise<ItemEditorFields>;
  saveFields?: (
    itemId: string,
    payload: ItemEditorFields,
  ) => Promise<ItemEditorFields>;
  checkout?: (itemId: string) => Promise<void>;
  checkin?: (itemId: string) => Promise<void>;
  loadType?: (typeName: string) => Promise<{ fields?: ContentTypeFieldSummary[] }>;
  uploadBinary?: (
    itemId: string,
    field: string,
    file: File,
  ) => Promise<unknown>;
  loadKeywords?: () => Promise<KeywordSummary[]>;
  loadCommunities?: () => Promise<CommunitySummary[]>;
  loadBinaryMeta?: (itemId: string, field: string) => Promise<ItemEditorBinaryMeta>;
}

function badgeKey(mode: EditorHostMode): string {
  if (mode === "view") {
    return EDITOR_MSG.BADGE_VIEW;
  }
  if (mode === "promote") {
    return EDITOR_MSG.BADGE_PROMOTE;
  }
  return EDITOR_MSG.BADGE_EDIT;
}

function EditorFieldControl({
  row,
  itemId,
  locked,
  onChange,
  onFile,
  loadKeywords,
  loadCommunities,
  loadBinaryMeta,
}: {
  row: EditorFieldRow;
  itemId: string;
  locked: boolean;
  onChange: (name: string, value: string) => void;
  onFile: (name: string, file: File | null) => void;
  loadKeywords?: () => Promise<KeywordSummary[]>;
  loadCommunities?: () => Promise<CommunitySummary[]>;
  loadBinaryMeta?: (itemId: string, field: string) => Promise<ItemEditorBinaryMeta>;
}): React.ReactElement {
  if (row.kind === "html") {
    return (
      <HtmlFieldWidget
        name={row.name}
        value={row.value}
        readOnly={locked}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "file") {
    return (
      <FileFieldWidget
        itemId={itemId}
        name={row.name}
        readOnly={locked}
        loadMeta={loadBinaryMeta}
        onFile={(file) => onFile(row.name, file)}
      />
    );
  }
  if (row.kind === "image") {
    return (
      <ImageFieldWidget
        itemId={itemId}
        name={row.name}
        readOnly={locked}
        loadMeta={loadBinaryMeta}
        onFile={(file) => onFile(row.name, file)}
      />
    );
  }
  if (row.kind === "keyword") {
    return (
      <KeywordFieldWidget
        name={row.name}
        value={fieldValueAsString(row.value)}
        readOnly={locked}
        loadKeywords={loadKeywords}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "community") {
    return (
      <CommunityFieldWidget
        name={row.name}
        value={row.value}
        readOnly={locked}
        loadCommunities={loadCommunities}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "longtext") {
    return (
      <textarea
        className={`${styles.textarea} ${locked ? styles.readonly : ""}`}
        data-testid={`editor-field-${row.name}`}
        data-editor-kind="longtext"
        name={row.name}
        value={row.value}
        readOnly={locked}
        onChange={(e) => onChange(row.name, e.target.value)}
      />
    );
  }
  return (
    <input
      className={`${styles.input} ${locked ? styles.readonly : ""}`}
      data-testid={`editor-field-${row.name}`}
      data-editor-kind="text"
      name={row.name}
      value={row.value}
      readOnly={locked}
      onChange={(e) => onChange(row.name, e.target.value)}
    />
  );
}

export function EditorHost({
  loadFields = fetchItemEditorFields,
  saveFields = saveItemEditorFields,
  checkout = checkoutEditorItem,
  checkin = checkinEditorItem,
  loadType = getContentTypeDetail,
  uploadBinary = uploadItemEditorBinary,
  loadKeywords,
  loadCommunities,
  loadBinaryMeta,
}: EditorHostProps = {}): React.ReactElement {
  const [params] = useSearchParams();
  const contentId = parsePositiveInt(params.get("contentId"));
  const mode: EditorHostMode = normalizeEditorMode(params.get("mode"));
  const readOnly = mode === "view";
  const promote = mode === "promote";

  const [payload, setPayload] = useState<ItemEditorFields | null>(null);
  const [schema, setSchema] = useState<ContentTypeFieldSummary[]>([]);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [pendingFiles, setPendingFiles] = useState<Record<string, File>>({});
  const [errorKey, setErrorKey] = useState<string | null>(
    contentId == null ? EDITOR_MSG.MISSING_ITEM : null,
  );
  const [errorDetail, setErrorDetail] = useState<string>("");
  const [loading, setLoading] = useState(contentId != null && !promote);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    document.title = message(EDITOR_MSG.TITLE);
  }, []);

  useEffect(() => {
    if (contentId == null || promote) {
      return;
    }
    const itemId = String(contentId);
    let cancelled = false;
    setLoading(true);
    setErrorKey(null);
    setErrorDetail("");
    void (async () => {
      try {
        if (!readOnly) {
          await checkout(itemId);
        }
        const fields = await loadFields(itemId);
        if (cancelled) {
          return;
        }
        setPayload(fields);
        setDraft(
          Object.fromEntries(
            fields.fields.map((f) => [f.name, fieldValueAsString(f.value)]),
          ),
        );
        if (fields.contentType) {
          try {
            const detail = await loadType(fields.contentType);
            if (!cancelled) {
              setSchema(detail.fields ?? []);
            }
          } catch {
            if (!cancelled) {
              setSchema([]);
            }
          }
        }
      } catch (err) {
        if (!cancelled) {
          setErrorDetail(err instanceof Error ? err.message : String(err));
          setErrorKey(EDITOR_MSG.LOAD_FAILED);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [contentId, readOnly, promote, checkout, loadFields, loadType]);

  const rows = useMemo(() => {
    if (!payload) {
      return [];
    }
    return mergeEditorRows(
      {
        ...payload,
        fields: payload.fields.map((f) => ({
          name: f.name,
          value: fieldValueAsString(draft[f.name] ?? f.value),
        })),
      },
      schema,
    ).map((row) => ({
      ...row,
      value: fieldValueAsString(draft[row.name] ?? row.value),
    }));
  }, [payload, draft, schema]);

  function setField(name: string, value: string): void {
    setDraft((prev) => ({ ...prev, [name]: value }));
  }

  function setFile(name: string, file: File | null): void {
    setPendingFiles((prev) => {
      const next = { ...prev };
      if (file) {
        next[name] = file;
      } else {
        delete next[name];
      }
      return next;
    });
  }

  async function handleSave(): Promise<void> {
    if (contentId == null || payload == null) {
      return;
    }
    setSaving(true);
    setSaved(false);
    setErrorKey(null);
    try {
      const itemId = String(contentId);
      const next: ItemEditorFields = {
        ...payload,
        fields: rows
          .filter((row) => row.kind !== "file" && row.kind !== "image")
          .map((row) => ({
            name: row.name,
            value: fieldValueAsString(draft[row.name] ?? row.value),
          })),
      };
      const savedPayload = await saveFields(itemId, next);
      for (const [field, file] of Object.entries(pendingFiles)) {
        await uploadBinary(itemId, field, file);
      }
      setPendingFiles({});
      setPayload(savedPayload);
      setDraft(
        Object.fromEntries(
          savedPayload.fields.map((f) => [f.name, fieldValueAsString(f.value)]),
        ),
      );
      setSaved(true);
    } catch {
      setErrorKey(EDITOR_MSG.SAVE_FAILED);
    } finally {
      setSaving(false);
    }
  }

  async function handleCheckin(): Promise<void> {
    if (contentId == null) {
      return;
    }
    try {
      await checkin(String(contentId));
      if (typeof window !== "undefined") {
        window.close();
      }
    } catch (err) {
      setErrorDetail(err instanceof Error ? err.message : String(err));
      setErrorKey(EDITOR_MSG.SAVE_FAILED);
    }
  }

  const canEdit = !readOnly && !promote;

  return (
    <div className={styles.root} data-testid="editor-host">
      <header className={styles.bar} data-testid="editor-overlay">
        <span className={styles.title}>{message(EDITOR_MSG.TITLE)}</span>
        <span className={styles.badge}>{message(badgeKey(mode))}</span>
        {contentId != null ? (
          <span className={styles.meta} data-testid="editor-content-id">
            {message(EDITOR_MSG.CONTENT_ID)} {contentId}
          </span>
        ) : null}
        {payload?.contentType ? (
          <span className={styles.meta} data-testid="editor-content-type">
            {message(EDITOR_MSG.TYPE_LABEL)} {payload.contentType}
          </span>
        ) : null}
        {payload?.checkoutUser ? (
          <span className={styles.meta} data-testid="editor-checkout-user">
            {message(EDITOR_MSG.CHECKOUT)} {payload.checkoutUser}
          </span>
        ) : null}
        <div className={styles.actions}>
          {saved ? <span className={styles.meta}>{message(EDITOR_MSG.SAVED)}</span> : null}
          {canEdit ? (
            <button
              type="button"
              className={`${styles.button} ${styles.buttonPrimary}`}
              data-testid="editor-save"
              disabled={saving || loading || payload == null}
              onClick={() => void handleSave()}
            >
              {message(saving ? EDITOR_MSG.SAVING : EDITOR_MSG.SAVE)}
            </button>
          ) : null}
          {canEdit ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-checkin"
              onClick={() => void handleCheckin()}
            >
              {message(EDITOR_MSG.CHECKIN)}
            </button>
          ) : null}
          <button
            type="button"
            className={styles.button}
            data-testid="editor-close"
            onClick={() => {
              if (typeof window !== "undefined") {
                window.close();
              }
            }}
          >
            {message(EDITOR_MSG.CLOSE)}
          </button>
        </div>
      </header>
      <div className={styles.stage} data-testid="editor-stage">
        {contentId != null && promote ? (
          <PromoteForm itemId={String(contentId)} />
        ) : errorKey ? (
          <div className={styles.status} role="alert" data-testid="editor-error">
            {message(errorKey)}
            {errorDetail ? ` ${errorDetail}` : ""}
          </div>
        ) : loading ? (
          <div className={styles.status} role="status" data-testid="editor-loading">
            {message(EDITOR_MSG.LOADING)}
          </div>
        ) : rows.length === 0 ? (
          <div className={styles.status} data-testid="editor-empty">
            {message(EDITOR_MSG.EMPTY)}
          </div>
        ) : (
          <form
            className={styles.form}
            data-testid="editor-form"
            onSubmit={(e) => {
              e.preventDefault();
              if (canEdit) {
                void handleSave();
              }
            }}
          >
            {rows.map((row) => (
              <label key={row.name} className={styles.field}>
                <span className={styles.label}>{row.label}</span>
                <EditorFieldControl
                  row={row}
                  itemId={String(contentId)}
                  locked={readOnly || row.readOnly}
                  onChange={setField}
                  onFile={setFile}
                  loadKeywords={loadKeywords}
                  loadCommunities={loadCommunities}
                  loadBinaryMeta={loadBinaryMeta}
                />
              </label>
            ))}
          </form>
        )}
      </div>
    </div>
  );
}
