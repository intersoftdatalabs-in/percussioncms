/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/**
 * Parse workflowmanagement metadata list.
 * Production payload is either a bare JSON array or a root object with a
 * {@code Workflow} property (PSUiWorkflowList / @JsonRootName("Workflow")).
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
    const raw = obj.Workflow ?? obj.workflow;
    if (Array.isArray(raw)) {
      return raw as WorkflowDef[];
    }
    if (raw != null && typeof raw === "object") {
      return [raw as WorkflowDef];
    }
    throw new Error(
      "Unexpected workflow list payload (expected array or Workflow wrapper)",
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
  const detail = await get<WorkflowDef>(`${PATHS.WORKFLOWS}${key}`);
  return withGaps(detail ?? {});
}
