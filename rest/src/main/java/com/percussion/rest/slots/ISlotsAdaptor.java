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

package com.percussion.rest.slots;

import java.net.URI;
import java.util.List;
import org.springframework.lang.Nullable;

public interface ISlotsAdaptor {

  List<SlotSummary> listSlots(URI baseUri);

  /**
   * Load slot design detail by numeric uuid or unique name.
   *
   * @return detail, or {@code null} if not found
   */
  @Nullable
  SlotDetail getSlot(URI baseUri, String idOrName);

  /**
   * Update mutable slot design fields (label, description) and optionally replace content-type /
   * template associations. When {@code body.associations} is {@code null}, associations are left
   * unchanged; a non-null list (including empty) replaces the full association set. Name/id is not
   * changed via this path.
   *
   * @return updated detail, or {@code null} if not found
   */
  @Nullable
  SlotDetail updateSlot(URI baseUri, String idOrName, SlotDetail body);
}
