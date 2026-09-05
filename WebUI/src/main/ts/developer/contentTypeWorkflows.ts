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
