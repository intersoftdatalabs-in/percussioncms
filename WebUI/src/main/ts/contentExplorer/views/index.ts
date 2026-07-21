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

import React from "react";

/**
 * Re-export the inventory of available US7 components for the bridge.
 * The other contentExplorer components are also exposed via the central
 * `componentRegistry`; this re-export makes the inventory of US7
 * capabilities visible from one place.
 */
export const US7_COMPONENTS: ReadonlyArray<string> = [
  "ClipboardPanel",
  "SiteCopyWizard",
  "SubfolderCopyWizard",
  "DependencyViewer",
  "RelationshipsView",
];

/**
 * Placeholder barrel so the `contentExplorer/views/index.ts` path
 * resolves. Importing this file alone has no side effects beyond
 * typing the inventory above.
 */
export function Us7Placeholder(): null {
  return null;
}
