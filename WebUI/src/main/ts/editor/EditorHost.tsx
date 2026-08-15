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
 * First Content Editor slice (995): checkout + content-type field form.
 * Rich controls / TinyMCE / New Item remain later slices.
 */

import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router";
import { getContentTypeDetail } from "../api/developer/contentTypesApi";
import type { ContentTypeFieldSummary } from "../api/developer/types";
import { parsePositiveInt } from "../assembly/assemblyHostUrl";
import { message } from "../i18n/message";
import {
  checkinEditorItem,
  checkoutEditorItem,
  fetchItemEditorFields,
  saveItemEditorFields,
  type ItemEditorField,
  type ItemEditorFields,
} from "./itemFieldsApi";
import styles from "./EditorHost.module.css";
import { normalizeEditorMode, type EditorHostMode } from "./editorHostUrl";
import { EDITOR_MSG } from "./messages";

export interface EditorHostProps {
  loadFields?: (itemId: string) => Promise<ItemEditorFields>;
  saveFields?: (
    itemId: string,
    payload: ItemEditorFields,
  ) => Promise<ItemEditorFields>;
  checkout?: (itemId: string) => Promise<void>;
  checkin?: (itemId: string) => Promise<void>;
  loadType?: (typeName: string) => Promise<{ fields?: ContentTypeFieldSummary[] }>;
}

function isLongField(schema: ContentTypeFieldSummary | undefined, value: string): boolean {
  const control = (schema?.control ?? "").toLowerCase();
  const dataType = (schema?.dataType ?? "").toLowerCase();
  if (control.includes("textarea") || control.includes("tinymce") || control.includes("html")) {
    return true;
  }
  if (dataType.includes("text") && value.length > 80) {
    return true;
  }
  return value.includes("\n") || value.length > 160;
}

export function mergeEditorRows(
  payload: ItemEditorFields,
  schemaFields: ContentTypeFieldSummary[],
): Array<ItemEditorField & { label: string; readOnly: boolean; long: boolean }> {
  const byName = new Map(schemaFields.map((f) => [f.name ?? "", f]));
  return payload.fields.map((field) => {
    const schema = byName.get(field.name);
    const readOnly = schema?.readOnly === true;
    return {
      ...field,
      label: schema?.label || field.name,
      readOnly,
      long: isLongField(schema, field.value),
    };
  });
}

export function EditorHost({
  loadFields = fetchItemEditorFields,
  saveFields = saveItemEditorFields,
  checkout = checkoutEditorItem,
  checkin = checkinEditorItem,
  loadType = getContentTypeDetail,
}: EditorHostProps = {}): React.ReactElement {
  const [params] = useSearchParams();
  const contentId = parsePositiveInt(params.get("contentId"));
  const mode: EditorHostMode = normalizeEditorMode(params.get("mode"));
  const readOnly = mode === "view";

  const [payload, setPayload] = useState<ItemEditorFields | null>(null);
  const [schema, setSchema] = useState<ContentTypeFieldSummary[]>([]);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [errorKey, setErrorKey] = useState<string | null>(
    contentId == null ? EDITOR_MSG.MISSING_ITEM : null,
  );
  const [errorDetail, setErrorDetail] = useState<string>("");
  const [loading, setLoading] = useState(contentId != null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    document.title = message(EDITOR_MSG.TITLE);
  }, []);

  useEffect(() => {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    let cancelled = false;
    setLoading(true);
    setErrorKey(null);
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
          Object.fromEntries(fields.fields.map((f) => [f.name, f.value])),
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
      } catch {
        if (!cancelled) {
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
  }, [contentId, readOnly, checkout, loadFields, loadType]);

  const rows = useMemo(() => {
    if (!payload) {
      return [];
    }
    return mergeEditorRows(
      {
        ...payload,
        fields: payload.fields.map((f) => ({
          name: f.name,
          value: draft[f.name] ?? f.value,
        })),
      },
      schema,
    );
  }, [payload, draft, schema]);

  async function handleSave(): Promise<void> {
    if (contentId == null || payload == null) {
      return;
    }
    setSaving(true);
    setSaved(false);
    setErrorKey(null);
    try {
      const next: ItemEditorFields = {
        ...payload,
        fields: payload.fields.map((f) => ({
          name: f.name,
          value: draft[f.name] ?? f.value,
        })),
      };
      const savedPayload = await saveFields(String(contentId), next);
      setPayload(savedPayload);
      setDraft(
        Object.fromEntries(savedPayload.fields.map((f) => [f.name, f.value])),
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

  return (
    <div className={styles.root} data-testid="editor-host">
      <header className={styles.bar} data-testid="editor-overlay">
        <span className={styles.title}>{message(EDITOR_MSG.TITLE)}</span>
        <span className={styles.badge}>
          {message(readOnly ? EDITOR_MSG.BADGE_VIEW : EDITOR_MSG.BADGE_EDIT)}
        </span>
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
          {!readOnly ? (
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
          {!readOnly ? (
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
        {errorKey ? (
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
              if (!readOnly) {
                void handleSave();
              }
            }}
          >
            {rows.map((row) => (
              <label key={row.name} className={styles.field}>
                <span className={styles.label}>{row.label}</span>
                {row.long ? (
                  <textarea
                    className={`${styles.textarea} ${readOnly || row.readOnly ? styles.readonly : ""}`}
                    data-testid={`editor-field-${row.name}`}
                    name={row.name}
                    value={row.value}
                    readOnly={readOnly || row.readOnly}
                    onChange={(e) =>
                      setDraft((prev) => ({ ...prev, [row.name]: e.target.value }))
                    }
                  />
                ) : (
                  <input
                    className={`${styles.input} ${readOnly || row.readOnly ? styles.readonly : ""}`}
                    data-testid={`editor-field-${row.name}`}
                    name={row.name}
                    value={row.value}
                    readOnly={readOnly || row.readOnly}
                    onChange={(e) =>
                      setDraft((prev) => ({ ...prev, [row.name]: e.target.value }))
                    }
                  />
                )}
              </label>
            ))}
          </form>
        )}
      </div>
    </div>
  );
}
