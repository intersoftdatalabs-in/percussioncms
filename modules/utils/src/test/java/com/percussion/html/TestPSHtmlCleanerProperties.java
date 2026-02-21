/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import javax.xml.transform.TransformerException;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;

/** Tests to validate that the html cleaner properties are working properly. */
public class TestPSHtmlCleanerProperties {

  @Test
  public void testDefaultProps() {
    Properties props = PSHtmlUtils.getDefaultCleanerProperties();

    assertNotNull(props);

    assertFalse(props.isEmpty());

    Safelist sl = PSHtmlUtils.getSafeListFromProperties(props, "");

    assertNotNull(sl);
  }

  @Test
  public void testFragment1() throws PSHtmlParsingException, TransformerException {

    String text =
        new Scanner(
                Objects.requireNonNull(
                    TestPSHtmlCleanerProperties.class.getResourceAsStream(
                        "/com/percussion/html/fragment1.html")),
                "UTF-8")
            .useDelimiter("\\A")
            .next();

    assertNotNull(text);

    Document doc = PSHtmlUtils.createHTMLDocument(text, StandardCharsets.UTF_8, false, null);
    assertNotNull(doc);
    System.out.println(doc.html());
    doc = PSHtmlUtils.createHTMLDocument(text, StandardCharsets.UTF_8, true, null);
    assertNotNull(doc);
    String out = doc.html();
    System.out.println(doc.html());
    assertTrue(out.contains("<aside>"));
    assertTrue(out.contains("</aside>"));
    assertTrue(out.contains("<footer>"));
    assertTrue(out.contains("</footer>"));
    assertTrue(out.contains("🤡 🤥"));
    assertTrue(out.contains("<div class=\"rxbodyfield\">"));
    assertTrue(out.contains("</div"));
    assertTrue(out.contains("<br />"));
    assertTrue(out.contains("<script>"));
    assertTrue(out.contains("</script>"));
  }

  @Test
  public void testFragment2() throws PSHtmlParsingException, TransformerException {

    String text =
        new Scanner(
                Objects.requireNonNull(
                    TestPSHtmlCleanerProperties.class.getResourceAsStream(
                        "/com/percussion/html/fragment2.html")),
                "UTF-8")
            .useDelimiter("\\A")
            .next();

    Document doc = PSHtmlUtils.createHTMLDocument(text, StandardCharsets.UTF_8, false, null);
    String parsed = doc.html();
    assertTrue(parsed.contains("/p>"));
  }

  @Test
  public void testFragment3() throws PSHtmlParsingException, TransformerException {

    String text =
        new Scanner(
                Objects.requireNonNull(
                    TestPSHtmlCleanerProperties.class.getResourceAsStream(
                        "/com/percussion/html/fragment3.html")),
                "UTF-8")
            .useDelimiter("\\Z")
            .next();
    Document doc = PSHtmlUtils.createHTMLDocument(text, StandardCharsets.UTF_8, true, null);
    String parsed = doc.body().toString();
    assertEquals(text, parsed);
  }

  @Test
  public void testMoreLink() throws PSHtmlParsingException, TransformerException {

    String text =
        new Scanner(
                Objects.requireNonNull(
                    TestPSHtmlCleanerProperties.class.getResourceAsStream(
                        "/com/percussion/html/morelink.html")),
                "UTF-8")
            .useDelimiter("\\Z")
            .next();
    Document doc = PSHtmlUtils.createHTMLDocument(text, StandardCharsets.UTF_8, true, null);
    String parsed = doc.body().toString();
    assertTrue(parsed.contains("<span class=\"perc-blog-more-link\"></span>"));
  }

  @Test
  public void testRemoveDataPathItem() throws PSHtmlParsingException {

    String text =
        new Scanner(
                Objects.requireNonNull(
                    TestPSHtmlCleanerProperties.class.getResourceAsStream(
                        "/com/percussion/html/datapathitem.html")),
                "UTF-8")
            .useDelimiter("\\Z")
            .next();
    Document doc = PSHtmlUtils.createHTMLDocument(text, StandardCharsets.UTF_8, true, null);
    String parsed = doc.body().toString();

    assertFalse(parsed.contains("data-pathitem"));
    assertTrue(parsed.contains("data-jcrpath"));
  }

  /**
   * Helper method to cleanse an HTML fragment using the default html-cleaner.properties and return
   * the cleansed result.
   */
  private String cleanseFragment(String html) {
    return PSHtmlUtils.cleanseHTMLContent(html, StandardCharsets.UTF_8, null);
  }

  @Test
  public void testRoleAttributePreservedOnTable() {
    String input = "<table role=\"presentation\" border=\"1\"><tr><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue(
        result.contains("role=\"presentation\""), "role attribute should be preserved on table");
  }

  @Test
  public void testRoleAttributePreservedOnDiv() {
    String input = "<div role=\"navigation\">Nav content</div>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("role=\"navigation\""), "role attribute should be preserved on div");
  }

  @Test
  public void testRoleAttributePreservedOnNav() {
    String input = "<nav role=\"navigation\">Nav content</nav>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("role=\"navigation\""), "role attribute should be preserved on nav");
  }

  @Test
  public void testTableAttributes() {
    String input =
        "<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\" role=\"grid\" summary=\"Data table\" width=\"100%\" align=\"center\" height=\"200\">"
            + "<tr><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("border=\"1\""), "border should be preserved on table");
    assertTrue(result.contains("cellpadding=\"5\""), "cellpadding should be preserved on table");
    assertTrue(result.contains("cellspacing=\"0\""), "cellspacing should be preserved on table");
    assertTrue(result.contains("role=\"grid\""), "role should be preserved on table");
    assertTrue(result.contains("summary=\"Data table\""), "summary should be preserved on table");
    assertTrue(result.contains("width=\"100%\""), "width should be preserved on table");
    assertTrue(result.contains("align=\"center\""), "align should be preserved on table");
    assertTrue(result.contains("height=\"200\""), "height should be preserved on table");
  }

  @Test
  public void testTdAttributes() {
    String input =
        "<table><tr><td colspan=\"2\" rowspan=\"1\" height=\"50\" align=\"center\" valign=\"top\" bgcolor=\"#ff0000\">Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("colspan=\"2\""), "colspan should be preserved on td");
    assertTrue(result.contains("rowspan=\"1\""), "rowspan should be preserved on td");
    assertTrue(result.contains("height=\"50\""), "height should be preserved on td");
    assertTrue(result.contains("align=\"center\""), "align should be preserved on td");
    assertTrue(result.contains("valign=\"top\""), "valign should be preserved on td");
    assertTrue(result.contains("bgcolor=\"#ff0000\""), "bgcolor should be preserved on td");
  }

  @Test
  public void testThAttributes() {
    String input =
        "<table><tr><th scope=\"col\" colspan=\"2\" height=\"40\" align=\"left\" valign=\"middle\" bgcolor=\"#cccccc\">Header</th></tr></table>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("scope=\"col\""), "scope should be preserved on th");
    assertTrue(result.contains("colspan=\"2\""), "colspan should be preserved on th");
    assertTrue(result.contains("height=\"40\""), "height should be preserved on th");
    assertTrue(result.contains("align=\"left\""), "align should be preserved on th");
    assertTrue(result.contains("valign=\"middle\""), "valign should be preserved on th");
    assertTrue(result.contains("bgcolor=\"#cccccc\""), "bgcolor should be preserved on th");
  }

  @Test
  public void testTrAttributes() {
    String input =
        "<table><tr align=\"center\" valign=\"top\" height=\"30\" bgcolor=\"#eeeeee\"><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("align=\"center\""), "align should be preserved on tr");
    assertTrue(result.contains("valign=\"top\""), "valign should be preserved on tr");
    assertTrue(result.contains("height=\"30\""), "height should be preserved on tr");
    assertTrue(result.contains("bgcolor=\"#eeeeee\""), "bgcolor should be preserved on tr");
  }

  @Test
  public void testTheadTbodyTfootAttributes() {
    String input =
        "<table>"
            + "<thead align=\"center\" valign=\"top\"><tr><th>Header</th></tr></thead>"
            + "<tbody align=\"left\" valign=\"middle\"><tr><td>Body</td></tr></tbody>"
            + "<tfoot align=\"right\" valign=\"bottom\"><tr><td>Footer</td></tr></tfoot>"
            + "</table>";
    String result = cleanseFragment(input);
    // thead
    assertTrue(result.contains("<thead align=\"center\""), "align should be preserved on thead");
    assertTrue(result.contains("valign=\"top\""), "valign should be preserved on thead");
    // tbody
    assertTrue(result.contains("<tbody align=\"left\""), "align should be preserved on tbody");
    // tfoot
    assertTrue(result.contains("<tfoot align=\"right\""), "align should be preserved on tfoot");
  }

  @Test
  public void testCaptionAttributes() {
    String input =
        "<table><caption align=\"bottom\">Table Caption</caption><tr><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("align=\"bottom\""), "align should be preserved on caption");
    assertTrue(result.contains("Table Caption"), "caption content should be preserved");
  }

  @Test
  public void testFontAttributes() {
    String input = "<font color=\"red\" face=\"Arial\" size=\"3\">Styled text</font>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("color=\"red\""), "color should be preserved on font");
    assertTrue(result.contains("face=\"Arial\""), "face should be preserved on font");
    assertTrue(result.contains("size=\"3\""), "size should be preserved on font");
  }

  @Test
  public void testHrAttributes() {
    String input = "<hr width=\"50%\" size=\"2\" align=\"center\" />";
    String result = cleanseFragment(input);
    assertTrue(result.contains("width=\"50%\""), "width should be preserved on hr");
    assertTrue(result.contains("size=\"2\""), "size should be preserved on hr");
    assertTrue(result.contains("align=\"center\""), "align should be preserved on hr");
  }

  @Test
  public void testVideoPlaysinline() {
    String input =
        "<video src=\"video.mp4\" controls=\"controls\" playsinline=\"playsinline\" width=\"640\" height=\"480\"></video>";
    String result = cleanseFragment(input);
    assertTrue(result.contains("playsinline"), "playsinline should be preserved on video");
    assertTrue(result.contains("controls"), "controls should be preserved on video");
    assertTrue(result.contains("width=\"640\""), "width should be preserved on video");
    assertTrue(result.contains("height=\"480\""), "height should be preserved on video");
  }

  @Test
  public void testSvgAttributes() {
    String input =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"100\" height=\"100\" fill=\"none\" stroke=\"black\" preserveAspectRatio=\"xMidYMid meet\"></svg>";
    String result = cleanseFragment(input);
    assertTrue(
        result.contains("xmlns=\"http://www.w3.org/2000/svg\""),
        "xmlns should be preserved on svg");
    assertTrue(result.contains("viewBox=\"0 0 100 100\""), "viewBox should be preserved on svg");
    assertTrue(result.contains("width=\"100\""), "width should be preserved on svg");
    assertTrue(result.contains("height=\"100\""), "height should be preserved on svg");
    assertTrue(result.contains("fill=\"none\""), "fill should be preserved on svg");
    assertTrue(result.contains("stroke=\"black\""), "stroke should be preserved on svg");
    assertTrue(
        result.contains("preserveAspectRatio=\"xMidYMid meet\""),
        "preserveAspectRatio should be preserved on svg");
  }

  @Test
  public void testComplexTableWithRoles() {
    // Simulates a TinyMCE rich text table with role, scope, and formatting attributes
    String input =
        "<table role=\"presentation\" border=\"1\" cellpadding=\"5\" cellspacing=\"0\" width=\"100%\">"
            + "<caption align=\"top\">Monthly Report</caption>"
            + "<thead align=\"center\" valign=\"middle\">"
            + "<tr bgcolor=\"#f0f0f0\"><th scope=\"col\" align=\"left\">Month</th><th scope=\"col\" align=\"right\">Revenue</th></tr>"
            + "</thead>"
            + "<tbody>"
            + "<tr><td align=\"left\" valign=\"top\" height=\"30\">January</td><td align=\"right\" bgcolor=\"#e0ffe0\">$1,000</td></tr>"
            + "</tbody>"
            + "<tfoot align=\"center\">"
            + "<tr><td colspan=\"2\" align=\"center\"><strong>Total: $1,000</strong></td></tr>"
            + "</tfoot>"
            + "</table>";
    String result = cleanseFragment(input);

    // Table-level
    assertTrue(result.contains("role=\"presentation\""), "table role should be preserved");
    assertTrue(result.contains("border=\"1\""), "table border should be preserved");

    // Caption
    assertTrue(result.contains("Monthly Report"), "caption should be preserved");

    // Thead
    assertTrue(result.contains("<thead"), "thead should be preserved");

    // Th scope
    assertTrue(result.contains("scope=\"col\""), "th scope should be preserved");

    // Td attributes
    assertTrue(result.contains("align=\"left\""), "td align should be preserved");
    assertTrue(result.contains("colspan=\"2\""), "td colspan should be preserved");

    // Tr bgcolor
    assertTrue(result.contains("bgcolor=\"#f0f0f0\""), "tr bgcolor should be preserved");
  }
}
