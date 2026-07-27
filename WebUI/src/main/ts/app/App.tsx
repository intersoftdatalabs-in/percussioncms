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

import React from "react";
import { BrowserRouter } from "react-router";
import { ThemeProvider } from "../ui-themes/ThemeProvider";
import { BootstrapProvider } from "./bootstrap/BootstrapContext";
import type { SpaBootstrap } from "./bootstrap/types";
import { parseEntryQuery } from "./deepLinks/parseEntryQuery";
import { detectSpaBasename } from "./deepLinks/spaBasename";
import { AppRoutes } from "./routes";

export interface AppProps {
  bootstrap: SpaBootstrap;
  /**
   * Override document search for tests / SSR-style entry handoff
   * (default {@code window.location.search}).
   */
  entrySearch?: string;
  /** Override BrowserRouter basename (tests / dual-tree) */
  basename?: string;
}

/**
 * If the document URL is still {@code spa.jsp?entry=…}, rewrite the address bar
 * to the path client route under the SPA basename via {@code history.replaceState}.
 *
 * <p>Must run <em>before</em> {@code BrowserRouter} mounts so its initial location
 * is the feature path. Prefer {@link handoffSpaEntryBeforeMount} from {@code boot()}.
 * Idempotent when already on a path route.
 *
 * <p>Not implemented via {@code useMemo} (Kilo #1542): that API is for pure
 * computations, not history side effects.
 */
export function applyEntryQueryToPath(
  pathname: string = typeof window !== "undefined" ? window.location.pathname : "",
  search: string = typeof window !== "undefined" ? window.location.search : "",
  basename: string = detectSpaBasename(pathname),
): string | null {
  if (!search || search === "?") {
    return null;
  }
  const params = new URLSearchParams(
    search.startsWith("?") ? search.slice(1) : search,
  );
  if (!params.has("entry")) {
    return null;
  }
  const isSpaDocument =
    pathname.endsWith("/spa.jsp") ||
    pathname.endsWith("spa.jsp") ||
    pathname === basename ||
    pathname === `${basename}/`;
  if (!isSpaDocument) {
    return null;
  }

  const parsed = parseEntryQuery(search);
  const [pathPart, queryPart] = parsed.clientPath.split("?");
  let next = `${basename}${pathPart.startsWith("/") ? pathPart : `/${pathPart}`}`;
  if (queryPart) {
    next += `?${queryPart}`;
  }
  if (typeof window !== "undefined" && window.history?.replaceState) {
    try {
      window.history.replaceState(null, "", next);
    } catch {
      // ignore
    }
  }
  return next;
}

/** Call from {@code boot()} before {@code createRoot} for a clean first paint. */
export function handoffSpaEntryBeforeMount(
  basenameProp?: string,
  entrySearch?: string,
): string {
  const pathname =
    typeof window !== "undefined" ? window.location.pathname : "/cm/app";
  const basename = basenameProp ?? detectSpaBasename(pathname);
  const search =
    entrySearch ??
    (typeof window !== "undefined" ? window.location.search : "");
  applyEntryQueryToPath(pathname, search, basename);
  return basename;
}

/**
 * Authenticated SPA root: theme → bootstrap → BrowserRouter → routes.
 */
export function App({
  bootstrap,
  entrySearch,
  basename: basenameProp,
}: AppProps): React.ReactElement {
  const pathname =
    typeof window !== "undefined" ? window.location.pathname : "/cm/app";
  const basename = basenameProp ?? detectSpaBasename(pathname);

  // Handoff before BrowserRouter is created so initial location is the feature
  // path. Production also calls handoffSpaEntryBeforeMount() in boot() — both
  // are idempotent. Not useMemo (Kilo #1542).
  if (typeof window !== "undefined") {
    applyEntryQueryToPath(
      window.location.pathname,
      entrySearch ?? window.location.search,
      basename,
    );
  }

  return (
    <ThemeProvider>
      <BootstrapProvider value={bootstrap}>
        <BrowserRouter basename={basename}>
          <AppRoutes />
        </BrowserRouter>
      </BootstrapProvider>
    </ThemeProvider>
  );
}
