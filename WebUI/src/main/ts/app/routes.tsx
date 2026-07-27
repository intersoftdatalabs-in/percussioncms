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

/**
 * Authenticated SPA routes. Placeholders until PR-3/PR-4 embed real shells.
 */
export function AppRoutes(): React.ReactElement {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/home" replace />} />
        <Route
          path="home"
          element={
            <FeaturePlaceholder
              title="Home"
              legacyHref="/cm/app/?view=home"
              testId="route-home"
            />
          }
        />
        <Route
          path="home/:section"
          element={
            <FeaturePlaceholder
              title="Home"
              legacyHref="/cm/app/?view=home"
              testId="route-home"
            />
          }
        />
        <Route
          path="publish"
          element={
            <FeaturePlaceholder
              title="Publish"
              legacyHref="/cm/app/?view=publish"
              testId="route-publish"
            />
          }
        />
        <Route
          path="publish/:section"
          element={
            <FeaturePlaceholder
              title="Publish"
              legacyHref="/cm/app/?view=publish"
              testId="route-publish"
            />
          }
        />
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
              legacyHref="/cm/app/?view=home"
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
        <Route
          path="*"
          element={<Navigate to="/unavailable" replace />}
        />
      </Route>
    </Routes>
  );
}
