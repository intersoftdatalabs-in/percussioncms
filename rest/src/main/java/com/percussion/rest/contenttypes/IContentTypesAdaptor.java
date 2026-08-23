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

import com.percussion.rest.ObjectLockSummary;
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
   * Acquire a self-only design-session lock on the content type. Does not save.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return lock summary, or {@code null} when not found
   */
  ObjectLockSummary lockContentType(URI baseUri, String idOrName);

  /**
   * Release a design-session lock owned by the current user/session. Does not save.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return {@code Boolean.TRUE} when released; {@code null} when not found. Throws {@code
   *     IllegalStateException} when locked by another user.
   */
  Boolean unlockContentType(URI baseUri, String idOrName);
}
