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
 * Content → Set folder display format (#5131 / parent #4530).
 *
 * <p>Assigns {@code sys_displayformat} (the display-format id) on one selected
 * folder, then reads folder properties again. Success requires the refresh to
 * show that id and the catalog name resolved from it. A name-only change is
 * not sent and is not success. Cancel, pages, assets, empty selection,
 * multi-select, and HTTP 400/403/409 must not be reported as a saved format.
 * This is not the list-column chooser.</p>
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
import { resolvePublishKind } from "./itemPublish";
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
