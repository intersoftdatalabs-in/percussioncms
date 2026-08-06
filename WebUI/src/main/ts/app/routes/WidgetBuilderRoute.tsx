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
import { loadComponent } from "../../registry";
import { LazyRouteFrame } from "./RouteErrorBoundary";
import { RequireRole } from "./RequireRole";

const WidgetBuilderAppLazy = lazy(() =>
  loadComponent("WidgetBuilderApp").then((C) => ({ default: C })),
);

/**
 * SPA Widget Builder route — Admin/Designer when WB is active.
 */
export function WidgetBuilderRoute(): React.ReactElement {
  return (
    <RequireRole gate="widgetBuilder">
      <LazyRouteFrame
        label="Widget Builder"
        fallback={
          <div
            data-testid="route-widget-builder-loading"
            style={{ padding: "1.5rem" }}
          >
            Loading Widget Builder…
          </div>
        }
      >
        <WidgetBuilderAppLazy embedded />
      </LazyRouteFrame>
    </RequireRole>
  );
}
