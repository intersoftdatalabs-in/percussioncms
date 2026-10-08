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
 * Keyword-sequence chrome for Developer → Keywords (#5380).
 *
 * <p>Kept out of {@code messages.ts} so sibling keyword slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const KW_SEQUENCE_MSG_KEYS = {
  ACTION: "perc.ui.developer@Change keyword sequence",
  HINT: "perc.ui.developer@Changes the keyword sequence with the keyword update. The new sequence shows only after that save succeeds. The keyword label and description stay. Choices stay because this update does not send them. A blank or non-numeric sequence is not saved. This is not a choice sequence. Cancel does not write.",
  FIELD: "perc.ui.developer@Keyword sequence",
  SAVE: "perc.ui.developer@Save keyword sequence",
  SAVED: "perc.ui.developer@Keyword sequence saved.",
  ERROR: "perc.ui.developer@Could not change the keyword sequence.",
  BLANK: "perc.ui.developer@Enter a keyword sequence. A blank sequence was not saved.",
  INVALID:
    "perc.ui.developer@Enter a whole number from 0 through 2147483647. That sequence was not saved.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a keyword sequence.",
  INVALID_HTTP: "perc.ui.developer@The keyword sequence was not changed.",
  CONFLICT:
    "perc.ui.developer@The keyword sequence was not changed. The previous sequence is unchanged.",
} as const;

type KeywordSequenceMsgKey = keyof typeof KW_SEQUENCE_MSG_KEYS;

export const KW_SEQUENCE_MSG: { readonly [K in KeywordSequenceMsgKey]: string } = new Proxy(
  KW_SEQUENCE_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as KeywordSequenceMsgKey]);
    },
  },
) as { readonly [K in KeywordSequenceMsgKey]: string };
