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

/**
 * Replace landing page dialog for Architecture (#3097 / #3304).
 * Embeds ContentBrowser page picker when available; falls back to page id
 * entry for tests / minimal hosts. Empty or cancel never calls onSubmit.
 */

import React, { useEffect, useState } from "react";
import type { SelectionResult } from "../api/contentExplorer/types";
import { ContentBrowser } from "../contentBrowser/ContentBrowser";
import { catalogColors } from "../developer/catalogStyles";
import {
  LANDING_PAGE_ALLOWED_TYPES,
  resolveLandingPagePick,
} from "./landingPagePicker";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface ReplaceLandingPageDialogProps {
  open: boolean;
  siteName: string;
  sectionTitle: string;
  busy: boolean;
  /**
   * When false, skip embedding ContentBrowser (unit tests) and use id field.
   * Default true in product shell.
   */
  useContentBrowser?: boolean;
  onCancel: () => void;
  onSubmit: (newLandingPageId: string) => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(15, 23, 42, 0.45)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
  padding: 16,
};

const panelStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  border: `1px solid ${catalogColors.headerBorder}`,
  maxWidth: 900,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
  maxHeight: "92vh",
  overflow: "auto",
};

export function ReplaceLandingPageDialog({
  open,
  siteName,
  sectionTitle,
  busy,
  useContentBrowser = true,
  onCancel,
  onSubmit,
}: ReplaceLandingPageDialogProps): React.ReactElement | null {
  const [pageId, setPageId] = useState("");
  const [pageLabel, setPageLabel] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);
  const [browserOpen, setBrowserOpen] = useState(false);

  useDialogEscape(open && !browserOpen, busy, onCancel);

  useEffect(() => {
    if (open) {
      setPageId("");
      setPageLabel("");
      setLocalError(null);
      setBrowserOpen(useContentBrowser);
    }
  }, [open, useContentBrowser]);

  if (!open) {
    return null;
  }

  const initialPath = siteName.trim()
    ? `//Sites/${siteName.trim()}`
    : "//Sites";

  const applyPick = (selection: SelectionResult | null | undefined): boolean => {
    const pick = resolveLandingPagePick(selection);
    if (!pick.ok) {
      setLocalError(
        pick.error === "notPage"
          ? ARCH_MSG.LANDING_NOT_A_PAGE
          : ARCH_MSG.LANDING_NO_PAGE,
      );
      return false;
    }
    setPageId(pick.id);
    setPageLabel(pick.label);
    setLocalError(null);
    return true;
  };

  const handleBrowserConfirm = (selection: SelectionResult) => {
    if (!applyPick(selection)) {
      return;
    }
    setBrowserOpen(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const id = pageId.trim();
    if (!id) {
      setLocalError(ARCH_MSG.LANDING_NO_PAGE);
      return;
    }
    onSubmit(id);
  };

  return (
    <div
      style={overlayStyle}
      role="presentation"
      data-testid="architecture-landing-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-landing-title"
        data-testid="architecture-landing-dialog"
        style={panelStyle}
      >
        <h2
          id="architecture-landing-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.15rem" }}
        >
          {ARCH_MSG.LANDING_DIALOG_TITLE}
        </h2>
        <p
          style={{
            margin: "0 0 8px",
            color: catalogColors.muted,
            fontSize: "0.9rem",
          }}
          data-testid="architecture-landing-section"
        >
          {ARCH_MSG.LANDING_SECTION_LABEL}: {sectionTitle}
        </p>
        <p
          style={{
            margin: "0 0 12px",
            color: catalogColors.muted,
            fontSize: "0.9rem",
          }}
        >
          {ARCH_MSG.LANDING_HINT}
        </p>

        {useContentBrowser && browserOpen ? (
          <div data-testid="architecture-landing-browser">
            <ContentBrowser
              mode="select"
              multiSelect={false}
              allowFolderSelect={false}
              allowItemSelect
              allowedTypes={[...LANDING_PAGE_ALLOWED_TYPES]}
              roots="sites"
              initialPath={initialPath}
              enablePreview={false}
              enableSearch={false}
              title={ARCH_MSG.LANDING_PICKER_TITLE}
              onConfirm={handleBrowserConfirm}
              onCancel={() => setBrowserOpen(false)}
            />
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            {pageLabel ? (
              <p
                data-testid="architecture-landing-selected"
                style={{ margin: "0 0 12px", fontSize: "0.95rem" }}
              >
                {pageLabel}
              </p>
            ) : (
              <p
                data-testid="architecture-landing-empty"
                style={{
                  margin: "0 0 12px",
                  color: catalogColors.muted,
                  fontSize: "0.9rem",
                }}
              >
                {ARCH_MSG.LANDING_EMPTY_STATE}
              </p>
            )}
            {!useContentBrowser ? (
              <>
                <label
                  htmlFor="architecture-landing-page-id"
                  style={{ fontWeight: 600, fontSize: "0.9rem" }}
                >
                  {ARCH_MSG.LANDING_PAGE_ID_LABEL}
                </label>
                <input
                  id="architecture-landing-page-id"
                  data-testid="architecture-landing-page-id"
                  type="text"
                  value={pageId}
                  onChange={(e) => setPageId(e.target.value)}
                  disabled={busy}
                  aria-required="true"
                  style={{
                    display: "block",
                    width: "100%",
                    marginTop: 4,
                    marginBottom: 12,
                    padding: "0.4rem 0.5rem",
                    fontSize: "0.95rem",
                    boxSizing: "border-box",
                  }}
                />
              </>
            ) : (
              <button
                type="button"
                data-testid="architecture-landing-reopen-browser"
                disabled={busy}
                onClick={() => setBrowserOpen(true)}
                style={{
                  marginBottom: 12,
                  padding: "0.4rem 0.85rem",
                  border: `1px solid ${catalogColors.softBorder}`,
                  borderRadius: 4,
                  background: "#fff",
                  cursor: busy ? "not-allowed" : "pointer",
                }}
              >
                {ARCH_MSG.LANDING_PICKER_TITLE}
              </button>
            )}
            {localError ? (
              <p
                role="alert"
                data-testid="architecture-landing-error"
                style={{ color: catalogColors.error, fontSize: "0.9rem" }}
              >
                {localError}
              </p>
            ) : null}
            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                gap: 8,
                marginTop: 8,
              }}
            >
              <button
                type="button"
                data-testid="architecture-landing-cancel"
                disabled={busy}
                onClick={onCancel}
                style={{
                  padding: "0.4rem 0.85rem",
                  border: `1px solid ${catalogColors.softBorder}`,
                  borderRadius: 4,
                  background: "#fff",
                  cursor: busy ? "not-allowed" : "pointer",
                }}
              >
                {ARCH_MSG.LANDING_CANCEL}
              </button>
              <button
                type="submit"
                data-testid="architecture-landing-submit"
                disabled={busy || !pageId.trim()}
                style={{
                  padding: "0.4rem 0.85rem",
                  border: `1px solid ${catalogColors.accent}`,
                  borderRadius: 4,
                  background:
                    busy || !pageId.trim() ? "#f0f0f0" : catalogColors.accent,
                  color: busy || !pageId.trim() ? "#999" : "#fff",
                  cursor:
                    busy || !pageId.trim() ? "not-allowed" : "pointer",
                }}
              >
                {ARCH_MSG.LANDING_SUBMIT}
              </button>
            </div>
          </form>
        )}

        {useContentBrowser && browserOpen ? (
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              marginTop: 12,
            }}
          >
            <button
              type="button"
              data-testid="architecture-landing-cancel"
              disabled={busy}
              onClick={onCancel}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: 4,
                background: "#fff",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {ARCH_MSG.LANDING_CANCEL}
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default ReplaceLandingPageDialog;
