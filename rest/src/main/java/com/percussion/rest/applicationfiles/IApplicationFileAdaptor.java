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

package com.percussion.rest.applicationfiles;

import java.util.List;

/**
 * Adaptor for XML application CMS/resource files (SY-05). Path-safe list/get/put under a catalog
 * application root — not {@code /serverconfigs} (SY-02).
 */
public interface IApplicationFileAdaptor {

  /**
   * List relative file paths under the named application root.
   *
   * @return summaries (no content); {@code null} if the application is unknown / unsafe
   */
  List<ApplicationFileSummary> listFiles(String appName);

  /**
   * Load one file by application name and relative path under that app root.
   *
   * @return detail with content, or {@code null} if app/path unknown or unsafe
   */
  ApplicationFileSummary getFile(String appName, String relativePath);

  /**
   * Admin. Replace UTF-8 text content of an existing or new file under the application root.
   *
   * @return updated detail, or {@code null} if app/path unknown or unsafe
   */
  ApplicationFileSummary putFile(String appName, String relativePath, ApplicationFileSummary body);
}
