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
 * Dual-run feature flag for Content Explorer folder mutations (#3074 / parent #3054).
 *
 * <p>When <strong>off</strong> (default), Explorer create/rename/move/delete stay on
 * pathmanagement ({@code pathApi.ts}) — zero behavior change.</p>
 *
 * <p>When <strong>on</strong>, mutations under RX-capable roots ({@code /Folders},
 * {@code /Sites} and repository forms) use the content-explorer folders REST façade
 * ({@code /Rhythmyx/rest/content-explorer/folders}). Browse / list / pagination remain
 * on pathmanagement.</p>
 *
 * <h3>Resolution order (first hit wins)</h3>
 * <ol>
 *   <li>Test / programmatic override via {@link setRxFolderMutationsFlagOverride}</li>
 *   <li>URL query {@code ?rxFolderMutations=1|true|on} (or {@code =0|false|off} to force off)</li>
 *   <li>{@code sessionStorage} then {@code localStorage} key {@link RX_FOLDER_MUTATIONS_STORAGE_KEY}</li>
 *   <li>{@code window.__PERC_RX_FOLDER_MUTATIONS__} (boolean or string)</li>
 *   <li>Default {@code false}</li>
 * </ol>
 *
 * <p>Operator-facing name (product-docs / {@code server.properties} comment):
 * {@code perc.explorer.rxFolderMutations}. Server bootstrap injection is reserved;
 * client dual-run uses the Explorer URL/storage pattern (peers: {@code legacyDashboard},
 * {@code mkdLang}).</p>
 */

/** Storage / property key shared with product-docs and server.properties comments. */
export const RX_FOLDER_MUTATIONS_STORAGE_KEY = "perc.explorer.rxFolderMutations";

/** URL query param name for opt-in dual-run during QA. */
export const RX_FOLDER_MUTATIONS_QUERY_PARAM = "rxFolderMutations";

/** Window global for hosts / tests. */
export const RX_FOLDER_MUTATIONS_WINDOW_KEY = "__PERC_RX_FOLDER_MUTATIONS__";

let override: boolean | null = null;

/**
 * Force the flag for unit tests (or host harness). Pass {@code null} to clear.
 */
export function setRxFolderMutationsFlagOverride(value: boolean | null): void {
  override = value;
}

/** @returns current override, or {@code null} when using live resolution. */
export function getRxFolderMutationsFlagOverride(): boolean | null {
  return override;
}

function parseTruthy(raw: string | null | undefined): boolean | null {
  if (raw == null) {
    return null;
  }
  const v = String(raw).trim().toLowerCase();
  if (v === "" || v === "0" || v === "false" || v === "off" || v === "no") {
    return false;
  }
  if (v === "1" || v === "true" || v === "on" || v === "yes") {
    return true;
  }
  return null;
}

function readStorage(store: Storage | undefined): boolean | null {
  if (!store) {
    return null;
  }
  try {
    return parseTruthy(store.getItem(RX_FOLDER_MUTATIONS_STORAGE_KEY));
  } catch {
    return null;
  }
}

function readWindowGlobal(): boolean | null {
  try {
    if (typeof window === "undefined") {
      return null;
    }
    const w = window as unknown as Record<string, unknown>;
    const raw = w[RX_FOLDER_MUTATIONS_WINDOW_KEY];
    if (typeof raw === "boolean") {
      return raw;
    }
    if (typeof raw === "string") {
      return parseTruthy(raw);
    }
  } catch {
    /* ignore */
  }
  return null;
}

function readQueryParamFromSearch(search: string | null | undefined): boolean | null {
  if (search == null || search === "") {
    return null;
  }
  try {
    const raw = search.startsWith("?") ? search.slice(1) : search;
    const params = new URLSearchParams(raw);
    if (!params.has(RX_FOLDER_MUTATIONS_QUERY_PARAM)) {
      return null;
    }
    return parseTruthy(params.get(RX_FOLDER_MUTATIONS_QUERY_PARAM));
  } catch {
    return null;
  }
}

function readQueryParam(): boolean | null {
  try {
    if (typeof window === "undefined" || !window.location?.search) {
      return null;
    }
    return readQueryParamFromSearch(window.location.search);
  } catch {
    return null;
  }
}

function writeSessionFlag(value: boolean): void {
  try {
    if (typeof sessionStorage === "undefined") {
      return;
    }
    sessionStorage.setItem(RX_FOLDER_MUTATIONS_STORAGE_KEY, value ? "true" : "false");
  } catch {
    /* private mode */
  }
}

/**
 * Persist {@code ?rxFolderMutations=} from a spa.jsp entry URL before the SPA
 * rewrites to a path route (which would otherwise drop the diagnostic query).
 *
 * @returns parsed flag, or {@code null} when the param is absent
 */
export function captureRxFolderMutationsFromSearch(
  search: string | null | undefined = typeof window !== "undefined"
    ? window.location.search
    : "",
): boolean | null {
  const parsed = readQueryParamFromSearch(search);
  if (parsed !== null) {
    writeSessionFlag(parsed);
  }
  return parsed;
}

/**
 * Whether Explorer folder mutations should use content-explorer folders REST.
 *
 * <p>Default {@code false}. Safe for SSR / non-browser (returns false).</p>
 */
export function isRxFolderMutationsEnabled(): boolean {
  if (override !== null) {
    return override;
  }
  const fromQuery = readQueryParam();
  if (fromQuery !== null) {
    writeSessionFlag(fromQuery);
    return fromQuery;
  }
  try {
    if (typeof sessionStorage !== "undefined") {
      const fromSession = readStorage(sessionStorage);
      if (fromSession !== null) {
        return fromSession;
      }
    }
  } catch {
    /* private mode */
  }
  try {
    if (typeof localStorage !== "undefined") {
      const fromLocal = readStorage(localStorage);
      if (fromLocal !== null) {
        return fromLocal;
      }
    }
  } catch {
    /* private mode */
  }
  const fromWindow = readWindowGlobal();
  if (fromWindow !== null) {
    return fromWindow;
  }
  return false;
}

/**
 * Whether {@code lower} is {@code prefix} or a child segment of it.
 * Trailing slashes are ignored so {@code /Assets/} matches {@code /assets}.
 */
function isPrefixOrEqual(lower: string, prefix: string): boolean {
  let p = lower;
  while (p.length > 1 && p.endsWith("/")) {
    p = p.slice(0, -1);
  }
  return p === prefix || p.startsWith(`${prefix}/`);
}

/**
 * CM1 library roots that live under {@code //Folders/$System$} in the
 * repository but must stay on pathmanagement (#3363).
 *
 * <p>Explorer selection prefers {@code PathItem.folderPath}, so Assets is
 * often {@code /Folders/$System$/Assets} (or {@code //Folders/$System$/Assets})
 * rather than the finder id {@code /Assets}. A naive {@code /Folders/}
 * prefix would incorrectly send create/rename/move to the RX façade.</p>
 */
function isNonRxSystemLibraryPath(normalizedLower: string): boolean {
  return (
    isPrefixOrEqual(normalizedLower, "/assets") ||
    isPrefixOrEqual(normalizedLower, "/recycling") ||
    isPrefixOrEqual(normalizedLower, "/folders/$system$/assets") ||
    isPrefixOrEqual(normalizedLower, "/folders/$system$/recycling")
  );
}

/**
 * Whether a CMS path is under an RX-capable root that the content-explorer
 * folders façade is intended to serve ({@code Folders} / {@code Sites}).
 *
 * <p>Accepts finder form ({@code /Folders/...}, {@code /Sites/...}) and
 * repository form ({@code //Folders/...}, {@code //Sites/...}). Other roots
 * ({@code /Assets}, {@code /Design}, {@code /Recycling}, …) stay on
 * pathmanagement even when the dual-run flag is on.</p>
 *
 * <p>Assets (and Recycling) are also non-RX when represented as
 * {@code /Folders/$System$/Assets} or {@code //Folders/$System$/Assets}
 * (#3363). Sibling folders under {@code $System$} remain RX-capable.</p>
 */
export function isRxCapableFolderPath(path: string | null | undefined): boolean {
  if (path == null) {
    return false;
  }
  let p = String(path).trim().replace(/\\/g, "/");
  if (p.length === 0) {
    return false;
  }
  // Strip Windows drive letter if present.
  if (p.length >= 2 && /[A-Za-z]/.test(p.charAt(0)) && p.charAt(1) === ":") {
    p = p.slice(2);
  }
  // Collapse accidental triple+ leading slashes.
  while (p.startsWith("///")) {
    p = p.slice(1);
  }
  // Normalize to a form starting with / or //
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  // Strip one leading slash for comparison so //Folders and /Folders share logic.
  const withoutRepo = p.startsWith("//") ? p.slice(1) : p;
  const lower = withoutRepo.toLowerCase();
  if (isNonRxSystemLibraryPath(lower)) {
    return false;
  }
  return (
    lower === "/folders" ||
    lower.startsWith("/folders/") ||
    lower === "/sites" ||
    lower.startsWith("/sites/")
  );
}

/**
 * Dual-run gate for a specific mutation path: flag on <em>and</em> RX-capable root.
 */
export function shouldUseRxFolderMutations(
  path: string | null | undefined,
): boolean {
  return isRxFolderMutationsEnabled() && isRxCapableFolderPath(path);
}
