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

import { findItemByPath } from "../api/contentExplorer/pathApi";
import { parseExplorerContentId } from "../contentExplorer/menuCatalogLoad";
import {
  EDITOR_WINDOW_FEATURES,
  buildEditorHostUrl,
  editorWindowName,
  type EditorHostMode,
} from "./editorHostUrl";

export interface OpenEditorHostInput {
  id?: string | number | null;
  path?: string | null;
  mode?: EditorHostMode;
}

export interface OpenEditorHostDeps {
  openWindow?: (
    url: string,
    target?: string,
    features?: string,
  ) => Window | null;
  findByPath?: (path: string) => Promise<{ id?: string | number }>;
  /**
   * Window opened on a user gesture (typically {@code about:blank}) so
   * navigation after an async create is not popup-blocked.
   */
  reservedWindow?: Window | null;
}

function defaultOpenWindow(
  url: string,
  target?: string,
  features?: string,
): Window | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.open(url, target, features);
}

/**
 * Resolve a host-relative editor URL against the opener, not against
 * {@code about:blank}. Path-only {@code location.href} assignment on a
 * reserved blank popup has no valid URL base, so the window stays blank
 * (Home Create asset Playwright gate).
 */
export function resolveEditorNavigationHref(
  url: string,
  baseHref?: string,
): string {
  const trimmed = String(url ?? "").trim();
  if (!trimmed) {
    return trimmed;
  }
  if (/^[a-zA-Z][a-zA-Z+\-.]*:/.test(trimmed)) {
    return trimmed;
  }
  const base =
    baseHref ??
    (typeof window !== "undefined" && window.location?.href
      ? window.location.href
      : "http://localhost/");
  try {
    return new URL(trimmed, base).href;
  } catch {
    return trimmed;
  }
}

/**
 * Open a placeholder popup on the current user gesture. Navigate it later
 * via {@link openEditorHost} {@code reservedWindow} so async create is not
 * popup-blocked.
 */
export function reserveEditorWindow(
  openWindow: NonNullable<OpenEditorHostDeps["openWindow"]> = defaultOpenWindow,
): Window | null {
  return openWindow("about:blank", "_blank", EDITOR_WINDOW_FEATURES);
}

export function closeReservedWindow(reserved: Window | null | undefined): void {
  if (!reserved || reserved.closed) {
    return;
  }
  try {
    reserved.close();
  } catch {
    // ignore
  }
}

function navigateReservedWindow(reserved: Window, url: string): Window | null {
  const href = resolveEditorNavigationHref(url);
  if (!href) {
    return null;
  }
  try {
    const loc = reserved.location;
    if (loc && typeof loc.assign === "function") {
      loc.assign(href);
    } else if (loc) {
      loc.href = href;
    } else {
      return null;
    }
    reserved.focus?.();
    return reserved;
  } catch {
    return null;
  }
}

/**
 * Open the React Content Editor host for an item id or CMS path.
 * Does not navigate to leftover {@code ?view=editor}.
 */
export async function openEditorHost(
  input: OpenEditorHostInput,
  deps: OpenEditorHostDeps = {},
): Promise<boolean> {
  let contentId = parseExplorerContentId(input.id ?? undefined);
  const path = input.path != null ? String(input.path).trim() : "";
  if (contentId == null && path) {
    const findByPath = deps.findByPath ?? findItemByPath;
    try {
      const item = await findByPath(path);
      contentId = parseExplorerContentId(item?.id);
    } catch {
      return false;
    }
  }
  if (contentId == null) {
    return false;
  }
  const url = resolveEditorNavigationHref(
    buildEditorHostUrl(contentId, input.mode ?? "edit"),
  );
  const reserved = deps.reservedWindow;
  if (reserved && !reserved.closed) {
    const navigated = navigateReservedWindow(reserved, url);
    if (navigated != null) {
      return true;
    }
  }
  const opened = (deps.openWindow ?? defaultOpenWindow)(
    url,
    editorWindowName(contentId),
    EDITOR_WINDOW_FEATURES,
  );
  return opened != null;
}
