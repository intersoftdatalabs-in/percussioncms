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
 * Architecture folder ACL helpers (#3588 / parent #3092).
 *
 * <p>Operators add/remove section-folder principals via Explorer
 * {@code FolderSecurityPanel} + {@code pathApi.saveFolderProperties}.
 * The section id is a navon; the folder id is resolved from
 * {@code NavTreeNode.folderPath} the same way Explorer resolves
 * security-panel folder ids.</p>
 */

import { canEditSectionProperties } from "../api/architecture/sectionMutations";
import type {
  NavTreeNode,
  SiteSectionPropertiesWire,
} from "../api/architecture/types";
import type {
  FolderAccessLevel,
  PSFolderPermission,
  PSFolderProperties,
  PSPrincipal,
} from "../api/contentExplorer/types";
import {
  findChildren,
  findItemByPath,
  isPathItemLookupPath,
} from "../api/contentExplorer/pathApi";
import { isFolderIdLookupPath } from "../contentExplorer/folderPath";

export interface SectionFolderPathOptions {
  /** Selected site name — used when the navon omits {@code folderPath}. */
  siteName?: string | null;
  /** True when {@code node} is the site navigation root. */
  isRoot?: boolean;
}

/**
 * Folder path for Folder ACL. Prefers the navon {@code folderPath}.
 * Site-root navons from Page-site create often omit the path — fall back
 * to {@code //Sites/{siteName}} (same as create-parent path).
 */
export function resolveSectionFolderPath(
  node: NavTreeNode | null,
  options?: SectionFolderPathOptions,
): string | null {
  if (!node) {
    return null;
  }
  const fromNode = node.folderPath != null ? node.folderPath.trim() : "";
  if (fromNode.length > 0 && isFolderIdLookupPath(fromNode)) {
    return fromNode;
  }
  const site = options?.siteName != null ? options.siteName.trim() : "";
  if (options?.isRoot && site.length > 0) {
    return `//Sites/${site}`;
  }
  return null;
}

/**
 * Regular section (including site root) with a resolvable folder path.
 * Blogs, section links, and external links stay on other editors.
 */
export function canEditFolderAcl(
  node: NavTreeNode | null,
  options?: SectionFolderPathOptions,
): boolean {
  if (!canEditSectionProperties(node)) {
    return false;
  }
  return isFolderIdLookupPath(resolveSectionFolderPath(node, options));
}

/**
 * Resolve the CMS folder content id for {@code FolderSecurityPanel}.
 * Skips root / blank paths so pathmanagement is not probed with
 * {@code GET …/path/item/} (#3468).
 */
/**
 * folderProperties rejects site-name slugs ({@code Invalid id}).
 * Real folder content ids are numeric or {@code host-type-uuid} GUIDs.
 */
export function isFolderPropertiesId(id: string | null | undefined): boolean {
  const t = id != null ? String(id).trim() : "";
  if (!t) {
    return false;
  }
  if (/^\d+$/.test(t)) {
    return true;
  }
  return t.includes("-");
}

function lastCmsPathSegment(path: string): {
  parent: string;
  name: string;
} | null {
  const parts = path
    .replace(/\\/g, "/")
    .replace(/\/+$/, "")
    .split("/")
    .filter((p) => p.length > 0 && p !== "");
  if (parts.length < 2) {
    return null;
  }
  return {
    parent: `/${parts.slice(0, -1).join("/")}`,
    name: parts[parts.length - 1]!,
  };
}

async function resolveFolderIdFromParentChildren(
  path: string,
): Promise<string | undefined> {
  const parts = lastCmsPathSegment(path);
  if (!parts) {
    return undefined;
  }
  try {
    const kids = await findChildren(parts.parent);
    const match = kids.find((k) => {
      const kn = k.name != null ? String(k.name).trim() : "";
      const kp = String(k.path ?? k.folderPath ?? "")
        .replace(/\\/g, "/")
        .replace(/\/+$/, "");
      return kn === parts.name || kp.endsWith(`/${parts.name}`);
    });
    const id = match?.id != null ? String(match.id).trim() : "";
    return isFolderPropertiesId(id) ? id : undefined;
  } catch {
    return undefined;
  }
}

export async function defaultResolveSectionFolderId(
  path: string,
): Promise<string | undefined> {
  if (!isFolderIdLookupPath(path) || !isPathItemLookupPath(path)) {
    return undefined;
  }
  try {
    const item = await findItemByPath(path);
    const id = item?.id != null ? String(item.id).trim() : "";
    if (isFolderPropertiesId(id)) {
      return id;
    }
  } catch {
    // Fall through to parent children (site PathItem often omits id).
  }
  return resolveFolderIdFromParentChildren(path);
}

/**
 * Resolve the selected section folder id, or {@code undefined} when
 * the path is not a folder-id lookup path.
 */
function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function principalsFromWire(value: unknown): PSPrincipal[] {
  if (value == null) {
    return [];
  }
  const list = Array.isArray(value)
    ? value
    : asRecord(value)?.Principal
      ? ([] as unknown[]).concat(
          (asRecord(value) as Record<string, unknown>).Principal as unknown,
        )
      : [value];
  const out: PSPrincipal[] = [];
  for (const raw of list) {
    const rec = asRecord(raw);
    if (!rec) continue;
    const name = rec.name != null ? String(rec.name).trim() : "";
    if (!name) continue;
    const type = String(rec.type ?? "USER").toUpperCase();
    out.push({
      type: type === "ROLE" ? "ROLE" : "USER",
      name,
    });
  }
  return out;
}

/**
 * Map {@code SiteSectionProperties.folderPermission} onto Explorer
 * {@link PSFolderProperties} so FolderSecurityPanel can edit when
 * pathmanagement has no folder GUID (Page-site roots).
 */
export function folderPropertiesFromSection(
  props: SiteSectionPropertiesWire,
): PSFolderProperties {
  const raw = asRecord(props.folderPermission) ?? {};
  const accessRaw = String(raw.accessLevel ?? "WRITE").toUpperCase();
  const accessLevel: FolderAccessLevel =
    accessRaw === "ADMIN" ||
    accessRaw === "WRITE" ||
    accessRaw === "READ" ||
    accessRaw === "VIEW"
      ? accessRaw
      : "WRITE";
  const permission: PSFolderPermission = {
    accessLevel,
    adminPrincipals: principalsFromWire(raw.adminPrincipals),
    writePrincipals: principalsFromWire(raw.writePrincipals),
    readPrincipals: principalsFromWire(raw.readPrincipals),
    viewPrincipals: principalsFromWire(raw.viewPrincipals),
  };
  return {
    id: props.id,
    name: props.folderName || props.title || props.id,
    permission,
  };
}

export async function resolveSectionFolderId(
  folderPath: string | null | undefined,
  resolveFolderId: (
    path: string,
  ) => Promise<string | undefined> = defaultResolveSectionFolderId,
): Promise<string | undefined> {
  if (!isFolderIdLookupPath(folderPath)) {
    return undefined;
  }
  return resolveFolderId(String(folderPath));
}
