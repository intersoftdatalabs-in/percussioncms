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

import React from "react";
import { message, MSG } from "../../i18n/message";
import { buttonStyle, toolbarStyle } from "../publishing.styles";
import { statusJobDetail } from "../statusJobDetail";
import type { PublishingJob } from "../types";

export interface StatusJobDetailPanelProps {
  job: PublishingJob;
  onClose: () => void;
}

const labelStyle: React.CSSProperties = { fontWeight: 600, margin: 0 };
const valueStyle: React.CSSProperties = { margin: "0 0 8px" };

/**
 * Read-only detail for one Status job. Closing this panel does not stop the job.
 */
export function StatusJobDetailPanel({
  job,
  onClose,
}: StatusJobDetailPanelProps): React.ReactElement {
  const detail = statusJobDetail(job);
  return (
    <div data-testid="publish-status-job-detail" style={{ marginTop: 16 }}>
      <div style={toolbarStyle}>
        <h3 style={{ margin: 0, fontSize: "1rem" }}>Job details</h3>
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
        <dt style={labelStyle}>Job ID</dt>
        <dd style={valueStyle} data-testid="publish-status-detail-job-id">
          {detail.jobId}
        </dd>
        <dt style={labelStyle}>{message(MSG.PUBLISH_SECTION_SITES)}</dt>
        <dd style={valueStyle} data-testid="publish-status-detail-site">
          {detail.site}
        </dd>
        <dt style={labelStyle}>Edition</dt>
        <dd style={valueStyle} data-testid="publish-status-detail-edition">
          {detail.editionName}
        </dd>
        <dt style={labelStyle}>{message(MSG.PUBLISH_SECTION_STATUS)}</dt>
        <dd style={valueStyle} data-testid="publish-status-detail-status">
          {detail.status}
        </dd>
        {detail.showError && (
          <>
            <dt style={labelStyle}>Error</dt>
            <dd style={valueStyle} data-testid="publish-status-detail-error">
              {detail.errorText}
            </dd>
          </>
        )}
      </dl>
    </div>
  );
}
