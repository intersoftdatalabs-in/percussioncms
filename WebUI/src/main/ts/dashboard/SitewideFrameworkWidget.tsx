/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { UnavailableGadgetShell } from "./UnavailableGadgetShell";

export interface SitewideFrameworkWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/** Invented {@code /services/framework/config} has no product peer. */
export const SitewideFrameworkWidget: React.FC<SitewideFrameworkWidgetProps> = ({
  title = "Sitewide Framework",
}) => (
  <UnavailableGadgetShell
    title={title}
    testId="sitewide-framework-widget"
    reason="Sitewide framework is not exposed as a Home dashboard REST resource. Configure themes/templates in Design."
  />
);

export default SitewideFrameworkWidget;
