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

import { normalizeNamedObjectRefs } from "../api/developer/contentTypeLists";
import type { ContentTypeWorkflowsBody } from "../api/developer/contentTypesApi";
import type { NamedObjectRef } from "../api/developer/types";

/** Stable React/list key for a NamedObjectRef (name, then guid, then index). */
export function refKey(r: NamedObjectRef, index: number): string {
  if (r.name) return `name:${r.name}`;
  if (r.guid?.stringValue) return `guid:${r.guid.stringValue}`;
  if (r.guid?.uuid != null) return `uuid:${r.guid.uuid}`;
  return `idx:${index}`;
}

export function cloneNamedObjectRefs(list: unknown): NamedObjectRef[] {
  return normalizeNamedObjectRefs(list).map((r) => ({
    name: r.name,
    label: r.label,
    isDefault: r.isDefault,
    guid: r.guid ? { ...r.guid } : undefined,
  }));
}

export function namedObjectRefsEqual(a: NamedObjectRef[], b: NamedObjectRef[]): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (refKey(a[i], i) !== refKey(b[i], i)) return false;
    if (!!a[i].isDefault !== !!b[i].isDefault) return false;
  }
  return true;
}

/**
 * Draft order for the allowed-workflow edge list. The server GET sorts refs
 * by label case-insensitively ({@code ContentTypeAdaptor.loadWorkflows});
 * the draft mirrors that order after every mutation so a
 * remove-then-re-associate round-trip restores the exact row sequence.
 */
export function compareWorkflowRefs(a: NamedObjectRef, b: NamedObjectRef): number {
  const ka = (a.label || a.name || "").toLowerCase();
  const kb = (b.label || b.name || "").toLowerCase();
  if (ka < kb) return -1;
  if (ka > kb) return 1;
  return 0;
}

/** Return a sorted copy of the draft edge list (server GET order). */
export function sortWorkflowRefs(list: NamedObjectRef[]): NamedObjectRef[] {
  return [...list].sort(compareWorkflowRefs);
}

/**
 * Stage one workflow edge onto the draft in server GET order (#6289).
 *
 * The staged label is the workflow name: the server reports label == name
 * for workflow refs ({@code PSWorkflow.getLabel()} returns the name), so
 * staging the catalog description instead would make a
 * remove-then-re-associate round-trip render different row text.
 *
 * When the staged name is the persisted default, the default flag is
 * restored (and cleared on the other rows): {@code removeWorkflow} promotes
 * the next row when the default edge is removed, so without this step a
 * remove-then-re-associate round-trip of the default edge would leave the
 * draft dirty and the save button enabled.
 */
export function stageWorkflowRef(
  prev: NamedObjectRef[],
  name: string,
  persistedDefaultName: string | undefined,
): NamedObjectRef[] {
  const trimmed = (name || "").trim();
  if (!trimmed) return prev;
  if (
    prev.some((w) => (w.name || "").toLowerCase() === trimmed.toLowerCase())
  ) {
    return prev;
  }
  const wantsDefault = persistedDefaultName
    ? trimmed.toLowerCase() === persistedDefaultName.toLowerCase()
    : prev.length === 0;
  return sortWorkflowRefs([
    ...prev.map((w) => (wantsDefault ? { ...w, isDefault: false } : w)),
    { name: trimmed, label: trimmed, isDefault: wantsDefault },
  ]);
}

/**
 * Align isDefault flags with server defaultWorkflow (or first row when missing).
 */
export function withDefaultWorkflowFlags(
  list: unknown,
  defaultWorkflow?: NamedObjectRef | null,
): NamedObjectRef[] {
  const wfs = cloneNamedObjectRefs(list);
  if (defaultWorkflow) {
    const defKey = refKey(defaultWorkflow, -1);
    for (const w of wfs) {
      w.isDefault = refKey(w, -1) === defKey || w.name === defaultWorkflow.name;
    }
  }
  if (wfs.length > 0 && !wfs.some((w) => w.isDefault)) {
    wfs[0] = { ...wfs[0], isDefault: true };
  }
  return wfs;
}

/** Strip UI-only fields for CD-08 PUT .../allowedWorkflows. */
export function toNamedObjectRefPayload(list: NamedObjectRef[]): NamedObjectRef[] {
  return list.map((r) => {
    const out: NamedObjectRef = {};
    if (r.name) out.name = r.name;
    if (r.guid?.stringValue || r.guid?.uuid != null) {
      out.guid = {};
      if (r.guid.stringValue) out.guid.stringValue = r.guid.stringValue;
      if (r.guid.uuid != null) out.guid.uuid = r.guid.uuid;
    }
    if (r.isDefault) out.isDefault = true;
    return out;
  });
}

/**
 * Full-replace body for {@code PUT .../allowedWorkflows}. Empty list clears.
 * Omits {@code defaultWorkflow} when the set is empty.
 */
export function buildAllowedWorkflowsReplaceBody(
  workflows: NamedObjectRef[],
): ContentTypeWorkflowsBody {
  const allowedWorkflows = toNamedObjectRefPayload(workflows);
  const def = workflows.find((w) => w.isDefault) || workflows[0];
  if (!def) {
    return { allowedWorkflows };
  }
  return {
    allowedWorkflows,
    defaultWorkflow: toNamedObjectRefPayload([def])[0],
  };
}
