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
import { useParams } from "react-router";
import { loadComponent } from "../../registry";
import { DESIGN_MSG } from "../../design/messages";
import { LazyRouteFrame } from "./RouteErrorBoundary";
import { RequireRole } from "./RequireRole";

const DesignShellLazy = lazy(() =>
  loadComponent("DesignShell").then((C) => ({ default: C })),
);

/**
 * SPA Design module — Admin or Designer.
 * Slice 1 (#2808): template library list shell + read-only detail drawer.
 */
export function DesignRoute(): React.ReactElement {
  const { section } = useParams();

  return (
    <RequireRole gate="adminOrDesigner">
      <LazyRouteFrame
        label={DESIGN_MSG.TITLE}
        fallback={
          <div data-testid="route-design-loading" style={{ padding: "1.5rem" }}>
            {DESIGN_MSG.SHELL_LOADING}
          </div>
        }
      >
        <DesignShellLazy embedded initialSection={section} />
      </LazyRouteFrame>
    </RequireRole>
  );
}
