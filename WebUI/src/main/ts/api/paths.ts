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

/**
 * Service roots matching WebUI perc_path_constants (SERVER_ROOT + /services).
 *
 * <p>Jetty deploys the CMS webapp at context {@code /} (folder name Rhythmyx does not
 * mean context path /Rhythmyx). Hardcoding {@code /Rhythmyx/services} 404s or redirects
 * to login incorrectly when the UI is served from {@code /cm/...}. Detect context from
 * the current location; fall back to {@code /services}.
 */
export function detectServicesRoot(): string {
  try {
    if (typeof window !== "undefined" && window.location?.pathname) {
      const p = window.location.pathname;
      if (p === "/Rhythmyx" || p.startsWith("/Rhythmyx/")) {
        return "/Rhythmyx/services";
      }
    }
  } catch {
    /* ignore */
  }
  return "/services";
}

/** Mutable so tests can override; initialized for the current page context. */
export let SERVICES_ROOT = detectServicesRoot();

/** Test / re-init helper when pathname changes. */
export function refreshServicesRoot(): string {
  SERVICES_ROOT = detectServicesRoot();
  return SERVICES_ROOT;
}

export const PATHS = {
  get RECENT_ROOT() {
    return `${SERVICES_ROOT}/recentmanagement/recent/`;
  },
  get SITES_ALL() {
    return `${SERVICES_ROOT}/sitemanage/site`;
  },
  get PATH_FOLDER() {
    return `${SERVICES_ROOT}/pathmanagement/path/folder`;
  },
  get FINDER_SEARCH_EXTENDED() {
    return `${SERVICES_ROOT}/searchmanagement/search/get/extendedresults`;
  },
  get PAGE_CREATE() {
    return `${SERVICES_ROOT}/pagemanagement/page`;
  },
  /** Page-level copy (US7 clipboard paste; PSPageRestService#copy). */
  get PAGE_COPY() {
    return `${SERVICES_ROOT}/pagemanagement/page/copy`;
  },
  get MY_CONTENT() {
    return `${SERVICES_ROOT}/itemmanagement/item/mycontent`;
  },
  get TEMPLATES_BY_SITE() {
    return `${SERVICES_ROOT}/sitemanage/sitetemplates/templates`;
  },
  get BLOGS_FOR_SITE() {
    return `${SERVICES_ROOT}/sitemanage/section/blogs`;
  },
  get ASSET_TYPES() {
    return `${SERVICES_ROOT}/assetmanagement/asset/assetTypes`;
  },
  get WIDGET_BUILDER() {
    return `${SERVICES_ROOT}/widgetmanagement/widgetbuilder`;
  },
  /** Site publish / status (sitemanage) — see specs/990-unified-publishing-ui/research/ops-path-inventory.md */
  get SITE_PUBLISH() {
    return `${SERVICES_ROOT}/sitemanage/publish`;
  },
  get PUBLISH_CURRENT_STATUS() {
    return `${SERVICES_ROOT}/sitemanage/pubstatus/current`;
  },
  get PUBLISH_LOGS() {
    return `${SERVICES_ROOT}/sitemanage/pubstatus/logs`;
  },
  get PUBLISH_LOGS_DETAILS() {
    return `${SERVICES_ROOT}/sitemanage/pubstatus/details`;
  },
  get PUBLISH_PURGE() {
    return `${SERVICES_ROOT}/sitemanage/pubstatus/purge`;
  },
  get INCREMENTAL_LIST() {
    return `${SERVICES_ROOT}/sitemanage/publish/incremental/content/`;
  },
  get INCREMENTAL_RELATED_LIST() {
    return `${SERVICES_ROOT}/sitemanage/publish/incremental/relatedcontent/`;
  },
  get INCREMENTAL_PUBLISH() {
    return `${SERVICES_ROOT}/sitemanage/publish/incremental/publish/`;
  },
  /** Publish servers (publishmanagement) */
  get PUB_SERVERS() {
    return `${SERVICES_ROOT}/publishmanagement/servers/`;
  },
  /** Sitemanage path management — used by ContentBrowser (US2). */
  get PATH_PAGINATED_FOLDER() {
    return `${SERVICES_ROOT}/pathmanagement/path/paginatedFolder`;
  },
  get PATH_ITEM() {
    return `${SERVICES_ROOT}/pathmanagement/path/item`;
  },
  get PATH_ITEM_ID() {
    return `${SERVICES_ROOT}/pathmanagement/path/item/id`;
  },
  get PATH_ADD_NEW_FOLDER() {
    return `${SERVICES_ROOT}/pathmanagement/path/addNewFolder`;
  },
  get PATH_RENAME_FOLDER() {
    return `${SERVICES_ROOT}/pathmanagement/path/renameFolder`;
  },
  get PATH_MOVE_ITEM() {
    return `${SERVICES_ROOT}/pathmanagement/path/moveItem`;
  },
  get PATH_DELETE_ITEM() {
    return `${SERVICES_ROOT}/pathmanagement/path/delete`;
  },
  get PATH_FOLDER_PROPERTIES() {
    return `${SERVICES_ROOT}/pathmanagement/path/folderProperties`;
  },
  get PATH_SAVE_FOLDER_PROPERTIES() {
    return `${SERVICES_ROOT}/pathmanagement/path/saveFolderProperties`;
  },
  get PATH_VALIDATE() {
    return `${SERVICES_ROOT}/pathmanagement/path/validate`;
  },
  get PATH_LAST_EXISTING() {
    return `${SERVICES_ROOT}/pathmanagement/path/lastExisting`;
  },
  /** Action menu (US3) — see capability-matrix.md P-Menu. */
  get ACTIONS_ROOT() {
    return `${SERVICES_ROOT}/actions`;
  },
  /** Workflow management (workflowmanagement) — Feature 993 */
  get WORKFLOWS() {
    return `${SERVICES_ROOT}/workflowmanagement/workflows/`;
  },
  get WORKFLOW_METADATA() {
    return `${SERVICES_ROOT}/workflowmanagement/workflows/metadata`;
  },
  get WORKFLOW_METADATA_DEFAULT() {
    return `${SERVICES_ROOT}/workflowmanagement/workflows/metadata/default`;
  },
  /** User management (user) — Feature 993 */
  get USERS() {
    return `${SERVICES_ROOT}/user/user/users`;
  },
  get USER_FIND() {
    return `${SERVICES_ROOT}/user/user/find`;
  },
  get USER_CREATE() {
    return `${SERVICES_ROOT}/user/user/create`;
  },
  get USER_UPDATE() {
    return `${SERVICES_ROOT}/user/user/update`;
  },
  get USER_DELETE() {
    return `${SERVICES_ROOT}/user/user/delete`;
  },
  get USER_CHANGE_PW() {
    return `${SERVICES_ROOT}/user/user/changepw`;
  },
  get USER_LDAP_FIND() {
    return `${SERVICES_ROOT}/user/user/external/find`;
  },
  get USER_LDAP_IMPORT() {
    return `${SERVICES_ROOT}/user/user/import`;
  },
  get USER_LDAP_STATUS() {
    return `${SERVICES_ROOT}/user/user/external/status`;
  },
  /** Role management (rolemanagement) — Feature 993 */
  get ROLES_FIND() {
    return `${SERVICES_ROOT}/rolemanagement/role/find`;
  },
  get ROLE_CREATE() {
    return `${SERVICES_ROOT}/rolemanagement/role/create`;
  },
  get ROLE_UPDATE() {
    return `${SERVICES_ROOT}/rolemanagement/role/update`;
  },
  get ROLE_DELETE() {
    return `${SERVICES_ROOT}/rolemanagement/role/delete`;
  },
  get ROLE_AVAILABLE_USERS() {
    return `${SERVICES_ROOT}/rolemanagement/role/availableUsers`;
  },
  /** Item workflow management (itemmanagement) — Feature 993 */
  get ITEM_WORKFLOW_TRANSITIONS() {
    return `${SERVICES_ROOT}/itemmanagement/workflow/getTransitions/`;
  },
  get ITEM_WORKFLOW_CHECKIN() {
    return `${SERVICES_ROOT}/itemmanagement/workflow/checkIn/`;
  },
  get ITEM_WORKFLOW_CHECKOUT() {
    return `${SERVICES_ROOT}/itemmanagement/workflow/checkOut/`;
  },
  get ITEM_WORKFLOW_FORCE_CHECKOUT() {
    return `${SERVICES_ROOT}/itemmanagement/workflow/forceCheckOut/`;
  },
  /** Scheduled task management — Feature 993 */
  get SCHEDULED_TASKS() {
    return `${SERVICES_ROOT}/taskmanagement/tasks`;
  },
} as const;
