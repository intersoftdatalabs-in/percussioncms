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
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../../api/client";
import {
  formatTakedownConfirmBody,
  linkedPagePathsForConfirm,
  loadLinkedPagesForTakedown,
  takedownSelectedItem,
  type LinkedPageForTakedown,
} from "../../contentExplorer/itemPublish";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  mapTakedownKind,
  pathItemForTakedown,
  type TakedownKind,
} from "../itemTakedown";
import {
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface ItemTakedownPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

function takedownErrorMessage(err: unknown): string {
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
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_CONFLICT));
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
  if (/\b409\b|\bCONFLICT\b|checked out|editing this/i.test(text)) {
    return message(MSG.PUBLISH_SCHEDULE_CONFLICT);
  }
  return text;
}

export function ItemTakedownPanel({
  itemId,
  onItemIdChange,
}: ItemTakedownPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [kind, setKind] = useState<TakedownKind>("page");
  const [linked, setLinked] = useState<LinkedPageForTakedown[]>([]);
  const [reviewed, setReviewed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setInputId(itemId ?? "");
  }, [itemId]);

  const review = useCallback(async (rawId: string) => {
    const safe = mapIdParam(rawId);
    if (!safe) {
      setLinked([]);
      setReviewed(false);
      setSuccess(null);
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      // Linked-list failures return [] (classic Finder still confirms).
      const pages = await loadLinkedPagesForTakedown(safe);
      setLinked(pages);
      setReviewed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initial = mapIdParam(itemId);
    if (initial) {
      void review(initial);
    }
  }, [itemId, review]);

  function onLookup(e: React.FormEvent): void {
    e.preventDefault();
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void review(next);
  }

  async function onTakeDown(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    const safe = mapIdParam(inputId) || mapIdParam(itemId);
    if (!safe) {
      setError(message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID));
      setSuccess(null);
      return;
    }
    if (!reviewed) {
      await review(safe);
      return;
    }
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const ok = await takedownSelectedItem(
        pathItemForTakedown(safe, kind),
        linked,
      );
      if (!ok) {
        setError(message(MSG.PUBLISH_ERROR));
        return;
      }
      onItemIdChange?.(safe);
      setSuccess(message(MSG.PUBLISH_TAKEDOWN_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(takedownErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  const linkedPaths = linkedPagePathsForConfirm(linked);

  return (
    <section
      data-testid="item-takedown"
      aria-labelledby="item-takedown-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-takedown-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_TAKEDOWN)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_TAKEDOWN_HINT)}
      </p>
      <form onSubmit={onLookup} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-takedown-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-takedown-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setReviewed(false);
              setSuccess(null);
            }}
            data-testid="item-takedown-id"
            autoComplete="off"
          />
        </div>
        <fieldset
          style={{ border: "none", margin: 0, padding: 0 }}
          data-testid="item-takedown-kind"
        >
          <legend style={{ fontSize: 13 }}>
            {message(MSG.PUBLISH_TAKEDOWN_KIND)}
          </legend>
          <label style={{ marginRight: 12, fontSize: 13 }}>
            <input
              type="radio"
              name="item-takedown-kind"
              value="page"
              checked={kind === "page"}
              onChange={(e) => setKind(mapTakedownKind(e.target.value))}
              data-testid="item-takedown-kind-page"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_PAGE)}
          </label>
          <label style={{ fontSize: 13 }}>
            <input
              type="radio"
              name="item-takedown-kind"
              value="resource"
              checked={kind === "resource"}
              onChange={(e) => setKind(mapTakedownKind(e.target.value))}
              data-testid="item-takedown-kind-resource"
            />{" "}
            {message(MSG.PUBLISH_TAKEDOWN_KIND_RESOURCE)}
          </label>
        </fieldset>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-takedown-review"
          disabled={loading || submitting}
        >
          {message(MSG.PUBLISH_TAKEDOWN_LOAD)}
        </button>
      </form>
      {reviewed && (
        <div data-testid="item-takedown-confirm" style={{ marginTop: 12 }}>
          <p style={{ margin: "0 0 8px", fontSize: 13, whiteSpace: "pre-wrap" }}>
            {formatTakedownConfirmBody(linked)}
          </p>
          {linkedPaths.length > 0 ? (
            <ul data-testid="item-takedown-linked" style={{ margin: "0 0 12px" }}>
              {linkedPaths.map((path) => (
                <li key={path}>{path}</li>
              ))}
            </ul>
          ) : (
            <p data-testid="item-takedown-linked-empty" style={{ fontSize: 13 }}>
              {message(MSG.PUBLISH_TAKEDOWN_NONE)}
            </p>
          )}
          <form onSubmit={(e) => void onTakeDown(e)}>
            <button
              type="submit"
              style={primaryButtonStyle}
              data-testid="item-takedown-submit"
              disabled={loading || submitting}
            >
              {message(MSG.PUBLISH_TAKEDOWN_SUBMIT)}
            </button>
          </form>
        </div>
      )}
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-takedown-error">
          {error}
        </p>
      )}
      {success && (
        <p
          data-testid="item-takedown-success"
          style={{ color: "#166534", fontSize: 13 }}
        >
          {success}
        </p>
      )}
    </section>
  );
}
