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
 * Create / edit external-link dialog for Architecture (#3097).
 * Peer: PercEditSectionLinksDialog (external-link mode).
 */

import React, { useEffect, useState } from "react";
import {
  validateExternalUrl,
  validateSectionTitle,
} from "../api/architecture/sectionMutations";
import type { SectionTarget } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";

export interface ExternalLinkDialogValues {
  linkTitle: string;
  externalUrl: string;
  target: SectionTarget;
}

export interface ExternalLinkDialogProps {
  open: boolean;
  mode: "create" | "edit";
  parentTitle: string;
  busy: boolean;
  /** When editing, seed fields from loaded section. */
  initial?: Partial<ExternalLinkDialogValues> | null;
  onCancel: () => void;
  onSubmit: (values: ExternalLinkDialogValues) => void;
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
  maxWidth: 440,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

const fieldStyle: React.CSSProperties = {
  display: "block",
  width: "100%",
  marginTop: 4,
  marginBottom: 12,
  padding: "0.4rem 0.5rem",
  fontSize: "0.95rem",
  boxSizing: "border-box",
};

export function ExternalLinkDialog({
  open,
  mode,
  parentTitle,
  busy,
  initial = null,
  onCancel,
  onSubmit,
}: ExternalLinkDialogProps): React.ReactElement | null {
  const [linkTitle, setLinkTitle] = useState("");
  const [externalUrl, setExternalUrl] = useState("");
  const [target, setTarget] = useState<SectionTarget>("_self");
  const [localError, setLocalError] = useState<string | null>(null);

  // Seed fields when the dialog opens or when async edit initial arrives after
  // open (externalInitial starts null). Do not re-seed on unrelated re-renders
  // once the user is typing with stable initial values.
  const seedKey = open
    ? `${initial?.linkTitle ?? ""}|${initial?.externalUrl ?? ""}|${initial?.target ?? ""}`
    : "";
  useEffect(() => {
    if (!open) return;
    setLinkTitle(initial?.linkTitle ?? "");
    setExternalUrl(initial?.externalUrl ?? "");
    setTarget(initial?.target ?? "_self");
    setLocalError(null);
    // seedKey collapses open + initial payload so late async initial still applies
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, seedKey]);

  if (!open) {
    return null;
  }

  const title =
    mode === "edit"
      ? ARCH_MSG.EXTERNAL_LINK_EDIT_TITLE
      : ARCH_MSG.EXTERNAL_LINK_DIALOG_TITLE;
  const submitLabel =
    mode === "edit"
      ? ARCH_MSG.EXTERNAL_LINK_SAVE
      : ARCH_MSG.EXTERNAL_LINK_SUBMIT;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const titleErr = validateSectionTitle(linkTitle);
    if (titleErr) {
      setLocalError(titleErr);
      return;
    }
    const urlErr = validateExternalUrl(externalUrl);
    if (urlErr) {
      setLocalError(urlErr);
      return;
    }
    onSubmit({
      linkTitle: linkTitle.trim(),
      externalUrl: externalUrl.trim(),
      target,
    });
  };

  return (
    <div
      style={overlayStyle}
      role="presentation"
      data-testid="architecture-external-link-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-external-link-title"
        data-testid="architecture-external-link-dialog"
        style={panelStyle}
      >
        <h2
          id="architecture-external-link-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.15rem" }}
        >
          {title}
        </h2>
        {mode === "create" ? (
          <p
            style={{
              margin: "0 0 12px",
              color: catalogColors.muted,
              fontSize: "0.9rem",
            }}
            data-testid="architecture-external-link-parent"
          >
            {ARCH_MSG.CREATE_PARENT_LABEL}: {parentTitle}
          </p>
        ) : null}
        <form onSubmit={handleSubmit}>
          <label
            htmlFor="architecture-external-link-text"
            style={{ fontWeight: 600, fontSize: "0.9rem" }}
          >
            {ARCH_MSG.EXTERNAL_LINK_TEXT_LABEL}
          </label>
          <input
            id="architecture-external-link-text"
            data-testid="architecture-external-link-text"
            type="text"
            value={linkTitle}
            onChange={(e) => setLinkTitle(e.target.value)}
            disabled={busy}
            maxLength={512}
            aria-required="true"
            style={fieldStyle}
          />
          <label
            htmlFor="architecture-external-link-url"
            style={{ fontWeight: 600, fontSize: "0.9rem" }}
          >
            {ARCH_MSG.EXTERNAL_LINK_URL_LABEL}
          </label>
          <input
            id="architecture-external-link-url"
            data-testid="architecture-external-link-url"
            type="text"
            value={externalUrl}
            onChange={(e) => setExternalUrl(e.target.value)}
            disabled={busy}
            maxLength={2048}
            aria-required="true"
            placeholder="https://example.com"
            style={fieldStyle}
          />
          <label
            htmlFor="architecture-external-link-target"
            style={{ fontWeight: 600, fontSize: "0.9rem" }}
          >
            {ARCH_MSG.EXTERNAL_LINK_TARGET_LABEL}
          </label>
          <select
            id="architecture-external-link-target"
            data-testid="architecture-external-link-target"
            value={target}
            onChange={(e) => setTarget(e.target.value as SectionTarget)}
            disabled={busy}
            style={fieldStyle}
          >
            <option value="_self">{ARCH_MSG.EXTERNAL_LINK_TARGET_SELF}</option>
            <option value="_blank">
              {ARCH_MSG.EXTERNAL_LINK_TARGET_BLANK}
            </option>
            <option value="_top">{ARCH_MSG.EXTERNAL_LINK_TARGET_TOP}</option>
            <option value="_parent">
              {ARCH_MSG.EXTERNAL_LINK_TARGET_PARENT}
            </option>
          </select>
          {localError ? (
            <p
              role="alert"
              data-testid="architecture-external-link-error"
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
              data-testid="architecture-external-link-cancel"
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
              {ARCH_MSG.EXTERNAL_LINK_CANCEL}
            </button>
            <button
              type="submit"
              data-testid="architecture-external-link-submit"
              disabled={busy}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.accent}`,
                borderRadius: 4,
                background: busy ? "#f0f0f0" : catalogColors.accent,
                color: busy ? "#999" : "#fff",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ExternalLinkDialog;
