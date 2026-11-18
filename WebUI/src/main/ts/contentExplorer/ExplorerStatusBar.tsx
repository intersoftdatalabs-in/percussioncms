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
 * Explorer status bar (#6517 / parent #4530).
 *
 * <p>DCE chrome parity: the Desktop Content Explorer always showed a
 * status bar under the list (selection counts, folder/item counts,
 * operation feedback, user/context readout). The modern shell had header
 * chrome only. This footer closes that gap with the same icon+text
 * affordance as the toolbar (#6515): glyphs resolve through the shared
 * {@code ActionIcon} pipeline, labels stay text so nothing is icon-only.</p>
 *
 * <p>Accessibility: the footer is {@code role="contentinfo"} with an
 * i18n {@code aria-label}; the operation segment is
 * {@code role="status"} + {@code aria-live="polite"} so success/error
 * feedback is announced without moving focus. The bar renders after the
 * main list in DOM order so keyboard and screen-reader order match the
 * visual order (header → list → status).</p>
 */

import React from "react";
import { message } from "../i18n/message";
import { ActionIcon } from "./actionIcons";
import { EXPLORER_MSG } from "./messages";

export interface StatusBarOperation {
  kind: "success" | "error";
  text: string;
}

export interface ExplorerStatusBarProps {
  /** Rows checked in the detail list (multi-select). */
  multiSelectedCount: number;
  /** True when a single row is the active selection. */
  hasSingleSelection: boolean;
  /** Label of the active selection (item name/title), when known. */
  selectedItemLabel?: string | null;
  /** Active folder path (CMS {@code /}-joined form), or null. */
  folderPath: string | null | undefined;
  /**
   * Rows in the active folder ({@code PSPagedResult.totalCount}), or
   * {@code null} while the list is loading / unavailable.
   */
  folderItemCount: number | null;
  /** Signed-in user name (bootstrap {@code userName}). */
  userName?: string | null;
  /** True when the session is an Admin user. */
  isAdmin?: boolean;
  /** Last menu/dispatch outcome; null renders the idle hint. */
  lastOperation?: StatusBarOperation | null;
}

const barStyle: React.CSSProperties = {
  gridArea: "status",
  display: "flex",
  alignItems: "center",
  gap: 16,
  flexWrap: "wrap",
  borderTop: "1px solid #cbd5e1",
  background: "#f8fafc",
  color: "#0f172a",
  padding: "4px 12px",
  fontSize: "0.8rem",
};

const segmentStyle: React.CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  gap: 4,
  minWidth: 0,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

const operationErrorStyle: React.CSSProperties = {
  ...segmentStyle,
  color: "#b00020",
};

function formatCount(template: string, count: number): string {
  return template.split("{count}").join(String(count));
}

function formatNamed(template: string, name: string, value: string): string {
  return template.split(name).join(value);
}

export function ExplorerStatusBar(
  props: ExplorerStatusBarProps,
): React.ReactElement {
  const {
    multiSelectedCount,
    hasSingleSelection,
    selectedItemLabel,
    folderPath,
    folderItemCount,
    userName,
    isAdmin,
    lastOperation,
  } = props;

  const selectionText =
    multiSelectedCount > 0
      ? formatCount(
          message(EXPLORER_MSG.STATUS_BAR_SELECTED_MULTI),
          multiSelectedCount,
        )
      : hasSingleSelection
        ? selectedItemLabel
          ? formatNamed(
              message(EXPLORER_MSG.STATUS_BAR_SELECTED_SINGLE_NAMED),
              "{name}",
              selectedItemLabel,
            )
          : message(EXPLORER_MSG.STATUS_BAR_SELECTED_SINGLE)
        : message(EXPLORER_MSG.STATUS_BAR_SELECTED_NONE);

  const folderText =
    folderPath == null || folderPath === ""
      ? message(EXPLORER_MSG.STATUS_BAR_FOLDER_NONE)
      : folderItemCount == null
        ? formatNamed(
            message(EXPLORER_MSG.STATUS_BAR_FOLDER_LOADING),
            "{path}",
            folderPath,
          )
        : folderItemCount === 0
          ? formatNamed(
              message(EXPLORER_MSG.STATUS_BAR_FOLDER_EMPTY),
              "{path}",
              folderPath,
            )
          : formatNamed(
              formatCount(
                message(EXPLORER_MSG.STATUS_BAR_FOLDER_COUNT),
                folderItemCount,
              ),
              "{path}",
              folderPath,
            );

  const userText = formatNamed(
    message(
      isAdmin
        ? EXPLORER_MSG.STATUS_BAR_USER_ADMIN
        : EXPLORER_MSG.STATUS_BAR_USER,
    ),
    "{user}",
    userName && userName.trim() ? userName : message(EXPLORER_MSG.STATUS_BAR_USER_UNKNOWN),
  );

  return (
    <footer
      data-testid="explorer-status-bar"
      role="contentinfo"
      aria-label={message(EXPLORER_MSG.STATUS_BAR_ARIA)}
      style={barStyle}
    >
      <span
        data-testid="explorer-status-bar-selection"
        style={segmentStyle}
      >
        <ActionIcon name="selected item" />
        <span>{selectionText}</span>
      </span>
      <span data-testid="explorer-status-bar-folder" style={segmentStyle}>
        <ActionIcon name="folder status" />
        <span>{folderText}</span>
      </span>
      <span
        data-testid="explorer-status-bar-operation"
        role="status"
        aria-live="polite"
        data-kind={lastOperation ? lastOperation.kind : "idle"}
        style={
          lastOperation?.kind === "error" ? operationErrorStyle : segmentStyle
        }
      >
        {lastOperation ? (
          <span>{lastOperation.text}</span>
        ) : (
          <span>{message(EXPLORER_MSG.STATUS_BAR_OPERATION_IDLE)}</span>
        )}
      </span>
      <span data-testid="explorer-status-bar-context" style={segmentStyle}>
        <ActionIcon name="community user" />
        <span>{userText}</span>
      </span>
    </footer>
  );
}
