/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/** Product SPA mount under the cm tree (app tree preferred). */
export const SPA_APP_MOUNT = "/cm/app";
/** Dual-tree pages mount (legacy Track A path). */
export const SPA_PAGES_MOUNT = "/cm/pages/app";

const MOUNTS = [SPA_APP_MOUNT, SPA_PAGES_MOUNT] as const;

/**
 * Detect BrowserRouter basename from the current pathname.
 * Supports optional servlet context prefix (e.g. {@code /Rhythmyx/cm/app/...}).
 */
export function detectSpaBasename(
  pathname: string = typeof window !== "undefined" ? window.location.pathname : SPA_APP_MOUNT,
): string {
  for (const mount of MOUNTS) {
    const idx = pathname.indexOf(mount);
    if (idx >= 0) {
      return pathname.slice(0, idx + mount.length);
    }
  }
  return SPA_APP_MOUNT;
}

/**
 * Path after the SPA basename (starts with {@code /}), or empty string when
 * the pathname is exactly the basename (or basename + {@code /spa.jsp}).
 */
export function pathAfterBasename(
  pathname: string,
  basename: string = detectSpaBasename(pathname),
): string {
  if (!pathname.startsWith(basename)) {
    return "";
  }
  let rest = pathname.slice(basename.length);
  if (rest.startsWith("/spa.jsp")) {
    return "";
  }
  if (!rest || rest === "/") {
    return "";
  }
  return rest.startsWith("/") ? rest : `/${rest}`;
}
