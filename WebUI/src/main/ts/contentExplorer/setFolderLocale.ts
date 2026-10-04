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
 * Content → Set folder locale for one selected folder (#5106 / parent #4530)
 * and for every checked folder (#5157).
 *
 * <p>Assigns the locale language string from the locale catalog, then reads
 * folder properties again. Cancel, pages, assets, empty selection, and HTTP
 * 400/403/409 must not be reported as a saved locale for the whole selection.
 * A page or asset in a multi-selection is not written. The locale name is
 * shown on a folder only after that folder's save returns and a properties
 * refresh shows the new code. This is not an item translation variant
 * (#5037) and not a free-text locale in the security panel. Single-item
 * still refuses a multi-count so callers that have not opted into
 * {@link planSetFolderLocaleMulti} keep #5106.</p>
 */

import { isApiError, post } from "../api/client";
import {
  listFolderLocaleCatalog,
  type FolderLocaleChoice,
} from "../api/contentExplorer/folderLocaleApi";
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

export type SetFolderLocaleBlockReason =
  | "empty"
  | "page"
  | "asset"
  | "not-folder"
  | "multi"
  | "no-id";

export type SetFolderLocaleSelection =
  | { status: "blocked"; reason: SetFolderLocaleBlockReason; name: string }
  | { status: "ready"; folderId: string; name: string };

export function classifySetFolderLocaleSelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetFolderLocaleSelection {
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

/** Stored locale language string, or empty when the folder has none. */
export function folderLocaleText(props: PSFolderProperties | null | undefined): string {
  const raw = props?.locale;
  return raw == null ? "" : String(raw).trim();
}

function sameLocale(left: string, right: string): boolean {
  const a = String(left ?? "").trim().toLowerCase();
  const b = String(right ?? "").trim().toLowerCase();
  return a.length > 0 && a === b;
}

export type LocaleChoiceGate =
  | { ok: true; localeCode: string }
  | { ok: false; reason: "blank" | "unchanged" | "forbidden" };

/** Client gate before saveFolderProperties. The server repeats the catalog check. */
export function gateExplorerFolderLocaleChange(input: {
  selectedCode: string;
  currentCode: string;
  allowedCodes: readonly string[];
}): LocaleChoiceGate {
  const localeCode = String(input.selectedCode ?? "").trim();
  if (!localeCode) {
    return { ok: false, reason: "blank" };
  }
  if (sameLocale(localeCode, input.currentCode)) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = input.allowedCodes
    .map((code) => String(code ?? "").trim())
    .filter((code) => code.length > 0);
  if (!allowed.some((code) => sameLocale(code, localeCode))) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, localeCode };
}

export type SetFolderLocaleCatalog =
  | {
      status: "ready";
      folderId: string;
      name: string;
      currentCode: string;
      choices: FolderLocaleChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderLocaleBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetFolderLocaleCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderLocaleCatalog;
}): Promise<SetFolderLocaleCatalog> {
  const classified = classifySetFolderLocaleSelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderLocaleCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(classified.folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.code ?? "").trim().length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      folderId: classified.folderId,
      name: classified.name,
      currentCode: folderLocaleText(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderLocaleSave =
  | { status: "saved"; localeCode: string; localeName: string }
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

export async function saveSetFolderLocale(input: {
  folderId: string;
  props: PSFolderProperties;
  selectedCode: string;
  currentCode: string;
  allowedCodes: readonly string[];
  localeName?: string;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
}): Promise<SetFolderLocaleSave> {
  const gate = gateExplorerFolderLocaleChange({
    selectedCode: input.selectedCode,
    currentCode: input.currentCode,
    allowedCodes: input.allowedCodes,
  });
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const save = input.save ?? postFolderProperties;
  const reload = input.reload ?? folderProperties;
  const localeName = (input.localeName ?? "").trim() || gate.localeCode;
  const next: PSFolderProperties = {
    ...input.props,
    id: input.props.id || input.folderId,
    locale: gate.localeCode,
  };
  try {
    await save(next);
    const again = await reload(input.folderId);
    if (!sameLocale(folderLocaleText(again), gate.localeCode)) {
      return { status: "mismatch" };
    }
    return {
      status: "saved",
      localeCode: folderLocaleText(again) || gate.localeCode,
      localeName,
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
export function readReloadedLocale(data: unknown): string {
  return folderLocaleText(unwrapFolderProperties(data) ?? undefined);
}

export interface SetFolderLocaleTarget {
  folderId: string;
  name: string;
}

export type SetFolderLocaleMultiPlan =
  | { status: "blocked"; reason: SetFolderLocaleBlockReason; name: string }
  | {
      status: "ready";
      targets: SetFolderLocaleTarget[];
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

interface FolderLocaleBuckets {
  targets: SetFolderLocaleTarget[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
  noIdNames: string[];
}

function emptyBuckets(): FolderLocaleBuckets {
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
  buckets: FolderLocaleBuckets,
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
  buckets: FolderLocaleBuckets,
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

function blockedWithoutFolderTargets(buckets: FolderLocaleBuckets): SetFolderLocaleMultiPlan {
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
 * use {@link classifySetFolderLocaleSelection}, which blocks {@code multi}.
 */
export function planSetFolderLocaleMulti(items: readonly PSPathItem[]): SetFolderLocaleMultiPlan {
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

export type SetFolderLocaleMultiCatalog =
  | {
      status: "ready";
      targets: SetFolderLocaleTarget[];
      skippedPageNames: string[];
      skippedAssetNames: string[];
      skippedOtherNames: string[];
      noIdNames: string[];
      currentCode: string;
      choices: FolderLocaleChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderLocaleBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

/** Shared catalog plus the first folder's current locale. Does not save. */
export async function loadSetFolderLocaleMultiCatalog(input: {
  items: readonly PSPathItem[];
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderLocaleCatalog;
}): Promise<SetFolderLocaleMultiCatalog> {
  const plan = planSetFolderLocaleMulti(input.items);
  if (plan.status === "blocked") {
    return plan;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderLocaleCatalog;
  try {
    const [props, catalog] = await Promise.all([
      loadProps(plan.targets[0].folderId),
      loadCatalog(),
    ]);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.code ?? "").trim().length > 0,
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
      currentCode: folderLocaleText(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderLocaleFailureCode =
  | 400
  | 403
  | 409
  | "other"
  | "forbidden"
  | "blank"
  | "mismatch";

export interface SetFolderLocaleItemFailure {
  folderId: string;
  name: string;
  http: SetFolderLocaleFailureCode;
}

export interface SetFolderLocaleMultiSave {
  status: "saved" | "partial" | "failed" | "unchanged";
  localeCode: string;
  saved: SetFolderLocaleTarget[];
  unchanged: SetFolderLocaleTarget[];
  failures: SetFolderLocaleItemFailure[];
  skippedPageNames: string[];
  skippedAssetNames: string[];
  skippedOtherNames: string[];
}

export interface SetFolderLocaleShown {
  folderId: string;
  name: string;
  localeCode: string;
  localeName: string;
}

/**
 * One locale on every target folder. Each save reads that folder, posts its
 * properties, and reads them again. {@code onFolderSaved} runs only after
 * that refresh shows the new locale, and never for a failure. Pages and
 * assets are not posted.
 */
export async function saveSetFolderLocaleOnSelection(input: {
  targets: readonly SetFolderLocaleTarget[];
  skippedPageNames?: readonly string[];
  skippedAssetNames?: readonly string[];
  skippedOtherNames?: readonly string[];
  noIdNames?: readonly string[];
  selectedCode: string;
  allowedCodes: readonly string[];
  localeName?: string;
  loadProps?: (id: string) => Promise<PSFolderProperties>;
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
  onFolderSaved?: (saved: SetFolderLocaleShown) => void;
}): Promise<SetFolderLocaleMultiSave> {
  const selectedCode = String(input.selectedCode ?? "").trim();
  const localeName = (input.localeName ?? "").trim() || selectedCode;
  const saved: SetFolderLocaleTarget[] = [];
  const unchanged: SetFolderLocaleTarget[] = [];
  const failures: SetFolderLocaleItemFailure[] = [];
  const loadProps = input.loadProps ?? folderProperties;
  for (const name of input.noIdNames ?? []) {
    failures.push({ folderId: "", name, http: "other" });
  }
  for (const target of input.targets) {
    await saveOneFolderLocale(target, {
      selectedCode,
      localeName,
      allowedCodes: input.allowedCodes,
      loadProps,
      save: input.save,
      reload: input.reload,
      onFolderSaved: input.onFolderSaved,
      saved,
      unchanged,
      failures,
    });
  }

  let status: SetFolderLocaleMultiSave["status"];
  if (failures.length > 0) {
    status = saved.length > 0 ? "partial" : "failed";
  } else if (saved.length === 0) {
    status = "unchanged";
  } else {
    status = "saved";
  }
  return {
    status,
    localeCode: status === "saved" ? selectedCode : "",
    saved,
    unchanged,
    failures,
    skippedPageNames: [...(input.skippedPageNames ?? [])],
    skippedAssetNames: [...(input.skippedAssetNames ?? [])],
    skippedOtherNames: [...(input.skippedOtherNames ?? [])],
  };
}

async function saveOneFolderLocale(
  target: SetFolderLocaleTarget,
  input: {
    selectedCode: string;
    localeName: string;
    allowedCodes: readonly string[];
    loadProps: (id: string) => Promise<PSFolderProperties>;
    save?: (props: PSFolderProperties) => Promise<void>;
    reload?: (id: string) => Promise<PSFolderProperties>;
    onFolderSaved?: (saved: SetFolderLocaleShown) => void;
    saved: SetFolderLocaleTarget[];
    unchanged: SetFolderLocaleTarget[];
    failures: SetFolderLocaleItemFailure[];
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
  const one = await saveSetFolderLocale({
    folderId: target.folderId,
    props,
    selectedCode: input.selectedCode,
    currentCode: folderLocaleText(props),
    allowedCodes: input.allowedCodes,
    localeName: input.localeName,
    save: input.save,
    reload: input.reload,
  });
  if (one.status === "saved") {
    input.saved.push(target);
    input.onFolderSaved?.({
      folderId: target.folderId,
      name: target.name,
      localeCode: one.localeCode,
      localeName: one.localeName,
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
  one: Exclude<SetFolderLocaleSave, { status: "saved" }>,
): SetFolderLocaleFailureCode {
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
export function describeSetFolderLocaleMultiSave(
  result: SetFolderLocaleMultiSave,
  localeName: string,
): {
  kind: "success" | "error";
  reason: string;
  localeCode: string;
  localeName: string;
  text: string;
} {
  const name = localeName.trim() || result.localeCode;
  const parts: string[] = [];
  if (result.status === "saved") {
    parts.push(`${message(EXPLORER_MSG.SET_FOLDER_LOCALE_SAVED)} ${name}`.trim());
  } else if (result.status === "unchanged") {
    parts.push(message(EXPLORER_MSG.SET_FOLDER_LOCALE_UNCHANGED));
  } else {
    const detail = result.failures
      .map((failure) => `${failure.name} (${folderFailureLabel(failure.http)})`)
      .join("; ");
    parts.push(message(EXPLORER_MSG.SET_FOLDER_LOCALE_PARTIAL).split("{detail}").join(detail));
  }
  if (result.skippedPageNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_LOCALE_PAGE)}: ${result.skippedPageNames.join(", ")}`,
    );
  }
  if (result.skippedAssetNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_LOCALE_ASSET)}: ${result.skippedAssetNames.join(", ")}`,
    );
  }
  if (result.skippedOtherNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_FOLDER_LOCALE_NOT_FOLDER)}: ${result.skippedOtherNames.join(", ")}`,
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
    localeCode: success ? result.localeCode : "",
    localeName: success ? name : "",
    text: parts.join(" "),
  };
}

function folderFailureLabel(code: SetFolderLocaleFailureCode): string {
  if (code === 400 || code === 403 || code === 409) {
    return `HTTP ${code}`;
  }
  if (code === "forbidden") {
    return message(EXPLORER_MSG.SET_FOLDER_LOCALE_FORBIDDEN);
  }
  if (code === "blank") {
    return message(EXPLORER_MSG.SET_FOLDER_LOCALE_BLANK);
  }
  if (code === "mismatch") {
    return message(EXPLORER_MSG.SET_FOLDER_LOCALE_MISMATCH);
  }
  return message(EXPLORER_MSG.SET_FOLDER_LOCALE_FAILED);
}
