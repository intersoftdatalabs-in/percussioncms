/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useId, useState } from "react";
import { getTemplateDetail } from "../api/developer/assemblyApi";
import type { TemplateDetail } from "../api/developer/types";
import {
  extractRestErrorMessage,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import { catalogColors } from "../developer/catalogStyles";
import { DESIGN_MSG } from "./messages";

function drawerErrMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DESIGN_MSG.SESSION_REDIRECT;
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) return `${fallback} ${fromBody}`;
    return `${fallback} (${err.status})`;
  }
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

const drawerShell: React.CSSProperties = {
  position: "fixed",
  top: 0,
  right: 0,
  width: "min(420px, 100vw)",
  height: "100vh",
  background: "#fff",
  borderLeft: `1px solid ${catalogColors.headerBorder}`,
  boxShadow: "-4px 0 16px rgba(0,0,0,0.08)",
  zIndex: 1200,
  display: "flex",
  flexDirection: "column",
  fontFamily: "var(--perc-font-family, sans-serif)",
};

const backdrop: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(0,0,0,0.25)",
  zIndex: 1190,
};

const metaGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "auto 1fr",
  gap: "6px 14px",
  marginTop: "12px",
  fontSize: "0.9rem",
};

const mono: React.CSSProperties = {
  fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
  fontSize: "0.85rem",
};

function dash(value: string | number | null | undefined): string {
  if (value == null) return DESIGN_MSG.NONE;
  const s = String(value).trim();
  return s.length > 0 ? s : DESIGN_MSG.NONE;
}

/**
 * Read-only template summary drawer for Design template library (#2808).
 * Edit surfaces (source, bindings, slots) remain out of scope for this slice.
 */
export function TemplateDetailDrawer({
  idOrName,
  onClose,
}: {
  idOrName: string;
  onClose: () => void;
}): React.ReactElement {
  const titleId = useId();
  const [detail, setDetail] = useState<TemplateDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setDetail(null);
    getTemplateDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(drawerErrMsg(e, DESIGN_MSG.DRAWER_ERROR));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  useEffect(() => {
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key === "Escape") {
        ev.preventDefault();
        onClose();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const bindingCount = detail?.bindings?.length ?? 0;
  const slotCount = detail?.slots?.length ?? 0;
  const gaps = (detail?.designGaps || []).filter((g) => g && String(g).trim());

  return (
    <>
      <div
        data-testid="design-tpl-drawer-backdrop"
        style={backdrop}
        onClick={onClose}
        aria-hidden="true"
      />
      <aside
        data-testid="design-tpl-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        style={drawerShell}
      >
        <header
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "8px",
            padding: "14px 16px",
            borderBottom: `1px solid ${catalogColors.headerBorder}`,
          }}
        >
          <div>
            <h2
              id={titleId}
              style={{ margin: 0, fontSize: "1.1rem" }}
              data-testid="design-tpl-drawer-title"
            >
              {DESIGN_MSG.DRAWER_TITLE}
            </h2>
            <p
              style={{
                margin: "4px 0 0",
                fontSize: "0.8rem",
                color: catalogColors.muted,
              }}
            >
              {DESIGN_MSG.DRAWER_READONLY}
            </p>
          </div>
          <button
            type="button"
            data-testid="design-tpl-drawer-close"
            aria-label={DESIGN_MSG.DRAWER_CLOSE_ARIA}
            onClick={onClose}
            style={{
              background: "transparent",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: "4px",
              padding: "6px 12px",
              cursor: "pointer",
              font: "inherit",
            }}
          >
            {DESIGN_MSG.DRAWER_CLOSE}
          </button>
        </header>

        <div style={{ padding: "16px", overflowY: "auto", flex: 1 }}>
          {loading && (
            <div data-testid="design-tpl-drawer-loading">
              {DESIGN_MSG.DRAWER_LOADING}
            </div>
          )}
          {!loading && error && (
            <div data-testid="design-tpl-drawer-error" role="alert" style={{ color: catalogColors.error }}>
              {error}
            </div>
          )}
          {!loading && !error && detail && (
            <div data-testid="design-tpl-drawer-body">
              <div style={metaGrid}>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_LABEL}</span>
                <span data-testid="design-tpl-drawer-label">
                  {dash(detail.label || detail.name)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_NAME}</span>
                <span data-testid="design-tpl-drawer-name" style={mono}>
                  {dash(detail.name)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_ID}</span>
                <span data-testid="design-tpl-drawer-id" style={mono}>
                  {detail.templateId != null
                    ? String(detail.templateId)
                    : dash(detail.guid?.stringValue)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_DESCRIPTION}</span>
                <span data-testid="design-tpl-drawer-description">
                  {dash(detail.description)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_ASSEMBLER}</span>
                <span data-testid="design-tpl-drawer-assembler" style={mono}>
                  {dash(detail.assembler)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_MIME}</span>
                <span data-testid="design-tpl-drawer-mime" style={mono}>
                  {dash(detail.mimeType)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_TYPE}</span>
                <span data-testid="design-tpl-drawer-type" style={mono}>
                  {dash(detail.templateType)}
                </span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_BINDINGS}</span>
                <span data-testid="design-tpl-drawer-bindings">{bindingCount}</span>
                <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_SLOTS}</span>
                <span data-testid="design-tpl-drawer-slots">{slotCount}</span>
                {gaps.length > 0 && (
                  <>
                    <span style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_GAPS}</span>
                    <span data-testid="design-tpl-drawer-gaps">{gaps.join(", ")}</span>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}
