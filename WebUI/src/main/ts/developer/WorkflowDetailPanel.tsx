/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useRef, useState } from "react";
import { isApiError } from "../api/client";
import { isValidContentTypeName } from "../api/developer/contentTypesApi";
import {
  createWorkflowStep,
  deleteWorkflow,
  getWorkflowAllowedContentTypes,
  getWorkflowDetail,
  isValidWorkflowStepName,
  setWorkflowAllowedContentTypes,
  updateWorkflow,
  updateWorkflowStep,
  type WorkflowCreateResult,
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
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import {
  cloneNamedObjectRefs,
  namedObjectRefsEqual,
  refKey,
} from "./contentTypeWorkflows";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { buildAllowedContentTypesReplaceBody } from "./workflowContentTypes";
import { formatStepTransitionNames } from "./workflowStepTransitions";
import { WorkflowGraphView } from "./WorkflowGraphView";

/** Canonical Percussion GUID shape: type-host-uuid (three numeric groups). */
const PERC_GUID_RE = /^\d+-\d+-\d+$/;

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

function isAllowedContentTypeInput(raw: string): boolean {
  return isValidContentTypeName(raw) || PERC_GUID_RE.test(raw);
}

function detailLoadErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 404) {
      return DEV_MSG.WF_NOT_FOUND;
    }
    if (err.status === 403) {
      return DEV_MSG.WF_FORBIDDEN;
    }
  }
  return DEV_MSG.WF_DETAIL_ERROR;
}

export function WorkflowDetailPanel({
  name,
  onBack,
  onDeleted,
}: {
  name: string;
  onBack: () => void;
  onDeleted?: () => void;
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
  const [descriptionDraft, setDescriptionDraft] = useState("");
  const [baselineDescription, setBaselineDescription] = useState("");
  const [descriptionDirty, setDescriptionDirty] = useState(false);
  const [descriptionBusy, setDescriptionBusy] = useState(false);
  const [descriptionError, setDescriptionError] = useState<string | null>(null);
  const [descriptionNotice, setDescriptionNotice] = useState<string | null>(null);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [stepNameDraft, setStepNameDraft] = useState("");
  const [stepAfter, setStepAfter] = useState("");
  const [stepRolesDraft, setStepRolesDraft] = useState("Admin");
  const [editingStep, setEditingStep] = useState<string | null>(null);
  const [stepBusy, setStepBusy] = useState(false);
  const [stepError, setStepError] = useState<string | null>(null);
  const [stepNotice, setStepNotice] = useState<string | null>(null);
  const inflight = useRef(false);

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
    setDescriptionDraft("");
    setBaselineDescription("");
    setDescriptionDirty(false);
    setDescriptionError(null);
    setDescriptionNotice(null);
    setConfirmDeleteOpen(false);
    setDeleteError(null);
    setStepNameDraft("");
    setStepAfter("");
    setStepRolesDraft("Admin");
    setEditingStep(null);
    setStepError(null);
    setStepNotice(null);

    getWorkflowDetail(name)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        const description = d.workflowDescription ?? "";
        setDescriptionDraft(description);
        setBaselineDescription(description);
        setDescriptionDirty(false);
        const firstStep = Array.isArray(d.workflowSteps)
          ? d.workflowSteps.find((s) => s.stepName)?.stepName
          : "";
        if (firstStep) {
          setStepAfter(firstStep);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(panelErrMsg(err, detailLoadErrorFallback(err)));
        }
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

  const trimmedCtInput = newCtName.trim();
  const ctInputValid = !trimmedCtInput || isAllowedContentTypeInput(trimmedCtInput);
  const canAddContentType =
    !!trimmedCtInput && ctInputValid && !busy && !ctLoading;

  const addContentType = () => {
    const trimmed = newCtName.trim();
    if (!trimmed || busy || ctLoading) {
      return;
    }
    if (!isAllowedContentTypeInput(trimmed)) {
      setCtError(DEV_MSG.WF_CT_NAME_INVALID);
      return;
    }
    const looksLikeGuid = PERC_GUID_RE.test(trimmed);
    const exists = contentTypes.some((r) => {
      if (looksLikeGuid) {
        return (
          r.guid?.stringValue === trimmed ||
          (r.name || "").toLowerCase() === trimmed.toLowerCase()
        );
      }
      return (
        (r.name || "").toLowerCase() === trimmed.toLowerCase() ||
        r.guid?.stringValue === trimmed
      );
    });
    if (exists) {
      setCtError(DEV_MSG.WF_CT_DUP);
      return;
    }
    setCtError(null);
    setNotice(null);
    setContentTypes((prev) => [
      ...prev,
      looksLikeGuid
        ? { guid: { stringValue: trimmed }, name: trimmed, label: trimmed }
        : { name: trimmed },
    ]);
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

  function handleDescriptionChange(next: string): void {
    setDescriptionDraft(next);
    setDescriptionDirty(next !== baselineDescription);
    if (descriptionError) {
      setDescriptionError(null);
    }
    if (descriptionNotice) {
      setDescriptionNotice(null);
    }
  }

  function descriptionSaveErrorFallback(err: unknown): string {
    if (isApiError(err)) {
      if (err.status === 400) {
        return DEV_MSG.WF_DETAIL_NAME_MISMATCH;
      }
      if (err.status === 404) {
        return DEV_MSG.WF_DETAIL_ERROR;
      }
      if (err.status === 403) {
        return DEV_MSG.WF_FORBIDDEN;
      }
    }
    return DEV_MSG.WF_DETAIL_SAVE_ERROR;
  }

  async function handleDescriptionSave(): Promise<void> {
    if (!descriptionDirty || descriptionBusy || inflight.current) {
      return;
    }
    const targetName = (detail?.workflowName || name).trim();
    if (!targetName) {
      setDescriptionError(DEV_MSG.WF_DETAIL_NAME_MISMATCH);
      return;
    }
    inflight.current = true;
    setDescriptionBusy(true);
    setDescriptionError(null);
    setDescriptionNotice(null);
    try {
      const updated: WorkflowCreateResult = await updateWorkflow(targetName, {
        name: targetName,
        description: descriptionDraft,
      });
      const next = updated.workflowDescription ?? "";
      setDetail((prev) =>
        prev == null
          ? prev
          : ({
              ...prev,
              workflowName: updated.workflowName || prev.workflowName,
              workflowDescription: next,
            } as WorkflowDef),
      );
      setDescriptionDraft(next);
      setBaselineDescription(next);
      setDescriptionDirty(false);
      setDescriptionNotice(DEV_MSG.WF_DETAIL_SAVED);
    } catch (err: unknown) {
      setDescriptionError(
        panelErrMsg(err, descriptionSaveErrorFallback(err)),
      );
    } finally {
      inflight.current = false;
      setDescriptionBusy(false);
    }
  }

  function descriptionDeleteErrorFallback(err: unknown): string {
    if (isApiError(err)) {
      if (err.status === 404) {
        return DEV_MSG.WF_DETAIL_ERROR;
      }
      if (err.status === 403) {
        return DEV_MSG.WF_FORBIDDEN;
      }
      if (err.status === 409) {
        const raw = typeof err.body === "string" ? err.body : "";
        const msg = (raw || err.statusText || "").toLowerCase();
        if (msg.includes("system")) {
          return DEV_MSG.WF_DETAIL_DELETE_SYSTEM;
        }
        return DEV_MSG.WF_DETAIL_DELETE_HAS_ITEMS;
      }
    }
    return DEV_MSG.WF_DETAIL_DELETE_ERROR;
  }

  function requestDelete(): void {
    if (inflight.current || deleteBusy) {
      return;
    }
    setDeleteError(null);
    setConfirmDeleteOpen(true);
  }

  function stepSaveErrorFallback(err: unknown): string {
    if (isApiError(err)) {
      if (err.status === 400) {
        return DEV_MSG.WF_STEP_INVALID;
      }
      if (err.status === 403) {
        const raw = typeof err.body === "string" ? err.body : "";
        const msg = (raw || err.statusText || "").toLowerCase();
        if (msg.includes("packaged") || msg.includes("default")) {
          return DEV_MSG.WF_STEP_PACKAGED;
        }
        return DEV_MSG.WF_STEP_FORBIDDEN;
      }
    }
    return DEV_MSG.WF_STEP_ERROR;
  }

  function parseRoleNames(raw: string): string[] {
    return raw
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
  }

  async function reloadDetail(): Promise<void> {
    const d = await getWorkflowDetail(name);
    setDetail(d);
    const firstStep = Array.isArray(d.workflowSteps)
      ? d.workflowSteps.find((s) => s.stepName)?.stepName
      : "";
    if (firstStep && !editingStep) {
      setStepAfter((prev) => prev || firstStep);
    }
  }

  function beginEditStep(stepName: string): void {
    setEditingStep(stepName);
    setStepNameDraft(stepName);
    setStepError(null);
    setStepNotice(null);
  }

  async function handleStepSave(): Promise<void> {
    if (stepBusy || inflight.current) {
      return;
    }
    const trimmed = stepNameDraft.trim();
    if (!isValidWorkflowStepName(trimmed)) {
      setStepError(DEV_MSG.WF_STEP_INVALID);
      return;
    }
    inflight.current = true;
    setStepBusy(true);
    setStepError(null);
    setStepNotice(null);
    const roles = parseRoleNames(stepRolesDraft);
    try {
      if (editingStep) {
        await updateWorkflowStep(name, editingStep, {
          name: trimmed,
          roleNames: roles,
        });
      } else {
        await createWorkflowStep(name, {
          name: trimmed,
          afterStep: stepAfter.trim() || undefined,
          roleNames: roles,
        });
      }
      await reloadDetail();
      setStepNotice(DEV_MSG.WF_STEP_SAVED);
      setStepNameDraft("");
      setEditingStep(null);
    } catch (err: unknown) {
      setStepError(panelErrMsg(err, stepSaveErrorFallback(err)));
    } finally {
      inflight.current = false;
      setStepBusy(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (inflight.current || deleteBusy) {
      return;
    }
    inflight.current = true;
    setDeleteBusy(true);
    setConfirmDeleteOpen(false);
    setDeleteError(null);
    try {
      await deleteWorkflow(name);
      onDeleted?.();
    } catch (err: unknown) {
      setDeleteError(panelErrMsg(err, descriptionDeleteErrorFallback(err)));
    } finally {
      inflight.current = false;
      setDeleteBusy(false);
    }
  }

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

          <section
            style={{ marginBottom: "16px" }}
            data-testid="developer-wf-description-editor"
          >
            <h3 style={{ fontSize: "1rem", margin: "0 0 4px" }}>
              {DEV_MSG.WF_DETAIL_DESCRIPTION_EDIT_LABEL}
            </h3>
            <p
              style={{
                color: catalogColors.muted,
                fontSize: "0.9rem",
                margin: "0 0 8px",
              }}
            >
              {DEV_MSG.WF_DETAIL_DESCRIPTION_HINT}
            </p>
            {descriptionError ? (
              <div
                role="alert"
                data-testid="developer-wf-description-error"
                style={{ ...errorAlert, marginBottom: "8px" }}
              >
                {descriptionError}
              </div>
            ) : null}
            {descriptionNotice ? (
              <div
                role="status"
                aria-live="polite"
                data-testid="developer-wf-description-notice"
                style={{
                  color: catalogColors.accent,
                  marginBottom: "8px",
                }}
              >
                {descriptionNotice}
              </div>
            ) : null}
            <input
              id="wf-description-edit"
              data-testid="developer-wf-description-input"
              style={inputStyle}
              value={descriptionDraft}
              disabled={descriptionBusy}
              onChange={(e) => handleDescriptionChange(e.target.value)}
              aria-label={DEV_MSG.WF_DETAIL_DESCRIPTION_EDIT_LABEL}
            />
            <div style={{ marginTop: "8px" }}>
              <button
                type="button"
                data-testid="developer-wf-description-save"
                disabled={!descriptionDirty || descriptionBusy}
                onClick={() => void handleDescriptionSave()}
                style={{
                  ...primaryBtnStyle,
                  background:
                    !descriptionDirty || descriptionBusy
                      ? catalogColors.disabled
                      : catalogColors.accent,
                  cursor:
                    !descriptionDirty || descriptionBusy ? "not-allowed" : "pointer",
                }}
              >
                {descriptionBusy ? DEV_MSG.WF_DETAIL_SAVING : DEV_MSG.WF_DETAIL_SAVE}
              </button>
            </div>
          </section>

          {deleteError ? (
            <div
              role="alert"
              data-testid="developer-wf-delete-error"
              style={{ ...errorAlert, marginBottom: "12px" }}
            >
              {deleteError}
            </div>
          ) : null}
          <div style={{ marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-wf-delete"
              aria-label={DEV_MSG.WF_DETAIL_DELETE}
              disabled={deleteBusy}
              onClick={requestDelete}
              style={{
                padding: "8px 16px",
                background: "#c53030",
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: deleteBusy ? "wait" : "pointer",
              }}
            >
              {DEV_MSG.WF_DETAIL_DELETE}
            </button>
          </div>

          <CatalogConfirmDialog
            open={confirmDeleteOpen}
            busy={deleteBusy}
            message={DEV_MSG.WF_DETAIL_DELETE_CONFIRM}
            onCancel={() => setConfirmDeleteOpen(false)}
            onConfirm={() => void handleDelete()}
          />

          <WorkflowGraphView workflowName={name} />

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
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_TRANSITIONS}</th>
                      <th style={{ padding: "8px" }} />
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
                      const transitions = formatStepTransitionNames(s);
                      return (
                        <tr
                          key={`${s.stepName ?? "s"}-${i}`}
                          data-testid={`developer-wf-step-row-${i}`}
                          style={tableRow}
                        >
                          <td
                            style={{ padding: "8px", fontFamily: "monospace" }}
                            data-testid={`developer-wf-step-name-${i}`}
                          >
                            {s.stepName || "—"}
                          </td>
                          <td style={{ padding: "8px" }}>{perms || "—"}</td>
                          <td style={{ padding: "8px" }}>{roles || "—"}</td>
                          <td
                            style={{ padding: "8px" }}
                            data-testid={`developer-wf-step-transitions-${i}`}
                          >
                            {transitions || "—"}
                          </td>
                          <td style={{ padding: "8px" }}>
                            {s.stepName ? (
                              <button
                                type="button"
                                data-testid={`developer-wf-step-edit-${i}`}
                                style={smallBtnStyle}
                                onClick={() => beginEditStep(s.stepName as string)}
                              >
                                {DEV_MSG.WF_STEP_EDIT}
                              </button>
                            ) : null}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <div
              style={{ marginTop: "12px" }}
              data-testid="developer-wf-step-editor"
            >
              <p
                style={{
                  color: catalogColors.muted,
                  fontSize: "0.9rem",
                  margin: "0 0 8px",
                }}
              >
                {DEV_MSG.WF_STEP_HINT}
              </p>
              {stepError ? (
                <div
                  role="alert"
                  data-testid="developer-wf-step-error"
                  style={{ ...errorAlert, marginBottom: "8px" }}
                >
                  {stepError}
                </div>
              ) : null}
              {stepNotice ? (
                <div
                  role="status"
                  aria-live="polite"
                  data-testid="developer-wf-step-notice"
                  style={{ color: catalogColors.accent, marginBottom: "8px" }}
                >
                  {stepNotice}
                </div>
              ) : null}
              <label htmlFor="wf-step-name" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.WF_STEP_NAME_LABEL}
              </label>
              <input
                id="wf-step-name"
                data-testid="developer-wf-step-name"
                style={inputStyle}
                value={stepNameDraft}
                disabled={stepBusy}
                onChange={(e) => {
                  setStepNameDraft(e.target.value);
                  if (stepError) {
                    setStepError(null);
                  }
                }}
                aria-label={DEV_MSG.WF_STEP_NAME_LABEL}
              />
              {!editingStep ? (
                <>
                  <label
                    htmlFor="wf-step-after"
                    style={{ display: "block", margin: "8px 0 4px" }}
                  >
                    {DEV_MSG.WF_STEP_AFTER_LABEL}
                  </label>
                  <select
                    id="wf-step-after"
                    data-testid="developer-wf-step-after"
                    style={inputStyle}
                    value={stepAfter}
                    disabled={stepBusy}
                    onChange={(e) => setStepAfter(e.target.value)}
                    aria-label={DEV_MSG.WF_STEP_AFTER_LABEL}
                  >
                    {steps.map((s, i) =>
                      s.stepName ? (
                        <option key={`${s.stepName}-${i}`} value={s.stepName}>
                          {s.stepName}
                        </option>
                      ) : null,
                    )}
                  </select>
                </>
              ) : null}
              <label
                htmlFor="wf-step-roles"
                style={{ display: "block", margin: "8px 0 4px" }}
              >
                {DEV_MSG.WF_STEP_ROLES_LABEL}
              </label>
              <input
                id="wf-step-roles"
                data-testid="developer-wf-step-roles"
                style={inputStyle}
                value={stepRolesDraft}
                disabled={stepBusy}
                onChange={(e) => setStepRolesDraft(e.target.value)}
                aria-label={DEV_MSG.WF_STEP_ROLES_LABEL}
              />
              <div style={{ marginTop: "8px" }}>
                <button
                  type="button"
                  data-testid="developer-wf-step-save"
                  disabled={stepBusy || !stepNameDraft.trim()}
                  onClick={() => void handleStepSave()}
                  style={{
                    ...primaryBtnStyle,
                    background:
                      stepBusy || !stepNameDraft.trim()
                        ? catalogColors.disabled
                        : catalogColors.accent,
                    cursor:
                      stepBusy || !stepNameDraft.trim() ? "not-allowed" : "pointer",
                  }}
                >
                  {stepBusy
                    ? DEV_MSG.WF_STEP_SAVING
                    : editingStep
                      ? DEV_MSG.WF_STEP_SAVE
                      : DEV_MSG.WF_STEP_ADD}
                </button>
              </div>
            </div>
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
                role="status"
                aria-live="polite"
                data-testid="developer-wf-ct-notice"
                style={{ color: catalogColors.accent, marginBottom: "8px" }}
              >
                {notice}
              </div>
            ) : null}
            {!ctInputValid ? (
              <div
                role="alert"
                data-testid="developer-wf-ct-name-invalid"
                style={{ ...errorAlert, marginBottom: "8px" }}
              >
                {DEV_MSG.WF_CT_NAME_INVALID}
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
                      aria-label={`${DEV_MSG.CT_ASSOC_REMOVE} ${
                        ct.name || ct.label || `row ${i}`
                      }`}
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
                  {DEV_MSG.WF_CT_ADD_LABEL}
                </label>
                <input
                  id="wf-ct-add"
                  data-testid="developer-wf-ct-add-name"
                  style={inputStyle}
                  placeholder={DEV_MSG.WF_CT_NAME_PLACEHOLDER}
                  value={newCtName}
                  onChange={(e) => {
                    setNewCtName(e.target.value);
                    if (ctError === DEV_MSG.WF_CT_NAME_INVALID || ctError === DEV_MSG.WF_CT_DUP) {
                      setCtError(null);
                    }
                  }}
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
                disabled={!canAddContentType}
                onClick={addContentType}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor: !canAddContentType ? "not-allowed" : "pointer",
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
                : [DEV_MSG.WF_GAP_GRAPH]
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
