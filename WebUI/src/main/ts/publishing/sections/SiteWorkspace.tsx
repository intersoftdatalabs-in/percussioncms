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

import React, { useCallback, useEffect, useState } from "react";
import {
  clearIncrementalQueue,
  getIncrementalItems,
  getIncrementalRelatedItems,
  incrementalPublishSite,
  publishIncrementalWithApproval,
  publishSite,
  removeIncrementalQueueItem,
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
import { ItemPublishNowPanel } from "../components/ItemPublishNowPanel";
import { ItemPublishingActionsMenu } from "../components/ItemPublishingActionsMenu";
import { ItemScheduleDatesPanel } from "../components/ItemScheduleDatesPanel";
import { ItemStagePanel } from "../components/ItemStagePanel";
import { ItemTakedownPanel } from "../components/ItemTakedownPanel";
import { ServerEditor } from "../components/ServerEditor";
import { ServerList } from "../components/ServerList";
import { useDirtyForm } from "../dirtyFormContext";
import {
  buildApprovalPayload,
  collectRelatedItemIds,
  relatedItemId,
  relatedItemLabel,
  shouldUseApprovalPath,
} from "../incrementalApproval";
import {
  extractQueueItems,
  isQueueEmpty,
  queueItemId,
  queueItemLabel,
  queueRemoveFailure,
} from "../incrementalQueue";
import { isJobStoppable, mapJobStopError } from "../jobStop";
import {
  extractPublishJobId,
  mapPublishError,
  mapPublishResponse,
  startPublishState,
  successPublishState,
} from "../publishActions";
import type { PublishActionResult } from "../publishActions";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  primaryButtonStyle,
  tableStyle,
  tdStyle,
  thStyle,
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
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
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

function caughtErrorMessage(result: PublishActionResult): string {
  if (result.state === "forbidden") {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (result.state === "badconfig") {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  return result.message || message(MSG.PUBLISH_ERROR);
}

function preflightErrorMessage(result: PublishActionResult): string {
  if (result.message && result.message !== result.token) {
    return result.message;
  }
  if (result.state === "forbidden") {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (result.state === "badconfig") {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  return result.message || message(MSG.PUBLISH_ERROR);
}

export function SiteWorkspace({
  site,
  initialServerId = "",
  itemId,
  onItemIdChange,
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
  const [queueLoadError, setQueueLoadError] = useState<string | null>(null);
  const [queueRemoveError, setQueueRemoveError] = useState<string | null>(null);
  const [relatedPreview, setRelatedPreview] = useState<unknown[]>([]);
  const [selectedRelated, setSelectedRelated] = useState<Set<string>>(
    new Set(),
  );
  const [previewLoaded, setPreviewLoaded] = useState(false);
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
    const id = selectedServer;
    await deleteServer(siteId, id);
    setServers((prev) => prev.filter((s) => serverIdOf(s) !== id));
    setSelectedServer("");
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
      const result = await publishSite(siteName, selectedServerName);
      const preflight = mapPublishResponse(result);
      if (preflight) {
        setActionState(preflight.state);
        setActionMessage(preflightErrorMessage(preflight));
        return;
      }
      setActionState(successPublishState());
      setActionMessage(message(MSG.PUBLISH_SUCCESS));
      refreshJobs();
    } catch (err) {
      const mapped = mapPublishError(err);
      setActionState(mapped.state);
      setActionMessage(caughtErrorMessage(mapped));
    }
  }

  async function loadIncrementalPreview(options?: {
    preserveRemoveError?: boolean;
  }): Promise<unknown[] | null> {
    if (!selectedServerName) {
      return null;
    }
    setQueueLoadError(null);
    if (!options?.preserveRemoveError) {
      setQueueRemoveError(null);
    }
    try {
      const page = await getIncrementalItems(siteName, selectedServerName, 1, 25);
      const items = extractQueueItems(page);
      setQueuePreview(items);
      const related = await getIncrementalRelatedItems(
        siteName,
        selectedServerName,
        1,
        25,
      );
      const relatedItems = extractQueueItems(related);
      setRelatedPreview(relatedItems);
      // Default: select none (user explicitly chooses related items to approve)
      setSelectedRelated(new Set());
      setPreviewLoaded(true);
      setQueueLoadError(null);
      return items;
    } catch (err) {
      setQueuePreview([]);
      setRelatedPreview([]);
      setSelectedRelated(new Set());
      setPreviewLoaded(false);
      const mapped = mapPublishError(err);
      const text = caughtErrorMessage(mapped);
      setQueueLoadError(text);
      setActionMessage(text);
      setActionState("error");
      return null;
    }
  }

  function queueRemoveMessage(reason: ReturnType<typeof queueRemoveFailure>): string {
    if (reason === "forbidden") {
      return message(MSG.PUBLISH_QUEUE_REMOVE_FORBIDDEN);
    }
    if (reason === "not_found") {
      return message(MSG.PUBLISH_QUEUE_REMOVE_NOT_FOUND);
    }
    return message(MSG.PUBLISH_QUEUE_REMOVE_FAILED);
  }

  async function removeOneQueueItem(contentId: string): Promise<void> {
    if (!selectedServerName || contentId === "") {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_REMOVE_QUEUE_ITEM))) {
      return;
    }
    setQueueRemoveError(null);
    try {
      await removeIncrementalQueueItem(siteName, selectedServerName, contentId);
      setQueuePreview((prev) =>
        prev.filter((item) => queueItemId(item) !== contentId),
      );
      setActionState(successPublishState());
      setActionMessage(message(MSG.PUBLISH_QUEUE_ITEM_REMOVED));
    } catch (err) {
      setQueueRemoveError(queueRemoveMessage(queueRemoveFailure(err)));
      setActionState("error");
    }
  }

  function queueClearMessage(reason: ReturnType<typeof queueRemoveFailure>): string {
    if (reason === "forbidden") {
      return message(MSG.PUBLISH_QUEUE_CLEAR_FORBIDDEN);
    }
    if (reason === "not_found") {
      return message(MSG.PUBLISH_QUEUE_CLEAR_NOT_FOUND);
    }
    return message(MSG.PUBLISH_QUEUE_CLEAR_FAILED);
  }

  async function clearWholeQueue(): Promise<void> {
    if (!selectedServerName || isQueueEmpty({ items: queuePreview })) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_CLEAR_QUEUE))) {
      return;
    }
    setQueueRemoveError(null);
    try {
      await clearIncrementalQueue(siteName, selectedServerName);
    } catch (err) {
      setQueueRemoveError(queueClearMessage(queueRemoveFailure(err)));
      setActionState("error");
      await loadIncrementalPreview({ preserveRemoveError: true });
      return;
    }
    const remaining = await loadIncrementalPreview();
    if (remaining == null) {
      return;
    }
    if (remaining.length === 0) {
      setActionState(successPublishState());
      setActionMessage(message(MSG.PUBLISH_QUEUE_CLEARED));
      return;
    }
    setActionState("error");
    setActionMessage(message(MSG.PUBLISH_QUEUE_CLEAR_PARTIAL));
  }

  function toggleRelated(id: string): void {
    setSelectedRelated((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function toggleSelectAllRelated(checked: boolean): void {
    if (checked) {
      setSelectedRelated(new Set(collectRelatedItemIds(relatedPreview)));
    } else {
      setSelectedRelated(new Set());
    }
  }

  async function runIncrementalPublish(): Promise<void> {
    if (!selectedServerName) {
      setActionMessage(message(MSG.PUBLISH_SELECT_SERVER));
      setActionState("error");
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_INCREMENTAL))) {
      return;
    }
    setActionState(startPublishState());
    setActionMessage(null);
    try {
      let result: unknown;
      if (shouldUseApprovalPath(relatedPreview)) {
        const payload = buildApprovalPayload([...selectedRelated]);
        result = await publishIncrementalWithApproval(
          siteName,
          selectedServerName,
          payload,
        );
      } else {
        result = await incrementalPublishSite(siteName, selectedServerName);
      }
      const preflight = mapPublishResponse(result);
      if (preflight) {
        setActionState(preflight.state);
        setActionMessage(preflightErrorMessage(preflight));
        return;
      }
      setActionState(successPublishState());
      const jobId = extractPublishJobId(result);
      setActionMessage(
        jobId
          ? `${message(MSG.PUBLISH_JOB_STARTED)} ${jobId}`
          : message(MSG.PUBLISH_SUCCESS),
      );
      setPreviewLoaded(false);
      setRelatedPreview([]);
      setQueuePreview([]);
      setQueueLoadError(null);
      setQueueRemoveError(null);
      setSelectedRelated(new Set());
      refreshJobs();
    } catch (err) {
      const mapped = mapPublishError(err);
      setActionState(mapped.state);
      setActionMessage(caughtErrorMessage(mapped));
    }
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
      refreshJobs();
    } catch (err) {
      setActionMessage(mapJobStopError(err));
      setActionState("error");
    }
  }

  const noServers = !loadingServers && servers.length === 0;
  const allRelatedIds = collectRelatedItemIds(relatedPreview);
  const allRelatedSelected =
    allRelatedIds.length > 0 &&
    allRelatedIds.every((id) => selectedRelated.has(id));

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

      <ItemPublishingActionsMenu
        itemId={itemId}
        onItemIdChange={onItemIdChange}
      />
      <ItemScheduleDatesPanel
        itemId={itemId}
        onItemIdChange={onItemIdChange}
      />
      <ItemPublishNowPanel
        itemId={itemId}
        onItemIdChange={onItemIdChange}
        onPublished={refreshJobs}
      />
      <ItemStagePanel itemId={itemId} onItemIdChange={onItemIdChange} />
      <ItemTakedownPanel itemId={itemId} onItemIdChange={onItemIdChange} />

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
            data-testid="publish-edit-server"
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
          data-testid="publish-incremental-preview-btn"
        >
          {message(MSG.PUBLISH_INCREMENTAL)} preview
        </button>
        <button
          type="button"
          style={buttonStyle}
          disabled={!selectedServerName || actionState === "starting"}
          onClick={() => void runIncrementalPublish()}
          data-testid="publish-incremental-confirm"
        >
          {message(MSG.PUBLISH_INCREMENTAL)}
          {shouldUseApprovalPath(relatedPreview) ? " (with approval)" : ""}
        </button>
      </div>

      {actionMessage && (
        <p
          style={actionState === "success" ? emptyStyle : errorStyle}
          role={actionState === "success" ? "status" : "alert"}
          data-testid="publish-action-message"
        >
          {actionMessage}
        </p>
      )}

      {queueRemoveError && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="publish-incremental-queue-remove-error"
        >
          {queueRemoveError}
        </p>
      )}

      {queueLoadError && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="publish-incremental-queue-error"
        >
          {queueLoadError}
        </p>
      )}

      {previewLoaded && (
        <div style={{ marginTop: 12 }} data-testid="publish-incremental-preview">
          <h3 style={{ fontSize: "1rem" }}>
            {message(MSG.PUBLISH_INCREMENTAL)}
          </h3>
          {!isQueueEmpty({ items: queuePreview }) && (
            <div style={{ marginBottom: 8 }}>
              <button
                type="button"
                style={buttonStyle}
                disabled={actionState === "starting"}
                data-testid="publish-incremental-queue-clear"
                onClick={() => void clearWholeQueue()}
              >
                {message(MSG.PUBLISH_CLEAR_QUEUE)}
              </button>
            </div>
          )}
          {isQueueEmpty({ items: queuePreview }) ? (
            <p style={emptyStyle} data-testid="publish-incremental-queue-empty">
              {message(MSG.PUBLISH_EMPTY_QUEUE)}
            </p>
          ) : (
            <table style={tableStyle} data-testid="publish-incremental-queue-list">
              <thead>
                <tr>
                  <th style={thStyle}>Id</th>
                  <th style={thStyle}>Item</th>
                  <th style={thStyle}>{message(MSG.PUBLISH_REMOVE_QUEUE_ITEM)}</th>
                </tr>
              </thead>
              <tbody>
                {queuePreview.map((item, idx) => {
                  const id = queueItemId(item);
                  const label = queueItemLabel(item);
                  return (
                    <tr
                      key={id !== "" ? id : `queue-${idx}`}
                      data-testid="publish-incremental-queue-row"
                    >
                      <td style={tdStyle}>{id !== "" ? id : "—"}</td>
                      <td style={tdStyle}>{label}</td>
                      <td style={tdStyle}>
                        <button
                          type="button"
                          style={buttonStyle}
                          disabled={id === "" || actionState === "starting"}
                          data-testid="publish-incremental-queue-remove"
                          onClick={() => void removeOneQueueItem(id)}
                        >
                          {message(MSG.PUBLISH_REMOVE_QUEUE_ITEM)}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          {relatedPreview.length > 0 && (
            <div style={{ marginTop: 12 }} data-testid="publish-related-approval">
              <h4 style={{ fontSize: "0.95rem" }}>
                {message(MSG.PUBLISH_RELATED_ITEMS)} for approval
              </h4>
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>
                      <input
                        type="checkbox"
                        checked={allRelatedSelected}
                        onChange={(e) =>
                          toggleSelectAllRelated(e.target.checked)
                        }
                        aria-label="select all related items"
                        data-testid="publish-related-select-all"
                      />
                    </th>
                    <th style={thStyle}>Item</th>
                    <th style={thStyle}>Id</th>
                  </tr>
                </thead>
                <tbody>
                  {relatedPreview.map((item, idx) => {
                    const id = relatedItemId(item) ?? `row-${idx}`;
                    const selectable = relatedItemId(item) != null;
                    return (
                      <tr key={id}>
                        <td style={tdStyle}>
                          {selectable && (
                            <input
                              type="checkbox"
                              checked={selectedRelated.has(id)}
                              onChange={() => toggleRelated(id)}
                              aria-label={`approve related ${id}`}
                            />
                          )}
                        </td>
                        <td style={tdStyle}>{relatedItemLabel(item)}</td>
                        <td style={tdStyle}>{id}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              <p style={{ fontSize: "0.85rem", color: "#555" }}>
                Selected for approval: {selectedRelated.size} of{" "}
                {allRelatedIds.length}
              </p>
            </div>
          )}
        </div>
      )}

      <div style={{ marginTop: 20 }} data-testid="publish-site-jobs">
        <h3 style={{ fontSize: "1rem" }}>{message(MSG.PUBLISH_SECTION_STATUS)}</h3>
        {jobs.length === 0 ? (
          <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_JOBS)}</p>
        ) : (
          <ul>
            {jobs.map((job) => {
              const id = job.jobId ?? "";
              const stoppable = isJobStoppable(job);
              return (
                <li key={String(id)} data-testid={`publish-job-${String(id)}`}>
                  {job.status} {job.serverName ?? ""}{" "}
                  {id !== "" ? `job ${id}` : ""}{" "}
                  {stoppable && (
                    <button
                      type="button"
                      style={buttonStyle}
                      data-testid={`publish-stop-job-${String(id)}`}
                      onClick={() => void onStop(job)}
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
