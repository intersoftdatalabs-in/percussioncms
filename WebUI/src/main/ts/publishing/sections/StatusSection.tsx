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

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { stopPublishing } from "../../api/publishing/serversApi";
import { fetchCurrentJobs } from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import { isJobStoppable, mapJobStopError } from "../jobStop";
import { formatProgressLabel } from "../progressUtils";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";
import { filterJobsBySite } from "../statusSiteFilter";
import {
  nextSortState,
  sortIndicator,
  sortJobs,
  type StatusSortKey,
  type StatusSortState,
} from "../statusSort";
import { ItemPublishingHistoryPanel } from "../components/ItemPublishingHistoryPanel";
import type { PublishSection, PublishingJob } from "../types";

/** Minuet-comparable default poll interval (ms). */
export const STATUS_POLL_INTERVAL_MS = 5000;

const DEFAULT_SORT: StatusSortState = {
  key: "siteName",
  direction: "asc",
};

export interface StatusSectionProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
  onOpenSection?: (section: PublishSection) => void;
}

export function StatusSection({
  itemId,
  onItemIdChange,
  onOpenSection,
}: StatusSectionProps = {}): React.ReactElement {
  const [jobs, setJobs] = useState<PublishingJob[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [siteFilter, setSiteFilter] = useState("");
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

  const visibleJobs = useMemo(
    () => sortJobs(filterJobsBySite(jobs, siteFilter), sort),
    [jobs, siteFilter, sort],
  );
  function onHeaderClick(key: StatusSortKey): void {
    setSort((prev) => nextSortState(prev, key));
  }

  async function onStop(job: PublishingJob): Promise<void> {
    if (!isJobStoppable(job)) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_STOP))) {
      return;
    }
    try {
      await stopPublishing(job.jobId as string | number);
      load();
    } catch (err) {
      setError(mapJobStopError(err));
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
      <ItemPublishingHistoryPanel
        itemId={itemId}
        currentSection="status"
        onItemIdChange={onItemIdChange}
        onOpenSection={onOpenSection}
      />
      <div style={toolbarStyle}>
        <label>
          <span className="sr-only">{message(MSG.PUBLISH_FILTER_SITES)}</span>
          <input
            type="search"
            data-testid="publish-status-site-filter"
            value={siteFilter}
            onChange={(e) => setSiteFilter(e.target.value)}
            placeholder={message(MSG.PUBLISH_FILTER_SITES)}
            aria-label={message(MSG.PUBLISH_FILTER_SITES)}
            style={{ padding: "6px 10px", minWidth: 200 }}
          />
        </label>
      </div>
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      {!loading && jobs.length === 0 && (
        <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_JOBS)}</p>
      )}
      {!loading && jobs.length > 0 && visibleJobs.length === 0 && (
        <p style={emptyStyle} data-testid="publish-status-empty-filter">
          No jobs match this site filter. Clear the filter to see the full list.
        </p>
      )}
      {visibleJobs.length > 0 && (
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
            {visibleJobs.map((job) => {
              const id = job.jobId ?? "";
              const stoppable = isJobStoppable(job);
              return (
                <tr key={String(id || job.siteName)}>
                  <td style={tdStyle}>{job.siteName ?? "—"}</td>
                  <td style={tdStyle}>{job.status ?? "—"}</td>
                  <td style={tdStyle}>
                    {formatProgressLabel(job.completedItems, job.totalItems)}
                  </td>
                  <td style={tdStyle}>
                    {stoppable ? (
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid={`publish-stop-job-${String(id)}`}
                        onClick={() => void onStop(job)}
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
