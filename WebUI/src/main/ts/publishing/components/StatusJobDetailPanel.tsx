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
import { fetchLogDetails } from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import { reserveEditorWindow } from "../../editor/openEditorHost";
import {
  openLogItemInEditor,
  type OpenLogItemReason,
  type OpenLogItemResult,
} from "../openLogItemInEditor";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";
import { statusJobDetail } from "../statusJobDetail";
import {
  statusJobItemRows,
  statusJobItemsFailure,
  type StatusJobItemRow,
  type StatusJobItemsFailure,
} from "../statusJobItems";
import type { PublishingJob } from "../types";

export interface StatusJobDetailPanelProps {
  job: PublishingJob;
  onClose: () => void;
  /** Test seam. Production posts {@code /pubstatus/details}. */
  loadItems?: (jobId: string | number) => Promise<unknown>;
  /** Test seam. Production uses {@link openLogItemInEditor}. */
  openItem?: (
    contentId: string | number | null | undefined,
    reservedWindow?: Window | null,
  ) => Promise<OpenLogItemResult>;
}

const labelStyle: React.CSSProperties = { fontWeight: 600, margin: 0 };
const valueStyle: React.CSSProperties = { margin: "0 0 8px" };

function listFailureMessage(reason: StatusJobItemsFailure): string {
  if (reason === "forbidden") {
    return message(MSG.PUBLISH.STATUS_DETAIL.ITEMS_FORBIDDEN);
  }
  if (reason === "not_found") {
    return message(MSG.PUBLISH.STATUS_DETAIL.ITEMS_NOT_FOUND);
  }
  return message(MSG.PUBLISH.STATUS_DETAIL.ITEMS_FAILED);
}

function openFailureMessage(reason: OpenLogItemReason): string {
  if (reason === "forbidden") {
    return message(MSG.PUBLISH_LOG_ITEM_FORBIDDEN);
  }
  if (reason === "not_found") {
    return message(MSG.PUBLISH_LOG_ITEM_NOT_FOUND);
  }
  if (reason === "missing_id") {
    return message(MSG.PUBLISH_LOG_ITEM_NO_ID);
  }
  return message(MSG.PUBLISH_LOG_ITEM_OPEN_FAILED);
}

/**
 * Read-only detail for one Status job, including that job's content items.
 * Closing this panel does not stop the job. Item-list 403/404 stay here.
 */
export function StatusJobDetailPanel({
  job,
  onClose,
  loadItems = fetchLogDetails,
  openItem = (contentId, reservedWindow) =>
    openLogItemInEditor(contentId, { reservedWindow }),
}: StatusJobDetailPanelProps): React.ReactElement {
  const detail = statusJobDetail(job);
  const loadRef = useRef(loadItems);
  loadRef.current = loadItems;
  const [rows, setRows] = useState<StatusJobItemRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [openingId, setOpeningId] = useState<number | null>(null);
  const [openError, setOpenError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (detail.jobId === "") {
      setRows([]);
      setListError(null);
      setLoading(false);
      return () => {
        cancelled = true;
      };
    }
    setLoading(true);
    setListError(null);
    setOpenError(null);
    setRows([]);
    void loadRef.current(detail.jobId)
      .then((payload) => {
        if (!cancelled) {
          setRows(statusJobItemRows(payload));
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setRows([]);
          setListError(listFailureMessage(statusJobItemsFailure(err)));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [detail.jobId]);

  return (
    <div data-testid="publish-status-job-detail" style={{ marginTop: 16 }}>
      <div style={toolbarStyle}>
        <h3 style={{ margin: 0, fontSize: "1rem" }} data-testid="publish-status-detail-heading">
          {message(MSG.PUBLISH.STATUS_DETAIL.HEADING)}
        </h3>
        <button
          type="button"
          style={buttonStyle}
          data-testid="publish-status-job-detail-close"
          onClick={onClose}
        >
          {message(MSG.PUBLISH_BACK)}
        </button>
      </div>
      <dl style={{ fontSize: "0.9rem", margin: 0 }}>
        <dt style={labelStyle} data-testid="publish-status-detail-job-id-label">
          {message(MSG.PUBLISH.STATUS_DETAIL.JOB_ID)}
        </dt>
        <dd style={valueStyle} data-testid="publish-status-detail-job-id">
          {detail.jobId}
        </dd>
        <dt style={labelStyle}>{message(MSG.PUBLISH_SECTION_SITES)}</dt>
        <dd style={valueStyle} data-testid="publish-status-detail-site">
          {detail.site}
        </dd>
        <dt style={labelStyle} data-testid="publish-status-detail-edition-label">
          {message(MSG.PUBLISH.STATUS_DETAIL.EDITION)}
        </dt>
        <dd style={valueStyle} data-testid="publish-status-detail-edition">
          {detail.editionName}
        </dd>
        <dt style={labelStyle}>{message(MSG.PUBLISH_SECTION_STATUS)}</dt>
        <dd style={valueStyle} data-testid="publish-status-detail-status">
          {detail.status}
        </dd>
        {detail.showError && (
          <>
            <dt style={labelStyle} data-testid="publish-status-detail-error-label">
              {message(MSG.PUBLISH.STATUS_DETAIL.ERROR)}
            </dt>
            <dd style={valueStyle} data-testid="publish-status-detail-error">
              {detail.errorText}
            </dd>
          </>
        )}
      </dl>

      <h4 style={{ fontSize: "0.95rem", margin: "12px 0 8px" }}>
        {message(MSG.PUBLISH.STATUS_DETAIL.ITEMS_HEADING)}
      </h4>
      {loading && (
        <p data-testid="publish-status-items-loading">{message(MSG.PUBLISH_LOADING)}</p>
      )}
      {listError && (
        <p role="alert" style={errorStyle} data-testid="publish-status-items-error">
          {listError}
        </p>
      )}
      {!loading && !listError && rows.length === 0 && (
        <p style={emptyStyle} data-testid="publish-status-items-empty">
          {message(MSG.PUBLISH.STATUS_DETAIL.ITEMS_EMPTY)}
        </p>
      )}
      {!loading && !listError && rows.length > 0 && (
        <table style={tableStyle} data-testid="publish-status-items">
          <thead>
            <tr>
              <th style={thStyle}>{message(MSG.PUBLISH.LOGS_DETAILS.CONTENT_ID)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH.STATUS_DETAIL.ITEM_NAME)}</th>
              <th style={thStyle} />
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => {
              const key = row.openId != null ? String(row.openId) : `row-${idx}`;
              return (
                <tr key={key} data-testid={`publish-status-item-${key}`}>
                  <td style={tdStyle} data-testid={`publish-status-item-id-${key}`}>
                    {row.contentId !== "" ? row.contentId : "—"}
                  </td>
                  <td style={tdStyle} data-testid={`publish-status-item-name-${key}`}>
                    {row.title !== "" ? row.title : "—"}
                  </td>
                  <td style={tdStyle}>
                    {row.openId != null ? (
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid={`publish-status-open-item-${row.openId}`}
                        disabled={openingId != null}
                        onClick={() => {
                          const reserved = reserveEditorWindow();
                          setOpeningId(row.openId);
                          setOpenError(null);
                          void openItem(row.openId, reserved)
                            .then((result) => {
                              if (!result.ok) {
                                setOpenError(openFailureMessage(result.reason));
                              }
                            })
                            .catch(() => {
                              setOpenError(message(MSG.PUBLISH_LOG_ITEM_OPEN_FAILED));
                            })
                            .finally(() => {
                              setOpeningId(null);
                            });
                        }}
                      >
                        {message(MSG.PUBLISH_LOG_OPEN_EDITOR)}
                      </button>
                    ) : (
                      "—"
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
      {openError && (
        <p role="alert" style={errorStyle} data-testid="publish-status-item-open-error">
          {openError}
        </p>
      )}
    </div>
  );
}
