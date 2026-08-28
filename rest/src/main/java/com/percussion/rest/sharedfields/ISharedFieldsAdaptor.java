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

package com.percussion.rest.sharedfields;

import java.net.URI;
import java.util.List;

public interface ISharedFieldsAdaptor {

  /**
   * List shared field groups from the content-editor shared definition.
   *
   * @param baseUri reserved for HATEOAS
   * @return group summaries, never {@code null}
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  List<SharedFieldGroupSummary> listGroups(URI baseUri);

  /**
   * Load one shared field group by name (case-insensitive).
   *
   * @return detail, or {@code null} when not found / unsafe name
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  SharedFieldGroupDetail getGroup(URI baseUri, String name);

  /**
   * Create and persist an empty shared field group (Workbench new shared-field file). Admin only.
   * Acquires the shared-def design lock for this request and releases it on save.
   *
   * @param body {@code name} required; unique case-insensitive; no spaces. Optional {@code
   *     filename} defaults to {@code {name}.xml}.
   * @return persisted detail
   * @throws IllegalArgumentException when the name/filename is invalid
   * @throws jakarta.ws.rs.WebApplicationException {@code 409} when a group with that name exists;
   *     {@code 403} when the caller is not Admin
   * @throws SharedFieldDesignLockException when the shared def is locked by another user
   */
  SharedFieldGroupDetail createGroup(URI baseUri, SharedFieldGroupDetail body);

  /**
   * Update an existing shared field group. Admin only. Acquires the shared-def design lock for this
   * request and releases it on save.
   *
   * <p>Supports filename, rename ({@code body.name} different from the path name), and patches to
   * existing fields ({@code searchable}, occurrence / required). Null {@code fields} leaves fields
   * unchanged. Does not create or delete fields.
   *
   * @return updated detail, or {@code null} when not found / unsafe name
   * @throws IllegalArgumentException when input is invalid or a field name is unknown
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     409} when the new name already exists
   * @throws SharedFieldDesignLockException when the shared def is locked by another user
   */
  SharedFieldGroupDetail updateGroup(URI baseUri, String name, SharedFieldGroupDetail body);

  /**
   * Delete a shared field group. Admin only. Acquires the shared-def design lock for this request
   * and releases it on save.
   *
   * @throws SharedFieldNotFoundException when the group does not exist
   * @throws IllegalArgumentException when the name is blank or unsafe
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SharedFieldDesignLockException when the shared def is locked by another user
   */
  void deleteGroup(URI baseUri, String name);
}
