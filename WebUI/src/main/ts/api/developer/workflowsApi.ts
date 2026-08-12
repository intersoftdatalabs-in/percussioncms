/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { asJsonRecord } from "../jsonList";
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

function looksLikeWorkflowItem(obj: Record<string, unknown>): boolean {
  return (
    typeof obj.workflowName === "string" ||
    typeof obj.name === "string" ||
    Array.isArray(obj.workflowSteps) ||
    Array.isArray(obj.steps) ||
    typeof obj.defaultWorkflow === "boolean" ||
    typeof obj.isDefault === "boolean"
  );
}

function unwrapWorkflowWrapper(
  obj: Record<string, unknown>,
  depth: number,
): WorkflowDef[] | null {
  if (depth > 5) {
    return null;
  }
  for (const key of LIST_WRAPPER_KEYS) {
    const raw = obj[key];
    if (raw == null) {
      continue;
    }
    if (Array.isArray(raw)) {
      return raw as WorkflowDef[];
    }
    const nested = asJsonRecord(raw);
    if (!nested) {
      continue;
    }
    const deeper = unwrapWorkflowWrapper(nested, depth + 1);
    if (deeper) {
      return deeper;
    }
    if (looksLikeWorkflowItem(nested)) {
      return [nested as WorkflowDef];
    }
  }
  return null;
}

/**
 * Parse workflowmanagement metadata list.
 * Accepts a bare JSON array or a known list wrapper object. Unknown object shapes throw
 * so the UI surfaces an error instead of a silent empty catalog.
 *
 * <p>Shared by Developer catalog and Admin WorkflowSection — Jackson WRAP_ROOT often
 * returns {@code { "Workflow": [ ... ] }} (PSUiWorkflowList @JsonRootName). Nested
 * envelopes ({@code { Workflow: { Workflow: [...] } }}) must unwrap fully; treating
 * a wrapper object as the list causes {@code TypeError: e.map is not a function}
 * (#2959 / #3202).
 */
export function parseWorkflowList(payload: unknown): WorkflowDef[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload as WorkflowDef[];
  }
  const obj = asJsonRecord(payload);
  if (obj) {
    const unwrapped = unwrapWorkflowWrapper(obj, 0);
    if (unwrapped) {
      return unwrapped;
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
