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

import { get, put } from "../client";
import { asJsonRecord, asStringArray } from "../jsonList";
import { PATHS } from "../paths";

/** One assignable Developer API key scope. */
export type ApiKeyScope = {
  key: string;
  name: string;
  description?: string;
  group: string;
};

/** A least-privilege starter set for the scope editor. */
export type ApiKeyScopePreset = {
  id: string;
  name: string;
  description?: string;
  scopes: string[];
};

/** Scope set assigned to one named Developer API key. */
export type ApiKeyAssignment = {
  name: string;
  displayName?: string;
  scopes: string[];
  /** Preset id whose scope set exactly matches, or null for custom combinations. */
  presetId?: string | null;
};

/** Jackson / JAXB root for ApiKeyAssignment (UNWRAP_ROOT_VALUE on PUT). */
export const API_KEY_ASSIGNMENT_ROOT = "ApiKeyAssignment";

/** Key names start with a letter, then letters/digits/dash/underscore/dot (same as server). */
const KEY_NAME_PATTERN = /^[A-Za-z][A-Za-z0-9_.-]{0,63}$/;

/** Blank names are rejected before the request. Unsafe names never reach the wire. */
export function isApiKeyNameReady(name: string | null | undefined): boolean {
  if (typeof name !== "string") return false;
  return KEY_NAME_PATTERN.test(name.trim());
}

/** Normalize a key name for display/lookup; blank stays blank so callers can gate. */
export function normalizeApiKeyName(name: string | null | undefined): string {
  return typeof name === "string" ? name.trim() : "";
}

function normalizeScope(raw: unknown): ApiKeyScope | null {
  const obj = asJsonRecord(raw);
  if (!obj) return null;
  const key = typeof obj.key === "string" ? obj.key.trim() : "";
  if (!key) return null;
  const name = typeof obj.name === "string" && obj.name.trim() ? obj.name.trim() : key;
  const group =
    typeof obj.group === "string" && obj.group.trim() ? obj.group.trim() : "other";
  const scope: ApiKeyScope = { key, name, group };
  if (typeof obj.description === "string" && obj.description.trim()) {
    scope.description = obj.description.trim();
  }
  return scope;
}

function normalizePreset(raw: unknown): ApiKeyScopePreset | null {
  const obj = asJsonRecord(raw);
  if (!obj) return null;
  const id = typeof obj.id === "string" ? obj.id.trim() : "";
  if (!id) return null;
  const name = typeof obj.name === "string" && obj.name.trim() ? obj.name.trim() : id;
  const preset: ApiKeyScopePreset = { id, name, scopes: asStringArray(obj.scopes) };
  if (typeof obj.description === "string" && obj.description.trim()) {
    preset.description = obj.description.trim();
  }
  return preset;
}

function asItemArray(payload: unknown): unknown[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  const obj = asJsonRecord(payload);
  if (!obj) return [];
  for (const key of Object.keys(obj)) {
    const raw = obj[key];
    if (raw == null) continue;
    if (Array.isArray(raw)) return raw;
    // Jackson single-item: the list field is one object.
    if (typeof raw === "object") return [raw];
  }
  return [];
}

/**
 * Unwrap a scope-list payload. Live CXF uses {@code WRAP_ROOT_VALUE}, so the body may be
 * {@code {ApiKeyScope:[…]}}; a flat array is also accepted. Entries without a key are dropped.
 */
export function unwrapApiKeyScopeList(payload: unknown): ApiKeyScope[] {
  const root = asJsonRecord(payload);
  const body = root
    ? (asJsonRecord(root.ApiKeyScope) ?? asJsonRecord(root.apiKeyScope) ?? root)
    : payload;
  return asItemArray(Array.isArray(body) ? body : (body as unknown))
    .map(normalizeScope)
    .filter((s): s is ApiKeyScope => s != null);
}

/**
 * Unwrap a preset-list payload ({@code {ApiKeyScopePreset:[…]}} or flat). Entries without
 * an id are dropped.
 */
export function unwrapApiKeyScopePresetList(payload: unknown): ApiKeyScopePreset[] {
  const root = asJsonRecord(payload);
  const body = root
    ? (asJsonRecord(root.ApiKeyScopePreset) ??
      asJsonRecord(root.apiKeyScopePreset) ??
      root)
    : payload;
  return asItemArray(Array.isArray(body) ? body : (body as unknown))
    .map(normalizePreset)
    .filter((p): p is ApiKeyScopePreset => p != null);
}

function normalizeAssignment(raw: unknown): ApiKeyAssignment | null {
  const obj = asJsonRecord(raw);
  if (!obj) return null;
  const name = typeof obj.name === "string" ? obj.name.trim() : "";
  if (!name) return null;
  const out: ApiKeyAssignment = { name, scopes: asStringArray(obj.scopes) };
  if (typeof obj.displayName === "string" && obj.displayName.trim()) {
    out.displayName = obj.displayName.trim();
  }
  if (typeof obj.presetId === "string" && obj.presetId.trim()) {
    out.presetId = obj.presetId.trim();
  } else {
    out.presetId = null;
  }
  return out;
}

/**
 * Unwrap an assignment-list payload. A flat array, a wrapped list, or a single
 * assignment object are all accepted. Nameless entries are dropped.
 */
export function unwrapApiKeyAssignmentList(payload: unknown): ApiKeyAssignment[] {
  const root = asJsonRecord(payload);
  const body = root
    ? (asJsonRecord(root.ApiKeyAssignment) ??
      asJsonRecord(root.apiKeyAssignment) ??
      root)
    : payload;
  if (asJsonRecord(body)?.name) {
    const single = normalizeAssignment(body);
    return single ? [single] : [];
  }
  return asItemArray(Array.isArray(body) ? body : (body as unknown))
    .map(normalizeAssignment)
    .filter((a): a is ApiKeyAssignment => a != null);
}

/**
 * Unwrap a single assignment. Accepts {@code {ApiKeyAssignment:{…}}} and flat bodies.
 * Throws when the payload has no name so callers do not treat an error body as success.
 */
export function unwrapApiKeyAssignment(payload: unknown): ApiKeyAssignment {
  const root = asJsonRecord(payload);
  const body = root
    ? (asJsonRecord(root.ApiKeyAssignment) ?? asJsonRecord(root.apiKeyScopeAssignment) ?? root)
    : null;
  const out = normalizeAssignment(body);
  if (!out) {
    throw new Error("API key response missing name");
  }
  return out;
}

/** Wire JSON for PUT — a flat body fails JAXB root unwrap. */
export function wrapApiKeyAssignmentForWire(body: {
  name?: string;
  displayName?: string;
  scopes: string[];
}): Record<string, unknown> {
  return { [API_KEY_ASSIGNMENT_ROOT]: body };
}

/** GET /services/developer/apikeys/scopes */
export async function listApiKeyScopes(): Promise<ApiKeyScope[]> {
  return unwrapApiKeyScopeList(await get<unknown>(`${PATHS.API_KEYS}/scopes`));
}

/** GET /services/developer/apikeys/presets */
export async function listApiKeyScopePresets(): Promise<ApiKeyScopePreset[]> {
  return unwrapApiKeyScopePresetList(await get<unknown>(`${PATHS.API_KEYS}/presets`));
}

/** GET /services/developer/apikeys/keys */
export async function listApiKeyAssignments(): Promise<ApiKeyAssignment[]> {
  return unwrapApiKeyAssignmentList(await get<unknown>(`${PATHS.API_KEYS}/keys`));
}

/** Scope catalog URL for one assignment read. The name is encoded once. */
export function apiKeyAssignmentUrl(name: string): string {
  return `${PATHS.API_KEYS}/keys/${encodeURIComponent(name.trim())}`;
}

/**
 * GET /services/developer/apikeys/keys/{name}. HTTP 403 and 404 reject before
 * scopes are read, so an error body cannot be shown as an assignment.
 */
export async function loadApiKeyAssignment(name: string): Promise<ApiKeyAssignment> {
  const trimmed = normalizeApiKeyName(name);
  if (!isApiKeyNameReady(trimmed)) {
    throw new Error("API key name is required");
  }
  return unwrapApiKeyAssignment(await get<unknown>(apiKeyAssignmentUrl(trimmed)));
}

/** Scope set sent by the editor save. The server recomputes presetId. */
export type ApiKeyScopesWriteBody = {
  scopes: string[];
  displayName?: string;
};

/**
 * PUT /services/developer/apikeys/keys/{name} — Admin create-or-replace of one key's
 * scope set. HTTP 400 (unknown scope), 403, and 404 reject; this function does not
 * resolve for those.
 */
export async function saveApiKeyScopes(
  name: string,
  body: ApiKeyScopesWriteBody,
): Promise<ApiKeyAssignment> {
  const trimmed = normalizeApiKeyName(name);
  if (!isApiKeyNameReady(trimmed)) {
    throw new Error("API key name is required");
  }
  const wire: { name: string; displayName?: string; scopes: string[] } = {
    name: trimmed,
    scopes: [...body.scopes],
  };
  if (body.displayName != null && body.displayName.trim()) {
    wire.displayName = body.displayName.trim();
  }
  return unwrapApiKeyAssignment(
    await put<unknown>(apiKeyAssignmentUrl(trimmed), wrapApiKeyAssignmentForWire(wire)),
  );
}

/**
 * Preset whose scope set exactly equals {@code scopes} (order-insensitive), or null
 * for custom combinations. Mirrors the server recompute so the editor can label drafts.
 */
export function matchPresetForScopes(
  presets: readonly ApiKeyScopePreset[],
  scopes: readonly string[],
): string | null {
  const want = new Set(scopes.map((s) => s.trim()).filter(Boolean));
  for (const preset of presets) {
    const have = new Set(preset.scopes.map((s) => s.trim()).filter(Boolean));
    if (want.size === have.size && [...want].every((s) => have.has(s))) {
      return preset.id;
    }
  }
  return null;
}

/** Group consecutive catalog scopes for the editor. Unknown groups sort last. */
export const API_KEY_SCOPE_GROUP_ORDER = [
  "content",
  "assets",
  "design",
  "publishing",
  "sites",
  "admin",
] as const;

export function scopeGroupRank(group: string): number {
  const i = (API_KEY_SCOPE_GROUP_ORDER as readonly string[]).indexOf(group);
  return i < 0 ? API_KEY_SCOPE_GROUP_ORDER.length : i;
}
