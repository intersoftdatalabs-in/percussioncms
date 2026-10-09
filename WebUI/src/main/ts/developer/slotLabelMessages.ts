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
 * Slot label chrome for Developer → Slots (#5431).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land without
 * editing that shared catalog. English after {@code @} is the offline fallback.
 */
export const SLOT_LABEL_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set slot label",
  HINT: "perc.ui.developer@Sets the slot label with the slot update. The new label shows only after that save succeeds. The name stays. The description, type, and finder stay because this update does not send them. A blank label does not clear the slot name. Cancel does not write.",
  FIELD: "perc.ui.developer@Slot label",
  SAVE: "perc.ui.developer@Save label",
  SAVED: "perc.ui.developer@Slot label saved.",
  CLEARED: "perc.ui.developer@Slot label cleared. The slot name is unchanged.",
  ERROR: "perc.ui.developer@Could not change the slot label.",
  FORBIDDEN: "perc.ui.developer@You need a signed-in session to change a slot label.",
  INVALID: "perc.ui.developer@The slot label was not changed.",
  CONFLICT:
    "perc.ui.developer@The slot label was not changed. The previous label is unchanged.",
  CANCEL: "perc.ui.developer@Cancel",
} as const;

type SlotLabelMsgKey = keyof typeof SLOT_LABEL_MSG_KEYS;

export const SLOT_LABEL_MSG: { readonly [K in SlotLabelMsgKey]: string } = new Proxy(
  SLOT_LABEL_MSG_KEYS,
  {
    get(target, prop: string | symbol) {
      if (typeof prop !== "string" || !(prop in target)) {
        return undefined;
      }
      return message(target[prop as SlotLabelMsgKey]);
    },
  },
) as { readonly [K in SlotLabelMsgKey]: string };
