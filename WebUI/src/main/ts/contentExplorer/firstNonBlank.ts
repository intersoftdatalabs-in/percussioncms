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
 * First non-null, non-whitespace string among {@code values}.
 *
 * Shared by Explorer selection keys and clipboard mapping so the
 * trim / stringify contract cannot drift.
 */
export function firstNonBlank(
  ...values: ReadonlyArray<string | number | null | undefined>
): string | null {
  for (const value of values) {
    if (value == null) continue;
    const text = String(value).trim();
    if (text.length > 0) return text;
  }
  return null;
}
