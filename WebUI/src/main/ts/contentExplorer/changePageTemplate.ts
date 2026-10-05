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
 * Content → Change page template for one selected page (#5200 / parent #4530).
 *
 * <p>Save uses the same page {@code changeTemplate} call EditorHost uses.
 * Cancel, folders, assets, multi-select, and HTTP 400/403/409 must not be
 * reported as a new template. This is not assembly slot change-template and
 * not the site-create or create-page template picker.</p>
 */

import { isApiError } from "../api/client";
import type { PSPathItem } from "../api/contentExplorer/types";
import { fetchItemEditorFields, type ItemEditorFields } from "../editor/itemFieldsApi";
import {
  changePageTemplate,
  isAllowedPageTemplate,
  pageTemplateIdFromFields,
} from "../editor/editorPageTemplate";
import { loadPageTemplates, type PageTemplateChoice } from "../editor/pageTemplates";
import { resolvePublishKind } from "./itemPublish";
import { isFolder } from "./selection";

export type ChangePageTemplateBlockReason =
  | "empty"
  | "folder"
  | "multi"
  | "asset"
  | "not-page"
  | "no-id";

export type ChangePageTemplateSelection =
  | { status: "blocked"; reason: ChangePageTemplateBlockReason; name: string }
  | {
      status: "ready";
      itemId: string;
      name: string;
      folderPath: string;
      contentType: string;
    };

/** CMS folder paths always use {@code /}. This is not an OS filesystem join. */
export function folderPathForPageItem(item: PSPathItem): string {
  const explicit = (item.folderPath ?? "").trim().replace(/\\/g, "/");
  if (explicit) {
    return explicit.replace(/\/+$/, "");
  }
  const path = (item.path ?? "").trim().replace(/\\/g, "/");
  const slash = path.lastIndexOf("/");
  if (slash <= 0) {
    return "";
  }
  return path.slice(0, slash);
}

export function classifyChangePageTemplateSelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): ChangePageTemplateSelection {
  if (input.selectedCount >= 2) {
    return { status: "blocked", reason: "multi", name: "" };
  }
  const item = input.item ?? null;
  if (!item) {
    return { status: "blocked", reason: "empty", name: "" };
  }
  const name = (item.name ?? item.path ?? "").trim();
  if (isFolder(item)) {
    return { status: "blocked", reason: "folder", name };
  }
  const kind = resolvePublishKind(item);
  if (kind === "asset") {
    return { status: "blocked", reason: "asset", name };
  }
  if (kind !== "page") {
    return { status: "blocked", reason: "not-page", name };
  }
  const itemId = item.id == null ? "" : String(item.id).trim();
  if (!itemId) {
    return { status: "blocked", reason: "no-id", name };
  }
  return {
    status: "ready",
    itemId,
    name,
    folderPath: folderPathForPageItem(item),
    contentType: (item.type ?? "").trim(),
  };
}

export type PageTemplateChoiceGate =
  | { ok: true; templateId: string }
  | { ok: false; reason: "blank" | "unchanged" | "forbidden" };

/** Client gate before PUT changeTemplate. The page service repeats the check. */
export function gateExplorerPageTemplateChange(input: {
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
}): PageTemplateChoiceGate {
  const templateId = String(input.selectedId ?? "").trim();
  if (!templateId) {
    return { ok: false, reason: "blank" };
  }
  const current = String(input.currentId ?? "").trim();
  if (current && templateId === current) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = input.allowedIds
    .map((id) => String(id ?? "").trim())
    .filter((id) => id.length > 0);
  if (!allowed.includes(templateId)) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, templateId };
}

export type ChangePageTemplateCatalog =
  | {
      status: "ready";
      itemId: string;
      currentId: string;
      choices: PageTemplateChoice[];
    }
  | { status: "blocked"; reason: ChangePageTemplateBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

function storedTemplateId(
  fields: ItemEditorFields,
  choices: readonly PageTemplateChoice[],
): string {
  const stored = pageTemplateIdFromFields(fields.fields);
  if (isAllowedPageTemplate(stored, choices)) {
    return stored.trim();
  }
  const byName = choices.find(
    (choice) => choice.name.trim().toLowerCase() === stored.trim().toLowerCase(),
  );
  return byName?.id ?? "";
}

export async function loadChangePageTemplateCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadFields?: (itemId: string) => Promise<ItemEditorFields>;
  loadTemplates?: (
    folderPath: string,
    contentType: string,
  ) => Promise<PageTemplateChoice[]>;
}): Promise<ChangePageTemplateCatalog> {
  const classified = classifyChangePageTemplateSelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const loadFields = input.loadFields ?? fetchItemEditorFields;
  const loadTemplates = input.loadTemplates ?? loadPageTemplates;
  try {
    const fields = await loadFields(classified.itemId);
    const contentType = fields.contentType.trim() || classified.contentType;
    const choices = (await loadTemplates(classified.folderPath, contentType)).filter(
      (row) => row && String(row.id ?? "").trim().length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      itemId: classified.itemId,
      currentId: storedTemplateId(fields, choices),
      choices,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type ChangePageTemplateSave =
  | { status: "saved"; templateId: string }
  | { status: "gate"; reason: "blank" | "unchanged" | "forbidden" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

export async function saveChangePageTemplate(input: {
  itemId: string;
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
  change?: (pageId: string, templateId: string) => Promise<void>;
}): Promise<ChangePageTemplateSave> {
  const gate = gateExplorerPageTemplateChange(input);
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const change = input.change ?? changePageTemplate;
  try {
    await change(input.itemId, gate.templateId);
    return { status: "saved", templateId: gate.templateId };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export interface ShownPageTemplate {
  templateId: string;
  templateName: string;
}

/**
 * The selection shows a template only after a successful save.
 * HTTP 400/403/409 and client gates keep the previous template.
 */
export function pageTemplateShownAfterSave(
  previous: ShownPageTemplate | undefined,
  save: ChangePageTemplateSave,
  templateName: string,
): ShownPageTemplate | undefined {
  if (save.status !== "saved") {
    return previous;
  }
  const templateId = save.templateId.trim();
  if (!templateId) {
    return previous;
  }
  return {
    templateId,
    templateName: templateName.trim() || templateId,
  };
}

function httpBucket(err: unknown): 400 | 403 | 409 | "other" {
  if (isApiError(err)) {
    if (err.status === 400 || err.status === 403 || err.status === 409) {
      return err.status;
    }
  }
  return "other";
}
