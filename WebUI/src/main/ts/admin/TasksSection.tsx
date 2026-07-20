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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useState, useRef } from "react";
import { get, post, put, del } from "../api/client";
import { PATHS } from "../api/paths";
import { message } from "../i18n/message";
import { ADMIN_MSG } from "./messages";

export interface ScheduledTask {
  id: string;
  name: string;
  cronSpecification: string;
  extensionName: string;
  emailAddresses?: string;
  notify?: boolean;
  notifyWhen?: "ALWAYS" | "ON_FAILURE" | "NEVER" | "ON_SUCCESS";
  server?: string;
  notificationTemplateId?: string;
}

export interface NotificationTemplate {
  id: string;
  name: string;
}

export const TasksSection: React.FC = () => {
  const [tasks, setTasks] = useState<ScheduledTask[]>([]);
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [editingTask, setEditingTask] = useState<Partial<ScheduledTask> | null>(null);

  const isMountedRef = useRef<boolean>(true);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const resTasks = await get<ScheduledTask[]>(PATHS.SCHEDULED_TASKS);
      const resTemplates = await get<NotificationTemplate[]>(`${PATHS.SCHEDULED_TASKS}/templates`);
      if (isMountedRef.current) {
        setTasks(resTasks || []);
        setTemplates(resTemplates || []);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || message(ADMIN_MSG.ERROR_GENERIC));
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    isMountedRef.current = true;
    loadData();
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handleRunNow = async (id: string) => {
    setError(null);
    try {
      await post(`${PATHS.SCHEDULED_TASKS}/${id}/run`, {});
      alert("Task execution triggered successfully.");
    } catch (err: any) {
      setError(err?.message || "Failed to trigger task execution.");
    }
  };

  const handleDelete = async (task: ScheduledTask) => {
    if (!window.confirm(message(ADMIN_MSG.CONFIRM_DELETE_TASK).replace("{0}", task.name))) {
      return;
    }
    setError(null);
    try {
      await del(`${PATHS.SCHEDULED_TASKS}/${task.id}`);
      if (isMountedRef.current) {
        setTasks((prev) => prev.filter((t) => t.id !== task.id));
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to delete task.");
      }
    }
  };

  const handleSave = async () => {
    if (!editingTask?.name?.trim()) {
      setError(message(ADMIN_MSG.NAME_REQUIRED));
      return;
    }
    if (!editingTask?.cronSpecification?.trim()) {
      setError(message(ADMIN_MSG.CRON_REQUIRED));
      return;
    }
    if (!editingTask?.extensionName?.trim()) {
      setError(message(ADMIN_MSG.TYPE_REQUIRED));
      return;
    }

    setError(null);
    try {
      if (editingTask.id) {
        // Update
        const updated = await put<ScheduledTask>(`${PATHS.SCHEDULED_TASKS}/${editingTask.id}`, editingTask);
        if (isMountedRef.current) {
          setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
        }
      } else {
        // Create
        const created = await post<ScheduledTask>(PATHS.SCHEDULED_TASKS, editingTask);
        if (isMountedRef.current) {
          setTasks((prev) => [...prev, created]);
        }
      }
      setEditingTask(null);
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to save scheduled task.");
      }
    }
  };

  if (isLoading) {
    return <div style={{ padding: "20px" }}>{message(ADMIN_MSG.LOADING)}</div>;
  }

  return (
    <div className="perc-tasks-section" data-testid="perc-tasks-section">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h2>{message(ADMIN_MSG.TAB_TASKS)}</h2>
        <button
          type="button"
          onClick={() =>
            setEditingTask({
              name: "",
              cronSpecification: "0 0 12 * * ?",
              extensionName: "com.percussion.services.schedule.impl.PSPurgeScheduledTaskLog",
              notify: false,
              notifyWhen: "ON_FAILURE",
              emailAddresses: "",
              server: "",
            })
          }
          className="perc-button-primary"
          data-testid="create-task-btn"
        >
          {message(ADMIN_MSG.CREATE_TASK)}
        </button>
      </div>

      {error && (
        <div style={{ color: "#ef4444", marginBottom: "16px" }} data-testid="tasks-error">
          {error}
        </div>
      )}

      {/* Grid list of Tasks */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: "16px", marginBottom: "24px" }}>
        {tasks.map((task) => (
          <div
            key={task.id}
            style={{
              padding: "20px",
              background: "#ffffff",
              border: "1px solid #e2e8f0",
              borderRadius: "8px",
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-between",
            }}
            data-testid={`task-card-${task.id}`}
          >
            <div>
              <h3 style={{ margin: "0 0 8px 0", color: "#1e293b" }}>{task.name}</h3>
              <div style={{ fontSize: "13px", color: "#64748b", marginBottom: "8px" }}>
                <strong>Cron:</strong> {task.cronSpecification}
              </div>
              <div style={{ fontSize: "13px", color: "#64748b", marginBottom: "8px" }}>
                <strong>Class:</strong> {task.extensionName.split(".").pop()}
              </div>
            </div>

            <div style={{ display: "flex", gap: "6px", marginTop: "16px", borderTop: "1px solid #f1f5f9", paddingTop: "12px" }}>
              <button
                type="button"
                onClick={() => handleRunNow(task.id)}
                className="perc-button-secondary"
                style={{ padding: "4px 8px", fontSize: "12px" }}
                data-testid={`run-task-${task.id}`}
              >
                {message(ADMIN_MSG.RUN_NOW)}
              </button>
              <button
                type="button"
                onClick={() => setEditingTask(task)}
                className="perc-button-secondary"
                style={{ padding: "4px 8px", fontSize: "12px" }}
                data-testid={`edit-task-${task.id}`}
              >
                Edit
              </button>
              <button
                type="button"
                onClick={() => handleDelete(task)}
                className="perc-button-secondary"
                style={{ padding: "4px 8px", fontSize: "12px", color: "#ef4444", borderColor: "#fecaca" }}
                data-testid={`delete-task-${task.id}`}
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Create/Edit Dialog Modal */}
      {editingTask && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0,0,0,0.4)",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 1100,
          }}
          data-testid="task-dialog"
        >
          <div style={{ background: "#ffffff", padding: "24px", borderRadius: "8px", width: "100%", maxWidth: "500px", maxHeight: "90vh", overflowY: "auto" }}>
            <h3 style={{ margin: "0 0 16px 0" }}>
              {editingTask.id ? message(ADMIN_MSG.EDIT_TASK) : message(ADMIN_MSG.CREATE_TASK)}
            </h3>

            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
                {message(ADMIN_MSG.TASK_NAME)}
              </label>
              <input
                type="text"
                value={editingTask.name || ""}
                onChange={(e) => setEditingTask((prev) => ({ ...prev, name: e.target.value }))}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="task-name-input"
              />
            </div>

            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
                {message(ADMIN_MSG.CRON_EXPRESSION)}
              </label>
              <input
                type="text"
                value={editingTask.cronSpecification || ""}
                onChange={(e) => setEditingTask((prev) => ({ ...prev, cronSpecification: e.target.value }))}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="task-cron-input"
              />
            </div>

            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
                {message(ADMIN_MSG.TASK_TYPE)} (Extension Class)
              </label>
              <input
                type="text"
                value={editingTask.extensionName || ""}
                onChange={(e) => setEditingTask((prev) => ({ ...prev, extensionName: e.target.value }))}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="task-type-input"
              />
            </div>

            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
                {message(ADMIN_MSG.EMAIL_ADDRESSES)}
              </label>
              <input
                type="text"
                value={editingTask.emailAddresses || ""}
                onChange={(e) => setEditingTask((prev) => ({ ...prev, emailAddresses: e.target.value }))}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="task-emails-input"
              />
            </div>

            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
                {message(ADMIN_MSG.NOTIFICATION_TEMPLATE)}
              </label>
              <select
                value={editingTask.notificationTemplateId || ""}
                onChange={(e) => setEditingTask((prev) => ({ ...prev, notificationTemplateId: e.target.value }))}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="task-template-input"
              >
                <option value="">-- No Notification --</option>
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ marginBottom: "16px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={!!editingTask.notify}
                  onChange={(e) => setEditingTask((prev) => ({ ...prev, notify: e.target.checked }))}
                  data-testid="task-notify-input"
                />
                Send Alerts
              </label>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px", marginTop: "20px" }}>
              <button type="button" onClick={() => setEditingTask(null)} className="perc-button-secondary">
                {message(ADMIN_MSG.CANCEL)}
              </button>
              <button type="button" onClick={handleSave} className="perc-button-primary" data-testid="save-task-btn">
                {message(ADMIN_MSG.SAVE)}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
