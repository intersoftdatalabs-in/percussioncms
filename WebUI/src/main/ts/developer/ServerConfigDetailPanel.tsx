/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useState } from "react";
import { getServerConfigDetail } from "../api/developer/serverConfigsApi";
import type { ServerConfigDef } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function ServerConfigDetailPanel({
  name,
  onBack,
}: {
  name: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ServerConfigDef | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getServerConfigDetail(name)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.CFG_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  const gaps =
    detail != null && detail.designGaps && detail.designGaps.length
      ? detail.designGaps
      : [DEV_MSG.CFG_GAP_SAVE, DEV_MSG.CFG_GAP_LOCK];

  return (
    <div data-testid="developer-cfg-detail">
      <button type="button" onClick={onBack} data-testid="developer-cfg-back" style={backButton}>
        ← {DEV_MSG.CFG_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-cfg-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-cfg-detail-loading">{DEV_MSG.CFG_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
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

          <section data-testid="developer-cfg-content">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CFG_CONTENT}</h3>
            {detail.content == null || detail.content === "" ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-cfg-content-empty">
                {DEV_MSG.CFG_CONTENT_EMPTY}
              </p>
            ) : (
              <pre
                data-testid="developer-cfg-content-pre"
                style={{
                  background: "#f7fafc",
                  border: `1px solid ${catalogColors.headerBorder}`,
                  borderRadius: 4,
                  padding: 12,
                  overflow: "auto",
                  maxHeight: "28rem",
                  fontSize: "0.85rem",
                  whiteSpace: "pre-wrap",
                  wordBreak: "break-word",
                }}
              >
                {detail.content}
              </pre>
            )}
          </section>

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
