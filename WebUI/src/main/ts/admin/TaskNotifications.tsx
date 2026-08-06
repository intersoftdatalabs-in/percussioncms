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

import React, { useEffect, useState, useRef } from "react";
import { get, put } from "../api/client";
import { PATHS } from "../api/paths";
import { message } from "../i18n/message";
import { ADMIN_MSG } from "./messages";

export interface NotificationTemplate {
  id: string;
  name: string;
  subject: string;
  body: string;
}

export const TaskNotifications: React.FC = () => {
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<NotificationTemplate | null>(null);
  const [subject, setSubject] = useState<string>("");
  const [bodyText, setBodyText] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);

  const isMountedRef = useRef<boolean>(true);

  const loadTemplates = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await get<NotificationTemplate[]>(`${PATHS.SCHEDULED_TASKS}/templates`);
      if (isMountedRef.current) {
        setTemplates(res || []);
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
    loadTemplates();
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handleSelectTemplate = (template: NotificationTemplate) => {
    setSelectedTemplate(template);
    setSubject(template.subject);
    setBodyText(template.body);
  };

  const handleSave = async () => {
    if (!selectedTemplate) return;
    setIsSaving(true);
    setError(null);
    try {
      const updated = await put<NotificationTemplate>(
        `${PATHS.SCHEDULED_TASKS}/templates/${selectedTemplate.id}`,
        { subject, body: bodyText }
      );
      if (isMountedRef.current) {
        setTemplates((prev) =>
          prev.map((t) => (t.id === selectedTemplate.id ? updated : t))
        );
        setSelectedTemplate(null);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to save template.");
      }
    } finally {
      if (isMountedRef.current) {
        setIsSaving(false);
      }
    }
  };

  if (isLoading) {
    return <div style={{ padding: "20px" }}>{message(ADMIN_MSG.LOADING)}</div>;
  }

  return (
    <div className="perc-task-notifications" data-testid="perc-task-notifications">
      {error && (
        <div style={{ color: "#ef4444", marginBottom: "16px" }} data-testid="notifications-error">
          {error}
        </div>
      )}

      {!selectedTemplate ? (
        <div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "16px" }}>
            {templates.map((template) => (
              <div
                key={template.id}
                onClick={() => handleSelectTemplate(template)}
                style={{
                  padding: "16px",
                  background: "#f8fafc",
                  border: "1px solid #e2e8f0",
                  borderRadius: "8px",
                  cursor: "pointer",
                }}
                data-testid={`template-card-${template.id}`}
              >
                <h3 style={{ margin: "0 0 8px 0", color: "#007ea8" }}>{template.name}</h3>
                <div style={{ fontSize: "14px", color: "#64748b" }}>
                  <strong>Subject:</strong> {template.subject}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: "8px", padding: "20px", maxWidth: "600px" }} data-testid="edit-template-form">
          <h3 style={{ marginTop: 0 }}>{message(ADMIN_MSG.TAB_NOTIFICATIONS)} - {selectedTemplate.name}</h3>
          
          <div style={{ marginBottom: "16px" }}>
            <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
              {message(ADMIN_MSG.SUBJECT)}
            </label>
            <input
              type="text"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
              data-testid="template-subject-input"
            />
          </div>

          <div style={{ marginBottom: "20px" }}>
            <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
              {message(ADMIN_MSG.BODY)}
            </label>
            <textarea
              value={bodyText}
              rows={8}
              onChange={(e) => setBodyText(e.target.value)}
              style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
              data-testid="template-body-input"
            />
          </div>

          <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
            <button
              type="button"
              onClick={() => setSelectedTemplate(null)}
              className="perc-button-secondary"
              disabled={isSaving}
            >
              {message(ADMIN_MSG.CANCEL)}
            </button>
            <button
              type="button"
              onClick={handleSave}
              className="perc-button-primary"
              disabled={isSaving}
              data-testid="save-template-btn"
            >
              {isSaving ? message(ADMIN_MSG.LOADING) : message(ADMIN_MSG.SAVE)}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
