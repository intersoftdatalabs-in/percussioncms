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

import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { FeaturePlaceholder } from "./FeaturePlaceholder";
import { AppLayout } from "./layout/AppLayout";
import { HomeRoute } from "./routes/HomeRoute";
import { PublishRoute } from "./routes/PublishRoute";

/**
 * Authenticated SPA routes.
 * Home is the product default landing. Publish is fully embedded (PR-3).
 * Other modern modules remain placeholders until PR-4+.
 */
export function AppRoutes(): React.ReactElement {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/home" replace />} />
        <Route path="home" element={<HomeRoute />} />
        <Route path="home/:section" element={<HomeRoute />} />
        <Route path="publish" element={<PublishRoute />} />
        <Route path="publish/:section" element={<PublishRoute />} />
        <Route
          path="workflow"
          element={
            <FeaturePlaceholder
              title="Administration"
              legacyHref="/cm/app/?view=workflow"
              testId="route-workflow"
            />
          }
        />
        <Route
          path="workflow/:tab"
          element={
            <FeaturePlaceholder
              title="Administration"
              legacyHref="/cm/app/?view=workflow"
              testId="route-workflow"
            />
          }
        />
        <Route
          path="admin"
          element={
            <FeaturePlaceholder
              title="Admin tools"
              legacyHref="/cm/app/?view=admin"
              testId="route-admin"
            />
          }
        />
        <Route
          path="admin/:tab"
          element={
            <FeaturePlaceholder
              title="Admin tools"
              legacyHref="/cm/app/?view=admin"
              testId="route-admin"
            />
          }
        />
        <Route
          path="widget-builder"
          element={
            <FeaturePlaceholder
              title="Widget Builder"
              legacyHref="/cm/app/?view=widgetbuilder"
              testId="route-widget-builder"
            />
          }
        />
        <Route
          path="explorer"
          element={
            <FeaturePlaceholder
              title="Content Explorer"
              legacyHref="/cm/app/explorerModern.jsp"
              testId="route-explorer"
            />
          }
        />
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
