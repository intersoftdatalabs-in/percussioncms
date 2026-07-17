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

/** Service roots matching WebUI perc_path_constants (SERVER_ROOT + /services). */
export const SERVICES_ROOT = "/Rhythmyx/services";

export const PATHS = {
  RECENT_ROOT: `${SERVICES_ROOT}/recentmanagement/recent/`,
  SITES_ALL: `${SERVICES_ROOT}/sitemanage/site`,
  PATH_FOLDER: `${SERVICES_ROOT}/pathmanagement/path/folder`,
  FINDER_SEARCH_EXTENDED: `${SERVICES_ROOT}/searchmanagement/search/get/extendedresults`,
  PAGE_CREATE: `${SERVICES_ROOT}/pagemanagement/page`,
  MY_CONTENT: `${SERVICES_ROOT}/itemmanagement/item/mycontent`,
  WIDGET_BUILDER: `${SERVICES_ROOT}/widgetmanagement/widgetbuilder`,
} as const;
