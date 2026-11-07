/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { message } from "../i18n/message";

/**
 * Per-locale auto-translation chrome for Developer → Locales (#5704).
 *
 * <p>Kept out of {@code messages.ts} so sibling Developer slices can land
 * without editing that shared catalog. English after {@code @} is the
 * offline fallback.</p>
 */
export const LOC_AT_LOCALE_MSG_KEYS = {
  ACTION: "perc.ui.developer@Set auto-translations for this locale",
  HINT: "perc.ui.developer@Shows only this locale's auto-translation rows. Save replaces this locale's rows; every other locale's rows stay. Removing every row and saving clears this locale. The language string, label, description, enabled status, and format profile stay because this update does not send them. Unknown content type is 400 and does not save. A design lock held by another user is 409.",
  FIELD: "perc.ui.developer@Auto-translations for this locale",
  COUNT_ZERO: "perc.ui.developer@No auto-translation rows for this locale.",
  COUNT_ONE: "perc.ui.developer@1 auto-translation row for this locale.",
  COUNT_MANY: "perc.ui.developer@{0} auto-translation rows for this locale.",
  LOADING: "perc.ui.developer@Loading auto-translations for this locale...",
  LOAD_ERROR: "perc.ui.developer@Could not load auto-translations for this locale.",
  CATALOG_ERROR:
    "perc.ui.developer@Could not load content type, workflow, or community choices.",
  EMPTY: "perc.ui.developer@No auto-translation rows for this locale. Add a row, or save to clear this locale.",
  ADD: "perc.ui.developer@Add row",
  REMOVE: "perc.ui.developer@Remove",
  SAVE: "perc.ui.developer@Save locale auto-translations",
  SAVED: "perc.ui.developer@Locale auto-translations saved.",
  CLEARED: "perc.ui.developer@Locale auto-translations cleared.",
  SAVE_ERROR: "perc.ui.developer@Could not save locale auto-translations.",
  UNKNOWN: "perc.ui.developer@Unknown locale or content type.",
  LOCK: "perc.ui.developer@Could not save locale auto-translations; the set is locked.",
  DUPLICATE: "perc.ui.developer@Duplicate content type for this locale.",
  COL_TYPE: "perc.ui.developer@Content type",
  COL_WORKFLOW: "perc.ui.developer@Workflow",
  COL_COMMUNITY: "perc.ui.developer@Community",
} as const;

type LocaleAtLocaleMsgKey = keyof typeof LOC_AT_LOCALE_MSG_KEYS;

export const LOC_AT_LOCALE_MSG: {
  readonly [K in LocaleAtLocaleMsgKey]: string;
} = new Proxy(LOC_AT_LOCALE_MSG_KEYS, {
  get(target, prop: string | symbol) {
    if (typeof prop !== "string" || !(prop in target)) {
      return undefined;
    }
    return message(target[prop as LocaleAtLocaleMsgKey]);
  },
}) as { readonly [K in LocaleAtLocaleMsgKey]: string };
