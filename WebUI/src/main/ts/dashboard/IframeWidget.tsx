/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React from "react";
import { UnavailableGadgetShell } from "./UnavailableGadgetShell";

export interface IframeWidgetProps {
  title?: string;
  refreshInterval?: number;
  /** Optional embed URL when provided via gadget settings (future). */
  src?: string;
}

/**
 * Classic iframe gadget embeds a configured URL.
 * Without saved settings persist, we do not invent a CMS config endpoint.
 */
export const IframeWidget: React.FC<IframeWidgetProps> = ({
  title = "Iframe",
  src,
}) => {
  if (src && src.trim()) {
    return (
      <div data-testid="iframe-widget" style={{ border: "1px solid #ddd" }}>
        <div style={{ padding: "8px 12px", background: "#007ea8", color: "#fff" }}>
          {title}
        </div>
        <iframe
          title={title}
          src={src.trim()}
          style={{ width: "100%", height: 320, border: 0 }}
        />
      </div>
    );
  }
  return (
    <UnavailableGadgetShell
      title={title}
      testId="iframe-widget"
      reason="Provide an embed URL in gadget settings when layout persist is available. There is no /services/embed/iframe CMS API."
    />
  );
};

export default IframeWidget;
