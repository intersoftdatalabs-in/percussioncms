/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/** Message keys for Developer module (fallback to English string until TMX). */
export const DEV_MSG = {
  TITLE: "Developer",
  INTRO:
    "Design-time tools for content types, assembly, and related CMS objects. Replaces the classic Workbench / Design surfaces.",
  SESSION_REDIRECT: "Session expired — redirecting to login…",
  TAB_CONTENT_TYPES: "Content Types",
  TAB_TEMPLATES: "Templates",
  TAB_SLOTS: "Slots",
  TAB_KEYWORDS: "Keywords",
  TAB_COMMUNITIES: "Communities",
  TAB_PIPELINES: "Pipelines",
  CT_LOADING: "Loading content types…",
  CT_EMPTY: "No content types returned.",
  CT_ERROR: "Could not load content types.",
  CT_COL_NAME: "Name",
  CT_COL_LABEL: "Label",
  CT_COL_DESCRIPTION: "Description",
  CT_COL_ID: "Id",
  CT_HINT:
    "Select a content type to view its field catalog. Editing rules/workflows is not available yet.",
  CT_BACK: "Back to list",
  CT_DETAIL_LOADING: "Loading content type…",
  CT_DETAIL_ERROR: "Could not load content type detail.",
  CT_FIELDS: "Fields",
  CT_CHILD_SETS: "Child field sets",
  CT_GAPS: "Design gaps (not in this API yet)",
  CT_META_ENABLED: "Enabled",
  CT_META_HIDDEN: "Hidden from menu",
  CT_META_APP: "Application",
  CT_COL_FIELD: "Field",
  CT_COL_ORIGIN: "Origin",
  CT_COL_DATATYPE: "Data type",
  CT_COL_CONTROL: "Control",
  CT_COL_REQUIRED: "Required",
  CT_COL_READONLY: "Read-only",
  CT_COL_OCCURRENCE: "Occurrence",
  CT_COL_RULES: "Rules",
  CT_COL_SEARCH: "Searchable",
  CT_COL_FIELDSET: "Field set",
  CT_RULE_VALIDATION: "validation",
  CT_RULE_VISIBILITY: "visibility",
  CT_RULE_IN_XFORM: "in-xform",
  CT_RULE_OUT_XFORM: "out-xform",
  CT_WORKFLOWS: "Allowed workflows",
  CT_DEFAULT_WF: "Default workflow",
  CT_TEMPLATES: "Allowed templates",
  CT_NONE: "None",
  YES: "Yes",
  NO: "No",
  KW_LOADING: "Loading keywords…",
  KW_EMPTY: "No keywords returned.",
  KW_ERROR: "Could not load keywords.",
  KW_HINT:
    "Keyword catalog (read-only). Create/edit/delete remains a later slice.",
  KW_COL_LABEL: "Label",
  KW_COL_VALUE: "Value",
  KW_COL_CHOICES: "Choices",
  KW_COL_DESCRIPTION: "Description",
  TPL_LOADING: "Loading templates…",
  TPL_EMPTY: "No templates returned.",
  TPL_ERROR: "Could not load templates.",
  TPL_HINT: "Assembly template catalog (read-only). Editor comes later.",
  TPL_COL_LABEL: "Label",
  TPL_COL_NAME: "Name",
  TPL_COL_ID: "Id",
  TPL_COL_DESCRIPTION: "Description",
  SLOT_LOADING: "Loading slots…",
  SLOT_EMPTY: "No slots returned.",
  SLOT_ERROR: "Could not load slots.",
  SLOT_HINT: "Assembly slot catalog (read-only).",
  SLOT_COL_LABEL: "Label",
  SLOT_COL_NAME: "Name",
  SLOT_COL_DESCRIPTION: "Description",
  COMM_LOADING: "Loading communities…",
  COMM_EMPTY: "No communities returned.",
  COMM_ERROR: "Could not load communities.",
  COMM_HINT: "Community catalog (read-only list). Role membership / ACL edit later.",
  COMM_COL_LABEL: "Label",
  COMM_COL_NAME: "Name",
  COMM_COL_ID: "Id",
  COMM_COL_DESCRIPTION: "Description",
  PLACEHOLDER_TITLE: "Not implemented yet",
  PLACEHOLDER_BODY:
    "This section is planned for Developer P0. See docs/ai-generated/tasks/developer-module-p0 and docs/developer-module.",
} as const;
