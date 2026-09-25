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
 * Related / inline content list, insert of an existing item, and remove of a slot row.
 */

import React, { useEffect, useState } from "react";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { fetchLocal } from "../api/contentExplorer/relationshipsApi";
import type { PSLocalDependencySummary } from "../api/contentExplorer/relationship";
import {
  addSlotRelationship,
  changeSlotTemplateSlot,
  fetchSlotAllowedTemplates,
  fetchSlotCanvas,
  moveSlotRelationship,
  removeSlotRelationship,
  type SlotAddRequest,
  type SlotAllowedChoice,
  type SlotCanvas,
  type SlotRelationship,
} from "../api/contentExplorer/slotRelationshipApi";
import { parseExplorerContentId } from "../contentExplorer/menuCatalogLoad";
import { message } from "../i18n/message";
import {
  canChangeRelatedSnippetTemplate,
  flattenRelatedContent,
  insertSlotChoices,
  relatedContentErrorReason,
  relatedInsertErrorReason,
  relatedItemOpenMode,
  relatedRemoveErrorReason,
  relatedReorderEnds,
  relatedReorderErrorReason,
  relatedRowCanOpen,
  type InsertSlotChoice,
  type RelatedContentRow,
} from "./editorRelatedContent";
import type { EditorHostMode } from "./editorHostUrl";
import { saveRelatedSnippetTemplate } from "./editorRelatedTemplate";
import {
  closeReservedWindow,
  openEditorHost,
  reserveEditorWindow,
  type OpenEditorHostDeps,
} from "./openEditorHost";
import styles from "./EditorHost.module.css";
import { EDITOR_MSG } from "./messages";

export interface EditorRelatedContentPanelProps {
  itemId: string;
  /** View / promote: list only. Insert and remove are edit actions. */
  readOnly?: boolean;
  /** Current host mode. Open uses view for view and promote, otherwise edit. */
  hostMode?: EditorHostMode;
  loadCanvas?: (ownerId: number) => Promise<SlotCanvas>;
  loadLocal?: (itemId: string) => Promise<PSLocalDependencySummary>;
  insertRelationship?: (request: SlotAddRequest) => Promise<SlotRelationship>;
  removeRelationship?: (relationshipId: number) => Promise<void>;
  moveRelationship?: (
    relationshipId: number,
    direction: "UP" | "DOWN",
  ) => Promise<void>;
  /** POST template-slot. Keeps the current slot; only the snippet template changes. */
  changeSnippetTemplate?: (
    relationshipId: number,
    slotId: number,
    templateId: number,
  ) => Promise<SlotRelationship>;
  loadAllowedTemplates?: (
    slotId: number,
  ) => Promise<SlotAllowedChoice[]>;
  openRelatedItem?: (
    input: { id: number; mode: "edit" | "view" },
    deps: OpenEditorHostDeps,
  ) => Promise<boolean>;
  reserveRelatedWindow?: () => Window | null;
}

function insertMessageKey(reason: ReturnType<typeof relatedInsertErrorReason>): string {
  if (reason === "bad_request") {
    return EDITOR_MSG.RELATED_INSERT_BAD;
  }
  if (reason === "forbidden") {
    return EDITOR_MSG.RELATED_INSERT_FORBIDDEN;
  }
  if (reason === "not_found") {
    return EDITOR_MSG.RELATED_INSERT_NOT_FOUND;
  }
  return EDITOR_MSG.RELATED_INSERT_FAILED;
}

function reorderMessageKey(
  reason: ReturnType<typeof relatedReorderErrorReason>,
): string {
  if (reason === "forbidden") {
    return EDITOR_MSG.RELATED_MOVE_FORBIDDEN;
  }
  if (reason === "not_found") {
    return EDITOR_MSG.RELATED_MOVE_NOT_FOUND;
  }
  if (reason === "conflict") {
    return EDITOR_MSG.RELATED_MOVE_CONFLICT;
  }
  return EDITOR_MSG.RELATED_MOVE_FAILED;
}

function templateMessageKey(
  reason: ReturnType<typeof relatedInsertErrorReason>,
): string {
  if (reason === "bad_request") {
    return EDITOR_MSG.RELATED_TEMPLATE_BAD_REQUEST;
  }
  if (reason === "forbidden") {
    return EDITOR_MSG.RELATED_TEMPLATE_FORBIDDEN;
  }
  if (reason === "not_found") {
    return EDITOR_MSG.RELATED_TEMPLATE_NOT_FOUND;
  }
  return EDITOR_MSG.RELATED_TEMPLATE_FAILED;
}

function removeMessageKey(reason: ReturnType<typeof relatedRemoveErrorReason>): string {
  if (reason === "forbidden") {
    return EDITOR_MSG.RELATED_REMOVE_FORBIDDEN;
  }
  if (reason === "not_found") {
    return EDITOR_MSG.RELATED_REMOVE_NOT_FOUND;
  }
  return EDITOR_MSG.RELATED_REMOVE_FAILED;
}

function handleOpenRelated(
  rowItemId: string,
  hostMode: EditorHostMode,
  reserveRelatedWindow: () => Window | null,
  openRelatedItem: NonNullable<EditorRelatedContentPanelProps["openRelatedItem"]>,
  setOpenErrorKey: (key: string | null) => void,
): void {
  const contentId = parseExplorerContentId(rowItemId);
  if (contentId == null || !relatedRowCanOpen(rowItemId)) {
    return;
  }
  const reserved = reserveRelatedWindow();
  const mode = relatedItemOpenMode(hostMode);
  void openRelatedItem({ id: contentId, mode }, { reservedWindow: reserved }).then(
    (ok) => {
      if (!ok) {
        closeReservedWindow(reserved);
        setOpenErrorKey(EDITOR_MSG.RELATED_OPEN_FAILED);
        return;
      }
      setOpenErrorKey(null);
    },
  );
}

export function EditorRelatedContentPanel({
  itemId,
  readOnly = false,
  hostMode = "edit",
  loadCanvas = fetchSlotCanvas,
  loadLocal = fetchLocal,
  insertRelationship = addSlotRelationship,
  removeRelationship = removeSlotRelationship,
  moveRelationship = (relationshipId, direction) =>
    moveSlotRelationship(relationshipId, direction),
  changeSnippetTemplate = changeSlotTemplateSlot,
  loadAllowedTemplates = (slotId) => fetchSlotAllowedTemplates(slotId),
  openRelatedItem = openEditorHost,
  reserveRelatedWindow = reserveEditorWindow,
}: EditorRelatedContentPanelProps): React.ReactElement {
  const [rows, setRows] = useState<RelatedContentRow[]>([]);
  const [choices, setChoices] = useState<InsertSlotChoice[]>([]);
  const [slotId, setSlotId] = useState("");
  const [dependentId, setDependentId] = useState("");
  const [inserting, setInserting] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [movingId, setMovingId] = useState<number | null>(null);
  const [moveErrorKey, setMoveErrorKey] = useState<string | null>(null);
  const [moveDetail, setMoveDetail] = useState("");
  const [insertErrorKey, setInsertErrorKey] = useState<string | null>(null);
  const [insertDetail, setInsertDetail] = useState("");
  const [removeErrorKey, setRemoveErrorKey] = useState<string | null>(null);
  const [removeDetail, setRemoveDetail] = useState("");
  const [openErrorKey, setOpenErrorKey] = useState<string | null>(null);
  const [templateRow, setTemplateRow] = useState<RelatedContentRow | null>(null);
  const [templateChoices, setTemplateChoices] = useState<SlotAllowedChoice[]>([]);
  const [pickedTemplate, setPickedTemplate] = useState("");
  const [templateNotice, setTemplateNotice] = useState<string | null>(null);
  const [templateErrorKey, setTemplateErrorKey] = useState<string | null>(null);
  const [templateDetail, setTemplateDetail] = useState("");
  const [templateBusy, setTemplateBusy] = useState(false);
  const [templateSaved, setTemplateSaved] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const [loading, setLoading] = useState(true);
  const [errorKey, setErrorKey] = useState<string | null>(null);
  const [errorDetail, setErrorDetail] = useState("");

  useEffect(() => {
    let cancelled = false;
    const ownerId = Number(itemId);
    if (!Number.isFinite(ownerId) || ownerId <= 0) {
      setRows([]);
      setLoading(false);
      setErrorKey(EDITOR_MSG.RELATED_FAILED);
      return;
    }
    setLoading(true);
    setErrorKey(null);
    setErrorDetail("");
    setChoices([]);
    void (async () => {
      try {
        const [canvasSettled, localSettled] = await Promise.allSettled([
          loadCanvas(ownerId),
          loadLocal(itemId),
        ]);
        if (cancelled) {
          return;
        }
        const canvasForbidden =
          canvasSettled.status === "rejected" &&
          relatedContentErrorReason(canvasSettled.reason) === "forbidden";
        const localForbidden =
          localSettled.status === "rejected" &&
          relatedContentErrorReason(localSettled.reason) === "forbidden";
        if (canvasForbidden && localForbidden) {
          setRows([]);
          setChoices([]);
          setErrorKey(EDITOR_MSG.RELATED_FORBIDDEN);
          return;
        }
        if (
          canvasSettled.status === "rejected" &&
          localSettled.status === "rejected"
        ) {
          const reason = relatedContentErrorReason(canvasSettled.reason);
          setRows([]);
          setChoices([]);
          setErrorKey(
            reason === "forbidden"
              ? EDITOR_MSG.RELATED_FORBIDDEN
              : EDITOR_MSG.RELATED_FAILED,
          );
          setErrorDetail(
            formatApiError(
              canvasSettled.reason,
              message(EDITOR_MSG.RELATED_FAILED),
            ),
          );
          return;
        }
        const canvas =
          canvasSettled.status === "fulfilled" ? canvasSettled.value : null;
        const local =
          localSettled.status === "fulfilled" ? localSettled.value : null;
        const nextChoices = insertSlotChoices(canvas);
        setChoices(nextChoices);
        setSlotId((current) => {
          if (nextChoices.some((c) => String(c.slotId) === current)) {
            return current;
          }
          return nextChoices.length > 0 ? String(nextChoices[0].slotId) : "";
        });
        setRows(flattenRelatedContent(canvas, local));
        if (canvasForbidden || localForbidden) {
          setErrorKey(EDITOR_MSG.RELATED_FORBIDDEN);
        }
      } catch (err) {
        if (cancelled || isSessionRedirectError(err)) {
          return;
        }
        const reason = relatedContentErrorReason(err);
        setRows([]);
        setChoices([]);
        setErrorKey(
          reason === "forbidden"
            ? EDITOR_MSG.RELATED_FORBIDDEN
            : EDITOR_MSG.RELATED_FAILED,
        );
        setErrorDetail(formatApiError(err, message(EDITOR_MSG.RELATED_FAILED)));
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [itemId, loadCanvas, loadLocal, reloadToken]);

  const selected = choices.find((c) => String(c.slotId) === slotId) ?? null;

  async function handleInsert(): Promise<void> {
    const ownerId = Number(itemId);
    const dependent = Number(dependentId.trim());
    if (
      !selected ||
      selected.templateId <= 0 ||
      !Number.isFinite(ownerId) ||
      ownerId <= 0 ||
      !Number.isFinite(dependent) ||
      dependent <= 0
    ) {
      setInsertErrorKey(EDITOR_MSG.RELATED_INSERT_BAD);
      setInsertDetail("");
      return;
    }
    setInserting(true);
    setInsertErrorKey(null);
    setInsertDetail("");
    try {
      await insertRelationship({
        ownerId,
        dependentId: dependent,
        slotId: selected.slotId,
        templateId: selected.templateId,
      });
      setDependentId("");
      setReloadToken((n) => n + 1);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = relatedInsertErrorReason(err);
      setInsertErrorKey(insertMessageKey(reason));
      setInsertDetail(formatApiError(err, message(EDITOR_MSG.RELATED_INSERT_FAILED)));
    } finally {
      setInserting(false);
    }
  }

  async function handleMove(
    relationshipId: number,
    direction: "UP" | "DOWN",
  ): Promise<void> {
    if (!(relationshipId > 0) || movingId != null || removingId != null) {
      return;
    }
    setMovingId(relationshipId);
    setMoveErrorKey(null);
    setMoveDetail("");
    try {
      await moveRelationship(relationshipId, direction);
      setReloadToken((n) => n + 1);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = relatedReorderErrorReason(err);
      setMoveErrorKey(reorderMessageKey(reason));
      setMoveDetail(formatApiError(err, message(EDITOR_MSG.RELATED_MOVE_FAILED)));
    } finally {
      setMovingId(null);
    }
  }

  async function openTemplateDialog(row: RelatedContentRow): Promise<void> {
    if (!canChangeRelatedSnippetTemplate(row) || readOnly) {
      return;
    }
    setTemplateRow(row);
    setTemplateNotice(null);
    setTemplateErrorKey(null);
    setTemplateDetail("");
    setTemplateSaved(false);
    setPickedTemplate(row.templateId != null && row.templateId > 0 ? String(row.templateId) : "");
    setTemplateChoices([]);
    try {
      const choicesForSlot = await loadAllowedTemplates(Number(row.slotId));
      setTemplateChoices(choicesForSlot);
      setPickedTemplate((current) => {
        if (current && choicesForSlot.some((c) => String(c.id) === current)) {
          return current;
        }
        return choicesForSlot[0] ? String(choicesForSlot[0].id) : "";
      });
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setTemplateNotice(message(EDITOR_MSG.RELATED_TEMPLATE_FAILED));
    }
  }

  function cancelTemplateDialog(): void {
    setTemplateRow(null);
    setTemplateChoices([]);
    setPickedTemplate("");
    setTemplateNotice(null);
  }

  async function applyTemplateDialog(): Promise<void> {
    if (templateRow == null || templateBusy) {
      return;
    }
    setTemplateBusy(true);
    setTemplateErrorKey(null);
    setTemplateDetail("");
    setTemplateSaved(false);
    const result = await saveRelatedSnippetTemplate({
      row: templateRow,
      templateId: Number(pickedTemplate),
      change: changeSnippetTemplate,
    });
    setTemplateBusy(false);
    if (!result.ok) {
      if (result.reason === "needs_template" || result.reason === "not_slot") {
        setTemplateNotice(message(EDITOR_MSG.RELATED_TEMPLATE_NEEDS));
        return;
      }
      setTemplateErrorKey(templateMessageKey(result.reason));
      setTemplateDetail("");
      return;
    }
    setTemplateSaved(true);
    setTemplateRow(null);
    setReloadToken((n) => n + 1);
  }

  async function handleRemove(relationshipId: number): Promise<void> {
    if (!(relationshipId > 0)) {
      return;
    }
    setRemovingId(relationshipId);
    setRemoveErrorKey(null);
    setRemoveDetail("");
    try {
      await removeRelationship(relationshipId);
      setReloadToken((n) => n + 1);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = relatedRemoveErrorReason(err);
      setRemoveErrorKey(removeMessageKey(reason));
      setRemoveDetail(formatApiError(err, message(EDITOR_MSG.RELATED_REMOVE_FAILED)));
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <section className={styles.form} data-testid="editor-related-panel">
      <h2 className={styles.label}>{message(EDITOR_MSG.RELATED_TITLE)}</h2>
      {!readOnly && !loading && choices.length > 0 ? (
        <div className={styles.form} data-testid="editor-related-insert">
          <label className={styles.field}>
            {message(EDITOR_MSG.RELATED_SLOT)}
            <select
              className={styles.input}
              data-testid="editor-related-slot"
              value={slotId}
              onChange={(e) => setSlotId(e.target.value)}
            >
              {choices.map((choice) => (
                <option key={choice.slotId} value={String(choice.slotId)}>
                  {choice.label}
                </option>
              ))}
            </select>
          </label>
          <label className={styles.field}>
            {message(EDITOR_MSG.RELATED_ITEM_ID)}
            <input
              className={styles.input}
              data-testid="editor-related-item"
              value={dependentId}
              inputMode="numeric"
              onChange={(e) => setDependentId(e.target.value)}
            />
          </label>
          <div className={styles.actions}>
            <button
              type="button"
              className={`${styles.button} ${styles.buttonPrimary}`}
              data-testid="editor-related-insert-submit"
              disabled={inserting}
              onClick={() => void handleInsert()}
            >
              {message(
                inserting ? EDITOR_MSG.RELATED_INSERTING : EDITOR_MSG.RELATED_INSERT,
              )}
            </button>
          </div>
          {insertErrorKey ? (
            <div
              className={styles.status}
              role="alert"
              data-testid="editor-related-insert-error"
            >
              {message(insertErrorKey)}
              {insertDetail ? ` ${insertDetail}` : ""}
            </div>
          ) : null}
        </div>
      ) : null}
      {loading ? (
        <div className={styles.status} data-testid="editor-related-loading">
          {message(EDITOR_MSG.RELATED_LOADING)}
        </div>
      ) : null}
      {errorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-related-error"
        >
          {message(errorKey)}
          {errorDetail ? ` ${errorDetail}` : ""}
        </div>
      ) : null}
      {!loading && rows.length === 0 && errorKey == null ? (
        <div className={styles.status} data-testid="editor-related-empty">
          {message(EDITOR_MSG.RELATED_EMPTY)}
        </div>
      ) : null}
      {moveErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-related-move-error"
        >
          {message(moveErrorKey)}
          {moveDetail ? ` ${moveDetail}` : ""}
        </div>
      ) : null}
      {removeErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-related-remove-error"
        >
          {message(removeErrorKey)}
          {removeDetail ? ` ${removeDetail}` : ""}
        </div>
      ) : null}
      {templateErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-related-template-error"
        >
          {message(templateErrorKey)}
          {templateDetail ? ` ${templateDetail}` : ""}
        </div>
      ) : null}
      {templateSaved ? (
        <div className={styles.status} data-testid="editor-related-template-saved">
          {message(EDITOR_MSG.RELATED_TEMPLATE_SAVED)}
        </div>
      ) : null}
      {openErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-related-open-error"
        >
          {message(openErrorKey)}
        </div>
      ) : null}
      {rows.length > 0 ? (
        <ul className={styles.relatedList} data-testid="editor-related-list">
          {rows.map((row) => (
            <li
              key={row.key}
              className={styles.relatedItem}
              data-testid="editor-related-row"
              data-related-kind={row.kind}
            >
              <span className={styles.relatedSlot}>{row.slotLabel}</span>
              <span data-testid="editor-related-item-id">{row.itemId}</span>
              {relatedRowCanOpen(row.itemId) ? (
                <button
                  type="button"
                  className={styles.button}
                  data-testid="editor-related-open"
                  onClick={() =>
                    handleOpenRelated(
                      row.itemId,
                      hostMode,
                      reserveRelatedWindow,
                      openRelatedItem,
                      setOpenErrorKey,
                    )
                  }
                >
                  {message(EDITOR_MSG.RELATED_OPEN)}
                </button>
              ) : null}
              {!readOnly && canChangeRelatedSnippetTemplate(row) ? (
                <button
                  type="button"
                  className={styles.button}
                  data-testid="editor-related-change-template"
                  disabled={templateBusy || removingId != null || movingId != null}
                  onClick={() => void openTemplateDialog(row)}
                >
                  {message(EDITOR_MSG.RELATED_CHANGE_TEMPLATE)}
                </button>
              ) : null}
              {!readOnly && row.relationshipId != null && row.relationshipId > 0 ? (
                <>
                {relatedReorderEnds(rows, row).up ? (
                  <button
                    type="button"
                    className={styles.button}
                    data-testid="editor-related-move-up"
                    disabled={movingId != null || removingId != null}
                    onClick={() =>
                      void handleMove(row.relationshipId as number, "UP")
                    }
                  >
                    {message(
                      movingId === row.relationshipId
                        ? EDITOR_MSG.RELATED_MOVING
                        : EDITOR_MSG.RELATED_MOVE_UP,
                    )}
                  </button>
                ) : null}
                {relatedReorderEnds(rows, row).down ? (
                  <button
                    type="button"
                    className={styles.button}
                    data-testid="editor-related-move-down"
                    disabled={movingId != null || removingId != null}
                    onClick={() =>
                      void handleMove(row.relationshipId as number, "DOWN")
                    }
                  >
                    {message(
                      movingId === row.relationshipId
                        ? EDITOR_MSG.RELATED_MOVING
                        : EDITOR_MSG.RELATED_MOVE_DOWN,
                    )}
                  </button>
                ) : null}
                <button
                  type="button"
                  className={styles.button}
                  data-testid="editor-related-remove"
                  disabled={removingId != null || movingId != null}
                  onClick={() => void handleRemove(row.relationshipId as number)}
                >
                  {message(
                    removingId === row.relationshipId
                      ? EDITOR_MSG.RELATED_REMOVING
                      : EDITOR_MSG.RELATED_REMOVE,
                  )}
                </button>
                </>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
      {templateRow ? (
        <div
          className={styles.form}
          role="dialog"
          aria-modal="true"
          data-testid="editor-related-template-dialog"
        >
          <h3 className={styles.label}>{message(EDITOR_MSG.RELATED_CHANGE_TITLE)}</h3>
          {templateNotice ? (
            <div role="alert" data-testid="editor-related-template-notice">
              {templateNotice}
            </div>
          ) : null}
          <label className={styles.field}>
            {message(EDITOR_MSG.RELATED_TEMPLATE_LABEL)}
            <select
              className={styles.input}
              data-testid="editor-related-template-select"
              value={pickedTemplate}
              onChange={(e) => setPickedTemplate(e.target.value)}
            >
              {templateChoices.map((choice) => (
                <option key={choice.id} value={String(choice.id)}>
                  {choice.label || choice.name || choice.id}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className={styles.button}
            data-testid="editor-related-template-cancel"
            onClick={cancelTemplateDialog}
          >
            {message(EDITOR_MSG.RELATED_TEMPLATE_CANCEL)}
          </button>
          <button
            type="button"
            className={styles.buttonPrimary}
            data-testid="editor-related-template-apply"
            disabled={templateBusy}
            onClick={() => void applyTemplateDialog()}
          >
            {message(EDITOR_MSG.RELATED_TEMPLATE_APPLY)}
          </button>
        </div>
      ) : null}
    </section>
  );
}
