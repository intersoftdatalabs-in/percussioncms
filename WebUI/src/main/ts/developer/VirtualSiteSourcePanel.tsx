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
  publishVirtualSite,
  updateVirtualSiteProperties,
  virtualSitePreviewContentHref,
} from "../api/developer/sitesApi";
import type {
  VirtualSiteBuildResult,
  VirtualSitePublishResult,
} from "../api/developer/types";
import {
  catalogColors,
  errorAlert,
  mutedHintText,
  mutedText,
} from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { copyTextToClipboard } from "./templateSourceViewer";
import {
  formatVirtualSiteBuildSummary,
  formatVirtualSitePublishSummary,
  sanitizeVirtualPreviewHomePath,
  shouldShowVirtualBuildChrome,
  shouldShowVirtualPublishChrome,
} from "./virtualSiteBuild";
import {
  SOURCE_KIND_CSV_FILESYSTEM,
  SOURCE_KIND_GIT_FILESYSTEM,
  SOURCE_KIND_REPOSITORY,
  emptyVirtualSiteForm,
  formToVirtualProps,
  isCsvFilesystemSourceKind,
  isGitFilesystemSourceKind,
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
 * build ({@code POST …/virtual/build}) for git-filesystem and csv-filesystem, or
 * publish ({@code POST …/virtual/publish}) for git-filesystem.
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
  const [publishing, setPublishing] = useState(false);
  /** Load-time failure (hides form; retry reloads). */
  const [loadError, setLoadError] = useState<string | null>(null);
  /** Save / client validation failure (keeps form mounted). */
  const [formError, setFormError] = useState<string | null>(null);
  const [savedNotice, setSavedNotice] = useState<string | null>(null);
  const [isVirtual, setIsVirtual] = useState(false);
  const [buildError, setBuildError] = useState<string | null>(null);
  const [buildResult, setBuildResult] = useState<VirtualSiteBuildResult | null>(null);
  const [publishError, setPublishError] = useState<string | null>(null);
  const [publishResult, setPublishResult] = useState<VirtualSitePublishResult | null>(
    null,
  );
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [previewBusy, setPreviewBusy] = useState(false);
  const [linkCopyNotice, setLinkCopyNotice] = useState<string | null>(null);

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
    setPublishError(null);
    setPublishResult(null);
    setPreviewError(null);
    setLinkCopyNotice(null);
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
      // GET-roundtrip so virtual=true / sourceKind persist without a full page reload (#3365).
      let confirmed = saved;
      try {
        confirmed = await getVirtualSiteProperties(siteName);
      } catch {
        // Persist already succeeded; keep PUT result.
      }
      setForm(virtualPropsToForm(confirmed));
      setIsVirtual(
        confirmed.virtual === true || isVirtualSourceKind(confirmed.sourceKind),
      );
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
    setLinkCopyNotice(null);
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

  async function onPublish(): Promise<void> {
    // Publish uses *saved* server properties — validate form only as a soft gate.
    const clientErr = validateVirtualSiteForm(form);
    if (clientErr) {
      setPublishError(validationMessage(clientErr));
      setPublishResult(null);
      return;
    }
    setPublishing(true);
    setPublishError(null);
    setPublishResult(null);
    setBuildError(null);
    setPreviewError(null);
    setFormError(null);
    try {
      const result = await publishVirtualSite(siteName);
      setPublishResult(result);
    } catch (e: unknown) {
      setPublishError(panelErrMsg(e, DEV_MSG.SITE_VIRT_PUBLISH_ERROR));
    } finally {
      setPublishing(false);
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

  async function onCopyLinkProblems(lines: string[]): Promise<void> {
    if (lines.length === 0) {
      return;
    }
    const ok = await copyTextToClipboard(lines.join("\n"));
    setLinkCopyNotice(ok ? DEV_MSG.SITE_VIRT_BUILD_LINK_COPIED : null);
  }

  const virtualMode = isVirtualSourceKind(form.sourceKind);
  const gitMode = isGitFilesystemSourceKind(form.sourceKind);
  const csvMode = isCsvFilesystemSourceKind(form.sourceKind);
  /** Build chrome: git-filesystem and csv-filesystem (never repository). */
  const showBuildChrome = shouldShowVirtualBuildChrome(form.sourceKind);
  /** Publish chrome: git-filesystem only (CSV publish is a later slice). */
  const showPublishChrome = shouldShowVirtualPublishChrome(form.sourceKind);
  const busy = saving || building || publishing;
  const buildSummary = buildResult ? formatVirtualSiteBuildSummary(buildResult) : null;
  const publishSummary = publishResult
    ? formatVirtualSitePublishSummary(publishResult)
    : null;

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
              <option value={SOURCE_KIND_CSV_FILESYSTEM}>
                {DEV_MSG.SITE_VIRT_KIND_CSV_FILESYSTEM}
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
              {csvMode ? (
                <p
                  style={{ ...mutedHintText, margin: "0 0 10px" }}
                  data-testid="developer-site-virtual-csv-hint"
                >
                  {DEV_MSG.SITE_VIRT_CSV_HINT}
                </p>
              ) : null}
              {gitMode ? (
                <>
                  <div style={formRow}>
                    <label htmlFor="site-virt-remote-url">{DEV_MSG.SITE_VIRT_REMOTE_URL}</label>
                    <input
                      id="site-virt-remote-url"
                      data-testid="developer-site-virtual-remote-url"
                      type="text"
                      value={form.remoteUrl}
                      onChange={(e) => setForm((prev) => ({ ...prev, remoteUrl: e.target.value }))}
                      style={inputStyle}
                      placeholder="https://git.example.com/org/docs.git"
                      autoComplete="off"
                      spellCheck={false}
                      disabled={busy}
                    />
                  </div>
                  <div style={formRow}>
                    <label htmlFor="site-virt-branch">{DEV_MSG.SITE_VIRT_BRANCH}</label>
                    <input
                      id="site-virt-branch"
                      data-testid="developer-site-virtual-branch"
                      type="text"
                      value={form.branch}
                      onChange={(e) => setForm((prev) => ({ ...prev, branch: e.target.value }))}
                      style={inputStyle}
                      placeholder="main"
                      autoComplete="off"
                      spellCheck={false}
                      disabled={busy}
                    />
                  </div>
                  <p
                    style={{ ...mutedHintText, margin: "0 0 10px" }}
                    data-testid="developer-site-virtual-remote-hint"
                  >
                    {DEV_MSG.SITE_VIRT_REMOTE_HINT}
                  </p>
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

          {/* Build chrome: git-filesystem and csv-filesystem (never repository). */}
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
                {showPublishChrome ? (
                  <button
                    type="button"
                    data-testid="developer-site-virtual-publish"
                    style={busy ? disabledSecondaryButton : secondaryButton}
                    disabled={busy}
                    onClick={() => void onPublish()}
                  >
                    {publishing ? DEV_MSG.SITE_VIRT_PUBLISHING : DEV_MSG.SITE_VIRT_PUBLISH}
                  </button>
                ) : null}
              </div>
              <p
                style={{ ...mutedHintText, margin: "8px 0 0" }}
                data-testid="developer-site-virtual-preview-hint"
              >
                {DEV_MSG.SITE_VIRT_PREVIEW_HINT}
              </p>
              {showPublishChrome ? (
                <>
                  <p
                    style={{ ...mutedHintText, margin: "8px 0 0" }}
                    data-testid="developer-site-virtual-publish-hint"
                  >
                    {DEV_MSG.SITE_VIRT_PUBLISH_HINT}
                  </p>
                  <p
                    style={{ ...mutedHintText, margin: "8px 0 0" }}
                    data-testid="developer-site-virtual-publish-save-first"
                  >
                    {DEV_MSG.SITE_VIRT_PUBLISH_SAVE_FIRST}
                  </p>
                </>
              ) : null}

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
                      style={{ marginTop: "8px", color: catalogColors.error }}
                    >
                      <div>
                        {DEV_MSG.SITE_VIRT_BUILD_LINK_PROBLEMS}: {buildSummary.linkLine}
                      </div>
                      <p
                        style={{ ...mutedHintText, margin: "6px 0 0", color: catalogColors.error }}
                        data-testid="developer-site-virtual-build-link-report-hint"
                      >
                        {DEV_MSG.SITE_VIRT_BUILD_LINK_REPORT_HINT}
                      </p>
                      {buildSummary.linkProblems.length > 0 ? (
                        <details
                          data-testid="developer-site-virtual-build-link-details"
                          style={{ marginTop: "8px" }}
                        >
                          <summary
                            data-testid="developer-site-virtual-build-link-toggle"
                            style={{ cursor: "pointer" }}
                          >
                            {DEV_MSG.SITE_VIRT_BUILD_LINK_DETAILS}
                          </summary>
                          <div
                            style={{
                              display: "flex",
                              gap: "12px",
                              alignItems: "center",
                              marginTop: "8px",
                              flexWrap: "wrap",
                            }}
                          >
                            <button
                              type="button"
                              data-testid="developer-site-virtual-build-link-copy"
                              style={secondaryButton}
                              onClick={() =>
                                void onCopyLinkProblems(buildSummary.linkProblems)
                              }
                            >
                              {DEV_MSG.SITE_VIRT_BUILD_LINK_COPY}
                            </button>
                            {linkCopyNotice ? (
                              <span data-testid="developer-site-virtual-build-link-copied">
                                {linkCopyNotice}
                              </span>
                            ) : null}
                          </div>
                          <ul
                            data-testid="developer-site-virtual-build-link-list"
                            style={{
                              ...buildResultMono,
                              margin: "8px 0 0",
                              paddingLeft: "1.25rem",
                              maxHeight: "16rem",
                              overflow: "auto",
                            }}
                          >
                            {buildSummary.linkProblems.map((line, index) => (
                              <li
                                key={`${index}:${line}`}
                                data-testid="developer-site-virtual-build-link-line"
                              >
                                {line}
                              </li>
                            ))}
                          </ul>
                        </details>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              ) : null}

              {showPublishChrome && publishing ? (
                <div data-testid="developer-site-virtual-publish-busy" style={{ ...mutedText, marginTop: "10px" }}>
                  {DEV_MSG.SITE_VIRT_PUBLISHING}
                </div>
              ) : null}

              {showPublishChrome && publishError ? (
                <div
                  role="alert"
                  data-testid="developer-site-virtual-publish-error"
                  style={{ ...errorAlert, marginTop: "10px" }}
                >
                  {publishError}
                </div>
              ) : null}

              {showPublishChrome && publishResult && publishSummary ? (
                <div data-testid="developer-site-virtual-publish-result" style={buildResultBox}>
                  <div data-testid="developer-site-virtual-publish-success" style={successNotice}>
                    {DEV_MSG.SITE_VIRT_PUBLISH_SUCCESS}
                  </div>
                  <div style={{ marginTop: "6px" }}>
                    <span>{DEV_MSG.SITE_VIRT_PUBLISH_FILES}: </span>
                    <span data-testid="developer-site-virtual-publish-files" style={buildResultMono}>
                      {publishSummary.filesLine}
                    </span>
                  </div>
                  {publishSummary.destLine ? (
                    <div style={{ marginTop: "4px" }}>
                      <span>{DEV_MSG.SITE_VIRT_PUBLISH_DEST}: </span>
                      <span data-testid="developer-site-virtual-publish-dest" style={buildResultMono}>
                        {publishSummary.destLine}
                      </span>
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
