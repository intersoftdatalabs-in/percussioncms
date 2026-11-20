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
 * Page site-impact panel for the React Content Editor (#5739, parent
 * #5622 slice 6 batch 2).
 *
 * <p>Ports the retired classic {@code perc_page_edit_dialog} site-impact
 * chrome ({@code PercSiteImpactView.renderSiteImpact} with
 * {@code ITEM_TYPE_PAGE}): the pages and templates that reference this
 * page, loaded from the same
 * {@code GET item/siteimpact/page/{id}} endpoint. Read-only in every host
 * mode; empty results render an explicit empty state instead of a blank
 * panel. A load failure renders a named error alert with a retry control
 * ({@code editor-retry-site-impact}) that re-runs the load for the same
 * item id (#6771, parent #4532).</p>
 *
 * <p>Retrying unmounts the error alert (and the focused Retry button) while
 * the load runs, so keyboard and screen-reader users would lose their
 * place. After a retry settles, focus is returned to something meaningful:
 * the panel heading on success, or the Retry button again when the load
 * fails again — and the loading indicator is announced through a live
 * region (#6786).</p>
 */

import React, { useEffect, useRef, useState } from "react";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { message } from "../i18n/message";
import type { PageSiteImpact } from "./editorPageMetadata";
import { EDITOR_MSG } from "./messages";
import { fetchPageSiteImpact } from "./pageSiteImpactApi";
import styles from "./EditorHost.module.css";

export interface EditorSiteImpactPanelProps {
  itemId: string;
  loadSiteImpact?: (itemId: string) => Promise<PageSiteImpact>;
}

export function EditorSiteImpactPanel({
  itemId,
  loadSiteImpact = fetchPageSiteImpact,
}: EditorSiteImpactPanelProps): React.ReactElement {
  const [impact, setImpact] = useState<PageSiteImpact | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorKey, setErrorKey] = useState<string | null>(null);
  const [errorDetail, setErrorDetail] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [retryPending, setRetryPending] = useState(false);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const retryRef = useRef<HTMLButtonElement>(null);
  const loadingRef = useRef(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setErrorKey(null);
    setErrorDetail("");
    setImpact(null);
    void (async () => {
      try {
        const next = await loadSiteImpact(itemId);
        if (!cancelled) {
          setImpact(next);
        }
      } catch (err) {
        if (cancelled || isSessionRedirectError(err)) {
          return;
        }
        if (!cancelled) {
          setImpact(null);
          setErrorKey(EDITOR_MSG.SITE_IMPACT_FAILED);
          setErrorDetail(formatApiError(err, message(EDITOR_MSG.SITE_IMPACT_FAILED)));
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
  }, [itemId, loadSiteImpact, attempt]);

  /**
   * Return focus after a retry unmounts the control that had it (#6786).
   * Clicking Retry replaces the error alert (and the focused Retry button)
   * with the loading indicator, dropping focus to {@code <body>}. When the
   * retried load settles, keyboard and screen-reader users land on what
   * just changed: the panel heading on success, or the Retry button again
   * when the failure repeats. The flag is set only by the Retry click and
   * consumed here, so initial loads and item switches never steal focus.
   * The previous-loading guard skips the click commit itself (the outgoing
   * alert is still mounted there) and fires only on the settling
   * transition, when the incoming heading or Retry button exists.
   */
  useEffect(() => {
    const wasLoading = loadingRef.current;
    loadingRef.current = loading;
    if (loading || !wasLoading || !retryPending) {
      return;
    }
    setRetryPending(false);
    if (errorKey != null) {
      retryRef.current?.focus();
    } else {
      headingRef.current?.focus();
    }
  }, [loading, errorKey, retryPending]);

  const pages = impact?.pages ?? [];
  const templates = impact?.templates ?? [];

  return (
    <section className={styles.form} data-testid="editor-site-impact-panel">
      <h2
        ref={headingRef}
        tabIndex={-1}
        className={styles.label}
        data-testid="editor-site-impact-heading"
      >
        {message(EDITOR_MSG.SITE_IMPACT_TITLE)}
      </h2>
      {loading ? (
        <div
          className={styles.status}
          role="status"
          data-testid="editor-site-impact-loading"
        >
          {message(EDITOR_MSG.SITE_IMPACT_LOADING)}
        </div>
      ) : null}
      {errorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-site-impact-error"
        >
          {message(errorKey)}
          {errorDetail ? ` ${errorDetail}` : ""}
          <div className={styles.actions}>
            <button
              type="button"
              ref={retryRef}
              className={styles.button}
              data-testid="editor-retry-site-impact"
              onClick={() => {
                setRetryPending(true);
                setAttempt((value) => value + 1);
              }}
            >
              {message(EDITOR_MSG.LOAD_RETRY)}
            </button>
          </div>
        </div>
      ) : null}
      {!loading && errorKey == null && pages.length === 0 && templates.length === 0 ? (
        <div className={styles.status} data-testid="editor-site-impact-empty">
          {message(EDITOR_MSG.SITE_IMPACT_EMPTY)}
        </div>
      ) : null}
      {!loading && errorKey == null && pages.length > 0 ? (
        <div data-testid="editor-site-impact-pages">
          <h3 className={styles.label}>{message(EDITOR_MSG.SITE_IMPACT_PAGES)}</h3>
          <table className={styles.tableGrid} data-testid="editor-site-impact-pages-table">
            <thead>
              <tr>
                <th scope="col">{message(EDITOR_MSG.SITE_IMPACT_PAGE_NAME)}</th>
                <th scope="col">{message(EDITOR_MSG.SITE_IMPACT_SITE)}</th>
                <th scope="col">{message(EDITOR_MSG.SITE_IMPACT_STATUS)}</th>
              </tr>
            </thead>
            <tbody>
              {pages.map((page, index) => (
                <tr key={`${page.id || page.name}:${index}`} data-testid="editor-site-impact-page-row">
                  <td title={page.path || undefined}>{page.name}</td>
                  <td>{page.site}</td>
                  <td>{page.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {!loading && errorKey == null ? (
        <div data-testid="editor-site-impact-templates">
          <h3 className={styles.label}>
            {message(EDITOR_MSG.SITE_IMPACT_TEMPLATES)}
          </h3>
          {templates.length > 0 ? (
            <table
              className={styles.tableGrid}
              data-testid="editor-site-impact-templates-table"
            >
              <thead>
                <tr>
                  <th scope="col">
                    {message(EDITOR_MSG.SITE_IMPACT_TEMPLATE_NAME)}
                  </th>
                  <th scope="col">{message(EDITOR_MSG.SITE_IMPACT_SITE)}</th>
                </tr>
              </thead>
              <tbody>
                {templates.map((template, index) => (
                  <tr
                    key={`${template.name || template.site}:${index}`}
                    data-testid="editor-site-impact-template-row"
                  >
                    <td>{template.name}</td>
                    <td>{template.site}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div
              className={styles.status}
              data-testid="editor-site-impact-templates-empty"
            >
              {message(EDITOR_MSG.SITE_IMPACT_NO_TEMPLATES)}
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
}
