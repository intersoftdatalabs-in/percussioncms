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

/**
 * Edit-mode workflow triggers on the React Content Editor host (#4539).
 */

import React from "react";
import { message } from "../i18n/message";
import styles from "./EditorHost.module.css";
import { triggerRequiresAssignees, triggerRequiresComment } from "./editorWorkflow";
import { EDITOR_MSG } from "./messages";

export interface EditorWorkflowChoiceOption {
  id: string;
  name: string;
}

export interface EditorWorkflowPanelProps {
  stateName?: string;
  triggers: readonly string[];
  comment: string;
  onCommentChange: (value: string) => void;
  assignees?: readonly string[];
  assigneeDraft?: string;
  onAssigneeDraftChange?: (value: string) => void;
  onAddAssignee?: () => void;
  onRemoveAssignee?: (name: string) => void;
  assigneeRequiredTriggers?: readonly string[] | null;
  onTransition: (trigger: string) => void;
  busy?: boolean;
  errorKey?: string | null;
  errorDetail?: string;
  commentRequiredTriggers?: readonly string[] | null;
  workflowChoices?: readonly EditorWorkflowChoiceOption[];
  selectedWorkflowId?: string;
  onWorkflowIdChange?: (workflowId: string) => void;
  onChangeWorkflow?: () => void;
  workflowChanged?: boolean;
}

export function EditorWorkflowPanel({
  stateName,
  triggers,
  comment,
  onCommentChange,
  assignees = [],
  assigneeDraft = "",
  onAssigneeDraftChange,
  onAddAssignee,
  onRemoveAssignee,
  assigneeRequiredTriggers,
  onTransition,
  busy = false,
  errorKey,
  errorDetail,
  commentRequiredTriggers,
  workflowChoices = [],
  selectedWorkflowId = "",
  onWorkflowIdChange,
  onChangeWorkflow,
  workflowChanged = false,
}: EditorWorkflowPanelProps): React.ReactElement {
  return (
    <section
      className={styles.workflow}
      data-testid="editor-workflow"
      aria-label={message(EDITOR_MSG.WORKFLOW_LABEL)}
    >
      <div className={styles.workflowHeader}>
        <span className={styles.label}>{message(EDITOR_MSG.WORKFLOW_LABEL)}</span>
        {stateName ? (
          <span className={styles.workflowState} data-testid="editor-workflow-state">
            {message(EDITOR_MSG.WORKFLOW_STATE)} {stateName}
          </span>
        ) : null}
      </div>
      {triggers.length === 0 ? (
        <p className={styles.hint} data-testid="editor-workflow-empty">
          {message(EDITOR_MSG.WORKFLOW_EMPTY)}
        </p>
      ) : (
        <div className={styles.workflowActions} data-testid="editor-workflow-actions">
          {triggers.map((trigger) => (
            <button
              key={trigger}
              type="button"
              className={styles.button}
              data-testid={`editor-workflow-trigger-${trigger}`}
              data-comment-required={
                triggerRequiresComment(trigger, commentRequiredTriggers)
                  ? "true"
                  : "false"
              }
              data-assignees-required={
                triggerRequiresAssignees(trigger, assigneeRequiredTriggers)
                  ? "true"
                  : "false"
              }
              disabled={busy}
              onClick={() => onTransition(trigger)}
            >
              {trigger}
            </button>
          ))}
        </div>
      )}
      <label className={styles.field}>
        <span className={styles.label}>{message(EDITOR_MSG.WORKFLOW_COMMENT)}</span>
        <textarea
          className={styles.textarea}
          data-testid="editor-workflow-comment"
          value={comment}
          disabled={busy}
          rows={3}
          onChange={(e) => onCommentChange(e.target.value)}
        />
      </label>
      <div className={styles.field} data-testid="editor-workflow-assignees">
        <span className={styles.label}>{message(EDITOR_MSG.WORKFLOW_ASSIGNEES)}</span>
        {assignees.length > 0 ? (
          <ul className={styles.workflowActions}>
            {assignees.map((name) => (
              <li key={name} data-testid={`editor-workflow-assignee-${name}`}>
                {name}
                <button
                  type="button"
                  className={styles.button}
                  data-testid={`editor-workflow-assignee-remove-${name}`}
                  disabled={busy}
                  aria-label={message(EDITOR_MSG.WORKFLOW_ASSIGNEE_REMOVE)}
                  onClick={() => onRemoveAssignee?.(name)}
                >
                  {message(EDITOR_MSG.WORKFLOW_ASSIGNEE_REMOVE)}
                </button>
              </li>
            ))}
          </ul>
        ) : null}
        <input
          className={styles.input}
          data-testid="editor-workflow-assignee-input"
          value={assigneeDraft}
          disabled={busy}
          onChange={(e) => onAssigneeDraftChange?.(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              onAddAssignee?.();
            }
          }}
        />
        <button
          type="button"
          className={styles.button}
          data-testid="editor-workflow-assignee-add"
          disabled={busy}
          onClick={() => onAddAssignee?.()}
        >
          {message(EDITOR_MSG.WORKFLOW_ASSIGNEE_ADD)}
        </button>
      </div>
      {workflowChoices.length > 0 ? (
        <div className={styles.workflowActions} data-testid="editor-workflow-change">
          <label className={styles.field}>
            <span className={styles.label}>{message(EDITOR_MSG.WORKFLOW_CHANGE)}</span>
            <select
              data-testid="editor-workflow-picker"
              value={selectedWorkflowId}
              disabled={busy}
              onChange={(e) => onWorkflowIdChange?.(e.target.value)}
            >
              <option value="">{message(EDITOR_MSG.WORKFLOW_CHANGE_EMPTY)}</option>
              {workflowChoices.map((choice) => (
                <option key={choice.id} value={choice.id}>
                  {choice.name}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className={styles.button}
            data-testid="editor-workflow-save"
            disabled={busy}
            onClick={() => onChangeWorkflow?.()}
          >
            {message(EDITOR_MSG.WORKFLOW_CHANGE_APPLY)}
          </button>
          {workflowChanged ? (
            <span className={styles.meta} data-testid="editor-workflow-changed">
              {message(EDITOR_MSG.WORKFLOW_CHANGED)}
            </span>
          ) : null}
        </div>
      ) : null}
      {errorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-workflow-error"
        >
          {message(errorKey)}
          {errorDetail ? ` ${errorDetail}` : ""}
        </div>
      ) : null}
    </section>
  );
}
