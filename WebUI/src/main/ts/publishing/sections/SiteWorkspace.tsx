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

import React, { useCallback, useEffect, useState } from "react";
import {
  getIncrementalItems,
  getIncrementalRelatedItems,
  incrementalPublishSite,
  publishSite,
} from "../../api/publishing/publishApi";
import {
  createServer,
  deleteServer,
  fetchAvailableRegions,
  getServer,
  isEC2Instance,
  listServers,
  stopPublishing,
  updateServer,
} from "../../api/publishing/serversApi";
import { fetchCurrentJobsForSite } from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import { ServerEditor } from "../components/ServerEditor";
import { ServerList } from "../components/ServerList";
import { useDirtyForm } from "../dirtyFormContext";
import {
  extractQueueItems,
  isQueueEmpty,
} from "../incrementalQueue";
import {
  mapPublishError,
  startPublishState,
  successPublishState,
} from "../publishActions";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import type {
  PublishActionState,
  PublishServer,
  PublishSiteSummary,
  PublishingJob,
} from "../types";

export interface SiteWorkspaceProps {
  site: PublishSiteSummary;
  initialServerId?: string;
  onBack: () => void;
}

function serverDisplayName(s: PublishServer): string {
  return s.serverName ?? s.name ?? String(s.serverId ?? "");
}

function serverIdOf(s: PublishServer): string {
  return String(s.serverId ?? s.serverName ?? s.name ?? "");
}

function unwrapServer(data: unknown): PublishServer {
  if (data && typeof data === "object" && "serverInfo" in data) {
    return (data as { serverInfo: PublishServer }).serverInfo;
  }
  return (data ?? {}) as PublishServer;
}

export function SiteWorkspace({
  site,
  initialServerId = "",
  onBack,
}: SiteWorkspaceProps): React.ReactElement {
  const siteName = site.name;
  const siteId = site.siteId ?? site.id ?? site.name;
  const { setDirty, confirmIfDirty } = useDirtyForm();

  const [servers, setServers] = useState<PublishServer[]>([]);
  const [selectedServer, setSelectedServer] = useState(initialServerId);
  const [actionState, setActionState] = useState<PublishActionState>("idle");
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [jobs, setJobs] = useState<PublishingJob[]>([]);
  const [queuePreview, setQueuePreview] = useState<unknown[]>([]);
  const [relatedPreview, setRelatedPreview] = useState<unknown[]>([]);
  const [loadingServers, setLoadingServers] = useState(true);
  const [editorMode, setEditorMode] = useState<"closed" | "create" | "edit">(
    "closed",
  );
  const [editServer, setEditServer] = useState<PublishServer | null>(null);
  const [regions, setRegions] = useState<string[]>([]);
  const [ec2, setEc2] = useState(false);

  const refreshJobs = useCallback(() => {
    if (siteId == null || siteId === "") {
      return;
    }
    fetchCurrentJobsForSite(siteId)
      .then(setJobs)
      .catch(() => setJobs([]));
  }, [siteId]);

  const loadServers = useCallback(() => {
    setLoadingServers(true);
    listServers(siteId)
      .then((list) => {
        setServers(list);
        setSelectedServer((prev) => {
          if (prev && list.some((s) => serverIdOf(s) === prev)) {
            return prev;
          }
          return list.length > 0 ? serverIdOf(list[0]) : "";
        });
      })
      .catch(() => setServers([]))
      .finally(() => setLoadingServers(false));
  }, [siteId]);

  useEffect(() => {
    loadServers();
  }, [loadServers]);

  useEffect(() => {
    refreshJobs();
  }, [refreshJobs]);

  useEffect(() => {
    isEC2Instance()
      .then((v) => setEc2(v === true || v === "true"))
      .catch(() => setEc2(false));
    fetchAvailableRegions()
      .then((data) => {
        if (Array.isArray(data)) {
          setRegions(data.map(String));
        } else if (data && typeof data === "object") {
          const arr = Object.values(data as Record<string, unknown>).flat();
          setRegions(arr.map(String));
        }
      })
      .catch(() => setRegions([]));
  }, []);

  const selectedServerName = (() => {
    const found = servers.find((s) => serverIdOf(s) === selectedServer);
    return found ? serverDisplayName(found) : selectedServer;
  })();

  async function openEdit(): Promise<void> {
    if (!confirmIfDirty()) {
      return;
    }
    if (!selectedServer) {
      return;
    }
    try {
      const raw = await getServer(siteId, selectedServer);
      setEditServer(unwrapServer(raw));
      setEditorMode("edit");
    } catch {
      setActionMessage(message(MSG.PUBLISH_ERROR));
      setActionState("error");
    }
  }

  function openCreate(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setEditServer(null);
    setEditorMode("create");
  }

  function closeEditor(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setEditorMode("closed");
    setEditServer(null);
    setDirty(false);
  }

  async function handleSave(
    body: { serverInfo: Record<string, unknown> },
    isCreate: boolean,
  ): Promise<void> {
    const info = body.serverInfo;
    const name = String(info.serverName ?? "");
    if (isCreate) {
      await createServer(siteId, name, body);
    } else {
      const id = String(info.serverId ?? selectedServer);
      await updateServer(siteId, id, body);
    }
    setEditorMode("closed");
    setEditServer(null);
    setDirty(false);
    loadServers();
    setActionMessage(message(MSG.PUBLISH_SUCCESS));
    setActionState("success");
  }

  async function handleDelete(): Promise<void> {
    if (!selectedServer) {
      return;
    }
    await deleteServer(siteId, selectedServer);
    setEditorMode("closed");
    setEditServer(null);
    setDirty(false);
    loadServers();
  }

  async function runFullPublish(): Promise<void> {
    if (!selectedServerName) {
      setActionMessage(message(MSG.PUBLISH_SELECT_SERVER));
      setActionState("error");
      return;
    }
    setActionState(startPublishState());
    setActionMessage(null);
    try {
      await publishSite(siteName, selectedServerName);
      setActionState(successPublishState());
      setActionMessage(message(MSG.PUBLISH_SUCCESS));
      refreshJobs();
    } catch (err) {
      const mapped = mapPublishError(err);
      setActionState(mapped.state);
      setActionMessage(
        mapped.state === "forbidden"
          ? message(MSG.PUBLISH_FORBIDDEN)
          : mapped.state === "badconfig"
            ? message(MSG.PUBLISH_BADCONFIG)
            : mapped.message || message(MSG.PUBLISH_ERROR),
      );
    }
  }

  async function loadIncrementalPreview(): Promise<void> {
    if (!selectedServerName) {
      return;
    }
    try {
      const page = await getIncrementalItems(siteName, selectedServerName, 1, 25);
      setQueuePreview(extractQueueItems(page));
      const related = await getIncrementalRelatedItems(
        siteName,
        selectedServerName,
        1,
        25,
      );
      setRelatedPreview(extractQueueItems(related));
    } catch {
      setQueuePreview([]);
      setRelatedPreview([]);
      setActionMessage(message(MSG.PUBLISH_ERROR));
      setActionState("error");
    }
  }

  async function runIncrementalPublish(): Promise<void> {
    if (!selectedServerName) {
      setActionMessage(message(MSG.PUBLISH_SELECT_SERVER));
      setActionState("error");
      return;
    }
    setActionState(startPublishState());
    setActionMessage(null);
    try {
      await incrementalPublishSite(siteName, selectedServerName);
      setActionState(successPublishState());
      setActionMessage(message(MSG.PUBLISH_SUCCESS));
      refreshJobs();
    } catch (err) {
      const mapped = mapPublishError(err);
      setActionState(mapped.state);
      setActionMessage(
        mapped.state === "forbidden"
          ? message(MSG.PUBLISH_FORBIDDEN)
          : mapped.state === "badconfig"
            ? message(MSG.PUBLISH_BADCONFIG)
            : mapped.message || message(MSG.PUBLISH_ERROR),
      );
    }
  }

  async function onStop(jobId: string | number): Promise<void> {
    try {
      await stopPublishing(jobId);
      refreshJobs();
    } catch {
      setActionMessage(message(MSG.PUBLISH_ERROR));
      setActionState("error");
    }
  }

  const noServers = !loadingServers && servers.length === 0;

  if (editorMode !== "closed") {
    return (
      <div data-testid="publish-site-workspace">
        <div style={toolbarStyle}>
          <button type="button" style={buttonStyle} onClick={closeEditor}>
            {message(MSG.PUBLISH_BACK)}
          </button>
          <h2 style={{ margin: 0, fontSize: "1.1rem" }}>{siteName}</h2>
        </div>
        <ServerEditor
          siteId={siteId}
          server={editorMode === "edit" ? editServer : null}
          regions={regions}
          isEC2={ec2}
          onSave={handleSave}
          onDelete={editorMode === "edit" ? handleDelete : undefined}
          onCancel={closeEditor}
          onDirtyChange={setDirty}
        />
      </div>
    );
  }

  return (
    <div data-testid="publish-site-workspace">
      <div style={toolbarStyle}>
        <button
          type="button"
          style={buttonStyle}
          onClick={() => {
            if (confirmIfDirty()) {
              onBack();
            }
          }}
        >
          {message(MSG.PUBLISH_BACK)}
        </button>
        <h2 style={{ margin: 0, fontSize: "1.1rem" }}>{siteName}</h2>
      </div>

      {loadingServers && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {noServers && (
        <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_SERVERS)}</p>
      )}

      <div style={{ marginBottom: 16 }}>
        <h3 style={{ fontSize: "1rem" }}>{message(MSG.PUBLISH_SELECT_SERVER)}</h3>
        <ServerList
          servers={servers}
          selectedId={selectedServer}
          onSelect={setSelectedServer}
          onRefresh={loadServers}
          onAdd={openCreate}
        />
        <div style={toolbarStyle}>
          <button
            type="button"
            style={buttonStyle}
            disabled={!selectedServer}
            onClick={() => void openEdit()}
          >
            {message(MSG.PUBLISH_EDIT_SERVER)}
          </button>
        </div>
      </div>

      <div style={toolbarStyle}>
        <button
          type="button"
          style={primaryButtonStyle}
          disabled={!selectedServerName || actionState === "starting"}
          onClick={() => void runFullPublish()}
        >
          {message(MSG.PUBLISH_FULL)}
        </button>
        <button
          type="button"
          style={buttonStyle}
          disabled={!selectedServerName || actionState === "starting"}
          onClick={() => void loadIncrementalPreview()}
        >
          {message(MSG.PUBLISH_INCREMENTAL)} preview
        </button>
        <button
          type="button"
          style={buttonStyle}
          disabled={!selectedServerName || actionState === "starting"}
          onClick={() => void runIncrementalPublish()}
        >
          {message(MSG.PUBLISH_INCREMENTAL)}
        </button>
      </div>

      {actionMessage && (
        <p
          style={actionState === "success" ? emptyStyle : errorStyle}
          role={actionState === "success" ? "status" : "alert"}
        >
          {actionMessage}
        </p>
      )}

      {(queuePreview.length > 0 || relatedPreview.length > 0) && (
        <div style={{ marginTop: 12 }}>
          <h3 style={{ fontSize: "1rem" }}>
            {message(MSG.PUBLISH_INCREMENTAL)}
          </h3>
          {isQueueEmpty({ items: queuePreview }) ? (
            <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_QUEUE)}</p>
          ) : (
            <p>
              Queue items: {queuePreview.length}
              {relatedPreview.length > 0
                ? `; related: ${relatedPreview.length}`
                : ""}
            </p>
          )}
        </div>
      )}

      <div style={{ marginTop: 20 }}>
        <h3 style={{ fontSize: "1rem" }}>{message(MSG.PUBLISH_SECTION_STATUS)}</h3>
        {jobs.length === 0 ? (
          <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_JOBS)}</p>
        ) : (
          <ul>
            {jobs.map((job) => {
              const id = job.jobId ?? "";
              const stoppable =
                !job.isStopping &&
                String(job.status ?? "")
                  .toLowerCase()
                  .includes("run");
              return (
                <li key={String(id)}>
                  {job.status} {job.serverName ?? ""}{" "}
                  {stoppable && id !== "" && (
                    <button
                      type="button"
                      style={buttonStyle}
                      onClick={() => void onStop(id)}
                    >
                      {message(MSG.PUBLISH_STOP)}
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}
