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
 * Explorer item properties panel (#4701): name + display title save.
 */

import React, { useEffect, useState } from "react";
import {
  getItemProperties,
  saveItemProperties,
} from "../api/contentExplorer/pathApi";
import { message } from "../i18n/message";
import { formatItemPropertiesError } from "./itemPropertiesErrors";
import { EXPLORER_MSG } from "./messages";

export interface ItemPropertiesPanelProps {
  itemPath: string;
  itemName?: string;
  /** When false, inputs and save are disabled (view-only). */
  canEdit: boolean;
  load?: typeof getItemProperties;
  save?: typeof saveItemProperties;
  onSaved?: (name: string) => void;
}

type Status =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; name: string; displayTitle: string; dirty: boolean };

export function ItemPropertiesPanel(
  props: ItemPropertiesPanelProps,
): React.JSX.Element {
  const {
    itemPath,
    itemName,
    canEdit,
    load = getItemProperties,
    save = saveItemProperties,
    onSaved,
  } = props;
  const [status, setStatus] = useState<Status>({ kind: "loading" });
  const [pending, setPending] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setStatus({ kind: "loading" });
    setSaveMessage(null);
    void (async () => {
      try {
        const loaded = await load(itemPath);
        if (cancelled) return;
        setStatus({
          kind: "ready",
          name: String(loaded.name ?? itemName ?? "").trim(),
          displayTitle: String(loaded.displayTitle ?? ""),
          dirty: false,
        });
      } catch (err) {
        if (cancelled) return;
        setStatus({ kind: "error", message: formatItemPropertiesError(err) });
      }
    })();
    return () => {
      cancelled = true;
    };
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

  const handleSave = (): void => {
    const name = status.name.trim();
    if (!name) {
      setSaveMessage(message(EXPLORER_MSG.ITEM_PROPS_BAD_REQUEST));
      return;
    }
    setPending(true);
    setSaveMessage(null);
    void (async () => {
      try {
        await save({
          itemPath,
          name,
          displayTitle: status.displayTitle,
        });
        setStatus({ ...status, name, dirty: false });
        setSaveMessage(message(EXPLORER_MSG.ITEM_PROPS_SAVE_SUCCESS));
        onSaved?.(name);
      } catch (err) {
        setSaveMessage(formatItemPropertiesError(err));
      } finally {
        setPending(false);
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
