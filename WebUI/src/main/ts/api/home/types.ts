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

export interface SiteSummary {
  name: string;
  id?: string | number;
  siteId?: string | number;
}

export interface ContentListItem {
  id?: string;
  name?: string;
  title?: string;
  path?: string;
  type?: string;
  status?: string;
  folder?: boolean;
  [key: string]: unknown;
}

export interface FolderChild {
  name?: string;
  path?: string;
  type?: string;
  folder?: boolean;
  [key: string]: unknown;
}

export interface TemplateSummary {
  id: string;
  name: string;
  thumbPath?: string;
}

export interface AssetTypeSummary {
  /**
   * Widget definition id used by classic createAsset / editAsset
   * (e.g. {@code percImage}), not the numeric content type id.
   */
  id: string;
  name: string;
  label?: string;
  contentTypeId?: string;
  contentTypeName?: string;
}

export interface BlogSummary {
  title: string;
  folderPath: string;
  templateId: string;
  site?: string;
  path?: string;
}

/** Classic Page JSON create payload (perc_page_manager). */
export interface CreatePageRequest {
  name: string;
  title: string;
  linkTitle: string;
  templateId: string;
  folderPath: string;
}
