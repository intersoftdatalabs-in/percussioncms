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
 * Content → Set folder display format for one selected folder (#5131 /
 * parent #4530) and for every checked folder (#5180).
 *
 * <p>Assigns {@code sys_displayformat} (the display-format id) on each target
 * folder, then reads that folder's properties again. Success requires the
 * refresh to show that id and the catalog name resolved from it. A name-only
 * change is not sent and is not success. The format name is shown on a folder
 * only after that refresh. Cancel, pages, assets, empty selection, and HTTP
 * 400/403/409 must not be reported as a saved format for the whole selection.
 * A page or asset in a multi-selection is not written. This is not the
 * list-column chooser. Single-item still refuses a multi-count so callers
 * that have not opted into {@link planSetFolderDisplayFormatMulti} keep
 * #5131.</p>
 */

import { isApiError, post } from "../api/client";
import {
  listFolderDisplayFormatCatalog,
  type FolderDisplayFormatChoice,
} from "../api/contentExplorer/folderDisplayFormatApi";
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

export type SetFolderDisplayFormatBlockReason =
  | "empty"
  | "page"
  | "asset"
  | "not-folder"
  | "multi"
  | "no-id";

export type SetFolderDisplayFormatSelection =
  | { status: "blocked"; reason: SetFolderDisplayFormatBlockReason; name: string }
  | { status: "ready"; folderId: string; name: string };

export function classifySetFolderDisplayFormatSelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetFolderDisplayFormatSelection {
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

/** Positive {@code sys_displayformat} id, or empty when the value is not an id. */
export function canonicalDisplayFormatId(raw: unknown): string {
  if (typeof raw === "number" && Number.isFinite(raw)) {
    if (!Number.isInteger(raw) || raw <= 0) {
      return "";
    }
    return String(raw);
  }
  const text = raw == null ? "" : String(raw).trim();
  if (!/^\d+$/.test(text)) {
    return "";
  }
  const id = Number(text);
  if (!Number.isInteger(id) || id <= 0) {
    return "";
  }
  return String(id);
}

export function folderDisplayFormatId(
  props: PSFolderProperties | null | undefined,
): string {
  return canonicalDisplayFormatId(props?.displayFormatId);
}

export function folderDisplayFormatName(
  props: PSFolderProperties | null | undefined,
): string {
  const raw = props?.displayFormatName;
  return raw == null ? "" : String(raw).trim();
}

function sameId(left: string, right: string): boolean {
  const a = canonicalDisplayFormatId(left);
  const b = canonicalDisplayFormatId(right);
  return a.length > 0 && a === b;
}

function sameName(left: string, right: string): boolean {
  const a = String(left ?? "").trim().toLowerCase();
  const b = String(right ?? "").trim().toLowerCase();
  return a.length > 0 && a === b;
}

export type DisplayFormatChoiceGate =
  | { ok: true; id: string }
  | { ok: false; reason: "blank" | "unchanged" | "forbidden" };

/** Client gate before saveFolderProperties. The server repeats the catalog check. */
export function gateExplorerFolderDisplayFormatChange(input: {
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
}): DisplayFormatChoiceGate {
  const id = canonicalDisplayFormatId(input.selectedId);
  if (!id) {
    return { ok: false, reason: "blank" };
  }
  if (sameId(id, input.currentId)) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = input.allowedIds
    .map((value) => canonicalDisplayFormatId(value))
    .filter((value) => value.length > 0);
  if (!allowed.some((value) => value === id)) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, id };
}

export type SetFolderDisplayFormatCatalog =
  | {
      status: "ready";
      folderId: string;
      name: string;
      currentId: string;
      choices: FolderDisplayFormatChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderDisplayFormatBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetFolderDisplayFormatCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderDisplayFormatCatalog;
}): Promise<SetFolderDisplayFormatCatalog> {
  const classified = classifySetFolderDisplayFormatSelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderDisplayFormatCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(classified.folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => canonicalDisplayFormatId(row?.id).length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      folderId: classified.folderId,
      name: classified.name,
      currentId: folderDisplayFormatId(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderDisplayFormatSave =
  | { status: "saved"; displayFormatId: string; displayFormatName: string }
  | { status: "gate"; reason: "blank" | "unchanged" | "forbidden" }
  | { status: "mismatch" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

async function postFolderProperties(props: PSFolderProperties): Promise<void> {
  const body = wrapFolderProperties({
    ...props,
    permission: props.permission ?? { accessLevel: "ADMIN" },
  });
  await post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, body);
}

export async function saveSetFolderDisplayFormat(input: {
  folderId: string;
  props: PSFolderProperties;
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
  displayFormatName?: string;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
}): Promise<SetFolderDisplayFormatSave> {
  const gate = gateExplorerFolderDisplayFormatChange({
    selectedId: input.selectedId,
    currentId: input.currentId,
    allowedIds: input.allowedIds,
  });
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const save = input.save ?? postFolderProperties;
  const reload = input.reload ?? folderProperties;
  const expectedName = (input.displayFormatName ?? "").trim();
  const next: PSFolderProperties = {
    ...input.props,
    id: input.props.id || input.folderId,
    displayFormatId: gate.id,
  };
  try {
    await save(next);
    const again = await reload(input.folderId);
    if (!sameId(folderDisplayFormatId(again), gate.id)) {
      return { status: "mismatch" };
    }
    const resolved = folderDisplayFormatName(again);
    if (!resolved || (expectedName && !sameName(resolved, expectedName))) {
      return { status: "mismatch" };
    }
    return {
      status: "saved",
      displayFormatId: folderDisplayFormatId(again) || gate.id,
      displayFormatName: resolved,
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
export function readReloadedDisplayFormat(data: unknown): {
  id: string;
  name: string;
} {
  const props = unwrapFolderProperties(data) ?? undefined;
  return {
    id: folderDisplayFormatId(props),
    name: folderDisplayFormatName(props),
  };
}

export interface SetFolderDisplayFormatTarget {
  folderId: string;
  name: string;
}

export type SetFolderDisplayFormatMultiPlan =
  | { status: "blocked"; reason: SetFolderDisplayFormatBlockReason; name: string }
  | {
      status: "ready";
      targets: SetFolderDisplayFormatTarget[];
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

interface FolderDisplayFormatBuckets {
  targets: SetFolderDisplayFormatTarget[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
  noIdNames: string[];
}

function emptyBuckets(): FolderDisplayFormatBuckets {
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
  buckets: FolderDisplayFormatBuckets,
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
  buckets: FolderDisplayFormatBuckets,
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
  buckets: FolderDisplayFormatBuckets,
): SetFolderDisplayFormatMultiPlan {
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
 * use {@link classifySetFolderDisplayFormatSelection}, which blocks {@code multi}.
 */
export function planSetFolderDisplayFormatMulti(
  items: readonly PSPathItem[],
): SetFolderDisplayFormatMultiPlan {
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

export type SetFolderDisplayFormatMultiCatalog =
  | {
      status: "ready";
      targets: SetFolderDisplayFormatTarget[];
      skippedPageNames: string[];
      skippedAssetNames: string[];
      skippedOtherNames: string[];
      noIdNames: string[];
      currentId: string;
      choices: FolderDisplayFormatChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderDisplayFormatBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

/** Shared catalog plus the first folder's current format. Does not save. */
export async function loadSetFolderDisplayFormatMultiCatalog(input: {
  items: readonly PSPathItem[];
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderDisplayFormatCatalog;
}): Promise<SetFolderDisplayFormatMultiCatalog> {
  const plan = planSetFolderDisplayFormatMulti(input.items);
  if (plan.status === "blocked") {
    return plan;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderDisplayFormatCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(plan.targets[0].folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => canonicalDisplayFormatId(row?.id).length > 0,
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
      currentId: folderDisplayFormatId(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderDisplayFormatFailureCode =
  | 400
  | 403
  | 409
  | "other"
  | "forbidden"
  | "blank"
  | "mismatch";

export interface SetFolderDisplayFormatItemFailure {
  folderId: string;
  name: string;
  http: SetFolderDisplayFormatFailureCode;
}

export interface SetFolderDisplayFormatMultiSave {
  status: "saved" | "partial" | "failed" | "unchanged";
  displayFormatId: string;
  saved: SetFolderDisplayFormatTarget[];
  unchanged: SetFolderDisplayFormatTarget[];
  failures: SetFolderDisplayFormatItemFailure[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
}

export interface SetFolderDisplayFormatShown {
  folderId: string;
  name: string;
  displayFormatId: string;
  displayFormatName: string;
}

/**
 * One catalog display format on every target folder. Each save reads that
 * folder, posts its properties with {@code sys_displayformat}, and reads them
 * again. {@code onFolderSaved} runs only after that refresh shows the new id
 * and the catalog name, and never for a failure. Pages and assets are not posted.
 */
export async function saveSetFolderDisplayFormatOnSelection(input: {
  targets: readonly SetFolderDisplayFormatTarget[];
  skippedPageNames?: readonly string[];
  skippedAssetNames?: readonly string[];
  skippedOtherNames?: readonly string[];
  noIdNames?: readonly string[];
  selectedId: string;
  allowedIds: readonly string[];
  displayFormatName?: string;
  loadProps?: (id: string) => Promise<PSFolderProperties>;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
  onFolderSaved?: (saved: SetFolderDisplayFormatShown) => void;
}): Promise<SetFolderDisplayFormatMultiSave> {
  const selectedId = canonicalDisplayFormatId(input.selectedId);
  const displayFormatName = (input.displayFormatName ?? "").trim() || selectedId;
  const saved: SetFolderDisplayFormatTarget[] = [];
  const unchanged: SetFolderDisplayFormatTarget[] = [];
  const failures: SetFolderDisplayFormatItemFailure[] = [];
  const loadProps = input.loadProps ?? folderProperties;
  for (const name of input.noIdNames ?? []) {
    failures.push({ folderId: "", name, http: "other" });
  }
  for (const target of input.targets) {
    await saveOneFolderDisplayFormat(target, {
      selectedId,
      displayFormatName,
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

  let status: SetFolderDisplayFormatMultiSave["status"];
  if (failures.length > 0) {
    status = saved.length > 0 ? "partial" : "failed";
  } else if (saved.length === 0) {
    status = "unchanged";
  } else {
    status = "saved";
  }
  return {
    status,
    displayFormatId: status === "saved" ? selectedId : "",
    saved,
    unchanged,
    failures,
    skippedPageNames: [...(input.skippedPageNames ?? [])],
    skippedAssetNames: [...(input.skippedAssetNames ?? [])],
    skippedOtherNames: [...(input.skippedOtherNames ?? [])],
  };
}

async function saveOneFolderDisplayFormat(
  target: SetFolderDisplayFormatTarget,
  input: {
    selectedId: string;
    displayFormatName: string;
    allowedIds: readonly string[];
    loadProps: (id: string) => Promise<PSFolderProperties>;
    save?: (props: PSFolderProperties) => Promise<void>;
    reload?: (id: string) => Promise<PSFolderProperties>;
    onFolderSaved?: (saved: SetFolderDisplayFormatShown) => void;
    saved: SetFolderDisplayFormatTarget[];
    unchanged: SetFolderDisplayFormatTarget[];
    failures: SetFolderDisplayFormatItemFailure[];
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
  const one = await saveSetFolderDisplayFormat({
    folderId: target.folderId,
    props,
    selectedId: input.selectedId,
    currentId: folderDisplayFormatId(props),
    allowedIds: input.allowedIds,
    displayFormatName: input.displayFormatName,
    save: input.save,
    reload: input.reload,
  });
  if (one.status === "saved") {
    input.saved.push(target);
    input.onFolderSaved?.({
      folderId: target.folderId,
      name: target.name,
      displayFormatId: one.displayFormatId,
      displayFormatName: one.displayFormatName,
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
  one: Exclude<SetFolderDisplayFormatSave, { status: "saved" }>,
): SetFolderDisplayFormatFailureCode {
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
export function describeSetFolderDisplayFormatMultiSave(
  result: SetFolderDisplayFormatMultiSave,
  displayFormatName: string,
): {
  kind: "success" | "error";
  reason: string;
  displayFormatId: string;
  displayFormatName: string;
  text: string;
} {
  const name = displayFormatName.trim() || result.displayFormatId;
  const parts: string[] = [];
  if (result.status === "saved") {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_SAVED)} ${name}`.trim(),
    );
  } else if (result.status === "unchanged") {
    parts.push(message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_UNCHANGED));
  } else {
    const detail = result.failures
      .map((failure) => `${failure.name} (${folderFailureLabel(failure.http)})`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_PARTIAL)
        .split("{detail}")
        .join(detail),
    );
  }
  if (result.skippedPageNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_PAGE)}: ${result.skippedPageNames.join(", ")}`,
    );
  }
  if (result.skippedAssetNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_ASSET)}: ${result.skippedAssetNames.join(", ")}`,
    );
  }
  if (result.skippedOtherNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_NOT_FOLDER)}: ${result.skippedOtherNames.join(", ")}`,
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
        : ""
      : result.status === "unchanged"
        ? "unchanged"
        : "partial",
    displayFormatId: success ? result.displayFormatId : "",
    displayFormatName: success ? name : "",
    text: parts.join(" "),
  };
}

function folderFailureLabel(code: SetFolderDisplayFormatFailureCode): string {
  if (code === 400 || code === 403 || code === 409) {
    return `HTTP ${code}`;
  }
  if (code === "forbidden") {
    return message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_FORBIDDEN);
  }
  if (code === "blank") {
    return message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_BLANK);
  }
  if (code === "mismatch") {
    return message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_MISMATCH);
  }
  return message(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_FAILED);
}
