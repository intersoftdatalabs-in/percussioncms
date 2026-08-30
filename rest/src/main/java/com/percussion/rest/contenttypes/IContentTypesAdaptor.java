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
   * spaces. Reserved system names such as {@code Folder} collide with existing catalog types.
   *
   * @param baseUri requesting URI
   * @param body request body; {@code name} is required. Optional label, description, and enabled
   *     are applied before save.
   * @return persisted detail
   * @throws IllegalArgumentException when the name is blank, contains whitespace, or contains
   *     wildcards
   * @throws jakarta.ws.rs.WebApplicationException {@code 409} when a content type with that name
   *     already exists (including reserved system types); {@code 403} when the caller is not Admin
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
   * Export Workbench-equivalent design XML for one content type (CD-14).
   *
   * <p>Read-only: loads through {@code IPSContentDesignWs} without acquiring or stealing locks.
   * Admin only.
   *
   * @param baseUri the base URI (reserved for HATEOAS)
   * @param idOrName content type uuid (numeric) or unique name
   * @return export payload, or {@code null} if not found
   */
  ContentTypeExport exportContentType(URI baseUri, String idOrName);

  /**
   * Update content type design fields. Requires a design-session lock already held by the current
   * user/session ({@link #lockContentType}); does not acquire or release the lock.
   *
   * <p>Supports label, description, enabled, per-field {@code searchable} (and optional
   * occurrence), allowed workflows (+ default workflow id), and allowed templates. Association
   * lists use full-replace semantics when non-null; omit them to leave associations unchanged.
   * Does not change name — use {@link #renameContentType}. Field rule expressions use {@link
   * #replaceFieldRuleExpressions}. Control property values use
   * {@link #replaceFieldControlProperties}. Local field create/delete uses {@link #addLocalField}
   * / {@link #deleteLocalField}.
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
   * Delete a content type via {@code IPSContentDesignWs.deleteContentTypes}. Admin only. Requires a
   * design-session lock already held by the current user ({@link #lockContentType}). Does not
   * acquire, steal, or ignore locks. Does not cascade item delete ({@code ignoreDependencies=false}).
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return {@code Boolean.TRUE} when deleted; {@code null} when not found
   * @throws ContentTypeDesignLockException when no lock is held or the lock is owned by another
   *     user
   * @throws IllegalArgumentException when the design web service rejects the delete (in-use /
   *     dependents)
   */
  Boolean deleteContentType(URI baseUri, String idOrName);

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
   * Load type-level search indexing for a content type (CD-10). No design lock is required.
   * Reflects the root mapper field-set {@code isUserSearchable} (Workbench Properties {@code
   * Enable searching for this Content Type}). Default is on when the field-set is missing.
   * Distinct from per-field {@code searchable}.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return envelope, or {@code null} when the content type is not found
   */
  ContentTypeSearchIndexing getContentTypeSearchIndexing(URI baseUri, String idOrName);

  /**
   * Enable or disable type-level search indexing (CD-10). Requires a design-session lock already
   * held by the current user (peer lock REST). Does not acquire or release the lock. Persists
   * root mapper field-set {@code setUserSearchable} via {@code IPSContentDesignWs}. Distinct from
   * per-field {@code searchable}.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @param searchIndexing {@code true} to allow indexing, {@code false} to disable type-level
   *     search
   * @return updated envelope, or {@code null} when not found
   * @throws ContentTypeDesignLockException when no lock is held or the lock is owned by another
   *     user
   */
  ContentTypeSearchIndexing setContentTypeSearchIndexing(
      URI baseUri, String idOrName, boolean searchIndexing);

  /**
   * Load the content type icon strategy (CD-11). No design lock is required. {@code none} has no
   * value. {@code specified} is a file path/name. {@code fromFileField} is a file field name.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @return icon envelope, or {@code null} when not found
   */
  ContentTypeIcon getContentTypeIcon(URI baseUri, String idOrName);

  /**
   * Set the content type icon strategy (CD-11). Requires a design-session lock already held by the
   * current user. Does not acquire or release the lock. {@code none} clears value. Non-{@code
   * none} with a blank value is invalid. Does not upload icon binaries.
   *
   * @param baseUri requesting URI
   * @param idOrName content type uuid (numeric) or internal name
   * @param source {@code none}, {@code specified}, or {@code fromFileField}
   * @param value file path/name or field name; ignored when source is {@code none}
   * @return persisted icon envelope, or {@code null} when not found
   * @throws ContentTypeDesignLockException when no lock is held or the lock is owned by another
   *     user
   * @throws IllegalArgumentException when source is invalid or a non-none value is blank
   */
  ContentTypeIcon setContentTypeIcon(URI baseUri, String idOrName, String source, String value);

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
   * Choice filter, null-entry, and default-selected round-trip on {@code choices} when present.
   *
   * @return envelope, or {@code null} when the content type is not found
   */
  ContentTypeFieldControlProperties getFieldControlProperties(
      URI baseUri, String idOrName, String fieldName);

  /**
   * Replace control property values (and optionally the choice catalog) for one field. Requires a
   * design-session lock already held by the current user. Does not acquire or release the lock.
   * {@code properties} is a full replace (empty clears). {@code choices} null leaves the catalog
   * unchanged. When {@code choices} is present, filter, null-entry, and default-selected are
   * written as part of the catalog replace ({@code type} none/empty still clears).
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

  /**
   * Add a persistable local field (backend column + display mapping) to an existing content type
   * (CD-03). Requires a design-session lock already held by the current user. Does not acquire or
   * release the lock. Optional {@code fieldSet} names an existing child field set, or creates a
   * named complex child when missing.
   *
   * @param body {@code name} required; unique case-insensitive on the type. Optional {@code
   *     dataType} defaults to {@code text}. Optional {@code searchable}, {@code occurrence} /
   *     {@code required} as on PUT field patches.
   * @return updated detail, or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when input is invalid
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     409} when a field with that name already exists
   */
  ContentTypeDetail addLocalField(URI baseUri, String idOrName, ContentTypeField body);

  /**
   * Remove a local field (backend column mapping + display mapping) from an existing content
   * type (CD-03). Requires a design-session lock already held by the current user. Does not acquire
   * or release the lock. System and shared fields are not removed (CD-04).
   *
   * @return {@code Boolean.TRUE} when deleted; {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when the field is not local
   * @throws jakarta.ws.rs.WebApplicationException {@code 404} when the field name is unknown
   */
  Boolean deleteLocalField(URI baseUri, String idOrName, String fieldName);

  /**
   * Include an existing system or shared field into a content type (CD-04). Requires a
   * design-session lock already held by the current user. Does not acquire or release the lock.
   * Origin stays system/shared (the field is not copied as local). Duplicate include is {@code
   * 409}. Unknown catalog field is {@code 404}.
   *
   * @param body {@code name} required; {@code fieldType} must be {@code system} or {@code shared}
   * @return updated detail, or {@code null} when the content type is not found
   * @throws ContentTypeDesignLockException when no lock is held or another user owns the lock
   * @throws IllegalArgumentException when input is invalid
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     409} when the field is already on the type; {@code 404} when the catalog field is unknown
   */
  ContentTypeDetail includeField(URI baseUri, String idOrName, ContentTypeField body);

  /**
   * Import one Workbench-equivalent content-type design XML (CD-14).
   *
   * <p>Creates a new content type via {@code IPSContentDesignWs} ({@code createContentTypes} then
   * {@code saveContentTypes} with {@code release=true}). Does not steal design locks on existing
   * types. Name collision is a conflict (HTTP 409) — this surface does not replace an existing
   * type. Admin only.
   *
   * @param baseUri the base URI (reserved for HATEOAS)
   * @param xml Workbench / REST-export {@code ItemDefData} design XML
   * @return created detail (never {@code null}); {@link ContentTypeDetail#getName()} round-trips
   *     the imported name
   * @throws IllegalArgumentException when XML is blank, not {@code ItemDefData} design XML, or the
   *     name is invalid
   * @throws jakarta.ws.rs.WebApplicationException {@code 409} when a content type with that name
   *     already exists; {@code 403} when the caller is not Admin
   */
  ContentTypeDetail importContentType(URI baseUri, String xml);
}
