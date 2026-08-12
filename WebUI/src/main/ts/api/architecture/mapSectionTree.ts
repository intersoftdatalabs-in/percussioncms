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

/**
 * Pure helpers: section REST DTOs → Architecture nav tree model (#3095).
 *
 * <p>No I/O. Safe for Vitest without fetch. Cycle-safe for bad nested payloads.</p>
 */

import type { NavTreeNode, SectionNodeWire, SectionType } from "./types";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asNullableString(value: unknown): string | null {
  if (value == null) return null;
  if (typeof value === "string") {
    const t = value.trim();
    return t.length > 0 ? t : null;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  return null;
}

/**
 * Normalize a childNodes-like field into an array of wire nodes.
 * Handles single object, array, and empty/null.
 */
export function normalizeChildNodes(raw: unknown): SectionNodeWire[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw.filter(
      (n): n is SectionNodeWire => n != null && typeof n === "object",
    ) as SectionNodeWire[];
  }
  if (typeof raw === "object") {
    const obj = raw as Record<string, unknown>;
    if (obj.SectionNode != null) {
      return normalizeChildNodes(obj.SectionNode);
    }
    if (obj.childNodes != null) {
      return normalizeChildNodes(obj.childNodes);
    }
    return [raw as SectionNodeWire];
  }
  return [];
}

/**
 * Unwrap Jackson root name {@code SectionNode} (or plain object) to wire DTO.
 * Returns {@code null} when payload is empty / not an object.
 */
export function parseSectionNodePayload(
  payload: unknown,
): SectionNodeWire | null {
  if (payload == null) {
    return null;
  }
  if (typeof payload !== "object") {
    return null;
  }
  if (Array.isArray(payload)) {
    if (payload.length === 0) return null;
    return parseSectionNodePayload(payload[0]);
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.SectionNode as unknown) ??
    (obj.sectionNode as unknown) ??
    payload;
  const rec = asRecord(root);
  if (!rec) {
    return null;
  }
  if (
    rec.id == null &&
    rec.title == null &&
    (rec.SectionNode != null || rec.childNodes != null)
  ) {
    const nested = normalizeChildNodes(
      rec.SectionNode ?? rec.childNodes ?? rec.ChildNodes,
    );
    return nested[0] ?? (rec as SectionNodeWire);
  }
  return rec as SectionNodeWire;
}

function normalizeSectionType(raw: unknown): SectionType {
  if (typeof raw === "string" && raw.trim()) {
    return raw.trim().toLowerCase() as SectionType;
  }
  return "section";
}

/**
 * Map a wire {@link SectionNodeWire} to a {@link NavTreeNode} tree.
 * Skips cycles (same id already on path) and nodes without a usable id.
 */
export function mapSectionNodeToTree(
  wire: SectionNodeWire,
  seen: Set<string> = new Set(),
): NavTreeNode {
  const id =
    asNullableString(wire.id) ??
    `anon-${asNullableString(wire.title) ?? "section"}-${seen.size}`;
  const title =
    asNullableString(wire.title) ?? asNullableString(wire.folderPath) ?? id;
  const folderPath = asNullableString(wire.folderPath);
  const sectionType = normalizeSectionType(wire.sectionType);
  const requiresLogin = wire.requiresLogin === true;

  const children: NavTreeNode[] = [];
  if (!seen.has(id)) {
    const nextSeen = new Set(seen);
    nextSeen.add(id);
    const rawChildren = normalizeChildNodes(
      wire.childNodes ?? wire.ChildNodes ?? wire.SectionNode ?? null,
    );
    for (const child of rawChildren) {
      const childId = asNullableString(child.id);
      if (childId != null && nextSeen.has(childId)) {
        continue;
      }
      children.push(mapSectionNodeToTree(child, nextSeen));
    }
  }

  return {
    id,
    title,
    folderPath,
    sectionType,
    requiresLogin,
    children,
  };
}

/** Depth-first count of nodes including root. */
export function countNavTreeNodes(
  root: NavTreeNode | null | undefined,
): number {
  if (!root) return 0;
  let n = 1;
  for (const c of root.children) {
    n += countNavTreeNodes(c);
  }
  return n;
}

/** Flatten tree depth-first (root first). */
export function flattenNavTree(
  root: NavTreeNode | null | undefined,
): NavTreeNode[] {
  if (!root) return [];
  const out: NavTreeNode[] = [root];
  for (const c of root.children) {
    out.push(...flattenNavTree(c));
  }
  return out;
}

/** True when the node is a branch (has children). */
export function isNavBranch(node: NavTreeNode): boolean {
  return node.children.length > 0;
}

/**
 * Human-readable section type label for badges
 * ({@code perc.ui.architecture.modern} keys — #3098).
 */
export function sectionTypeLabel(sectionType: SectionType): string | null {
  // Lazy import avoided: pure English after @ is the runtime fallback when
  // TMX is not loaded; callers that need full i18n should prefer ARCH_MSG.
  switch (String(sectionType).toLowerCase()) {
    case "section":
      return null;
    case "sectionlink":
      return "Section link";
    case "externallink":
      return "External link";
    case "blog":
      return "Blog";
    default:
      return String(sectionType);
  }
}
