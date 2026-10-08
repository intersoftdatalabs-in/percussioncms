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
 * Keyword-label chrome for Developer → Keywords (#5379).
 *
 * <p>Kept out of {@code messages.ts} so sibling keyword slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const KW_LABEL_MSG_KEYS = {
  ACTION: "perc.ui.developer@Change keyword label",
  HINT: "perc.ui.developer@Changes the keyword label with the keyword update. The new label shows only after that save succeeds. The keyword description and sequence stay. Choices stay because this update does not send them. A blank label is not saved. This is not a choice label. Cancel does not write.",
  FIELD: "perc.ui.developer@Keyword label",
  SAVE: "perc.ui.developer@Save keyword label",
  SAVED: "perc.ui.developer@Keyword label saved.",
  ERROR: "perc.ui.developer@Could not change the keyword label.",
  BLANK: "perc.ui.developer@Enter a keyword label. A blank label was not saved.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a keyword label.",
  INVALID: "perc.ui.developer@The keyword label was not changed.",
  CONFLICT:
    "perc.ui.developer@The keyword label was not changed. The previous label is unchanged.",
} as const;

type KeywordLabelMsgKey = keyof typeof KW_LABEL_MSG_KEYS;

export const KW_LABEL_MSG: { readonly [K in KeywordLabelMsgKey]: string } = new Proxy(
  KW_LABEL_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as KeywordLabelMsgKey]);
    },
  },
) as { readonly [K in KeywordLabelMsgKey]: string };
