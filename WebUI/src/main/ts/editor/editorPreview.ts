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
 * Preview the assembled page/asset from the React Content Editor host
 * (#4568 / parent #4532).
 *
 * <p>Reuses Explorer {@link openPreviewItem} (Page Management render for
 * pages, asset view URL for assets). Preview is the last saved revision —
 * unsaved field edits are not assembled. HTTP 403 / 404 is a failure, not
 * success. Does not open leftover Content Editor HTML or Active Assembly.</p>
 */

import { get } from "../api/client";
import type { PSPathItem } from "../api/contentExplorer/types";
import { SERVICES_ROOT, withCmsContextPrefix } from "../api/paths";
import {
  openPreviewItem,
  resolvePreviewTarget,
  type PreviewOpenDeps,
} from "../contentExplorer/previewItem";
import type { EditorHostMode } from "./editorHostUrl";
import type { EditorPublishKind } from "./editorPublish";

export interface EditorPreviewDeps extends PreviewOpenDeps {
  /**
   * GET the page render URL before {@code window.open}. Throws on 403/404
   * so forbidden / unknown ids are not treated as success.
   */
  probeUrl?: (url: string) => Promise<void>;
}

function fieldAsString(value: unknown): string {
  return value == null ? "" : String(value);
}

/** Build a path item Explorer preview can classify from editor id + kind. */
export function editorPreviewPathItem(
  itemId: string,
  kind: EditorPublishKind,
  name?: string,
): PSPathItem {
  const id = itemId.trim();
  return {
    id,
    name: (name ?? "").trim() || id,
    path: kind === "asset" ? `/Assets/${id}` : `/Sites/${id}`,
    type: kind === "asset" ? "asset" : "page",
  };
}

/** View and edit may preview; promote stays on the restore form. */
export function canPreviewFromEditor(
  mode: EditorHostMode,
  kind: EditorPublishKind,
): boolean {
  return (
    (mode === "edit" || mode === "view") &&
    (kind === "page" || kind === "asset")
  );
}

/**
 * True when the form differs from the last loaded/saved field map or has a
 * pending binary. Preview still uses the last saved revision.
 */
export function editorDraftIsDirty(
  saved: ReadonlyArray<{ name: string; value?: unknown }> | undefined,
  draft: Record<string, string>,
  pendingFiles: Record<string, File>,
): boolean {
  if (Object.keys(pendingFiles).length > 0) {
    return true;
  }
  if (!saved) {
    return false;
  }
  for (const field of saved) {
    const current = Object.prototype.hasOwnProperty.call(draft, field.name)
      ? draft[field.name]
      : fieldAsString(field.value);
    if (current !== fieldAsString(field.value)) {
      return true;
    }
  }
  return false;
}

async function defaultProbe(url: string): Promise<void> {
  await get<unknown>(url);
}

/**
 * Open assembled preview for the open editor item. Throws when kind/id is
 * missing, when the probe GET is 403/404, or when the asset view URL fails.
 */
export async function previewEditorItem(
  itemId: string,
  kind: EditorPublishKind,
  deps: EditorPreviewDeps = {},
  name?: string,
): Promise<void> {
  const id = itemId.trim();
  if (!id || kind === "none") {
    throw new Error("Preview is not available for this item");
  }
  const item = editorPreviewPathItem(id, kind, name);
  const servicesRoot = deps.servicesRoot ?? SERVICES_ROOT;
  const target = resolvePreviewTarget(item, servicesRoot);
  if (target.kind === "none" || !target.url) {
    throw new Error("Preview is not available for this item");
  }
  if (kind === "page" && !target.needsFetch) {
    const probe = deps.probeUrl ?? defaultProbe;
    await probe(withCmsContextPrefix(target.url));
  }
  await openPreviewItem(item, deps);
}
