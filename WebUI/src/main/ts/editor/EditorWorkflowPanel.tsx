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
  /**
   * True while a panel transition run is in flight (#7134, parent #4532).
   * Rendered as a persistently-mounted polite live region (same pattern as
   * the publish/takedown loading rows, #7087/#7125) so screen-reader users
   * hear the run start; the text swaps on the same node because a live
   * region mounted together with its text is missed by most SR/browser
   * combos. Reuses the assignee-dialog busy copy — no new i18n key.
   */
  transitionRunning?: boolean;
  errorKey?: string | null;
  errorDetail?: string;
  /**
   * True when the visible panel error is a comment-gate failure (missing
   * or NUL comment, #6931). The comment box is marked invalid and its
   * accessible description points at the inline error so screen-reader
   * and keyboard users hear where the fix goes; any other error leaves
   * the comment box unmarked.
   */
  commentInvalid?: boolean;
  /**
   * Panel-level transition-list load failure (#5769). When set, the panel
   * shows a load notice with retry instead of the empty-allowlist hint so
   * operators can tell "no transitions allowed" from "the list failed to load".
   */
  loadErrorKey?: string | null;
  loadErrorDetail?: string;
  onRetryLoad?: () => void;
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
  transitionRunning = false,
  errorKey,
  errorDetail,
  commentInvalid = false,
  loadErrorKey,
  loadErrorDetail,
  onRetryLoad,
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
      {loadErrorKey ? (
        <div
          className={styles.status}
          role="alert"
          data-testid="editor-workflow-load-error"
        >
          {message(loadErrorKey)}
          {loadErrorDetail ? ` ${loadErrorDetail}` : ""}
          <div className={styles.actions}>
            <button
              type="button"
              className={styles.button}
              data-testid="editor-retry-workflow-load"
              onClick={() => onRetryLoad?.()}
            >
              {message(EDITOR_MSG.LOAD_RETRY)}
            </button>
          </div>
        </div>
      ) : null}
      {triggers.length === 0 ? (
        loadErrorKey ? null : (
          <p className={styles.hint} data-testid="editor-workflow-empty">
            {message(EDITOR_MSG.WORKFLOW_EMPTY)}
          </p>
        )
      ) : (
        <div className={styles.workflowActions} data-testid="editor-workflow-actions">
          {triggers.map((trigger) => {
            const commentRequired = triggerRequiresComment(
              trigger,
              commentRequiredTriggers,
            );
            return (
              <button
                key={trigger}
                type="button"
                className={styles.button}
                data-testid={`editor-workflow-trigger-${trigger}`}
                data-comment-required={
                  commentRequired ? "true" : "false"
                }
                data-assignees-required={
                  triggerRequiresAssignees(trigger, assigneeRequiredTriggers)
                    ? "true"
                    : "false"
                }
                // Comment-required triggers announce where the comment goes
                // (#6632): the visible suffix keeps the affordance in the
                // accessible name while describedby points at the comment box.
                aria-describedby={
                  commentRequired
                    ? "editor-workflow-comment-label"
                    : undefined
                }
                disabled={busy}
                onClick={() => onTransition(trigger)}
              >
                {trigger}
                {commentRequired ? (
                  <span
                    className={styles.meta}
                    data-testid={`editor-workflow-trigger-${trigger}-requires-comment`}
                  >
                    {` (${message(EDITOR_MSG.WORKFLOW_TRIGGER_REQUIRES_COMMENT)})`}
                  </span>
                ) : null}
              </button>
            );
          })}
        </div>
      )}
      <p
        className={styles.meta}
        role="status"
        data-testid="editor-workflow-running"
      >
        {transitionRunning ? message(EDITOR_MSG.WORKFLOW_ASSIGNEE_BUSY) : ""}
      </p>
      <label className={styles.field}>
        <span className={styles.label} id="editor-workflow-comment-label">
          {message(EDITOR_MSG.WORKFLOW_COMMENT)}
        </span>
        <textarea
          className={styles.textarea}
          data-testid="editor-workflow-comment"
          id="editor-workflow-comment"
          value={comment}
          disabled={busy}
          rows={3}
          aria-invalid={commentInvalid ? true : undefined}
          aria-describedby={
            commentInvalid ? "editor-workflow-comment-error" : undefined
          }
          onChange={(e) => onCommentChange(e.target.value)}
        />
      </label>
      <div data-testid="editor-workflow-assignees">
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
        <label className={styles.field}>
          <span className={styles.label}>{message(EDITOR_MSG.WORKFLOW_ASSIGNEES)}</span>
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
        </label>
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
          id={commentInvalid ? "editor-workflow-comment-error" : undefined}
        >
          {message(errorKey)}
          {errorDetail ? ` ${errorDetail}` : ""}
        </div>
      ) : null}
    </section>
  );
}
