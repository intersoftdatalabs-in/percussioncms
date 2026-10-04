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
 * Content → Set allowed publish sites for one selected folder (#5132 /
 * parent #4530) and for every checked folder (#5181).
 *
 * <p>Stores {@code sys_allowed_sites} on each target folder, then reads that
 * folder's properties again. Zero checked sites, when confirmed, clears the
 * property so assets may publish to all sites. Success for a folder is
 * reported only after that refresh shows the same id list (or no list, for a
 * clear). Cancel, pages, assets, empty selection, and HTTP 400/403/409 must
 * not be reported as a saved list for the whole selection. A page or asset in
 * a multi-selection is named and not written. Single-item still refuses a
 * multi-count so callers that have not opted into
 * {@link planSetFolderAllowedSitesMulti} keep #5132.</p>
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
import { message } from "../i18n/message";
import { resolvePublishKind } from "./itemPublish";
import { EXPLORER_MSG } from "./messages";
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

export interface SetFolderAllowedSitesTarget {
  folderId: string;
  name: string;
}

export type SetFolderAllowedSitesMultiPlan =
  | { status: "blocked"; reason: SetFolderAllowedSitesBlockReason; name: string }
  | {
      status: "ready";
      targets: SetFolderAllowedSitesTarget[];
      skippedPageNames: string[];
      skippedAssetNames: string[];
      skippedOtherNames: string[];
      noIdNames: string[];
    };

function pushUnique(list: string[], seen: Set<string>, label: string): void {
  const name = label.trim();
  if (!name || seen.has(name)) {
    return;
  }
  seen.add(name);
  list.push(name);
}

interface FolderAllowedSitesBuckets {
  targets: SetFolderAllowedSitesTarget[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
  noIdNames: string[];
}

function emptyBuckets(): FolderAllowedSitesBuckets {
  return {
    targets: [],
    skippedPageNames: [],
    skippedAssetNames: [],
    skippedOtherNames: [],
    noIdNames: [],
  };
}

function rememberNonFolder(
  item: PSPathItem,
  buckets: FolderAllowedSitesBuckets,
  seenPages: Set<string>,
  seenAssets: Set<string>,
  seenOthers: Set<string>,
): void {
  const name = (item.name ?? item.path ?? "").trim();
  const kind = resolvePublishKind(item);
  if (kind === "page") {
    pushUnique(buckets.skippedPageNames, seenPages, name);
    return;
  }
  if (kind === "asset") {
    pushUnique(buckets.skippedAssetNames, seenAssets, name);
    return;
  }
  pushUnique(buckets.skippedOtherNames, seenOthers, name);
}

function rememberFolder(
  item: PSPathItem,
  buckets: FolderAllowedSitesBuckets,
  seenIds: Set<string>,
  seenNoId: Set<string>,
): void {
  const name = (item.name ?? item.path ?? "").trim();
  const folderId = item.id == null ? "" : String(item.id).trim();
  if (!folderId) {
    pushUnique(buckets.noIdNames, seenNoId, name);
    return;
  }
  if (seenIds.has(folderId)) {
    return;
  }
  seenIds.add(folderId);
  buckets.targets.push({ folderId, name: name || folderId });
}

function blockedWithoutFolderTargets(
  buckets: FolderAllowedSitesBuckets,
): SetFolderAllowedSitesMultiPlan {
  const pages = buckets.skippedPageNames;
  const assets = buckets.skippedAssetNames;
  const others = buckets.skippedOtherNames;
  if (pages.length > 0 && assets.length === 0 && others.length === 0) {
    return { status: "blocked", reason: "page", name: pages.join(", ") };
  }
  if (assets.length > 0 && pages.length === 0 && others.length === 0) {
    return { status: "blocked", reason: "asset", name: assets.join(", ") };
  }
  if (pages.length > 0) {
    return {
      status: "blocked",
      reason: "page",
      name: [...pages, ...assets, ...others].join(", "),
    };
  }
  if (assets.length > 0) {
    return {
      status: "blocked",
      reason: "asset",
      name: [...assets, ...others].join(", "),
    };
  }
  if (buckets.noIdNames.length > 0) {
    return { status: "blocked", reason: "no-id", name: buckets.noIdNames.join(", ") };
  }
  return { status: "blocked", reason: "not-folder", name: others.join(", ") };
}

/**
 * Folders are written. Pages and assets are named and skipped. Duplicate
 * folder ids are written once. An empty check set, or a set with no folder,
 * does not open a save. Callers that have not opted into this plan still
 * use {@link classifySetFolderAllowedSitesSelection}, which blocks {@code multi}.
 */
export function planSetFolderAllowedSitesMulti(
  items: readonly PSPathItem[],
): SetFolderAllowedSitesMultiPlan {
  if (items.length === 0) {
    return { status: "blocked", reason: "empty", name: "" };
  }
  const buckets = emptyBuckets();
  const seenIds = new Set<string>();
  const seenPages = new Set<string>();
  const seenAssets = new Set<string>();
  const seenOthers = new Set<string>();
  const seenNoId = new Set<string>();
  for (const item of items) {
    if (!isFolder(item)) {
      rememberNonFolder(item, buckets, seenPages, seenAssets, seenOthers);
    } else {
      rememberFolder(item, buckets, seenIds, seenNoId);
    }
  }
  if (buckets.targets.length === 0) {
    return blockedWithoutFolderTargets(buckets);
  }
  return { status: "ready", ...buckets };
}

export type SetFolderAllowedSitesMultiCatalog =
  | {
      status: "ready";
      targets: SetFolderAllowedSitesTarget[];
      skippedPageNames: string[];
      skippedAssetNames: string[];
      skippedOtherNames: string[];
      noIdNames: string[];
      currentSites: string;
      choices: FolderAllowedSiteChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderAllowedSitesBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

/** Shared catalog plus the first folder's current site list. Does not save. */
export async function loadSetFolderAllowedSitesMultiCatalog(input: {
  items: readonly PSPathItem[];
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderAllowedSitesCatalog;
}): Promise<SetFolderAllowedSitesMultiCatalog> {
  const plan = planSetFolderAllowedSitesMulti(input.items);
  if (plan.status === "blocked") {
    return plan;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderAllowedSitesCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(plan.targets[0].folderId),
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
      targets: plan.targets,
      skippedPageNames: plan.skippedPageNames,
      skippedAssetNames: plan.skippedAssetNames,
      skippedOtherNames: plan.skippedOtherNames,
      noIdNames: plan.noIdNames,
      currentSites,
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderAllowedSitesFailureCode =
  | 400
  | 403
  | 409
  | "other"
  | "forbidden"
  | "invalid"
  | "mismatch";

export interface SetFolderAllowedSitesItemFailure {
  folderId: string;
  name: string;
  http: SetFolderAllowedSitesFailureCode;
}

export interface SetFolderAllowedSitesMultiSave {
  status: "saved" | "partial" | "failed" | "unchanged";
  allowedSites: string;
  allowedSiteNames: string;
  cleared: boolean;
  saved: SetFolderAllowedSitesTarget[];
  unchanged: SetFolderAllowedSitesTarget[];
  failures: SetFolderAllowedSitesItemFailure[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
}

export interface SetFolderAllowedSitesShown {
  folderId: string;
  name: string;
  allowedSites: string;
  allowedSiteNames: string;
  cleared: boolean;
}

/**
 * One site list (or a clear) on every target folder. Each save reads that
 * folder, posts its properties with {@code allowedSites}, and reads them
 * again. {@code onFolderSaved} runs only after that refresh shows the same
 * list, and never for a failure. Pages and assets are not posted.
 */
export async function saveSetFolderAllowedSitesOnSelection(input: {
  targets: readonly SetFolderAllowedSitesTarget[];
  skippedPageNames?: readonly string[];
  skippedAssetNames?: readonly string[];
  skippedOtherNames?: readonly string[];
  noIdNames?: readonly string[];
  selectedIds: readonly string[];
  allowedIds: readonly string[];
  choices?: readonly FolderAllowedSiteChoice[];
  loadProps?: (id: string) => Promise<PSFolderProperties>;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
  onFolderSaved?: (saved: SetFolderAllowedSitesShown) => void;
}): Promise<SetFolderAllowedSitesMultiSave> {
  const selected = canonicalAllowedSites(input.selectedIds.join(","));
  const canonical = selected.ok ? selected.canonical : "";
  const saved: SetFolderAllowedSitesTarget[] = [];
  const unchanged: SetFolderAllowedSitesTarget[] = [];
  const failures: SetFolderAllowedSitesItemFailure[] = [];
  const loadProps = input.loadProps ?? folderProperties;
  for (const name of input.noIdNames ?? []) {
    failures.push({ folderId: "", name, http: "other" });
  }
  for (const target of input.targets) {
    await saveOneFolderAllowedSites(target, {
      selectedIds: input.selectedIds,
      allowedIds: input.allowedIds,
      choices: input.choices,
      loadProps,
      save: input.save,
      reload: input.reload,
      onFolderSaved: input.onFolderSaved,
      saved,
      unchanged,
      failures,
    });
  }

  let status: SetFolderAllowedSitesMultiSave["status"];
  if (failures.length > 0) {
    status = saved.length > 0 ? "partial" : "failed";
  } else if (saved.length === 0) {
    status = "unchanged";
  } else {
    status = "saved";
  }
  const full = status === "saved";
  return {
    status,
    allowedSites: full ? canonical : "",
    allowedSiteNames: full ? namesFor(canonical, input.choices ?? []) : "",
    cleared: full && canonical.length === 0,
    saved,
    unchanged,
    failures,
    skippedPageNames: [...(input.skippedPageNames ?? [])],
    skippedAssetNames: [...(input.skippedAssetNames ?? [])],
    skippedOtherNames: [...(input.skippedOtherNames ?? [])],
  };
}

async function saveOneFolderAllowedSites(
  target: SetFolderAllowedSitesTarget,
  input: {
    selectedIds: readonly string[];
    allowedIds: readonly string[];
    choices?: readonly FolderAllowedSiteChoice[];
    loadProps: (id: string) => Promise<PSFolderProperties>;
    save?: (props: PSFolderProperties) => Promise<void>;
    reload?: (id: string) => Promise<PSFolderProperties>;
    onFolderSaved?: (saved: SetFolderAllowedSitesShown) => void;
    saved: SetFolderAllowedSitesTarget[];
    unchanged: SetFolderAllowedSitesTarget[];
    failures: SetFolderAllowedSitesItemFailure[];
  },
): Promise<void> {
  let props: PSFolderProperties;
  try {
    props = await input.loadProps(target.folderId);
  } catch (err: unknown) {
    input.failures.push({
      folderId: target.folderId,
      name: target.name,
      http: httpBucket(err),
    });
    return;
  }
  const one = await saveSetFolderAllowedSites({
    folderId: target.folderId,
    props,
    selectedIds: input.selectedIds,
    currentSites: folderAllowedSitesText(props),
    allowedIds: input.allowedIds,
    choices: input.choices,
    save: input.save,
    reload: input.reload,
  });
  if (one.status === "saved") {
    input.saved.push(target);
    input.onFolderSaved?.({
      folderId: target.folderId,
      name: target.name,
      allowedSites: one.allowedSites,
      allowedSiteNames: one.allowedSiteNames,
      cleared: one.cleared,
    });
    return;
  }
  if (one.status === "gate" && one.reason === "unchanged") {
    input.unchanged.push(target);
    return;
  }
  input.failures.push({
    folderId: target.folderId,
    name: target.name,
    http: folderSaveFailureCode(one),
  });
}

function folderSaveFailureCode(
  one: Exclude<SetFolderAllowedSitesSave, { status: "saved" }>,
): SetFolderAllowedSitesFailureCode {
  if (one.status === "http") {
    return one.http;
  }
  if (one.status === "mismatch") {
    return "mismatch";
  }
  if (one.status === "gate" && one.reason === "forbidden") {
    return "forbidden";
  }
  return "invalid";
}

/** Status line. Success is only a complete write of every target folder. */
export function describeSetFolderAllowedSitesMultiSave(
  result: SetFolderAllowedSitesMultiSave,
): {
  kind: "success" | "error";
  reason: string;
  allowedSites: string;
  allowedSiteNames: string;
  text: string;
} {
  const parts: string[] = [];
  if (result.status === "saved") {
    parts.push(
      result.cleared
        ? message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_CLEARED)
        : `${message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_SAVED)} ${result.allowedSiteNames}`.trim(),
    );
  } else if (result.status === "unchanged") {
    parts.push(message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_UNCHANGED));
  } else {
    const detail = result.failures
      .map((failure) => `${failure.name} (${folderFailureLabel(failure.http)})`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_PARTIAL).split("{detail}").join(detail),
    );
  }
  if (result.skippedPageNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_PAGE)}: ${result.skippedPageNames.join(", ")}`,
    );
  }
  if (result.skippedAssetNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_ASSET)}: ${result.skippedAssetNames.join(", ")}`,
    );
  }
  if (result.skippedOtherNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_NOT_FOLDER)}: ${result.skippedOtherNames.join(", ")}`,
    );
  }
  const skipped =
    result.skippedPageNames.length +
    result.skippedAssetNames.length +
    result.skippedOtherNames.length;
  const success = result.status === "saved";
  return {
    kind: success ? "success" : "error",
    reason: success
      ? skipped > 0
        ? "items-skipped"
        : result.cleared
          ? "cleared"
          : ""
      : result.status === "unchanged"
        ? "unchanged"
        : "partial",
    allowedSites: success ? result.allowedSites : "",
    allowedSiteNames: success ? result.allowedSiteNames : "",
    text: parts.join(" "),
  };
}

function folderFailureLabel(code: SetFolderAllowedSitesFailureCode): string {
  if (code === 400 || code === 403 || code === 409) {
    return `HTTP ${code}`;
  }
  if (code === "forbidden") {
    return message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_FORBIDDEN);
  }
  if (code === "invalid") {
    return message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_INVALID);
  }
  if (code === "mismatch") {
    return message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_MISMATCH);
  }
  return message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_FAILED);
}
