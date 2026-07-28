/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { UnavailableGadgetShell } from "./UnavailableGadgetShell";

export interface WidgetConfigurationWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Deprecated dashboard configuration gadget.
 * React layout persist is session-local via useDashboardConfig (not this path).
 */
export const WidgetConfigurationWidget: React.FC<
  WidgetConfigurationWidgetProps
> = ({ title = "Dashboard Configuration" }) => (
  <UnavailableGadgetShell
    title={title}
    testId="widget-configuration-widget"
    reason="Use Add / Remove Gadget on the Home Gadgets host. Invented /services/dashboard/config is not a product API; classic dashboard persist remains Shindig-oriented."
  />
);

export default WidgetConfigurationWidget;
