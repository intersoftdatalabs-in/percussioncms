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
import { isApiError, isSessionRedirectError } from "../api/client";
import { createSiteSection, loadSection, loadSectionTree } from "../api/architecture/sectionApi";
import type { NavTreeNode } from "../api/architecture/types";
import type { SiteDef } from "../api/developer/types";
import { fetchTemplatesForSectionCreate } from "../api/home/homeApi";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  buildDeveloperAddSectionFields,
  isDeveloperNavSectionReadOnly,
  listDeveloperNavParents,
  listDeveloperSectionTitles,
  validateDeveloperSectionName,
  type DeveloperNavParentOption,
} from "./siteNavSection";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

/**
 * Add one navigation section (name + parent) on a Developer site.
 * Cancel does not post. Reorder and delete are not offered.
 */
export function SiteNavSections({ site }: { site: SiteDef }): React.ReactElement {
  const siteName = (site.name || "").trim();
  const readOnly = isDeveloperNavSectionReadOnly(site);
  const [titles, setTitles] = useState<string[]>([]);
  const [parents, setParents] = useState<DeveloperNavParentOption[]>([]);
  const [parentId, setParentId] = useState("");
  const [name, setName] = useState("");
  const [templateId, setTemplateId] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);

  const applyTree = useCallback(
    (root: NavTreeNode | null) => {
      const nextParents = listDeveloperNavParents(root, siteName);
      setTitles(listDeveloperSectionTitles(root));
      setParents(nextParents);
      setParentId((current) =>
        nextParents.some((p) => p.id === current) ? current : (nextParents[0]?.id ?? ""),
      );
    },
    [siteName],
  );

  useEffect(() => {
    if (!siteName || readOnly) {
      setTitles([]);
      setParents([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    void (async () => {
      try {
        const [root, templates] = await Promise.all([
          loadSectionTree(siteName),
          fetchTemplatesForSectionCreate(siteName),
        ]);
        if (cancelled) return;
        applyTree(root);
        setTemplateId(templates[0]?.id ?? "");
      } catch (err) {
        if (cancelled || isSessionRedirectError(err)) return;
        setLoadError(panelErrMsg(err, DEV_MSG.SITE_NAV_LOAD_ERROR));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [siteName, readOnly, reloadToken, applyTree]);

  const onCancel = () => {
    setName("");
    setError(null);
    setNotice(null);
  };

  const onAdd = () => {
    setError(null);
    setNotice(null);
    const nameError = validateDeveloperSectionName(name);
    if (nameError) {
      setError(DEV_MSG.SITE_NAV_INVALID);
      return;
    }
    if (!templateId.trim()) {
      setError(DEV_MSG.SITE_NAV_ERROR);
      return;
    }
    const parent = parents.find((p) => p.id === parentId) ?? parents[0];
    setBusy(true);
    void (async () => {
      try {
        let loadedFolderPath: string | null = null;
        if (parent?.node && !parent.node.folderPath?.trim() && parent.node.id) {
          const loaded = await loadSection(parent.node.id);
          loadedFolderPath = loaded.folderPath ?? null;
        }
        await createSiteSection(
          buildDeveloperAddSectionFields({
            name,
            siteName,
            parent: parent?.node ?? null,
            loadedFolderPath,
            templateId,
          }),
        );
        setName("");
        setNotice(DEV_MSG.SITE_NAV_ADDED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setError(panelErrMsg(err, DEV_MSG.SITE_NAV_FORBIDDEN));
          return;
        }
        setError(panelErrMsg(err, DEV_MSG.SITE_NAV_ERROR));
      } finally {
        setBusy(false);
      }
    })();
  };

  return (
    <section data-testid="developer-site-nav" style={{ marginTop: "16px" }}>
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SITE_NAV_TITLE}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.SITE_NAV_HINT}</p>
      {readOnly ? (
        <p data-testid="developer-site-nav-readonly">{DEV_MSG.SITE_NAV_READONLY}</p>
      ) : (
        <>
          {loading ? <p data-testid="developer-site-nav-loading">{DEV_MSG.SITE_NAV_LOADING}</p> : null}
          {loadError ? (
            <div data-testid="developer-site-nav-load-error" role="alert">
              {loadError}
            </div>
          ) : null}
          <div data-testid="developer-site-nav-list">
            <div style={{ fontSize: "0.9rem", marginBottom: "6px" }}>{DEV_MSG.SITE_NAV_LIST}</div>
            {titles.length === 0 ? (
              <p data-testid="developer-site-nav-empty">{DEV_MSG.SITE_NAV_EMPTY}</p>
            ) : (
              <ul>
                {titles.map((title, index) => (
                  <li key={`${title}-${index}`} data-testid="developer-site-nav-item">
                    {title}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "8px" }}>
            <label>
              {DEV_MSG.SITE_NAV_NAME}
              <input
                data-testid="developer-site-nav-name"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  setError(null);
                }}
                style={inputStyle}
                disabled={busy}
              />
            </label>
            <label>
              {DEV_MSG.SITE_NAV_PARENT}
              <select
                data-testid="developer-site-nav-parent"
                value={parentId}
                onChange={(e) => setParentId(e.target.value)}
                style={inputStyle}
                disabled={busy || parents.length === 0}
              >
                {parents.map((parent) => (
                  <option key={parent.id || "root"} value={parent.id}>
                    {parent.title}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-add"
                disabled={busy}
                onClick={onAdd}
              >
                {busy ? DEV_MSG.SITE_NAV_ADDING : DEV_MSG.SITE_NAV_ADD}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-cancel"
                disabled={busy}
                onClick={onCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
          </div>
          {error ? (
            <div data-testid="developer-site-nav-error" role="alert">
              {error}
            </div>
          ) : null}
          {notice ? <div data-testid="developer-site-nav-notice">{notice}</div> : null}
        </>
      )}
    </section>
  );
}
