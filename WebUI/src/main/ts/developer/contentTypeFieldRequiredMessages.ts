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
 * Local-field required chrome for Developer → Content types (#5465).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const CT_FIELD_REQUIRED_MSG_KEYS = {
  ACTION: "perc.ui.developer@Mark field required",
  HINT: "perc.ui.developer@Marks one local field required, or clears that flag, with the content type field update. Other fields stay. The content type label and the description stay. Shared and system fields have no required editor. The new flag shows only after that save succeeds. Cancel does not write.",
  NONE: "perc.ui.developer@Shared and system fields have no required editor.",
  FIELD: "perc.ui.developer@Local field",
  REQUIRED: "perc.ui.developer@Required",
  NOT_REQUIRED: "perc.ui.developer@Not required",
  SAVE: "perc.ui.developer@Save required",
  SAVED: "perc.ui.developer@Field required saved.",
  CLEARED: "perc.ui.developer@Field required cleared.",
  ERROR: "perc.ui.developer@Could not change the field required flag.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a field required flag.",
  INVALID: "perc.ui.developer@The field required flag was not changed.",
  CONFLICT:
    "perc.ui.developer@The field required flag was not changed. The previous required flag is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
  NOT_LOCAL: "perc.ui.developer@Shared and system fields have no required editor.",
  MISSING: "perc.ui.developer@Choose one local field.",
} as const;

type ContentTypeFieldRequiredMsgKey = keyof typeof CT_FIELD_REQUIRED_MSG_KEYS;

export const CT_FIELD_REQUIRED_MSG: {
  readonly [K in ContentTypeFieldRequiredMsgKey]: string;
} = new Proxy(CT_FIELD_REQUIRED_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as ContentTypeFieldRequiredMsgKey]);
  },
}) as { readonly [K in ContentTypeFieldRequiredMsgKey]: string };
