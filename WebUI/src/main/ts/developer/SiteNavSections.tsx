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
import {
  createSiteSection,
  deleteSiteSection,
  loadSection,
  loadSectionProperties,
  loadSectionTree,
  updateSiteSection,
} from "../api/architecture/sectionApi";
import type { NavTreeNode } from "../api/architecture/types";
import type { SiteDef } from "../api/developer/types";
import { fetchTemplatesForSectionCreate } from "../api/home/homeApi";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  buildDeveloperAddSectionFields,
  buildDeveloperRenameProperties,
  isDeveloperNavSectionReadOnly,
  isDeveloperSectionNameTaken,
  listDeveloperDeleteTargets,
  listDeveloperNavParents,
  listDeveloperRenameTargets,
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
 * Add, rename, or delete one navigation section on a Developer site.
 * Cancel does not write. Reorder and reparent are not offered. The site root is not deleted.
 */
export function SiteNavSections({ site }: { site: SiteDef }): React.ReactElement {
  const siteName = (site.name || "").trim();
  const readOnly = isDeveloperNavSectionReadOnly(site);
  const [treeRoot, setTreeRoot] = useState<NavTreeNode | null>(null);
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
  const [renameId, setRenameId] = useState("");
  const [renameName, setRenameName] = useState("");
  const [renameError, setRenameError] = useState<string | null>(null);
  const [renameNotice, setRenameNotice] = useState<string | null>(null);
  const [renameBusy, setRenameBusy] = useState(false);
  const [deleteId, setDeleteId] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteNotice, setDeleteNotice] = useState<string | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const renameTargets = listDeveloperRenameTargets(treeRoot);
  const deleteTargets = listDeveloperDeleteTargets(treeRoot);

  const applyTree = useCallback(
    (root: NavTreeNode | null) => {
      const nextParents = listDeveloperNavParents(root, siteName);
      const nextTargets = listDeveloperRenameTargets(root);
      const nextDeletes = listDeveloperDeleteTargets(root);
      setTreeRoot(root);
      setTitles(listDeveloperSectionTitles(root));
      setParents(nextParents);
      setParentId((current) =>
        nextParents.some((p) => p.id === current) ? current : (nextParents[0]?.id ?? ""),
      );
      setRenameId((current) => {
        const kept = nextTargets.find((t) => t.id === current);
        const next = kept ?? nextTargets[0];
        setRenameName(next?.title ?? "");
        return next?.id ?? "";
      });
      setDeleteId((current) =>
        nextDeletes.some((t) => t.id === current) ? current : (nextDeletes[0]?.id ?? ""),
      );
    },
    [siteName],
  );

  useEffect(() => {
    if (!siteName || readOnly) {
      setTreeRoot(null);
      setTitles([]);
      setParents([]);
      setRenameId("");
      setRenameName("");
      setDeleteId("");
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

  const onSelectRename = (id: string) => {
    setRenameId(id);
    const target = renameTargets.find((t) => t.id === id);
    setRenameName(target?.title ?? "");
    setRenameError(null);
    setRenameNotice(null);
  };

  const onRenameCancel = () => {
    const target = renameTargets.find((t) => t.id === renameId);
    setRenameName(target?.title ?? "");
    setRenameError(null);
    setRenameNotice(null);
  };

  const onRename = () => {
    setRenameError(null);
    setRenameNotice(null);
    const nameError = validateDeveloperSectionName(renameName);
    if (nameError) {
      setRenameError(DEV_MSG.SITE_NAV_INVALID);
      return;
    }
    if (!renameId) {
      setRenameError(DEV_MSG.SITE_NAV_RENAME_ERROR);
      return;
    }
    const current = renameTargets.find((t) => t.id === renameId);
    if (current && current.title === renameName.trim()) {
      return;
    }
    if (isDeveloperSectionNameTaken(treeRoot, renameId, renameName)) {
      setRenameError(DEV_MSG.SITE_NAV_RENAME_DUPLICATE);
      return;
    }
    setRenameBusy(true);
    void (async () => {
      try {
        const props = await loadSectionProperties(renameId);
        await updateSiteSection(buildDeveloperRenameProperties(props, renameName));
        setRenameNotice(DEV_MSG.SITE_NAV_RENAMED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setRenameError(panelErrMsg(err, DEV_MSG.SITE_NAV_RENAME_FORBIDDEN));
          return;
        }
        setRenameError(panelErrMsg(err, DEV_MSG.SITE_NAV_RENAME_ERROR));
      } finally {
        setRenameBusy(false);
      }
    })();
  };

  const onSelectDelete = (id: string) => {
    setDeleteId(id);
    setDeleteError(null);
    setDeleteNotice(null);
  };

  const onDeleteCancel = () => {
    setDeleteError(null);
    setDeleteNotice(null);
  };

  const onDeleteConfirm = () => {
    setDeleteError(null);
    setDeleteNotice(null);
    if (!deleteId) {
      setDeleteError(DEV_MSG.SITE_NAV_DELETE_ERROR);
      return;
    }
    setDeleteBusy(true);
    void (async () => {
      try {
        await deleteSiteSection(deleteId);
        setDeleteNotice(DEV_MSG.SITE_NAV_DELETED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setDeleteError(panelErrMsg(err, DEV_MSG.SITE_NAV_DELETE_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 409) {
          setDeleteError(panelErrMsg(err, DEV_MSG.SITE_NAV_DELETE_CONFLICT));
          return;
        }
        setDeleteError(panelErrMsg(err, DEV_MSG.SITE_NAV_DELETE_ERROR));
      } finally {
        setDeleteBusy(false);
      }
    })();
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
          <div
            data-testid="developer-site-nav-rename"
            style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "16px" }}
          >
            <label>
              {DEV_MSG.SITE_NAV_RENAME_TARGET}
              <select
                data-testid="developer-site-nav-rename-target"
                value={renameId}
                onChange={(e) => onSelectRename(e.target.value)}
                style={inputStyle}
                disabled={renameBusy || renameTargets.length === 0}
              >
                {renameTargets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {DEV_MSG.SITE_NAV_RENAME_NAME}
              <input
                data-testid="developer-site-nav-rename-name"
                value={renameName}
                onChange={(e) => {
                  setRenameName(e.target.value);
                  setRenameError(null);
                }}
                style={inputStyle}
                disabled={renameBusy || renameTargets.length === 0}
              />
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-rename-save"
                disabled={renameBusy || renameTargets.length === 0}
                onClick={onRename}
              >
                {renameBusy ? DEV_MSG.SITE_NAV_RENAMING : DEV_MSG.SITE_NAV_RENAME}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-rename-cancel"
                disabled={renameBusy}
                onClick={onRenameCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {renameError ? (
              <div data-testid="developer-site-nav-rename-error" role="alert">
                {renameError}
              </div>
            ) : null}
            {renameNotice ? (
              <div data-testid="developer-site-nav-rename-notice">{renameNotice}</div>
            ) : null}
          </div>
          <div
            data-testid="developer-site-nav-delete"
            style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "16px" }}
          >
            <label>
              {DEV_MSG.SITE_NAV_DELETE_TARGET}
              <select
                data-testid="developer-site-nav-delete-target"
                value={deleteId}
                onChange={(e) => onSelectDelete(e.target.value)}
                style={inputStyle}
                disabled={deleteBusy || deleteTargets.length === 0}
              >
                {deleteTargets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.title}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-delete-confirm"
                disabled={deleteBusy || deleteTargets.length === 0}
                onClick={onDeleteConfirm}
              >
                {deleteBusy ? DEV_MSG.SITE_NAV_DELETING : DEV_MSG.SITE_NAV_DELETE_CONFIRM}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-delete-cancel"
                disabled={deleteBusy}
                onClick={onDeleteCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {deleteError ? (
              <div data-testid="developer-site-nav-delete-error" role="alert">
                {deleteError}
              </div>
            ) : null}
            {deleteNotice ? (
              <div data-testid="developer-site-nav-delete-notice">{deleteNotice}</div>
            ) : null}
          </div>
        </>
      )}
    </section>
  );
}
