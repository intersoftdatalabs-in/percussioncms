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

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { stopPublishing } from "../../api/publishing/serversApi";
import { fetchCurrentJobs } from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import { formatProgressLabel } from "../progressUtils";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  tableStyle,
  tdStyle,
  thStyle,
} from "../publishing.styles";
import {
  nextSortState,
  sortIndicator,
  sortJobs,
  type StatusSortKey,
  type StatusSortState,
} from "../statusSort";
import type { PublishingJob } from "../types";

/** Minuet-comparable default poll interval (ms). */
export const STATUS_POLL_INTERVAL_MS = 5000;

const DEFAULT_SORT: StatusSortState = {
  key: "siteName",
  direction: "asc",
};

export function StatusSection(): React.ReactElement {
  const [jobs, setJobs] = useState<PublishingJob[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [sort, setSort] = useState<StatusSortState>(DEFAULT_SORT);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const load = useCallback(() => {
    fetchCurrentJobs()
      .then((list) => {
        setJobs(list);
        setError(null);
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
    timerRef.current = setInterval(load, STATUS_POLL_INTERVAL_MS);
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [load]);

  const sortedJobs = useMemo(() => sortJobs(jobs, sort), [jobs, sort]);

  function onHeaderClick(key: StatusSortKey): void {
    setSort((prev) => nextSortState(prev, key));
  }

  async function onStop(jobId: string | number): Promise<void> {
    try {
      await stopPublishing(jobId);
      load();
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    }
  }

  function sortTh(
    key: StatusSortKey,
    label: string,
  ): React.ReactElement {
    return (
      <th style={thStyle}>
        <button
          type="button"
          style={{
            ...buttonStyle,
            border: "none",
            background: "transparent",
            padding: 0,
            fontWeight: 600,
            textAlign: "left",
          }}
          onClick={() => onHeaderClick(key)}
          aria-label={`Sort by ${label}`}
          data-testid={`status-sort-${key}`}
        >
          {label}
          {sortIndicator(sort, key)}
        </button>
      </th>
    );
  }

  return (
    <div data-testid="publish-section-status">
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      {!loading && jobs.length === 0 && (
        <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_JOBS)}</p>
      )}
      {jobs.length > 0 && (
        <table style={tableStyle}>
          <thead>
            <tr>
              {sortTh("siteName", message(MSG.PUBLISH_SECTION_SITES))}
              {sortTh("status", message(MSG.PUBLISH_SECTION_STATUS))}
              {sortTh("completedItems", message(MSG.PUBLISH_FULL))}
              <th style={thStyle}>{message(MSG.PUBLISH_STOP)}</th>
            </tr>
          </thead>
          <tbody>
            {sortedJobs.map((job) => {
              const id = job.jobId ?? "";
              const stoppable =
                !job.isStopping &&
                String(job.status ?? "")
                  .toLowerCase()
                  .includes("run");
              return (
                <tr key={String(id || job.siteName)}>
                  <td style={tdStyle}>{job.siteName ?? "—"}</td>
                  <td style={tdStyle}>{job.status ?? "—"}</td>
                  <td style={tdStyle}>
                    {formatProgressLabel(job.completedItems, job.totalItems)}
                  </td>
                  <td style={tdStyle}>
                    {stoppable && id !== "" ? (
                      <button
                        type="button"
                        style={buttonStyle}
                        onClick={() => void onStop(id)}
                      >
                        {message(MSG.PUBLISH_STOP)}
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
    </div>
  );
}
