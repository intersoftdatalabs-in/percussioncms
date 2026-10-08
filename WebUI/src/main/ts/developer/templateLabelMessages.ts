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
 * Template label chrome for Developer → Templates (#5407).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const TPL_LABEL_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set template label",
  HINT: "perc.ui.developer@Sets the template label with the template update. Lock the template first. The new label shows only after that save succeeds. The name stays. The description, bindings, slots, content-type associations, and source stay because this update does not send them. A blank label does not clear the template name. Cancel does not write.",
  FIELD: "perc.ui.developer@Template label",
  SAVE: "perc.ui.developer@Save label",
  SAVED: "perc.ui.developer@Template label saved.",
  CLEARED: "perc.ui.developer@Template label cleared. The template name is unchanged.",
  ERROR: "perc.ui.developer@Could not change the template label.",
  FORBIDDEN: "perc.ui.developer@You need the Admin role to change a template label.",
  INVALID: "perc.ui.developer@The template label was not changed.",
  CONFLICT:
    "perc.ui.developer@The template label was not changed. The previous label is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type TemplateLabelMsgKey = keyof typeof TPL_LABEL_MSG_KEYS;

export const TPL_LABEL_MSG: { readonly [K in TemplateLabelMsgKey]: string } = new Proxy(
  TPL_LABEL_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as TemplateLabelMsgKey]);
    },
  },
) as { readonly [K in TemplateLabelMsgKey]: string };
