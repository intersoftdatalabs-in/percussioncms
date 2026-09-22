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
  fetchSlotCanvas,
  removeSlotRelationship,
  type SlotAddRequest,
  type SlotCanvas,
  type SlotRelationship,
} from "../api/contentExplorer/slotRelationshipApi";
import { message } from "../i18n/message";
import {
  flattenRelatedContent,
  insertSlotChoices,
  relatedContentErrorReason,
  relatedInsertErrorReason,
  relatedRemoveErrorReason,
  type InsertSlotChoice,
  type RelatedContentRow,
} from "./editorRelatedContent";
import styles from "./EditorHost.module.css";
import { EDITOR_MSG } from "./messages";

export interface EditorRelatedContentPanelProps {
  itemId: string;
  /** View / promote: list only. Insert and remove are edit actions. */
  readOnly?: boolean;
  loadCanvas?: (ownerId: number) => Promise<SlotCanvas>;
  loadLocal?: (itemId: string) => Promise<PSLocalDependencySummary>;
  insertRelationship?: (request: SlotAddRequest) => Promise<SlotRelationship>;
  removeRelationship?: (relationshipId: number) => Promise<void>;
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

function removeMessageKey(reason: ReturnType<typeof relatedRemoveErrorReason>): string {
  if (reason === "forbidden") {
    return EDITOR_MSG.RELATED_REMOVE_FORBIDDEN;
  }
  if (reason === "not_found") {
    return EDITOR_MSG.RELATED_REMOVE_NOT_FOUND;
  }
  return EDITOR_MSG.RELATED_REMOVE_FAILED;
}

export function EditorRelatedContentPanel({
  itemId,
  readOnly = false,
  loadCanvas = fetchSlotCanvas,
  loadLocal = fetchLocal,
  insertRelationship = addSlotRelationship,
  removeRelationship = removeSlotRelationship,
}: EditorRelatedContentPanelProps): React.ReactElement {
  const [rows, setRows] = useState<RelatedContentRow[]>([]);
  const [choices, setChoices] = useState<InsertSlotChoice[]>([]);
  const [slotId, setSlotId] = useState("");
  const [dependentId, setDependentId] = useState("");
  const [inserting, setInserting] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [insertErrorKey, setInsertErrorKey] = useState<string | null>(null);
  const [insertDetail, setInsertDetail] = useState("");
  const [removeErrorKey, setRemoveErrorKey] = useState<string | null>(null);
  const [removeDetail, setRemoveDetail] = useState("");
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
              {!readOnly && row.relationshipId != null && row.relationshipId > 0 ? (
                <button
                  type="button"
                  className={styles.button}
                  data-testid="editor-related-remove"
                  disabled={removingId != null}
                  onClick={() => void handleRemove(row.relationshipId as number)}
                >
                  {message(
                    removingId === row.relationshipId
                      ? EDITOR_MSG.RELATED_REMOVING
                      : EDITOR_MSG.RELATED_REMOVE,
                  )}
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
