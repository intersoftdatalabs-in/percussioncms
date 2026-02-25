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

/**
 * Main Dashboard component.
 *
 * <p>Replaces the retired Apache Shindig 3.0.0-beta4 gadget container with
 * a modern React/TypeScript implementation. Loads user's saved dashboard
 * layout and renders widgets accordingly.</p>
 *
 * <p>Features fallback to legacy dashboard via feature flag (URL param
 * {@code ?legacyDashboard=true}).</p>
 */

import React, { useEffect, useState } from "react";
import { DashboardLayout, type DashboardWidget } from "./DashboardLayout";
import { WelcomeWidget } from "./WelcomeWidget";

/**
 * Default widget configuration for initial dashboard.
 *
 * <p>This is the "home" configuration. In the future, users can customize
 * this via dashboard configuration endpoints.</p>
 */
const DEFAULT_WIDGETS: DashboardWidget[] = [
  {
    id: "welcome",
    name: "Welcome",
    component: WelcomeWidget,
    props: { userName: "User" },
    position: { column: "left", order: 0 },
  },
  // More widgets will be added here in Phase 1b
  // {
  //   id: 'workflow',
  //   name: 'Workflow Status',
  //   component: WorkflowStatusWidget,
  //   position: { column: 'left', order: 1 },
  // },
  // {
  //   id: 'activity',
  //   name: 'Activity',
  //   component: ActivityWidget,
  //   position: { column: 'right', order: 0 },
  // },
];

export interface DashboardProps {
  legacyDashboardUrl?: string;
}

/**
 * Main dashboard component.
 *
 * @param legacyDashboardUrl - Optional URL to legacy dashboard for fallback
 */
export const Dashboard: React.FC<DashboardProps> = ({
  legacyDashboardUrl = "/cm/app/dashboard.jsp?legacy=true",
}) => {
  const [widgets, setWidgets] = useState<DashboardWidget[]>(DEFAULT_WIDGETS);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Check for feature flag to use legacy dashboard
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("legacyDashboard") === "true") {
      console.log("Legacy dashboard flag detected, redirecting...");
      window.location.href = legacyDashboardUrl;
    }
  }, [legacyDashboardUrl]);

  // TODO: Load user's saved dashboard configuration from REST API
  // useEffect(() => {
  //   setLoading(true);
  //   try {
  //     // const config = await getDashboardConfig(userId);
  //     // setWidgets(config.widgets);
  //   } catch (err) {
  //     setError(err instanceof Error ? err.message : String(err));
  //   } finally {
  //     setLoading(false);
  //   }
  // }, []);

  if (error) {
    return (
      <div
        style={{
          padding: "20px",
          color: "#c33",
          backgroundColor: "#fee",
          borderRadius: "4px",
          margin: "20px",
        }}
      >
        <strong>Error loading dashboard:</strong> {error}
        <p>
          <a href={legacyDashboardUrl}>Fall back to legacy dashboard</a>
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          height: "100vh",
          color: "#999",
        }}
      >
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return <DashboardLayout widgets={widgets} />;
};
