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
 * Explorer breadcrumb wayfinding bar (#6468 / parent #4530).
 *
 * <p>Renders the active folder as clickable segments so operators can step
 * back to any ancestor without reopening the tree. Long trails collapse the
 * middle behind an overflow affordance (first + tail stay visible; the
 * hidden names ride on the overflow button label). A copy affordance writes
 * an {@code ExplorerRoute} deep link ({@code ?path=…}) for the open folder
 * so it can be pasted into tickets and chat.</p>
 */

import React, { useEffect, useState } from "react";
import { message } from "../i18n/message";
import {
  buildExplorerDeepLink,
  collapseBreadcrumbSegments,
  splitBreadcrumbSegments,
  EXPLORER_BREADCRUMB_MAX_VISIBLE,
} from "./breadcrumbPath";
import { EXPLORER_MSG } from "./messages";

export interface ExplorerBreadcrumbProps {
  /** Active Explorer folder path (CMS {@code /}-joined form). */
  folderPath: string | null | undefined;
  /** Navigate the shell to an ancestor folder path. */
  onNavigate: (path: string) => void;
  /** Test seam: page URL the deep link is built from. */
  pageUrl?: string | null;
  /** Test seam: clipboard writer (default {@code navigator.clipboard}). */
  writeClipboard?: (text: string) => Promise<void>;
  /** Test seam: collapse threshold (default 4 visible segments). */
  maxVisible?: number;
}

const crumbButtonStyle: React.CSSProperties = {
  background: "none",
  border: "none",
  padding: "2px 4px",
  cursor: "pointer",
  color: "#1d4ed8",
  textDecoration: "underline",
  fontSize: "0.85rem",
  maxWidth: 220,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

const currentCrumbStyle: React.CSSProperties = {
  padding: "2px 4px",
  fontSize: "0.85rem",
  fontWeight: 600,
  color: "#0f172a",
  maxWidth: 220,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

const separatorStyle: React.CSSProperties = {
  color: "#64748b",
  fontSize: "0.85rem",
  padding: "0 2px",
};

async function defaultWriteClipboard(text: string): Promise<void> {
  const clipboard = navigator?.clipboard;
  if (!clipboard || typeof clipboard.writeText !== "function") {
    throw new Error("clipboard unavailable");
  }
  await clipboard.writeText(text);
}

export function ExplorerBreadcrumb(props: ExplorerBreadcrumbProps): React.ReactElement {
  const {
    folderPath,
    onNavigate,
    pageUrl,
    writeClipboard = defaultWriteClipboard,
    maxVisible = EXPLORER_BREADCRUMB_MAX_VISIBLE,
  } = props;
  const [overflowOpen, setOverflowOpen] = useState(false);
  const [copyNotice, setCopyNotice] = useState<{
    kind: "success" | "error";
    reason: string;
    text: string;
  } | null>(null);

  const segments = splitBreadcrumbSegments(folderPath);
  const crumbs = collapseBreadcrumbSegments(segments, maxVisible);
  const fullPath = segments.length > 0 ? `/${segments.join("/")}` : "";
  // A new folder starts collapsed again so a stale expansion does not ride along.
  useEffect(() => {
    setOverflowOpen(false);
  }, [fullPath]);
  const overflowNames = (name: string[]): string =>
    message(EXPLORER_MSG.BREADCRUMB_OVERFLOW_ARIA_WITH_NAMES)
      .split("{names}")
      .join(name.join(", "));

  async function handleCopyLink(): Promise<void> {
    const href =
      pageUrl !== undefined && pageUrl !== null
        ? pageUrl
        : typeof window !== "undefined" && window.location?.href
          ? window.location.href
          : null;
    const link = buildExplorerDeepLink(href, folderPath);
    if (link == null) {
      setCopyNotice({
        kind: "error",
        reason: "empty",
        text: message(EXPLORER_MSG.BREADCRUMB_LINK_EMPTY),
      });
      return;
    }
    try {
      await writeClipboard(link);
    } catch {
      setCopyNotice({
        kind: "error",
        reason: "failed",
        text: message(EXPLORER_MSG.BREADCRUMB_LINK_FAILED),
      });
      return;
    }
    setCopyNotice({
      kind: "success",
      reason: "copied",
      text: `${message(EXPLORER_MSG.BREADCRUMB_LINK_COPIED)} ${link}`,
    });
  }

  function renderOverflow(names: string[], key: string): React.ReactElement {
    const label =
      names.length > 0
        ? overflowNames(names)
        : message(EXPLORER_MSG.BREADCRUMB_OVERFLOW_ARIA);
    return (
      <React.Fragment key={key}>
        <button
          type="button"
          data-testid="explorer-breadcrumb-overflow"
          data-hidden-names={names.join(", ")}
          aria-label={label}
          aria-expanded={overflowOpen}
          title={label}
          style={crumbButtonStyle}
          onClick={() => setOverflowOpen((open) => !open)}
        >
          {"…"}
        </button>
        {overflowOpen ? (
          <span data-testid="explorer-breadcrumb-overflow-list">
            {names.map((name, offset) => (
              <React.Fragment key={`hidden-${offset}-${name}`}>
                <span style={separatorStyle} aria-hidden="true">
                  {"/"}
                </span>
                <span data-testid="explorer-breadcrumb-hidden-segment">{name}</span>
              </React.Fragment>
            ))}
          </span>
        ) : null}
      </React.Fragment>
    );
  }

  return (
    <div>
      <nav
        data-testid="explorer-breadcrumb"
        data-full-path={fullPath}
        aria-label={message(EXPLORER_MSG.BREADCRUMB_ARIA)}
        style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 0 }}
      >
        <button
          type="button"
          data-testid="explorer-breadcrumb-home"
          aria-label={message(EXPLORER_MSG.BREADCRUMB_HOME_ARIA)}
          title={message(EXPLORER_MSG.BREADCRUMB_HOME_ARIA)}
          style={crumbButtonStyle}
          onClick={() => onNavigate("/")}
        >
          {message(EXPLORER_MSG.BREADCRUMB_HOME)}
        </button>
        {crumbs.map((crumb, position) => {
          if (crumb.kind === "overflow") {
            return renderOverflow(crumb.names, `overflow-${position}`);
          }
          const isCurrent = position === crumbs.length - 1;
          return (
            <React.Fragment key={`segment-${crumb.index}-${crumb.name}`}>
              <span style={separatorStyle} aria-hidden="true">
                {"/"}
              </span>
              {isCurrent ? (
                <span
                  data-testid="explorer-breadcrumb-current"
                  data-path={crumb.path}
                  aria-current="page"
                  title={fullPath}
                  style={currentCrumbStyle}
                >
                  {crumb.name}
                </span>
              ) : (
                <button
                  type="button"
                  data-testid="explorer-breadcrumb-segment"
                  data-path={crumb.path}
                  data-index={String(crumb.index)}
                  title={crumb.path}
                  style={crumbButtonStyle}
                  onClick={() => onNavigate(crumb.path)}
                >
                  {crumb.name}
                </button>
              )}
            </React.Fragment>
          );
        })}
        <span style={{ ...separatorStyle, padding: "0 6px" }} aria-hidden="true">
          {"·"}
        </span>
        <button
          type="button"
          data-testid="explorer-breadcrumb-copy-link"
          aria-label={message(EXPLORER_MSG.BREADCRUMB_COPY_LINK_ARIA)}
          title={message(EXPLORER_MSG.BREADCRUMB_COPY_LINK_ARIA)}
          style={crumbButtonStyle}
          onClick={() => void handleCopyLink()}
        >
          {message(EXPLORER_MSG.BREADCRUMB_COPY_LINK)}
        </button>
      </nav>
      {copyNotice ? (
        <div
          data-testid="explorer-breadcrumb-copy-status"
          data-kind={copyNotice.kind}
          data-reason={copyNotice.reason}
          role="status"
          aria-live="polite"
        >
          {copyNotice.text}
        </div>
      ) : null}
    </div>
  );
}
