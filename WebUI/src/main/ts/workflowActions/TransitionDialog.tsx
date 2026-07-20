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

import React, { useState } from "react";
import { AdhocSearch } from "./AdhocSearch";

interface TransitionDialogProps {
  trigger: string;
  requiresComment: boolean;
  supportsAdhocAssignees: boolean;
  onSubmit: (comment: string, assignees: string[]) => void;
  onCancel: () => void;
}

export const TransitionDialog: React.FC<TransitionDialogProps> = ({
  trigger,
  requiresComment,
  supportsAdhocAssignees,
  onSubmit,
  onCancel,
}) => {
  const [comment, setComment] = useState<string>("");
  const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (requiresComment && !comment.trim()) {
      setError("Comment is required for this transition.");
      return;
    }
    onSubmit(comment.trim(), selectedUsers);
  };

  return (
    <div
      className="perc-transition-dialog-overlay"
      data-testid="perc-transition-dialog-overlay"
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.4)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        zIndex: 1100,
      }}
    >
      <form
        onSubmit={handleSubmit}
        style={{
          background: "#fff",
          padding: "24px",
          borderRadius: "8px",
          width: "100%",
          maxWidth: "450px",
          boxShadow: "0 10px 25px rgba(0,0,0,0.1)",
        }}
      >
        <h3 style={{ margin: "0 0 16px 0" }}>Workflow Action: {trigger}</h3>

        {error && (
          <div style={{ color: "#d9534f", marginBottom: "12px", fontSize: "14px" }} data-testid="transition-error">
            {error}
          </div>
        )}

        <div style={{ marginBottom: "16px" }}>
          <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
            Comments {requiresComment && <span style={{ color: "#d9534f" }}>*</span>}
          </label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={4}
            style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
            data-testid="transition-comment-input"
          />
        </div>

        {supportsAdhocAssignees && (
          <AdhocSearch onSelect={setSelectedUsers} selectedUsers={selectedUsers} />
        )}

        <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px", marginTop: "24px" }}>
          <button type="button" onClick={onCancel} style={{ padding: "8px 16px", borderRadius: "4px", border: "1px solid #cbd5e1", cursor: "pointer" }}>
            Cancel
          </button>
          <button
            type="submit"
            className="perc-button-primary"
            style={{ padding: "8px 16px", borderRadius: "4px", cursor: "pointer" }}
            data-testid="transition-submit-button"
          >
            Submit
          </button>
        </div>
      </form>
    </div>
  );
};
