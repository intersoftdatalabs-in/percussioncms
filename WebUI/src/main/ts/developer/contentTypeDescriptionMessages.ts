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
 * Content-type description chrome for Developer → Content types (#5433).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const CT_DESC_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set content type description",
  HINT: "perc.ui.developer@Sets the content type description with the content type update. The new description shows only after that save succeeds. The label stays. The enabled flag stays because this update does not send it. A blank description clears the description. Cancel does not write.",
  FIELD: "perc.ui.developer@Content type description",
  SAVE: "perc.ui.developer@Save description",
  SAVED: "perc.ui.developer@Content type description saved.",
  CLEARED: "perc.ui.developer@Content type description cleared.",
  ERROR: "perc.ui.developer@Could not change the content type description.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a content type description.",
  INVALID: "perc.ui.developer@The content type description was not changed.",
  CONFLICT:
    "perc.ui.developer@The content type description was not changed. The previous description is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type ContentTypeDescriptionMsgKey = keyof typeof CT_DESC_MSG_KEYS;

export const CT_DESC_MSG: { readonly [K in ContentTypeDescriptionMsgKey]: string } = new Proxy(
  CT_DESC_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as ContentTypeDescriptionMsgKey]);
    },
  },
) as { readonly [K in ContentTypeDescriptionMsgKey]: string };
