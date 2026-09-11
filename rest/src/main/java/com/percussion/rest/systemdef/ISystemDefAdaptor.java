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

package com.percussion.rest.systemdef;

import java.net.URI;

public interface ISystemDefAdaptor {

  /**
   * Load the content-editor system definition field catalog.
   *
   * @param baseUri reserved for HATEOAS
   * @return detail, never {@code null} (empty fields when def missing)
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  SystemDefDetail getSystemDef(URI baseUri);

  /**
   * Persist patches to existing system-def field properties. Admin only. Acquires the system-def
   * design lock for this request and releases it on save.
   *
   * <p>Supports patches to existing fields ({@code searchable}, occurrence / required). Null or
   * empty {@code fields} leaves the catalog unchanged and does not rewrite the system-def XML. Does
   * not create or delete fields. {@code dataType}, {@code readOnly}, and {@code
   * cacheTimeoutMinutes} are not written.
   *
   * @return persisted detail, never {@code null}
   * @throws IllegalArgumentException when input is invalid, a field name is unknown, or {@code
   *     occurrence} and {@code required} conflict
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  SystemDefDetail updateSystemDef(URI baseUri, SystemDefDetail body);

  /**
   * Add a persistable TYPE_SYSTEM field (backend column + display mapping) to the content-editor
   * system definition. Admin only. Acquires the system-def design lock for this request and
   * releases it on save.
   *
   * @param body {@code name} required; unique case-insensitive. Optional {@code dataType} defaults
   *     to {@code text}. Optional {@code searchable}, {@code occurrence} / {@code required} as on
   *     PUT patches.
   * @return persisted detail, never {@code null}
   * @throws IllegalArgumentException when field name/dataType is invalid or occurrence/required
   *     conflict
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     409} when a field with that name already exists
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  SystemDefDetail addField(URI baseUri, SystemDefFieldSummary body);

  /**
   * Remove a field (backend column mapping + display mapping) from the content-editor system
   * definition. Admin only. Acquires the system-def design lock for this request and releases it on
   * save.
   *
   * @throws IllegalArgumentException when the name is blank, unknown, or the field is
   *     system-mandatory / system-internal
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  void deleteField(URI baseUri, String fieldName);

  /**
   * Load control property values and the choice catalog for one system-def field (CD-16 / CD-07).
   * Admin only. No design lock is required. Empty {@code properties} means none configured. {@code
   * choices} is null when none.
   *
   * @return envelope, or {@code null} when the field name is unsafe (maps to 404)
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin; {@code
   *     404} when the field is unknown
   */
  SystemDefControlProperties getFieldControlProperties(URI baseUri, String fieldName);

  /**
   * Replace control property values (and optionally the choice catalog) for one system-def field.
   * Admin only. Acquires the system-def design lock for this request and releases it on save.
   * {@code properties} is a full replace (empty clears). {@code choices} null leaves the catalog
   * unchanged.
   *
   * @return persisted envelope, or {@code null} when the field name is unsafe (maps to 404)
   * @throws SystemDefFieldNotFoundException when the field does not exist
   * @throws IllegalArgumentException when the path name is blank, properties is missing, or a
   *     choice catalog is invalid
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  SystemDefControlProperties replaceFieldControlProperties(
      URI baseUri, String fieldName, SystemDefControlProperties body);

  /**
   * Load command-handler stylesheet associations from the content-editor system definition (CD-16).
   * Admin only. No design lock is required.
   *
   * @return envelope, never {@code null} (empty handlers when none)
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  SystemDefStylesheets getStylesheets(URI baseUri);

  /**
   * Replace command-handler stylesheet associations. Admin only. Acquires the system-def design
   * lock for this request and releases it on save. Full replace of {@code handlers}: omitted
   * handlers are removed; a blank {@code href} removes that handler. At least one remaining
   * handler with a valid href is required.
   *
   * @return persisted envelope, never {@code null}
   * @throws IllegalArgumentException when handlers is missing, empty after clear, a name/href is
   *     invalid, or a command handler is duplicated
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  SystemDefStylesheets replaceStylesheets(URI baseUri, SystemDefStylesheets body);

  /**
   * Load command-handler application-flow redirects from the content-editor system definition
   * (CD-16). Admin only. No design lock is required.
   *
   * @return envelope, never {@code null} (empty handlers when none)
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   */
  SystemDefApplicationFlow getApplicationFlow(URI baseUri);

  /**
   * Replace command-handler application-flow default redirects. Admin only. Acquires the system-def
   * design lock for this request and releases it on save. Full replace of {@code handlers}: omitted
   * handlers are removed. Empty {@code href} keeps the handler with an empty default path. At least
   * one remaining handler is required.
   *
   * @return persisted envelope, never {@code null}
   * @throws IllegalArgumentException when handlers is missing, empty after omit, a name/href is
   *     invalid, or a command handler is duplicated
   * @throws jakarta.ws.rs.WebApplicationException {@code 403} when the caller is not Admin
   * @throws SystemDefDesignLockException when the system def is locked by another user or is not
   *     locked for this session
   */
  SystemDefApplicationFlow replaceApplicationFlow(URI baseUri, SystemDefApplicationFlow body);
}
