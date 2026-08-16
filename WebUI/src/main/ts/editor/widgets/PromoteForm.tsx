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

import React, { useEffect, useState } from "react";
import { formatApiError } from "../../api/client";
import {
  fetchItemRevisions,
  restoreItemRevision,
  type ItemRevision,
  type ItemRevisionsSummary,
} from "../../api/contentExplorer/itemRevisionsApi";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";

export interface PromoteFormProps {
  itemId: string;
  loadRevisions?: (itemId: string) => Promise<ItemRevisionsSummary>;
  promoteRevision?: (itemId: string, revId: number) => Promise<void>;
}

export function PromoteForm({
  itemId,
  loadRevisions = fetchItemRevisions,
  promoteRevision = restoreItemRevision,
}: PromoteFormProps): React.ReactElement {
  const [revisions, setRevisions] = useState<ItemRevision[]>([]);
  const [restorable, setRestorable] = useState(false);
  const [selected, setSelected] = useState<number | "">("");
  const [error, setError] = useState<string | null>(null);
  const [working, setWorking] = useState(false);
  const [done, setDone] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void loadRevisions(itemId)
      .then((summary) => {
        if (cancelled) {
          return;
        }
        setRevisions(summary.revisions);
        setRestorable(summary.restorable);
        if (summary.revisions.length > 0) {
          setSelected(summary.revisions[0].revId);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(formatApiError(err, message(EDITOR_MSG.PROMOTE_FAILED)));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [itemId, loadRevisions]);

  async function handlePromote(): Promise<void> {
    if (selected === "" || !restorable) {
      return;
    }
    setWorking(true);
    setError(null);
    setDone(false);
    try {
      await promoteRevision(itemId, Number(selected));
      setDone(true);
    } catch (err) {
      setError(formatApiError(err, message(EDITOR_MSG.PROMOTE_FAILED)));
    } finally {
      setWorking(false);
    }
  }

  return (
    <form
      className={styles.form}
      data-testid="editor-promote-form"
      onSubmit={(e) => {
        e.preventDefault();
        void handlePromote();
      }}
    >
      <p className={styles.hint}>{message(EDITOR_MSG.PROMOTE_HINT)}</p>
      {error ? (
        <div className={styles.status} role="alert" data-testid="editor-promote-error">
          {error}
        </div>
      ) : null}
      {done ? (
        <div className={styles.status} role="status" data-testid="editor-promote-ok">
          {message(EDITOR_MSG.PROMOTED)}
        </div>
      ) : null}
      {revisions.length === 0 ? (
        <div className={styles.status} data-testid="editor-promote-empty">
          {message(EDITOR_MSG.PROMOTE_NONE)}
        </div>
      ) : (
        <label className={styles.field}>
          <span className={styles.label}>{message(EDITOR_MSG.PROMOTE_REVISION)}</span>
          <select
            className={styles.input}
            data-testid="editor-promote-select"
            value={selected}
            onChange={(e) => setSelected(e.target.value ? Number(e.target.value) : "")}
          >
            {revisions.map((rev) => (
              <option key={rev.revId} value={rev.revId}>
                {rev.revId} — {rev.status} — {rev.lastModifier}
              </option>
            ))}
          </select>
        </label>
      )}
      <div className={styles.actions}>
        <button
          type="submit"
          className={`${styles.button} ${styles.buttonPrimary}`}
          data-testid="editor-promote"
          disabled={working || selected === "" || !restorable}
        >
          {message(EDITOR_MSG.PROMOTE)}
        </button>
      </div>
    </form>
  );
}
