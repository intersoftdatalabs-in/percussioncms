/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useRef, useState } from "react";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { isApiError } from "../api/client";
import {
  APPLICATION_FILE_DESIGN_GAPS,
  getApplicationFileDetail,
  updateApplicationFile,
} from "../api/developer/applicationFilesApi";
import type { ApplicationFileSummary } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/** Soft ceiling for in-browser text editing (bytes). Larger files stay read-blocked. */
export const APPFILE_MAX_EDIT_BYTES = 2 * 1024 * 1024;

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

function looksLikeXmlPath(path: string): boolean {
  const lower = (path || "").toLowerCase();
  return lower.endsWith(".xml") || lower.endsWith(".xsl") || lower.endsWith(".xslt");
}

/**
 * Soft well-formedness hint for XML-ish paths. Tag-stack only — do not feed
 * operator XML into {@code DOMParser.parseFromString} (CodeQL
 * {@code js/xss-through-dom}; peers: templateImportExport / contentTypeImportExport).
 */
export function hasXmlParseError(content: string): boolean {
  const trimmed = (content ?? "").trim();
  if (!trimmed) {
    return false;
  }
  if (!trimmed.startsWith("<")) {
    return true;
  }
  const stripped = trimmed
    .replace(/<!--[\s\S]*?-->/g, "")
    .replace(/<!\[CDATA\[[\s\S]*?\]\]>/g, "")
    .replace(/<\?[\s\S]*?\?>/g, "");
  const stack: string[] = [];
  const tagRe = /<\/?([A-Za-z_][\w:.-]*)\b[^>]*\/?>/g;
  let m: RegExpExecArray | null;
  let lastIndex = 0;
  while ((m = tagRe.exec(stripped)) !== null) {
    const between = stripped.slice(lastIndex, m.index);
    if (between.includes("<") || between.includes(">")) {
      return true;
    }
    lastIndex = m.index + m[0].length;
    const full = m[0];
    const name = m[1].toLowerCase();
    if (full.endsWith("/>")) {
      continue;
    }
    if (full.startsWith("</")) {
      if (stack.length === 0 || stack[stack.length - 1] !== name) {
        return true;
      }
      stack.pop();
    } else {
      stack.push(name);
    }
  }
  const rest = stripped.slice(lastIndex);
  if (rest.includes("<") || rest.includes(">")) {
    return true;
  }
  return stack.length !== 0;
}

/**
 * SY-05 — edit and save one application CMS/resource file body (Admin PUT).
 */
export function ApplicationFileDetailPanel({
  applicationName,
  path,
  onBack,
  onSaved,
}: {
  applicationName: string;
  path: string;
  onBack: () => void;
  onSaved?: (detail: ApplicationFileSummary) => void;
}): React.ReactElement {
  const { isAdmin } = useSpaBootstrap();
  const [detail, setDetail] = useState<ApplicationFileSummary | null>(null);
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [tooLarge, setTooLarge] = useState(false);
  const inflight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setContent("");
    setError(null);
    setNotice(null);
    setTooLarge(false);
    setLoading(true);
    getApplicationFileDetail(applicationName, path)
      .then((d) => {
        if (cancelled) return;
        const len =
          typeof d.contentLength === "number"
            ? d.contentLength
            : d.content != null
              ? new TextEncoder().encode(d.content).length
              : 0;
        if (len > APPFILE_MAX_EDIT_BYTES) {
          setDetail(d);
          setTooLarge(true);
          setContent("");
          setError(DEV_MSG.APPFILE_TOO_LARGE);
          setLoading(false);
          return;
        }
        setDetail(d);
        setContent(d.content ?? "");
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.APPFILE_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [applicationName, path]);

  const dirty = detail != null && !tooLarge && content !== (detail.content ?? "");
  const canSave =
    Boolean(isAdmin) && !busy && !loading && !tooLarge && detail != null && dirty;

  function confirmLeaveIfDirty(): boolean {
    if (!dirty) {
      return true;
    }
    if (typeof window !== "undefined" && typeof window.confirm === "function") {
      return window.confirm(DEV_MSG.APPFILE_UNSAVED_CONFIRM);
    }
    return true;
  }

  function handleBack(): void {
    if (!confirmLeaveIfDirty()) {
      return;
    }
    onBack();
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) return;
    if (looksLikeXmlPath(path) && hasXmlParseError(content)) {
      if (typeof window !== "undefined" && typeof window.confirm === "function") {
        if (!window.confirm(DEV_MSG.APPFILE_XML_CONFIRM)) {
          return;
        }
      } else {
        // No confirm available (tests / embedded hosts): block rather than silent bypass.
        setError(DEV_MSG.APPFILE_XML_BLOCKED);
        return;
      }
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateApplicationFile(applicationName, path, { content });
      setDetail(saved);
      setContent(saved.content ?? content);
      setNotice(DEV_MSG.APPFILE_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.APPFILE_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.APPFILE_NOT_FOUND
            : DEV_MSG.APPFILE_SAVE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const gaps =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? detail.designGaps
      : APPLICATION_FILE_DESIGN_GAPS;

  return (
    <div data-testid="developer-appfile-detail">
      <button
        type="button"
        onClick={handleBack}
        data-testid="developer-appfile-back"
        aria-label={DEV_MSG.APPFILE_BACK_FILES}
        style={backButton}
      >
        ← {DEV_MSG.APPFILE_BACK_FILES}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-appfile-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div
          role="status"
          data-testid="developer-appfile-editor-notice"
          style={{ color: "#276749" }}
        >
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-appfile-detail-loading">
          {DEV_MSG.APPFILE_DETAIL_LOADING}
        </div>
      ) : null}

      {!loading && detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-appfile-detail-title">
              {detail.name || detail.path || path}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.APPFILE_COL_APP}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.applicationName || applicationName}
              </dd>
              <dt>{DEV_MSG.APPFILE_COL_PATH}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.path || path}</dd>
              <dt>{DEV_MSG.APPFILE_COL_MIME}</dt>
              <dd style={{ margin: 0 }}>{detail.mimeType || "—"}</dd>
              <dt>{DEV_MSG.APPFILE_COL_ENC}</dt>
              <dd style={{ margin: 0 }}>{detail.characterEncoding || "—"}</dd>
            </dl>
          </header>

          {!tooLarge ? (
            <section data-testid="developer-appfile-content" style={fieldStyle}>
              <label htmlFor="appfile-content">{DEV_MSG.APPFILE_CONTENT}</label>
              <textarea
                id="appfile-content"
                data-testid="developer-appfile-content-editor"
                aria-label={DEV_MSG.APPFILE_CONTENT}
                style={textareaStyle}
                value={content}
                disabled={busy || !isAdmin}
                onChange={(e) => setContent(e.target.value)}
                spellCheck={false}
              />
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.APPFILE_CONTENT_HINT}
              </span>
            </section>
          ) : null}

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-appfile-save"
              aria-label={DEV_MSG.APPFILE_SAVE}
              disabled={!canSave}
              title={!isAdmin ? DEV_MSG.APPFILE_SAVE_ADMIN_ONLY : undefined}
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
              {DEV_MSG.APPFILE_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-appfile-cancel"
              disabled={busy}
              onClick={handleBack}
              style={{
                padding: "8px 16px",
                background: "transparent",
                color: catalogColors.text,
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.APPFILE_CANCEL}
            </button>
          </div>

          {!isAdmin ? (
            <div
              role="status"
              data-testid="developer-appfile-admin-hint"
              style={{ color: catalogColors.muted, fontSize: "0.9rem", marginBottom: "12px" }}
            >
              {DEV_MSG.APPFILE_SAVE_ADMIN_ONLY}
            </div>
          ) : null}

          <section style={{ marginTop: "16px" }} data-testid="developer-appfile-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.APPFILE_GAPS}</h3>
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
