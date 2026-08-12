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
 * Architecture / Navigation SPA chrome keys (#3094 / parent #3092).
 * English after {@code @} is the source fallback when TMX is not loaded.
 * Small key set only — no multi-locale mass TMX backfill in this slice.
 */
const KEYS = {
  TITLE: "perc.ui.architecture.modern@Architecture",
  INTRO:
    "perc.ui.architecture.modern@Manage site navigation trees (navons / sections). The modern navigation editor is rolling out under this product shell.",
  SHELL_LOADING: "perc.ui.architecture.modern@Loading Architecture…",
  EMPTY_TITLE: "perc.ui.architecture.modern@Navigation editor coming soon",
  EMPTY_BODY:
    "perc.ui.architecture.modern@This SPA shell replaces the legacy Architecture page as the primary product entry. Browse and edit navigation trees will land in follow-on slices. Select Architecture from the top nav or open a site deep link when the tree is ready.",
  SITE_HINT: "perc.ui.architecture.modern@Site context: {0}",
  SITE_NONE:
    "perc.ui.architecture.modern@No site selected yet. Site picker and nav tree land in the next Architecture slices.",
} as const;

export type ArchitectureMsgKey = keyof typeof KEYS;

/** Stable message-key map (tests / i18n key attributes). */
export const ARCH_MSG_KEYS = KEYS;

/** Resolved English-fallback strings for Architecture shell chrome. */
export const ARCH_MSG: { readonly [K in ArchitectureMsgKey]: string } = {
  TITLE: message(KEYS.TITLE),
  INTRO: message(KEYS.INTRO),
  SHELL_LOADING: message(KEYS.SHELL_LOADING),
  EMPTY_TITLE: message(KEYS.EMPTY_TITLE),
  EMPTY_BODY: message(KEYS.EMPTY_BODY),
  SITE_HINT: message(KEYS.SITE_HINT),
  SITE_NONE: message(KEYS.SITE_NONE),
};
