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
import { useParams } from "react-router";
import { loadComponent } from "../../registry";
import { LazyRouteFrame } from "./RouteErrorBoundary";
import { RequireRole } from "./RequireRole";

const AdminShellLazy = lazy(() =>
  loadComponent("AdminShell").then((C) => ({ default: C })),
);

/**
 * SPA Admin tools route — Admin only (includes tools tab).
 */
export function AdminRoute(): React.ReactElement {
  const { tab } = useParams();

  return (
    <RequireRole gate="admin">
      <LazyRouteFrame
        label="Admin tools"
        fallback={
          <div data-testid="route-admin-loading" style={{ padding: "1.5rem" }}>
            Loading Admin tools…
          </div>
        }
      >
        <AdminShellLazy embedded initialTab={tab} />
      </LazyRouteFrame>
    </RequireRole>
  );
}
