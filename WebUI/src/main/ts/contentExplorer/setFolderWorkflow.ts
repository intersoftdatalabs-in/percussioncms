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
 * Content → Set folder workflow (#5104 / parent #4530).
 *
 * <p>Assigns {@code sys_workflowid} on one selected folder from the workflow
 * catalog, then reads folder properties again. Cancel, pages, assets, empty
 * selection, and HTTP 400/403/409 must not be reported as a saved workflow.
 * This is not item Set workflow (#5076) and not the security panel's free-text
 * workflow id.</p>
 */

import { isApiError, post } from "../api/client";
import {
  listFolderWorkflowCatalog,
  type FolderWorkflowChoice,
} from "../api/contentExplorer/folderWorkflowApi";
import {
  folderProperties,
  unwrapFolderProperties,
  wrapFolderProperties,
} from "../api/contentExplorer/pathApi";
import { PATHS } from "../api/paths";
import type { PSFolderProperties, PSPathItem } from "../api/contentExplorer/types";
import { resolvePublishKind } from "./itemPublish";
import { gateExplorerWorkflowChange } from "./setItemWorkflow";
import { isFolder } from "./selection";

export type SetFolderWorkflowBlockReason =
  | "empty"
  | "page"
  | "asset"
  | "not-folder"
  | "multi"
  | "no-id";

export type SetFolderWorkflowSelection =
  | { status: "blocked"; reason: SetFolderWorkflowBlockReason; name: string }
  | { status: "ready"; folderId: string; name: string };

export function classifySetFolderWorkflowSelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetFolderWorkflowSelection {
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

/** Stored workflow id, or empty when the folder has none ({@code -1} / {@code 0}). */
export function folderWorkflowIdText(props: PSFolderProperties | null | undefined): string {
  const raw = props?.workflowId as unknown;
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

export type SetFolderWorkflowCatalog =
  | {
      status: "ready";
      folderId: string;
      name: string;
      currentId: string;
      choices: FolderWorkflowChoice[];
      props: PSFolderProperties;
    }
  | { status: "blocked"; reason: SetFolderWorkflowBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetFolderWorkflowCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadProps?: typeof folderProperties;
  loadCatalog?: typeof listFolderWorkflowCatalog;
}): Promise<SetFolderWorkflowCatalog> {
  const classified = classifySetFolderWorkflowSelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const loadProps = input.loadProps ?? folderProperties;
  const loadCatalog = input.loadCatalog ?? listFolderWorkflowCatalog;
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
      currentId: folderWorkflowIdText(props),
      choices,
      props,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetFolderWorkflowSave =
  | { status: "saved"; workflowId: string }
  | { status: "gate"; reason: "blank" | "unchanged" | "forbidden" }
  | { status: "mismatch" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

async function postFolderProperties(props: PSFolderProperties): Promise<void> {
  const body: PSFolderProperties = {
    ...props,
    permission: props.permission ?? { accessLevel: "ADMIN" },
  };
  await post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, wrapFolderProperties(body));
}

export async function saveSetFolderWorkflow(input: {
  folderId: string;
  props: PSFolderProperties;
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
  save?: (props: PSFolderProperties) => Promise<void>;
  reload?: (id: string) => Promise<PSFolderProperties>;
}): Promise<SetFolderWorkflowSave> {
  const gate = gateExplorerWorkflowChange({
    selectedId: input.selectedId,
    currentId: input.currentId,
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
    workflowId: gate.workflowId,
  };
  try {
    await save(next);
    const again = await reload(input.folderId);
    if (folderWorkflowIdText(again) !== gate.workflowId) {
      return { status: "mismatch" };
    }
    return { status: "saved", workflowId: gate.workflowId };
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
export function readReloadedWorkflowId(data: unknown): string {
  return folderWorkflowIdText(unwrapFolderProperties(data) ?? undefined);
}
