/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get, put } from "../client";
import { asJsonRecord } from "../jsonList";
import { PATHS } from "../paths";
import type { NamedObjectRef, WorkflowDef } from "./types";
import { unwrapNamedObjectRefList } from "./contentTypesApi";

/** Honest design gaps for the Developer SY-04 browse surface (not full workflow admin). */
export const WORKFLOW_DESIGN_GAPS: string[] = [
  "Full workflow graph design is not exposed in the Developer catalog",
  "Workflow create / update / delete is not supported from this Developer surface",
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

/** Same envelope keys as the list plus the JAXB/Jackson class alias. */
const DETAIL_WRAPPER_KEYS = [
  "Workflow",
  "workflow",
  "PSUiWorkflow",
  "uiWorkflow",
] as const;

/**
 * Resolve a catalog name from {@code workflowName} or the Jackson {@code name} alias.
 */
function resolveWorkflowName(obj: Record<string, unknown>): string {
  if (typeof obj.workflowName === "string" && obj.workflowName.trim()) {
    return obj.workflowName.trim();
  }
  if (typeof obj.name === "string" && obj.name.trim()) {
    return obj.name.trim();
  }
  return "";
}

/**
 * Unwrap Jackson WRAP_ROOT / nested {@code Workflow} envelopes for a single detail
 * payload. Mirrors {@link unwrapWorkflowWrapper} but returns one object, not a list.
 */
function unwrapWorkflowDetailObject(
  obj: Record<string, unknown>,
  depth: number,
): Record<string, unknown> | null {
  if (depth > 5) {
    return null;
  }
  for (const key of DETAIL_WRAPPER_KEYS) {
    const raw = obj[key];
    if (raw == null) {
      continue;
    }
    const nested = asJsonRecord(raw);
    if (nested) {
      const deeper = unwrapWorkflowDetailObject(nested, depth + 1);
      if (deeper) {
        return deeper;
      }
    }
    if (Array.isArray(raw) && raw.length > 0) {
      const first = asJsonRecord(raw[0]);
      if (first) {
        const deeper = unwrapWorkflowDetailObject(first, depth + 1);
        if (deeper) {
          return deeper;
        }
      }
    }
  }
  if (looksLikeWorkflowItem(obj) || resolveWorkflowName(obj)) {
    return obj;
  }
  return null;
}

/**
 * Parse GET /services/workflowmanagement/workflows/{name}.
 *
 * Accepts a flat PSUiWorkflow body, Jackson WRAP_ROOT {@code { Workflow: { … } }},
 * nested wrappers, a one-item array, and the {@code name} alias when
 * {@code workflowName} is absent (#3562 / #2640).
 */
export function parseWorkflowDetail(payload: unknown): WorkflowDef {
  if (payload == null) {
    throw new Error("Workflow not found or empty response");
  }
  if (Array.isArray(payload)) {
    if (payload.length === 0) {
      throw new Error("Workflow not found or empty response");
    }
    return parseWorkflowDetail(payload[0]);
  }
  const obj = asJsonRecord(payload);
  if (!obj) {
    throw new Error("Workflow not found or empty response");
  }
  const unwrapped = unwrapWorkflowDetailObject(obj, 0);
  if (!unwrapped) {
    throw new Error("Workflow response missing workflowName");
  }
  const workflowName = resolveWorkflowName(unwrapped);
  if (!workflowName) {
    throw new Error("Workflow response missing workflowName");
  }
  const stepsRaw = unwrapped.workflowSteps ?? unwrapped.steps;
  const detail: WorkflowDef = {
    ...(unwrapped as WorkflowDef),
    workflowName,
  };
  if (Array.isArray(stepsRaw)) {
    detail.workflowSteps = stepsRaw as WorkflowDef["workflowSteps"];
  }
  return detail;
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
  const payload = await get<unknown>(`${PATHS.WORKFLOWS}${key}`);
  return withGaps(parseWorkflowDetail(payload));
}

/** Wire body for {@code PUT .../workflows/{id}/allowedContentTypes} (Jackson root {@code WorkflowContentTypes}). */
export type WorkflowContentTypesBody = {
  allowedContentTypes: NamedObjectRef[];
};

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code WorkflowContentTypes}. */
export const WORKFLOW_CONTENT_TYPES_ROOT = "WorkflowContentTypes";

/**
 * Build the wire JSON body for {@code PUT .../allowedContentTypes} under
 * {@link WORKFLOW_CONTENT_TYPES_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapWorkflowContentTypesForWire(
  body: WorkflowContentTypesBody,
): Record<string, WorkflowContentTypesBody> {
  return { [WORKFLOW_CONTENT_TYPES_ROOT]: body };
}

/**
 * GET /services/workflows/{idOrName}/allowedContentTypes — SY-06 Admin read.
 * No design lock required. Empty list means none.
 */
export async function getWorkflowAllowedContentTypes(
  idOrName: string,
): Promise<NamedObjectRef[]> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/allowedContentTypes`,
  );
  return unwrapNamedObjectRefList(payload);
}

/**
 * PUT /services/workflows/{idOrName}/allowedContentTypes — SY-06 Admin full replace.
 *
 * <p>Admin only. Server acquires and releases a design lock per affected content
 * type (unlike CD-08 CT→workflow PUT, which requires a pre-held CT lock). Empty
 * {@code allowedContentTypes} clears associations for this workflow. Response is
 * the new {@link NamedObjectRef} list.
 */
export async function setWorkflowAllowedContentTypes(
  idOrName: string,
  body: WorkflowContentTypesBody,
): Promise<NamedObjectRef[]> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/allowedContentTypes`,
    wrapWorkflowContentTypesForWire(body),
  );
  return unwrapNamedObjectRefList(payload);
}
