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

package com.percussion.share.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.share.service.exception.PSExtractHTMLException;
import java.io.InputStream;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PSHtmlUtils}. Sunny Sal: "HTML utils, Java 11, and tag ka hero!" This test must
 * run in a server environment because it relies on PSServer.getRxDir() to load tidy property file.
 */

@Tag("integration")
public class PSHtmlUtilsTest {

  private static final String MISSING_P_TAG = "<div> <p>Hello</div>";
  // Could this be moved to a separate html file in a testing resources location?
  private static final String VALID_HTML =
      "<html><head> <title>peter</title> </header> <body> <link rel=\"canonical\""
          + " href=\"/myparentfolder\" /> <div> <p>Hello</div> </body></html>";
  private static final String VALID_HTML_NO_CANONICAL =
      "<html><head> <title>peter</title> </header> <body>  <div> <p>Hello</div> </body></html>";

  @Test
  void testExtractionNegative() throws Exception {
    var html = PSHtmlUtils.extractHtml("unknownTag", MISSING_P_TAG, null, true);
    assertTrue(StringUtils.isBlank(html));

    Exception thrown =
        assertThrows(
            PSExtractHTMLException.class,
            () -> PSHtmlUtils.extractHtml("foo (2)", MISSING_P_TAG, null, true));
    // Bad input (or bad selector)
    System.out.println(thrown.getMessage());
  }

  @Test
  void testExtract() throws Exception {
    var html = PSHtmlUtils.extractHtml("div", MISSING_P_TAG, null, true);
    assertTrue(html.contains("<div>"));
    assertTrue(html.contains("</div>"));
    assertTrue(html.contains("<p>"));
    assertTrue(html.contains("</p>"));

    html = PSHtmlUtils.extractHtml("div", MISSING_P_TAG, null, false);
    assertFalse(html.contains("<div>"));
    assertFalse(html.contains("</div>"));
    assertTrue(html.contains("<p>"));
    assertTrue(html.contains("</p>"));

    html = PSHtmlUtils.extractHtml("body", VALID_HTML, null, false);
    assertTrue(html.contains("Hello"));
    assertFalse(html.contains("<body>"));
    assertFalse(html.contains("</body>"));
    assertFalse(html.contains("peter"));

    html = PSHtmlUtils.extractHtml("body", VALID_HTML, null, true);
    assertTrue(html.contains("<body>"));
    assertTrue(html.contains("</body>"));
    assertTrue(html.contains("Hello"));
    assertFalse(html.contains("peter"));
  }

  /** Test against some real site home pages. */
  @Test
  void testHtmlFile() throws Exception {
    System.out.println("Testing home_bst.html....");
    validateHtmlFile("home_bst.html");

    System.out.println("Testing home_cc.html....");
    validateHtmlFile("home_cc.html");

    System.out.println("Testing home_sw.html....");
    validateHtmlFile("home_sw.html");

    System.out.println("Testing home_ws.html....");
    validateHtmlFile("home_ws.html");

    Exception thrown =
        assertThrows(
            Exception.class,
            () -> {
              System.out.println("Testing home_w.html....");
              validateHtmlFile("home_w.html");
            });
    // tidy fail on "unknown" tag -- <MACRO_PREVIEWMENUITEM>
    // this can be "fixed" by add "macro_previewmenuitem" into "new-empty-tags" properties
    System.out.println("Expecting tidy fail on \"home_w.html\".");
  }

  /**
   * Validates the specified HTML file.
   *
   * @param name the name of the HTML file, assumed not empty.
   */
  private void validateHtmlFile(String name) throws Exception {
    try (InputStream in = this.getClass().getResourceAsStream(name)) {
      var htmlSource = IOUtils.toString(in);
      var html = PSHtmlUtils.extractHtml("body", htmlSource, name, true);
      assertTrue(html.contains("<body"));

      html = PSHtmlUtils.extractHtml("body", htmlSource, name, false);
      assertFalse(html.contains("<body"));
    }
  }

  /**
   * Test two sample htmls, one that contains a canonical link and one that does not. Additionally
   * test something that is not html.
   */
  @Test
  void testCheckLinkCanonicalElement() {
    assertTrue(PSHtmlUtils.checkLinkCanonicalElement(VALID_HTML));
    assertFalse(PSHtmlUtils.checkLinkCanonicalElement(VALID_HTML_NO_CANONICAL));
    assertFalse(PSHtmlUtils.checkLinkCanonicalElement(UUID.randomUUID().toString()));
  }

  /**
   * Test stripping of canonical element from a sample html, ensure it changes and equals a
   * non-canonical version of the html. Try stripping a canonical element from a sample html and a
   * random string that do not contain canonical elements. Ensure they are unchanged.
   */
  @Test
  void testStripLinkCanonicalElement() {
    var strippedHtml = PSHtmlUtils.stripLinkCanonicalElement(VALID_HTML);
    assertEquals(VALID_HTML_NO_CANONICAL, strippedHtml);
    assertNotEquals(VALID_HTML, strippedHtml);

    var unchangedHtmlString = PSHtmlUtils.stripLinkCanonicalElement(VALID_HTML_NO_CANONICAL);
    assertEquals(VALID_HTML_NO_CANONICAL, unchangedHtmlString);

    var unchangedString = UUID.randomUUID().toString();
    assertEquals(unchangedString, PSHtmlUtils.stripLinkCanonicalElement(unchangedString));
  }
}
