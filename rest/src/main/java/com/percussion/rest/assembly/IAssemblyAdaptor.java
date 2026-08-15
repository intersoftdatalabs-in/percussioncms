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

package com.percussion.rest.assembly;

/** Adaptor for assembly preview location (Explorer template preview). */
public interface IAssemblyAdaptor {

  /**
   * Build an assembly preview location for the item and template.
   *
   * @param contentId content item id
   * @param templateId assembly template id
   * @param revision revision, or {@code null} to use the item's current revision
   * @return location, or {@code null} if the item is not found
   */
  PreviewLocation previewLocation(int contentId, int templateId, Integer revision);

  /**
   * Flush assembler pages. Empty keys flush all assembler pages (same as {@code
   * PSExitFlushAssemblerCache} with omitted keys).
   */
  void flushAssemblerCache();

  /**
   * Reset managed navigation configuration. Same goal as classic {@code PSNavReset}.
   */
  void resetNavigation();
}
