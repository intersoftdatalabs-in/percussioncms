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
 * Two-column dashboard layout component.
 *
 * <p>Renders dashboard widgets in a responsive two-column grid using native
 * CSS Grid. Each column is a flex container that stacks widgets vertically.</p>
 *
 * <p>Note: Drag-and-drop can be added later via react-grid-layout or similar.</p>
 */

import React from "react";
import { styles } from "./dashboard.styles";

export interface WidgetPosition {
  column: "left" | "right";
  order: number;
}

export interface DashboardWidget {
  id: string;
  name: string;
  component: React.ComponentType<any>;
  props?: Record<string, unknown>;
  position: WidgetPosition;
}

export interface DashboardLayoutProps {
  widgets: DashboardWidget[];
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ widgets }) => {
  // Separate widgets by column
  const leftWidgets = widgets
    .filter((w) => w.position.column === "left")
    .sort((a, b) => a.position.order - b.position.order);

  const rightWidgets = widgets
    .filter((w) => w.position.column === "right")
    .sort((a, b) => a.position.order - b.position.order);

  return (
    <div style={styles.container}>
      <div style={styles.column}>
        {leftWidgets.map((widget) => (
          <div key={widget.id}>
            <widget.component {...(widget.props || {})} />
          </div>
        ))}
      </div>
      <div style={styles.column}>
        {rightWidgets.map((widget) => (
          <div key={widget.id}>
            <widget.component {...(widget.props || {})} />
          </div>
        ))}
      </div>
    </div>
  );
};
