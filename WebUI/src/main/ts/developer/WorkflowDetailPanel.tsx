/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import {
  getWorkflowAllowedContentTypes,
  getWorkflowDetail,
  setWorkflowAllowedContentTypes,
} from "../api/developer/workflowsApi";
import type { NamedObjectRef, WorkflowDef } from "../api/developer/types";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import {
  cloneNamedObjectRefs,
  namedObjectRefsEqual,
} from "./contentTypeWorkflows";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { buildAllowedContentTypesReplaceBody } from "./workflowContentTypes";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const smallBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "4px 10px",
  font: "inherit",
};

const primaryBtnStyle: React.CSSProperties = {
  background: catalogColors.accent,
  color: "#fff",
  border: "none",
  borderRadius: "4px",
  padding: "8px 14px",
  font: "inherit",
};

function refKey(r: NamedObjectRef, index: number): string {
  if (r.name) return `name:${r.name}`;
  if (r.guid?.stringValue) return `guid:${r.guid.stringValue}`;
  if (r.guid?.uuid != null) return `uuid:${r.guid.uuid}`;
  return `idx:${index}`;
}

export function WorkflowDetailPanel({
  name,
  onBack,
}: {
  name: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<WorkflowDef | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [contentTypes, setContentTypes] = useState<NamedObjectRef[]>([]);
  const [baselineContentTypes, setBaselineContentTypes] = useState<NamedObjectRef[]>(
    [],
  );
  const [ctLoading, setCtLoading] = useState(false);
  const [ctError, setCtError] = useState<string | null>(null);
  const [newCtName, setNewCtName] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setNewCtName("");
    setContentTypes([]);
    setBaselineContentTypes([]);
    setCtError(null);
    setCtLoading(true);

    getWorkflowDetail(name)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.WF_DETAIL_ERROR));
      });

    getWorkflowAllowedContentTypes(name)
      .then((list) => {
        if (cancelled) {
          return;
        }
        const cloned = cloneNamedObjectRefs(list);
        setContentTypes(cloned);
        setBaselineContentTypes(cloneNamedObjectRefs(cloned));
      })
      .catch((err: unknown) => {
        if (cancelled) {
          return;
        }
        setCtError(panelErrMsg(err, DEV_MSG.WF_CT_LOAD_ERROR));
        setContentTypes([]);
        setBaselineContentTypes([]);
      })
      .finally(() => {
        if (!cancelled) {
          setCtLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [name]);

  const steps =
    detail != null && Array.isArray(detail.workflowSteps) ? detail.workflowSteps : [];

  const dirty = useMemo(
    () => !namedObjectRefsEqual(contentTypes, baselineContentTypes),
    [contentTypes, baselineContentTypes],
  );

  const addContentType = () => {
    const trimmed = newCtName.trim();
    if (!trimmed || busy) {
      return;
    }
    if (
      contentTypes.some(
        (r) => (r.name || "").toLowerCase() === trimmed.toLowerCase(),
      )
    ) {
      setCtError(DEV_MSG.WF_CT_DUP);
      return;
    }
    setCtError(null);
    setNotice(null);
    setContentTypes((prev) => [...prev, { name: trimmed }]);
    setNewCtName("");
  };

  const removeContentType = (index: number) => {
    if (busy) {
      return;
    }
    setCtError(null);
    setNotice(null);
    setContentTypes((prev) => prev.filter((_, i) => i !== index));
  };

  const saveContentTypes = async () => {
    if (!dirty || busy) {
      return;
    }
    setBusy(true);
    setCtError(null);
    setNotice(null);
    try {
      const saved = await setWorkflowAllowedContentTypes(
        name,
        buildAllowedContentTypesReplaceBody(contentTypes),
      );
      const cloned = cloneNamedObjectRefs(saved);
      setContentTypes(cloned);
      setBaselineContentTypes(cloneNamedObjectRefs(cloned));
      setNotice(DEV_MSG.WF_CT_SAVE_SUCCESS);
    } catch (err: unknown) {
      setCtError(panelErrMsg(err, DEV_MSG.WF_CT_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div data-testid="developer-wf-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-wf-back"
        aria-label={DEV_MSG.WF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.WF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-wf-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-wf-detail-loading">{DEV_MSG.WF_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-wf-detail-title">
              {detail.workflowName || name}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.WF_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.workflowName || "—"}</dd>
              <dt>{DEV_MSG.WF_COL_DESC}</dt>
              <dd style={{ margin: 0 }}>{detail.workflowDescription || "—"}</dd>
              <dt>{DEV_MSG.WF_COL_DEFAULT}</dt>
              <dd style={{ margin: 0 }}>
                {detail.defaultWorkflow ? DEV_MSG.WF_YES : DEV_MSG.WF_NO}
              </dd>
              <dt>{DEV_MSG.WF_COL_STAGING}</dt>
              <dd style={{ margin: 0 }}>{detail.stagingRoleNames || "—"}</dd>
            </dl>
          </header>

          <section data-testid="developer-wf-steps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_STEPS}</h3>
            {steps.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.WF_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-wf-steps-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_STEP}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_PERMS}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_ROLES}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {steps.map((s, i) => {
                      const roles = Array.isArray(s.stepRoles)
                        ? s.stepRoles
                            .map((r) => r.roleName)
                            .filter(Boolean)
                            .join(", ")
                        : "";
                      const perms = Array.isArray(s.permissionNames)
                        ? s.permissionNames.join(", ")
                        : "";
                      return (
                        <tr key={`${s.stepName ?? "s"}-${i}`} style={tableRow}>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {s.stepName || "—"}
                          </td>
                          <td style={{ padding: "8px" }}>{perms || "—"}</td>
                          <td style={{ padding: "8px" }}>{roles || "—"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section
            style={{ marginTop: "16px", marginBottom: "16px" }}
            data-testid="developer-wf-content-types"
          >
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_CONTENT_TYPES}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DEV_MSG.WF_CONTENT_TYPES_HINT}
            </p>
            {ctError ? (
              <div
                role="alert"
                data-testid="developer-wf-ct-error"
                style={{ ...errorAlert, marginBottom: "8px" }}
              >
                {ctError}
              </div>
            ) : null}
            {notice ? (
              <div
                data-testid="developer-wf-ct-notice"
                style={{ color: catalogColors.accent, marginBottom: "8px" }}
              >
                {notice}
              </div>
            ) : null}
            {ctLoading ? (
              <div data-testid="developer-wf-ct-loading">{DEV_MSG.WF_CT_LOADING}</div>
            ) : null}
            {!ctLoading && contentTypes.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-wf-ct-empty">
                {DEV_MSG.WF_NONE}
              </p>
            ) : null}
            {!ctLoading && contentTypes.length > 0 ? (
              <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {contentTypes.map((ct, i) => (
                  <li
                    key={refKey(ct, i)}
                    data-testid={`developer-wf-ct-row-${i}`}
                    style={{
                      ...tableRow,
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "6px 0",
                    }}
                  >
                    <span>
                      {ct.label || ct.name || "—"}
                      {ct.name ? (
                        <span
                          style={{
                            fontFamily: "monospace",
                            color: catalogColors.empty,
                            marginLeft: "8px",
                            fontSize: "0.85rem",
                          }}
                        >
                          {ct.name}
                        </span>
                      ) : null}
                    </span>
                    <button
                      type="button"
                      data-testid={`developer-wf-ct-remove-${i}`}
                      aria-label={`${DEV_MSG.CT_ASSOC_REMOVE} ${ct.name || ct.label || ""}`}
                      disabled={busy}
                      onClick={() => removeContentType(i)}
                      style={{
                        ...smallBtnStyle,
                        marginLeft: "auto",
                        cursor: busy ? "not-allowed" : "pointer",
                      }}
                    >
                      {DEV_MSG.CT_ASSOC_REMOVE}
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
            <div
              style={{
                marginTop: "12px",
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: "8px",
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="wf-ct-add" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.WF_CONTENT_TYPES}
                </label>
                <input
                  id="wf-ct-add"
                  data-testid="developer-wf-ct-add-name"
                  style={inputStyle}
                  placeholder={DEV_MSG.WF_CT_NAME_PLACEHOLDER}
                  value={newCtName}
                  onChange={(e) => setNewCtName(e.target.value)}
                  disabled={busy || ctLoading}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addContentType();
                    }
                  }}
                />
              </div>
              <button
                type="button"
                data-testid="developer-wf-ct-add"
                disabled={busy || ctLoading || !newCtName.trim()}
                onClick={addContentType}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor:
                    busy || ctLoading || !newCtName.trim() ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.CT_ASSOC_ADD}
              </button>
            </div>
            <div style={{ marginTop: "12px" }}>
              <button
                type="button"
                data-testid="developer-wf-ct-save"
                disabled={!dirty || busy || ctLoading}
                onClick={() => void saveContentTypes()}
                style={{
                  ...primaryBtnStyle,
                  background: !dirty || busy || ctLoading ? catalogColors.disabled : catalogColors.accent,
                  cursor: !dirty || busy || ctLoading ? "not-allowed" : "pointer",
                }}
              >
                {busy ? DEV_MSG.WF_CT_SAVING : DEV_MSG.WF_CT_SAVE}
              </button>
            </div>
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-wf-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {(detail.designGaps && detail.designGaps.length
                ? detail.designGaps
                : [DEV_MSG.WF_GAP_GRAPH, DEV_MSG.WF_GAP_WRITE]
              ).map((g, i) => (
                <li key={`${g}-${i}`}>{g}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
