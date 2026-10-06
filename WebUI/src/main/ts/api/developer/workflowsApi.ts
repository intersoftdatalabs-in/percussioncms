/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get, post, put, del } from "../client";
import { asJsonRecord } from "../jsonList";
import { PATHS } from "../paths";
import type {
  NamedObjectRef,
  WorkflowDef,
  WorkflowGraph,
  WorkflowStepRoleAssignment,
} from "./types";
import { unwrapNamedObjectRefList } from "./contentTypesApi";

/** Honest design gaps for the Developer SY-04 browse surface (not full workflow admin). */
export const WORKFLOW_DESIGN_GAPS: string[] = [
  "Ad-hoc type can be set to disabled, enabled, or anonymous for one Reader or Assignee role already assigned to a step. Inbox can be turned on or off for one Reader or Assignee role already assigned to a step. Notify can be turned on or off for one role already assigned to a step. Reader or Assignee can be set for one role already assigned to a step, and one Reader or Assignee role can be removed from a step. How many approvals one regular transition requires can be set on the graph. One more existing workflow role can be added to a regular transition that is already limited to specific roles, and that list can be cleared so every role may fire it again. The default transition, and repeated or system-field aging stay outside this surface.",
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
 * State names copied from {@code DefaultWorkflow.xml} on POST
 * /services/workflows. The create body is name (and optional description)
 * only; the stepped editor always loads this template. Keep the names in
 * lockstep with that file.
 */
export const DEFAULT_WORKFLOW_TEMPLATE_STEPS = [
  "Draft",
  "Review",
  "Pending",
  "Live",
  "Quick Edit",
  "Archive",
] as const;

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

/**
 * POST /services/workflows/{source}/copy — Admin. Copies states and transitions
 * onto a new unique name. Duplicate name is 409 and does not overwrite.
 */
export async function copyWorkflow(
  sourceName: string,
  body: WorkflowCreateBody,
): Promise<WorkflowCreateResult> {
  const key = encodeURIComponent(sourceName);
  const payload = await post<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/copy`,
    wrapWorkflowCreateForWire(body),
  );
  return parseWorkflowSummary(payload);
}

/** Writable fields for {@code PUT /services/workflows/{idOrName}} (slice 21 update). */
export type WorkflowUpdateBody = {
  /** Workflow name echoed from the path (must match). */
  name: string;
  /**
   * Replacement description. {@code undefined} is treated as no-op on the wire
   * (server treats a missing field as "leave unchanged"); empty string clears
   * the description.
   */
  description?: string;
};

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code WorkflowUpdate}. */
export const WORKFLOW_UPDATE_ROOT = "WorkflowUpdate";

/**
 * Build the wire JSON body for WorkflowsResource PUT under
 * {@link WORKFLOW_UPDATE_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapWorkflowUpdateForWire(
  body: WorkflowUpdateBody,
): Record<string, WorkflowUpdateBody> {
  return { [WORKFLOW_UPDATE_ROOT]: body };
}

/**
 * PUT /services/workflows/{idOrName} — Admin. Updates the description on an
 * existing workflow. Body {@code name} must match the path idOrName. The wire
 * body is wrapped under {@link WORKFLOW_UPDATE_ROOT}. Server returns the new
 * {@link WorkflowSummary}. 404 when the workflow is not found; 400 when the
 * body's name does not match the path or idOrName is invalid.
 */
export async function updateWorkflow(
  idOrName: string,
  body: WorkflowUpdateBody,
): Promise<WorkflowCreateResult> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}`,
    wrapWorkflowUpdateForWire(body),
  );
  return parseWorkflowSummary(payload);
}

/** New name for {@code POST /services/workflows/{idOrName}/rename} (slice 60). */
export type WorkflowRenameBody = {
  name: string;
};

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code WorkflowRename}. */
export const WORKFLOW_RENAME_ROOT = "WorkflowRename";

/**
 * Build the wire JSON body for WorkflowsResource rename under
 * {@link WORKFLOW_RENAME_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapWorkflowRenameForWire(
  body: WorkflowRenameBody,
): Record<string, WorkflowRenameBody> {
  return { [WORKFLOW_RENAME_ROOT]: body };
}

/**
 * POST /services/workflows/{idOrName}/rename — Admin. Renames one custom
 * workflow. Description, steps, transitions, and roles stay as they were.
 * Packaged and system-default workflows are 403. Duplicate name is 409.
 * Invalid name is 400. This does not use the description-only PUT.
 */
export async function renameWorkflow(
  idOrName: string,
  body: WorkflowRenameBody,
): Promise<WorkflowCreateResult> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/rename`,
    wrapWorkflowRenameForWire(body),
  );
  return parseWorkflowSummary(payload);
}

/**
 * POST /services/workflows/{idOrName}/default — Admin. Marks this workflow as
 * the single system default. A later call for another workflow replaces it.
 */
export async function setDefaultWorkflow(
  idOrName: string,
): Promise<WorkflowCreateResult> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(`${PATHS.WORKFLOWS_ASSOC}/${key}/default`);
  return parseWorkflowSummary(payload);
}

/**
 * DELETE /services/workflows/{idOrName} — Admin. Deletes the workflow via the
 * stepped-workflow editor. 404 when not found; 409 when the workflow is a
 * system workflow or still owns content items. Returns void on success.
 */
/**
 * Jackson/JAXB emits a one-element string list as a bare string. Joining that
 * value throws and unmounts the workflow graph (#5233).
 */
function parseWorkflowRoleNames(raw: unknown): string[] | undefined {
  if (typeof raw === "string") {
    const name = raw.trim();
    return name ? [name] : undefined;
  }
  if (!Array.isArray(raw)) {
    return undefined;
  }
  return raw.filter((item): item is string => typeof item === "string" && item.trim().length > 0);
}

function parseWorkflowEdges(raw: unknown): NonNullable<WorkflowGraph["edges"]> {
  const items = Array.isArray(raw) ? raw : [];
  return items.map((item) => {
    const edge = asJsonRecord(item);
    if (!edge) {
      return {};
    }
    const allowedRoles = parseWorkflowRoleNames(edge.allowedRoles);
    const next = { ...edge } as NonNullable<WorkflowGraph["edges"]>[number];
    if (allowedRoles) {
      next.allowedRoles = allowedRoles;
    } else {
      delete next.allowedRoles;
    }
    if (typeof next.agingType === "string" && next.agingType.trim()) {
      next.agingType = next.agingType.trim();
    } else {
      delete next.agingType;
    }
    if (typeof next.systemField === "string" && next.systemField.trim()) {
      next.systemField = next.systemField.trim();
    } else {
      delete next.systemField;
    }
    return next;
  });
}

export function parseWorkflowGraph(payload: unknown): WorkflowGraph {
  const raw = asJsonRecord(payload) ?? {};
  const wrapped = raw.WorkflowGraph;
  const obj =
    wrapped && typeof wrapped === "object" && !Array.isArray(wrapped)
      ? (wrapped as Record<string, unknown>)
      : raw;
  const nodesRaw = obj.nodes;
  const roles = parseWorkflowRoleNames(obj.roles);
  return {
    workflowName: typeof obj.workflowName === "string" ? obj.workflowName : undefined,
    packaged: obj.packaged === true,
    defaultWorkflow: obj.defaultWorkflow === true,
    ...(roles ? { roles } : {}),
    nodes: Array.isArray(nodesRaw) ? (nodesRaw as WorkflowGraph["nodes"]) : [],
    edges: parseWorkflowEdges(obj.edges),
  };
}

/** GET /services/workflows/{idOrName}/graph — Admin read-only state graph. */
export async function getWorkflowGraph(idOrName: string): Promise<WorkflowGraph> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.WORKFLOWS_ASSOC}/${key}/graph`);
  return parseWorkflowGraph(payload);
}

/** DELETE /services/workflows/{id}/transitions?from&label&to — one edge, steps stay. */
export function workflowTransitionDeletePath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("label", label);
  if (toStep && toStep.trim()) {
    q.set("to", toStep.trim());
  }
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions?${q.toString()}`;
}

export const WORKFLOW_TRANSITION_COMMENT_ROOT = "WorkflowTransitionComment";

/** PUT .../transitions/comment-required?from&label&to */
export function workflowTransitionCommentPath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("label", label);
  if (toStep && toStep.trim()) {
    q.set("to", toStep.trim());
  }
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions/comment-required?${q.toString()}`;
}

export async function updateTransitionCommentRequired(
  idOrName: string,
  fromStep: string,
  label: string,
  commentRequired: boolean,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await put<unknown>(
    workflowTransitionCommentPath(idOrName, fromStep, label, toStep),
    { [WORKFLOW_TRANSITION_COMMENT_ROOT]: { commentRequired } },
  );
  return parseWorkflowGraph(payload);
}

export const WORKFLOW_TRANSITION_APPROVALS_ROOT = "WorkflowTransitionApprovals";

/** PUT .../transitions/approvals-required?from&label&to */
export function workflowTransitionApprovalsPath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("label", label);
  if (toStep && toStep.trim()) {
    q.set("to", toStep.trim());
  }
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions/approvals-required?${q.toString()}`;
}

/** Whole number, including zero. Rejects blank, negatives, and fractions. */
export function isNonNegativeApprovalCount(raw: string | number | null | undefined): boolean {
  if (typeof raw === "number") {
    return Number.isSafeInteger(raw) && raw >= 0;
  }
  if (typeof raw !== "string") {
    return false;
  }
  return /^(0|[1-9]\d*)$/.test(raw.trim());
}

export async function updateTransitionApprovalsRequired(
  idOrName: string,
  fromStep: string,
  label: string,
  approvalsRequired: number,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await put<unknown>(
    workflowTransitionApprovalsPath(idOrName, fromStep, label, toStep),
    { [WORKFLOW_TRANSITION_APPROVALS_ROOT]: { approvalsRequired } },
  );
  return parseWorkflowGraph(payload);
}

export const WORKFLOW_TRANSITION_DEFAULT_ROOT = "WorkflowTransitionDefault";

/** PUT .../transitions/default?from&label&to */
export function workflowTransitionDefaultPath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("label", label);
  if (toStep && toStep.trim()) {
    q.set("to", toStep.trim());
  }
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions/default?${q.toString()}`;
}

/** Mark one regular transition as the default from its step. Does not clear a flag by itself. */
export async function markTransitionAsDefault(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await put<unknown>(
    workflowTransitionDefaultPath(idOrName, fromStep, label, toStep),
    { [WORKFLOW_TRANSITION_DEFAULT_ROOT]: { defaultTransition: true } },
  );
  return parseWorkflowGraph(payload);
}

export const WORKFLOW_TRANSITION_ALLOWED_ROLE_ROOT = "WorkflowTransitionAllowedRole";

/** PUT .../transitions/allowed-role?from&label&to */
export function workflowTransitionAllowedRolePath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("label", label);
  if (toStep && toStep.trim()) {
    q.set("to", toStep.trim());
  }
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions/allowed-role?${q.toString()}`;
}

/**
 * Restrict one allow-all regular transition to a single existing workflow role.
 * Does not add a second role or clear a restriction.
 */
export async function restrictTransitionToOneRole(
  idOrName: string,
  fromStep: string,
  label: string,
  roleName: string,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await put<unknown>(
    workflowTransitionAllowedRolePath(idOrName, fromStep, label, toStep),
    { [WORKFLOW_TRANSITION_ALLOWED_ROLE_ROOT]: { roleName } },
  );
  return parseWorkflowGraph(payload);
}

/** POST .../transitions/allowed-roles?from&label&to */
export function workflowTransitionAddAllowedRolePath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("label", label);
  if (toStep && toStep.trim()) {
    q.set("to", toStep.trim());
  }
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions/allowed-roles?${q.toString()}`;
}

/**
 * Add one existing workflow role to a regular transition that is already restricted.
 * Does not replace allow-all and does not clear the role list.
 */
export async function addTransitionAllowedRole(
  idOrName: string,
  fromStep: string,
  label: string,
  roleName: string,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await post<unknown>(
    workflowTransitionAddAllowedRolePath(idOrName, fromStep, label, toStep),
    { [WORKFLOW_TRANSITION_ALLOWED_ROLE_ROOT]: { roleName } },
  );
  return parseWorkflowGraph(payload);
}

/** DELETE .../transitions/allowed-roles?from&label&to */
export function workflowTransitionClearAllowedRolesPath(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): string {
  return workflowTransitionAddAllowedRolePath(idOrName, fromStep, label, toStep);
}

/**
 * Clear the role list on one regular transition that is already restricted so every role may
 * fire it. Does not append a role and does not replace allow-all with one role.
 */
export async function clearTransitionAllowedRoles(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await del<unknown>(
    workflowTransitionClearAllowedRolesPath(idOrName, fromStep, label, toStep),
  );
  return parseWorkflowGraph(payload);
}

export async function deleteWorkflowTransition(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep?: string,
): Promise<WorkflowGraph> {
  const payload = await del<unknown>(
    workflowTransitionDeletePath(idOrName, fromStep, label, toStep),
  );
  return parseWorkflowGraph(payload);
}

export const WORKFLOW_TRANSITION_WRITE_ROOT = "WorkflowTransitionWrite";

/** Writable fields for POST/PUT .../workflows/{id}/transitions (slice 31). */
export type WorkflowTransitionWriteBody = {
  from?: string;
  to: string;
  label: string;
};

export function wrapWorkflowTransitionWriteForWire(
  body: WorkflowTransitionWriteBody,
): Record<string, WorkflowTransitionWriteBody> {
  return { [WORKFLOW_TRANSITION_WRITE_ROOT]: body };
}

export const WORKFLOW_AGING_TRANSITION_WRITE_ROOT = "WorkflowAgingTransitionWrite";

/** Content-status date columns that can drive one system-field aging transition. */
export const WORKFLOW_AGING_SYSTEM_FIELDS = [
  "CONTENTSTARTDATE",
  "CONTENTEXPIRYDATE",
  "REMINDERDATE",
] as const;

/** True when {@code raw} is one of {@link WORKFLOW_AGING_SYSTEM_FIELDS}. */
export function isWorkflowAgingSystemField(raw: string | null | undefined): boolean {
  if (typeof raw !== "string") {
    return false;
  }
  const name = raw.trim().toUpperCase();
  return (WORKFLOW_AGING_SYSTEM_FIELDS as readonly string[]).includes(name);
}

/** Writable fields for POST .../workflows/{id}/aging-transitions (slice 57, 75, and 76). */
export type WorkflowAgingTransitionWriteBody = {
  from: string;
  to: string;
  /** Required for absolute and repeated. Omit for SYSTEM_FIELD. */
  intervalMinutes?: number;
  /** Omit or ABSOLUTE for absolute. REPEATED or SYSTEM_FIELD select the other kinds. */
  type?: "ABSOLUTE" | "REPEATED" | "SYSTEM_FIELD";
  /** Required when type is SYSTEM_FIELD. One of WORKFLOW_AGING_SYSTEM_FIELDS. */
  systemField?: string;
};

/** Positive whole minutes. Rejects blank, zero, negatives, and non-integers. */
export function isPositiveMinuteInterval(raw: string | number | null | undefined): boolean {
  if (typeof raw === "number") {
    return Number.isSafeInteger(raw) && raw > 0;
  }
  if (typeof raw !== "string") {
    return false;
  }
  const text = raw.trim();
  if (!/^[1-9]\d*$/.test(text)) {
    return false;
  }
  const minutes = Number(text);
  return Number.isSafeInteger(minutes) && minutes > 0;
}

export function wrapWorkflowAgingTransitionWriteForWire(
  body: WorkflowAgingTransitionWriteBody,
): Record<string, WorkflowAgingTransitionWriteBody> {
  return { [WORKFLOW_AGING_TRANSITION_WRITE_ROOT]: body };
}

/** POST /services/workflows/{id}/aging-transitions — one absolute, repeated, or system-field edge. */
export async function createWorkflowAgingTransition(
  idOrName: string,
  body: WorkflowAgingTransitionWriteBody,
): Promise<WorkflowGraph> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/aging-transitions`,
    wrapWorkflowAgingTransitionWriteForWire(body),
  );
  return parseWorkflowGraph(payload);
}

export const WORKFLOW_AGING_INTERVAL_WRITE_ROOT = "WorkflowAgingIntervalWrite";

/** Writable fields for PUT .../workflows/{id}/aging-transitions/interval (slice 58). */
export type WorkflowAgingIntervalWriteBody = {
  from: string;
  to: string;
  intervalMinutes: number;
  newIntervalMinutes: number;
};

export function wrapWorkflowAgingIntervalWriteForWire(
  body: WorkflowAgingIntervalWriteBody,
): Record<string, WorkflowAgingIntervalWriteBody> {
  return { [WORKFLOW_AGING_INTERVAL_WRITE_ROOT]: body };
}

/** PUT /services/workflows/{id}/aging-transitions/interval — change one absolute interval. */
export async function updateWorkflowAgingInterval(
  idOrName: string,
  body: WorkflowAgingIntervalWriteBody,
): Promise<WorkflowGraph> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/aging-transitions/interval`,
    wrapWorkflowAgingIntervalWriteForWire(body),
  );
  return parseWorkflowGraph(payload);
}

/** DELETE /services/workflows/{id}/aging-transitions?from&to&intervalMinutes */
export function workflowAgingDeletePath(
  idOrName: string,
  fromStep: string,
  toStep: string,
  intervalMinutes: number,
): string {
  const key = encodeURIComponent(idOrName);
  const q = new URLSearchParams();
  q.set("from", fromStep);
  q.set("to", toStep);
  q.set("intervalMinutes", String(intervalMinutes));
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/aging-transitions?${q.toString()}`;
}

/** DELETE one absolute aging edge. Does not delete a regular transition. */
export async function deleteWorkflowAgingTransition(
  idOrName: string,
  fromStep: string,
  toStep: string,
  intervalMinutes: number,
): Promise<WorkflowGraph> {
  const payload = await del<unknown>(
    workflowAgingDeletePath(idOrName, fromStep, toStep, intervalMinutes),
  );
  return parseWorkflowGraph(payload);
}

/** POST /services/workflows/{id}/transitions — one edge between existing steps. */
export async function createWorkflowTransition(
  idOrName: string,
  body: WorkflowTransitionWriteBody,
): Promise<WorkflowGraph> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/transitions`,
    wrapWorkflowTransitionWriteForWire(body),
  );
  return parseWorkflowGraph(payload);
}

/** PUT .../transitions?from&label&to — relabel or retarget one existing edge. */
export async function updateWorkflowTransition(
  idOrName: string,
  fromStep: string,
  label: string,
  toStep: string | undefined,
  body: WorkflowTransitionWriteBody,
): Promise<WorkflowGraph> {
  const payload = await put<unknown>(
    workflowTransitionDeletePath(idOrName, fromStep, label, toStep),
    wrapWorkflowTransitionWriteForWire(body),
  );
  return parseWorkflowGraph(payload);
}

/** DELETE /services/workflows/{id}/steps/{stepName} — one unreferenced step. */
export function workflowStepDeletePath(idOrName: string, stepName: string): string {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  return `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}`;
}

export async function deleteWorkflowStep(
  idOrName: string,
  stepName: string,
): Promise<WorkflowGraph> {
  const payload = await del<unknown>(workflowStepDeletePath(idOrName, stepName));
  return parseWorkflowGraph(payload);
}

export async function deleteWorkflow(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await del<void>(`${PATHS.WORKFLOWS_ASSOC}/${key}`);
}

/** Writable fields for {@code POST .../workflows/{id}/steps} (slice 30). */
export type WorkflowStepWriteBody = {
  name: string;
  afterStep?: string;
  roleNames?: string[];
};

export const WORKFLOW_STEP_WRITE_ROOT = "WorkflowStepWrite";

export function wrapWorkflowStepWriteForWire(
  body: WorkflowStepWriteBody,
): Record<string, WorkflowStepWriteBody> {
  return { [WORKFLOW_STEP_WRITE_ROOT]: body };
}

export function isValidWorkflowStepName(name: string | undefined | null): boolean {
  return isValidWorkflowName(name);
}

export async function createWorkflowStep(
  idOrName: string,
  body: WorkflowStepWriteBody,
): Promise<WorkflowCreateResult> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps`,
    wrapWorkflowStepWriteForWire(body),
  );
  return parseWorkflowSummary(payload);
}

export async function updateWorkflowStep(
  idOrName: string,
  stepName: string,
  body: WorkflowStepWriteBody,
): Promise<WorkflowCreateResult> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}`,
    wrapWorkflowStepWriteForWire(body),
  );
  return parseWorkflowSummary(payload);
}

export const WORKFLOW_STEP_ROLE_ASSIGNMENT_ROOT = "WorkflowStepRoleAssignmentWrite";
export const WORKFLOW_STEP_ROLE_ASSIGNMENT_LIST_ROOT = "WorkflowStepRoleAssignmentList";

export type WorkflowStepRoleAssignmentWriteBody = {
  roleName: string;
  assignmentType: string;
};

export function wrapWorkflowStepRoleAssignmentForWire(
  body: WorkflowStepRoleAssignmentWriteBody,
): Record<string, WorkflowStepRoleAssignmentWriteBody> {
  return { [WORKFLOW_STEP_ROLE_ASSIGNMENT_ROOT]: body };
}

/** Unwrap Jackson root {@code WorkflowStepRoleAssignmentList}. */
export function parseStepRoleAssignments(payload: unknown): WorkflowStepRoleAssignment[] {
  const raw = asJsonRecord(payload) ?? {};
  const wrapped = raw[WORKFLOW_STEP_ROLE_ASSIGNMENT_LIST_ROOT];
  const obj =
    wrapped && typeof wrapped === "object" && !Array.isArray(wrapped)
      ? (wrapped as Record<string, unknown>)
      : raw;
  const rows = obj.assignments ?? obj.Assignments;
  if (!Array.isArray(rows)) {
    return [];
  }
  return rows as WorkflowStepRoleAssignment[];
}

/** GET /services/workflows/{id}/role-assignments — stored types, including Reader. */
export async function listStepRoleAssignments(
  idOrName: string,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.WORKFLOWS_ASSOC}/${key}/role-assignments`);
  return parseStepRoleAssignments(payload);
}

/**
 * PUT /services/workflows/{id}/steps/{step}/role-assignment — Reader or Assignee
 * for one role already on the step. Does not rename the step or replace the role list.
 */
export async function setStepRoleAssignment(
  idOrName: string,
  stepName: string,
  body: WorkflowStepRoleAssignmentWriteBody,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}/role-assignment`,
    wrapWorkflowStepRoleAssignmentForWire(body),
  );
  return parseStepRoleAssignments(payload);
}

export const WORKFLOW_STEP_ROLE_NOTIFY_ROOT = "WorkflowStepRoleNotifyWrite";

export type WorkflowStepRoleNotifyWriteBody = {
  roleName: string;
  notify: boolean;
};

export function wrapWorkflowStepRoleNotifyForWire(
  body: WorkflowStepRoleNotifyWriteBody,
): Record<string, WorkflowStepRoleNotifyWriteBody> {
  return { [WORKFLOW_STEP_ROLE_NOTIFY_ROOT]: body };
}

/**
 * PUT /services/workflows/{id}/steps/{step}/role-notify — ISNOTIFYON for one
 * role already on the step. Not PUT role-assignment. Inbox is not sent.
 */
export async function setStepRoleNotify(
  idOrName: string,
  stepName: string,
  body: WorkflowStepRoleNotifyWriteBody,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}/role-notify`,
    wrapWorkflowStepRoleNotifyForWire(body),
  );
  return parseStepRoleAssignments(payload);
}

export const WORKFLOW_STEP_ROLE_INBOX_ROOT = "WorkflowStepRoleInboxWrite";

export type WorkflowStepRoleInboxWriteBody = {
  roleName: string;
  inbox: boolean;
};

export function wrapWorkflowStepRoleInboxForWire(
  body: WorkflowStepRoleInboxWriteBody,
): Record<string, WorkflowStepRoleInboxWriteBody> {
  return { [WORKFLOW_STEP_ROLE_INBOX_ROOT]: body };
}

/**
 * PUT /services/workflows/{id}/steps/{step}/role-inbox — SHOWININBOX for one
 * Reader or Assignee already on the step. Not PUT role-notify. Assignment type
 * and notify are not sent.
 */
export async function setStepRoleInbox(
  idOrName: string,
  stepName: string,
  body: WorkflowStepRoleInboxWriteBody,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}/role-inbox`,
    wrapWorkflowStepRoleInboxForWire(body),
  );
  return parseStepRoleAssignments(payload);
}

export const WORKFLOW_STEP_ROLE_ADHOC_ROOT = "WorkflowStepRoleAdhocWrite";

export type WorkflowStepRoleAdhocWriteBody = {
  roleName: string;
  adhocType: string;
};

export function wrapWorkflowStepRoleAdhocForWire(
  body: WorkflowStepRoleAdhocWriteBody,
): Record<string, WorkflowStepRoleAdhocWriteBody> {
  return { [WORKFLOW_STEP_ROLE_ADHOC_ROOT]: body };
}

/**
 * PUT /services/workflows/{id}/steps/{step}/role-adhoc — adhoc type for one
 * Reader or Assignee already on the step. Not PUT role-inbox. Assignment type,
 * notify, and inbox are not sent.
 */
export async function setStepRoleAdhoc(
  idOrName: string,
  stepName: string,
  body: WorkflowStepRoleAdhocWriteBody,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const payload = await put<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}/role-adhoc`,
    wrapWorkflowStepRoleAdhocForWire(body),
  );
  return parseStepRoleAssignments(payload);
}

export const WORKFLOW_STEP_ROLE_ADD_ROOT = "WorkflowStepRoleAdd";

export type WorkflowStepRoleAddBody = {
  roleName: string;
  assignmentType: string;
};

export function wrapWorkflowStepRoleAddForWire(
  body: WorkflowStepRoleAddBody,
): Record<string, WorkflowStepRoleAddBody> {
  return { [WORKFLOW_STEP_ROLE_ADD_ROOT]: body };
}

/**
 * POST /services/workflows/{id}/steps/{step}/roles — add one existing workflow
 * role onto one step. Not PUT role-assignment. Notify and inbox are not sent.
 */
export async function addStepRole(
  idOrName: string,
  stepName: string,
  body: WorkflowStepRoleAddBody,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const payload = await post<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}/roles`,
    wrapWorkflowStepRoleAddForWire(body),
  );
  return parseStepRoleAssignments(payload);
}

/**
 * DELETE /services/workflows/{id}/steps/{step}/roles/{role} — remove one
 * Reader or Assignee role from one step. Not PUT role-assignment.
 */
export async function removeStepRole(
  idOrName: string,
  stepName: string,
  roleName: string,
): Promise<WorkflowStepRoleAssignment[]> {
  const key = encodeURIComponent(idOrName);
  const step = encodeURIComponent(stepName);
  const role = encodeURIComponent(roleName.trim());
  const payload = await del<unknown>(
    `${PATHS.WORKFLOWS_ASSOC}/${key}/steps/${step}/roles/${role}`,
  );
  return parseStepRoleAssignments(payload);
}
