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
 * Content → Set folder community (#5105 / parent #4530).
 *
 * <p>Assigns the community id on one selected folder from the community
 * catalog, then reads folder properties again. Cancel, pages, assets, empty
 * selection, and HTTP 400/403/409 must not be reported as a saved community.
 * This is not item Set community (#5077) and not the security panel's free-text
 * community name.</p>
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
import { resolvePublishKind } from "./itemPublish";
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
