/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Default opener for explorer selections — navigates to the product editor
 * view using path-first (with id fallback). Mirrors the pattern used by
 * {@code WebUI/src/main/ts/home/HomeShell.tsx} defaultOpenItem so that the
 * explorer and Home shell open items through the same target.
 */

import type { PSPathItem } from "../api/contentExplorer/types";

const EDITOR_BASE = "/cm/app/?view=editor";

export function openInEditor(item: PSPathItem): void {
  const path = (item.path ?? "").trim();
  const id = (item.id ?? "").trim();
  if (path) {
    window.location.href = `${EDITOR_BASE}&path=${encodeURIComponent(path)}`;
    return;
  }
  if (id) {
    window.location.href = `${EDITOR_BASE}&id=${encodeURIComponent(id)}`;
  }
}