/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import javax.xml.transform.TransformerException;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.junit.Test;

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
    // jsoup 1.21.2+ correctly treats self-closing span tags as opening tags
    // (span elements cannot be self-closing in HTML)
    assertTrue(parsed.contains("<span class=\"perc-blog-more-link\"> Hi this is more</span>"));
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
        "role attribute should be preserved on table", result.contains("role=\"presentation\""));
  }

  @Test
  public void testRoleAttributePreservedOnDiv() {
    String input = "<div role=\"navigation\">Nav content</div>";
    String result = cleanseFragment(input);
    assertTrue("role attribute should be preserved on div", result.contains("role=\"navigation\""));
  }

  @Test
  public void testRoleAttributePreservedOnNav() {
    String input = "<nav role=\"navigation\">Nav content</nav>";
    String result = cleanseFragment(input);
    assertTrue("role attribute should be preserved on nav", result.contains("role=\"navigation\""));
  }

  @Test
  public void testTableAttributes() {
    String input =
        "<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\" role=\"grid\" summary=\"Data table\" width=\"100%\" align=\"center\" height=\"200\">"
            + "<tr><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue("border should be preserved on table", result.contains("border=\"1\""));
    assertTrue("cellpadding should be preserved on table", result.contains("cellpadding=\"5\""));
    assertTrue("cellspacing should be preserved on table", result.contains("cellspacing=\"0\""));
    assertTrue("role should be preserved on table", result.contains("role=\"grid\""));
    assertTrue("summary should be preserved on table", result.contains("summary=\"Data table\""));
    assertTrue("width should be preserved on table", result.contains("width=\"100%\""));
    assertTrue("align should be preserved on table", result.contains("align=\"center\""));
    assertTrue("height should be preserved on table", result.contains("height=\"200\""));
  }

  @Test
  public void testTdAttributes() {
    String input =
        "<table><tr><td colspan=\"2\" rowspan=\"1\" height=\"50\" align=\"center\" valign=\"top\" bgcolor=\"#ff0000\">Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue("colspan should be preserved on td", result.contains("colspan=\"2\""));
    assertTrue("rowspan should be preserved on td", result.contains("rowspan=\"1\""));
    assertTrue("height should be preserved on td", result.contains("height=\"50\""));
    assertTrue("align should be preserved on td", result.contains("align=\"center\""));
    assertTrue("valign should be preserved on td", result.contains("valign=\"top\""));
    assertTrue("bgcolor should be preserved on td", result.contains("bgcolor=\"#ff0000\""));
  }

  @Test
  public void testThAttributes() {
    String input =
        "<table><tr><th scope=\"col\" colspan=\"2\" height=\"40\" align=\"left\" valign=\"middle\" bgcolor=\"#cccccc\">Header</th></tr></table>";
    String result = cleanseFragment(input);
    assertTrue("scope should be preserved on th", result.contains("scope=\"col\""));
    assertTrue("colspan should be preserved on th", result.contains("colspan=\"2\""));
    assertTrue("height should be preserved on th", result.contains("height=\"40\""));
    assertTrue("align should be preserved on th", result.contains("align=\"left\""));
    assertTrue("valign should be preserved on th", result.contains("valign=\"middle\""));
    assertTrue("bgcolor should be preserved on th", result.contains("bgcolor=\"#cccccc\""));
  }

  @Test
  public void testTrAttributes() {
    String input =
        "<table><tr align=\"center\" valign=\"top\" height=\"30\" bgcolor=\"#eeeeee\"><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue("align should be preserved on tr", result.contains("align=\"center\""));
    assertTrue("valign should be preserved on tr", result.contains("valign=\"top\""));
    assertTrue("height should be preserved on tr", result.contains("height=\"30\""));
    assertTrue("bgcolor should be preserved on tr", result.contains("bgcolor=\"#eeeeee\""));
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
    assertTrue("align should be preserved on thead", result.contains("<thead align=\"center\""));
    assertTrue("valign should be preserved on thead", result.contains("valign=\"top\""));
    // tbody
    assertTrue("align should be preserved on tbody", result.contains("<tbody align=\"left\""));
    // tfoot
    assertTrue("align should be preserved on tfoot", result.contains("<tfoot align=\"right\""));
  }

  @Test
  public void testCaptionAttributes() {
    String input =
        "<table><caption align=\"bottom\">Table Caption</caption><tr><td>Cell</td></tr></table>";
    String result = cleanseFragment(input);
    assertTrue("align should be preserved on caption", result.contains("align=\"bottom\""));
    assertTrue("caption content should be preserved", result.contains("Table Caption"));
  }

  @Test
  public void testFontAttributes() {
    String input = "<font color=\"red\" face=\"Arial\" size=\"3\">Styled text</font>";
    String result = cleanseFragment(input);
    assertTrue("color should be preserved on font", result.contains("color=\"red\""));
    assertTrue("face should be preserved on font", result.contains("face=\"Arial\""));
    assertTrue("size should be preserved on font", result.contains("size=\"3\""));
  }

  @Test
  public void testHrAttributes() {
    String input = "<hr width=\"50%\" size=\"2\" align=\"center\" />";
    String result = cleanseFragment(input);
    assertTrue("width should be preserved on hr", result.contains("width=\"50%\""));
    assertTrue("size should be preserved on hr", result.contains("size=\"2\""));
    assertTrue("align should be preserved on hr", result.contains("align=\"center\""));
  }

  @Test
  public void testVideoPlaysinline() {
    String input =
        "<video src=\"video.mp4\" controls=\"controls\" playsinline=\"playsinline\" width=\"640\" height=\"480\"></video>";
    String result = cleanseFragment(input);
    assertTrue("playsinline should be preserved on video", result.contains("playsinline"));
    assertTrue("controls should be preserved on video", result.contains("controls"));
    assertTrue("width should be preserved on video", result.contains("width=\"640\""));
    assertTrue("height should be preserved on video", result.contains("height=\"480\""));
  }

  @Test
  public void testSvgAttributes() {
    String input =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"100\" height=\"100\" fill=\"none\" stroke=\"black\" preserveAspectRatio=\"xMidYMid meet\"></svg>";
    String result = cleanseFragment(input);
    assertTrue(
        "xmlns should be preserved on svg",
        result.contains("xmlns=\"http://www.w3.org/2000/svg\""));
    assertTrue("viewBox should be preserved on svg", result.contains("viewBox=\"0 0 100 100\""));
    assertTrue("width should be preserved on svg", result.contains("width=\"100\""));
    assertTrue("height should be preserved on svg", result.contains("height=\"100\""));
    assertTrue("fill should be preserved on svg", result.contains("fill=\"none\""));
    assertTrue("stroke should be preserved on svg", result.contains("stroke=\"black\""));
    assertTrue(
        "preserveAspectRatio should be preserved on svg",
        result.contains("preserveAspectRatio=\"xMidYMid meet\""));
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
    assertTrue("table role should be preserved", result.contains("role=\"presentation\""));
    assertTrue("table border should be preserved", result.contains("border=\"1\""));

    // Caption
    assertTrue("caption should be preserved", result.contains("Monthly Report"));

    // Thead
    assertTrue("thead should be preserved", result.contains("<thead"));

    // Th scope
    assertTrue("th scope should be preserved", result.contains("scope=\"col\""));

    // Td attributes
    assertTrue("td align should be preserved", result.contains("align=\"left\""));
    assertTrue("td colspan should be preserved", result.contains("colspan=\"2\""));

    // Tr bgcolor
    assertTrue("tr bgcolor should be preserved", result.contains("bgcolor=\"#f0f0f0\""));
  }
}
