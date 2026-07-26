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
package com.percussion.sitemanage.importer.theme;

import static com.percussion.test.TestAssertions.*;

import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.*;

/**
 * Unit test to cover the use cases when importing the header.
 *
 * @author Leonardo M. Hildt, Sunny Sal (refactored)
 */
@Disabled("Uses P.com")
class PSHTMLHeaderImporterTest {

  private IPSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);

  private Document sourceDoc;
  private Element docHeader;
  private Element docBody;

  private String installDir = "C:/DevEnv/Installs/dev/";
  private String absoluteThemePath = "web_resources/themes/www.percussion.com";
  private static final String siteName = "www.percussion.com";
  private static final String themeName = "www.percussion.com";
  private static final String themePath = "/web_resources/themes/" + themeName;
  private static final String siteUrl = "http://www.percussion.com";
  private PSHTMLHeaderImporter headerImporter;

  @BeforeEach
  void setup() throws IOException {
    if (System.getProperty("install.dir") != null) {
      installDir = System.getProperty("install.dir");
      if (!(installDir.endsWith("/") || installDir.endsWith("\\"))) {
        installDir = installDir.concat("/");
      }
    }
    absoluteThemePath = installDir + absoluteThemePath;
    sourceDoc =
        Jsoup.parse(
            "<!DOCTYPE html><html><head></head><body></body></html>", "http://www.percussion.com");
    docHeader = sourceDoc.head();
    docBody = sourceDoc.body();
    headerImporter =
        new PSHTMLHeaderImporter(
            sourceDoc, siteUrl, siteName, absoluteThemePath, themePath, logger);
  }

  @Test
  void testGetLinks() {
    // ...existing code...
    try {
      var originalLinks = this.docHeader.select("link");
      originalLinks.addAll(this.docBody.select("link"));

      assertTrue(
          this.docHeader
              .toString()
              .contains("http://www.functravel.com/css/0.1/screen/common/masthead.css"));

      assertEquals(
          "/Rhythmyx/web_resources/cm/css/perc_decoration.css", originalLinks.get(0).attr("href"));
      assertEquals("perc_theme.css", originalLinks.get(2).attr("href"));
      assertEquals(
          "http://www.percussion.com/css/0.1/screen/common/masthead.css",
          originalLinks.get(3).attr("href"));
      assertEquals(
          "http://www.functravel.com/css/0.1/screen/common/masthead.css",
          originalLinks.get(4).attr("href"));

      var linkPaths = headerImporter.getLinkPaths();
      assertNotNull(linkPaths);
      assertFalse(linkPaths.isEmpty());
      assertEquals(12L, linkPaths.size());

      var links = docHeader.select("link");
      links.addAll(docBody.select("link"));

      String newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/Rhythmyx/web_resources/cm/css/perc_decoration.css";
      assertEquals(newLinkExpected, links.get(0).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/perc_theme.css";
      assertEquals(newLinkExpected, links.get(2).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/css/0.1/screen/common/masthead.css";
      assertEquals(newLinkExpected, links.get(3).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/s/en_US-j7rwzw/649/favicon.ico";
      assertEquals(newLinkExpected, links.get(6).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/s/en_US-j7rwzw/649/icons/favicon.png";
      assertEquals(newLinkExpected, links.get(7).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/favicon.ico";
      assertEquals(newLinkExpected, links.get(8).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/cache.boston.com/universal/newsprojects/widgets/slider/slider.css";
      assertEquals(newLinkExpected, links.get(9).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/CSS/homepage.cfm.css";
      assertEquals(newLinkExpected, links.get(10).attr("href"));

      newLinkExpected =
          "/web_resources/themes/www.percussion.com/import/www.percussion.com/NewHome/engine3/style.css";
      assertEquals(newLinkExpected, links.get(11).attr("href"));
    } catch (Exception e) {
      fail("No exception should have been thrown.");
    }
  }

  @Test
  void testGetScripts() {
    var originalLinks = this.docHeader.select("script");
    originalLinks.addAll(this.docBody.select("script"));

    assertTrue(
        this.docHeader.toString().contains("http://www.functravel.com/js/utils/myscript.js"));
    assertEquals("/Rhythmyx/web_resources/cm/jslib/jquery.js", originalLinks.get(0).attr("src"));
    assertEquals("http://www.percussion.com/js/scriptaculous.js", originalLinks.get(4).attr("src"));
    assertEquals(
        "http://www.functravel.com/js/utils/myscript.js", originalLinks.get(5).attr("src"));
    assertEquals(
        "/templates/percussion/js/jquery.hoverIntent.minified.js",
        originalLinks.get(9).attr("src"));

    var scriptsPaths = headerImporter.getScriptPaths();
    assertNotNull(scriptsPaths);
    assertFalse(scriptsPaths.isEmpty());
    assertEquals(9L, scriptsPaths.size());

    var headerScripts = docHeader.select("script");
    String newSrcExpected =
        "/web_resources/themes/www.percussion.com/import/www.percussion.com/Rhythmyx/web_resources/cm/jslib/jquery.js";
    assertEquals(newSrcExpected, headerScripts.get(0).attr("src"));
    assertEquals(
        "/web_resources/themes/www.percussion.com/import/www.percussion.com/js/scriptaculous.js",
        headerScripts.get(4).attr("src"));

    var bodyScripts = docBody.select("script");
    newSrcExpected =
        "/web_resources/themes/www.percussion.com/import/www.percussion.com/templates/percussion/js/jquery.min.js";
    assertEquals(newSrcExpected, bodyScripts.get(0).attr("src"));
    assertEquals(
        "/web_resources/themes/www.percussion.com/import/www.percussion.com/templates/percussion/js/jquery.hoverIntent.minified.js",
        bodyScripts.get(1).attr("src"));
  }

  @Test
  void testProcessInlineImages() {
    var inlineImages = headerImporter.processInlineStyles();
    assertNotNull(inlineImages);
    assertFalse(inlineImages.isEmpty());
    assertEquals(5L, inlineImages.size());

    assertTrue(
        inlineImages.containsKey(
            "http://www.percussion.com/images/ui-bg_highlight-soft_100_eeeeee_1x100.png"));
    assertTrue(inlineImages.containsKey("http://www.percussion.com/images/bullet.png"));
    assertTrue(inlineImages.containsKey("http://www.percussion.com/bullet.png"));

    assertTrue(
        inlineImages.containsValue(
            "C:/DevEnv/Installs/dev/web_resources/themes/www.percussion.com/import/www.percussion.com/images/ui-bg_highlight-soft_100_eeeeee_1x100.png"));
    assertTrue(
        inlineImages.containsValue(
            "C:/DevEnv/Installs/dev/web_resources/themes/www.percussion.com/import/www.percussion.com/images/bullet.png"));
    assertTrue(
        inlineImages.containsValue(
            "C:/DevEnv/Installs/dev/web_resources/themes/www.percussion.com/import/www.percussion.com/bullet.png"));

    assertTrue(
        StringUtils.contains(
            docHeader.toString(),
            "/web_resources/themes/www.percussion.com/import/www.percussion.com/images/ui-bg_highlight-soft_100_eeeeee_1x100.png"));
    assertTrue(
        StringUtils.contains(
            docHeader.toString(),
            "/web_resources/themes/www.percussion.com/import/www.percussion.com/images/bullet.png"));

    assertTrue(
        StringUtils.contains(
            docBody.toString(),
            "/web_resources/themes/www.percussion.com/import/www.percussion.com/images/grad.gif"));
    assertTrue(StringUtils.contains(docBody.toString(), "images/grad3.gif"));
  }

  @Test
  void testProcessHeaderAndBodyImages() {
    var imgElements = docBody.getElementsByTag("img");
    assertNotNull(imgElements);
    assertFalse(imgElements.isEmpty());
    assertEquals(12L, imgElements.size());

    assertEquals("/homepage/2011/047.jpg", imgElements.get(2).attr("src"));
    assertEquals(
        "http://www.percussion.com/homepage/giveToCentral.gif", imgElements.get(9).attr("src"));
    assertEquals(
        "http://img.centralcollege.info/homepage/2011/049.jpg", imgElements.get(4).attr("src"));

    var bodyImages = headerImporter.processHeaderAndBodyImages();
    assertNotNull(bodyImages);
    assertFalse(bodyImages.isEmpty());
    assertEquals(13L, bodyImages.size());

    assertTrue(bodyImages.containsKey("http://www.percussion.com/homepage/2011/047.jpg"));
    assertTrue(
        bodyImages.containsKey(
            "http://www.percussion.com/homepage/studentProfiles/2011/KatieTokle.jpg"));

    assertTrue(
        bodyImages.containsValue(
            "/Assets/uploads/www.percussion.com/import/www.percussion.com/homepage/2011/047.jpg"));
    assertTrue(
        bodyImages.containsValue(
            "/Assets/uploads/www.percussion.com/import/www.percussion.com/homepage/studentProfiles/2011/KatieTokle.jpg"));

    assertTrue(
        StringUtils.contains(
            docBody.toString(),
            "/Assets/uploads/www.percussion.com/import/www.percussion.com/images/menu/goteal.gif"));
    assertTrue(
        StringUtils.contains(
            docBody.toString(),
            "/Assets/uploads/www.percussion.com/import/www.percussion.com/homepage/2011/047.jpg"));
    assertTrue(
        StringUtils.contains(
            docBody.toString(),
            "/Assets/uploads/www.percussion.com/import/www.percussion.com/homepage/studentProfiles/2011/KatieTokle.jpg"));
    assertTrue(
        StringUtils.contains(
            docBody.toString(),
            "/Assets/uploads/www.percussion.com/import/www.google.com.ar/images/srpr/logo3w.png"));
  }

  @Test
  void testGetFlashFiles() {
    var originalFlashFiles = this.docHeader.select("embed");
    originalFlashFiles.addAll(this.docBody.select("embed"));

    assertTrue(this.docBody.toString().contains("images/media/your_flash_file.swf"));
    assertEquals("images/media/your_flash_file.swf", originalFlashFiles.get(0).attr("src"));

    var flashPaths = headerImporter.processFlashFiles(themeName);
    assertNotNull(flashPaths);
    assertFalse(flashPaths.isEmpty());
    assertEquals(3L, flashPaths.size());

    var objectFlash = docBody.select("object");
    var bodyFlashFiles = docBody.select("embed");
    var movieFlashFiles = docBody.select("param[name=movie]");

    String newSrcExpected =
        "/Assets/uploads/www.percussion.com/import/www.percussion.com/images/media/your_flash_file.swf";
    assertEquals(newSrcExpected, bodyFlashFiles.get(0).attr("src"));
    assertEquals(newSrcExpected, movieFlashFiles.get(0).attr("value"));
    assertEquals(newSrcExpected, objectFlash.get(0).attr("data"));

    newSrcExpected =
        "/Assets/uploads/www.percussion.com/import/www.percussion.com/flash_slider/slider.swf";
    assertEquals(newSrcExpected, bodyFlashFiles.get(1).attr("src"));
    assertEquals(newSrcExpected, movieFlashFiles.get(1).attr("value"));
  }
}
