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

import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { formatApiError, isSessionRedirectError } from "../api/client";
import {
  convertSectionToFolder,
  createExternalLinkSection,
  createSectionFromFolder,
  createSectionLink,
  createSiteSection,
  deleteSectionLink,
  deleteSiteSection,
  loadSection,
  loadSectionProperties,
  loadSectionTree,
  moveSiteSection,
  replaceLandingPage,
  updateExternalLink,
  updateSectionLink,
  updateSiteSection,
} from "../api/architecture/sectionApi";
import {
  applyTitleToProperties,
  buildSiblingReorderMove,
  canConvertSectionToFolder,
  canCreateChildUnder,
  canDeleteNavNode,
  canEditLinkNode,
  canMoveNavNodeDown,
  canMoveNavNodeUp,
  canReplaceLandingPage,
  findNavNodeById,
  findSiblingPlacement,
  isExternalLinkType,
  isSectionLinkType,
  resolveCreateParentFolderPath,
} from "../api/architecture/sectionMutations";
import type { NavTreeNode } from "../api/architecture/types";
import {
  copyManagedSite,
  deleteManagedSite,
  isSiteBeingImported,
  isSiteCopyInProgress,
  loadSiteCopyInfo,
  suggestCopySiteName,
} from "../api/architecture/siteAdminApi";
import type { PSSiteCopyRequest } from "../api/contentExplorer/types";
import { fetchSites } from "../api/home/homeApi";
import { SiteCopyWizard } from "../contentExplorer/wizards/SiteCopyWizard";
import { SiteCreateWizard } from "../contentExplorer/wizards/SiteCreateWizard";
import { catalogColors } from "../developer/catalogStyles";
import { CreateSectionDialog } from "./CreateSectionDialog";
import { CreateSectionFromFolderDialog } from "./CreateSectionFromFolderDialog";
import { ExternalLinkDialog } from "./ExternalLinkDialog";
import { NavTree } from "./NavTree";
import { RenameSectionDialog } from "./RenameSectionDialog";
import { ReplaceLandingPageDialog } from "./ReplaceLandingPageDialog";
import { SectionLinkDialog } from "./SectionLinkDialog";
import { SitePicker } from "./SitePicker";
import { StructureActionBar } from "./StructureActionBar";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface ArchitectureShellProps {
  /**
   * Optional site name from SPA path {@code /architecture/:site} or deep-link
   * query. When present and found in the site list, it becomes the selection.
   */
  initialSite?: string | null;
  /**
   * When true (SPA AppLayout), shell is under product chrome — tighter padding.
   */
  embedded?: boolean;
  /**
   * Optional confirm hook (tests). Defaults to {@code window.confirm}.
   */
  confirmFn?: (message: string) => boolean;
  /**
   * When false, landing dialog uses page-id field instead of ContentBrowser
   * (unit tests). Default true.
   */
  useLandingContentBrowser?: boolean;
  /**
   * Show New / Copy / Delete Site for entitled roles.
   * Default true — Architecture is already Admin/Designer gated (#3219 / #3303).
   */
  allowNewSite?: boolean;
}

type SitesLoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; names: string[] };

type TreeLoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; root: NavTreeNode | null };

/**
 * Architecture / Navigation SPA shell
 * (#3094 shell + #3095 tree + #3096 mutations + #3097 landing/links).
 */
export const ArchitectureShell: React.FC<ArchitectureShellProps> = ({
  initialSite = null,
  embedded = false,
  confirmFn,
  useLandingContentBrowser = true,
  allowNewSite = true,
}) => {
  const confirmAction = useCallback(
    (msg: string) => (confirmFn ? confirmFn(msg) : window.confirm(msg)),
    [confirmFn],
  );

  const [sitesState, setSitesState] = useState<SitesLoadState>({
    status: "loading",
  });
  const [selectedSite, setSelectedSite] = useState<string | null>(() => {
    const t = initialSite != null ? String(initialSite).trim() : "";
    return t.length > 0 ? t : null;
  });
  const [treeState, setTreeState] = useState<TreeLoadState>({ status: "idle" });
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [mutationBusy, setMutationBusy] = useState(false);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createFromFolderOpen, setCreateFromFolderOpen] = useState(false);
  const [renameOpen, setRenameOpen] = useState(false);
  const [landingOpen, setLandingOpen] = useState(false);
  const [sectionLinkOpen, setSectionLinkOpen] = useState(false);
  const [sectionLinkMode, setSectionLinkMode] = useState<"create" | "edit">(
    "create",
  );
  const [externalLinkOpen, setExternalLinkOpen] = useState(false);
  const [externalLinkMode, setExternalLinkMode] = useState<"create" | "edit">(
    "create",
  );
  const [externalInitial, setExternalInitial] = useState<{
    linkTitle: string;
    externalUrl: string;
    target: string;
  } | null>(null);
  const [showNewSite, setShowNewSite] = useState(false);
  const [showCopySite, setShowCopySite] = useState(false);
  const newSiteToggleRef = useRef<HTMLButtonElement>(null);
  const newSitePanelRef = useRef<HTMLElement>(null);
  const copySiteToggleRef = useRef<HTMLButtonElement>(null);
  const copySitePanelRef = useRef<HTMLElement>(null);
  const copyTargetRef = useRef<string | null>(null);

  useDialogEscape(showNewSite, mutationBusy, () => setShowNewSite(false));
  useDialogEscape(showCopySite, mutationBusy, () => setShowCopySite(false));

  useEffect(() => {
    if (!showNewSite) {
      return;
    }
    const root = newSitePanelRef.current;
    if (!root) {
      return;
    }
    const focusables = (): HTMLElement[] =>
      Array.from(
        root.querySelectorAll<HTMLElement>(
          'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );
    focusables()[0]?.focus();
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Tab") {
        return;
      }
      const list = focusables();
      if (list.length === 0) {
        return;
      }
      const first = list[0];
      const last = list[list.length - 1];
      if (ev.shiftKey && document.activeElement === first) {
        ev.preventDefault();
        last.focus();
      } else if (!ev.shiftKey && document.activeElement === last) {
        ev.preventDefault();
        first.focus();
      }
    };
    root.addEventListener("keydown", onKey);
    return () => {
      root.removeEventListener("keydown", onKey);
      newSiteToggleRef.current?.focus();
    };
  }, [showNewSite]);

  useEffect(() => {
    if (!showCopySite) {
      return;
    }
    const root = copySitePanelRef.current;
    if (!root) {
      return;
    }
    const focusables = (): HTMLElement[] =>
      Array.from(
        root.querySelectorAll<HTMLElement>(
          'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );
    focusables()[0]?.focus();
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Tab") {
        return;
      }
      const list = focusables();
      if (list.length === 0) {
        return;
      }
      const first = list[0];
      const last = list[list.length - 1];
      if (ev.shiftKey && document.activeElement === first) {
        ev.preventDefault();
        last.focus();
      } else if (!ev.shiftKey && document.activeElement === last) {
        ev.preventDefault();
        first.focus();
      }
    };
    root.addEventListener("keydown", onKey);
    return () => {
      root.removeEventListener("keydown", onKey);
      copySiteToggleRef.current?.focus();
    };
  }, [showCopySite]);

  // Honor route/deep-link site when prop changes
  useEffect(() => {
    const t = initialSite != null ? String(initialSite).trim() : "";
    if (t.length > 0) {
      setSelectedSite(t);
    }
  }, [initialSite]);

  const applySiteNames = useCallback(
    (names: string[], preferSite?: string | null) => {
      setSitesState({ status: "ready", names });
      const preferred = preferSite != null ? String(preferSite).trim() : "";
      setSelectedSite((prev) => {
        if (preferred && names.includes(preferred)) return preferred;
        if (prev && names.includes(prev)) return prev;
        if (prev && names.length === 0) return prev;
        if (prev && !names.includes(prev) && names.length > 0) {
          return prev;
        }
        if (!prev && names.length > 0) return names[0];
        return prev;
      });
    },
    [],
  );

  const loadSiteNames = useCallback(async (): Promise<string[]> => {
    const list = await fetchSites();
    return list
      .map((s) => (s.name != null ? String(s.name).trim() : ""))
      .filter((n) => n.length > 0)
      .sort((a, b) => a.localeCompare(b));
  }, []);

  // Load site list once
  useEffect(() => {
    let cancelled = false;
    setSitesState({ status: "loading" });
    void (async () => {
      try {
        const names = await loadSiteNames();
        if (cancelled) return;
        applySiteNames(names);
      } catch (err) {
        if (cancelled) return;
        if (isSessionRedirectError(err)) return;
        setSitesState({
          status: "error",
          message: formatApiError(err, ARCH_MSG.SITES_ERROR),
        });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [applySiteNames, loadSiteNames]);

  // Load tree when site or refresh changes
  useEffect(() => {
    if (!selectedSite) {
      setTreeState({ status: "idle" });
      setSelectedNodeId(null);
      return;
    }
    let cancelled = false;
    setTreeState({ status: "loading" });
    setSelectedNodeId(null);
    setMutationError(null);
    void (async () => {
      try {
        const root = await loadSectionTree(selectedSite);
        if (cancelled) return;
        setTreeState({ status: "ready", root });
      } catch (err) {
        if (cancelled) return;
        if (isSessionRedirectError(err)) return;
        setTreeState({
          status: "error",
          message: formatApiError(err, ARCH_MSG.TREE_ERROR),
        });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [selectedSite, refreshToken]);

  const onRefresh = useCallback(() => {
    setRefreshToken((n) => n + 1);
  }, []);

  const reloadSites = useCallback(
    async (
      preferSite?: string | null,
      reloadErrorTemplate: string = ARCH_MSG.NEW_SITE_RELOAD_ERROR,
    ) => {
      const preferred = preferSite != null ? String(preferSite).trim() : "";
      try {
        const names = await loadSiteNames();
        applySiteNames(names, preferred);
        if (preferred) {
          setMutationError(null);
        }
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        setSitesState({
          status: "error",
          message: formatApiError(err, ARCH_MSG.SITES_ERROR),
        });
        if (preferred) {
          setMutationError(reloadErrorTemplate.split("{0}").join(preferred));
        }
      }
    },
    [applySiteNames, loadSiteNames],
  );

  const reloadSitesAfterDelete = useCallback(
    async (deletedName: string) => {
      const gone = deletedName.trim();
      try {
        const names = await loadSiteNames();
        const remaining = names.filter((n) => n !== gone);
        setSitesState({ status: "ready", names: remaining });
        setSelectedSite((prev) => {
          if (prev && remaining.includes(prev)) {
            return prev;
          }
          return remaining[0] ?? null;
        });
        setMutationError(null);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        setSitesState({
          status: "error",
          message: formatApiError(err, ARCH_MSG.SITES_ERROR),
        });
        setSelectedSite((prev) => (prev === gone ? null : prev));
        setMutationError(
          ARCH_MSG.DELETE_SITE_RELOAD_ERROR.split("{0}").join(gone),
        );
      }
    },
    [loadSiteNames],
  );

  const treeRoot =
    treeState.status === "ready" ? treeState.root : null;
  const selectedNode = useMemo(
    () =>
      selectedNodeId
        ? findNavNodeById(treeRoot, selectedNodeId)
        : null,
    [treeRoot, selectedNodeId],
  );

  const createParent = useMemo(() => {
    if (selectedNode && canCreateChildUnder(selectedNode)) {
      return selectedNode;
    }
    if (treeRoot && canCreateChildUnder(treeRoot)) {
      return treeRoot;
    }
    return null;
  }, [selectedNode, treeRoot]);

  const canCreate = !!selectedSite && !!createParent && !mutationBusy;
  const canCreateFromFolder = canCreate;
  const canConvertToFolder =
    canConvertSectionToFolder(treeRoot, selectedNode) && !mutationBusy;
  const canCreateSectionLink = canCreate;
  const canCreateExternalLink = canCreate;
  const canLanding =
    !!selectedNode && canReplaceLandingPage(selectedNode) && !mutationBusy;
  const canEditLink =
    !!selectedNode && canEditLinkNode(selectedNode) && !mutationBusy;
  const canRename =
    !!selectedNode &&
    String(selectedNode.sectionType || "section").toLowerCase() ===
      "section" &&
    !mutationBusy;
  const canMoveUp =
    !!selectedNodeId &&
    canMoveNavNodeUp(treeRoot, selectedNodeId) &&
    !mutationBusy;
  const canMoveDown =
    !!selectedNodeId &&
    canMoveNavNodeDown(treeRoot, selectedNodeId) &&
    !mutationBusy;
  const canDelete =
    canDeleteNavNode(treeRoot, selectedNode) && !mutationBusy;

  const runMutation = useCallback(
    async (work: () => Promise<void>) => {
      setMutationBusy(true);
      setMutationError(null);
      try {
        await work();
        setRefreshToken((n) => n + 1);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        setMutationError(formatApiError(err, ARCH_MSG.MUTATION_ERROR));
      } finally {
        setMutationBusy(false);
      }
    },
    [],
  );

  const onCreateSubmit = useCallback(
    (input: { title: string; urlName: string; templateId: string }) => {
      if (!selectedSite || !createParent) return;
      const folderPath = resolveCreateParentFolderPath(
        createParent,
        selectedSite,
      );
      void runMutation(async () => {
        await createSiteSection({
          pageTitle: input.title,
          pageLinkTitle: input.title,
          pageName: input.urlName,
          pageUrlIdentifier: input.urlName,
          templateId: input.templateId,
          folderPath,
          sectionType: "section",
          copyTemplates: true,
          target: "_self",
        });
        setCreateOpen(false);
      });
    },
    [selectedSite, createParent, runMutation],
  );

  const onRenameSubmit = useCallback(
    (title: string) => {
      if (!selectedNode) return;
      void runMutation(async () => {
        const props = await loadSectionProperties(selectedNode.id);
        const next = applyTitleToProperties(props, title);
        await updateSiteSection(next);
        setRenameOpen(false);
      });
    },
    [selectedNode, runMutation],
  );

  const onMove = useCallback(
    (direction: "up" | "down") => {
      if (!selectedNodeId || !treeRoot) return;
      const fields = buildSiblingReorderMove(
        treeRoot,
        selectedNodeId,
        direction,
      );
      if (!fields) return;
      void runMutation(async () => {
        await moveSiteSection(fields);
      });
    },
    [selectedNodeId, treeRoot, runMutation],
  );

  const onConvertToFolder = useCallback(() => {
    if (!selectedNode || !treeRoot) return;
    if (!canConvertSectionToFolder(treeRoot, selectedNode)) {
      setMutationError(
        selectedNode.id === treeRoot.id
          ? ARCH_MSG.CONVERT_ROOT_BLOCKED
          : ARCH_MSG.CONVERT_BLOCKED,
      );
      return;
    }
    const msg = ARCH_MSG.CONVERT_CONFIRM.split("{0}").join(selectedNode.title);
    if (!confirmAction(msg)) {
      return;
    }
    const nodeId = selectedNode.id;
    void runMutation(async () => {
      await convertSectionToFolder(nodeId);
      setSelectedNodeId(null);
    });
  }, [selectedNode, treeRoot, confirmAction, runMutation]);

  const onCreateFromFolderSubmit = useCallback(
    (input: { sourceFolderPath: string; pageName: string }) => {
      if (!selectedSite || !createParent) return;
      const parentFolderPath = resolveCreateParentFolderPath(
        createParent,
        selectedSite,
      );
      void runMutation(async () => {
        await createSectionFromFolder({
          sourceFolderPath: input.sourceFolderPath,
          pageName: input.pageName,
          parentFolderPath,
        });
        setCreateFromFolderOpen(false);
      });
    },
    [selectedSite, createParent, runMutation],
  );

  const onDelete = useCallback(() => {
    if (!selectedNode || !treeRoot) return;
    if (!canDeleteNavNode(treeRoot, selectedNode)) {
      setMutationError(ARCH_MSG.DELETE_ROOT_BLOCKED);
      return;
    }
    // Avoid String#replace $`/$& injection from user-controlled titles.
    const msg = ARCH_MSG.DELETE_CONFIRM.split("{0}").join(selectedNode.title);
    if (!confirmAction(msg)) {
      return;
    }
    const nodeId = selectedNode.id;
    const sectionType = selectedNode.sectionType;
    void runMutation(async () => {
      if (isSectionLinkType(sectionType)) {
        const place = findSiblingPlacement(treeRoot, nodeId);
        if (!place) {
          throw new Error("Could not resolve parent for section link delete");
        }
        await deleteSectionLink(nodeId, place.parent.id);
      } else {
        await deleteSiteSection(nodeId);
      }
      setSelectedNodeId(null);
    });
  }, [selectedNode, treeRoot, confirmAction, runMutation]);

  const onCopySite = useCallback(() => {
    if (!selectedSite) {
      setMutationError(ARCH_MSG.COPY_SITE_NEED_SELECTION);
      return;
    }
    if (mutationBusy) {
      return;
    }
    setMutationBusy(true);
    setMutationError(null);
    void (async () => {
      try {
        const info = await loadSiteCopyInfo();
        if (isSiteCopyInProgress(info)) {
          setMutationError(ARCH_MSG.COPY_SITE_IN_PROGRESS);
          return;
        }
        setShowNewSite(false);
        setShowCopySite(true);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        setMutationError(formatApiError(err, ARCH_MSG.COPY_SITE_ERROR));
      } finally {
        setMutationBusy(false);
      }
    })();
  }, [selectedSite, mutationBusy]);

  const submitCopySite = useCallback(
    async (req: PSSiteCopyRequest) => {
      const dest = String(req.targetSite ?? "").trim();
      copyTargetRef.current = dest;
      await copyManagedSite({
        sourceSite: req.sourceSite ?? selectedSite ?? "",
        targetSite: dest,
        targetFolder: req.targetFolder,
      });
    },
    [selectedSite],
  );

  const onCopySiteSettled = useCallback(
    (ok: boolean) => {
      if (!ok) {
        return;
      }
      setShowCopySite(false);
      const dest = copyTargetRef.current || selectedSite;
      void reloadSites(dest, ARCH_MSG.COPY_SITE_RELOAD_ERROR);
    },
    [reloadSites, selectedSite],
  );

  const onDeleteSite = useCallback(() => {
    if (!selectedSite) {
      setMutationError(ARCH_MSG.DELETE_SITE_NEED_SELECTION);
      return;
    }
    if (mutationBusy) {
      return;
    }
    const name = selectedSite;
    const msg = ARCH_MSG.DELETE_SITE_CONFIRM.split("{0}").join(name);
    if (!confirmAction(msg)) {
      return;
    }
    setMutationBusy(true);
    setMutationError(null);
    void (async () => {
      try {
        const info = await loadSiteCopyInfo();
        if (isSiteCopyInProgress(info)) {
          setMutationError(ARCH_MSG.COPY_SITE_IN_PROGRESS);
          return;
        }
        if (await isSiteBeingImported(name)) {
          setMutationError(ARCH_MSG.DELETE_SITE_IMPORTING);
          return;
        }
        await deleteManagedSite(name);
        setShowCopySite(false);
        setShowNewSite(false);
        await reloadSitesAfterDelete(name);
      } catch (err) {
        if (isSessionRedirectError(err)) return;
        setMutationError(formatApiError(err, ARCH_MSG.DELETE_SITE_ERROR));
      } finally {
        setMutationBusy(false);
      }
    })();
  }, [selectedSite, mutationBusy, confirmAction, reloadSitesAfterDelete]);

  const onLandingSubmit = useCallback(
    (newLandingPageId: string) => {
      if (!selectedNode) return;
      void runMutation(async () => {
        await replaceLandingPage({
          sectionId: selectedNode.id,
          newLandingPageId,
        });
        setLandingOpen(false);
      });
    },
    [selectedNode, runMutation],
  );

  const onSectionLinkSubmit = useCallback(
    (targetSectionId: string) => {
      if (sectionLinkMode === "create") {
        if (!createParent) return;
        void runMutation(async () => {
          await createSectionLink(targetSectionId, createParent.id);
          setSectionLinkOpen(false);
        });
        return;
      }
      // edit
      if (!selectedNode || !treeRoot) return;
      const place = findSiblingPlacement(treeRoot, selectedNode.id);
      if (!place) {
        setMutationError(ARCH_MSG.MUTATION_ERROR);
        return;
      }
      void runMutation(async () => {
        await updateSectionLink({
          oldSectionId: selectedNode.id,
          newSectionId: targetSectionId,
          parentSectionId: place.parent.id,
        });
        setSectionLinkOpen(false);
      });
    },
    [sectionLinkMode, createParent, selectedNode, treeRoot, runMutation],
  );

  const onExternalLinkSubmit = useCallback(
    (values: {
      linkTitle: string;
      externalUrl: string;
      target: string;
    }) => {
      if (externalLinkMode === "create") {
        if (!selectedSite || !createParent) return;
        const folderPath = resolveCreateParentFolderPath(
          createParent,
          selectedSite,
        );
        void runMutation(async () => {
          await createExternalLinkSection({
            linkTitle: values.linkTitle,
            externalUrl: values.externalUrl,
            folderPath,
            sectionType: "externallink",
            target: values.target,
          });
          setExternalLinkOpen(false);
        });
        return;
      }
      if (!selectedNode) return;
      void runMutation(async () => {
        const loaded = await loadSection(selectedNode.id);
        // Prefer wire folderPath; fall back to tree node path so we never POST
        // an empty folderPath that would re-parent the link to site root.
        const parentForPath =
          findSiblingPlacement(treeRoot, selectedNode.id)?.parent ??
          treeRoot ??
          null;
        const folderPath =
          (loaded.folderPath && String(loaded.folderPath).trim()) ||
          (selectedNode.folderPath && String(selectedNode.folderPath).trim()) ||
          (selectedSite
            ? resolveCreateParentFolderPath(parentForPath, selectedSite)
            : "");
        await updateExternalLink(selectedNode.id, {
          linkTitle: values.linkTitle,
          externalUrl: values.externalUrl,
          folderPath,
          sectionType: "externallink",
          target: values.target,
          cssClassNames: loaded.cssClassNames,
        });
        setExternalLinkOpen(false);
      });
    },
    [
      externalLinkMode,
      selectedSite,
      createParent,
      selectedNode,
      treeRoot,
      runMutation,
    ],
  );

  const externalEditLoadGen = useRef(0);

  const openEditLink = useCallback(() => {
    if (!selectedNode) return;
    setMutationError(null);
    if (isSectionLinkType(selectedNode.sectionType)) {
      setSectionLinkMode("edit");
      setSectionLinkOpen(true);
      return;
    }
    if (isExternalLinkType(selectedNode.sectionType)) {
      setExternalLinkMode("edit");
      setExternalInitial(null);
      setExternalLinkOpen(true);
      const nodeId = selectedNode.id;
      const fallbackTitle = selectedNode.title;
      const gen = ++externalEditLoadGen.current;
      void (async () => {
        try {
          const loaded = await loadSection(nodeId);
          if (gen !== externalEditLoadGen.current) {
            return; // superseded by a later edit click or cancel
          }
          setExternalInitial({
            // Preserve intentional empty title from API; only fall back when nullish.
            linkTitle: loaded.title ?? fallbackTitle,
            externalUrl: loaded.externalLinkUrl || "",
            target: loaded.target || "_self",
          });
        } catch (err) {
          if (gen !== externalEditLoadGen.current) return;
          if (isSessionRedirectError(err)) return;
          setMutationError(formatApiError(err, ARCH_MSG.MUTATION_ERROR));
          setExternalLinkOpen(false);
        }
      })();
    }
  }, [selectedNode]);

  const siteNames =
    sitesState.status === "ready" ? sitesState.names : [];
  const siteOptions =
    selectedSite && !siteNames.includes(selectedSite)
      ? [{ name: selectedSite }, ...siteNames.map((name) => ({ name }))]
      : siteNames.map((name) => ({ name }));

  const treeLoading = treeState.status === "loading";
  const treeError =
    treeState.status === "error" ? treeState.message : null;

  const selectedSiblingPlace = useMemo(() => {
    if (!selectedNode || !treeRoot) return null;
    return findSiblingPlacement(treeRoot, selectedNode.id);
  }, [selectedNode, treeRoot]);

  const sectionLinkParentId =
    sectionLinkMode === "edit"
      ? selectedSiblingPlace?.parent.id ?? createParent?.id ?? ""
      : createParent?.id ?? "";
  const sectionLinkParentTitle =
    sectionLinkMode === "edit"
      ? selectedSiblingPlace?.parent.title ?? createParent?.title ?? ""
      : createParent?.title ?? "";

  return (
    <div
      className="perc-architecture-shell"
      data-testid="perc-architecture-shell"
      data-embedded={embedded ? "true" : "false"}
      data-site={selectedSite ?? ""}
      aria-busy={mutationBusy || undefined}
      style={{
        fontFamily: "var(--perc-font-family, sans-serif)",
        padding: embedded ? "8px 12px 20px" : "20px",
        maxWidth: "960px",
        margin: "0 auto",
      }}
    >
      <header style={{ marginBottom: "16px" }}>
        <h1
          style={{ marginBottom: "8px" }}
          data-testid="architecture-shell-title"
        >
          {ARCH_MSG.TITLE}
        </h1>
        <p
          style={{ margin: 0, color: catalogColors.muted, maxWidth: "48rem" }}
          data-testid="architecture-shell-intro"
        >
          {ARCH_MSG.INTRO}
        </p>
      </header>

      <section
        aria-label={ARCH_MSG.SITE_LABEL}
        data-testid="architecture-toolbar"
        style={{
          display: "flex",
          flexWrap: "wrap",
          alignItems: "center",
          gap: "0.75rem 1rem",
          marginBottom: "12px",
        }}
      >
        {sitesState.status === "loading" ? (
          <p
            style={{ margin: 0, color: catalogColors.muted }}
            data-testid="architecture-sites-loading"
            aria-live="polite"
          >
            {ARCH_MSG.SITES_LOADING}
          </p>
        ) : sitesState.status === "error" ? (
          <p
            style={{ margin: 0, color: catalogColors.error }}
            role="alert"
            data-testid="architecture-sites-error"
          >
            {sitesState.message}
          </p>
        ) : siteNames.length === 0 && !selectedSite ? (
          <p
            style={{ margin: 0, color: catalogColors.empty }}
            data-testid="architecture-sites-empty"
          >
            {ARCH_MSG.SITES_EMPTY}
          </p>
        ) : (
          <SitePicker
            sites={siteOptions}
            selectedSite={selectedSite}
            onChange={(name) => setSelectedSite(name)}
          />
        )}
        <button
          type="button"
          data-testid="architecture-refresh"
          onClick={onRefresh}
          disabled={!selectedSite || treeLoading || mutationBusy}
          style={{
            padding: "0.4rem 0.85rem",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: 4,
            background:
              !selectedSite || treeLoading || mutationBusy
                ? "#f0f0f0"
                : "#fff",
            color:
              !selectedSite || treeLoading || mutationBusy ? "#999" : "#222",
            cursor:
              !selectedSite || treeLoading || mutationBusy
                ? "not-allowed"
                : "pointer",
            fontSize: "0.9rem",
          }}
        >
          {ARCH_MSG.REFRESH}
        </button>
        {allowNewSite ? (
          <button
            type="button"
            ref={newSiteToggleRef}
            data-testid="architecture-action-new-site"
            aria-expanded={showNewSite}
            aria-haspopup="dialog"
            aria-controls={
              showNewSite ? "architecture-new-site-panel" : undefined
            }
            onClick={() => {
              setShowCopySite(false);
              setShowNewSite((open) => !open);
            }}
            style={{
              padding: "0.4rem 0.85rem",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              background: showNewSite ? "#e8eef8" : "#fff",
              color: "#222",
              cursor: "pointer",
              fontSize: "0.9rem",
            }}
          >
            {ARCH_MSG.ACTION_NEW_SITE}
          </button>
        ) : null}
        {allowNewSite ? (
          <button
            type="button"
            ref={copySiteToggleRef}
            data-testid="architecture-action-copy-site"
            aria-expanded={showCopySite}
            aria-haspopup="dialog"
            aria-controls={
              showCopySite ? "architecture-copy-site-panel" : undefined
            }
            disabled={!selectedSite || mutationBusy}
            onClick={onCopySite}
            style={{
              padding: "0.4rem 0.85rem",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              background: showCopySite ? "#e8eef8" : "#fff",
              color: !selectedSite || mutationBusy ? "#999" : "#222",
              cursor:
                !selectedSite || mutationBusy ? "not-allowed" : "pointer",
              fontSize: "0.9rem",
            }}
          >
            {ARCH_MSG.ACTION_COPY_SITE}
          </button>
        ) : null}
        {allowNewSite ? (
          <button
            type="button"
            data-testid="architecture-action-delete-site"
            disabled={!selectedSite || mutationBusy}
            onClick={onDeleteSite}
            style={{
              padding: "0.4rem 0.85rem",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              background: "#fff",
              color: !selectedSite || mutationBusy ? "#999" : "#a11",
              cursor:
                !selectedSite || mutationBusy ? "not-allowed" : "pointer",
              fontSize: "0.9rem",
            }}
          >
            {ARCH_MSG.ACTION_DELETE_SITE}
          </button>
        ) : null}
      </section>

      {mutationError ? (
        <p
          role="alert"
          data-testid="architecture-mutation-error"
          style={{
            margin: "0 0 12px",
            color: catalogColors.error,
            fontSize: "0.9rem",
          }}
        >
          {mutationError}
        </p>
      ) : null}

      {allowNewSite && showNewSite ? (
        <section
          id="architecture-new-site-panel"
          ref={newSitePanelRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="architecture-new-site-title"
          aria-label={ARCH_MSG.NEW_SITE_REGION}
          data-testid="architecture-new-site-panel"
          style={{
            border: `1px solid ${catalogColors.headerBorder}`,
            borderRadius: 8,
            padding: "1rem 1.25rem",
            marginBottom: "12px",
            background: "#fff",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: "0.75rem",
              marginBottom: "0.75rem",
            }}
          >
            <h2
              id="architecture-new-site-title"
              style={{ margin: 0, fontSize: "1.05rem", color: "#1a202c" }}
              data-testid="architecture-new-site-title"
            >
              {ARCH_MSG.ACTION_NEW_SITE}
            </h2>
            <button
              type="button"
              data-testid="architecture-new-site-close"
              onClick={() => setShowNewSite(false)}
              style={{
                padding: "0.3rem 0.7rem",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: 4,
                background: "#fff",
                cursor: "pointer",
              }}
            >
              {ARCH_MSG.NEW_SITE_CLOSE}
            </button>
          </div>
          <SiteCreateWizard
            onCreated={({ siteName }) => {
              setShowNewSite(false);
              void reloadSites(siteName);
            }}
          />
        </section>
      ) : null}

      {allowNewSite && showCopySite && selectedSite ? (
        <section
          id="architecture-copy-site-panel"
          ref={copySitePanelRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="architecture-copy-site-title"
          aria-label={ARCH_MSG.COPY_SITE_REGION}
          data-testid="architecture-copy-site-panel"
          style={{
            border: `1px solid ${catalogColors.headerBorder}`,
            borderRadius: 8,
            padding: "1rem 1.25rem",
            marginBottom: "12px",
            background: "#fff",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: "0.75rem",
              marginBottom: "0.75rem",
            }}
          >
            <h2
              id="architecture-copy-site-title"
              style={{ margin: 0, fontSize: "1.05rem", color: "#1a202c" }}
              data-testid="architecture-copy-site-title"
            >
              {ARCH_MSG.ACTION_COPY_SITE}
            </h2>
            <button
              type="button"
              data-testid="architecture-copy-site-close"
              onClick={() => setShowCopySite(false)}
              style={{
                padding: "0.3rem 0.7rem",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: 4,
                background: "#fff",
                cursor: "pointer",
              }}
            >
              {ARCH_MSG.COPY_SITE_CLOSE}
            </button>
          </div>
          <SiteCopyWizard
            key={selectedSite}
            initialSource={selectedSite}
            initialTarget={suggestCopySiteName(selectedSite)}
            submit={submitCopySite}
            onSettled={onCopySiteSettled}
          />
        </section>
      ) : null}

      {!selectedSite ? (
        <section
          aria-labelledby="architecture-empty-title"
          data-testid="architecture-empty-state"
          style={{
            border: `1px solid ${catalogColors.headerBorder}`,
            borderRadius: "8px",
            padding: "1.25rem 1.5rem",
            background: "#f8fafc",
          }}
        >
          <h2
            id="architecture-empty-title"
            style={{
              marginTop: 0,
              marginBottom: "0.5rem",
              fontSize: "1.1rem",
              color: "#1a202c",
            }}
            data-testid="architecture-empty-title"
          >
            {ARCH_MSG.EMPTY_TITLE}
          </h2>
          <p
            style={{
              margin: "0 0 0.75rem",
              color: catalogColors.muted,
              lineHeight: 1.5,
            }}
            data-testid="architecture-empty-body"
          >
            {ARCH_MSG.EMPTY_BODY}
          </p>
          <p
            style={{
              margin: 0,
              color: catalogColors.empty,
              fontSize: "0.95rem",
            }}
            data-testid="architecture-site-hint"
          >
            {ARCH_MSG.SITE_NONE}
          </p>
        </section>
      ) : (
        <section
          aria-labelledby="architecture-tree-heading"
          data-testid="architecture-tree-panel"
        >
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              alignItems: "baseline",
              justifyContent: "space-between",
              gap: "0.5rem",
              marginBottom: "8px",
            }}
          >
            <h2
              id="architecture-tree-heading"
              style={{
                margin: 0,
                fontSize: "1.05rem",
                color: "#1a202c",
              }}
              data-testid="architecture-tree-heading"
            >
              {ARCH_MSG.TREE_PANEL_TITLE}
            </h2>
            <p
              style={{
                margin: 0,
                color: catalogColors.empty,
                fontSize: "0.85rem",
              }}
              data-testid="architecture-site-hint"
            >
              {ARCH_MSG.SITE_HINT.split("{0}").join(selectedSite)}
            </p>
          </div>

          <StructureActionBar
            busy={mutationBusy}
            canCreate={canCreate}
            canCreateFromFolder={canCreateFromFolder}
            canConvertToFolder={canConvertToFolder}
            canCreateSectionLink={canCreateSectionLink}
            canCreateExternalLink={canCreateExternalLink}
            canLanding={canLanding}
            canEditLink={canEditLink}
            canRename={canRename}
            canMoveUp={canMoveUp}
            canMoveDown={canMoveDown}
            canDelete={canDelete}
            onCreate={() => {
              setMutationError(null);
              if (!createParent) {
                setMutationError(ARCH_MSG.CREATE_PARENT_BLOCKED);
                return;
              }
              setCreateOpen(true);
            }}
            onCreateFromFolder={() => {
              setMutationError(null);
              if (!createParent) {
                setMutationError(ARCH_MSG.CREATE_PARENT_BLOCKED);
                return;
              }
              setCreateFromFolderOpen(true);
            }}
            onConvertToFolder={onConvertToFolder}
            onCreateSectionLink={() => {
              setMutationError(null);
              if (!createParent) {
                setMutationError(ARCH_MSG.CREATE_PARENT_BLOCKED);
                return;
              }
              setSectionLinkMode("create");
              setSectionLinkOpen(true);
            }}
            onCreateExternalLink={() => {
              setMutationError(null);
              if (!createParent) {
                setMutationError(ARCH_MSG.CREATE_PARENT_BLOCKED);
                return;
              }
              setExternalLinkMode("create");
              setExternalInitial(null);
              setExternalLinkOpen(true);
            }}
            onLanding={() => {
              setMutationError(null);
              if (!selectedNode || !canReplaceLandingPage(selectedNode)) {
                setMutationError(ARCH_MSG.LANDING_BLOCKED);
                return;
              }
              setLandingOpen(true);
            }}
            onEditLink={openEditLink}
            onRename={() => {
              setMutationError(null);
              setRenameOpen(true);
            }}
            onMoveUp={() => onMove("up")}
            onMoveDown={() => onMove("down")}
            onDelete={onDelete}
          />

          {!selectedNodeId ? (
            <p
              style={{
                margin: "0 0 8px",
                color: catalogColors.muted,
                fontSize: "0.85rem",
              }}
              data-testid="architecture-select-hint"
            >
              {ARCH_MSG.SELECT_HINT}
            </p>
          ) : null}

          <NavTree
            root={treeRoot}
            loading={treeLoading}
            error={treeError}
            selectedId={selectedNodeId}
            onSelect={(node) => {
              setSelectedNodeId(node.id);
              setMutationError(null);
            }}
          />
          <p
            style={{
              margin: "0.75rem 0 0",
              color: catalogColors.empty,
              fontSize: "0.85rem",
            }}
            data-testid="architecture-structure-note"
          >
            {ARCH_MSG.TREE_STRUCTURE_NOTE}
          </p>
          <p
            style={{
              margin: "0.35rem 0 0",
              color: catalogColors.empty,
              fontSize: "0.8rem",
            }}
            data-testid="architecture-blog-note"
          >
            {ARCH_MSG.BLOG_NOTE}
          </p>
        </section>
      )}

      <CreateSectionDialog
        open={createOpen}
        siteName={selectedSite ?? ""}
        parentTitle={createParent?.title ?? ""}
        busy={mutationBusy}
        onCancel={() => {
          if (!mutationBusy) setCreateOpen(false);
        }}
        onSubmit={onCreateSubmit}
      />
      <CreateSectionFromFolderDialog
        open={createFromFolderOpen}
        siteName={selectedSite ?? ""}
        parentTitle={createParent?.title ?? ""}
        busy={mutationBusy}
        useContentBrowser={useLandingContentBrowser}
        onCancel={() => {
          if (!mutationBusy) setCreateFromFolderOpen(false);
        }}
        onSubmit={onCreateFromFolderSubmit}
      />
      <RenameSectionDialog
        open={renameOpen}
        initialTitle={selectedNode?.title ?? ""}
        busy={mutationBusy}
        onCancel={() => {
          if (!mutationBusy) setRenameOpen(false);
        }}
        onSubmit={onRenameSubmit}
      />
      <ReplaceLandingPageDialog
        open={landingOpen}
        siteName={selectedSite ?? ""}
        sectionTitle={selectedNode?.title ?? ""}
        busy={mutationBusy}
        useContentBrowser={useLandingContentBrowser}
        onCancel={() => {
          if (!mutationBusy) setLandingOpen(false);
        }}
        onSubmit={onLandingSubmit}
      />
      <SectionLinkDialog
        open={sectionLinkOpen}
        mode={sectionLinkMode}
        parentId={sectionLinkParentId}
        parentTitle={sectionLinkParentTitle}
        treeRoot={treeRoot}
        busy={mutationBusy}
        linkSectionId={
          sectionLinkMode === "edit" ? selectedNode?.id ?? null : null
        }
        onCancel={() => {
          if (!mutationBusy) setSectionLinkOpen(false);
        }}
        onSubmit={onSectionLinkSubmit}
      />
      <ExternalLinkDialog
        open={externalLinkOpen}
        mode={externalLinkMode}
        parentTitle={createParent?.title ?? selectedNode?.title ?? ""}
        busy={mutationBusy}
        initial={externalInitial}
        onCancel={() => {
          if (!mutationBusy) setExternalLinkOpen(false);
        }}
        onSubmit={onExternalLinkSubmit}
      />
    </div>
  );
};

export default ArchitectureShell;
