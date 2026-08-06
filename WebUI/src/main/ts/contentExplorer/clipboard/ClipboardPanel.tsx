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

/**
 * Clipboard panel for the modern Content Explorer (US7 / T075).
 *
 * <p>Renders the in-memory {@link Clipboard} state + per-item paste /
 * cut actions. The host (typically the explorer shell) feeds selected
 * items in via {@link ClipboardPanelProps.items} and a `mode` prop;
 * the panel wraps the {@code setClipboard} / {@code pasteClipboardItems}
 * helpers and surfaces the per-item paste summary.</p>
 *
 * <p>The component is split from the pure {@code model} module so
 * the model can be unit-tested without a DOM; the component layer
 * focuses on rendering and ARIA wiring.</p>
 */

import React, { useState } from "react";
import {
  pasteClipboardItems,
  type ClipboardPasteTransport,
} from "../../api/contentExplorer/clipboardApi";
import type {
  Clipboard,
  ClipboardItem,
  ClipboardPasteSummary,
} from "../../api/contentExplorer/types";
import { message } from "../../i18n/message";
import {
  canPasteInto,
  EMPTY_CLIPBOARD,
  isPasteFullySuccessful,
  setClipboard,
  size,
} from "./model";
import { EXPLORER_MSG } from "../messages";

export interface ClipboardPanelProps {
  /** Current clipboard state (host-controlled). */
  clipboard: Clipboard;
  /** Replace the clipboard state on copy / cut. */
  onClipboardChange: (cb: Clipboard) => void;
  /** Selection to copy / cut into the clipboard. */
  items: ReadonlyArray<ClipboardItem>;
  /** The current clipboard operation mode (Copy / Cut). */
  mode: "copy" | "cut";
  /** Reflect a mode change back to the host. */
  onModeChange: (mode: "copy" | "cut") => void;
  /**
   * Target folder the user picked to paste into. The host supplies
   * the folder metadata (path, accessLevel) — the panel uses it for
   * the FR-016 read-only-without-rights gate.
   */
  target?: { path: string; accessLevel?: ClipboardItem["sourceAccessLevel"] };
  /** Override the paste transport (default: per-kind REST). */
  paste?: ClipboardPasteTransport;
  /** Triggered after a successful paste so the host can refresh the tree / list. */
  onPasteSettled?: (summary: ClipboardPasteSummary) => void;
  ariaLabel?: string;
  className?: string;
}

export function ClipboardPanel(props: ClipboardPanelProps): React.JSX.Element {
  const {
    clipboard,
    onClipboardChange,
    items,
    mode,
    onModeChange,
    target,
    paste,
    onPasteSettled,
    ariaLabel,
    className,
  } = props;
  const [pending, setPending] = useState(false);
  const [lastSummary, setLastSummary] = useState<ClipboardPasteSummary | null>(
    null,
  );

  function handleAdd(): void {
    onClipboardChange(setClipboard(clipboard, mode, items));
  }

  function handleClear(): void {
    onClipboardChange(EMPTY_CLIPBOARD);
  }

  async function handlePaste(): Promise<void> {
    if (pending || clipboard.items.length === 0) return;
    setPending(true);
    try {
      const summary = await pasteClipboardItems(
        clipboard.items,
        clipboard.operation,
        paste,
      );
      setLastSummary(summary);
      onPasteSettled?.(summary);
      if (isPasteFullySuccessful(summary)) {
        onClipboardChange(EMPTY_CLIPBOARD);
      }
    } finally {
      setPending(false);
    }
  }

  const cb: Clipboard = clipboard
    ? clipboard
    : { operation: "copy", items: [], updatedAt: new Date().toISOString() };
  const safeItems: ReadonlyArray<ClipboardItem> = Array.isArray(items)
    ? items
    : [];
  const pasteAllowed =
    target && typeof target.accessLevel === "string"
      ? canPasteInto(cb, target.accessLevel)
      : false;

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.CLIPBOARD_TITLE)}
      data-testid="clipboard-panel"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <header style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <strong>{message(EXPLORER_MSG.CLIPBOARD_TITLE)}</strong>
        <span
          aria-live="polite"
          data-testid="clipboard-size"
          style={{ color: "#888" }}
        >
          ({size(cb)})
        </span>
      </header>
      <fieldset style={{ display: "flex", gap: 4, marginTop: 8 }}>
        <legend style={{ display: "none" }}>{message(EXPLORER_MSG.CLIPBOARD_MODE_LABEL)}</legend>
        <label>
          <input
            type="radio"
            name="clipboard-mode"
            value="copy"
            data-testid="clipboard-mode-copy"
            checked={mode === "copy"}
            onChange={() => onModeChange("copy")}
          />
          {message(EXPLORER_MSG.CLIPBOARD_MODE_COPY)}
        </label>
        <label>
          <input
            type="radio"
            name="clipboard-mode"
            value="cut"
            data-testid="clipboard-mode-cut"
            checked={mode === "cut"}
            onChange={() => onModeChange("cut")}
          />
          {message(EXPLORER_MSG.CLIPBOARD_MODE_CUT)}
        </label>
      </fieldset>
      <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
        <button
          type="button"
          disabled={safeItems.length === 0}
          onClick={handleAdd}
          data-testid="clipboard-add"
        >
          {message(EXPLORER_MSG.CLIPBOARD_ADD)}
        </button>
        <button
          type="button"
          disabled={cb.items.length === 0}
          onClick={handleClear}
          data-testid="clipboard-clear"
        >
          {message(EXPLORER_MSG.CLIPBOARD_CLEAR)}
        </button>
        <button
          type="button"
          disabled={!pasteAllowed || pending || cb.items.length === 0}
          onClick={() => void handlePaste()}
          data-testid="clipboard-paste"
        >
          {message(EXPLORER_MSG.CLIPBOARD_PASTE)}
        </button>
      </div>
      <ul
        data-testid="clipboard-items"
        style={{ listStyle: "none", padding: 0, margin: "8px 0 0 0" }}
      >
        {cb.items.map((it, idx) => (
          <li
            key={`${it.path}-${idx}`}
            data-testid="clipboard-item-row"
            style={{ padding: "2px 0" }}
          >
            <code style={{ fontFamily: "monospace" }}>{it.path}</code>
            <small style={{ marginLeft: 6, color: "#888" }}>
              ({it.kind})
            </small>
          </li>
        ))}
      </ul>
      {lastSummary ? (
        <SummaryView summary={lastSummary} />
      ) : null}
    </section>
  );
}

function SummaryView(props: { summary: ClipboardPasteSummary }): React.JSX.Element {
  const { summary } = props;
  const okCount = summary.results.filter((r) => r.ok).length;
  const failCount = summary.results.length - okCount;
  return (
    <div
      role="status"
      aria-live="polite"
      data-testid="clipboard-summary"
      style={{
        marginTop: 8,
        padding: 8,
        background: failCount === 0 ? "#e8f5e9" : "#fff3cd",
      }}
    >
      <strong>
        {summary.operation.toUpperCase()}: {okCount}/{summary.results.length} ok
      </strong>
      {failCount > 0 ? (
        <ul
          style={{ marginTop: 4, fontSize: "0.85rem" }}
          data-testid="clipboard-summary-failures"
        >
          {summary.results
            .filter((r) => !r.ok)
            .map((r, idx) => (
              <li
                key={`${r.item.path}-${idx}`}
                data-testid={`clipboard-summary-failure-${idx}`}
                data-conflict={r.status === 409 ? "true" : undefined}
              >
                <code>{r.item.path}</code>: {r.message}
              </li>
            ))}
        </ul>
      ) : null}
    </div>
  );
}
