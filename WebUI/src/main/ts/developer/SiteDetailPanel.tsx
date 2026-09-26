/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useRef, useState } from "react";
import { isApiError } from "../api/client";
import { objectGuidString } from "../api/displayFormatGuid";
import {
  coerceDisplayString,
  deleteSite,
  updateSite,
} from "../api/developer/sitesApi";
import type { SiteDef } from "../api/developer/types";
import { listWorkflows } from "../api/developer/workflowsApi";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
} from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";
import { VirtualSiteSourcePanel } from "./VirtualSiteSourcePanel";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

export function SiteDetailPanel({
  site,
  onBack,
  onDeleted,
  onUpdated,
}: {
  site: SiteDef;
  onBack: () => void;
  onDeleted?: () => void;
  onUpdated?: (site: SiteDef) => void;
}): React.ReactElement {
  const name = coerceDisplayString(site.name) || "—";
  const siteKey = coerceDisplayString(site.name);
  const objectGuid = objectGuidString(site.guid);
  const gaps =
    site.designGaps && site.designGaps.length
      ? site.designGaps
      : [DEV_MSG.SITE_GAP_PUBLISH];

  const [description, setDescription] = useState(site.description || "");
  const [baseUrl, setBaseUrl] = useState(site.baseUrl || "");
  const [workflowName, setWorkflowName] = useState(site.workflowName || "");
  const [workflowOptions, setWorkflowOptions] = useState<string[]>([]);
  const [workflowLoadError, setWorkflowLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const [saveBusy, setSaveBusy] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const inflight = useRef(false);

  useEffect(() => {
    setWorkflowName(site.workflowName || "");
  }, [site.workflowName, siteKey]);

  useEffect(() => {
    let cancelled = false;
    setWorkflowLoadError(null);
    listWorkflows()
      .then((rows) => {
        if (cancelled) return;
        const names = rows
          .map((row) => (row.workflowName || "").trim())
          .filter((name) => name.length > 0);
        setWorkflowOptions(Array.from(new Set(names)));
      })
      .catch(() => {
        if (!cancelled) setWorkflowLoadError(DEV_MSG.SITE_WF_LOAD_ERROR);
      });
    return () => {
      cancelled = true;
    };
  }, [siteKey]);

  function saveFallback(err: unknown): string {
    if (isApiError(err)) {
      if (err.status === 403) return DEV_MSG.SITE_FORBIDDEN;
      if (err.status === 404) return DEV_MSG.SITE_NOT_FOUND;
      if (err.status === 400) return DEV_MSG.SITE_UNKNOWN_WORKFLOW;
    }
    return DEV_MSG.SITE_SAVE_ERROR;
  }

  function deleteFallback(err: unknown): string {
    if (isApiError(err) && err.status === 403) {
      return DEV_MSG.SITE_FORBIDDEN;
    }
    return DEV_MSG.SITE_DELETE_ERROR;
  }

  async function handleSave(): Promise<void> {
    if (!siteKey || inflight.current) return;
    inflight.current = true;
    setSaveBusy(true);
    setSaveError(null);
    setSaveNotice(null);
    try {
      const chosen = workflowName.trim();
      const saved = await updateSite(siteKey, {
        name: siteKey,
        description: description.trim(),
        baseUrl: baseUrl.trim(),
        ...(chosen ? { workflowName: chosen } : {}),
      });
      if (saved.workflowName) {
        setWorkflowName(saved.workflowName);
      }
      setSaveNotice(DEV_MSG.SITE_SAVED);
      onUpdated?.(saved);
    } catch (err: unknown) {
      setSaveError(panelErrMsg(err, saveFallback(err)));
    } finally {
      inflight.current = false;
      setSaveBusy(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (!siteKey || inflight.current) return;
    inflight.current = true;
    setDeleteBusy(true);
    setConfirmDeleteOpen(false);
    setDeleteError(null);
    try {
      await deleteSite(siteKey);
      onDeleted?.();
    } catch (err: unknown) {
      setDeleteError(panelErrMsg(err, deleteFallback(err)));
    } finally {
      inflight.current = false;
      setDeleteBusy(false);
    }
  }

  return (
    <div data-testid="developer-site-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-site-back"
        aria-label={DEV_MSG.SITE_BACK}
        style={backButton}
      >
        ← {DEV_MSG.SITE_BACK}
      </button>

      {saveError ? (
        <div role="alert" data-testid="developer-site-save-error" style={errorAlert}>
          {saveError}
        </div>
      ) : null}
      {saveNotice ? (
        <div data-testid="developer-site-save-notice" style={{ color: "#276749" }}>
          {saveNotice}
        </div>
      ) : null}
      {deleteError ? (
        <div role="alert" data-testid="developer-site-delete-error" style={errorAlert}>
          {deleteError}
        </div>
      ) : null}

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 4px" }} data-testid="developer-site-detail-title">
          {name}
        </h2>
        <dl style={metaGrid}>
          <dt>{DEV_MSG.SITE_COL_NAME}</dt>
          <dd style={{ margin: 0, ...monoCell }}>{name}</dd>
          <dt>{DEV_MSG.SITE_COL_GUID}</dt>
          <dd
            style={{ margin: 0, ...monoCell }}
            data-testid="developer-site-detail-guid"
          >
            {objectGuid || "—"}
          </dd>
          <dt>{DEV_MSG.SITE_COL_DESC}</dt>
          <dd style={{ margin: 0 }}>
            <input
              data-testid="developer-site-description-input"
              style={inputStyle}
              value={description}
              disabled={saveBusy || deleteBusy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </dd>
          <dt>{DEV_MSG.SITE_COL_URL}</dt>
          <dd style={{ margin: 0 }}>
            <input
              data-testid="developer-site-base-url-input"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={baseUrl}
              disabled={saveBusy || deleteBusy}
              onChange={(e) => setBaseUrl(e.target.value)}
            />
          </dd>
          <dt>{DEV_MSG.SITE_COL_PROTOCOL}</dt>
          <dd style={{ margin: 0 }}>{site.siteProtocol || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_DEFAULT_DOC}</dt>
          <dd style={{ margin: 0, ...monoCell }}>{site.defaultDocument || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_EXT}</dt>
          <dd style={{ margin: 0 }}>{site.defaultFileExtention || "—"}</dd>
          <dt>{DEV_MSG.SITE_COL_PAGE_BASED}</dt>
          <dd style={{ margin: 0 }}>
            {site.pageBasedSite ? DEV_MSG.SITE_YES : DEV_MSG.SITE_NO}
          </dd>
          <dt>{DEV_MSG.SITE_COL_WORKFLOW}</dt>
          <dd style={{ margin: 0 }}>
            <select
              data-testid="developer-site-workflow"
              style={inputStyle}
              value={workflowName}
              disabled={saveBusy || deleteBusy}
              aria-label={DEV_MSG.SITE_COL_WORKFLOW}
              onChange={(e) => setWorkflowName(e.target.value)}
            >
              <option value="">{DEV_MSG.SITE_WF_PLACEHOLDER}</option>
              {workflowName && !workflowOptions.includes(workflowName) ? (
                <option value={workflowName}>{workflowName}</option>
              ) : null}
              {workflowOptions.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
            {workflowLoadError ? (
              <div data-testid="developer-site-workflow-error">{workflowLoadError}</div>
            ) : null}
          </dd>
        </dl>
        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "12px" }}>
          <button
            type="button"
            data-testid="developer-site-save"
            disabled={saveBusy || deleteBusy || !siteKey}
            onClick={() => void handleSave()}
            style={{
              padding: "8px 14px",
              background: catalogColors.accent,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
            }}
          >
            {saveBusy ? DEV_MSG.SITE_SAVING : DEV_MSG.SITE_SAVE}
          </button>
          <button
            type="button"
            data-testid="developer-site-delete"
            disabled={saveBusy || deleteBusy || !siteKey}
            onClick={() => setConfirmDeleteOpen(true)}
          >
            {deleteBusy ? DEV_MSG.SITE_DELETING : DEV_MSG.SITE_DELETE}
          </button>
        </div>
      </header>

      {siteKey ? <VirtualSiteSourcePanel siteName={siteKey} /> : null}

      <ObjectAclSection
        objectGuid={objectGuid}
        objectKind="site"
        testIdPrefix="developer-site-acl"
      />

      <section data-testid="developer-site-gaps">
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SITE_GAPS}</h3>
        <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {gaps.map((g, i) => (
            <li key={`${g}-${i}`}>{g}</li>
          ))}
        </ul>
      </section>

      <CatalogConfirmDialog
        open={confirmDeleteOpen}
        busy={deleteBusy}
        message={DEV_MSG.SITE_DELETE_CONFIRM}
        onCancel={() => setConfirmDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
