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
 * Page template on an open EditorHost page (#4839).
 * Choices come from {@link loadPageTemplates}. Save writes the existing
 * {@code templateid} item field and the existing page {@code changeTemplate} API.
 */

import { put } from "../api/client";
import { detectServicesRoot } from "../api/paths";
import type { PageTemplateChoice } from "./pageTemplates";

/** Content field the page DAO persists as {@code PSPage.templateId}. */
export const PAGE_TEMPLATE_FIELD = "templateid";

export function pageTemplateIdFromFields(
  fields: readonly { name: string; value: string }[] | null | undefined,
): string {
  for (const row of fields ?? []) {
    if (row.name.trim().toLowerCase() === PAGE_TEMPLATE_FIELD) {
      return String(row.value ?? "").trim();
    }
  }
  return "";
}

/**
 * Prefer the stored id when it is an allowed choice (by id or display name).
 * Otherwise the first allowed choice, or empty when there are none.
 */
export function resolvePageTemplateSelection(
  currentId: string,
  choices: readonly PageTemplateChoice[],
): string {
  const current = currentId.trim();
  if (choices.length === 0) {
    return "";
  }
  if (current) {
    const byId = choices.find((choice) => choice.id === current);
    if (byId) {
      return byId.id;
    }
    const byName = choices.find(
      (choice) => choice.name.trim().toLowerCase() === current.toLowerCase(),
    );
    if (byName) {
      return byName.id;
    }
  }
  return choices[0].id;
}

export function isAllowedPageTemplate(
  templateId: string,
  choices: readonly PageTemplateChoice[],
): boolean {
  const id = templateId.trim();
  return id.length > 0 && choices.some((choice) => choice.id === id);
}

/** Replace or append {@code templateid} so the editor field PUT persists it. */
export function withPageTemplateField<T extends { name: string; value: string }>(
  fields: readonly T[],
  templateId: string,
): T[] {
  const id = templateId.trim();
  if (!id) {
    return [...fields];
  }
  let replaced = false;
  const next = fields.map((row) => {
    if (row.name.trim().toLowerCase() !== PAGE_TEMPLATE_FIELD) {
      return row;
    }
    replaced = true;
    return { ...row, name: PAGE_TEMPLATE_FIELD, value: id };
  });
  if (!replaced) {
    next.push({ name: PAGE_TEMPLATE_FIELD, value: id } as T);
  }
  return next;
}

export function pageChangeTemplatePath(pageId: string, templateId: string): string {
  const page = encodeURIComponent(pageId.trim());
  const template = encodeURIComponent(templateId.trim());
  return `${detectServicesRoot()}/pagemanagement/page/changeTemplate/${page}/${template}`;
}

/** Existing page service. Does not create a second template store. */
export async function changePageTemplate(
  pageId: string,
  templateId: string,
): Promise<void> {
  await put(pageChangeTemplatePath(pageId, templateId), {});
}
