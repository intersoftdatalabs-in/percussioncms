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
import { useParams, useSearchParams } from "react-router-dom";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";
import { loadComponent } from "../../registry";
import { LazyRouteFrame } from "./RouteErrorBoundary";

const PublishingShellLazy = lazy(() =>
  loadComponent("PublishingShell").then((C) => ({ default: C })),
);

/**
 * SPA Publish route — embeds PublishingShell under AppLayout.
 */
export function PublishRoute(): React.ReactElement {
  const { section } = useParams();
  const [search] = useSearchParams();
  const { isAdmin, isDesigner } = useSpaBootstrap();
  const siteId = search.get("siteId") ?? undefined;
  const serverId = search.get("serverId") ?? undefined;
  // Design section for Admin/Designer (product progressive disclosure)
  const showDesign = isAdmin || isDesigner;

  return (
    <LazyRouteFrame
      label="Publish"
      fallback={
        <div data-testid="route-publish-loading" style={{ padding: "1.5rem" }}>
          Loading Publish…
        </div>
      }
    >
      <PublishingShellLazy
        embedded
        section={section}
        siteId={siteId}
        serverId={serverId}
        showDesign={showDesign}
      />
    </LazyRouteFrame>
  );
}
