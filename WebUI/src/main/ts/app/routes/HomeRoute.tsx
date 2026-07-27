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

import React, { lazy } from "react";
import { useParams } from "react-router-dom";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import { loadComponent } from "../../registry";
import { LazyRouteFrame } from "./RouteErrorBoundary";

const HomeShellLazy = lazy(() =>
  loadComponent("HomeShell").then((C) => ({ default: C })),
);

/**
 * SPA Home route — product default landing after login.
 * Dashboard is intentionally not a peer SPA route; legacy dash remains optional exit.
 */
export function HomeRoute(): React.ReactElement {
  const { section } = useParams();
  const { isAdmin } = useSpaBootstrap();

  return (
    <LazyRouteFrame
      label="Home"
      fallback={
        <div data-testid="route-home-loading" style={{ padding: "1.5rem" }}>
          Loading Home…
        </div>
      }
    >
      <HomeShellLazy embedded initialSection={section} isAdmin={isAdmin} />
    </LazyRouteFrame>
  );
}
