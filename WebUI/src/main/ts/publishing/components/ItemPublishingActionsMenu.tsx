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
import { isSessionRedirectError } from "../../api/client";
import { fetchItemPublishingActions } from "../../api/publishing/itemPublishingActionsApi";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  actionTargetForName,
  PUBLISHING_ACTION_PANEL_TESTID,
  publishingActionsErrorMessage,
  safeActionsItemId,
  type PublishingAction,
} from "../itemPublishingActions";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface ItemPublishingActionsMenuProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

/**
 * Server-driven actions menu for the site workspace (issue #4581).
 *
 * <p>Loads {@code publishingActions} for the selected item and renders one
 * button per server row. Rows the server marks unavailable render disabled
 * (with {@code aria-disabled}); enabled rows navigate to the already-shipped
 * panels (publish now, schedule, takedown, stage) for that item. Unknown
 * action names are skipped. HTTP 403/404 surface as errors, never as an
 * empty success.</p>
 */
export function ItemPublishingActionsMenu({
  itemId,
  onItemIdChange,
}: ItemPublishingActionsMenuProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [actions, setActions] = useState<PublishingAction[] | null>(null);
  const [loadedFor, setLoadedFor] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (rawId: string): Promise<void> => {
    const safe = safeActionsItemId(rawId);
    if (!safe) {
      setActions(null);
      setLoadedFor(null);
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const rows = await fetchItemPublishingActions(safe);
      setActions(rows);
      setLoadedFor(safe);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setActions(null);
      setLoadedFor(null);
      setError(publishingActionsErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  // Follow the selected workspace item (deep links set itemId like peers).
  useEffect(() => {
    setInputId(itemId ?? "");
    const safe = safeActionsItemId(itemId);
    if (safe && safe !== loadedFor) {
      void load(itemId ?? "");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemId]);

  function onLoad(e: React.FormEvent): void {
    e.preventDefault();
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void load(inputId);
  }

  function gotoPanel(targetTestId: string): void {
    if (typeof document === "undefined") {
      return;
    }
    const el = document.querySelector(`[data-testid="${targetTestId}"]`);
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView();
    }
  }

  const visibleActions = (actions ?? []).filter(
    (a) => actionTargetForName(a.name) !== null,
  );

  return (
    <section
      data-testid="item-publishing-actions"
      aria-labelledby="item-publishing-actions-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-publishing-actions-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_ACTIONS)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_ACTIONS_HINT)}
      </p>
      <form onSubmit={onLoad} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-publishing-actions-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-publishing-actions-id"
            name="itemId"
            value={inputId}
            onChange={(e) => {
              setInputId(e.target.value);
              setError(null);
            }}
            data-testid="item-publishing-actions-id"
            autoComplete="off"
          />
        </div>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-publishing-actions-load"
          disabled={loading}
        >
          {message(MSG.PUBLISH_ACTIONS_LOAD)}
        </button>
      </form>
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert" data-testid="item-publishing-actions-error">
          {error}
        </p>
      )}
      {!loading && !error && actions !== null && visibleActions.length === 0 && (
        <p style={emptyStyle} data-testid="item-publishing-actions-empty">
          {message(MSG.PUBLISH_ACTIONS_EMPTY)}
        </p>
      )}
      {visibleActions.length > 0 && (
        <div
          style={{ ...toolbarStyle, marginTop: 12 }}
          role="toolbar"
          aria-label={message(MSG.PUBLISH_ACTIONS)}
          data-testid="item-publishing-actions-menu"
        >
          {visibleActions.map((action) => {
            const target = actionTargetForName(action.name);
            if (!target) {
              return null;
            }
            return (
              <button
                key={action.name}
                type="button"
                style={buttonStyle}
                disabled={!action.enabled || loading}
                aria-disabled={!action.enabled}
                title={
                  action.enabled
                    ? action.name
                    : `${action.name} — ${message(MSG.PUBLISH_ACTIONS_UNAVAILABLE)}`
                }
                data-testid={`item-action-${target}`}
                onClick={() => {
                  const safe = loadedFor ?? safeActionsItemId(inputId);
                  if (safe) {
                    onItemIdChange?.(safe);
                  }
                  gotoPanel(PUBLISHING_ACTION_PANEL_TESTID[target]);
                }}
              >
                {action.name}
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}
