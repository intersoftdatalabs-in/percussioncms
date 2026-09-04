/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useRef, useState } from "react";
import { isApiError } from "../api/client";
import {
  getServerConfigDetail,
  SERVER_CONFIG_DESIGN_GAPS,
  updateServerConfig,
  withoutStaleServerConfigWriteGap,
} from "../api/developer/serverConfigsApi";
import type { ServerConfigDef } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  marginBottom: "12px",
};

const textareaStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  fontFamily: "monospace",
  minHeight: "16rem",
  maxHeight: "28rem",
  overflow: "auto",
  whiteSpace: "pre",
  wordBreak: "normal",
};

export function ServerConfigDetailPanel({
  name,
  onBack,
  onSaved,
}: {
  name: string;
  onBack: () => void;
  onSaved?: (detail: ServerConfigDef) => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ServerConfigDef | null>(null);
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const inflight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setContent("");
    setError(null);
    setNotice(null);
    setLoading(true);
    getServerConfigDetail(name)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setContent(d.content ?? "");
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.CFG_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  const dirty = detail != null && content !== (detail.content ?? "");
  const canSave = !busy && !loading && detail != null && dirty;

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateServerConfig(name, { content });
      setDetail(saved);
      setContent(saved.content ?? content);
      setNotice(DEV_MSG.CFG_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.CFG_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.CFG_NOT_FOUND
            : DEV_MSG.CFG_SAVE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const gaps =
    detail != null
      ? (() => {
          const filtered = withoutStaleServerConfigWriteGap(detail.designGaps);
          if (filtered.length > 0) return filtered;
          return SERVER_CONFIG_DESIGN_GAPS.length > 0
            ? SERVER_CONFIG_DESIGN_GAPS
            : [DEV_MSG.CFG_GAP_LOCK];
        })()
      : [];

  return (
    <div data-testid="developer-cfg-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-cfg-back"
        aria-label={DEV_MSG.CFG_BACK}
        style={backButton}
      >
        ← {DEV_MSG.CFG_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-cfg-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div
          role="status"
          data-testid="developer-cfg-editor-notice"
          style={{ color: "#276749" }}
        >
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-cfg-detail-loading">{DEV_MSG.CFG_DETAIL_LOADING}</div>
      ) : null}

      {!loading && detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-cfg-detail-title">
              {detail.displayName || detail.name || name}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.CFG_COL_KEY}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.CFG_COL_FILE}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.fileName || "—"}</dd>
              <dt>{DEV_MSG.CFG_COL_MIME}</dt>
              <dd style={{ margin: 0 }}>{detail.mimeType || "—"}</dd>
              <dt>{DEV_MSG.CFG_COL_ENC}</dt>
              <dd style={{ margin: 0 }}>{detail.characterEncoding || "—"}</dd>
            </dl>
          </header>

          <section data-testid="developer-cfg-content" style={fieldStyle}>
            <label htmlFor="cfg-content">{DEV_MSG.CFG_CONTENT}</label>
            <textarea
              id="cfg-content"
              data-testid="developer-cfg-content-editor"
              aria-label={DEV_MSG.CFG_CONTENT}
              style={textareaStyle}
              value={content}
              disabled={busy}
              onChange={(e) => setContent(e.target.value)}
              spellCheck={false}
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.CFG_CONTENT_HINT}
            </span>
          </section>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-cfg-save"
              aria-label={DEV_MSG.CFG_SAVE}
              disabled={!canSave}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: canSave ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSave ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.CFG_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-cfg-cancel"
              disabled={busy}
              onClick={onBack}
              style={{
                padding: "8px 16px",
                background: "transparent",
                color: catalogColors.text,
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.CFG_CANCEL}
            </button>
          </div>

          <section style={{ marginTop: "16px" }} data-testid="developer-cfg-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CFG_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {gaps.map((g, i) => (
                <li key={`${g}-${i}`}>{g}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
