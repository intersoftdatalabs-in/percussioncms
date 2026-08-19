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
    "perc.ui.design.modern@Browse modern templates from the design catalog. Create or delete a template without Widget XML, or open a row to edit assembler, slots, source, and JEXL bindings.",
  SHELL_LOADING: "perc.ui.design.modern@Loading Design…",
  TAB_TEMPLATES: "perc.ui.design.modern@Templates",
  TPL_HINT:
    "perc.ui.design.modern@Assembly templates available in this CMS. Create or delete a modern template (no Widget XML), or open a row to edit assembler, slots, source, and JEXL bindings.",
  TPL_CREATE: "perc.ui.design.modern@Create template",
  TPL_CREATE_ARIA: "perc.ui.design.modern@Create a new assembly template",
  TPL_CREATE_TITLE: "perc.ui.design.modern@Create template",
  TPL_CREATE_HINT:
    "perc.ui.design.modern@Creates a modern assembly template in the catalog. No Widget definition XML is written.",
  TPL_CREATE_NAME: "perc.ui.design.modern@Name",
  TPL_CREATE_LABEL: "perc.ui.design.modern@Label",
  TPL_CREATE_DESCRIPTION: "perc.ui.design.modern@Description",
  TPL_CREATE_ASSEMBLER: "perc.ui.design.modern@Assembler",
  TPL_CREATE_SUBMIT: "perc.ui.design.modern@Create",
  TPL_CREATE_CANCEL: "perc.ui.design.modern@Cancel",
  TPL_CREATE_ERROR: "perc.ui.design.modern@Could not create template.",
  TPL_CREATE_NAME_REQUIRED: "perc.ui.design.modern@Name is required.",
  TPL_CREATE_NAME_SPACES: "perc.ui.design.modern@Name cannot contain spaces.",
  TPL_CREATE_NAME_FORMAT:
    "perc.ui.design.modern@Name must start with a letter and use only letters, digits, '.', '_' or '-'.",
  TPL_CREATE_ASSEMBLER_REQUIRED: "perc.ui.design.modern@Choose an assembler.",
  TPL_DELETE: "perc.ui.design.modern@Delete",
  TPL_DELETE_ARIA: "perc.ui.design.modern@Delete template {0}",
  TPL_DELETE_TITLE: "perc.ui.design.modern@Delete template",
  TPL_DELETE_HINT:
    "perc.ui.design.modern@Permanently removes this assembly template from the catalog. No Widget definition XML is written.",
  TPL_DELETE_CONFIRM: "perc.ui.design.modern@Delete {0}? This cannot be undone.",
  TPL_DELETE_SUBMIT: "perc.ui.design.modern@Delete",
  TPL_DELETE_CANCEL: "perc.ui.design.modern@Cancel",
  TPL_DELETE_ERROR: "perc.ui.design.modern@Could not delete template.",
  TPL_LOADING: "perc.ui.design.modern@Loading templates…",
  TPL_EMPTY: "perc.ui.design.modern@No templates found.",
  TPL_ERROR: "perc.ui.design.modern@Could not load templates.",
  TPL_COL_LABEL: "perc.ui.design.modern@Label",
  TPL_COL_NAME: "perc.ui.design.modern@Name",
  TPL_COL_ID: "perc.ui.design.modern@Id",
  TPL_COL_DESCRIPTION: "perc.ui.design.modern@Description",
  TPL_COL_ACTIONS: "perc.ui.design.modern@Actions",
  TPL_OPEN_ARIA: "perc.ui.design.modern@Edit template assembler, slots, source, and bindings for {0}",
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
  EDITOR_SAVED: "perc.ui.design.modern@Template assembler, slots, source, and bindings saved.",
  EDITOR_SAVE: "perc.ui.design.modern@Save",
  EDITOR_DELETE: "perc.ui.design.modern@Delete",
  EDITOR_DELETE_ARIA: "perc.ui.design.modern@Delete this template",
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
  EDITOR_ASSEMBLER: "perc.ui.design.modern@Assembler",
  EDITOR_ASSEMBLER_HINT:
    "perc.ui.design.modern@Choose the render assembler for this template (HTML-first, Markdown, Velocity, …). See Design assemblers help for guidance.",
  EDITOR_ASSEMBLER_ARIA: "perc.ui.design.modern@Select template assembler",
  EDITOR_SLOTS: "perc.ui.design.modern@Slots (layout and styles)",
  EDITOR_SLOTS_HINT:
    "perc.ui.design.modern@Template holes with ADR-003 slot_layout and slot_styles. Edit orientation, columns, classes, then save.",
  EDITOR_SLOTS_EMPTY: "perc.ui.design.modern@This template has no slots.",
  EDITOR_SLOTS_LOADING: "perc.ui.design.modern@Loading slot details.",
  EDITOR_SLOTS_ERROR: "perc.ui.design.modern@Could not load slot details.",
  EDITOR_SLOT_SAVE_ERROR: "perc.ui.design.modern@Could not save slot layout/styles.",
  EDITOR_SLOT_LAYOUT: "perc.ui.design.modern@Layout",
  EDITOR_SLOT_STYLES: "perc.ui.design.modern@Styles",
  EDITOR_SLOT_ORIENTATION: "perc.ui.design.modern@Orientation",
  EDITOR_SLOT_COLUMNS: "perc.ui.design.modern@Columns",
  EDITOR_SLOT_MAX_ITEMS: "perc.ui.design.modern@Max items",
  EDITOR_SLOT_EMPTY_STATE: "perc.ui.design.modern@Empty state",
  EDITOR_SLOT_WRAPPER: "perc.ui.design.modern@Wrapper class policy",
  EDITOR_SLOT_ROOTCLASS: "perc.ui.design.modern@Root class",
  EDITOR_SLOT_ITEMCLASS: "perc.ui.design.modern@Item class",
  EDITOR_SLOT_ORIENTATION_H: "perc.ui.design.modern@horizontal",
  EDITOR_SLOT_ORIENTATION_V: "perc.ui.design.modern@vertical",
  EDITOR_SLOT_ORIENTATION_NONE: "perc.ui.design.modern@(default)",
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
