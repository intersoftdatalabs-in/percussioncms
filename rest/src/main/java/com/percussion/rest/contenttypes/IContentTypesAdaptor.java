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
   * Create and persist a content type (Workbench Finish: {@code createContentTypes} then {@code
   * saveContentTypes}). Admin only. Name must be unique (case-insensitive) and must not contain
   * spaces.
   *
   * @param baseUri requesting URI
   * @param body request body; {@code name} is required. Optional label, description, and enabled
   *     are applied before save.
   * @return persisted detail
   * @throws IllegalArgumentException when the name is blank, contains whitespace, or contains
   *     wildcards
   * @throws jakarta.ws.rs.WebApplicationException {@code 409} when a content type with that name
   *     already exists; {@code 403} when the caller is not Admin
   */
  ContentTypeDetail createContentType(URI baseUri, ContentTypeDetail body);

  /**
   * Load a read-only design summary for one content type (fields catalog).
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return detail or {@code null} when not found
   */
  ContentTypeDetail getContentType(URI baseUri, String idOrName);

  /**
   * Update content type design fields. Requires a design-session lock already held by the current
   * user/session ({@link #lockContentType}); does not acquire or release the lock.
   *
   * <p>Supports label, description, enabled, per-field {@code searchable} (and optional
   * occurrence), allowed workflows (+ default workflow id), and allowed templates. Association
   * lists use full-replace semantics when non-null; omit them to leave associations unchanged.
   * Does not change name — use {@link #renameContentType}. Field rule expressions use {@link
   * #replaceFieldRuleExpressions}. Control property values use
   * {@link #replaceFieldControlProperties}.
   *
   * @return updated detail, or {@code null} when not found
   * @throws ContentTypeDesignLockException when no lock is held or the lock is owned by another
   *     user
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
   * @return {@code Boolean.TRUE} when released; {@code null} when not found. Throws {@link
   *     ContentTypeDesignLockException} when locked by another user.
   */
  Boolean unlockContentType(URI baseUri, String idOrName);

  /**
   * Enable or disable a content type for runtime use (CD-13). Requires a design-session lock
   * already held by the current user (peer lock REST). Does not acquire or release the lock.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @param enabled {@code true} to enable, {@code false} to disable
   * @return updated detail, or {@code null} when not found
   * @throws ContentTypeDesignLockException when no lock is held or the lock is owned by another
   *     user
   */
  ContentTypeDetail setContentTypeEnabled(URI baseUri, String idOrName, boolean enabled);

  /**
   * Rename a content type (CD-01). Requires a design-session lock already held by the current
   * user. Does not acquire or release the lock. Bulk {@link #updateContentType} does not change
   * name. After a successful rename, GET by the previous name is not found; GET by id returns the
   * new name.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or current internal name
   * @param newName unique internal name (no spaces; case-insensitive unique)
   * @return updated detail, or {@code null} when not found
   * @throws ContentTypeDesignLockException when no lock is held or the lock is owned by another
   *     user
   * @throws IllegalArgumentException when the new name is invalid or collides
   */
  ContentTypeDetail renameContentType(URI baseUri, String idOrName, String newName);

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

  /**
   * Replace allowed-workflow associations for a content type (CD-08). Requires a design-session
   * lock already held by the current user. Full-replace semantics: empty list clears associations.
   * Does not release the lock.
   *
   * @param allowedWorkflows replacement set, never {@code null} (empty clears)
   * @param defaultWorkflow optional default workflow id/name; may be {@code null}
   * @return updated detail, or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when a workflow id/name cannot be resolved
   */
  ContentTypeDetail setAllowedWorkflows(
      URI baseUri,
      String idOrName,
      List<NamedObjectRef> allowedWorkflows,
      NamedObjectRef defaultWorkflow);

  /**
   * Load item-level pre/post exits, input/output translations, and validations (CD-09). No design
   * lock is required. Empty lists mean none configured.
   *
   * @return envelope (lists may be empty), or {@code null} when the content type is not found
   */
  ContentTypeItemExits getItemExits(URI baseUri, String idOrName);

  /**
   * Replace item-level translations/validations (and optionally pipe pre/post exits). Requires a
   * design-session lock already held by the current user. Does not acquire or release the lock.
   * {@code inputTranslations}, {@code outputTranslations}, and {@code validations} are full
   * replace (empty clears). {@code preExits}/{@code postExits} null leaves pipe extensions
   * unchanged. Apply-when conditions are not written.
   *
   * @return persisted envelope, or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when a required list is missing or an extension FQN is invalid
   */
  ContentTypeItemExits replaceItemExits(URI baseUri, String idOrName, ContentTypeItemExits body);

  /**
   * Load control property values and the choice catalog for one field (CD-07). No design lock is
   * required. Empty {@code properties} means none configured. {@code choices} is null when none.
   *
   * @return envelope, or {@code null} when the content type is not found
   */
  ContentTypeFieldControlProperties getFieldControlProperties(
      URI baseUri, String idOrName, String fieldName);

  /**
   * Replace control property values (and optionally the choice catalog) for one field. Requires a
   * design-session lock already held by the current user. Does not acquire or release the lock.
   * {@code properties} is a full replace (empty clears). {@code choices} null leaves the catalog
   * unchanged.
   *
   * @return persisted envelope, or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when properties is missing or a choice catalog is invalid
   */
  ContentTypeFieldControlProperties replaceFieldControlProperties(
      URI baseUri, String idOrName, String fieldName, ContentTypeFieldControlProperties body);

  /**
   * Load field-level validation, visibility, and input/output translation expressions (CD-05–07).
   * No design lock is required. Empty lists mean none configured.
   *
   * @return envelope, or {@code null} when the content type is not found
   * @throws jakarta.ws.rs.WebApplicationException {@code 404} when the field name is unknown
   */
  ContentTypeFieldRuleExpressions getFieldRuleExpressions(
      URI baseUri, String idOrName, String fieldName);

  /**
   * Replace field-level validation, visibility, and translation expressions. Requires a
   * design-session lock already held by the current user. Does not acquire or release the lock.
   * {@code validation}, {@code visibility}, {@code inputTranslation}, and {@code outputTranslation}
   * are full replace (empty clears).
   *
   * @return persisted envelope, or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when required lists are missing, a rule is invalid, or the
   *     field name is unknown
   */
  ContentTypeFieldRuleExpressions replaceFieldRuleExpressions(
      URI baseUri, String idOrName, String fieldName, ContentTypeFieldRuleExpressions body);
}
