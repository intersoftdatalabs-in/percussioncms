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
 * Field searchable chrome for Developer → Content types (#5466).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const CT_FIELD_SEARCHABLE_MSG_KEYS = {
  ACTION: "perc.ui.developer@Mark field searchable",
  HINT: "perc.ui.developer@Marks one field searchable, or clears that flag, with the content type field update. Other fields stay. The type-level search indexing flag stays. The new flag shows only after that save succeeds. Cancel does not write.",
  NONE: "perc.ui.developer@This content type has no fields.",
  FIELD: "perc.ui.developer@Field",
  SEARCHABLE: "perc.ui.developer@Searchable",
  NOT_SEARCHABLE: "perc.ui.developer@Not searchable",
  SAVE: "perc.ui.developer@Save searchable",
  SAVED: "perc.ui.developer@Field searchable saved.",
  CLEARED: "perc.ui.developer@Field searchable cleared.",
  ERROR: "perc.ui.developer@Could not change the field searchable flag.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a field searchable flag.",
  INVALID: "perc.ui.developer@The field searchable flag was not changed.",
  CONFLICT:
    "perc.ui.developer@The field searchable flag was not changed. The previous searchable flag is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
  MISSING: "perc.ui.developer@Choose one field.",
} as const;

type ContentTypeFieldSearchableMsgKey = keyof typeof CT_FIELD_SEARCHABLE_MSG_KEYS;

export const CT_FIELD_SEARCHABLE_MSG: {
  readonly [K in ContentTypeFieldSearchableMsgKey]: string;
} = new Proxy(CT_FIELD_SEARCHABLE_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as ContentTypeFieldSearchableMsgKey]);
  },
}) as { readonly [K in ContentTypeFieldSearchableMsgKey]: string };
