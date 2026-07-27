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

import React, { useMemo } from "react";
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
 * If the document URL is still {@code spa.jsp?entry=…}, rewrite to the path
 * client route under the SPA basename before BrowserRouter mounts.
 * Keeps the server query contract while giving BrowserRouter a clean first paint.
 * Path refreshes (filter → spa.jsp forward with browser URL already path-based)
 * leave the address bar unchanged.
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
  // Only rewrite when serving the SPA document by name (query handoff)
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

/**
 * Authenticated SPA root: theme → bootstrap → BrowserRouter → routes.
 */
export function App({
  bootstrap,
  entrySearch,
  basename: basenameProp,
}: AppProps): React.ReactElement {
  const basename = useMemo(
    () =>
      basenameProp ??
      detectSpaBasename(
        typeof window !== "undefined" ? window.location.pathname : "/cm/app",
      ),
    [basenameProp],
  );

  // Synchronous handoff so the first router match is the feature path, not /spa.jsp
  useMemo(() => {
    const search =
      entrySearch ??
      (typeof window !== "undefined" ? window.location.search : "");
    const pathname =
      typeof window !== "undefined" ? window.location.pathname : `${basename}/spa.jsp`;
    applyEntryQueryToPath(pathname, search, basename);
    return null;
  }, [basename, entrySearch]);

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
