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
 * Folder display-format catalog for Explorer (#5131). {@code id} is the value
 * stored in {@code sys_displayformat}. {@code name} is the catalog label and is
 * not persisted. Distinct from the list-column chooser.
 */

import { get } from "../client";
import { PATHS } from "../paths";

export interface FolderDisplayFormatChoice {
  id: string;
  name: string;
}

export interface FolderDisplayFormatCatalog {
  choices: FolderDisplayFormatChoice[];
}

const CATALOG_ROOTS = [
  "FolderDisplayFormatCatalog",
  "folderDisplayFormatCatalog",
] as const;

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asText(value: unknown): string {
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value);
  }
  if (typeof value === "string") {
    return value.trim();
  }
  return "";
}

function coerceChoices(value: unknown): FolderDisplayFormatChoice[] {
  const list = Array.isArray(value)
    ? value
    : (() => {
        const wrapped = asRecord(value);
        if (!wrapped) {
          return [];
        }
        const nested =
          wrapped.FolderDisplayFormatChoice ?? wrapped.folderDisplayFormatChoice;
        if (Array.isArray(nested)) {
          return nested;
        }
        if (nested != null) {
          return [nested];
        }
        return [];
      })();
  const out: FolderDisplayFormatChoice[] = [];
  for (const entry of list) {
    const row = asRecord(entry);
    if (!row) {
      continue;
    }
    const id = asText(row.id);
    if (!id) {
      continue;
    }
    const name = asText(row.name) || id;
    out.push({ id, name });
  }
  return out;
}

/** Accepts a Jackson root wrap or a flat {@code choices} array. */
export function unwrapFolderDisplayFormatCatalog(
  data: unknown,
): FolderDisplayFormatCatalog {
  const root = asRecord(data);
  if (!root) {
    return { choices: [] };
  }
  let body = root;
  for (const name of CATALOG_ROOTS) {
    const nested = asRecord(root[name]);
    if (nested) {
      body = nested;
      break;
    }
  }
  return { choices: coerceChoices(body.choices) };
}

export async function listFolderDisplayFormatCatalog(): Promise<FolderDisplayFormatCatalog> {
  const data = await get<unknown>(PATHS.PATH_FOLDER_DISPLAY_FORMAT_CATALOG);
  return unwrapFolderDisplayFormatCatalog(data);
}
