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
   * Update mutable template design fields (label, description, source). Name/id are not changed.
   * Bindings/slots/create-delete remain out of scope.
   *
   * @return updated detail, or {@code null} if not found
   */
  TemplateDetail updateTemplate(URI baseUri, String idOrName, TemplateDetail body);
}
