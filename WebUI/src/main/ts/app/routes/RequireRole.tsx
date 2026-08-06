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
import { Navigate } from "react-router";
import { useSpaBootstrap } from "../bootstrap/BootstrapContext";

export type SpaRoleGate = "admin" | "adminOrDesigner" | "widgetBuilder";

/**
 * Client UX gate for SPA routes. Server REST remains authoritative.
 */
export function RequireRole({
  gate,
  children,
}: {
  gate: SpaRoleGate;
  children: React.ReactNode;
}): React.ReactElement {
  const { isAdmin, isDesigner, isWidgetBuilderActive } = useSpaBootstrap();

  let allowed = false;
  switch (gate) {
    case "admin":
      allowed = isAdmin;
      break;
    case "adminOrDesigner":
      allowed = isAdmin || isDesigner;
      break;
    case "widgetBuilder":
      allowed = isWidgetBuilderActive && (isAdmin || isDesigner);
      break;
    default:
      allowed = false;
  }

  if (!allowed) {
    return <Navigate to="/home" replace />;
  }
  return <>{children}</>;
}
