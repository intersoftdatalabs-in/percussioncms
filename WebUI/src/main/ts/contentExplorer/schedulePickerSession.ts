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

import type { ItemScheduleDates } from "./itemScheduleDates";

export interface SchedulePickerSession {
  current: ItemScheduleDates;
  /** Publishable rows this dialog will write. 1 is the single-item path. */
  applyCount: number;
  resolve: (dates: ItemScheduleDates | null) => void;
}

export function replaceSchedulePickerSession(
  previous: SchedulePickerSession | null,
  next: SchedulePickerSession,
): SchedulePickerSession {
  if (previous && previous !== next) {
    previous.resolve(null);
  }
  return next;
}

export function settleSchedulePickerSession(
  session: SchedulePickerSession | null,
  dates: ItemScheduleDates | null,
): null {
  session?.resolve(dates);
  return null;
}
