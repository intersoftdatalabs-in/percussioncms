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
 * EditorHost check-out / check-in (#4603). HTTP 403 and 409 are failures.
 */

import { isApiError } from "../api/client";
import type { EditorHostMode } from "./editorHostUrl";

export interface EditorCheckoutUserInfo {
  checkOutUser?: string;
  currentUser?: string;
  itemName?: string;
  assignmentType?: string;
}

export type EditorLockErrorReason = "forbidden" | "conflict" | "failed";

function namesEqual(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

/**
 * True when the item is checked out to the session user.
 *
 * <p>A successful check-out with an empty user-info body (legacy stubs) is
 * treated as checked out to self when {@code fieldsCheckoutUser} is set.</p>
 */
export function isCheckedOutToSelf(
  checkoutUser: string | undefined,
  currentUser: string | undefined,
  fieldsCheckoutUser?: string,
  allowEmptySession = false,
): boolean {
  const infoLock = (checkoutUser ?? "").trim();
  const lockUser = infoLock || (fieldsCheckoutUser ?? "").trim();
  const session = (currentUser ?? "").trim();
  if (!lockUser) {
    return false;
  }
  if (!session) {
    // Legacy empty user-info after a successful check-out (no named holder in
    // the response). Do not treat another named holder as self.
    return allowEmptySession && !infoLock;
  }
  return namesEqual(lockUser, session);
}

/** Check-out / check-in chrome is for edit mode only. */
export function canUseEditorCheckoutActions(mode: EditorHostMode): boolean {
  return mode === "edit";
}

export function editorLockErrorReason(err: unknown): EditorLockErrorReason {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 409) {
      return "conflict";
    }
  }
  return "failed";
}
