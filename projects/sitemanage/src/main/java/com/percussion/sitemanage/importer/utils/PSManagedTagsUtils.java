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
package com.percussion.sitemanage.importer.utils;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.notNull;

import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;

/**
 * Utility class for identifying and handling managed tags in HTML documents. Managed tags include
 * certain meta and script tags that are handled by the CMS.
 */
public class PSManagedTagsUtils {

  private static final String HTTP_EQUIV = "http-equiv";
  private static final String PROPERTY = "property";
  private static final String NAME = "name";
  private static final String COMMENT_START = "<!--";
  private static final String COMMENT_END = "-->";
  private static final List<String> HTTP_EQUIV_TO_EXCLUDE = asList("content-type");
  private static final String DESCRIPTION_META_NAME = "description";
  private static final List<String> META_NAMES_TO_EXCLUDE =
      asList("generator", "robots", DESCRIPTION_META_NAME);
  private static final List<String> META_PROPERTIES_TO_EXCLUDE =
      asList(
          "dcterms:author",
          "dcterms:type",
          "dcterms:source",
          "dcterms:created",
          "dcterms:alternative",
          "perc:tags",
          "perc:tags",
          "perc:category",
          "perc:calendar",
          "perc:start_date",
          "perc:end_date");
  private static final String SRC = "src";
  private static final String[] MANAGED_JS_FILENAMES =
      new String[] {
        "jquery.js",
        "jquery.min.js",
        "jquery.ui.core.js",
        "jquery.tools.min.js",
        "jquery-latest.js",
        "jquery-ui.min.js",
        "jquery.ui.js"
      };
  private static final String[] MANAGED_JS_PATTERN =
      new String[] {"jquery-[\\d].*", "jquery-ui-[\\d].*"};
  private static final String SCRIPT = "script";

  /**
   * Checks if the given tag is a managed reference to a jQuery or JavaScript file.
   *
   * @param tag Element to verify. Must not be null.
   * @return true if the tag references a managed JS file, false otherwise.
   */
  public static boolean isManagedJSReference(Element tag) {
    notNull(tag);
    if (!equalsIgnoreCase(tag.tagName(), SCRIPT)) {
      return false;
    }
    var srcAttr = tag.attr(SRC);
    var filename = getFilenameFromSrcAttribute(srcAttr);
    return matchByfilename(filename) || matchByPattern(filename);
  }

  /**
   * Checks if the given tag is managed by the CMS. Managed tags include certain meta and property
   * tags.
   *
   * @param metaTag Element to inspect, must not be null.
   * @return true if the tag is managed by the CMS, false otherwise.
   */
  public static boolean isManagedMetadataTag(Element metaTag) {
    notNull(metaTag);
    var httpEquivAttr = metaTag.attr(HTTP_EQUIV);
    if (isNotBlank(httpEquivAttr) && HTTP_EQUIV_TO_EXCLUDE.contains(httpEquivAttr.toLowerCase())) {
      return true;
    }
    var nameAttr = metaTag.attr(NAME);
    if (isNotBlank(nameAttr) && META_NAMES_TO_EXCLUDE.contains(nameAttr.toLowerCase())) {
      return true;
    }
    var propertyAttr = metaTag.attr(PROPERTY);
    if (isNotBlank(propertyAttr)
        && META_PROPERTIES_TO_EXCLUDE.contains(propertyAttr.toLowerCase())) {
      return true;
    }
    return false;
  }

  /**
   * Checks if the given tag is the description meta tag.
   *
   * @param metaTag The tag to check, not null.
   * @return true if it is the description tag, false otherwise.
   */
  public static boolean isDescriptionMetaTag(Element metaTag) {
    notNull(metaTag);
    return DESCRIPTION_META_NAME.equalsIgnoreCase(metaTag.attr(NAME));
  }

  /**
   * Removes the given tag from the DOM and adds a comment with the tag's text.
   *
   * @param docHead Element with the DOM to modify. Must not be null.
   * @param tag Element with the tag to comment. Must not be null.
   */
  public static void commentTag(Element docHead, Element tag) {
    notNull(docHead);
    notNull(tag);
    var commentedTag = new Comment(tag.toString());
    docHead.appendChild(commentedTag);
    tag.remove();
  }

  /**
   * Comments the given tag text.
   *
   * @param tagText String with the text of the tag to comment, must not be null but may be empty.
   * @return String with the commented tag.
   */
  public static String commentTagText(String tagText) {
    notNull(tagText);
    return COMMENT_START + tagText + COMMENT_END;
  }

  /**
   * Checks if the given filename matches managed JS patterns.
   *
   * @param filename String, may be null or empty.
   * @return true if the source references a managed JS file, false otherwise.
   */
  private static boolean matchByPattern(String filename) {
    if (isBlank(filename)) {
      return false;
    }
    for (var regex : MANAGED_JS_PATTERN) {
      var pattern = Pattern.compile(regex);
      if (pattern.matcher(filename).matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the given filename matches managed JS filenames.
   *
   * @param filename String, may be null or empty.
   * @return true if the source references a managed JS file, false otherwise.
   */
  private static boolean matchByfilename(String filename) {
    if (isBlank(filename)) {
      return false;
    }
    for (var managedJSFilename : MANAGED_JS_FILENAMES) {
      if (equalsIgnoreCase(filename, managedJSFilename)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the filename from a source reference.
   *
   * @param srcAttr String with the source reference, assumed not null.
   * @return String with the filename, never null but may be empty.
   */
  private static String getFilenameFromSrcAttribute(String srcAttr) {
    var pathParts = srcAttr.split("/");
    var filename = pathParts[pathParts.length - 1];
    if (filename.indexOf('?') >= 0) {
      return filename.substring(0, filename.indexOf('?'));
    }
    return filename;
  }
}
