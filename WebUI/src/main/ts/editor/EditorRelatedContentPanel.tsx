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
 * Browse-only related / inline related content list for EditorHost.
 */

import React, { useEffect, useState } from "react";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { fetchLocal } from "../api/contentExplorer/relationshipsApi";
import type { PSLocalDependencySummary } from "../api/contentExplorer/relationship";
import {
  fetchSlotCanvas,
  type SlotCanvas,
} from "../api/contentExplorer/slotRelationshipApi";
import { message } from "../i18n/message";
import {
  flattenRelatedContent,
  relatedContentErrorReason,
  type RelatedContentRow,
} from "./editorRelatedContent";
import styles from "./EditorHost.module.css";
import { EDITOR_MSG } from "./messages";

export interface EditorRelatedContentPanelProps {
  itemId: string;
  loadCanvas?: (ownerId: number) => Promise<SlotCanvas>;
  loadLocal?: (itemId: string) => Promise<PSLocalDependencySummary>;
}

export function EditorRelatedContentPanel({
  itemId,
  loadCanvas = fetchSlotCanvas,
  loadLocal = fetchLocal,
}: EditorRelatedContentPanelProps): React.ReactElement {
  const [rows, setRows] = useState<RelatedContentRow[]>([]);
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
          setErrorKey(EDITOR_MSG.RELATED_FORBIDDEN);
          return;
        }
        if (
          canvasSettled.status === "rejected" &&
          localSettled.status === "rejected"
        ) {
          const reason = relatedContentErrorReason(canvasSettled.reason);
          setRows([]);
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
  }, [itemId, loadCanvas, loadLocal]);

  return (
    <section className={styles.form} data-testid="editor-related-panel">
      <h2 className={styles.label}>{message(EDITOR_MSG.RELATED_TITLE)}</h2>
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
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
