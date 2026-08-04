/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useState } from "react";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

const STORAGE_KEY = "perc.home.gadget.iframe.src";

export interface IframeWidgetProps {
  title?: string;
  refreshInterval?: number;
  /** Optional embed URL from props; otherwise sessionStorage. */
  src?: string;
}

/**
 * Allow only absolute http(s) URLs for iframe src.
 * Blocks javascript:/data:/etc. and rejects unparseable values (CodeQL js/xss-through-dom).
 */
export function sanitizeEmbedUrl(raw: string): string | null {
  const trimmed = (raw ?? "").trim();
  if (!trimmed) {
    return null;
  }
  try {
    // Absolute URL only — no relative resolution from page origin.
    const u = new URL(trimmed);
    if (u.protocol !== "http:" && u.protocol !== "https:") {
      return null;
    }
    // Normalize; never return the raw DOM/session string to the sink.
    return u.toString();
  } catch {
    return null;
  }
}

function readStoredUrl(): string {
  try {
    if (typeof sessionStorage === "undefined") {
      return "";
    }
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return "";
    }
    return sanitizeEmbedUrl(raw) ?? "";
  } catch {
    return "";
  }
}

/**
 * Classic iframe gadget embeds a configured URL.
 * URL can be passed as prop or saved in sessionStorage (no CMS embed API).
 * Only http(s) URLs are accepted for the iframe {@code src} attribute.
 */
export const IframeWidget: React.FC<IframeWidgetProps> = ({
  title,
  src: srcProp,
}) => {
  const heading = title ?? message(MSG.GADGET_EXTERNAL_CONTENT);
  const initialSafe =
    sanitizeEmbedUrl(srcProp ?? "") || readStoredUrl() || "";
  const [draft, setDraft] = useState(initialSafe);
  const [active, setActive] = useState(initialSafe);
  const [error, setError] = useState<string | null>(null);

  const apply = () => {
    const safe = sanitizeEmbedUrl(draft);
    if (!safe) {
      setError("Enter a valid http(s) URL (javascript: and data: are blocked).");
      setActive("");
      try {
        sessionStorage.removeItem(STORAGE_KEY);
      } catch {
        /* ignore */
      }
      return;
    }
    setError(null);
    setActive(safe);
    try {
      sessionStorage.setItem(STORAGE_KEY, safe);
    } catch {
      /* ignore quota / private mode */
    }
  };

  return (
    <div
      style={styles.widget}
      data-testid="iframe-widget"
      data-mkd-lang-ignore="1"
    >
      <div style={styles.widgetTitle}>{heading}</div>
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
        {error ? (
          <p
            data-testid="iframe-src-error"
            style={{ fontSize: "0.85em", color: "#c62828", margin: "0 0 8px" }}
          >
            {error}
          </p>
        ) : null}
        {active ? (
          <iframe
            // Same string as widget heading: prop title or MSG.GADGET_EXTERNAL_CONTENT fallback
            // (intentional a11y — avoids empty/undefined title when prop omitted).
            title={heading}
            src={active}
            // Restrictive sandbox: no same-origin + scripts combo.
            sandbox="allow-scripts allow-popups allow-forms"
            referrerPolicy="no-referrer"
            style={{ width: "100%", height: 320, border: "1px solid #eee" }}
          />
        ) : (
          <p style={{ fontSize: "0.9em", color: "#666", margin: 0 }}>
            Enter an https URL to embed. Saved for this browser session only
            (http/https only; no CMS embed API).
          </p>
        )}
      </div>
    </div>
  );
};

export default IframeWidget;
