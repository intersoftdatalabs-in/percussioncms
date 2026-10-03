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
 * Content → Set folder locale (#5106 / parent #4530).
 *
 * <p>Assigns the locale language string on one selected folder from the locale
 * catalog, then reads folder properties again. Cancel, pages, assets, empty
 * selection, and HTTP 400/403/409 must not be reported as a saved locale.
 * This is not an item translation variant (#5037) and not a free-text locale
 * in the security panel.</p>
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
import { resolvePublishKind } from "./itemPublish";
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
