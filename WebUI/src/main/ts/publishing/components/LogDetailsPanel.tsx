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

import React, { useMemo, useState } from "react";
import { message, MSG } from "../../i18n/message";
import {
  extractLogItems,
  filterLogItems,
  type PublishLogItem,
} from "../logDetails";
import {
  buttonStyle,
  emptyStyle,
  formRowStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface LogDetailsPanelProps {
  details: unknown;
  jobSummary?: {
    jobId?: string | number;
    siteName?: string;
    serverName?: string;
    status?: string;
  };
  onClose: () => void;
}

/**
 * Structured publish log details (OPS-23) — item table instead of raw JSON only.
 */
export function LogDetailsPanel({
  details,
  jobSummary,
  onClose,
}: LogDetailsPanelProps): React.ReactElement {
  const items = useMemo(() => extractLogItems(details), [details]);
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState<PublishLogItem | null>(null);
  const filtered = useMemo(
    () => filterLogItems(items, filter),
    [items, filter],
  );

  return (
    <div data-testid="publish-log-details" style={{ marginTop: 16 }}>
      <div style={toolbarStyle}>
        <h3 style={{ margin: 0, fontSize: "1rem" }}>
          {message(MSG.PUBLISH_SECTION_LOGS)} details
        </h3>
        <button type="button" style={buttonStyle} onClick={onClose}>
          {message(MSG.PUBLISH_BACK)}
        </button>
      </div>

      {jobSummary && (
        <dl style={{ fontSize: "0.9rem", marginBottom: 12 }}>
          {jobSummary.jobId != null && (
            <>
              <dt style={{ fontWeight: 600 }}>Job ID</dt>
              <dd>{String(jobSummary.jobId)}</dd>
            </>
          )}
          {jobSummary.siteName && (
            <>
              <dt style={{ fontWeight: 600 }}>
                {message(MSG.PUBLISH_SECTION_SITES)}
              </dt>
              <dd>{jobSummary.siteName}</dd>
            </>
          )}
          {jobSummary.serverName && (
            <>
              <dt style={{ fontWeight: 600 }}>
                {message(MSG.PUBLISH_SELECT_SERVER)}
              </dt>
              <dd>{jobSummary.serverName}</dd>
            </>
          )}
          {jobSummary.status && (
            <>
              <dt style={{ fontWeight: 600 }}>
                {message(MSG.PUBLISH_SECTION_STATUS)}
              </dt>
              <dd>{jobSummary.status}</dd>
            </>
          )}
        </dl>
      )}

      <p style={{ fontSize: "0.9rem" }}>
        {items.length} publish item(s) attempted
      </p>

      {items.length === 0 ? (
        <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_LOGS)}</p>
      ) : (
        <>
          <div style={formRowStyle}>
            <label htmlFor="log-item-filter">Filter items</label>
            <input
              id="log-item-filter"
              data-testid="publish-log-item-filter"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="status, operation, location…"
            />
          </div>
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle} />
                <th style={thStyle}>{message(MSG.PUBLISH_SECTION_STATUS)}</th>
                <th style={thStyle}>Operation</th>
                <th style={thStyle}>Location</th>
                <th style={thStyle}>Elapsed</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((item, idx) => {
                const key = String(
                  item.itemStatusId ?? item.contentid ?? item.fileName ?? idx,
                );
                return (
                  <tr key={key}>
                    <td style={tdStyle}>
                      <button
                        type="button"
                        style={buttonStyle}
                        onClick={() => setSelected(item)}
                        aria-label={`item details ${key}`}
                      >
                        view
                      </button>
                    </td>
                    <td style={tdStyle}>{item.status ?? "—"}</td>
                    <td style={tdStyle}>{item.operation ?? "—"}</td>
                    <td style={tdStyle}>
                      {item.fileName ?? item.fileLocation ?? "—"}
                    </td>
                    <td style={tdStyle}>
                      {item.elapsedTime != null ? String(item.elapsedTime) : "—"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </>
      )}

      {selected && (
        <div
          data-testid="publish-log-item-detail"
          style={{
            marginTop: 12,
            padding: 12,
            border: "1px solid #ddd",
            borderRadius: 4,
            fontSize: "0.9rem",
          }}
        >
          <p>
            <strong>Content ID:</strong> {String(selected.contentid ?? "—")}
          </p>
          <p>
            <strong>Revision:</strong> {String(selected.revisionid ?? "—")}
          </p>
          <p>
            <strong>Template:</strong> {String(selected.templateid ?? "—")}
          </p>
          <p>
            <strong>Filename:</strong> {String(selected.fileName ?? "—")}
          </p>
          <p>
            <strong>Location:</strong> {String(selected.fileLocation ?? "—")}
          </p>
          <p>
            <strong>Operation:</strong> {String(selected.operation ?? "—")}
          </p>
          <p>
            <strong>Status:</strong> {String(selected.status ?? "—")}
          </p>
          <button
            type="button"
            style={buttonStyle}
            onClick={() => setSelected(null)}
          >
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      )}
    </div>
  );
}
