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

import React, { useEffect, useState } from "react";
import {
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../../api/client";
import {
  removeFromStagingSelectedItem,
  stageSelectedItem,
} from "../../contentExplorer/itemPublish";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  mapStageAction,
  mapStageKind,
  pathItemForStage,
  type StageAction,
  type StageKind,
} from "../itemStage";
import {
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface ItemStagePanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

function stageErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_BADCONFIG));
    }
    if (err.status === 404) {
      return formatApiError(err, message(MSG.PUBLISH_NOW_NOT_FOUND));
    }
  }
  const text = formatApiError(err, message(MSG.PUBLISH_ERROR));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (/\bBADCONFIG\b/i.test(text)) {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  if (/\b404\b|\bNOT FOUND\b/i.test(text)) {
    return message(MSG.PUBLISH_NOW_NOT_FOUND);
  }
  return text;
}

export function ItemStagePanel({
  itemId,
  onItemIdChange,
}: ItemStagePanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [kind, setKind] = useState<StageKind>("page");
  const [action, setAction] = useState<StageAction>("stage");
  const [reviewed, setReviewed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setInputId(itemId ?? "");
    setReviewed(false);
    setSuccess(null);
  }, [itemId]);

  function onReview(e: React.FormEvent): void {
    e.preventDefault();
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    const safe = mapIdParam(next);
    if (!safe) {
      setReviewed(false);
      setSuccess(null);
      setError(
        next.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    setError(null);
    setSuccess(null);
    setReviewed(true);
  }

  async function onSubmit(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const safe = mapIdParam(inputId) || mapIdParam(itemId);
    if (!safe) {
      setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
      setSuccess(null);
      return;
    }
    if (!reviewed) {
      setReviewed(true);
      return;
    }
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const item = pathItemForStage(safe, kind);
      const ok =
        action === "stage"
          ? await stageSelectedItem(item)
          : await removeFromStagingSelectedItem(item);
      if (!ok) {
        setError(message(MSG.PUBLISH_ERROR));
        return;
      }
      onItemIdChange?.(safe);
      setSuccess(
        action === "stage"
          ? message(MSG.PUBLISH_STAGE_SUCCESS_STAGE)
          : message(MSG.PUBLISH_STAGE_SUCCESS_UNSTAGE),
      );
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(stageErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  const confirmText =
    action === "stage"
      ? message(MSG.PUBLISH_STAGE_CONFIRM_STAGE)
      : message(MSG.PUBLISH_STAGE_CONFIRM_UNSTAGE);
  const submitText =
    action === "stage"
      ? message(MSG.PUBLISH_STAGE_SUBMIT_STAGE)
      : message(MSG.PUBLISH_STAGE_SUBMIT_UNSTAGE);

  return (
    <section
      data-testid="item-stage"
      aria-labelledby="item-stage-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-stage-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_STAGE)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_STAGE_HINT)}
      </p>
      <form onSubmit={onReview} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-stage-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-stage-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setReviewed(false);
              setSuccess(null);
            }}
            data-testid="item-stage-id"
            autoComplete="off"
          />
        </div>
        <fieldset
          style={{ border: "none", margin: 0, padding: 0 }}
          data-testid="item-stage-kind"
        >
          <legend style={{ fontSize: 13 }}>
            {message(MSG.PUBLISH_TAKEDOWN_KIND)}
          </legend>
          <label style={{ marginRight: 12, fontSize: 13 }}>
            <input
              type="radio"
              name="item-stage-kind"
              value="page"
              checked={kind === "page"}
              onChange={(e) => setKind(mapStageKind(e.target.value))}
              data-testid="item-stage-kind-page"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_PAGE)}
          </label>
          <label style={{ fontSize: 13 }}>
            <input
              type="radio"
              name="item-stage-kind"
              value="resource"
              checked={kind === "resource"}
              onChange={(e) => setKind(mapStageKind(e.target.value))}
              data-testid="item-stage-kind-resource"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_RESOURCE)}
          </label>
        </fieldset>
        <fieldset
          style={{ border: "none", margin: 0, padding: 0 }}
          data-testid="item-stage-action"
        >
          <legend style={{ fontSize: 13 }}>
            {message(MSG.PUBLISH_STAGE_ACTION)}
          </legend>
          <label style={{ marginRight: 12, fontSize: 13 }}>
            <input
              type="radio"
              name="item-stage-action"
              value="stage"
              checked={action === "stage"}
              onChange={(e) => setAction(mapStageAction(e.target.value))}
              data-testid="item-stage-action-stage"
            />{" "}
            {message(MSG.PUBLISH_STAGE_ACTION_STAGE)}
          </label>
          <label style={{ fontSize: 13 }}>
            <input
              type="radio"
              name="item-stage-action"
              value="unstage"
              checked={action === "unstage"}
              onChange={(e) => setAction(mapStageAction(e.target.value))}
              data-testid="item-stage-action-unstage"
            />{" "}
            {message(MSG.PUBLISH_STAGE_ACTION_UNSTAGE)}
          </label>
        </fieldset>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-stage-review"
          disabled={submitting}
        >
          {message(MSG.PUBLISH_STAGE_REVIEW)}
        </button>
      </form>
      {reviewed && (
        <div data-testid="item-stage-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13 }}>{confirmText}</p>
          <form onSubmit={(e) => void onSubmit(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-stage-submit"
              disabled={submitting}
            >
              {submitText}
            </button>
          </form>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-stage-error">
          {error}
        </p>
      )}
      {success && (
        <p
          data-testid="item-stage-success"
          style={{ color: "#166534", fontSize: 13 }}
        >
          {success}
        </p>
      )}
    </section>
  );
}
