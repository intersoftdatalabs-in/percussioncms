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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useRef, useState } from "react";
import { get } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";

export interface WorkflowSiteAssignProps {
  workflowName: string;
  onClose: () => void;
}

interface SiteItem {
  id: string;
  name: string;
  folderPath: string;
}

type JobState = "idle" | "inProgress" | "complete" | "failed";

export const WorkflowSiteAssign: React.FC<WorkflowSiteAssignProps> = ({
  workflowName,
  onClose,
}) => {
  const [sites, setSites] = useState<SiteItem[]>([]);
  const [selectedPath, setSelectedPath] = useState<string>("");
  const [customPath, setCustomPath] = useState<string>("");
  const [jobState, setJobState] = useState<JobState>("idle");
  const [jobStatusMsg, setJobStatusMsg] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;
    const fetchSites = async () => {
      try {
        const result = await get<SiteItem[]>(PATHS.SITES_ALL);
        if (isMountedRef.current) setSites(result || []);
      } catch {
        if (isMountedRef.current) setSites([]);
      } finally {
        if (isMountedRef.current) setIsLoading(false);
      }
    };
    fetchSites();

    return () => {
      isMountedRef.current = false;
      if (pollTimerRef.current) {
        clearTimeout(pollTimerRef.current);
      }
    };
  }, []);

  const pollJobStatus = async () => {
    if (!isMountedRef.current) return;
    try {
      const status = await get<{ isInProgress: boolean }>(PATHS.FOLDER_ASSIGNMENT_JOB_STATUS);
      if (!isMountedRef.current) return;

      if (!status.isInProgress) {
        setJobState("complete");
        setJobStatusMsg(message(WF_ADMIN_MSG.JOB_COMPLETE));
      } else {
        // Schedule next poll tick only after current request completes
        pollTimerRef.current = setTimeout(pollJobStatus, 1000);
      }
    } catch {
      if (isMountedRef.current) {
        setJobState("failed");
        setJobStatusMsg(message(WF_ADMIN_MSG.JOB_FAILED));
      }
    }
  };

  const handleStartAssignment = async () => {
    const targetPath = customPath.trim() || selectedPath;
    if (!targetPath) return;

    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
    }

    setJobState("inProgress");
    setJobStatusMsg(message(WF_ADMIN_MSG.JOB_IN_PROGRESS));

    try {
      const encodedPath = encodeURIComponent(targetPath.replace(/^\//, ""));
      // Legacy Percussion CMS PSFolderRestService contract defines job start endpoint as GET
      await get(`${PATHS.FOLDER_ASSIGNMENT_JOB_START}${encodeURIComponent(workflowName)}/${encodedPath}`);
      if (!isMountedRef.current) return;

      // Start recursive polling with setTimeout to avoid overlapping requests
      pollTimerRef.current = setTimeout(pollJobStatus, 1000);
    } catch {
      if (isMountedRef.current) {
        setJobState("failed");
        setJobStatusMsg(message(WF_ADMIN_MSG.JOB_FAILED));
      }
    }
  };

  const getStatusBgColor = () => {
    switch (jobState) {
      case "inProgress":
        return "#e8f4f8";
      case "failed":
        return "#fdf7f7";
      case "complete":
        return "#eaf6ea";
      default:
        return "transparent";
    }
  };

  const getStatusTextColor = () => {
    switch (jobState) {
      case "inProgress":
        return "#007ea8";
      case "failed":
        return "#d9534f";
      case "complete":
        return "#2e7d32";
      default:
        return "inherit";
    }
  };

  return (
    <div
      className="perc-dialog-backdrop"
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: "rgba(0,0,0,0.4)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
      }}
      data-testid="perc-workflow-site-assign"
    >
      <div style={{ background: "#fff", padding: "24px", borderRadius: "8px", width: "500px", maxWidth: "90%" }}>
        <h3>{message(WF_ADMIN_MSG.ASSIGN_TITLE)}</h3>
        <p style={{ color: "#666", fontSize: "14px", marginBottom: "16px" }}>
          {message(WF_ADMIN_MSG.SELECT_SITE_OR_FOLDER)}
        </p>

        {isLoading ? (
          <div>{message(WF_ADMIN_MSG.LOADING)}</div>
        ) : (
          <div style={{ marginBottom: "16px" }}>
            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>Site</label>
              <select
                value={selectedPath}
                disabled={jobState === "inProgress"}
                onChange={(e) => {
                  setSelectedPath(e.target.value);
                  setCustomPath("");
                }}
                style={{ width: "100%", padding: "8px" }}
                data-testid="site-select"
              >
                <option value="">-- Select a Site --</option>
                {sites.map((s) => (
                  <option key={s.id || s.name} value={s.folderPath || `//Sites/${s.name}`}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
                Or Subfolder Path
              </label>
              <input
                type="text"
                value={customPath}
                disabled={jobState === "inProgress"}
                placeholder="//Sites/MySite/folder"
                onChange={(e) => {
                  setCustomPath(e.target.value);
                  setSelectedPath("");
                }}
                style={{ width: "100%", padding: "8px" }}
                data-testid="custom-path-input"
              />
            </div>
          </div>
        )}

        {jobStatusMsg && (
          <div
            style={{
              padding: "10px",
              background: getStatusBgColor(),
              color: getStatusTextColor(),
              marginBottom: "16px",
              borderRadius: "4px",
            }}
            data-testid="job-status-msg"
          >
            {jobStatusMsg}
          </div>
        )}

        <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
          <button type="button" onClick={onClose} disabled={jobState === "inProgress"}>
            {message(WF_ADMIN_MSG.CANCEL)}
          </button>
          <button
            type="button"
            className="perc-button-primary"
            onClick={handleStartAssignment}
            disabled={jobState === "inProgress" || (!selectedPath && !customPath)}
            data-testid="start-assignment-button"
          >
            {message(WF_ADMIN_MSG.START_JOB)}
          </button>
        </div>
      </div>
    </div>
  );
};
