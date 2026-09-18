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
import {
  getItemWorkflowTransitions,
  transitionItem,
  type ItemStateTransition,
} from "../api/contentExplorer/itemWorkflowApi";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { parsePositiveInt } from "../assembly/assemblyHostUrl";
import { message } from "../i18n/message";
import { mergeEditorRows, type EditorFieldRow } from "./controlKinds";
import {
  collectInvalidDateFieldErrors,
  collectRequiredFieldErrors,
  mapSaveApiErrorToFieldErrors,
} from "./editorFieldErrors";
import { DateFieldWidget } from "./widgets/DateFieldWidget";
import {
  canPreviewFromEditor,
  editorDraftIsDirty,
  previewEditorItem,
} from "./editorPreview";
import {
  canPublishFromEditor,
  publishEditorItem,
  resolveEditorPublishKind,
  type EditorPublishKind,
} from "./editorPublish";
import {
  canRunEditorTransition,
  uniqueTransitionTriggers,
} from "./editorWorkflow";
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
import { EditorWorkflowPanel } from "./EditorWorkflowPanel";
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
  loadType?: (typeName: string) => Promise<{
    fields?: ContentTypeFieldSummary[];
    allowedTemplates?: unknown[];
  }>;
  uploadBinary?: (
    itemId: string,
    field: string,
    file: File,
  ) => Promise<unknown>;
  loadKeywords?: () => Promise<KeywordSummary[]>;
  loadCommunities?: () => Promise<CommunitySummary[]>;
  loadBinaryMeta?: (itemId: string, field: string) => Promise<ItemEditorBinaryMeta>;
  /** Test seam: allowed transitions ({@code getTransitions}). */
  loadTransitions?: (itemId: string) => Promise<ItemStateTransition>;
  /** Test seam: {@code transitionWithComments}. */
  runTransition?: (
    itemId: string,
    trigger: string,
    comment?: string,
  ) => Promise<unknown>;
  /** Extra / override names that require a comment (tests). */
  commentRequiredTriggers?: readonly string[];
  /** Test seam: sitemanage demand-publish ({@code publish/page|resource/{id}}). */
  publishItem?: (itemId: string, kind: EditorPublishKind) => Promise<boolean>;
  /** Test seam: confirm before Publish now (defaults to {@code window.confirm}). */
  confirmPublish?: (body: string) => boolean;
  /** Test seam: Explorer {@code openPreviewItem} wrapper. */
  previewItem?: (itemId: string, kind: EditorPublishKind) => Promise<void>;
  /** Test seam: confirm unsaved preview (defaults to {@code window.confirm}). */
  confirmUnsavedPreview?: (body: string) => boolean;
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
  invalid,
  onChange,
  onFile,
  loadKeywords,
  loadCommunities,
  loadBinaryMeta,
}: {
  row: EditorFieldRow;
  itemId: string;
  locked: boolean;
  invalid?: boolean;
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
  if (row.kind === "date" || row.kind === "datetime") {
    return (
      <DateFieldWidget
        name={row.name}
        value={fieldValueAsString(row.value)}
        kind={row.kind}
        readOnly={locked}
        invalid={invalid}
        required={row.required}
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
        aria-invalid={invalid ? true : undefined}
        aria-required={row.required ? true : undefined}
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
      aria-invalid={invalid ? true : undefined}
      aria-required={row.required ? true : undefined}
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
  loadTransitions = getItemWorkflowTransitions,
  runTransition = transitionItem,
  commentRequiredTriggers,
  publishItem = publishEditorItem,
  confirmPublish,
  previewItem = previewEditorItem,
  confirmUnsavedPreview,
}: EditorHostProps = {}): React.ReactElement {
  const [params] = useSearchParams();
  const contentId = parsePositiveInt(params.get("contentId"));
  const mode: EditorHostMode = normalizeEditorMode(params.get("mode"));
  const linkbackWarning = (params.get("warningMessage") ?? "").trim();
  const readOnly = mode === "view";
  const promote = mode === "promote";

  const [payload, setPayload] = useState<ItemEditorFields | null>(null);
  const [schema, setSchema] = useState<ContentTypeFieldSummary[]>([]);
  const [allowedTemplateCount, setAllowedTemplateCount] = useState(0);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [pendingFiles, setPendingFiles] = useState<Record<string, File>>({});
  const [errorKey, setErrorKey] = useState<string | null>(
    contentId == null ? EDITOR_MSG.MISSING_ITEM : null,
  );
  const [errorDetail, setErrorDetail] = useState<string>(
    contentId == null ? linkbackWarning : "",
  );
  const [loading, setLoading] = useState(contentId != null && !promote);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveErrorKey, setSaveErrorKey] = useState<string | null>(null);
  const [saveErrorDetail, setSaveErrorDetail] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [workflowTriggers, setWorkflowTriggers] = useState<string[]>([]);
  const [workflowState, setWorkflowState] = useState<string>("");
  const [workflowComment, setWorkflowComment] = useState("");
  const [workflowErrorKey, setWorkflowErrorKey] = useState<string | null>(null);
  const [workflowErrorDetail, setWorkflowErrorDetail] = useState("");
  const [workflowBusy, setWorkflowBusy] = useState(false);
  const [workflowDone, setWorkflowDone] = useState(false);
  const [publishBusy, setPublishBusy] = useState(false);
  const [publishDone, setPublishDone] = useState(false);
  const [publishErrorKey, setPublishErrorKey] = useState<string | null>(null);
  const [publishErrorDetail, setPublishErrorDetail] = useState("");
  const [previewBusy, setPreviewBusy] = useState(false);
  const [previewDone, setPreviewDone] = useState(false);
  const [previewErrorKey, setPreviewErrorKey] = useState<string | null>(null);
  const [previewErrorDetail, setPreviewErrorDetail] = useState("");

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
              setAllowedTemplateCount(detail.allowedTemplates?.length ?? 0);
            }
          } catch {
            if (!cancelled) {
              setSchema([]);
              setAllowedTemplateCount(0);
            }
          }
        }
        if (!readOnly) {
          try {
            const trans = await loadTransitions(itemId);
            if (!cancelled) {
              setWorkflowTriggers(
                uniqueTransitionTriggers(trans.transitionTriggers),
              );
              setWorkflowState(trans.stateName ?? "");
            }
          } catch {
            if (!cancelled) {
              setWorkflowTriggers([]);
              setWorkflowState("");
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
  }, [contentId, readOnly, promote, checkout, loadFields, loadType, loadTransitions]);

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
    setFieldErrors((prev) => {
      if (!(name in prev)) {
        return prev;
      }
      const next = { ...prev };
      delete next[name];
      return next;
    });
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

  function requiredErrorsForDraft(): Record<string, string> {
    return collectRequiredFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        required: row.required,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      pendingFiles,
      message(EDITOR_MSG.FIELD_REQUIRED),
    );
  }

  async function handleSave(): Promise<void> {
    if (contentId == null || payload == null) {
      return;
    }
    setSaving(true);
    setSaved(false);
    setSaveErrorKey(null);
    setSaveErrorDetail("");
    const missing = requiredErrorsForDraft();
    const invalidDates = collectInvalidDateFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        required: row.required,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      message(EDITOR_MSG.FIELD_INVALID_DATE),
    );
    if (Object.keys(missing).length > 0) {
      setFieldErrors({ ...invalidDates, ...missing });
      setSaveErrorKey(EDITOR_MSG.REQUIRED_SAVE);
      setSaving(false);
      return;
    }
    if (Object.keys(invalidDates).length > 0) {
      setFieldErrors(invalidDates);
      setSaveErrorKey(EDITOR_MSG.INVALID_DATE_SAVE);
      setSaving(false);
      return;
    }
    setFieldErrors({});
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
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const fallback = message(EDITOR_MSG.SAVE_FAILED);
      const mapped = mapSaveApiErrorToFieldErrors(
        err,
        rows.map((row) => row.name),
        fallback,
      );
      setFieldErrors(mapped.fieldErrors);
      setSaveErrorKey(EDITOR_MSG.SAVE_FAILED);
      setSaveErrorDetail(mapped.banner === fallback ? "" : mapped.banner);
    } finally {
      setSaving(false);
    }
  }

  function workflowErrorFor(reason: string): string {
    if (reason === "unauthorized") {
      return EDITOR_MSG.WORKFLOW_UNAUTHORIZED;
    }
    if (reason === "comment") {
      return EDITOR_MSG.WORKFLOW_COMMENT_REQUIRED;
    }
    return EDITOR_MSG.WORKFLOW_FAILED;
  }

  async function handleTransition(trigger: string): Promise<void> {
    if (contentId == null) {
      return;
    }
    const gate = canRunEditorTransition({
      mode,
      trigger,
      allowed: workflowTriggers,
      comment: workflowComment,
      commentRequiredTriggers,
    });
    if (!gate.ok) {
      setWorkflowDone(false);
      setWorkflowErrorDetail("");
      setWorkflowErrorKey(workflowErrorFor(gate.reason));
      return;
    }
    setWorkflowBusy(true);
    setWorkflowDone(false);
    setWorkflowErrorKey(null);
    setWorkflowErrorDetail("");
    const itemId = String(contentId);
    const comment = workflowComment.trim();
    try {
      await runTransition(itemId, trigger, comment.length > 0 ? comment : undefined);
      setWorkflowComment("");
      if (!readOnly) {
        try {
          await checkout(itemId);
        } catch {
          // Transition already succeeded; stay on the host without a second checkout.
        }
        const trans = await loadTransitions(itemId);
        setWorkflowTriggers(uniqueTransitionTriggers(trans.transitionTriggers));
        setWorkflowState(trans.stateName ?? "");
      }
      setWorkflowDone(true);
    } catch (err) {
      setWorkflowErrorDetail(err instanceof Error ? err.message : String(err));
      setWorkflowErrorKey(EDITOR_MSG.WORKFLOW_FAILED);
    } finally {
      setWorkflowBusy(false);
    }
  }

  async function handlePublish(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: itemId,
      allowedTemplateCount,
    });
    if (!canPublishFromEditor(mode, kind)) {
      setPublishDone(false);
      setPublishErrorDetail("");
      setPublishErrorKey(EDITOR_MSG.PUBLISH_UNAVAILABLE);
      return;
    }
    const confirmFn =
      confirmPublish ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    if (!confirmFn(message(EDITOR_MSG.CONFIRM_PUBLISH_NOW))) {
      return;
    }
    setPublishBusy(true);
    setPublishDone(false);
    setPublishErrorKey(null);
    setPublishErrorDetail("");
    try {
      const published = await publishItem(itemId, kind);
      if (!published) {
        setPublishErrorKey(EDITOR_MSG.PUBLISH_UNAVAILABLE);
        return;
      }
      setPublishDone(true);
    } catch (err) {
      setPublishErrorDetail(formatApiError(err, message(EDITOR_MSG.PUBLISH_FAILED)));
      setPublishErrorKey(EDITOR_MSG.PUBLISH_FAILED);
    } finally {
      setPublishBusy(false);
    }
  }

  async function handlePreview(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: itemId,
      allowedTemplateCount,
    });
    if (!canPreviewFromEditor(mode, kind)) {
      setPreviewDone(false);
      setPreviewErrorDetail("");
      setPreviewErrorKey(EDITOR_MSG.PREVIEW_UNAVAILABLE);
      return;
    }
    if (editorDraftIsDirty(payload?.fields, draft, pendingFiles)) {
      const confirmFn =
        confirmUnsavedPreview ??
        ((body: string) =>
          typeof window !== "undefined" ? window.confirm(body) : false);
      if (!confirmFn(message(EDITOR_MSG.CONFIRM_PREVIEW_UNSAVED))) {
        return;
      }
    }
    setPreviewBusy(true);
    setPreviewDone(false);
    setPreviewErrorKey(null);
    setPreviewErrorDetail("");
    try {
      await previewItem(itemId, kind);
      setPreviewDone(true);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setPreviewErrorDetail(formatApiError(err, message(EDITOR_MSG.PREVIEW_FAILED)));
      setPreviewErrorKey(EDITOR_MSG.PREVIEW_FAILED);
    } finally {
      setPreviewBusy(false);
    }
  }

  async function handleCheckin(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const missing = requiredErrorsForDraft();
    if (Object.keys(missing).length > 0) {
      setFieldErrors(missing);
      setSaveErrorKey(EDITOR_MSG.REQUIRED_CHECKIN);
      setSaveErrorDetail("");
      return;
    }
    try {
      await checkin(String(contentId));
      if (typeof window !== "undefined") {
        window.close();
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setSaveErrorDetail(formatApiError(err, message(EDITOR_MSG.SAVE_FAILED)));
      setSaveErrorKey(EDITOR_MSG.SAVE_FAILED);
    }
  }

  const canEdit = !readOnly && !promote;
  const publishKind = resolveEditorPublishKind(payload?.contentType, {
    id: contentId != null ? String(contentId) : "",
    allowedTemplateCount,
  });
  const showPublish = canPublishFromEditor(mode, publishKind);
  const showPreview = canPreviewFromEditor(mode, publishKind);

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
          {workflowDone ? (
            <span className={styles.meta} data-testid="editor-workflow-done">
              {message(EDITOR_MSG.WORKFLOW_DONE)}
            </span>
          ) : null}
          {publishDone ? (
            <span className={styles.meta} data-testid="editor-publish-done">
              {message(EDITOR_MSG.PUBLISH_DONE)}
            </span>
          ) : null}
          {previewDone ? (
            <span className={styles.meta} data-testid="editor-preview-done">
              {message(EDITOR_MSG.PREVIEW_DONE)}
            </span>
          ) : null}
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
          {showPublish ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-publish-now"
              disabled={publishBusy || loading || payload == null || saving}
              onClick={() => void handlePublish()}
            >
              {message(publishBusy ? EDITOR_MSG.PUBLISHING : EDITOR_MSG.PUBLISH_NOW)}
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
          {showPreview ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-preview"
              disabled={previewBusy || loading || payload == null}
              onClick={() => void handlePreview()}
            >
              {message(previewBusy ? EDITOR_MSG.PREVIEWING : EDITOR_MSG.PREVIEW)}
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
        ) : (
          <>
            {saveErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-save-error"
              >
                {message(saveErrorKey)}
                {saveErrorDetail ? ` ${saveErrorDetail}` : ""}
              </div>
            ) : null}
            {publishErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-publish-error"
              >
                {message(publishErrorKey)}
                {publishErrorDetail ? ` ${publishErrorDetail}` : ""}
              </div>
            ) : null}
            {previewErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-preview-error"
              >
                {message(previewErrorKey)}
                {previewErrorDetail ? ` ${previewErrorDetail}` : ""}
              </div>
            ) : null}
            {canEdit ? (
              <EditorWorkflowPanel
                stateName={workflowState}
                triggers={workflowTriggers}
                comment={workflowComment}
                onCommentChange={setWorkflowComment}
                onTransition={(t) => void handleTransition(t)}
                busy={workflowBusy || saving}
                errorKey={workflowErrorKey}
                errorDetail={workflowErrorDetail}
                commentRequiredTriggers={commentRequiredTriggers}
              />
            ) : null}
            {rows.length === 0 ? (
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
                  <label
                    key={row.name}
                    className={styles.field}
                    data-testid={`editor-field-row-${row.name}`}
                    data-required={row.required ? "true" : "false"}
                  >
                    <span className={styles.label}>
                      {row.label}
                      {row.required ? (
                        <span className={styles.requiredMark} aria-hidden="true">
                          {" "}
                          *
                        </span>
                      ) : null}
                    </span>
                    <EditorFieldControl
                      row={row}
                      itemId={String(contentId)}
                      locked={readOnly || row.readOnly}
                      invalid={Boolean(fieldErrors[row.name])}
                      onChange={setField}
                      onFile={setFile}
                      loadKeywords={loadKeywords}
                      loadCommunities={loadCommunities}
                      loadBinaryMeta={loadBinaryMeta}
                    />
                    {fieldErrors[row.name] ? (
                      <span
                        className={styles.fieldError}
                        role="alert"
                        data-testid={`editor-field-error-${row.name}`}
                      >
                        {fieldErrors[row.name]}
                      </span>
                    ) : null}
                  </label>
                ))}
              </form>
            )}
          </>
        )}
      </div>
    </div>
  );
}
