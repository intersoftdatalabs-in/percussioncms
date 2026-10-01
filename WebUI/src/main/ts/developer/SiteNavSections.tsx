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
import { get, isApiError, isSessionRedirectError, post } from "../api/client";
import { PATHS } from "../api/paths";
import {
  createExternalLinkSection,
  createSiteSection,
  deleteSiteSection,
  loadSection,
  loadSectionProperties,
  loadSectionTree,
  moveSiteSection,
  updateExternalLink,
  updateSiteSection,
} from "../api/architecture/sectionApi";
import type { NavTreeNode } from "../api/architecture/types";
import type { SiteDef } from "../api/developer/types";
import { fetchTemplatesForSectionCreate } from "../api/home/homeApi";
import type { TemplateSummary } from "../api/home/types";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  buildDeveloperAddSectionFields,
  buildDeveloperExternalLinkFields,
  listDeveloperExternalLinks,
  resolveDeveloperExternalLinkDelete,
  buildDeveloperRenameProperties,
  buildDeveloperReparent,
  buildDeveloperSiblingReorder,
  buildSectionLandingTemplateBody,
  isDeveloperNavSectionReadOnly,
  landingTemplateSavePosts,
  listDeveloperTemplateTargets,
  parseSectionLandingTemplate,
  isDeveloperSectionNameTaken,
  listDeveloperDeleteTargets,
  listDeveloperNavParents,
  listDeveloperRenameTargets,
  listDeveloperReparentParents,
  listDeveloperReorderTargets,
  listDeveloperSectionTitles,
  validateDeveloperSectionName,
  type DeveloperNavParentOption,
} from "./siteNavSection";
import {
  findSiblingPlacement,
  validateExternalUrl,
  validateSectionTitle,
} from "../api/architecture/sectionMutations";

function navRows(
  root: NavTreeNode | null,
): { id: string; title: string; parentId: string; external: boolean }[] {
  const rows: { id: string; title: string; parentId: string; external: boolean }[] = [];
  if (!root) {
    return rows;
  }
  const walk = (node: NavTreeNode, parentId: string): void => {
    const title = (node.title || "").trim();
    if (title) {
      rows.push({
        id: node.id,
        title,
        parentId,
        external: String(node.sectionType || "").toLowerCase() === "externallink",
      });
    }
    for (const child of node.children || []) {
      walk(child, node.id);
    }
  };
  walk(root, "");
  return rows;
}

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

/**
 * Add, rename, delete, reorder, or reparent one navigation section on a Developer site.
 * Cancel does not write. Same-parent reorder is one step up or down.
 * Reparent confirm moves one non-root section under a different parent.
 * The site root is not deleted, reordered, or reparented.
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
  const [templates, setTemplates] = useState<TemplateSummary[]>([]);
  const [tplSectionId, setTplSectionId] = useState("");
  const [tplLoaded, setTplLoaded] = useState("");
  const [tplSelected, setTplSelected] = useState("");
  const [tplError, setTplError] = useState<string | null>(null);
  const [tplNotice, setTplNotice] = useState<string | null>(null);
  const [tplBusy, setTplBusy] = useState(false);
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
  const [reorderId, setReorderId] = useState("");
  const [reorderError, setReorderError] = useState<string | null>(null);
  const [reorderNotice, setReorderNotice] = useState<string | null>(null);
  const [reorderBusy, setReorderBusy] = useState(false);
  const [reparentId, setReparentId] = useState("");
  const [reparentParentId, setReparentParentId] = useState("");
  const [reparentError, setReparentError] = useState<string | null>(null);
  const [reparentNotice, setReparentNotice] = useState<string | null>(null);
  const [reparentBusy, setReparentBusy] = useState(false);
  const [extTitle, setExtTitle] = useState("");
  const [extUrl, setExtUrl] = useState("");
  const [extTarget, setExtTarget] = useState("_self");
  const [extError, setExtError] = useState<string | null>(null);
  const [extNotice, setExtNotice] = useState<string | null>(null);
  const [extBusy, setExtBusy] = useState(false);
  const [extUrls, setExtUrls] = useState<Record<string, string>>({});
  const [editExtId, setEditExtId] = useState("");
  const [editUrl, setEditUrl] = useState("");
  const [editTarget, setEditTarget] = useState("_self");
  const [editTitle, setEditTitle] = useState("");
  const [editFolder, setEditFolder] = useState("");
  const [editCss, setEditCss] = useState("");
  const [editSnapshot, setEditSnapshot] = useState({ url: "", target: "_self" });
  const [editExtError, setEditExtError] = useState<string | null>(null);
  const [editExtNotice, setEditExtNotice] = useState<string | null>(null);
  const [editExtBusy, setEditExtBusy] = useState(false);
  const [delExtId, setDelExtId] = useState("");
  const [delExtError, setDelExtError] = useState<string | null>(null);
  const [delExtNotice, setDelExtNotice] = useState<string | null>(null);
  const [delExtBusy, setDelExtBusy] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const renameTargets = listDeveloperRenameTargets(treeRoot);
  const templateTargets = listDeveloperTemplateTargets(treeRoot);
  const deleteTargets = listDeveloperDeleteTargets(treeRoot);
  const reorderTargets = listDeveloperReorderTargets(treeRoot);
  const reparentTargets = listDeveloperReorderTargets(treeRoot);
  const reparentParents = listDeveloperReparentParents(treeRoot, reparentId);
  const externalLinks = listDeveloperExternalLinks(treeRoot);
  const delExtSelected = externalLinks.some((link) => link.id === delExtId)
    ? delExtId
    : (externalLinks[0]?.id ?? "");
  const canMoveUp = buildDeveloperSiblingReorder(treeRoot, reorderId, "up") != null;
  const canMoveDown = buildDeveloperSiblingReorder(treeRoot, reorderId, "down") != null;

  const applyTree = useCallback(
    (root: NavTreeNode | null) => {
      const nextParents = listDeveloperNavParents(root, siteName);
      const nextTargets = listDeveloperRenameTargets(root);
      const nextDeletes = listDeveloperDeleteTargets(root);
      const nextReorders = listDeveloperReorderTargets(root);
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
      setTplSectionId((current) =>
        nextTargets.some((t) => t.id === current) ? current : (nextTargets[0]?.id ?? ""),
      );
      setDeleteId((current) =>
        nextDeletes.some((t) => t.id === current) ? current : (nextDeletes[0]?.id ?? ""),
      );
      setReorderId((current) =>
        nextReorders.some((t) => t.id === current) ? current : (nextReorders[0]?.id ?? ""),
      );
      setReparentId((current) => {
        const next = nextReorders.some((t) => t.id === current)
          ? current
          : (nextReorders[0]?.id ?? "");
        setReparentParentId((parent) => {
          const options = listDeveloperReparentParents(root, next);
          if (options.some((option) => option.id === parent)) {
            return parent;
          }
          const place = findSiblingPlacement(root, next);
          return place?.parent.id ?? options[0]?.id ?? "";
        });
        return next;
      });
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
      setTplSectionId("");
      setTplLoaded("");
      setTplSelected("");
      setTemplates([]);
      setDeleteId("");
      setReorderId("");
      setReparentId("");
      setReparentParentId("");
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
        setTemplates(templates);
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

  useEffect(() => {
    const links = listDeveloperExternalLinks(treeRoot);
    setEditExtId((current) =>
      links.some((link) => link.id === current) ? current : (links[0]?.id ?? ""),
    );
  }, [treeRoot]);

  useEffect(() => {
    if (readOnly || !treeRoot) {
      setExtUrls({});
      return;
    }
    const links = listDeveloperExternalLinks(treeRoot);
    if (links.length === 0) {
      setExtUrls({});
      return;
    }
    let cancelled = false;
    void (async () => {
      const entries = await Promise.all(
        links.map(async (link) => {
          try {
            const loaded = await loadSection(link.id);
            return [link.id, (loaded.externalLinkUrl || "").trim()] as const;
          } catch (err) {
            if (isSessionRedirectError(err)) {
              return [link.id, ""] as const;
            }
            return [link.id, ""] as const;
          }
        }),
      );
      if (cancelled) return;
      const next: Record<string, string> = {};
      for (const [id, url] of entries) {
        next[id] = url;
      }
      setExtUrls(next);
    })();
    return () => {
      cancelled = true;
    };
  }, [treeRoot, readOnly, reloadToken]);

  useEffect(() => {
    if (!editExtId || readOnly) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const loaded = await loadSection(editExtId);
        if (cancelled) return;
        const url = (loaded.externalLinkUrl || "").trim();
        const target = (loaded.target || "_self").trim() || "_self";
        const picked = listDeveloperExternalLinks(treeRoot).find((link) => link.id === editExtId);
        setEditTitle((loaded.title || picked?.title || "").trim());
        setEditFolder((loaded.folderPath || "").trim());
        setEditCss(loaded.cssClassNames ? String(loaded.cssClassNames) : "");
        setEditUrl(url);
        setEditTarget(target);
        setEditSnapshot({ url, target });
        setEditExtError(null);
      } catch (err) {
        if (cancelled || isSessionRedirectError(err)) return;
        setEditExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_EDIT_ERROR));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [editExtId, readOnly, reloadToken, treeRoot]);

  useEffect(() => {
    if (!tplSectionId || readOnly) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const payload = await get<unknown>(
          `${PATHS.SECTION}/landingTemplate/${encodeURIComponent(tplSectionId)}`,
        );
        if (cancelled) return;
        const parsed = parseSectionLandingTemplate(payload);
        const current = parsed?.templateId ?? "";
        setTplLoaded(current);
        setTplSelected(current);
        setTplError(null);
      } catch (err) {
        if (cancelled || isSessionRedirectError(err)) return;
        setTplLoaded("");
        setTplSelected("");
        if (isApiError(err) && err.status === 404) {
          setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_MISSING));
          return;
        }
        if (isApiError(err) && err.status === 403) {
          setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 400) {
          setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_BAD));
          return;
        }
        setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_ERROR));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tplSectionId, readOnly, reloadToken]);

  const onTemplateCancel = () => {
    setTplSelected(tplLoaded);
    setTplError(null);
    setTplNotice(null);
  };

  const onSaveTemplate = () => {
    setTplError(null);
    setTplNotice(null);
    if (!landingTemplateSavePosts(tplLoaded, tplSelected)) {
      return;
    }
    setTplBusy(true);
    void (async () => {
      try {
        const payload = await post<unknown>(
          `${PATHS.SECTION}/landingTemplate`,
          buildSectionLandingTemplateBody(tplSectionId, tplSelected),
        );
        const parsed = parseSectionLandingTemplate(payload);
        const saved = parsed?.templateId || tplSelected.trim();
        setTplLoaded(saved);
        setTplSelected(saved);
        setTplNotice(DEV_MSG.SITE_NAV_TEMPLATE_SAVED);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 404) {
          setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_MISSING));
          return;
        }
        if (isApiError(err) && err.status === 400) {
          setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_BAD));
          return;
        }
        setTplError(panelErrMsg(err, DEV_MSG.SITE_NAV_TEMPLATE_ERROR));
      } finally {
        setTplBusy(false);
      }
    })();
  };

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

  const onDeleteExtCancel = () => {
    setDelExtError(null);
    setDelExtNotice(null);
  };

  const onDeleteExtConfirm = () => {
    setDelExtError(null);
    setDelExtNotice(null);
    const id = resolveDeveloperExternalLinkDelete(treeRoot, delExtSelected);
    if (!id) {
      setDelExtError(DEV_MSG.SITE_NAV_EXT_DELETE_REFUSED);
      return;
    }
    setDelExtBusy(true);
    void (async () => {
      try {
        await deleteSiteSection(id);
        setDelExtNotice(DEV_MSG.SITE_NAV_EXT_DELETED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setDelExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_DELETE_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 409) {
          setDelExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_DELETE_CONFLICT));
          return;
        }
        setDelExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_DELETE_ERROR));
      } finally {
        setDelExtBusy(false);
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

  const onSelectReorder = (id: string) => {
    setReorderId(id);
    setReorderError(null);
    setReorderNotice(null);
  };

  const onReorderCancel = () => {
    setReorderError(null);
    setReorderNotice(null);
  };

  const onSelectReparent = (id: string) => {
    setReparentId(id);
    const place = findSiblingPlacement(treeRoot, id);
    setReparentParentId(place?.parent.id ?? "");
    setReparentError(null);
    setReparentNotice(null);
  };

  const onReparentCancel = () => {
    setReparentError(null);
    setReparentNotice(null);
  };

  const onReparent = () => {
    setReparentError(null);
    setReparentNotice(null);
    const fields = buildDeveloperReparent(treeRoot, reparentId, reparentParentId);
    if (!fields) {
      return;
    }
    setReparentBusy(true);
    void (async () => {
      try {
        await moveSiteSection(fields);
        setReparentNotice(DEV_MSG.SITE_NAV_REPARENTED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setReparentError(panelErrMsg(err, DEV_MSG.SITE_NAV_REPARENT_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 409) {
          setReparentError(panelErrMsg(err, DEV_MSG.SITE_NAV_REPARENT_CONFLICT));
          return;
        }
        if (isApiError(err) && err.status === 404) {
          setReparentError(panelErrMsg(err, DEV_MSG.SITE_NAV_REPARENT_MISSING));
          return;
        }
        setReparentError(panelErrMsg(err, DEV_MSG.SITE_NAV_REPARENT_ERROR));
      } finally {
        setReparentBusy(false);
      }
    })();
  };

  const onReorder = (direction: "up" | "down") => {
    setReorderError(null);
    setReorderNotice(null);
    const fields = buildDeveloperSiblingReorder(treeRoot, reorderId, direction);
    if (!fields) {
      return;
    }
    setReorderBusy(true);
    void (async () => {
      try {
        await moveSiteSection(fields);
        setReorderNotice(DEV_MSG.SITE_NAV_MOVED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 403) {
          setReorderError(panelErrMsg(err, DEV_MSG.SITE_NAV_REORDER_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 409) {
          setReorderError(panelErrMsg(err, DEV_MSG.SITE_NAV_REORDER_CONFLICT));
          return;
        }
        setReorderError(panelErrMsg(err, DEV_MSG.SITE_NAV_REORDER_ERROR));
      } finally {
        setReorderBusy(false);
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

  const onExtCancel = () => {
    setExtTitle("");
    setExtUrl("");
    setExtTarget("_self");
    setExtError(null);
    setExtNotice(null);
  };

  const onAddExternal = () => {
    setExtError(null);
    setExtNotice(null);
    if (validateSectionTitle(extTitle) || validateExternalUrl(extUrl)) {
      setExtError(DEV_MSG.SITE_NAV_EXT_INVALID);
      return;
    }
    const parent = parents.find((p) => p.id === parentId) ?? parents[0];
    setExtBusy(true);
    void (async () => {
      try {
        let loadedFolderPath: string | null = null;
        if (parent?.node && !parent.node.folderPath?.trim() && parent.node.id) {
          const loaded = await loadSection(parent.node.id);
          loadedFolderPath = loaded.folderPath ?? null;
        }
        await createExternalLinkSection(
          buildDeveloperExternalLinkFields({
            title: extTitle,
            url: extUrl,
            target: extTarget,
            siteName,
            parent: parent?.node ?? null,
            loadedFolderPath,
          }),
        );
        setExtTitle("");
        setExtUrl("");
        setExtTarget("_self");
        setExtNotice(DEV_MSG.SITE_NAV_EXT_ADDED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 400) {
          setExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_BAD_REQUEST));
          return;
        }
        if (isApiError(err) && err.status === 403) {
          setExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 409) {
          setExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_CONFLICT));
          return;
        }
        setExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_ERROR));
      } finally {
        setExtBusy(false);
      }
    })();
  };

  const onEditExtCancel = () => {
    setEditUrl(editSnapshot.url);
    setEditTarget(editSnapshot.target);
    setEditExtError(null);
    setEditExtNotice(null);
  };

  const onSaveExternal = () => {
    setEditExtError(null);
    setEditExtNotice(null);
    if (!editExtId || validateExternalUrl(editUrl) || !/^https?:\/\//i.test(editUrl.trim())) {
      setEditExtError(DEV_MSG.SITE_NAV_EXT_EDIT_INVALID);
      return;
    }
    const link = externalLinks.find((item) => item.id === editExtId);
    const parent = findSiblingPlacement(treeRoot, editExtId)?.parent ?? treeRoot;
    setEditExtBusy(true);
    void (async () => {
      try {
        const fields = buildDeveloperExternalLinkFields({
          title: editTitle || link?.title || "",
          url: editUrl,
          target: editTarget,
          siteName,
          parent,
          loadedFolderPath: editFolder || null,
        });
        await updateExternalLink(editExtId, {
          ...fields,
          ...(editCss.trim() ? { cssClassNames: editCss.trim() } : {}),
        });
        setEditExtNotice(DEV_MSG.SITE_NAV_EXT_EDIT_SAVED);
        setReloadToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        if (isApiError(err) && err.status === 400) {
          setEditExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_EDIT_BAD_REQUEST));
          return;
        }
        if (isApiError(err) && err.status === 403) {
          setEditExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_EDIT_FORBIDDEN));
          return;
        }
        if (isApiError(err) && err.status === 409) {
          setEditExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_EDIT_CONFLICT));
          return;
        }
        setEditExtError(panelErrMsg(err, DEV_MSG.SITE_NAV_EXT_EDIT_ERROR));
      } finally {
        setEditExtBusy(false);
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
                {navRows(treeRoot).map((row) => (
                  <li
                    key={row.id}
                    data-testid="developer-site-nav-item"
                    data-section-id={row.id}
                    data-parent-id={row.parentId}
                    data-external-url={row.external ? extUrls[row.id] || "" : undefined}
                  >
                    {row.title}
                    {row.external && extUrls[row.id] ? (
                      <span data-testid="developer-site-nav-item-url"> {extUrls[row.id]}</span>
                    ) : null}
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
            data-testid="developer-site-nav-external"
            style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "16px" }}
          >
            <div style={{ fontSize: "0.95rem", fontWeight: 600 }}>{DEV_MSG.SITE_NAV_EXT_TITLE}</div>
            <label>
              {DEV_MSG.SITE_NAV_EXT_NAME}
              <input
                data-testid="developer-site-nav-ext-title"
                value={extTitle}
                onChange={(e) => {
                  setExtTitle(e.target.value);
                  setExtError(null);
                }}
                style={inputStyle}
                disabled={extBusy}
              />
            </label>
            <label>
              {DEV_MSG.SITE_NAV_EXT_URL}
              <input
                data-testid="developer-site-nav-ext-url"
                value={extUrl}
                onChange={(e) => {
                  setExtUrl(e.target.value);
                  setExtError(null);
                }}
                style={inputStyle}
                disabled={extBusy}
              />
            </label>
            <label>
              {DEV_MSG.SITE_NAV_EXT_TARGET}
              <select
                data-testid="developer-site-nav-ext-target"
                value={extTarget}
                onChange={(e) => setExtTarget(e.target.value)}
                style={inputStyle}
                disabled={extBusy}
              >
                <option value="_self">{DEV_MSG.SITE_NAV_EXT_TARGET_SELF}</option>
                <option value="_blank">{DEV_MSG.SITE_NAV_EXT_TARGET_BLANK}</option>
                <option value="_top">{DEV_MSG.SITE_NAV_EXT_TARGET_TOP}</option>
                <option value="_parent">{DEV_MSG.SITE_NAV_EXT_TARGET_PARENT}</option>
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-ext-add"
                disabled={extBusy}
                onClick={onAddExternal}
              >
                {extBusy ? DEV_MSG.SITE_NAV_EXT_ADDING : DEV_MSG.SITE_NAV_EXT_ADD}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-ext-cancel"
                disabled={extBusy}
                onClick={onExtCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {extError ? (
              <div data-testid="developer-site-nav-ext-error" role="alert">
                {extError}
              </div>
            ) : null}
            {extNotice ? (
              <div data-testid="developer-site-nav-ext-notice">{extNotice}</div>
            ) : null}
            <div style={{ fontSize: "0.95rem", fontWeight: 600, marginTop: "8px" }}>
              {DEV_MSG.SITE_NAV_EXT_EDIT_HEADING}
            </div>
            <label>
              {DEV_MSG.SITE_NAV_EXT_EDIT_PICK}
              <select
                data-testid="developer-site-nav-ext-edit-target"
                value={editExtId}
                onChange={(e) => {
                  setEditExtId(e.target.value);
                  setEditExtError(null);
                  setEditExtNotice(null);
                }}
                style={inputStyle}
                disabled={editExtBusy || externalLinks.length === 0}
              >
                {externalLinks.map((link) => (
                  <option key={link.id} value={link.id}>
                    {link.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {DEV_MSG.SITE_NAV_EXT_URL}
              <input
                data-testid="developer-site-nav-ext-edit-url"
                value={editUrl}
                onChange={(e) => {
                  setEditUrl(e.target.value);
                  setEditExtError(null);
                }}
                style={inputStyle}
                disabled={editExtBusy || externalLinks.length === 0}
              />
            </label>
            <label>
              {DEV_MSG.SITE_NAV_EXT_TARGET}
              <select
                data-testid="developer-site-nav-ext-edit-window"
                value={editTarget}
                onChange={(e) => setEditTarget(e.target.value)}
                style={inputStyle}
                disabled={editExtBusy || externalLinks.length === 0}
              >
                <option value="_self">{DEV_MSG.SITE_NAV_EXT_TARGET_SELF}</option>
                <option value="_blank">{DEV_MSG.SITE_NAV_EXT_TARGET_BLANK}</option>
                <option value="_top">{DEV_MSG.SITE_NAV_EXT_TARGET_TOP}</option>
                <option value="_parent">{DEV_MSG.SITE_NAV_EXT_TARGET_PARENT}</option>
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-ext-edit-save"
                disabled={editExtBusy || externalLinks.length === 0}
                onClick={onSaveExternal}
              >
                {editExtBusy ? DEV_MSG.SITE_NAV_EXT_EDIT_SAVING : DEV_MSG.SITE_NAV_EXT_EDIT_SAVE}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-ext-edit-cancel"
                disabled={editExtBusy || externalLinks.length === 0}
                onClick={onEditExtCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {editExtError ? (
              <div data-testid="developer-site-nav-ext-edit-error" role="alert">
                {editExtError}
              </div>
            ) : null}
            {editExtNotice ? (
              <div data-testid="developer-site-nav-ext-edit-notice">{editExtNotice}</div>
            ) : null}
            <div style={{ fontSize: "0.95rem", fontWeight: 600, marginTop: "8px" }}>
              {DEV_MSG.SITE_NAV_EXT_DELETE_HEADING}
            </div>
            <label>
              {DEV_MSG.SITE_NAV_EXT_DELETE_PICK}
              <select
                data-testid="developer-site-nav-ext-delete-target"
                value={delExtSelected}
                onChange={(e) => {
                  setDelExtId(e.target.value);
                  setDelExtError(null);
                  setDelExtNotice(null);
                }}
                style={inputStyle}
                disabled={delExtBusy || externalLinks.length === 0}
              >
                {externalLinks.map((link) => (
                  <option key={link.id} value={link.id}>
                    {link.title}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-ext-delete-confirm"
                disabled={delExtBusy || externalLinks.length === 0}
                onClick={onDeleteExtConfirm}
              >
                {delExtBusy ? DEV_MSG.SITE_NAV_EXT_DELETING : DEV_MSG.SITE_NAV_EXT_DELETE_CONFIRM}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-ext-delete-cancel"
                disabled={delExtBusy || externalLinks.length === 0}
                onClick={onDeleteExtCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {delExtError ? (
              <div data-testid="developer-site-nav-ext-delete-error" role="alert">
                {delExtError}
              </div>
            ) : null}
            {delExtNotice ? (
              <div data-testid="developer-site-nav-ext-delete-notice">{delExtNotice}</div>
            ) : null}
          </div>
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
            data-testid="developer-site-nav-template"
            style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "16px" }}
          >
            <label>
              {DEV_MSG.SITE_NAV_TEMPLATE_TARGET}
              <select
                data-testid="developer-site-nav-template-target"
                value={tplSectionId}
                onChange={(e) => {
                  setTplSectionId(e.target.value);
                  setTplError(null);
                  setTplNotice(null);
                }}
                style={inputStyle}
                disabled={tplBusy || templateTargets.length === 0}
              >
                {templateTargets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {DEV_MSG.SITE_NAV_TEMPLATE}
              <select
                data-testid="developer-site-nav-template-id"
                data-loaded-template={tplLoaded}
                value={tplSelected}
                onChange={(e) => {
                  setTplSelected(e.target.value);
                  setTplError(null);
                }}
                style={inputStyle}
                disabled={tplBusy || templateTargets.length === 0}
              >
                {!tplSelected ? <option value="">—</option> : null}
                {tplSelected && !templates.some((t) => t.id === tplSelected) ? (
                  <option value={tplSelected}>{tplSelected}</option>
                ) : null}
                {templates.map((template) => (
                  <option key={template.id} value={template.id}>
                    {template.name || template.id}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-template-save"
                disabled={tplBusy || templateTargets.length === 0}
                onClick={onSaveTemplate}
              >
                {tplBusy ? DEV_MSG.SITE_NAV_TEMPLATE_SAVING : DEV_MSG.SITE_NAV_TEMPLATE_SAVE}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-template-cancel"
                disabled={tplBusy}
                onClick={onTemplateCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {tplError ? (
              <div data-testid="developer-site-nav-template-error" role="alert">
                {tplError}
              </div>
            ) : null}
            {tplNotice ? (
              <div data-testid="developer-site-nav-template-notice">{tplNotice}</div>
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
          <div
            data-testid="developer-site-nav-reorder"
            style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "16px" }}
          >
            <label>
              {DEV_MSG.SITE_NAV_REORDER_TARGET}
              <select
                data-testid="developer-site-nav-reorder-target"
                value={reorderId}
                onChange={(e) => onSelectReorder(e.target.value)}
                style={inputStyle}
                disabled={reorderBusy || reorderTargets.length === 0}
              >
                {reorderTargets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.title}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-move-up"
                disabled={reorderBusy || !canMoveUp}
                onClick={() => onReorder("up")}
              >
                {reorderBusy ? DEV_MSG.SITE_NAV_MOVING : DEV_MSG.SITE_NAV_MOVE_UP}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-move-down"
                disabled={reorderBusy || !canMoveDown}
                onClick={() => onReorder("down")}
              >
                {reorderBusy ? DEV_MSG.SITE_NAV_MOVING : DEV_MSG.SITE_NAV_MOVE_DOWN}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-reorder-cancel"
                disabled={reorderBusy}
                onClick={onReorderCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {reorderError ? (
              <div data-testid="developer-site-nav-reorder-error" role="alert">
                {reorderError}
              </div>
            ) : null}
            {reorderNotice ? (
              <div data-testid="developer-site-nav-reorder-notice">{reorderNotice}</div>
            ) : null}
          </div>
          <div
            data-testid="developer-site-nav-reparent"
            style={{ display: "grid", gap: "8px", maxWidth: "420px", marginTop: "16px" }}
          >
            <label>
              {DEV_MSG.SITE_NAV_REPARENT_TARGET}
              <select
                data-testid="developer-site-nav-reparent-target"
                value={reparentId}
                onChange={(e) => onSelectReparent(e.target.value)}
                style={inputStyle}
                disabled={reparentBusy || reparentTargets.length === 0}
              >
                {reparentTargets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {DEV_MSG.SITE_NAV_REPARENT_PARENT}
              <select
                data-testid="developer-site-nav-reparent-parent"
                value={reparentParentId}
                onChange={(e) => {
                  setReparentParentId(e.target.value);
                  setReparentError(null);
                  setReparentNotice(null);
                }}
                style={inputStyle}
                disabled={reparentBusy || reparentParents.length === 0}
              >
                {reparentParents.map((parent) => (
                  <option key={parent.id} value={parent.id}>
                    {parent.title}
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-site-nav-reparent-confirm"
                disabled={reparentBusy || reparentTargets.length === 0}
                onClick={onReparent}
              >
                {reparentBusy ? DEV_MSG.SITE_NAV_REPARENTING : DEV_MSG.SITE_NAV_REPARENT}
              </button>
              <button
                type="button"
                data-testid="developer-site-nav-reparent-cancel"
                disabled={reparentBusy}
                onClick={onReparentCancel}
              >
                {DEV_MSG.SITE_NAV_CANCEL}
              </button>
            </div>
            {reparentError ? (
              <div data-testid="developer-site-nav-reparent-error" role="alert">
                {reparentError}
              </div>
            ) : null}
            {reparentNotice ? (
              <div data-testid="developer-site-nav-reparent-notice">{reparentNotice}</div>
            ) : null}
          </div>
        </>
      )}
    </section>
  );
}
