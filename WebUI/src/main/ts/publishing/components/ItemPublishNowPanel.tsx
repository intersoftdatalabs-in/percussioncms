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
import { publishSelectedItem } from "../../contentExplorer/itemPublish";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  mapPublishNowKind,
  pathItemForPublishNow,
  type PublishNowKind,
} from "../itemPublishNow";
import {
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface ItemPublishNowPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
  onPublished?: () => void;
}

function publishNowErrorMessage(err: unknown): string {
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

export function ItemPublishNowPanel({
  itemId,
  onItemIdChange,
  onPublished,
}: ItemPublishNowPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [kind, setKind] = useState<PublishNowKind>("page");
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

  async function onPublish(e: React.FormEvent): Promise<void> {
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
      const ok = await publishSelectedItem(pathItemForPublishNow(safe, kind));
      if (!ok) {
        setError(message(MSG.PUBLISH_ERROR));
        return;
      }
      onItemIdChange?.(safe);
      setSuccess(message(MSG.PUBLISH_NOW_SUCCESS));
      onPublished?.();
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(publishNowErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section
      data-testid="item-publish-now"
      aria-labelledby="item-publish-now-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-publish-now-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_NOW)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_NOW_HINT)}
      </p>
      <form onSubmit={onReview} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-publish-now-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-publish-now-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setReviewed(false);
              setSuccess(null);
            }}
            data-testid="item-publish-now-id"
            autoComplete="off"
          />
        </div>
        <fieldset
          style={{ border: "none", margin: 0, padding: 0 }}
          data-testid="item-publish-now-kind"
        >
          <legend style={{ fontSize: 13 }}>
            {message(MSG.PUBLISH_TAKEDOWN_KIND)}
          </legend>
          <label style={{ marginRight: 12, fontSize: 13 }}>
            <input
              type="radio"
              name="item-publish-now-kind"
              value="page"
              checked={kind === "page"}
              onChange={(e) => setKind(mapPublishNowKind(e.target.value))}
              data-testid="item-publish-now-kind-page"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_PAGE)}
          </label>
          <label style={{ fontSize: 13 }}>
            <input
              type="radio"
              name="item-publish-now-kind"
              value="resource"
              checked={kind === "resource"}
              onChange={(e) => setKind(mapPublishNowKind(e.target.value))}
              data-testid="item-publish-now-kind-resource"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_RESOURCE)}
          </label>
        </fieldset>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-publish-now-review"
          disabled={submitting}
        >
          {message(MSG.PUBLISH_NOW_REVIEW)}
        </button>
      </form>
      {reviewed && (
        <div data-testid="item-publish-now-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13 }}>
            {message(MSG.PUBLISH_NOW_CONFIRM)}
          </p>
          <form onSubmit={(e) => void onPublish(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-publish-now-submit"
              disabled={submitting}
            >
              {message(MSG.PUBLISH_NOW_SUBMIT)}
            </button>
          </form>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-publish-now-error">
          {error}
        </p>
      )}
      {success && (
        <p
          data-testid="item-publish-now-success"
          style={{ color: "#166534", fontSize: 13 }}
        >
          {success}
        </p>
      )}
    </section>
  );
}
