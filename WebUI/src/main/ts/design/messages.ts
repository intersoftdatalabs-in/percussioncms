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
 * Design SPA chrome keys (#2808 / parent #2631).
 * English after {@code @} is the source fallback when TMX is not loaded.
 * Small key set only — no multi-locale mass TMX backfill in this slice.
 */
const KEYS = {
  TITLE: "perc.ui.design.modern@Design",
  INTRO:
    "perc.ui.design.modern@Browse modern templates from the design catalog. Open a row to edit source and JEXL bindings.",
  SHELL_LOADING: "perc.ui.design.modern@Loading Design…",
  TAB_TEMPLATES: "perc.ui.design.modern@Templates",
  TPL_HINT:
    "perc.ui.design.modern@Assembly templates available in this CMS. Open a template to edit source text and JEXL bindings.",
  TPL_LOADING: "perc.ui.design.modern@Loading templates…",
  TPL_EMPTY: "perc.ui.design.modern@No templates found.",
  TPL_ERROR: "perc.ui.design.modern@Could not load templates.",
  TPL_COL_LABEL: "perc.ui.design.modern@Label",
  TPL_COL_NAME: "perc.ui.design.modern@Name",
  TPL_COL_ID: "perc.ui.design.modern@Id",
  TPL_COL_DESCRIPTION: "perc.ui.design.modern@Description",
  TPL_OPEN_ARIA: "perc.ui.design.modern@Edit template source and bindings for {0}",
  DRAWER_TITLE: "perc.ui.design.modern@Template details",
  DRAWER_CLOSE: "perc.ui.design.modern@Close",
  DRAWER_CLOSE_ARIA: "perc.ui.design.modern@Close template details",
  DRAWER_LOADING: "perc.ui.design.modern@Loading template…",
  DRAWER_ERROR: "perc.ui.design.modern@Could not load template details.",
  DRAWER_READONLY: "perc.ui.design.modern@Read-only summary",
  FIELD_LABEL: "perc.ui.design.modern@Label",
  FIELD_NAME: "perc.ui.design.modern@Name",
  FIELD_ID: "perc.ui.design.modern@Id",
  FIELD_DESCRIPTION: "perc.ui.design.modern@Description",
  FIELD_ASSEMBLER: "perc.ui.design.modern@Assembler",
  FIELD_MIME: "perc.ui.design.modern@MIME type",
  FIELD_TYPE: "perc.ui.design.modern@Template type",
  FIELD_BINDINGS: "perc.ui.design.modern@Bindings",
  FIELD_SLOTS: "perc.ui.design.modern@Slots",
  FIELD_GAPS: "perc.ui.design.modern@Design gaps",
  EDITOR_BACK: "perc.ui.design.modern@Templates",
  EDITOR_BACK_ARIA: "perc.ui.design.modern@Back to template library",
  EDITOR_LOADING: "perc.ui.design.modern@Loading template…",
  EDITOR_LOAD_ERROR: "perc.ui.design.modern@Could not load template.",
  EDITOR_SAVE_ERROR: "perc.ui.design.modern@Could not save template.",
  EDITOR_SAVED: "perc.ui.design.modern@Template source and bindings saved.",
  EDITOR_SAVE: "perc.ui.design.modern@Save",
  EDITOR_SOURCE: "perc.ui.design.modern@Template source",
  EDITOR_SOURCE_HINT:
    "perc.ui.design.modern@Velocity / assembler source text for this template.",
  EDITOR_BINDINGS: "perc.ui.design.modern@JEXL bindings",
  EDITOR_BINDINGS_HINT:
    "perc.ui.design.modern@JEXL bindings executed in order. Add, edit, or remove rows, then save (full replace).",
  EDITOR_BINDINGS_EMPTY: "perc.ui.design.modern@No bindings yet.",
  EDITOR_COL_ORDER: "perc.ui.design.modern@Order",
  EDITOR_COL_VARIABLE: "perc.ui.design.modern@Variable",
  EDITOR_COL_EXPRESSION: "perc.ui.design.modern@Expression",
  EDITOR_COL_ACTIONS: "perc.ui.design.modern@Actions",
  EDITOR_BINDING_ADD: "perc.ui.design.modern@Add binding",
  EDITOR_BINDING_REMOVE: "perc.ui.design.modern@Remove",
  SESSION_REDIRECT: "perc.ui.design.modern@Session expired — sign in again.",
  NONE: "perc.ui.design.modern@—",
} as const;

export type DesignMsgKey = keyof typeof KEYS;

/** Resolved Design SPA strings (TMX when loaded, English fallback after @). */
export const DESIGN_MSG: { [K in DesignMsgKey]: string } = new Proxy(
  {} as { [K in DesignMsgKey]: string },
  {
    get(_t, prop: string) {
      const key = KEYS[prop as DesignMsgKey];
      if (!key) return prop;
      return message(key);
    },
  },
);

/** Catalog keys for tests that assert raw TMX key strings. */
export const DESIGN_MSG_KEYS = KEYS;
