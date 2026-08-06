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
  /** Favorite / My Pages (classic CUI bookmarks) — PUT with page id */
  get ADD_TO_MYPAGES() {
    return `${SERVICES_ROOT}/itemmanagement/item/addtomypages`;
  },
  /** Favorite / My Pages — DELETE with page id */
  get REMOVE_FROM_MYPAGES() {
    return `${SERVICES_ROOT}/itemmanagement/item/removefrommypages`;
  },
  /** Favorite / My Pages membership — GET returns plain boolean */
  get IS_MY_PAGE() {
    return `${SERVICES_ROOT}/itemmanagement/item/ismypage`;
  },
  get TEMPLATES_BY_SITE() {
    return `${SERVICES_ROOT}/sitemanage/sitetemplates/templates`;
  },
  /** Full template load (widgets / region associations). */
  get TEMPLATE_LOAD() {
    return `${SERVICES_ROOT}/pagemanagement/template`;
  },
  get BLOGS_FOR_SITE() {
    return `${SERVICES_ROOT}/sitemanage/section/blogs`;
  },
  /** All blogs across sites (PSSiteSectionRestService#getAllBlogs). */
  get ALL_BLOGS() {
    return `${SERVICES_ROOT}/sitemanage/section/allBlogs`;
  },
  /** Create site section (including sectionType=blog). */
  get SECTION_CREATE() {
    return `${SERVICES_ROOT}/sitemanage/section`;
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
  /** Pages/items by workflow state (classic Pages By Status gadget). POST body. */
  get PATH_ITEM_BY_WF_STATE() {
    return `${SERVICES_ROOT}/pathmanagement/path/item/wfState`;
  },
  get PATH_ITEM_ID() {
    return `${SERVICES_ROOT}/pathmanagement/path/item/id`;
  },
  /**
   * Content activity metrics (classic Activity gadget).
   * POST body: {@code ContentActivityRequest} (path, durationType, duration).
   */
  get ACTIVITY_CONTENT() {
    return `${SERVICES_ROOT}/activitymanagement/activity/contentactivity`;
  },
  /** What's Working / effectiveness (POST EffectivenessRequest). */
  get ACTIVITY_EFFECTIVENESS() {
    return `${SERVICES_ROOT}/activitymanagement/activity/effectiveness`;
  },
  /** Traffic gadget (POST ContentTrafficRequest). */
  get ACTIVITY_TRAFFIC() {
    return `${SERVICES_ROOT}/activitymanagement/activity/contenttraffic`;
  },
  /**
   * Process Monitor list (classic PROCESS_STATUS_ALL).
   * {@code GET /sitemanage/monitor/all}
   */
  get MONITOR_ALL() {
    return `${SERVICES_ROOT}/sitemanage/monitor/all`;
  },
  /** Metadata find by key — e.g. {@code percglobalvariables}. */
  get METADATA_FIND() {
    return `${SERVICES_ROOT}/metadatamanagement/metadata`;
  },
  /** Form Tracker — GET asset forms for a site. */
  get ASSET_FORMS() {
    return `${SERVICES_ROOT}/assetmanagement/asset/forms`;
  },
  /** Cookie consent totals (proxies DTS; may fail if delivery not configured). */
  get COOKIE_CONSENT_TOTALS() {
    return `${SERVICES_ROOT}/delivery/consent/log/totals`;
  },
  get COOKIE_CONSENT_TOTALS_SITE() {
    return `${SERVICES_ROOT}/delivery/consent/log/totals`;
  },
  /** Pages with comments (proxies DTS comments service). */
  get COMMENTS_PAGES_WITH_COMMENTS() {
    return `${SERVICES_ROOT}/delivery/comment/pageswithcomments`;
  },
  /** Membership users for a site (proxies DTS membership). */
  get MEMBERSHIP_USERS() {
    return `${SERVICES_ROOT}/delivery/membership/admin/users`;
  },
  /** Non-SEO pages (classic SEO Audit gadget). POST NonSEOPagesRequest. */
  get PAGE_NON_SEO() {
    return `${SERVICES_ROOT}/pagemanagement/page/nonSEOPages`;
  },
  /** Siteimprove integration (token / publish config). */
  get SITEIMPROVE_TOKEN() {
    return `${SERVICES_ROOT}/integrations/siteimprove/token`;
  },
  get SITEIMPROVE_PUBLISH_CONFIG() {
    return `${SERVICES_ROOT}/integrations/siteimprove/publish/config`;
  },
  /**
   * Google Analytics provider config (classic Google Setup gadget).
   * GET/POST/DELETE {@code /analytics/provider/config}
   */
  get ANALYTICS_CONFIG() {
    return `${SERVICES_ROOT}/analytics/provider/config`;
  },
  /** Whether a site has a GA profile mapping. Returns plain {@code "true"|"false"}. */
  get ANALYTICS_IS_PROFILE_CONFIGURED() {
    return `${SERVICES_ROOT}/analytics/provider/isProfileConfigured`;
  },
  /** List GA profiles (requires stored credentials). */
  get ANALYTICS_PROFILES() {
    return `${SERVICES_ROOT}/analytics/provider/profiles`;
  },
  /**
   * Test connection + store keyfile.
   * {@code POST multipart /analytics/provider/testConnection/{uid}} field {@code file}.
   */
  get ANALYTICS_TEST_CONNECTION() {
    return `${SERVICES_ROOT}/analytics/provider/testConnection`;
  },
  /** Theme summaries (Sitewide Framework / Design themes). */
  get THEME_SUMMARY_ALL() {
    return `${SERVICES_ROOT}/pagemanagement/theme/summary/all`;
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
  /**
   * Public REST content type catalog ({@code rest} module ContentTypesResource).
   * List only — design-time field editor APIs are a P0 gap survey.
   */
  get CONTENT_TYPES() {
    return `${SERVICES_ROOT}/contenttypes`;
  },
  /** Public REST template summaries (assembly design list). */
  get TEMPLATES() {
    return `${SERVICES_ROOT}/templates`;
  },
  /** Keyword design catalog. */
  get KEYWORDS() {
    return `${SERVICES_ROOT}/keywords`;
  },
  /** Assembly slots design catalog. */
  get SLOTS() {
    return `${SERVICES_ROOT}/slots`;
  },
  /** Communities design catalog. */
  get COMMUNITIES() {
    return `${SERVICES_ROOT}/communities`;
  },
  /** Pipeline / XML application design catalog. */
  get PIPELINES() {
    return `${SERVICES_ROOT}/pipelines`;
  },
  /** CMS locale design catalog (RXLOCALE + format profile flag). */
  get LOCALES() {
    return `${SERVICES_ROOT}/locales`;
  },
  /** Shared field groups design catalog (content-editor shared def). */
  get SHARED_FIELDS() {
    return `${SERVICES_ROOT}/sharedfields`;
  },
  /** Content-editor system definition field catalog. */
  get SYSTEM_DEF() {
    return `${SERVICES_ROOT}/systemdef`;
  },
  /** Assembly item filter design catalog. */
  get ITEM_FILTERS() {
    return `${SERVICES_ROOT}/itemfilters`;
  },
  /** Content Explorer display format design catalog. */
  get DISPLAY_FORMATS() {
    return `${SERVICES_ROOT}/displayformats`;
  },
  /** CX action menu design catalog. */
  get ACTION_MENUS() {
    return `${SERVICES_ROOT}/actions/catalog`;
  },
  /** CX search design catalog. */
  get SEARCHES() {
    return `${SERVICES_ROOT}/searches`;
  },
  /** CX view design catalog. */
  get VIEWS() {
    return `${SERVICES_ROOT}/views`;
  },
  /** Server extension design catalog. */
  get EXTENSIONS() {
    return `${SERVICES_ROOT}/extensions/catalog`;
  },
  /** System relationship type design catalog (SY-03). */
  get RELATIONSHIP_TYPES() {
    return `${SERVICES_ROOT}/relationshiptypes`;
  },
  /** Server configuration files catalog (SY-02). */
  get SERVER_CONFIGS() {
    return `${SERVICES_ROOT}/serverconfigs`;
  },
  /** Content editor control catalog (UI-01). */
  get CE_CONTROLS() {
    return `${SERVICES_ROOT}/cecontrols`;
  },
  /** Site design catalog (SY-04 association browse) — rest SitesResource. */
  get SITES() {
    return `${SERVICES_ROOT}/sites`;
  },
  /** Object ACL catalog (design-time security). */
  get ACLS() {
    return `${SERVICES_ROOT}/acls`;
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
  /** Folder workflow assignment (foldermanagement) — Feature 993 */
  get FOLDER_ASSIGNMENT_JOB_START() {
    return `${SERVICES_ROOT}/foldermanagement/GetAssociatedFoldersJob/start/`;
  },
  get FOLDER_ASSIGNMENT_JOB_STATUS() {
    return `${SERVICES_ROOT}/foldermanagement/workflowassignment/isInProgress`;
  },
  /** User management (user) — Feature 993 */
  get USERS() {
    return `${SERVICES_ROOT}/user/user/users`;
  },
  get USER_ROLES() {
    return `${SERVICES_ROOT}/user/user/roles`;
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
  /**
   * User default CMS landing override (slice 2 / #2209).
   * GET/PUT/DELETE plain text at {@code /homepage} (current) or
   * {@code /homepage/{userName}} (admin-managed).
   */
  get USER_HOMEPAGE() {
    return `${SERVICES_ROOT}/user/user/homepage`;
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
  get ROLE_DELETE_VALIDATE() {
    return `${SERVICES_ROOT}/rolemanagement/role/validateForDelete`;
  },
  get ROLE_REMOVE_USERS_VALIDATE() {
    return `${SERVICES_ROOT}/rolemanagement/role/validateDeleteUsers`;
  },
  get ROLE_AVAILABLE_USERS() {
    return `${SERVICES_ROOT}/rolemanagement/role/availableUsers`;
  },
  /** Category tree management (categorymanagement) — Feature 993 */
  get CATEGORY_ALL() {
    return `${SERVICES_ROOT}/category/all`;
  },
  get CATEGORY_UPDATE() {
    return `${SERVICES_ROOT}/category/update`;
  },
  get CATEGORY_LOCK_INFO() {
    return `${SERVICES_ROOT}/category/lockinfo`;
  },
  get CATEGORY_LOCK_TAB() {
    return `${SERVICES_ROOT}/category/locktab/`;
  },
  get CATEGORY_REMOVE_LOCK_TAB() {
    return `${SERVICES_ROOT}/category/removelocktab`;
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
  get ITEM_WORKFLOW_TRANSITION() {
    return `${SERVICES_ROOT}/itemmanagement/workflow/transition/`;
  },
  get ITEM_WORKFLOW_TRANSITION_WITH_COMMENTS() {
    return `${SERVICES_ROOT}/itemmanagement/workflow/transitionWithComments/`;
  },
  /** Scheduled task management — Feature 993 */
  get SCHEDULED_TASKS() {
    return `${SERVICES_ROOT}/taskmanagement/tasks`;
  },
  /** System Consistency Checker — Feature 993 */
  get CONSISTENCY_CHECK() {
    return `${SERVICES_ROOT}/taskmanagement/tasks/consistency`;
  },
} as const;
