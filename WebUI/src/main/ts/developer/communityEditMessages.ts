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

import { message } from "../i18n/message";

/**
 * Community rename/description editor chrome for Developer → Communities
 * (#6906).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land
 * without editing that shared catalog. English after {@code @} is the
 * offline fallback.</p>
 */
export const COMM_EDIT_MSG_KEYS = {
  RENAME_EDIT: "perc.ui.developer@Edit name",
  DESCRIPTION_EDIT: "perc.ui.developer@Edit description",
} as const;

type CommEditMsgKey = keyof typeof COMM_EDIT_MSG_KEYS;

export const COMM_EDIT_MSG: { readonly [K in CommEditMsgKey]: string } = new Proxy(
  COMM_EDIT_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as CommEditMsgKey]);
    },
  },
) as { readonly [K in CommEditMsgKey]: string };
