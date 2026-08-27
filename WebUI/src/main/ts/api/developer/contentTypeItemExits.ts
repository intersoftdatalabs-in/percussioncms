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

import { asJacksonArray } from "./slotLists";
import { normalizeContentTypeDesignGaps } from "./contentTypeLists";
import type {
  ContentTypeItemExit,
  ContentTypeItemExitParam,
  ContentTypeItemExits,
} from "./types";

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeItemExits}. */
export const CONTENT_TYPE_ITEM_EXITS_ROOT = "ContentTypeItemExits";

export const ITEM_EXIT_LIST_KEYS = [
  "inputTranslations",
  "outputTranslations",
  "validations",
  "preExits",
  "postExits",
] as const;

export type ItemExitListKey = (typeof ITEM_EXIT_LIST_KEYS)[number];

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function normalizeParams(raw: unknown): ContentTypeItemExitParam[] {
  return asJacksonArray<ContentTypeItemExitParam>(
    raw,
    ["ContentTypeItemExitParam", "contentTypeItemExitParam", "parameters"],
    (o) => "name" in o || "value" in o,
  ).map((p) => {
    const out: ContentTypeItemExitParam = {};
    if (typeof p.name === "string") {
      out.name = p.name;
    }
    if (typeof p.value === "string") {
      out.value = p.value;
    }
    return out;
  });
}

function looksLikeItemExit(obj: Record<string, unknown>): boolean {
  return (
    "extension" in obj ||
    "name" in obj ||
    "parameters" in obj ||
    "summary" in obj ||
    "condition" in obj
  );
}

/** One list of exits — array, JAXB envelope, singleton, or empty bean. */
export function normalizeContentTypeItemExitsList(raw: unknown): ContentTypeItemExit[] {
  return asJacksonArray<ContentTypeItemExit>(
    raw,
    ["ContentTypeItemExit", "contentTypeItemExit"],
    looksLikeItemExit,
  ).map((exit) => {
    const out: ContentTypeItemExit = {};
    if (typeof exit.extension === "string") {
      out.extension = exit.extension;
    }
    if (typeof exit.name === "string") {
      out.name = exit.name;
    }
    out.parameters = normalizeParams(exit.parameters);
    if (typeof exit.condition === "string") {
      out.condition = exit.condition;
    }
    if (typeof exit.maxErrorsToStop === "number") {
      out.maxErrorsToStop = exit.maxErrorsToStop;
    }
    if (typeof exit.summary === "string") {
      out.summary = exit.summary;
    }
    return out;
  });
}

export function emptyContentTypeItemExits(): ContentTypeItemExits {
  return {
    inputTranslations: [],
    outputTranslations: [],
    validations: [],
    preExits: [],
    postExits: [],
  };
}

function normalizeMaxErrors(raw: unknown): number | undefined {
  if (typeof raw === "number" && Number.isFinite(raw)) {
    return raw;
  }
  if (typeof raw === "string" && raw.trim() !== "") {
    const n = Number(raw);
    if (Number.isFinite(n)) {
      return n;
    }
  }
  return undefined;
}

/**
 * Flatten GET/PUT {@code .../itemExits} JSON to {@link ContentTypeItemExits}.
 *
 * <p>Handles WRAP_ROOT {@code ContentTypeItemExits}, a flat body, JAXB list
 * envelopes, singleton objects, and empty-collection beans.
 */
export function unwrapContentTypeItemExits(payload: unknown): ContentTypeItemExits {
  const root = asRecord(payload);
  if (!root) {
    return emptyContentTypeItemExits();
  }
  const nested = asRecord(root[CONTENT_TYPE_ITEM_EXITS_ROOT] ?? root.contentTypeItemExits);
  const body = nested ?? root;
  return {
    inputTranslations: normalizeContentTypeItemExitsList(body.inputTranslations),
    outputTranslations: normalizeContentTypeItemExitsList(body.outputTranslations),
    validations: normalizeContentTypeItemExitsList(body.validations),
    preExits: normalizeContentTypeItemExitsList(body.preExits),
    postExits: normalizeContentTypeItemExitsList(body.postExits),
    maxErrorsToStopValidation: normalizeMaxErrors(body.maxErrorsToStopValidation),
    designGaps: normalizeContentTypeDesignGaps(body.designGaps),
  };
}

export function cloneContentTypeItemExits(env: ContentTypeItemExits): ContentTypeItemExits {
  const cloneList = (list: ContentTypeItemExit[] | undefined): ContentTypeItemExit[] =>
    (list ?? []).map((e) => ({
      extension: e.extension,
      name: e.name,
      parameters: (e.parameters ?? []).map((p) => ({ name: p.name, value: p.value })),
      condition: e.condition,
      maxErrorsToStop: e.maxErrorsToStop,
      summary: e.summary,
    }));
  return {
    inputTranslations: cloneList(env.inputTranslations),
    outputTranslations: cloneList(env.outputTranslations),
    validations: cloneList(env.validations),
    preExits: cloneList(env.preExits),
    postExits: cloneList(env.postExits),
    maxErrorsToStopValidation: env.maxErrorsToStopValidation,
    designGaps: env.designGaps ? [...env.designGaps] : undefined,
  };
}

function paramsEqual(
  a: ContentTypeItemExitParam[] | undefined,
  b: ContentTypeItemExitParam[] | undefined,
): boolean {
  const left = a ?? [];
  const right = b ?? [];
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if ((left[i].name || "") !== (right[i].name || "")) {
      return false;
    }
    if ((left[i].value || "") !== (right[i].value || "")) {
      return false;
    }
  }
  return true;
}

function exitKey(e: ContentTypeItemExit): string {
  return (e.extension || e.name || "").trim().toLowerCase();
}

function listsEqual(
  a: ContentTypeItemExit[] | undefined,
  b: ContentTypeItemExit[] | undefined,
): boolean {
  const left = a ?? [];
  const right = b ?? [];
  if (left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if (exitKey(left[i]) !== exitKey(right[i])) {
      return false;
    }
    if (!paramsEqual(left[i].parameters, right[i].parameters)) {
      return false;
    }
    if ((left[i].maxErrorsToStop ?? null) !== (right[i].maxErrorsToStop ?? null)) {
      return false;
    }
  }
  return true;
}

export function contentTypeItemExitsEqual(
  a: ContentTypeItemExits,
  b: ContentTypeItemExits,
): boolean {
  for (const key of ITEM_EXIT_LIST_KEYS) {
    if (!listsEqual(a[key], b[key])) {
      return false;
    }
  }
  return (a.maxErrorsToStopValidation ?? null) === (b.maxErrorsToStopValidation ?? null);
}

function toParamPayload(p: ContentTypeItemExitParam): ContentTypeItemExitParam {
  const out: ContentTypeItemExitParam = {};
  if (p.name) {
    out.name = p.name;
  }
  if (p.value != null && p.value !== "") {
    out.value = p.value;
  }
  return out;
}

function toExitPutPayload(exit: ContentTypeItemExit): ContentTypeItemExit {
  const out: ContentTypeItemExit = {
    extension: (exit.extension || exit.name || "").trim(),
  };
  const params = (exit.parameters ?? []).map(toParamPayload).filter((p) => p.value != null || p.name);
  if (params.length > 0) {
    out.parameters = params;
  }
  return out;
}

export function itemExitListsEqual(
  a: ContentTypeItemExit[] | undefined,
  b: ContentTypeItemExit[] | undefined,
): boolean {
  return listsEqual(a, b);
}

/**
 * PUT body lists (no designGaps). Always sends the three required translation /
 * validation lists. {@code preExits}/{@code postExits} are omitted unless
 * {@code includePipeExits} is true so live pipes that reject
 * {@code setInputDataExtensions} are left unchanged (REST omit = unchanged).
 */
export function toContentTypeItemExitsPutBody(
  env: ContentTypeItemExits,
  includePipeExits = false,
  includeMaxErrors = false,
): ContentTypeItemExits {
  const body: ContentTypeItemExits = {
    inputTranslations: (env.inputTranslations ?? []).map(toExitPutPayload),
    outputTranslations: (env.outputTranslations ?? []).map(toExitPutPayload),
    validations: (env.validations ?? []).map(toExitPutPayload),
  };
  if (includePipeExits) {
    body.preExits = (env.preExits ?? []).map(toExitPutPayload);
    body.postExits = (env.postExits ?? []).map(toExitPutPayload);
  }
  if (includeMaxErrors && env.maxErrorsToStopValidation != null) {
    body.maxErrorsToStopValidation = env.maxErrorsToStopValidation;
  }
  return body;
}

/**
 * Build the wire JSON body for {@code PUT .../itemExits} under
 * {@link CONTENT_TYPE_ITEM_EXITS_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapContentTypeItemExitsForWire(
  body: ContentTypeItemExits,
): Record<string, ContentTypeItemExits> {
  return { [CONTENT_TYPE_ITEM_EXITS_ROOT]: body };
}

export function itemExitDisplay(exit: ContentTypeItemExit): string {
  return (exit.extension || exit.name || exit.summary || "").trim();
}

export function listContainsExtension(
  list: ContentTypeItemExit[] | undefined,
  fqn: string,
): boolean {
  const key = fqn.trim().toLowerCase();
  if (!key) {
    return false;
  }
  return (list ?? []).some((e) => exitKey(e) === key);
}

export function addItemExit(
  env: ContentTypeItemExits,
  listKey: ItemExitListKey,
  fqn: string,
  paramValue?: string,
): ContentTypeItemExits {
  const extension = fqn.trim();
  if (!extension) {
    return env;
  }
  const list = env[listKey] ?? [];
  if (listContainsExtension(list, extension)) {
    return env;
  }
  const next: ContentTypeItemExit = { extension };
  const value = (paramValue ?? "").trim();
  if (value) {
    next.parameters = [{ value }];
  }
  return { ...env, [listKey]: [...list, next] };
}

export function removeItemExit(
  env: ContentTypeItemExits,
  listKey: ItemExitListKey,
  index: number,
): ContentTypeItemExits {
  const list = [...(env[listKey] ?? [])];
  if (index < 0 || index >= list.length) {
    return env;
  }
  list.splice(index, 1);
  return { ...env, [listKey]: list };
}
