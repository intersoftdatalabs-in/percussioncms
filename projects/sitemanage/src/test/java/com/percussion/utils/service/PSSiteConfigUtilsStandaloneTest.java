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

package com.percussion.utils.service;

import static com.percussion.test.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.validation.PathValidation;
import com.percussion.utils.service.impl.PSSiteConfigUtils;
import com.percussion.utils.service.impl.PSSiteConfigUtils.SecureXmlData;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** Standalone tests for PSSiteConfigUtils. */
public class PSSiteConfigUtilsStandaloneTest {

  @Test
  public void testGenerateCacheControlFilters() throws Exception {
    var xmlData = new SecureXmlData();
    xmlData.addSecureOrMemberSection("/section1/", "");
    xmlData.addSecureOrMemberSection("/section2/section2-2/", "editor,admin");
    xmlData.addSecureOrMemberSection("/section3/section3-1/", "");
    var sourceDoc = getWebXmlDoc("source-web.xml");
    var expectedDoc1 = getWebXmlDoc("expected1-web.xml");
    var expectedDoc2 = getWebXmlDoc("expected2-web.xml");
    var expectedDoc3 = getWebXmlDoc("expected3-web.xml");

    PSSiteConfigUtils.generateCacheControlFilters(xmlData, sourceDoc);
    assertXmlEquals(expectedDoc1, sourceDoc);

    // test update
    xmlData = new SecureXmlData();
    xmlData.addSecureOrMemberSection("/section4/section4-1/", "");
    PSSiteConfigUtils.generateCacheControlFilters(xmlData, sourceDoc);

    assertXmlEquals(expectedDoc2, sourceDoc);

    // test no section
    xmlData = new SecureXmlData();
    PSSiteConfigUtils.generateCacheControlFilters(xmlData, sourceDoc);

    assertXmlEquals(expectedDoc3, sourceDoc);
  }

  /**
   * Regression test for the {@code java/path-injection} alerts CodeQL raised at
   * PSSiteConfigUtils.java:260, 306, 340, 341 (alerts #1059, #1060, #1061, #1062). The pre-fix
   * code passed the user-supplied {@code sitename} straight into {@link java.io.File} and {@link
   * org.apache.commons.io.FileUtils} paths without validating that the name was a safe filename
   * (no path separators, no {@code ..}). The post-fix code calls {@link
   * com.percussion.security.validation.PathValidation#isValidFilename(String)} on the site name
   * inside {@code getSiteConfigFolder} and {@code getSecureFilesPath}, throwing a {@link
   * PathValidation.SecurityException} on traversal attempts.
   */
  @Test
  public void testGetSecureFilesPathRejectsPathTraversal() {
    // Each of these would, without validation, escape SitesConfig/ and resolve to a sibling
    // or parent directory (or an absolute location) once joined as a child File.
    String[] adversarialSiteNames = {
      "../etc", // parent traversal
      "..", // pure parent reference
      "site/../etc", // subdirectory then parent
      "site\\..\\etc", // Windows-style separators
      "/etc/passwd", // absolute path
      "C:/Windows", // Windows absolute drive path
      "site" + String.valueOf((char) 0) + "name", // null byte injection
    };

    for (String adversarial : adversarialSiteNames) {
      PathValidation.SecurityException ex =
          assertThrows(
              PathValidation.SecurityException.class,
              () -> PSSiteConfigUtils.getSecureFilesPath(adversarial),
              "Expected SecurityException for adversarial site name: '" + adversarial + "'");
      assertTrue(
          ex.getMessage().contains("CWE-22"),
          "SecurityException message should cite CWE-22 for input '" + adversarial + "'");
    }
  }

  /**
   * Documents that the post-fix code preserves the documented happy path: a normal site name (a
   * plain identifier) still resolves to the expected SitesConfig/${name} path.
   */
  @Test
  public void testGetSecureFilesPathAcceptsNormalSiteNames() {
    String[] validSiteNames = {"MySite", "site-1", "site_2", "123", "a"};

    for (String name : validSiteNames) {
      String path =
          assertDoesNotThrow(
              () -> PSSiteConfigUtils.getSecureFilesPath(name),
              "Expected getSecureFilesPath to accept normal site name: " + name);
      assertTrue(
          path.endsWith("/" + name) || path.endsWith(name),
          "Resolved path should end with the site name; got: " + path);
    }
  }

  private void assertXmlEquals(Document expectedDoc, Document resultDoc) throws Exception {
    var expected = PSXmlDocumentBuilder.toString(expectedDoc);
    var result = PSXmlDocumentBuilder.toString(resultDoc);

    expected = PSXmlDocumentBuilder.createXmlDocument(new StringReader(expected), false).toString();
    result = PSXmlDocumentBuilder.createXmlDocument(new StringReader(result), false).toString();

    assertEquals(expected, result);
  }

  private Document getWebXmlDoc(String name) throws Exception {
    return PSXmlDocumentBuilder.createXmlDocument(this.getClass().getResourceAsStream(name), false);
  }
}
