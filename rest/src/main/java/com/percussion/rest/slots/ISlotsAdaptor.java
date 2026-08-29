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
   * Update mutable slot design fields (label, description, {@code slotLayout}, {@code slotStyles})
   * and optionally replace content-type / template associations. When {@code body.associations} is
   * {@code null}, associations are left unchanged; a non-null list (including empty) replaces the
   * full association set. When {@code body.slotLayout} / {@code body.slotStyles} is non-null, that
   * map replaces the definition layout/styles (empty or schema-only clears to defaults); null
   * leaves the field unchanged. Name/id is not changed via this path.
   *
   * @return updated detail, or {@code null} if not found
   */
  @Nullable
  SlotDetail updateSlot(URI baseUri, String idOrName, SlotDetail body);

  /**
   * Create and persist a slot (Workbench Finish: {@code IPSAssemblyDesignWs.createSlots} then
   * {@code saveSlots}). Admin only. Name must be unique (case-insensitive) and must not contain
   * whitespace.
   *
   * @param baseUri requesting URI
   * @param body request body; {@code name} is required. Optional label, description, and {@code
   *     slotType} ({@code REGULAR} or {@code INLINE}) are applied before save.
   * @return persisted detail
   * @throws IllegalArgumentException when the name is blank, contains whitespace, or contains
   *     wildcards, or when {@code slotType} is not {@code REGULAR}/{@code INLINE}
   * @throws jakarta.ws.rs.WebApplicationException {@code 409} when a slot with that name already
   *     exists; {@code 403} when the caller is not Admin or the request has no session/user
   */
  SlotDetail createSlot(URI baseUri, SlotDetail body);

  /**
   * Delete a slot via {@code IPSAssemblyDesignWs.deleteSlots}. Admin only. Acquires a design lock
   * for this request ({@code loadSlots(lock=true)}, {@code overrideLock=false}) and does not steal
   * another user's lock. System slots are rejected.
   *
   * @param baseUri requesting URI
   * @param idOrName slot uuid (numeric) or unique name
   * @return {@code true} when deleted; {@code false} when not found
   * @throws IllegalArgumentException when {@code idOrName} is blank
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     409} when the slot is a system slot or locked by another user
   */
  boolean deleteSlot(URI baseUri, String idOrName);
}
