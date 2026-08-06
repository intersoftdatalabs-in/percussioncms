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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Pure multi-select helpers for ContentBrowser (992 US2).
 * Extracted so activation-path dedupe is unit-testable without mounting the
 * full ExplorerTree/DetailList shell.
 */

export type SelectableId = { id: string };

/**
 * Append {@code item} to a multi-select list if its id is not already present.
 * Repeated activate (double-click / Enter) on the same row is a no-op.
 */
export function appendUniqueById<T extends SelectableId>(
  prev: ReadonlyArray<T>,
  item: T,
): T[] {
  if (prev.some((s) => s.id === item.id)) {
    return prev.slice();
  }
  return [...prev, item];
}
