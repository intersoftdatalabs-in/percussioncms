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

package com.percussion.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.PSMetadataExtractorService;
import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import com.percussion.delivery.metadata.rdfa.PSTripleHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PSMetadataExtractorServiceTests {

  @TempDir public Path temporaryFolder;

  @Test
  public void testgetNamespace() {
    PSMetadataExtractorService svc = new PSMetadataExtractorService();

    PSTripleHandler handler = new PSTripleHandler();

    handler.getNamespace("");
  }

  @Test
  public void testgetPlainProperty() {
    PSMetadataExtractorService svc = new PSMetadataExtractorService();

    PSTripleHandler handler = new PSTripleHandler();

    handler.getPlainPropertyName("");
  }

  @Test
  public void testNoAbstract() throws IOException {
    InputStream is =
        PSMetadataExtractorServiceTests.class.getResourceAsStream(
            "/com/percussion/delivery/no-abstract.html");

    try (InputStreamReader inputStreamReader = new InputStreamReader(is)) {

      PSMetadataExtractorService svc = new PSMetadataExtractorService();
      PSMetadataEntry entry =
          svc.process(inputStreamReader, "text/html", "/Sites/test/no-abstract.html", null);

      assertNotNull(entry);
      assertEquals("page", entry.getType());
      HashMap map = new HashMap();

      for (IPSMetadataProperty prop : entry.getProperties()) {
        map.put(prop.getName(), prop.getValue());
      }

      assertEquals("test", map.get("dcterms:source"));
    }
  }

  @Test
  public void testbwcorona() throws IOException {
    InputStream is =
        PSMetadataExtractorServiceTests.class.getResourceAsStream(
            "/com/percussion/delivery/bw-corona.html");

    try (InputStreamReader inputStreamReader = new InputStreamReader(is)) {

      PSMetadataExtractorService svc = new PSMetadataExtractorService();
      PSMetadataEntry entry =
          svc.process(inputStreamReader, "text/html", "/Sites/www.bw.edu/bw-corona.html", null);

      assertNotNull(entry);
      assertEquals("page", entry.getType());
      HashMap map = new HashMap();

      for (IPSMetadataProperty prop : entry.getProperties()) {
        map.put(prop.getName(), prop.getValue());
      }
      assertEquals("standard-flex", map.get("dcterms:source"));
      assertEquals("2020-04-27T09:50:00", map.get("dcterms:created"));
      assertEquals("2020-03-16T17:02:57", map.get("dcterms:modified"));
      assertEquals("Modified Spring Semester Schedule", entry.getLinktext());
      assertEquals("/Categories/Event Types/BW Seasons", map.get("perc:category"));
      assertEquals("1200", map.get("og:image:width"));
      assertEquals("630", map.get("og:image:height"));
      assertEquals("Baldwin Wallace University", map.get("og:site_name"));
      assertEquals("Coronavirus Updates | Baldwin Wallace University", map.get("og:title"));
      assertEquals(
          "Find out the latest updates concerning the Coronavirus and Baldwin Wallace University.",
          map.get("og:description"));
      assertEquals("https://www.bw.edu/advisory/coronavirus/", map.get("og:url"));
      assertEquals(
          "https://www.bw.edu/Assets/social-media/social-share-default.jpg", map.get("og:image"));
      assertEquals("en_US", map.get("og:locale"));
      assertEquals("summary_large_image", map.get("twitter:card"));
      assertEquals("Coronavirus Updates | Baldwin Wallace University", map.get("twitter:title"));
      assertEquals(
          "Find out the latest updates concerning the Coronavirus and Baldwin Wallace University.",
          map.get("twitter:description"));
      assertEquals(
          "https://www.bw.edu/Assets/social-media/social-share-default.jpg",
          map.get("twitter:image"));
      assertEquals("banner image for Coronavirus Update", map.get("twitter:image:alt"));
      assertEquals(
          "<div class=\"rxbodyfield\">\n"
              + " <p><img alt=\"banner image for Coronavirus Update\" height=\"121\""
              + " src=\"/Assets/home-page/seasons/seasons-coronavirus.jpg\" title=\"Coronavirus"
              + " Update\" width=\"402\"></p>\n"
              + " <p>BW has instituted a Modified Spring Semester Schedule in response to Novel"
              + " COVID-19 (Coronavirus). <strong>There are currently no campus-associated cases of"
              + " COVID-19.</strong></p>\n"
              + "</div>",
          map.get("dcterms:abstract"));
      assertEquals("article", map.get("og:type"));
    }
  }

  /**
   * Regression for HTML entity decoding and script stripping in the RDFa metadata extractor
   * (v8.1.7 PR #107 / DTS platform track residue).
   *
   * <p>Fixture {@code /com/percussion/delivery/entity-test.html} includes encoded entities in meta
   * content and body abstract, plus {@code <script>} / JSON-LD that must not pollute extracted
   * properties.
   */
  @Test
  public void testEntityAndScriptHandling() throws IOException {
    InputStream is =
        PSMetadataExtractorServiceTests.class.getResourceAsStream(
            "/com/percussion/delivery/entity-test.html");
    assertNotNull(is, "entity-test.html fixture must be on the test classpath");

    try (InputStreamReader inputStreamReader = new InputStreamReader(is)) {
      PSMetadataExtractorService svc = new PSMetadataExtractorService();
      PSMetadataEntry entry =
          svc.process(inputStreamReader, "text/html", "/Sites/test/entity-test.html", null);

      assertNotNull(entry);
      HashMap<String, String> map = new HashMap<>();
      for (IPSMetadataProperty prop : entry.getProperties()) {
        map.put(prop.getName(), prop.getValue());
      }

      // Title meta uses &amp; which should be decoded to a bare ampersand
      assertEquals("Comprehensive Test Title & More", map.get("dcterms:title"));

      String description = map.get("dcterms:description");
      assertNotNull(description, "dcterms:description should be extracted");

      String abstractText = map.get("dcterms:abstract");
      assertNotNull(abstractText, "dcterms:abstract should be extracted");

      // Script bodies (including JSON-LD and inline JS) must not appear in property values
      for (String value : map.values()) {
        if (value == null) {
          continue;
        }
        assertFalse(
            value.contains("console.log"),
            "script text must be stripped from metadata properties: " + value);
        assertFalse(
            value.contains("@context"),
            "JSON-LD script must be stripped from metadata properties: " + value);
        assertFalse(
            value.contains("var x = 10"),
            "inline script must be stripped from metadata properties: " + value);
      }
    }
  }
}
