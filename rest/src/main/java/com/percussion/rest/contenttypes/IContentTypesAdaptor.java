/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.contenttypes;

import java.net.URI;
import java.util.List;

/** Defines the interface that backend API implementations must implement for ContentTypes. */
public interface IContentTypesAdaptor {

  /**
   * List all content types available to the System.
   *
   * @param baseUri Requesting URI
   * @return A list of all available Content Types
   */
  List<ContentType> listContentTypes(URI baseUri);

  /**
   * List ContentTypes available for the specified Site.
   *
   * @param baseUri Originating URI
   * @param siteId Site Id for Site to filter Types by
   * @return An array of ContentTypes
   */
  List<ContentType> listContentTypes(URI baseUri, int siteId);

  /**
   * List ContentTypes available for the specified filter.
   *
   * @param baseUri Originating URI
   * @param filter A ContentTypeFilter that can be used to filter content types.
   * @return An array of ContentTypes
   */
  List<ContentType> listContentTypesByFilter(URI baseUri, ContentTypeFilter filter);

  /**
   * Load a read-only design summary for one content type (fields catalog).
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return detail or {@code null} when not found
   */
  ContentTypeDetail getContentType(URI baseUri, String idOrName);

  /**
   * Update content type design fields under a design-session lock.
   *
   * <p>Supports label, description, enabled, per-field {@code searchable} (and optional
   * occurrence), allowed workflows (+ default workflow id), and allowed templates. Association
   * lists use full-replace semantics when non-null; omit them to leave associations unchanged.
   * Locks for the current request user, saves, and releases the lock.
   *
   * @return updated detail, or {@code null} when not found
   */
  ContentTypeDetail updateContentType(URI baseUri, String idOrName, ContentTypeDetail body);

  /**
   * List allowed template associations for a content type (read-only; no lock required).
   *
   * @return association list (empty when none), or {@code null} when the content type is not found
   */
  List<NamedObjectRef> getAllowedTemplates(URI baseUri, String idOrName);

  /**
   * Replace allowed template associations. Requires a design-session lock already held by the
   * current request user. Full-replace semantics: empty list clears associations. Does not release
   * the lock.
   *
   * @param templates replacement set, never {@code null} (empty clears)
   * @return the persisted set (empty when none), or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when a template id/name cannot be resolved
   */
  List<NamedObjectRef> replaceAllowedTemplates(
      URI baseUri, String idOrName, List<NamedObjectRef> templates);
}
