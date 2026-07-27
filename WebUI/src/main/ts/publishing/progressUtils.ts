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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Progress percent for status display. Returns null when total is missing/zero.
 */
export function progressPercent(
  completed: number | null | undefined,
  total: number | null | undefined,
): number | null {
  if (total == null || total <= 0 || completed == null || completed < 0) {
    return null;
  }
  const pct = Math.floor((completed / total) * 100);
  if (pct < 0) {
    return 0;
  }
  if (pct > 100) {
    return 100;
  }
  return pct;
}

export function formatProgressLabel(
  completed: number | null | undefined,
  total: number | null | undefined,
): string {
  const pct = progressPercent(completed, total);
  if (pct == null) {
    if (completed != null && total != null) {
      return `${completed} / ${total}`;
    }
    return "—";
  }
  return `${pct}% (${completed ?? 0}/${total})`;
}
