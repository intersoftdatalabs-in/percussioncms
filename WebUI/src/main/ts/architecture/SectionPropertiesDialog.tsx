/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
 * Section properties dialog (CM1 perc_editSectionDialog parity) for Navigation.
 * Parent loads GET /section/properties and posts POST /section/update.
 */

import React, { useEffect, useState } from "react";
import {
  canToggleRequiresLogin,
  validateSectionPropertiesForm,
} from "../api/architecture/sectionMutations";
import type { SectionPropertiesFormValues } from "../api/architecture/sectionMutations";
import type { SiteSectionPropertiesWire } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface SectionPropertiesDialogProps {
  open: boolean;
  busy: boolean;
  loading: boolean;
  loadError: string | null;
  initial: SiteSectionPropertiesWire | null;
  onCancel: () => void;
  onSubmit: (values: SectionPropertiesFormValues) => void;
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
  maxWidth: 480,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
  maxHeight: "90vh",
  overflowY: "auto",
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

const emptyForm: SectionPropertiesFormValues = {
  title: "",
  folderName: "",
  target: "_self",
  cssClassNames: "",
  requiresLogin: false,
  allowAccessTo: "",
};

function formFromInitial(
  initial: SiteSectionPropertiesWire | null,
): SectionPropertiesFormValues {
  if (!initial) {
    return { ...emptyForm };
  }
  return {
    title: initial.title ?? "",
    folderName: initial.folderName ?? "",
    target: initial.target ? String(initial.target) : "_self",
    cssClassNames: initial.cssClassNames != null ? String(initial.cssClassNames) : "",
    requiresLogin: Boolean(initial.requiresLogin),
    allowAccessTo:
      initial.allowAccessTo != null ? String(initial.allowAccessTo) : "",
  };
}

export function SectionPropertiesDialog({
  open,
  busy,
  loading,
  loadError,
  initial,
  onCancel,
  onSubmit,
}: SectionPropertiesDialogProps): React.ReactElement | null {
  const [form, setForm] = useState<SectionPropertiesFormValues>(emptyForm);
  const [localError, setLocalError] = useState<string | null>(null);

  useDialogEscape(open, busy, onCancel);

  const seedKey = open
    ? `${initial?.id ?? ""}|${initial?.title ?? ""}|${initial?.folderName ?? ""}`
    : "";
  useEffect(() => {
    if (!open) return;
    setForm(formFromInitial(initial));
    setLocalError(null);
    // seedKey collapses open + loaded identity so late GET still applies
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, seedKey]);

  if (!open) {
    return null;
  }

  const folderLocked = Boolean(initial?.siteRootSection);
  const loginEditable = canToggleRequiresLogin(initial);
  const groupsEnabled = loginEditable && form.requiresLogin && !busy && !loading;
  const formDisabled = busy || loading || !initial || !!loadError;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    if (!initial || loadError) {
      return;
    }
    const err = validateSectionPropertiesForm(form, {
      folderNameLocked: folderLocked,
    });
    if (err) {
      setLocalError(err);
      return;
    }
    onSubmit({
      title: form.title.trim(),
      folderName: folderLocked ? initial.folderName : form.folderName.trim(),
      target: form.target || "_self",
      cssClassNames: form.cssClassNames.replace(/ +/g, " ").trim(),
      requiresLogin: form.requiresLogin,
      allowAccessTo: form.requiresLogin ? form.allowAccessTo.trim() : "",
    });
  };

  const update = <K extends keyof SectionPropertiesFormValues>(
    key: K,
    value: SectionPropertiesFormValues[K],
  ) => {
    setLocalError(null);
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  return (
    <div
      style={overlayStyle}
      data-testid="architecture-properties-dialog"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-properties-title"
        style={panelStyle}
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="architecture-properties-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.1rem" }}
        >
          {ARCH_MSG.PROPERTIES_DIALOG_TITLE}
        </h2>
        <p
          style={{
            margin: "0 0 12px",
            color: catalogColors.muted,
            fontSize: "0.85rem",
          }}
        >
          {ARCH_MSG.PROPERTIES_HINT}
        </p>
        {loading ? (
          <p
            data-testid="architecture-properties-loading"
            aria-live="polite"
            style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
          >
            {ARCH_MSG.PROPERTIES_LOADING}
          </p>
        ) : null}
        {loadError ? (
          <p
            role="alert"
            data-testid="architecture-properties-load-error"
            style={{ color: catalogColors.error, fontSize: "0.9rem" }}
          >
            {loadError}
          </p>
        ) : null}
        <form onSubmit={handleSubmit}>
          <label
            htmlFor="architecture-properties-title-input"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.PROPERTIES_TITLE_LABEL}
            <input
              id="architecture-properties-title-input"
              data-testid="architecture-properties-title"
              value={form.title}
              onChange={(e) => update("title", e.target.value)}
              required
              disabled={formDisabled}
              autoFocus={!loading}
              maxLength={512}
              style={fieldStyle}
            />
          </label>
          <label
            htmlFor="architecture-properties-folder"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.PROPERTIES_FOLDER_LABEL}
            <input
              id="architecture-properties-folder"
              data-testid="architecture-properties-folder"
              value={form.folderName}
              onChange={(e) => update("folderName", e.target.value)}
              required={!folderLocked}
              disabled={formDisabled || folderLocked}
              maxLength={100}
              style={fieldStyle}
            />
          </label>
          {folderLocked ? (
            <p
              data-testid="architecture-properties-folder-locked"
              style={{
                margin: "-8px 0 12px",
                color: catalogColors.muted,
                fontSize: "0.8rem",
              }}
            >
              {ARCH_MSG.PROPERTIES_FOLDER_ROOT_HINT}
            </p>
          ) : null}
          <label
            htmlFor="architecture-properties-target"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.PROPERTIES_TARGET_LABEL}
            <select
              id="architecture-properties-target"
              data-testid="architecture-properties-target"
              value={form.target}
              onChange={(e) => update("target", e.target.value)}
              disabled={formDisabled}
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
          </label>
          <label
            htmlFor="architecture-properties-css"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.PROPERTIES_CSS_LABEL}
            <input
              id="architecture-properties-css"
              data-testid="architecture-properties-css"
              value={form.cssClassNames}
              onChange={(e) => update("cssClassNames", e.target.value)}
              disabled={formDisabled}
              maxLength={255}
              style={fieldStyle}
            />
          </label>
          <label
            htmlFor="architecture-properties-login"
            style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              fontSize: "0.9rem",
              marginBottom: 8,
            }}
          >
            <input
              id="architecture-properties-login"
              data-testid="architecture-properties-login"
              type="checkbox"
              checked={form.requiresLogin}
              disabled={formDisabled || !loginEditable}
              onChange={(e) => update("requiresLogin", e.target.checked)}
            />
            {ARCH_MSG.PROPERTIES_LOGIN_LABEL}
          </label>
          {!loginEditable && initial ? (
            <p
              data-testid="architecture-properties-login-locked"
              style={{
                margin: "0 0 8px",
                color: catalogColors.muted,
                fontSize: "0.8rem",
              }}
            >
              {ARCH_MSG.PROPERTIES_LOGIN_LOCKED}
            </p>
          ) : null}
          <label
            htmlFor="architecture-properties-groups"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.PROPERTIES_GROUPS_LABEL}
            <input
              id="architecture-properties-groups"
              data-testid="architecture-properties-groups"
              value={form.allowAccessTo}
              onChange={(e) => update("allowAccessTo", e.target.value)}
              disabled={!groupsEnabled}
              style={fieldStyle}
            />
          </label>
          <p
            style={{
              margin: "-8px 0 12px",
              color: catalogColors.muted,
              fontSize: "0.8rem",
            }}
          >
            {ARCH_MSG.PROPERTIES_GROUPS_HINT}
          </p>
          {localError ? (
            <p
              role="alert"
              data-testid="architecture-properties-error"
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
              data-testid="architecture-properties-cancel"
              onClick={onCancel}
              disabled={busy}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: 4,
                background: "#fff",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {ARCH_MSG.PROPERTIES_CANCEL}
            </button>
            <button
              type="submit"
              data-testid="architecture-properties-submit"
              disabled={formDisabled}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.accent}`,
                borderRadius: 4,
                background: catalogColors.accent,
                color: "#fff",
                cursor: formDisabled ? "not-allowed" : "pointer",
              }}
            >
              {busy ? ARCH_MSG.ACTION_BUSY : ARCH_MSG.PROPERTIES_SUBMIT}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SectionPropertiesDialog;
