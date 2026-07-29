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
    "List is read-only for now. Full field editor (validations, workflows, templates) is the next P0 slice.",
  PLACEHOLDER_TITLE: "Not implemented yet",
  PLACEHOLDER_BODY:
    "This section is planned for Developer P0. See docs/ai-generated/tasks/developer-module-p0 and docs/developer-module.",
} as const;
