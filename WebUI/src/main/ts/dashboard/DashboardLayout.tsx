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

/**
 * Two-column dashboard layout component.
 *
 * <p>Renders dashboard widgets in a responsive two-column grid using native
 * CSS Grid. Each column is a flex container that stacks widgets vertically.</p>
 *
 * <p>Note: Drag-and-drop can be added later via react-grid-layout or similar.</p>
 */

import React from "react";
import { message, MSG } from "../i18n/message";
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
  onRemoveGadget?: (gadgetId: string) => void;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ widgets, onRemoveGadget }) => {
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
          <div
            key={widget.id}
            style={{
              position: "relative",
              marginBottom: "16px",
            }}
          >
            {/* Gadget component */}
            <widget.component {...(widget.props || {})} />

            {/* Remove button */}
            {onRemoveGadget && (
              <button
                onClick={() => onRemoveGadget(widget.id)}
                style={{
                  position: "absolute",
                  top: "8px",
                  right: "8px",
                  backgroundColor: "#f44336",
                  color: "white",
                  border: "none",
                  borderRadius: "50%",
                  width: "28px",
                  height: "28px",
                  padding: "0",
                  cursor: "pointer",
                  fontSize: "1em",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  zIndex: 10,
                }}
                title={message(MSG.GADGET_REMOVE_TITLE)}
                aria-label={message(MSG.GADGET_REMOVE_TITLE)}
              >
                ✕
              </button>
            )}
          </div>
        ))}
      </div>
      <div style={styles.column}>
        {rightWidgets.map((widget) => (
          <div
            key={widget.id}
            style={{
              position: "relative",
              marginBottom: "16px",
            }}
          >
            {/* Gadget component */}
            <widget.component {...(widget.props || {})} />

            {/* Remove button */}
            {onRemoveGadget && (
              <button
                onClick={() => onRemoveGadget(widget.id)}
                style={{
                  position: "absolute",
                  top: "8px",
                  right: "8px",
                  backgroundColor: "#f44336",
                  color: "white",
                  border: "none",
                  borderRadius: "50%",
                  width: "28px",
                  height: "28px",
                  padding: "0",
                  cursor: "pointer",
                  fontSize: "1em",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  zIndex: 10,
                }}
                title={message(MSG.GADGET_REMOVE_TITLE)}
                aria-label={message(MSG.GADGET_REMOVE_TITLE)}
              >
                ✕
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};
