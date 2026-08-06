/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { WorkflowDef } from "./types";

/** Honest design gaps for the Developer SY-04 browse surface (not full workflow admin). */
export const WORKFLOW_DESIGN_GAPS: string[] = [
  "Full workflow graph design is not exposed in the Developer catalog",
  "Workflow create / update / delete is not supported from this Developer surface",
  "Content type workflow association is edited on the content type detail panel",
];

/** Known envelope keys for list payloads (PSUiWorkflowList @JsonRootName + historical aliases). */
const LIST_WRAPPER_KEYS = [
  "Workflow",
  "workflow",
  "WorkflowList",
  "PSUiWorkflowList",
  "entries",
] as const;

/**
 * Parse workflowmanagement metadata list.
 * Accepts a bare JSON array or a known list wrapper object. Unknown object shapes throw
 * so the UI surfaces an error instead of a silent empty catalog.
 */
function parseWorkflowList(payload: unknown): WorkflowDef[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload as WorkflowDef[];
  }
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of LIST_WRAPPER_KEYS) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) {
        return raw as WorkflowDef[];
      }
      if (typeof raw === "object") {
        return [raw as WorkflowDef];
      }
    }
    throw new Error(
      "Unexpected workflow list payload (expected array or known Workflow wrapper)",
    );
  }
  throw new Error("Unexpected workflow list payload type");
}

function withGaps(w: WorkflowDef): WorkflowDef {
  return {
    ...w,
    designGaps:
      w.designGaps && w.designGaps.length > 0 ? w.designGaps : [...WORKFLOW_DESIGN_GAPS],
  };
}

/**
 * GET /services/workflowmanagement/workflows/metadata
 * (existing stepped-workflow catalog; Developer SY-04 browse surface)
 */
export async function listWorkflows(): Promise<WorkflowDef[]> {
  const payload = await get<unknown>(PATHS.WORKFLOW_METADATA);
  return parseWorkflowList(payload).map(withGaps);
}

/**
 * GET /services/workflowmanagement/workflows/{name}
 */
export async function getWorkflowDetail(name: string): Promise<WorkflowDef> {
  const key = encodeURIComponent(name);
  // PATHS.WORKFLOWS already ends with '/'
  const detail = await get<WorkflowDef | null | undefined>(`${PATHS.WORKFLOWS}${key}`);
  if (detail == null || typeof detail !== "object") {
    throw new Error("Workflow not found or empty response");
  }
  const workflowName =
    typeof detail.workflowName === "string" ? detail.workflowName.trim() : "";
  if (!workflowName) {
    throw new Error("Workflow response missing workflowName");
  }
  return withGaps(detail);
}
