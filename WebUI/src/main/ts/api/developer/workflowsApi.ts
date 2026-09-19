/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get, post, put } from "../client";
import { asJsonRecord } from "../jsonList";
import { PATHS } from "../paths";
import type { NamedObjectRef, WorkflowDef } from "./types";
import { unwrapNamedObjectRefList } from "./contentTypesApi";

/** Honest design gaps for the Developer SY-04 browse surface (not full workflow admin). */
export const WORKFLOW_DESIGN_GAPS: string[] = [
  "Full workflow graph design is not exposed in the Developer catalog",
  "Workflow update / delete is not supported from this Developer surface",
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

/**
 * Writable fields for {@code POST /services/workflows} (slice 21 create).
 * Name is required; unique (case-insensitive); letters, digits, underscore,
 * hyphen, space; max 50 chars. States, transitions, and roles come from the
 * product base-workflow template; full graph design stays outside this surface.
 */
export type WorkflowCreateBody = {
  name: string;
  description?: string;
};

/** Created-workflow summary from {@code POST /services/workflows}. */
export type WorkflowCreateResult = {
  workflowName: string;
  workflowDescription?: string;
  defaultWorkflow?: boolean;
};

/** Workflow-admin name rules mirrored from the stepped editor (create path). */
export const WORKFLOW_NAME_PATTERN = /^[\s\w-]+$/;

/** Max workflow name length enforced by the stepped workflow editor. */
export const WORKFLOW_NAME_MAX_LENGTH = 50;

/** Trim; empty when missing. */
export function normalizeWorkflowName(name: string | undefined | null): string {
  if (name == null) {
    return "";
  }
  return name.trim();
}

/**
 * True when the name is a legal REST create key: non-blank, max 50 chars, no
 * wildcards, and only workflow-admin name characters.
 */
export function isValidWorkflowName(name: string | undefined | null): boolean {
  const n = normalizeWorkflowName(name);
  if (!n) {
    return false;
  }
  if (n.length > WORKFLOW_NAME_MAX_LENGTH) {
    return false;
  }
  if (n.includes("*") || n.includes("%")) {
    return false;
  }
  return WORKFLOW_NAME_PATTERN.test(n);
}

/** Create Save is enabled when the workflow name is valid. Description is optional. */
export function isWorkflowCreateReady(opts: { name: string }): boolean {
  return isValidWorkflowName(opts.name);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code WorkflowCreate}. */
export const WORKFLOW_CREATE_ROOT = "WorkflowCreate";

/**
 * Build the wire JSON body for WorkflowsResource POST under
 * {@link WORKFLOW_CREATE_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapWorkflowCreateForWire(
  body: WorkflowCreateBody,
): Record<string, WorkflowCreateBody> {
  return { [WORKFLOW_CREATE_ROOT]: body };
}

/** Envelope keys for a single create response (flat body or Jackson WRAP_ROOT). */
const SUMMARY_WRAPPER_KEYS = ["WorkflowSummary", "workflowSummary", "Workflow"] as const;

/**
 * Parse {@code POST /services/workflows} responses. Accepts a flat summary
 * body or a Jackson root-wrapped envelope. Throws when the workflow name is
 * missing so the UI surfaces an error instead of a silent no-op.
 */
export function parseWorkflowSummary(payload: unknown): WorkflowCreateResult {
  if (payload == null) {
    throw new Error("Workflow create returned an empty response");
  }
  const records: Record<string, unknown>[] = [];
  if (Array.isArray(payload)) {
    if (payload.length === 0) {
      throw new Error("Workflow create returned an empty response");
    }
    const first = asJsonRecord(payload[0]);
    if (first) {
      records.push(first);
    }
  } else {
    const obj = asJsonRecord(payload);
    if (!obj) {
      throw new Error("Workflow create returned an unexpected response");
    }
    records.push(obj);
    for (const key of SUMMARY_WRAPPER_KEYS) {
      const nested = asJsonRecord(obj[key]);
      if (nested) {
        records.push(nested);
      }
    }
  }
  for (const rec of records) {
    const name = rec.workflowName;
    if (typeof name === "string" && name.trim()) {
      return {
        workflowName: name.trim(),
        workflowDescription:
          typeof rec.workflowDescription === "string" ? rec.workflowDescription : undefined,
        defaultWorkflow:
          typeof rec.defaultWorkflow === "boolean" ? rec.defaultWorkflow : undefined,
      };
    }
  }
  throw new Error("Workflow create response missing workflowName");
}

/**
 * POST /services/workflows — Admin. Creates and persists a workflow (base
 * template states/transitions/roles; description stored when given).
 * Duplicate name is 409; blank/too-long/invalid name is 400; non-Admin is 403.
 */
export async function createWorkflow(
  body: WorkflowCreateBody,
): Promise<WorkflowCreateResult> {
  const payload = await post<unknown>(
    PATHS.WORKFLOWS_ASSOC,
    wrapWorkflowCreateForWire(body),
  );
  return parseWorkflowSummary(payload);
}
