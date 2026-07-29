/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useState } from "react";
import { styles } from "./dashboard.styles";

const STORAGE_KEY = "perc.home.gadget.iframe.src";

export interface IframeWidgetProps {
  title?: string;
  refreshInterval?: number;
  /** Optional embed URL from props; otherwise sessionStorage. */
  src?: string;
}

/**
 * Classic iframe gadget embeds a configured URL.
 * URL can be passed as prop or saved in sessionStorage (no CMS embed API).
 */
export const IframeWidget: React.FC<IframeWidgetProps> = ({
  title = "Iframe",
  src: srcProp,
}) => {
  const stored =
    typeof sessionStorage !== "undefined"
      ? sessionStorage.getItem(STORAGE_KEY) || ""
      : "";
  const [draft, setDraft] = useState(srcProp?.trim() || stored);
  const [active, setActive] = useState(srcProp?.trim() || stored);

  const apply = () => {
    const next = draft.trim();
    setActive(next);
    try {
      if (next) {
        sessionStorage.setItem(STORAGE_KEY, next);
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      /* ignore quota / private mode */
    }
  };

  return (
    <div style={styles.widget} data-testid="iframe-widget">
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div
          style={{
            display: "flex",
            gap: 8,
            marginBottom: 8,
            flexWrap: "wrap",
          }}
        >
          <input
            type="url"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="https://…"
            data-testid="iframe-src-input"
            style={{
              flex: 1,
              minWidth: 160,
              padding: "6px 8px",
              border: "1px solid #ccc",
              borderRadius: 4,
            }}
          />
          <button
            type="button"
            onClick={apply}
            data-testid="iframe-src-apply"
            style={{ padding: "6px 12px" }}
          >
            Load
          </button>
        </div>
        {active ? (
          <iframe
            title={title}
            src={active}
            style={{ width: "100%", height: 320, border: "1px solid #eee" }}
          />
        ) : (
          <p style={{ fontSize: "0.9em", color: "#666", margin: 0 }}>
            Enter a URL to embed. Saved for this browser session only (no CMS
            embed API).
          </p>
        )}
      </div>
    </div>
  );
};

export default IframeWidget;
