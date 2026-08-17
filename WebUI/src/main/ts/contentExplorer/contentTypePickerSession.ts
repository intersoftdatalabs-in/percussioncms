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

import type { ContentTypeChoice } from "./contentTypeChoices";

export interface ContentTypePickerSession {
  types: ContentTypeChoice[];
  resolve: (name: string | null) => void;
}

/**
 * Open a new type-picker session. Cancels any previous waiter so
 * {@code dispatchAction} does not hang if the user starts another create
 * before the first picker settles.
 */
export function replaceContentTypePickerSession(
  previous: ContentTypePickerSession | null,
  next: ContentTypePickerSession,
): ContentTypePickerSession {
  if (previous && previous !== next) {
    previous.resolve(null);
  }
  return next;
}

/**
 * Settle the current session (pick or cancel). Safe if {@code session} is
 * already null. The caller should drop its ref after this returns.
 */
export function settleContentTypePickerSession(
  session: ContentTypePickerSession | null,
  name: string | null,
): null {
  session?.resolve(name);
  return null;
}
