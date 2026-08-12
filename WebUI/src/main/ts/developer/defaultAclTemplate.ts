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

import type { ObjectAclEntry } from "../api/developer/types";

/**
 * Preference name for the Developer default object ACL template (Workbench
 * Security preferences parity — FR §5.4 #7 / §5.9 #2).
 *
 * Stored via existing {@code /services/preferences} REST (no dedicated ACL-prefs API).
 */
export const DEFAULT_ACL_TEMPLATE_PREF_NAME = "developer.defaultObjectAclTemplate";

/** Preference category used when saving (matches PreferenceResource default). */
export const DEFAULT_ACL_TEMPLATE_PREF_CATEGORY = "sys_preferences";

/** Preference context: private per-user (matches PreferenceResource default). */
export const DEFAULT_ACL_TEMPLATE_PREF_CONTEXT = "private";

/** Principal types supported in the default ACL template (REST PrincipalTypes). */
export const DEFAULT_ACL_TEMPLATE_ENTRY_TYPES = [
  "ROLE",
  "USER",
  "COMMUNITY",
  "GROUP",
] as const;

export type DefaultAclTemplateEntryType =
  (typeof DEFAULT_ACL_TEMPLATE_ENTRY_TYPES)[number];

export type DefaultAclTemplateEntry = {
  name: string;
  type: DefaultAclTemplateEntryType;
  permissions: string[];
};

export type DefaultAclTemplate = {
  version: 1;
  entries: DefaultAclTemplateEntry[];
};

/**
 * Workbench / design-WS seed when no user preference is stored.
 *
 * Mirrors {@code PSSystemDesignWs.configureDefaultAclEntries} names/types and
 * product-common AnyCommunity runtime visibility (CD-19 / FR §5.4).
 */
export function systemDefaultAclTemplate(): DefaultAclTemplate {
  return {
    version: 1,
    entries: [
      {
        name: "Default",
        type: "USER",
        permissions: ["READ", "UPDATE", "DELETE", "OWNER"],
      },
      {
        name: "AnyCommunity",
        type: "COMMUNITY",
        permissions: ["RUNTIME_VISIBLE"],
      },
    ],
  };
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function normalizeType(raw: unknown): DefaultAclTemplateEntryType | null {
  if (typeof raw !== "string") return null;
  const t = raw.trim().toUpperCase();
  if ((DEFAULT_ACL_TEMPLATE_ENTRY_TYPES as readonly string[]).includes(t)) {
    return t as DefaultAclTemplateEntryType;
  }
  return null;
}

function normalizePermissions(raw: unknown): string[] {
  if (!Array.isArray(raw)) return [];
  const out: string[] = [];
  for (const p of raw) {
    if (typeof p !== "string") continue;
    const s = p.trim().toUpperCase();
    if (!s) continue;
    if (!out.includes(s)) out.push(s);
  }
  return out;
}

function normalizeEntry(raw: unknown): DefaultAclTemplateEntry | null {
  const o = asRecord(raw);
  if (!o) return null;
  const name = typeof o.name === "string" ? o.name.trim() : "";
  if (!name) return null;
  const type = normalizeType(o.type);
  if (!type) return null;
  return {
    name,
    type,
    permissions: normalizePermissions(o.permissions),
  };
}

/**
 * Parse a preference value into a template. Returns null when empty/invalid
 * (caller should fall back to {@link systemDefaultAclTemplate}).
 *
 * <p>Accepts a JSON string or an already-parsed object. Some GET paths leave
 * {@code UserPreference.value} as a parsed object (or {@code String(obj)} would
 * become {@code [object Object]} and drop Runtime visibility on reload).
 */
export function parseDefaultAclTemplate(
  raw: string | Record<string, unknown> | null | undefined,
): DefaultAclTemplate | null {
  if (raw == null) return null;
  let data: unknown;
  if (typeof raw === "object") {
    data = raw;
  } else {
    if (!String(raw).trim()) return null;
    try {
      data = JSON.parse(String(raw));
    } catch {
      return null;
    }
  }
  const o = asRecord(data);
  if (!o) return null;
  const list = Array.isArray(o.entries) ? o.entries : null;
  if (!list) return null;
  const entries: DefaultAclTemplateEntry[] = [];
  for (const item of list) {
    const e = normalizeEntry(item);
    if (e) entries.push(e);
  }
  return { version: 1, entries };
}

/** Serialize template for preference storage. */
export function serializeDefaultAclTemplate(template: DefaultAclTemplate): string {
  const entries = (template.entries ?? [])
    .map((e) => normalizeEntry(e))
    .filter((e): e is DefaultAclTemplateEntry => e != null);
  return JSON.stringify({ version: 1, entries });
}

/** Clone a template (deep enough for UI draft state). */
export function cloneDefaultAclTemplate(
  template: DefaultAclTemplate,
): DefaultAclTemplate {
  return {
    version: 1,
    entries: (template.entries ?? []).map((e) => ({
      name: e.name,
      type: e.type,
      permissions: [...(e.permissions ?? [])],
    })),
  };
}

/** Structural equality for dirty detection. */
export function defaultAclTemplatesEqual(
  a: DefaultAclTemplate,
  b: DefaultAclTemplate,
): boolean {
  return serializeDefaultAclTemplate(a) === serializeDefaultAclTemplate(b);
}

function entryPrincipalName(e: ObjectAclEntry): string {
  return (e.name || e.principal?.name || e.type?.name || "").trim();
}

/**
 * Convert a template row to a REST-shaped {@link ObjectAclEntry} suitable for
 * bulk save (no server ids).
 */
export function templateEntryToObjectAclEntry(
  entry: DefaultAclTemplateEntry,
  aclId?: number,
): ObjectAclEntry {
  const name = entry.name.trim();
  const type = entry.type;
  return {
    name,
    aclId,
    principal: { name, type },
    type: { type, name },
    permissions: (entry.permissions ?? []).map((p) => ({ permission: p })),
  };
}

/**
 * Merge template entries onto an existing ACL entry list (create-time apply).
 *
 * <p>Existing principals (by case-insensitive name) are kept as-is so the
 * owner from {@code POST /acls} is preserved. Template rows only add missing
 * names. Returns a new array; does not mutate inputs.
 */
export function mergeTemplateOntoAclEntries(
  existing: readonly ObjectAclEntry[],
  template: DefaultAclTemplate,
  aclId?: number,
): { entries: ObjectAclEntry[]; added: number } {
  const base: ObjectAclEntry[] = existing.map((e) => {
    const copy: ObjectAclEntry = {
      ...e,
      principal: e.principal ? { ...e.principal } : undefined,
      type: e.type ? { ...e.type } : undefined,
      permissions: Array.isArray(e.permissions)
        ? e.permissions.map((p) => ({ ...p }))
        : e.permissions,
    };
    return copy;
  });
  const names = new Set(
    base.map((e) => entryPrincipalName(e).toLowerCase()).filter(Boolean),
  );
  let added = 0;
  for (const te of template.entries ?? []) {
    const n = te.name.trim();
    if (!n) continue;
    const key = n.toLowerCase();
    if (names.has(key)) continue;
    base.push(templateEntryToObjectAclEntry(te, aclId));
    names.add(key);
    added += 1;
  }
  return { entries: base, added };
}

/**
 * Whether apply-on-create should attempt a bulk save after merge.
 */
export function shouldApplyDefaultAclTemplate(
  template: DefaultAclTemplate | null | undefined,
): boolean {
  return !!template && Array.isArray(template.entries) && template.entries.length > 0;
}
