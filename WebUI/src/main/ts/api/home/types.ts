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
  [key: string]: unknown;
}

export interface FolderChild {
  name?: string;
  path?: string;
  type?: string;
  folder?: boolean;
  [key: string]: unknown;
}

export interface CreatePageRequest {
  name: string;
  title: string;
  linkTitle: string;
  templateId: string;
  folderPath: string;
}
