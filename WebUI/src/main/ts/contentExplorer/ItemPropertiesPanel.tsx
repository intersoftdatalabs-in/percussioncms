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
 * Explorer item properties panel (#4701, #5246, #5297): name + display title.
 *
 * <p>The saved display title is the value returned by a reload after POST.
 * Clear empties the draft and does not post. Save of that empty title keeps
 * the name and shows an empty title only after reload. Cancel does not post.
 * HTTP 400, 403, and 409 put the previous display title back. A non-empty
 * title still saves. Folders are not offered clear.</p>
 */

import React, { useEffect, useRef, useState } from "react";
import { isApiError } from "../api/client";
import {
  getItemProperties,
  saveItemProperties,
} from "../api/contentExplorer/pathApi";
import { message } from "../i18n/message";
import { formatItemPropertiesError } from "./itemPropertiesErrors";
import { EXPLORER_MSG } from "./messages";
import {
  committedDisplayTitleAfterAttempt,
  displayTitleDraftAfterClear,
  displayTitleDraftAfterFailure,
  itemPropertiesPathAfterSave,
  planItemPropertiesSave,
} from "./setItemDisplayTitle";

export interface ItemPropertiesPanelProps {
  itemPath: string;
  itemName?: string;
  /** When false, inputs and save are disabled (view-only). */
  canEdit: boolean;
  /**
   * When false, Clear display title is not rendered. Folders pass false.
   * Defaults to true for an editable page, file, or asset.
   */
  allowClearDisplayTitle?: boolean;
  load?: typeof getItemProperties;
  save?: typeof saveItemProperties;
  onSaved?: (name: string) => void;
}

type Status =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | {
      kind: "ready";
      name: string;
      displayTitle: string;
      committedName: string;
      committedDisplayTitle: string;
      dirty: boolean;
    };

export function ItemPropertiesPanel(
  props: ItemPropertiesPanelProps,
): React.JSX.Element {
  const {
    itemPath,
    itemName,
    canEdit,
    allowClearDisplayTitle = true,
    load = getItemProperties,
    save = saveItemProperties,
    onSaved,
  } = props;
  const [status, setStatus] = useState<Status>({ kind: "loading" });
  const [pending, setPending] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const sessionRef = useRef(0);

  useEffect(() => {
    const token = ++sessionRef.current;
    setStatus({ kind: "loading" });
    setSaveMessage(null);
    setPending(false);
    void (async () => {
      try {
        const loaded = await load(itemPath);
        if (sessionRef.current !== token) return;
        const name = String(loaded.name ?? itemName ?? "").trim();
        const displayTitle = String(loaded.displayTitle ?? "");
        setStatus({
          kind: "ready",
          name,
          displayTitle,
          committedName: name,
          committedDisplayTitle: displayTitle,
          dirty: false,
        });
      } catch (err) {
        if (sessionRef.current !== token) return;
        setStatus({ kind: "error", message: formatItemPropertiesError(err) });
      }
    })();
  }, [itemPath, itemName, load]);

  if (status.kind === "loading") {
    return (
      <div data-testid="item-properties-loading" role="status">
        {message(EXPLORER_MSG.ITEM_PROPS_LOADING)}
      </div>
    );
  }
  if (status.kind === "error") {
    return (
      <div data-testid="item-properties-error" role="alert">
        {status.message}
      </div>
    );
  }

  const handleCancel = (): void => {
    if (pending) {
      return;
    }
    const previous = status.committedDisplayTitle;
    setStatus({
      ...status,
      name: status.committedName,
      displayTitle: committedDisplayTitleAfterAttempt(previous, {
        outcome: "cancelled",
      }),
      dirty: false,
    });
    setSaveMessage(null);
  };

  const handleClearDisplayTitle = (): void => {
    if (pending || !canEdit || !allowClearDisplayTitle) {
      return;
    }
    const cleared = displayTitleDraftAfterClear({
      committedName: status.committedName,
      draftName: status.name,
      committedDisplayTitle: status.committedDisplayTitle,
    });
    setStatus({
      ...status,
      displayTitle: cleared.displayTitle,
      dirty: cleared.dirty,
    });
    setSaveMessage(null);
  };

  const handleSave = (): void => {
    const plan = planItemPropertiesSave({
      itemPath,
      loadedName: status.committedName,
      draftName: status.name,
      draftDisplayTitle: status.displayTitle,
    });
    if (!plan.ok) {
      setSaveMessage(message(EXPLORER_MSG.ITEM_PROPS_BAD_REQUEST));
      return;
    }
    const token = sessionRef.current;
    const previousTitle = status.committedDisplayTitle;
    setPending(true);
    setSaveMessage(null);
    void (async () => {
      try {
        await save({
          itemPath: plan.itemPath,
          name: plan.name,
          displayTitle: plan.displayTitle,
        });
        if (sessionRef.current !== token) {
          return;
        }
        let reloaded: { name?: string; displayTitle?: string | null };
        const reloadPath = itemPropertiesPathAfterSave(
          plan.itemPath,
          plan.name,
          plan.nameChanged,
        );
        try {
          reloaded = await load(reloadPath);
        } catch (err) {
          if (sessionRef.current !== token) {
            return;
          }
          setStatus((current) =>
            current.kind === "ready"
              ? {
                  ...current,
                  displayTitle: committedDisplayTitleAfterAttempt(
                    current.committedDisplayTitle,
                    { outcome: "reload-failed" },
                  ),
                  dirty: current.name.trim() !== current.committedName,
                }
              : current,
          );
          setSaveMessage(formatItemPropertiesError(err));
          return;
        }
        if (sessionRef.current !== token) {
          return;
        }
        const nextName = String(reloaded.name ?? plan.name).trim();
        const nextTitle = committedDisplayTitleAfterAttempt(previousTitle, {
          outcome: "saved",
          reloadedTitle: String(reloaded.displayTitle ?? ""),
        });
        setStatus({
          kind: "ready",
          name: nextName,
          displayTitle: nextTitle,
          committedName: nextName,
          committedDisplayTitle: nextTitle,
          dirty: false,
        });
        setSaveMessage(message(EXPLORER_MSG.ITEM_PROPS_SAVE_SUCCESS));
        onSaved?.(nextName);
      } catch (err) {
        if (sessionRef.current !== token) {
          return;
        }
        const http = isApiError(err) ? err.status : undefined;
        setStatus((current) => {
          if (current.kind !== "ready") {
            return current;
          }
          const displayTitle = displayTitleDraftAfterFailure(
            current.committedDisplayTitle,
            current.displayTitle,
            http,
          );
          return {
            ...current,
            displayTitle,
            dirty: current.name.trim() !== current.committedName,
          };
        });
        setSaveMessage(formatItemPropertiesError(err));
      } finally {
        if (sessionRef.current === token) {
          setPending(false);
        }
      }
    })();
  };

  return (
    <form
      data-testid="item-properties-panel"
      aria-label={message(EXPLORER_MSG.ITEM_PROPS_PANEL_REGION)}
      onSubmit={(e) => {
        e.preventDefault();
        if (canEdit && !pending) {
          handleSave();
        }
      }}
    >
      <h2>{message(EXPLORER_MSG.ITEM_PROPS_TITLE)}</h2>
      {!canEdit ? (
        <p data-testid="item-properties-readonly" role="status">
          {message(EXPLORER_MSG.ITEM_PROPS_READ_ONLY)}
        </p>
      ) : null}
      <div>
        <label htmlFor="item-props-name">
          {message(EXPLORER_MSG.ITEM_PROPS_NAME)}
        </label>
        <input
          id="item-props-name"
          data-testid="item-properties-name"
          type="text"
          value={status.name}
          disabled={!canEdit || pending}
          onChange={(e) =>
            setStatus({ ...status, name: e.target.value, dirty: true })
          }
        />
      </div>
      <p data-testid="item-properties-shown-display-title">
        {message(EXPLORER_MSG.ITEM_PROPS_SAVED_DISPLAY_TITLE)}{" "}
        <span data-testid="item-properties-shown-display-title-value">
          {status.committedDisplayTitle}
        </span>
      </p>
      <div>
        <label htmlFor="item-props-display-title">
          {message(EXPLORER_MSG.ITEM_PROPS_DISPLAY_TITLE)}
        </label>
        <input
          id="item-props-display-title"
          data-testid="item-properties-display-title"
          type="text"
          value={status.displayTitle}
          disabled={!canEdit || pending}
          onChange={(e) =>
            setStatus({
              ...status,
              displayTitle: e.target.value,
              dirty: true,
            })
          }
        />
      </div>
      {allowClearDisplayTitle && canEdit ? (
        <button
          type="button"
          data-testid="item-properties-clear-display-title"
          disabled={pending || status.displayTitle.trim() === ""}
          onClick={handleClearDisplayTitle}
        >
          {message(EXPLORER_MSG.ITEM_PROPS_CLEAR_DISPLAY_TITLE)}
        </button>
      ) : null}
      <button
        type="button"
        data-testid="item-properties-cancel"
        disabled={!canEdit || pending || !status.dirty}
        onClick={handleCancel}
      >
        {message(EXPLORER_MSG.ITEM_PROPS_CANCEL)}
      </button>
      <button
        type="submit"
        data-testid="item-properties-save"
        disabled={!canEdit || pending || !status.dirty}
      >
        {message(EXPLORER_MSG.ITEM_PROPS_SAVE)}
      </button>
      {saveMessage ? (
        <p data-testid="item-properties-status" role="status">
          {saveMessage}
        </p>
      ) : null}
    </form>
  );
}
