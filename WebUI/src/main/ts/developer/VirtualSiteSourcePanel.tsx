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
  getVirtualSiteProperties,
  updateVirtualSiteProperties,
} from "../api/developer/sitesApi";
import {
  catalogColors,
  errorAlert,
  mutedHintText,
  mutedText,
} from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
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

const disabledButton: React.CSSProperties = {
  ...primaryButton,
  background: catalogColors.disabled,
  cursor: "not-allowed",
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
 * ({@code GET|PUT /services/sites/{name}/virtual}).
 */
export function VirtualSiteSourcePanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [form, setForm] = useState<VirtualSiteFormModel>(emptyVirtualSiteForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  /** Load-time failure (hides form; retry reloads). */
  const [loadError, setLoadError] = useState<string | null>(null);
  /** Save / client validation failure (keeps form mounted). */
  const [formError, setFormError] = useState<string | null>(null);
  const [savedNotice, setSavedNotice] = useState<string | null>(null);
  const [isVirtual, setIsVirtual] = useState(false);

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

  const virtualMode = isVirtualSourceKind(form.sourceKind);

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
                />
              </div>
            </>
          ) : null}

          <div style={{ display: "flex", gap: "12px", alignItems: "center", marginTop: "8px" }}>
            <button
              type="button"
              data-testid="developer-site-virtual-save"
              style={saving ? disabledButton : primaryButton}
              disabled={saving}
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
        </div>
      ) : null}
    </section>
  );
}
