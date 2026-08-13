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

import React, { useCallback, useEffect, useState } from "react";
import {
  buildVirtualSite,
  getVirtualSitePreviewStatus,
  getVirtualSiteProperties,
  updateVirtualSiteProperties,
  virtualSitePreviewContentHref,
} from "../api/developer/sitesApi";
import type { VirtualSiteBuildResult } from "../api/developer/types";
import {
  catalogColors,
  errorAlert,
  mutedHintText,
  mutedText,
} from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  formatVirtualSiteBuildSummary,
  sanitizeVirtualPreviewHomePath,
  shouldShowVirtualBuildChrome,
} from "./virtualSiteBuild";
import {
  SOURCE_KIND_GIT_FILESYSTEM,
  SOURCE_KIND_REPOSITORY,
  emptyVirtualSiteForm,
  formToVirtualProps,
  isVirtualSourceKind,
  validateVirtualSiteForm,
  virtualPropsToForm,
  type VirtualSiteFormModel,
} from "./virtualSiteForm";

const formRow: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(8rem, 12rem) 1fr",
  gap: "8px 12px",
  alignItems: "center",
  marginBottom: "10px",
  fontSize: "0.9rem",
};

const inputStyle: React.CSSProperties = {
  fontFamily: "monospace",
  fontSize: "0.9rem",
  padding: "6px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  width: "100%",
  boxSizing: "border-box",
};

const primaryButton: React.CSSProperties = {
  background: catalogColors.accent,
  color: "#fff",
  border: "none",
  borderRadius: "4px",
  padding: "8px 14px",
  cursor: "pointer",
  fontSize: "0.9rem",
};

const secondaryButton: React.CSSProperties = {
  background: "#fff",
  color: catalogColors.text,
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "8px 14px",
  cursor: "pointer",
  fontSize: "0.9rem",
};

const disabledButton: React.CSSProperties = {
  ...primaryButton,
  background: catalogColors.disabled,
  cursor: "not-allowed",
};

const disabledSecondaryButton: React.CSSProperties = {
  ...secondaryButton,
  color: catalogColors.disabled,
  borderColor: catalogColors.softBorder,
  cursor: "not-allowed",
};

const successNotice: React.CSSProperties = {
  color: catalogColors.accent,
  marginTop: "8px",
  fontSize: "0.9rem",
};

const buildResultBox: React.CSSProperties = {
  marginTop: "10px",
  padding: "10px 12px",
  border: `1px solid ${catalogColors.headerBorder}`,
  borderRadius: "4px",
  background: "#f7fafc",
  fontSize: "0.9rem",
};

const buildResultMono: React.CSSProperties = {
  fontFamily: "monospace",
  wordBreak: "break-all",
};

function validationMessage(
  code: ReturnType<typeof validateVirtualSiteForm>,
): string | null {
  if (code === "root-required") return DEV_MSG.SITE_VIRT_ERR_ROOT_REQUIRED;
  if (code === "root-unsafe") return DEV_MSG.SITE_VIRT_ERR_ROOT_UNSAFE;
  if (code === "config-unsafe") return DEV_MSG.SITE_VIRT_ERR_CONFIG_UNSAFE;
  return null;
}

/**
 * Site detail section: view/edit Virtual Site source fields via public Site REST
 * ({@code GET|PUT /services/sites/{name}/virtual}) and trigger a CMS-integrated
 * build ({@code POST …/virtual/build}) when source kind is Virtual.
 */
export function VirtualSiteSourcePanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [form, setForm] = useState<VirtualSiteFormModel>(emptyVirtualSiteForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [building, setBuilding] = useState(false);
  /** Load-time failure (hides form; retry reloads). */
  const [loadError, setLoadError] = useState<string | null>(null);
  /** Save / client validation failure (keeps form mounted). */
  const [formError, setFormError] = useState<string | null>(null);
  const [savedNotice, setSavedNotice] = useState<string | null>(null);
  const [isVirtual, setIsVirtual] = useState(false);
  const [buildError, setBuildError] = useState<string | null>(null);
  const [buildResult, setBuildResult] = useState<VirtualSiteBuildResult | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [previewBusy, setPreviewBusy] = useState(false);

  const load = useCallback(() => {
    if (!siteName.trim()) {
      setLoadError(DEV_MSG.SITE_VIRT_ERROR);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    setFormError(null);
    setSavedNotice(null);
    setBuildError(null);
    setBuildResult(null);
    setPreviewError(null);
    getVirtualSiteProperties(siteName)
      .then((props) => {
        if (cancelled) return;
        setForm(virtualPropsToForm(props));
        setIsVirtual(props.virtual === true || isVirtualSourceKind(props.sourceKind));
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setLoadError(panelErrMsg(e, DEV_MSG.SITE_VIRT_ERROR));
        setForm(emptyVirtualSiteForm());
        setIsVirtual(false);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [siteName]);

  useEffect(() => {
    return load();
  }, [load]);

  async function onSave(): Promise<void> {
    const clientErr = validateVirtualSiteForm(form);
    if (clientErr) {
      setFormError(validationMessage(clientErr));
      setSavedNotice(null);
      return;
    }
    setSaving(true);
    setFormError(null);
    setSavedNotice(null);
    setBuildError(null);
    try {
      const body = formToVirtualProps(form);
      const saved = await updateVirtualSiteProperties(siteName, body);
      setForm(virtualPropsToForm(saved));
      setIsVirtual(saved.virtual === true || isVirtualSourceKind(saved.sourceKind));
      setSavedNotice(DEV_MSG.SITE_VIRT_SAVED);
    } catch (e: unknown) {
      setFormError(panelErrMsg(e, DEV_MSG.SITE_VIRT_SAVE_ERROR));
    } finally {
      setSaving(false);
    }
  }

  async function onBuild(): Promise<void> {
    // Build uses *saved* server properties — validate form only as a soft gate
    // so operators see client-side issues before a wasted POST.
    const clientErr = validateVirtualSiteForm(form);
    if (clientErr) {
      setBuildError(validationMessage(clientErr));
      setBuildResult(null);
      return;
    }
    setBuilding(true);
    setBuildError(null);
    setBuildResult(null);
    setPreviewError(null);
    setFormError(null);
    try {
      const result = await buildVirtualSite(siteName);
      setBuildResult(result);
    } catch (e: unknown) {
      setBuildError(panelErrMsg(e, DEV_MSG.SITE_VIRT_BUILD_ERROR));
    } finally {
      setBuilding(false);
    }
  }

  async function onPreview(): Promise<void> {
    setPreviewBusy(true);
    setPreviewError(null);
    try {
      const status = await getVirtualSitePreviewStatus(siteName);
      if (status.available !== true) {
        setPreviewError(
          status.message?.trim() || DEV_MSG.SITE_VIRT_PREVIEW_MISSING,
        );
        return;
      }
      const home = sanitizeVirtualPreviewHomePath(status.homePath);
      if (!home) {
        setPreviewError(DEV_MSG.SITE_VIRT_PREVIEW_MISSING);
        return;
      }
      const href = virtualSitePreviewContentHref(siteName, home);
      window.open(href, "_blank", "noopener,noreferrer");
    } catch (e: unknown) {
      setPreviewError(panelErrMsg(e, DEV_MSG.SITE_VIRT_PREVIEW_ERROR));
    } finally {
      setPreviewBusy(false);
    }
  }

  const virtualMode = isVirtualSourceKind(form.sourceKind);
  /** Build chrome only for virtual source kinds (never for repository). */
  const showBuildChrome = shouldShowVirtualBuildChrome(form.sourceKind);
  const busy = saving || building;
  const buildSummary = buildResult ? formatVirtualSiteBuildSummary(buildResult) : null;

  return (
    <section
      data-testid="developer-site-virtual"
      style={{
        marginTop: "20px",
        marginBottom: "20px",
        paddingTop: "12px",
        borderTop: `1px solid ${catalogColors.headerBorder}`,
      }}
    >
      <h3 style={{ fontSize: "1rem", margin: "0 0 6px" }} data-testid="developer-site-virtual-title">
        {DEV_MSG.SITE_VIRT_TITLE}
      </h3>
      <p style={{ ...mutedHintText, margin: "0 0 12px" }}>{DEV_MSG.SITE_VIRT_HINT}</p>

      {loading ? (
        <div data-testid="developer-site-virtual-loading">{DEV_MSG.SITE_VIRT_LOADING}</div>
      ) : null}

      {!loading && loadError ? (
        <>
          <div role="alert" data-testid="developer-site-virtual-error" style={errorAlert}>
            {loadError}
          </div>
          <button
            type="button"
            data-testid="developer-site-virtual-retry"
            style={{ ...primaryButton, marginTop: "10px" }}
            onClick={() => load()}
          >
            {DEV_MSG.SITE_VIRT_RETRY}
          </button>
        </>
      ) : null}

      {!loading && !loadError ? (
        <div data-testid="developer-site-virtual-form">
          <p style={{ ...mutedText, margin: "0 0 10px" }} data-testid="developer-site-virtual-status">
            {isVirtual || virtualMode
              ? DEV_MSG.SITE_VIRT_STATUS_VIRTUAL
              : DEV_MSG.SITE_VIRT_STATUS_REPO}
          </p>

          {formError ? (
            <div role="alert" data-testid="developer-site-virtual-error" style={{ ...errorAlert, marginBottom: "10px" }}>
              {formError}
            </div>
          ) : null}

          <div style={formRow}>
            <label htmlFor="site-virt-source-kind">{DEV_MSG.SITE_VIRT_SOURCE_KIND}</label>
            <select
              id="site-virt-source-kind"
              data-testid="developer-site-virtual-source-kind"
              value={form.sourceKind}
              onChange={(e) =>
                setForm((prev) => ({
                  ...prev,
                  sourceKind: e.target.value as VirtualSiteFormModel["sourceKind"],
                }))
              }
              style={inputStyle}
              disabled={busy}
            >
              <option value={SOURCE_KIND_REPOSITORY}>{DEV_MSG.SITE_VIRT_KIND_REPOSITORY}</option>
              <option value={SOURCE_KIND_GIT_FILESYSTEM}>
                {DEV_MSG.SITE_VIRT_KIND_GIT_FILESYSTEM}
              </option>
            </select>
          </div>

          {virtualMode ? (
            <>
              <div style={formRow}>
                <label htmlFor="site-virt-root-path">{DEV_MSG.SITE_VIRT_ROOT_PATH}</label>
                <input
                  id="site-virt-root-path"
                  data-testid="developer-site-virtual-root-path"
                  type="text"
                  value={form.rootPath}
                  onChange={(e) => setForm((prev) => ({ ...prev, rootPath: e.target.value }))}
                  style={inputStyle}
                  autoComplete="off"
                  spellCheck={false}
                  disabled={busy}
                />
              </div>
              <div style={formRow}>
                <label htmlFor="site-virt-config-file">{DEV_MSG.SITE_VIRT_CONFIG_FILE}</label>
                <input
                  id="site-virt-config-file"
                  data-testid="developer-site-virtual-config-file"
                  type="text"
                  value={form.configFile}
                  onChange={(e) => setForm((prev) => ({ ...prev, configFile: e.target.value }))}
                  style={inputStyle}
                  placeholder="_config.yaml"
                  autoComplete="off"
                  spellCheck={false}
                  disabled={busy}
                />
              </div>
              <div style={formRow}>
                <label htmlFor="site-virt-site-key">{DEV_MSG.SITE_VIRT_SITE_KEY}</label>
                <input
                  id="site-virt-site-key"
                  data-testid="developer-site-virtual-site-key"
                  type="text"
                  value={form.siteKey}
                  onChange={(e) => setForm((prev) => ({ ...prev, siteKey: e.target.value }))}
                  style={inputStyle}
                  autoComplete="off"
                  spellCheck={false}
                  disabled={busy}
                />
              </div>
            </>
          ) : null}

          <div style={{ display: "flex", gap: "12px", alignItems: "center", marginTop: "8px", flexWrap: "wrap" }}>
            <button
              type="button"
              data-testid="developer-site-virtual-save"
              style={busy ? disabledButton : primaryButton}
              disabled={busy}
              onClick={() => void onSave()}
            >
              {saving ? DEV_MSG.SITE_VIRT_SAVING : DEV_MSG.SITE_VIRT_SAVE}
            </button>
            {savedNotice ? (
              <span data-testid="developer-site-virtual-saved" style={{ color: catalogColors.accent }}>
                {savedNotice}
              </span>
            ) : null}
          </div>

          {/* Build chrome: only when form source kind is Virtual (never for repository). */}
          {showBuildChrome ? (
            <div
              data-testid="developer-site-virtual-build-section"
              style={{
                marginTop: "16px",
                paddingTop: "12px",
                borderTop: `1px dashed ${catalogColors.headerBorder}`,
              }}
            >
              <p style={{ ...mutedHintText, margin: "0 0 8px" }} data-testid="developer-site-virtual-build-hint">
                {DEV_MSG.SITE_VIRT_BUILD_HINT}
              </p>
              <p style={{ ...mutedHintText, margin: "0 0 10px" }} data-testid="developer-site-virtual-build-save-first">
                {DEV_MSG.SITE_VIRT_BUILD_SAVE_FIRST}
              </p>
              <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "center" }}>
                <button
                  type="button"
                  data-testid="developer-site-virtual-build"
                  style={busy ? disabledSecondaryButton : secondaryButton}
                  disabled={busy}
                  onClick={() => void onBuild()}
                >
                  {building ? DEV_MSG.SITE_VIRT_BUILDING : DEV_MSG.SITE_VIRT_BUILD}
                </button>
                <button
                  type="button"
                  data-testid="developer-site-virtual-preview"
                  style={busy || previewBusy ? disabledSecondaryButton : secondaryButton}
                  disabled={busy || previewBusy}
                  onClick={() => void onPreview()}
                >
                  {DEV_MSG.SITE_VIRT_PREVIEW}
                </button>
              </div>
              <p
                style={{ ...mutedHintText, margin: "8px 0 0" }}
                data-testid="developer-site-virtual-preview-hint"
              >
                {DEV_MSG.SITE_VIRT_PREVIEW_HINT}
              </p>

              {building ? (
                <div data-testid="developer-site-virtual-build-busy" style={{ ...mutedText, marginTop: "10px" }}>
                  {DEV_MSG.SITE_VIRT_BUILDING}
                </div>
              ) : null}

              {buildError ? (
                <div
                  role="alert"
                  data-testid="developer-site-virtual-build-error"
                  style={{ ...errorAlert, marginTop: "10px" }}
                >
                  {buildError}
                </div>
              ) : null}

              {previewError ? (
                <div
                  role="alert"
                  data-testid="developer-site-virtual-preview-error"
                  style={{ ...errorAlert, marginTop: "10px" }}
                >
                  {previewError}
                </div>
              ) : null}

              {buildResult && buildSummary ? (
                <div data-testid="developer-site-virtual-build-result" style={buildResultBox}>
                  <div data-testid="developer-site-virtual-build-success" style={successNotice}>
                    {DEV_MSG.SITE_VIRT_BUILD_SUCCESS}
                  </div>
                  <div style={{ marginTop: "6px" }}>
                    <span>{DEV_MSG.SITE_VIRT_BUILD_PAGES}: </span>
                    <span data-testid="developer-site-virtual-build-pages" style={buildResultMono}>
                      {buildSummary.pagesLine}
                    </span>
                  </div>
                  {buildSummary.outputLine ? (
                    <div style={{ marginTop: "4px" }}>
                      <span>{DEV_MSG.SITE_VIRT_BUILD_OUTPUT}: </span>
                      <span data-testid="developer-site-virtual-build-output" style={buildResultMono}>
                        {buildSummary.outputLine}
                      </span>
                    </div>
                  ) : null}
                  {buildSummary.hasLinkProblems && buildSummary.linkLine ? (
                    <div
                      data-testid="developer-site-virtual-build-link-problems"
                      style={{ marginTop: "4px", color: catalogColors.error }}
                    >
                      {DEV_MSG.SITE_VIRT_BUILD_LINK_PROBLEMS}: {buildSummary.linkLine}
                    </div>
                  ) : null}
                </div>
              ) : null}
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
