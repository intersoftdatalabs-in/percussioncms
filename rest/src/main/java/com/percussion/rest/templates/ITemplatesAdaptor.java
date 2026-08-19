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

// REFACTORED: CP-JAVA11

package com.percussion.rest.templates;

import java.net.URI;
import java.util.List;

/**
 * Adaptor interface for Template operations. Sunny Sal: "Template adaptor ka hero, assembly ka
 * zero!"
 */
public interface ITemplatesAdaptor {

  /**
   * Lists all template summaries (design catalog).
   *
   * @param baseUri the base URI
   * @return list of template summaries
   */
  List<TemplateSummary> listAllTemplateSummaries(URI baseUri);

  /**
   * Lists template summaries for the given filter.
   *
   * @param baseUri the base URI
   * @param filter the filter to apply
   * @return list of template summaries
   */
  List<TemplateSummary> listTemplateSummaries(URI baseUri, TemplateFilter filter);

  /**
   * Load a single template design detail by numeric id or unique name.
   *
   * @param baseUri the base URI (reserved for HATEOAS)
   * @param idOrName template uuid or name
   * @return detail or {@code null} if not found
   */
  TemplateDetail getTemplate(URI baseUri, String idOrName);

  /**
   * Update mutable template design fields (label, description, source, assembler) and optionally
   * bindings / contained slots. When {@code body.assembler} is non-null, sets the assembler
   * extension name (must be non-blank). When {@code body.bindings} or {@code body.slots} is
   * non-null, that collection is fully replaced (empty list clears). Name/id remain out of scope.
   *
   * @return updated detail, or {@code null} if not found
   */
  TemplateDetail updateTemplate(URI baseUri, String idOrName, TemplateDetail body);

  /**
   * Create a modern assembly template (package/manifest model — no Widget definition XML).
   *
   * <p>{@code body.name} is required (no spaces; unique). Optional label, description, assembler,
   * and templateSource. When assembler is omitted, the adaptor uses the HTML-first default.
   *
   * @return created detail (never {@code null})
   * @throws IllegalArgumentException when the name is invalid or already exists
   */
  TemplateDetail createTemplate(URI baseUri, TemplateDetail body);

  /**
   * Delete an assembly template by numeric id or unique name (no Widget XML).
   *
   * @param baseUri the base URI (reserved for HATEOAS)
   * @param idOrName template uuid or name
   * @return {@code true} if deleted, {@code false} if not found
   * @throws IllegalArgumentException when {@code idOrName} is blank
   */
  boolean deleteTemplate(URI baseUri, String idOrName);
}
