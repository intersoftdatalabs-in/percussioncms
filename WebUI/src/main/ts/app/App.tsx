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

import React, { useEffect } from "react";
import { HashRouter, useNavigate } from "react-router";
import { ThemeProvider } from "../ui-themes/ThemeProvider";
import { BootstrapProvider } from "./bootstrap/BootstrapContext";
import type { SpaBootstrap } from "./bootstrap/types";
import { parseEntryQuery } from "./deepLinks/parseEntryQuery";
import { AppRoutes } from "./routes";

export interface AppProps {
  bootstrap: SpaBootstrap;
  /** Override search for tests (default window.location.search) */
  entrySearch?: string;
}

/**
 * Applies server-driven {@code ?entry=} query once, then leaves client routing
 * to HashRouter (refresh-safe under Jetty).
 */
function EntryQueryApplier({
  entrySearch,
}: {
  entrySearch?: string;
}): null {
  const navigate = useNavigate();

  useEffect(() => {
    const search =
      entrySearch ??
      (typeof window !== "undefined" ? window.location.search : "");
    if (!search || search === "?") {
      return;
    }
    const parsed = parseEntryQuery(search);
    navigate(parsed.clientPath, { replace: true });
    // Strip entry query from the document URL after client route is applied
    // so address bar shows hash route only (server entry already consumed).
    if (typeof window !== "undefined" && window.history?.replaceState) {
      try {
        const url = new URL(window.location.href);
        if (url.searchParams.has("entry") || url.search.length > 1) {
          url.search = "";
          window.history.replaceState(null, "", url.pathname + url.hash);
        }
      } catch {
        // ignore
      }
    }
    // Run once on mount for server entry handoff
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
}

/**
 * Authenticated SPA root: theme → bootstrap → HashRouter → routes.
 */
export function App({ bootstrap, entrySearch }: AppProps): React.ReactElement {
  return (
    <ThemeProvider>
      <BootstrapProvider value={bootstrap}>
        <HashRouter>
          <EntryQueryApplier entrySearch={entrySearch} />
          <AppRoutes />
        </HashRouter>
      </BootstrapProvider>
    </ThemeProvider>
  );
}
