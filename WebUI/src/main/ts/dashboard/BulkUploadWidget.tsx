/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { UnavailableGadgetShell } from "./UnavailableGadgetShell";

export interface BulkUploadWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic bulk file upload gadget has no job-list REST peer for Home.
 * (Invented {@code /services/bulk-upload/jobs} does not exist.)
 */
export const BulkUploadWidget: React.FC<BulkUploadWidgetProps> = ({
  title = "Bulk Upload",
}) => (
  <UnavailableGadgetShell
    title={title}
    testId="bulk-upload-widget"
    reason="There is no CMS REST job tracker for bulk upload. Use classic bulk upload flows or asset import tools."
  />
);

export default BulkUploadWidget;
