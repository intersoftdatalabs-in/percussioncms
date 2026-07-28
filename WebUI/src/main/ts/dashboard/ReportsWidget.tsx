/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { UnavailableGadgetShell } from "./UnavailableGadgetShell";

export interface ReportsWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/** Invented {@code /services/reports/list} has no product peer. */
export const ReportsWidget: React.FC<ReportsWidgetProps> = ({
  title = "Reports",
}) => (
  <UnavailableGadgetShell
    title={title}
    testId="reports-widget"
    reason="No catalog REST API for dashboard reports. Use Admin reporting / classic reports if installed."
  />
);

export default ReportsWidget;
