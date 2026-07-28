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
package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRenderResult;
import com.percussion.pagemanagement.data.PSTemplate;

/**
 * Renders templates, pages, and regions.
 *
 * @author adamgent
 */
public interface IPSRenderService {

  /**
   * Assembles a single region that appears on the supplied page and returns the serialized version.
   * Sets the edit mode flag. See {@link #renderPageForEdit(String, String)} for details of this
   * flag.
   *
   * @param page The page to assemble. Not {@code null}.
   * @param regionId The name of the region within the supplied page. Not blank.
   * @return Just the rendered region as a string within the returned object. The supplied {@code
   *     regionId} is set on the returned object.
   * @throws PSRenderServiceException if rendering fails
   */
  PSRenderResult renderRegion(PSPage page, String regionId) throws PSRenderServiceException;

  /**
   * Renders all regions for the given template.
   *
   * @param template the template to render
   * @return the rendered regions as a string
   * @throws PSRenderServiceException if rendering fails
   */
  String renderRegionAll(PSTemplate template) throws PSRenderServiceException;

  /**
   * Renders a specific region for the given template.
   *
   * @param template the template
   * @param regionId the region ID
   * @return the rendered region result
   * @throws PSRenderServiceException if rendering fails
   */
  PSRenderResult renderRegion(PSTemplate template, String regionId) throws PSRenderServiceException;

  /**
   * Renders the template by ID.
   *
   * @param id the template ID
   * @return the rendered template as a string
   * @throws PSRenderServiceException if rendering fails
   */
  String renderTemplate(String id) throws PSRenderServiceException;

  /**
   * Similar to {@link #renderTemplate(String)}, except sets a scriptsOff variable as true to the
   * context. So that the macros can render the template by stripping the script tags.
   *
   * @param id the string format of template item GUID
   * @return The rendered template, typically an (x)html document. Never {@code null} or empty.
   * @throws PSRenderServiceException if rendering fails
   */
  String renderTemplateScriptsOff(String id) throws PSRenderServiceException;

  /**
   * Renders the page by ID.
   *
   * @param id the page ID
   * @return the rendered page as a string
   * @throws PSRenderServiceException if rendering fails
   */
  String renderPage(String id) throws PSRenderServiceException;

  /**
   * Renders a page for full-text search HTML extraction on a clean transaction.
   *
   * <p>Used by {@code PSExtractHtmlContent} from the FTS index queue thread. Must use {@code
   * REQUIRES_NEW} so assembly/Hibernate is not nested inside the legacy content-editor load
   * transaction (which otherwise leaves a null JDBC connection and aborts indexing).
   *
   * @param id page GUID string
   * @return rendered HTML, never {@code null} (empty if render fails is handled by the caller)
   * @throws PSRenderServiceException if rendering fails
   */
  String renderPageForSearchIndex(String id) throws PSRenderServiceException;

  /**
   * Similar to {@link #renderPage(String)}, except sets a flag that can be used by widgets if they
   * want or need to render their output differently when a page is being edited. For example, if a
   * widget has no content, it may render some sample content or it may allow in-line editing of its
   * content. Each widget is responsible for what is rendered in this situation.
   *
   * @param id The unique identifier of the page to be rendered. Not blank.
   * @param editType The edit type.
   * @return The rendered page, typically an (x)html document. Never {@code null} or empty.
   * @throws PSRenderServiceException if rendering fails
   */
  String renderPageForEdit(String id, String editType) throws PSRenderServiceException;

  /**
   * Similar to {@link #renderPageForEdit(String, String)}, this method sets scripts off flag to the
   * $perc context. So that the velocity macros can strip the scripts if the flag is set to false.
   *
   * @param id the string format of the GUID of the page
   * @param editType the edit type
   * @return The rendered page, typically an (x)html document. Never {@code null} or empty.
   * @throws PSRenderServiceException if rendering fails
   */
  String renderPageForEditScriptsOff(String id, String editType) throws PSRenderServiceException;

  /**
   * Parses the given HTML into a PSRegion.
   *
   * @param html the HTML to parse
   * @return the parsed region
   * @throws PSRenderServiceException if parsing fails
   */
  PSRegion parse(String html) throws PSRenderServiceException;

  /** Exception thrown for render service errors. */
  class PSRenderServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PSRenderServiceException(String message) {
      super(message);
    }

    public PSRenderServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSRenderServiceException(Throwable cause) {
      super(cause);
    }
  }
}
