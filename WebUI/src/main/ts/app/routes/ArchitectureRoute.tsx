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

import React, { lazy } from "react";
import { useParams, useSearchParams } from "react-router";
import { loadComponent } from "../../registry";
import { ARCH_MSG } from "../../architecture/messages";
import { normalizeArchitectureSite } from "../deepLinks/allowlists";
import { LazyRouteFrame } from "./RouteErrorBoundary";
import { RequireRole } from "./RequireRole";

const ArchitectureShellLazy = lazy(() =>
  loadComponent("ArchitectureShell").then((C) => ({ default: C })),
);

/**
 * SPA Architecture / Navigation module — Admin or Designer (#3094).
 * Slice B: shell + empty state; tree editing lands in later slices.
 */
export function ArchitectureRoute(): React.ReactElement {
  const { site: siteParam } = useParams();
  const [searchParams] = useSearchParams();
  const siteFromQuery = normalizeArchitectureSite(searchParams.get("site"));
  const siteFromPath = normalizeArchitectureSite(siteParam);
  const site = siteFromPath ?? siteFromQuery ?? null;

  return (
    <RequireRole gate="adminOrDesigner">
      <LazyRouteFrame
        label={ARCH_MSG.TITLE}
        fallback={
          <div
            data-testid="route-architecture-loading"
            style={{ padding: "1.5rem" }}
          >
            {ARCH_MSG.SHELL_LOADING}
          </div>
        }
      >
        <ArchitectureShellLazy embedded initialSite={site} />
      </LazyRouteFrame>
    </RequireRole>
  );
}
