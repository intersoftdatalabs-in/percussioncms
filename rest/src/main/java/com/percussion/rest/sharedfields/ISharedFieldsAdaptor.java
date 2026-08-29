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
   * @return updated detail, or {@code null} when not found / unsafe path name
   * @throws IllegalArgumentException when the path name is blank, input is invalid, a field name
   *     is unknown, or {@code occurrence} and {@code required} conflict
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

  /**
   * Add a persistable field (backend column + display mapping) to an existing shared field group.
   * Admin only. Acquires the shared-def design lock for this request and releases it on save.
   *
   * @param body {@code name} required; unique case-insensitive across shared groups. Optional {@code
   *     dataType} defaults to {@code text}. Optional {@code searchable}, {@code occurrence} /
   *     {@code required} as on PUT patches.
   * @return updated group detail, or {@code null} when the group is not found / unsafe path name
   * @throws IllegalArgumentException when the group path is blank, field name/dataType is invalid,
   *     or occurrence/required conflict
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     409} when a field with that name already exists
   * @throws SharedFieldDesignLockException when the shared def is locked by another user
   */
  SharedFieldGroupDetail addField(URI baseUri, String groupName, SharedFieldSummary body);

  /**
   * Remove a field (backend column mapping + display mapping) from an existing shared field group.
   * Admin only. Acquires the shared-def design lock for this request and releases it on save.
   *
   * @throws SharedFieldNotFoundException when the group or field does not exist
   * @throws IllegalArgumentException when a path name is blank
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SharedFieldDesignLockException when the shared def is locked by another user
   */
  void deleteField(URI baseUri, String groupName, String fieldName);

  /**
   * Load control property values and the choice catalog for one shared field (CD-15 / CD-07). Admin
   * only. No design lock is required. Empty {@code properties} means none configured. {@code
   * choices} is null when none.
   *
   * @return envelope, or {@code null} when the group is not found / unsafe name
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     404} when the field is unknown
   */
  SharedFieldControlProperties getFieldControlProperties(
      URI baseUri, String idOrName, String fieldName);

  /**
   * Replace control property values (and optionally the choice catalog) for one shared field. Admin
   * only. Acquires the shared-def design lock for this request and releases it on save. {@code
   * properties} is a full replace (empty clears). {@code choices} null leaves the catalog unchanged.
   *
   * @return persisted envelope, or {@code null} when the group is not found / unsafe path name
   * @throws SharedFieldNotFoundException when the field does not exist
   * @throws IllegalArgumentException when properties is missing or a choice catalog is invalid
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SharedFieldDesignLockException when the shared def is locked by another user
   */
  SharedFieldControlProperties replaceFieldControlProperties(
      URI baseUri, String idOrName, String fieldName, SharedFieldControlProperties body);
}
