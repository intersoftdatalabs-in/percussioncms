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

package com.percussion.rest.fileexplorer;

import java.util.List;

/**
 * Adaptor for File Explorer browse (Workbench §12.1). Distinct from SY-05 application CMS/resource
 * files and SY-02 {@code /serverconfigs}.
 */
public interface IFileExplorerAdaptor {

  /**
   * List configured allow-listed roots (ids and display names only).
   *
   * @return never {@code null}; empty when no roots are configured
   */
  List<FileExplorerRoot> listRoots();

  /**
   * List children under {@code rootId} at a relative path.
   *
   * @param rootId allow-listed catalog id (not a filesystem path)
   * @param relativePath {@code /}-separated path under the root; blank means the root itself
   * @return children, or {@code null} when the root/path is unknown (HTTP 404)
   */
  List<FileExplorerEntry> listChildren(String rootId, String relativePath);
}
