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
 * Content → Set folder community for one selected folder (#5105 / parent #4530)
 * and for every checked folder (#5156).
 *
 * <p>Assigns the community id from the community catalog, then reads folder
 * properties again. Cancel, pages, assets, empty selection, and HTTP 400/403/409
 * must not be reported as a saved community for the whole selection. A page or
 * asset in a multi-selection is not written. The community name is shown on a
 * folder only after that folder's save returns and a properties refresh shows
 * the new id. This is not item Set community (#5077 / #5133) and not the
 * security panel's free-text community name. Single-item still refuses a
 * multi-count so callers that have not opted into {@link planSetFolderCommunityMulti}
 * keep #5105.</p>
 */

import { isApiError, post } from "../api/client";
import {
  listFolderCommunityCatalog,
  type FolderCommunityChoice,
} from "../api/contentExplorer/folderCommunityApi";
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

export type SetFolderCommunityBlockReason =
  | "empty"
  | "page"
  | "asset"
  | "not-folder"
  | "multi"
  | "no-id";

export type SetFolderCommunitySelection =
  | { status: "blocked"; reason: SetFolderCommunityBlockReason; name: string }
  | { status: "ready"; folderId: string; name: string };

export function classifySetFolderCommunitySelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetFolderCommunitySelection {
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

/** Stored community id, or empty when the folder has none ({@code -1} / {@code 0}). */
export function folderCommunityIdText(props: PSFolderProperties | null | undefined): string {
  const raw = props?.communityId as unknown;
  if (typeof raw === "number" && Number.isFinite(raw)) {
    if (raw <= 0) {
      return "";
    }
    return String(raw);
  }
  const text = raw == null ? "" : String(raw).trim();
  if (!text || text === "0" || text === "-1") {
    return "";
  }
  return text;
}

export type CommunityChoiceGate =
  | { ok: true; communityId: string }
  | { ok: false; reason: "blank" | "unchanged" | "forbidden" };

/** Client gate before saveFolderProperties. The server repeats the catalog check. */
export function gateExplorerFolderCommunityChange(input: {
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
}): CommunityChoiceGate {
  const communityId = String(input.selectedId ?? "").trim();
  if (!communityId) {
    return { ok: false, reason: "blank" };
  }
  const current = String(input.currentId ?? "").trim();
  if (current && communityId === current) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = input.allowedIds
    .map((id) => String(id ?? "").trim())
    .filter((id) => id.length > 0);
  if (!allowed.includes(communityId)) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, communityId };
}

export type SetFolderCommunityCatalog =
  | {
      status: "ready";
      folderId: string;
      name: string;
      currentId: string;
      choices: FolderCommunityChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderCommunityBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetFolderCommunityCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderCommunityCatalog;
}): Promise<SetFolderCommunityCatalog> {
  const classified = classifySetFolderCommunitySelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderCommunityCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(classified.folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.id ?? "").trim().length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      folderId: classified.folderId,
      name: classified.name,
      currentId: folderCommunityIdText(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderCommunitySave =
  | { status: "saved"; communityId: string; communityName: string }
  | { status: "gate"; reason: "blank" | "unchanged" | "forbidden" }
  | { status: "mismatch" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

async function postFolderProperties(props: PSFolderProperties): Promise<void> {
  const body = wrapFolderProperties({
    ...props,
    permission: props.permission ?? { accessLevel: "ADMIN" },
  });
  const folder = body.FolderProperties;
  const raw = folder.communityId as unknown;
  const asNumber = typeof raw === "number" ? raw : Number(String(raw ?? "").trim());
  if (Number.isInteger(asNumber)) {
    (folder as { communityId?: number }).communityId = asNumber;
  }
  await post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, body);
}

export async function saveSetFolderCommunity(input: {
  folderId: string;
  props: PSFolderProperties;
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
  communityName?: string;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
}): Promise<SetFolderCommunitySave> {
  const gate = gateExplorerFolderCommunityChange({
    selectedId: input.selectedId,
    currentId: input.currentId,
    allowedIds: input.allowedIds,
  });
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const save = input.save ?? postFolderProperties;
  const reload = input.reload ?? folderProperties;
  const communityName = (input.communityName ?? "").trim() || gate.communityId;
  const next: PSFolderProperties = {
    ...input.props,
    id: input.props.id || input.folderId,
    communityId: gate.communityId,
    communityName,
  };
  try {
    await save(next);
    const again = await reload(input.folderId);
    if (folderCommunityIdText(again) !== gate.communityId) {
      return { status: "mismatch" };
    }
    const refreshedName = (again.communityName ?? "").trim();
    return {
      status: "saved",
      communityId: gate.communityId,
      communityName: refreshedName || communityName,
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
export function readReloadedCommunityId(data: unknown): string {
  return folderCommunityIdText(unwrapFolderProperties(data) ?? undefined);
}

export interface SetFolderCommunityTarget {
  folderId: string;
  name: string;
}

export type SetFolderCommunityMultiPlan =
  | { status: "blocked"; reason: SetFolderCommunityBlockReason; name: string }
  | {
      status: "ready";
      targets: SetFolderCommunityTarget[];
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

interface FolderCommunityBuckets {
  targets: SetFolderCommunityTarget[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
  noIdNames: string[];
}

function emptyBuckets(): FolderCommunityBuckets {
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
  buckets: FolderCommunityBuckets,
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
  buckets: FolderCommunityBuckets,
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
  buckets: FolderCommunityBuckets,
): SetFolderCommunityMultiPlan {
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
 * use {@link classifySetFolderCommunitySelection}, which blocks {@code multi}.
 */
export function planSetFolderCommunityMulti(
  items: readonly PSPathItem[],
): SetFolderCommunityMultiPlan {
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

export type SetFolderCommunityMultiCatalog =
  | {
      status: "ready";
      targets: SetFolderCommunityTarget[];
      skippedPageNames: string[];
      skippedAssetNames: string[];
      skippedOtherNames: string[];
      noIdNames: string[];
      currentId: string;
      choices: FolderCommunityChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderCommunityBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

/** Shared catalog plus the first folder's current community. Does not save. */
export async function loadSetFolderCommunityMultiCatalog(input: {
  items: readonly PSPathItem[];
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderCommunityCatalog;
}): Promise<SetFolderCommunityMultiCatalog> {
  const plan = planSetFolderCommunityMulti(input.items);
  if (plan.status === "blocked") {
    return plan;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderCommunityCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(plan.targets[0].folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.id ?? "").trim().length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      targets: plan.targets,
      skippedPageNames: plan.skippedPageNames,
      skippedAssetNames: plan.skippedAssetNames,
      skippedOtherNames: plan.skippedOtherNames,
      noIdNames: plan.noIdNames,
      currentId: folderCommunityIdText(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderCommunityFailureCode =
  | 400
  | 403
  | 409
  | "other"
  | "forbidden"
  | "blank"
  | "mismatch";

export interface SetFolderCommunityItemFailure {
  folderId: string;
  name: string;
  http: SetFolderCommunityFailureCode;
}

export interface SetFolderCommunityMultiSave {
  status: "saved" | "partial" | "failed" | "unchanged";
  communityId: string;
  saved: SetFolderCommunityTarget[];
  unchanged: SetFolderCommunityTarget[];
  failures: SetFolderCommunityItemFailure[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
}

export interface SetFolderCommunityShown {
  folderId: string;
  name: string;
  communityId: string;
  communityName: string;
}

/**
 * One community on every target folder. Each save reads that folder, posts
 * its properties, and reads them again. {@code onFolderSaved} runs only after
 * that refresh shows the new community id, and never for a failure.
 * Pages and assets are not posted.
 */
export async function saveSetFolderCommunityOnSelection(input: {
  targets: readonly SetFolderCommunityTarget[];
  skippedPageNames?: readonly string[];
  skippedAssetNames?: readonly string[];
  skippedOtherNames?: readonly string[];
  noIdNames?: readonly string[];
  selectedId: string;
  allowedIds: readonly string[];
  communityName?: string;
  loadProps?: (id: string) => Promise<PSFolderProperties>;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
  onFolderSaved?: (saved: SetFolderCommunityShown) => void;
}): Promise<SetFolderCommunityMultiSave> {
  const selectedId = String(input.selectedId ?? "").trim();
  const communityName = (input.communityName ?? "").trim() || selectedId;
  const saved: SetFolderCommunityTarget[] = [];
  const unchanged: SetFolderCommunityTarget[] = [];
  const failures: SetFolderCommunityItemFailure[] = [];
  const loadProps = input.loadProps ?? folderProperties;
  for (const name of input.noIdNames ?? []) {
    failures.push({ folderId: "", name, http: "other" });
  }
  for (const target of input.targets) {
    await saveOneFolderCommunity(target, {
      selectedId,
      communityName,
      allowedIds: input.allowedIds,
      loadProps,
      save: input.save,
      reload: input.reload,
      onFolderSaved: input.onFolderSaved,
      saved,
      unchanged,
      failures,
    });
  }

  let status: SetFolderCommunityMultiSave["status"];
  if (failures.length > 0) {
    status = saved.length > 0 ? "partial" : "failed";
  } else if (saved.length === 0) {
    status = "unchanged";
  } else {
    status = "saved";
  }
  return {
    status,
    communityId: status === "saved" ? selectedId : "",
    saved,
    unchanged,
    failures,
    skippedPageNames: [...(input.skippedPageNames ?? [])],
    skippedAssetNames: [...(input.skippedAssetNames ?? [])],
    skippedOtherNames: [...(input.skippedOtherNames ?? [])],
  };
}

async function saveOneFolderCommunity(
  target: SetFolderCommunityTarget,
  input: {
    selectedId: string;
    communityName: string;
    allowedIds: readonly string[];
    loadProps: (id: string) => Promise<PSFolderProperties>;
    save?: (props: PSFolderProperties) => Promise<void>;
    reload?: (id: string) => Promise<PSFolderProperties>;
    onFolderSaved?: (saved: SetFolderCommunityShown) => void;
    saved: SetFolderCommunityTarget[];
    unchanged: SetFolderCommunityTarget[];
    failures: SetFolderCommunityItemFailure[];
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
  const one = await saveSetFolderCommunity({
    folderId: target.folderId,
    props,
    selectedId: input.selectedId,
    currentId: folderCommunityIdText(props),
    allowedIds: input.allowedIds,
    communityName: input.communityName,
    save: input.save,
    reload: input.reload,
  });
  if (one.status === "saved") {
    input.saved.push(target);
    input.onFolderSaved?.({
      folderId: target.folderId,
      name: target.name,
      communityId: one.communityId,
      communityName: one.communityName,
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
  one: Exclude<SetFolderCommunitySave, { status: "saved" }>,
): SetFolderCommunityFailureCode {
  if (one.status === "http") {
    return one.http;
  }
  if (one.status === "mismatch") {
    return "mismatch";
  }
  if (one.status === "gate" && one.reason === "forbidden") {
    return "forbidden";
  }
  return "blank";
}

/** Status line. Success is only a complete write of every target folder. */
export function describeSetFolderCommunityMultiSave(
  result: SetFolderCommunityMultiSave,
  communityName: string,
): {
  kind: "success" | "error";
  reason: string;
  communityId: string;
  communityName: string;
  text: string;
} {
  const name = communityName.trim() || result.communityId;
  const parts: string[] = [];
  if (result.status === "saved") {
    parts.push(`${message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_SAVED)} ${name}`.trim());
  } else if (result.status === "unchanged") {
    parts.push(message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_UNCHANGED));
  } else {
    const detail = result.failures
      .map((failure) => `${failure.name} (${folderFailureLabel(failure.http)})`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_PARTIAL).split("{detail}").join(detail),
    );
  }
  if (result.skippedPageNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_PAGE)}: ${result.skippedPageNames.join(", ")}`,
    );
  }
  if (result.skippedAssetNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_ASSET)}: ${result.skippedAssetNames.join(", ")}`,
    );
  }
  if (result.skippedOtherNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_NOT_FOLDER)}: ${result.skippedOtherNames.join(", ")}`,
    );
  }
  const skipped =
    result.skippedPageNames.length +
    result.skippedAssetNames.length +
    result.skippedOtherNames.length;
  const success = result.status === "saved";
  return {
    kind: success ? "success" : "error",
    reason: success ? (skipped > 0 ? "items-skipped" : "") : result.status === "unchanged" ? "unchanged" : "partial",
    communityId: success ? result.communityId : "",
    communityName: success ? name : "",
    text: parts.join(" "),
  };
}

function folderFailureLabel(code: SetFolderCommunityFailureCode): string {
  if (code === 400 || code === 403 || code === 409) {
    return `HTTP ${code}`;
  }
  if (code === "forbidden") {
    return message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_FORBIDDEN);
  }
  if (code === "blank") {
    return message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_BLANK);
  }
  if (code === "mismatch") {
    return message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_MISMATCH);
  }
  return message(EXPLORER_MSG.SET_FOLDER_COMMUNITY_FAILED);
}
