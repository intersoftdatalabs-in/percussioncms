// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.data;

/**
 * HTML metadata for templates and pages. Provides accessors for doc type, protected regions, and
 * additional HTML content.
 *
 * @author adamgent
 */
public interface IPSHtmlMetadata {

  /**
   * Gets the custom doc type to be used in the template. Example: &lt;!DOCTYPE html&gt;
   *
   * @return never {@code null}, but may be empty.
   */
  PSMetadataDocType getDocType();

  /**
   * Sets the custom doc type to be used in the template. Example: &lt;!DOCTYPE html&gt;
   *
   * @param docType the doc type of the template, never {@code null}
   */
  void setDocType(PSMetadataDocType docType);

  /**
   * Gets the protected region name used to hide content in delivery-published pages. Example:
   * "header"
   *
   * @return never {@code null}, but may be empty.
   */
  String getProtectedRegion();

  /**
   * Sets the protected region name used to hide content in delivery-published pages. Example:
   * "header"
   *
   * @param protectedRegion the name of the protected region, never {@code null}
   */
  void setProtectedRegion(String protectedRegion);

  /**
   * Gets the text to show instead of the code in the protected region when the user is not logged
   * in. Example: "You're not authorized to see this content"
   *
   * @return never {@code null}, but may be empty.
   */
  String getProtectedRegionText();

  /**
   * Sets the text to show instead of the code in the protected region when the user is not logged
   * in. Example: "You're not authorized to see this content"
   *
   * @param protectedRegionText the text to place instead of the content in the protected region,
   *     never {@code null}
   */
  void setProtectedRegionText(String protectedRegionText);

  /**
   * Gets additional HTML that will go in the &lt;head&gt;&lt;/head&gt;.
   *
   * @return never {@code null}, but may be empty.
   */
  String getAdditionalHeadContent();

  /**
   * Sets additional HTML that will go in the &lt;head&gt;&lt;/head&gt;.
   *
   * @param additionalHeadContent additional head content of the page, never {@code null}
   */
  void setAdditionalHeadContent(String additionalHeadContent);

  /**
   * Gets the header of the page. Intended to be used within the HTML right after the &lt;body&gt;
   * tag.
   *
   * @return the header of the page, never {@code null}, but may be empty.
   */
  String getAfterBodyStartContent();

  /**
   * Sets the header of the page.
   *
   * @param header the new header of the page, may be {@code null} or empty.
   */
  void setAfterBodyStartContent(String header);

  /**
   * Gets the footer of the page. Intended to be used right before the &lt;/body&gt; tag in an HTML
   * page.
   *
   * @return the footer of the page, never {@code null}, but may be empty.
   */
  String getBeforeBodyCloseContent();

  /**
   * Sets the footer of the page.
   *
   * @param footer the new footer of the page, may be {@code null} or empty.
   */
  void setBeforeBodyCloseContent(String footer);

  /**
   * Sets the description.
   *
   * @param description may be {@code null} or empty.
   */
  void setDescription(String description);
}
