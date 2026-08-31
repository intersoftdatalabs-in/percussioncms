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
 * TMX catalog keys for the Developer module ({@code perc.ui.developer.*}).
 *
 * <p>English text lives after {@code @} for offline/test fallback via
 * {@link message}. Server translations are in {@code DeveloperUi.tmx}.
 */
export const DEV_MSG_KEYS = {
  TITLE: "perc.ui.developer@Developer",
  SHELL_LOADING: "perc.ui.developer@Loading Developer...",
  /**
   * Isolated tab/detail failure (#3377 / peer Admin #3195). Arg {0} is the section label.
   */
  SECTION_LOAD_FAILED:
    "perc.ui.developer@Unable to load {0}. Other Developer tabs remain available.",
  INTRO: "perc.ui.developer@Design-time tools for content types, assembly, and related CMS objects. Replaces the classic Workbench / Design surfaces.",
  SESSION_REDIRECT: "perc.ui.developer@Session expired - redirecting to login...",
  ACL_TITLE: "perc.ui.developer@Object ACL",
  ACL_HINT:
    "perc.ui.developer@Design-time and runtime access control for this object. Toggle Design access (Read, Update, Delete, Modify ACL) and Runtime visibility where applicable, add or remove entries, then save (full entry list replace via bulk ACL API).",
  ACL_LOADING: "perc.ui.developer@Loading ACL...",
  ACL_ERROR: "perc.ui.developer@Could not load object ACL.",
  ACL_EMPTY: "perc.ui.developer@No ACL defined for this object.",
  ACL_EMPTY_HINT: "perc.ui.developer@Create a design-time ACL with an owner principal. You can add more entries after it is created.",
  ACL_CREATE: "perc.ui.developer@Create ACL",
  ACL_CREATING: "perc.ui.developer@Creating...",
  ACL_CREATE_ERROR: "perc.ui.developer@Could not create object ACL.",
  ACL_OWNER_NAME: "perc.ui.developer@Owner principal",
  ACL_OWNER_NAME_PLACEHOLDER: "perc.ui.developer@User or role that owns the ACL",
  ACL_NO_GUID: "perc.ui.developer@Object GUID not available - cannot load ACL.",
  ACL_NO_GUID_SITE:
    "perc.ui.developer@This site has no object GUID — ACL cannot be loaded.",
  ACL_NO_GUID_DISPLAY_FORMAT:
    "perc.ui.developer@This display format has no object GUID — ACL cannot be loaded.",
  ACL_NO_ENTRIES: "perc.ui.developer@No ACL entries yet - add a principal below.",
  ACL_COL_ENTRY: "perc.ui.developer@Principal / name",
  ACL_COL_TYPE: "perc.ui.developer@Type",
  ACL_COL_PERMS: "perc.ui.developer@Permissions",
  ACL_COL_ACTIONS: "perc.ui.developer@Actions",
  ACL_LAYER_DESIGN: "perc.ui.developer@Design access",
  ACL_LAYER_RUNTIME: "perc.ui.developer@Runtime visibility",
  ACL_LAYER_DESIGN_HINT:
    "perc.ui.developer@Design access controls who can read, update, delete, or modify the ACL for this design object in Workbench / Developer tools.",
  ACL_LAYER_RUNTIME_HINT:
    "perc.ui.developer@Runtime visibility (RUNTIME_VISIBLE) controls Content Explorer / community visibility for this object at runtime.",
  ACL_PERM_READ: "perc.ui.developer@Read",
  ACL_PERM_UPDATE: "perc.ui.developer@Update",
  ACL_PERM_DELETE: "perc.ui.developer@Delete",
  ACL_PERM_OWNER: "perc.ui.developer@Modify ACL",
  ACL_PERM_RUNTIME_VISIBLE: "perc.ui.developer@Visible",
  ACL_ENTRY_ADD: "perc.ui.developer@Add entry",
  ACL_ENTRY_REMOVE: "perc.ui.developer@Remove",
  ACL_ENTRY_NAME: "perc.ui.developer@Principal name",
  ACL_ENTRY_NAME_PLACEHOLDER: "perc.ui.developer@Role, user, or community name",
  ACL_ENTRY_DUP: "perc.ui.developer@An entry with that name and type already exists.",
  ACL_SPECIAL_HINT:
    "perc.ui.developer@Default (USER) and AnyCommunity (COMMUNITY) are system principals. They cannot be removed; add them if missing, then toggle permissions and save.",
  ACL_SPECIAL_DEFAULT_LABEL: "perc.ui.developer@Default",
  ACL_SPECIAL_ANY_COMMUNITY_LABEL: "perc.ui.developer@Any community",
  ACL_SPECIAL_PROTECTED: "perc.ui.developer@Protected",
  ACL_SPECIAL_ADD_DEFAULT: "perc.ui.developer@Add Default entry",
  ACL_SPECIAL_ADD_ANY_COMMUNITY: "perc.ui.developer@Add AnyCommunity entry",
  ACL_SPECIAL_TYPE_DEFAULT: "perc.ui.developer@USER (system)",
  ACL_SPECIAL_TYPE_ANY_COMMUNITY: "perc.ui.developer@COMMUNITY (system)",
  ACL_SAVE: "perc.ui.developer@Save ACL",
  ACL_SAVING: "perc.ui.developer@Saving...",
  ACL_SAVED: "perc.ui.developer@Object ACL saved.",
  ACL_SAVE_ERROR: "perc.ui.developer@Could not save object ACL.",
  ACL_RELOAD_ERROR: "perc.ui.developer@ACL saved, but could not reload the updated ACL.",
  TAB_CONTENT_TYPES: "perc.ui.developer@Content Types",
  TAB_TEMPLATES: "perc.ui.developer@Templates",
  TAB_SLOTS: "perc.ui.developer@Slots",
  TAB_KEYWORDS: "perc.ui.developer@Keywords",
  TAB_LOCALES: "perc.ui.developer@Locales",
  TAB_SHARED_FIELDS: "perc.ui.developer@Shared Fields",
  TAB_SYSTEM_DEF: "perc.ui.developer@System Def",
  TAB_ITEM_FILTERS: "perc.ui.developer@Item Filters",
  TAB_DISPLAY_FORMATS: "perc.ui.developer@Display Formats",
  TAB_ACTION_MENUS: "perc.ui.developer@Action Menus",
  TAB_SEARCHES: "perc.ui.developer@Searches",
  TAB_VIEWS: "perc.ui.developer@Views",
  TAB_EXTENSIONS: "perc.ui.developer@Extensions",
  TAB_RELATIONSHIP_TYPES: "perc.ui.developer@Relationship Types",
  TAB_WORKFLOWS: "perc.ui.developer@Workflows",
  TAB_SERVER_CONFIGS: "perc.ui.developer@Server Configs",
  TAB_CE_CONTROLS: "perc.ui.developer@CE Controls",
  TAB_SITES: "perc.ui.developer@Sites",
  TAB_COMMUNITIES: "perc.ui.developer@Communities",
  TAB_COMMUNITY_VISIBILITY: "perc.ui.developer@Community Visibility",
  TAB_PIPELINES: "perc.ui.developer@Pipelines",
  TAB_PREFERENCES: "perc.ui.developer@Preferences",
  PREF_TITLE: "perc.ui.developer@Developer Preferences",
  PREF_INTRO:
    "perc.ui.developer@Workbench-parity preferences for design-time tools. Security preferences control the default ACL template applied when you create an object ACL.",
  PREF_SECURITY_TITLE: "perc.ui.developer@Security",
  PREF_ACL_HINT:
    "perc.ui.developer@Default object ACL template for newly created ACLs (Workbench Security preferences). Permission columns are grouped under Design access and Runtime visibility. Entries are merged onto the owner ACL after Create ACL on an object that has none.",
  PREF_ACL_LOADING: "perc.ui.developer@Loading default ACL template...",
  PREF_ACL_LOAD_ERROR: "perc.ui.developer@Could not load default ACL template preference.",
  PREF_ACL_SAVE: "perc.ui.developer@Save default ACL template",
  PREF_ACL_SAVING: "perc.ui.developer@Saving template...",
  PREF_ACL_SAVED: "perc.ui.developer@Default ACL template saved.",
  PREF_ACL_SAVE_ERROR: "perc.ui.developer@Could not save default ACL template preference.",
  PREF_ACL_RESET: "perc.ui.developer@Reset to system default",
  PREF_ACL_RESET_NOTICE: "perc.ui.developer@Editor reset to system default template (not saved yet).",
  PREF_ACL_SOURCE_SAVED: "perc.ui.developer@Source: saved user preference",
  PREF_ACL_SOURCE_SYSTEM: "perc.ui.developer@Source: system default (no preference stored)",
  PREF_ACL_NO_ENTRIES: "perc.ui.developer@No template entries — add principals below or reset to system default.",
  PREF_ACL_ENTRY_DUP: "perc.ui.developer@A template entry with that name and type already exists.",
  PREF_ACL_EMPTY_TEMPLATE: "perc.ui.developer@Template must include at least one entry before save.",
  PREF_ACL_NO_USER: "perc.ui.developer@Signed-in user is required to save preferences.",
  ACL_TEMPLATE_APPLIED: "perc.ui.developer@Object ACL created and default template applied.",
  ACL_TEMPLATE_APPLY_ERROR:
    "perc.ui.developer@ACL created, but could not apply the default ACL template.",
  CT_LOADING: "perc.ui.developer@Loading content types...",
  CT_EMPTY: "perc.ui.developer@No content types returned.",
  CT_ERROR: "perc.ui.developer@Could not load content types.",
  CT_COL_NAME: "perc.ui.developer@Name",
  CT_COL_LABEL: "perc.ui.developer@Label",
  CT_COL_DESCRIPTION: "perc.ui.developer@Description",
  CT_COL_ID: "perc.ui.developer@Id",
  CT_HINT: "perc.ui.developer@Create a type, import design XML, or select one to view fields and edit label, description, enabled, type-level search indexing, local field add/delete, icon strategy, field flags, include system or shared fields, workflows, templates, item-level exits, control property values, and field-rule expressions. Export downloads design XML. Import creates a new type from that XML (unique name; no overwrite). Delete requires a held lock.",
  CT_NEW: "perc.ui.developer@New content type",
  CT_FORM_NAME: "perc.ui.developer@Name",
  CT_NAME_HINT:
    "perc.ui.developer@Required. Unique, no spaces. Letters, digits, underscore, and period only.",
  CT_CREATE_SAVE: "perc.ui.developer@Create content type",
  CT_CANCEL: "perc.ui.developer@Cancel",
  CT_CREATED: "perc.ui.developer@Content type created.",
  CT_CREATE_ERROR: "perc.ui.developer@Could not create content type.",
  CT_DUPLICATE: "perc.ui.developer@A content type with this name already exists.",
  CT_INVALID_NAME: "perc.ui.developer@Content type name is invalid (blank, spaces, or wildcard).",
  CT_FORBIDDEN: "perc.ui.developer@Admin role is required.",
  CT_DELETE: "perc.ui.developer@Delete content type",
  CT_DELETE_CONFIRM: "perc.ui.developer@Delete this content type? This cannot be undone.",
  CT_DELETE_ERROR: "perc.ui.developer@Could not delete content type.",
  CT_DELETE_LOCK_REQUIRED:
    "perc.ui.developer@Lock this content type before deleting. The lock was not stolen.",
  CT_DELETED: "perc.ui.developer@Content type deleted.",
  CT_EXPORT: "perc.ui.developer@Export XML",
  CT_EXPORT_ERROR: "perc.ui.developer@Could not export content type.",
  CT_EXPORT_NOT_FOUND: "perc.ui.developer@Content type not found.",
  CT_IMPORT: "perc.ui.developer@Import XML",
  CT_IMPORT_HINT:
    "perc.ui.developer@Import creates a new content type from ItemDefData XML. Duplicate names are rejected (no overwrite). Choose a unique name when the XML already exists on the server.",
  CT_IMPORT_FILE: "perc.ui.developer@Design XML file",
  CT_IMPORT_NAME: "perc.ui.developer@Unique name",
  CT_IMPORT_NAME_PLACEHOLDER: "perc.ui.developer@Unique type name (no spaces)",
  CT_IMPORT_SUBMIT: "perc.ui.developer@Import content type",
  CT_IMPORTING: "perc.ui.developer@Importing...",
  CT_IMPORT_ERROR: "perc.ui.developer@Could not import content type.",
  CT_IMPORT_INVALID: "perc.ui.developer@Invalid content-type design XML.",
  CT_IMPORT_DUPLICATE: "perc.ui.developer@A content type with that name already exists.",
  CT_IMPORT_BAD_NAME: "perc.ui.developer@Name is required, must not contain spaces or wildcards.",
  CT_IMPORT_NO_FILE: "perc.ui.developer@Choose an ItemDefData XML file to import.",
  CT_IMPORTED: "perc.ui.developer@Content type imported.",
