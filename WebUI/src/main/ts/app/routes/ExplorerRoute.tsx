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

import React, { lazy, useMemo } from "react";
import { useLocation } from "react-router";
import { normalizeExplorerPath } from "../deepLinks/allowlists";
import { loadComponent } from "../../registry";
import { LazyRouteFrame } from "./RouteErrorBoundary";
import styles from "./ExplorerRoute.module.css";

const ContentExplorerShellLazy = lazy(() =>
  loadComponent("ContentExplorerShell").then((C) => ({ default: C })),
);

/**
 * SPA Content Explorer route (PR-6).
 * Deep-link path comes from client search ({@code /explorer?path=/Sites/...} under
 * basename) seeded from server {@code spa.jsp?entry=explorer&path=…} or path refresh.
 */
export function ExplorerRoute(): React.ReactElement {
  const location = useLocation();
  const initialPath = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return normalizeExplorerPath(params.get("path")) ?? "/";
  }, [location.search]);

  return (
    <LazyRouteFrame
      label="Content Explorer"
      fallback={
        <div
          className={styles.loading}
          data-testid="explorer-route-loading"
          role="status"
        >
          Loading Content Explorer…
        </div>
      }
    >
      <ContentExplorerShellLazy initialPath={initialPath} />
    </LazyRouteFrame>
  );
}
