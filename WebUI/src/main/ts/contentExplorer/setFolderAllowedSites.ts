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
 * Content → Set allowed publish sites (#5132 / parent #4530).
 *
 * <p>Stores {@code sys_allowed_sites} on one selected folder. Zero checked
 * sites, when confirmed, clears the property so assets may publish to all
 * sites. Success is reported only after a properties refresh shows that list.
 * Cancel, pages, assets, empty selection, multi-select, and HTTP 400/403/409
 * must not be reported as a saved list.</p>
 */

import { isApiError, post } from "../api/client";
import {
  listFolderAllowedSitesCatalog,
  type FolderAllowedSiteChoice,
} from "../api/contentExplorer/folderAllowedSitesApi";
import {
  folderProperties,
  unwrapFolderProperties,
  wrapFolderProperties,
} from "../api/contentExplorer/pathApi";
import { PATHS } from "../api/paths";
import type { PSFolderProperties, PSPathItem } from "../api/contentExplorer/types";
import { resolvePublishKind } from "./itemPublish";
import { isFolder } from "./selection";

export type SetFolderAllowedSitesBlockReason =
  | "empty"
  | "page"
  | "asset"
  | "not-folder"
  | "multi"
  | "no-id";

export type SetFolderAllowedSitesSelection =
  | { status: "blocked"; reason: SetFolderAllowedSitesBlockReason; name: string }
  | { status: "ready"; folderId: string; name: string };

export function classifySetFolderAllowedSitesSelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetFolderAllowedSitesSelection {
  if (input.selectedCount >= 2) {
    return { status: "blocked", reason: "multi", name: "" };
  }
  const item = input.item ?? null;
  if (!item) {
    return { status: "blocked", reason: "empty", name: "" };
  }
  const name = (item.name ?? item.path ?? "").trim();
  if (!isFolder(item)) {
    const kind = resolvePublishKind(item);
    if (kind === "page") {
      return { status: "blocked", reason: "page", name };
    }
    if (kind === "asset") {
      return { status: "blocked", reason: "asset", name };
    }
    return { status: "blocked", reason: "not-folder", name };
  }
  const folderId = item.id == null ? "" : String(item.id).trim();
  if (!folderId) {
    return { status: "blocked", reason: "no-id", name };
  }
  return { status: "ready", folderId, name };
}

export type AllowedSitesParse =
  | { ok: true; canonical: string }
  | { ok: false };

/** Canonical comma-separated site ids. Blank and null are an empty list. */
export function canonicalAllowedSites(raw: unknown): AllowedSitesParse {
  if (raw == null) {
    return { ok: true, canonical: "" };
  }
  if (typeof raw === "number") {
    if (!Number.isSafeInteger(raw) || raw <= 0) {
      return { ok: false };
    }
    return { ok: true, canonical: String(raw) };
  }
  if (Array.isArray(raw)) {
    return canonicalAllowedSites(raw.map((value) => String(value ?? "")).join(","));
  }
  if (typeof raw !== "string") {
    return { ok: false };
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    return { ok: true, canonical: "" };
  }
  const ids: number[] = [];
  for (const part of trimmed.split(",")) {
    const token = part.trim();
    if (!/^[0-9]+$/.test(token)) {
      return { ok: false };
    }
    const id = Number(token);
    if (!Number.isSafeInteger(id) || id <= 0) {
      return { ok: false };
    }
    if (!ids.includes(id)) {
      ids.push(id);
    }
  }
  ids.sort((left, right) => left - right);
  return { ok: true, canonical: ids.join(",") };
}

export function folderAllowedSitesText(props: PSFolderProperties | null | undefined): string {
  const parsed = canonicalAllowedSites(props?.allowedSites);
  return parsed.ok ? parsed.canonical : "";
}

export type AllowedSitesChoiceGate =
  | { ok: true; canonical: string }
  | { ok: false; reason: "unchanged" | "forbidden" | "invalid" };

/** Client gate before saveFolderProperties. The server repeats the catalog check. */
export function gateExplorerFolderAllowedSitesChange(input: {
  selectedIds: readonly string[];
  currentSites: string;
  allowedIds: readonly string[];
}): AllowedSitesChoiceGate {
  const selected = canonicalAllowedSites(input.selectedIds.join(","));
  if (!selected.ok) {
    return { ok: false, reason: "invalid" };
  }
  const current = canonicalAllowedSites(input.currentSites);
  if (current.ok && selected.canonical === current.canonical) {
    return { ok: false, reason: "unchanged" };
  }
  if (!selected.canonical) {
    return { ok: true, canonical: "" };
  }
  const allowed = new Set<string>();
  for (const id of input.allowedIds) {
    const parsed = canonicalAllowedSites(id);
    if (parsed.ok && parsed.canonical) {
      allowed.add(parsed.canonical);
    }
  }
  for (const id of selected.canonical.split(",")) {
    if (!allowed.has(id)) {
      return { ok: false, reason: "forbidden" };
    }
  }
  return { ok: true, canonical: selected.canonical };
}

export type SetFolderAllowedSitesCatalog =
  | {
      status: "ready";
      folderId: string;
      name: string;
      currentSites: string;
      choices: FolderAllowedSiteChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderAllowedSitesBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetFolderAllowedSitesCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderAllowedSitesCatalog;
}): Promise<SetFolderAllowedSitesCatalog> {
  const classified = classifySetFolderAllowedSitesSelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderAllowedSitesCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(classified.folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.id ?? "").trim().length > 0 && String(row.name ?? "").trim(),
    );
    const currentSites = folderAllowedSitesText(props);
    if (choices.length === 0 && !currentSites) {
      return { status: "none" };
    }
    return {
      status: "ready",
      folderId: classified.folderId,
      name: classified.name,
      currentSites,
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderAllowedSitesSave =
  | { status: "saved"; allowedSites: string; allowedSiteNames: string; cleared: boolean }
  | { status: "gate"; reason: "unchanged" | "forbidden" | "invalid" }
  | { status: "mismatch" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

async function postFolderProperties(props: PSFolderProperties): Promise<void> {
  const body = wrapFolderProperties({
    ...props,
    permission: props.permission ?? { accessLevel: "ADMIN" },
  });
  await post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, body);
}

function namesFor(canonical: string, choices: readonly FolderAllowedSiteChoice[]): string {
  if (!canonical) {
    return "";
  }
  return canonical
    .split(",")
    .map((id) => choices.find((row) => row.id === id)?.name || id)
    .join(", ");
}

export async function saveSetFolderAllowedSites(input: {
  folderId: string;
  props: PSFolderProperties;
  selectedIds: readonly string[];
  currentSites: string;
  allowedIds: readonly string[];
  choices?: readonly FolderAllowedSiteChoice[];
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
}): Promise<SetFolderAllowedSitesSave> {
  const gate = gateExplorerFolderAllowedSitesChange({
    selectedIds: input.selectedIds,
    currentSites: input.currentSites,
    allowedIds: input.allowedIds,
  });
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const save = input.save ?? postFolderProperties;
  const reload = input.reload ?? folderProperties;
  const next: PSFolderProperties = {
    ...input.props,
    id: input.props.id || input.folderId,
    allowedSites: gate.canonical,
  };
  try {
    await save(next);
    const again = await reload(input.folderId);
    const stored = canonicalAllowedSites(again?.allowedSites);
    if (!stored.ok || stored.canonical !== gate.canonical) {
      return { status: "mismatch" };
    }
    return {
      status: "saved",
      allowedSites: stored.canonical,
      allowedSiteNames: namesFor(stored.canonical, input.choices ?? []),
      cleared: stored.canonical.length === 0,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

function httpBucket(err: unknown): 400 | 403 | 409 | "other" {
  if (isApiError(err)) {
    if (err.status === 400 || err.status === 403 || err.status === 409) {
      return err.status;
    }
  }
  return "other";
}

/** Exposed for tests that feed a raw GET body through the same unwrap as the client. */
export function readReloadedAllowedSites(data: unknown): string {
  return folderAllowedSitesText(unwrapFolderProperties(data) ?? undefined);
}
