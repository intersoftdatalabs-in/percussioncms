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

import static com.percussion.test.TestAssertions.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSManagedTagsUtils}.
 *
 * @author Santiago M. Murchio
 */
class PSManagedTagsUtilsTest {

  @BeforeEach
  void setUp() {
    // No setup needed
  }

  @AfterEach
  void tearDown() {
    // No teardown needed
  }

  @Test
  void testIsManagedJSReference_noScriptTag() {
    var title = new Element(Tag.valueOf("title"), "http://");
    title.html("The title of the page");
    assertFalse(
        PSManagedTagsUtils.isManagedJSReference(title),
        "The element " + title + " should not have been detected as managed.");

    var metadataAttributes = new Attributes();
    metadataAttributes.put("name", "keywords");
    metadataAttributes.put("content", "javascript html add");
    var metadata = new Element(Tag.valueOf("meta"), "http://", metadataAttributes);
    assertFalse(
        PSManagedTagsUtils.isManagedJSReference(metadata),
        "The element " + metadata + " should not have been detected as managed.");

    var aAttributes = new Attributes();
    aAttributes.put("href", "http://javascript.about.com/");
    aAttributes.put("content", "javascript html add");
    var a = new Element(Tag.valueOf("a"), "http://", aAttributes);
    a.html("link text");
    assertFalse(
        PSManagedTagsUtils.isManagedJSReference(a),
        "The element " + a + " should not have been detected as managed.");
  }

  @Test
  void testIsManagedJSReference_externalJSReferences() {
    assertManagedJSElement(EXTERNAL_REFERENCE_PREFIX_1);
    assertManagedJSElement(EXTERNAL_REFERENCE_PREFIX_2);
    assertManagedJSElement(EXTERNAL_REFERENCE_PREFIX_3);

    assertNotManagedJSElement(EXTERNAL_REFERENCE_PREFIX_1);
    assertNotManagedJSElement(EXTERNAL_REFERENCE_PREFIX_2);
    assertNotManagedJSElement(EXTERNAL_REFERENCE_PREFIX_3);
  }

  @Test
  void testIsManagedJSReference_relativeToPageOrSiteReferences() {
    // relative to page
    assertManagedJSElement("");
    assertNotManagedJSElement("");

    // relative to site
    assertManagedJSElement(RELATIVE_TO_SITE_PREFIX_1);
    assertManagedJSElement(RELATIVE_TO_SITE_PREFIX_2);
    assertManagedJSElement(RELATIVE_TO_SITE_PREFIX_3);

    assertNotManagedJSElement(RELATIVE_TO_SITE_PREFIX_1);
    assertNotManagedJSElement(RELATIVE_TO_SITE_PREFIX_2);
    assertNotManagedJSElement(RELATIVE_TO_SITE_PREFIX_3);
  }

  @Test
  void testIsManagedJSReference_CDNReferences() {
    assertManagedJSElement(CDN_REFERENCE_1);
    assertManagedJSElement(CDN_REFERENCE_2);

    assertNotManagedJSElement(RELATIVE_TO_SITE_PREFIX_1);
    assertNotManagedJSElement(RELATIVE_TO_SITE_PREFIX_2);
  }

  @Test
  void testIsManagedMetadataTag_managed() {
    for (var managedTag : MANAGED_META_TAGS) {
      var header = Jsoup.parse(managedTag).head();
      var tag = header.children().get(0);
      assertTrue(
          PSManagedTagsUtils.isManagedMetadataTag(tag),
          "The element " + tag + " should have been detected as managed, but was not.");
    }
  }

  @Test
  void testIsManagedMetadataTag_notManaged() {
    for (var notManagedTag : NOT_MANAGED_META_TAGS) {
      var header = Jsoup.parse(notManagedTag).head();
      var tag = header.children().get(0);
      assertFalse(
          PSManagedTagsUtils.isManagedMetadataTag(tag),
          "The element " + tag + " should not have been detected as managed.");
    }
  }

  /** Checks that the managed element is recognized as managed. */
  private void assertManagedJSElement(String prefix) {
    for (var suffix : MANAGED_JS_SUFFIX) {
      var managedJs = buildJSReferenceTag(prefix + suffix);
      assertTrue(
          PSManagedTagsUtils.isManagedJSReference(managedJs),
          "The element " + managedJs + " should have been detected as managed, but was not.");
    }
  }

  /** Checks that the managed element is not recognized as managed. */
  private void assertNotManagedJSElement(String prefix) {
    for (var suffix : NOT_MANAGED_JS_SUFFIX) {
      var managedJs = buildJSReferenceTag(prefix + suffix);
      assertFalse(
          PSManagedTagsUtils.isManagedJSReference(managedJs),
          "The element " + managedJs + " should not have been detected as managed.");
    }
  }

  /** Builds an {@link Element} to emulate a given tag. */
  private Element buildJSReferenceTag(String src) {
    var tag = Tag.valueOf("script");
    var attributes = new Attributes();
    attributes.put("src", src);
    attributes.put("type", "text/javascript");
    return new Element(tag, "http://", attributes);
  }

  private static final String EXTERNAL_REFERENCE_PREFIX_1 =
      "http://ajax.googleapis.com/ajax/libs/jquery/1.8/";
  private static final String EXTERNAL_REFERENCE_PREFIX_2 =
      "http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/";
  private static final String EXTERNAL_REFERENCE_PREFIX_3 =
      "https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/";

  private static final String RELATIVE_TO_SITE_PREFIX_1 = "/scripts/";
  private static final String RELATIVE_TO_SITE_PREFIX_2 = "/scripts/js/";
  private static final String RELATIVE_TO_SITE_PREFIX_3 = "/scripts/js/min/";

  private static final String CDN_REFERENCE_1 = "http://ajax.aspnetcdn.com/ajax/jQuery/";
  private static final String CDN_REFERENCE_2 = "http://ajax.aspnetcdn.com/ajax/jquery.ui/1.8.18/";

  private static final String[] MANAGED_JS_SUFFIX =
      new String[] {
        "jquery.js",
        "jquery.min.js",
        "jquery.ui.core.js",
        "jquery.tools.min.js",
        "jquery-latest.js",
        "jquery-ui.min.js",
        "jquery-1.3.2.js",
        "jquery-1.8.1.min.js",
        "jquery-1.5.2.min.js",
        "jquery-ui-1.8.12.custom.min.js",
        "jquery-ui-1.7.2.custom.min.js",
        "jquery-ui-1.8.17.custom.min.js",
        "jquery-1.4.2.js",
        "jquery-1.7.1.min.js",
        "jquery-1.4.2.min.js",
        "jquery-1.3.2.min.js",
        "jquery-1.7.2.min.js",
        "jquery-1.7.1.min.js?b=111",
        "jquery.ui.js"
      };

  private static final String[] NOT_MANAGED_JS_SUFFIX =
      new String[] {"jquery-dialog-1.3.2.js", "jquery.carrousel-1.4.2.min.js"};

  private static final String[] MANAGED_META_TAGS =
      new String[] {
        "<meta content=\"text/html; charset=UTF-8\" http-equiv=\"content-type\" />",
        "<meta name=\"generator\" content=\"Percussion\" />",
        "<meta name=\"robots\" content=\"noindex\" />",
        "<meta name=\"description\" content=\"The description of the page\" />",
        "<meta property=\"dcterms:author\" content=\"author of the page\" />",
        "<meta property=\"dcterms:type\" content=\"page\" />",
        "<meta property=\"dcterms:source\" content=\"perc.template.name\" />",
        "<meta property=\"dcterms:created\" datatype=\"xsd:dateTime\" content=\"2012-10-10\" />",
        "<meta property=\"dcterms:alternative\" content=\"perc.page.linkTitle\" />",
        "<meta property=\"perc:tags\" content=\"tag1.String\" />",
        "<meta property=\"perc:tags\" content=\"tag2.String\" />",
        "<meta property=\"perc:category\" content=\"category.String\" />",
        "<meta property=\"perc:calendar\" content=\"Calendar Name\" />",
        "<meta property=\"perc:start_date\" content=\"10/12/2012\" />",
        "<meta property=\"perc:end_date\" datatype=\"xsd:dateTime\" content=\"10/12/2012\" />"
      };

  private static final String[] NOT_MANAGED_META_TAGS =
      new String[] {
        "<meta http-equiv=\"refresh\" content=\"600\" />",
        "<meta http-equiv=\"default-style\" content=\"link_element\" />"
      };
}
