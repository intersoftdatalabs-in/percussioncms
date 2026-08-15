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

import React from "react";
import { Navigate, Route, Routes } from "react-router";
import { FeaturePlaceholder } from "./FeaturePlaceholder";
import { AppLayout } from "./layout/AppLayout";
import { AdminRoute } from "./routes/AdminRoute";
import { ArchitectureRoute } from "./routes/ArchitectureRoute";
import { AssemblyRoute } from "./routes/AssemblyRoute";
import { EditorRoute } from "./routes/EditorRoute";
import { DesignRoute } from "./routes/DesignRoute";
import { DeveloperRoute } from "./routes/DeveloperRoute";
import { ExplorerRoute } from "./routes/ExplorerRoute";
import { HomeRoute } from "./routes/HomeRoute";
import { PublishRoute } from "./routes/PublishRoute";
import { ProfileRoute } from "./routes/ProfileRoute";
import { WidgetBuilderRoute } from "./routes/WidgetBuilderRoute";
import { WorkflowRoute } from "./routes/WorkflowRoute";

/**
 * Authenticated SPA routes.
 * Home is the product default landing. Feature shells embedded (PR-3–PR-6).
 * Residual bridge embeds remain only on unmigrated legacy pages.
 */
export function AppRoutes(): React.ReactElement {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="assembly" element={<AssemblyRoute />} />
        <Route path="editor" element={<EditorRoute />} />
        <Route index element={<Navigate to="/home" replace />} />
        <Route path="home" element={<HomeRoute />} />
        <Route path="home/:section" element={<HomeRoute />} />
        <Route path="publish" element={<PublishRoute />} />
        <Route path="publish/:section" element={<PublishRoute />} />
        <Route path="workflow" element={<WorkflowRoute />} />
        <Route path="workflow/:tab" element={<WorkflowRoute />} />
        <Route path="admin" element={<AdminRoute />} />
        <Route path="admin/:tab" element={<AdminRoute />} />
        <Route path="widget-builder" element={<WidgetBuilderRoute />} />
        <Route path="developer" element={<DeveloperRoute />} />
        <Route path="developer/:section" element={<DeveloperRoute />} />
        <Route path="design" element={<DesignRoute />} />
        <Route path="design/:section" element={<DesignRoute />} />
        <Route path="architecture" element={<ArchitectureRoute />} />
        <Route path="architecture/:site" element={<ArchitectureRoute />} />
        <Route path="explorer" element={<ExplorerRoute />} />
        <Route path="profile" element={<ProfileRoute />} />
        <Route
          path="unavailable"
          element={
            <FeaturePlaceholder title="Unavailable" testId="route-unavailable" />
          }
        />
        <Route path="*" element={<Navigate to="/unavailable" replace />} />
      </Route>
    </Routes>
  );
}
