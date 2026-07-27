/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.distribution.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Regression tests for the install/upgrade ANT script's {@code <delete>} block in {@code
 * modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml}.
 *
 * <p>For feature 002-jdbc-drivers-cleanup. See FR-003, FR-004, FR-008.a and SC-003, SC-005, SC-006.
 */
class InstallXmlDeleteSetTest {

  private static final String INSTALL_XML_CLASSPATH =
      "/distribution/rxconfig/Installer/install.xml";

  private static final String INSTALL_JDBC_TARGET = "install_jdbc_drivers";

  @Test
  @DisplayName("FR-003: delete set contains all bundled driver filenames (current + prior release)")
  void deleteSetContainsAllBundledFilenames() throws Exception {
    Document doc = loadInstallXml();
    Element deleteEl = findInstallJdbcDriversDelete(doc);
    assertNotNull(deleteEl, "Expected <delete> inside <target name=\"install_jdbc_drivers\">");

    // The delete set is the union of the CURRENT bundled filenames and
    // the immediately-prior release's bundled filenames. The prior set
    // exists so an upgrade from N-1 to N purges the prior-version JARs
    // from jetty/base/lib/jdbc/ (preventing duplicate-driver-version
    // classpath issues). See BundledJdbcDrivers.PRIOR_FILENAMES.
    Set<String> expected = new LinkedHashSet<>();
    expected.addAll(BundledJdbcDrivers.EXACT_FILENAMES);
    expected.addAll(BundledJdbcDrivers.PRIOR_FILENAMES);

    Set<String> actual = extractIncludeNames(deleteEl);
    assertEquals(
        expected,
        actual,
        "Delete <include> set must equal the union of CURRENT and PRIOR bundled driver filenames");
  }

  @Test
  @DisplayName("FR-003: delete set contains no glob/wildcard patterns")
  void deleteSetContainsNoGlobPatterns() throws Exception {
    Document doc = loadInstallXml();
    Element deleteEl = findInstallJdbcDriversDelete(doc);
    assertNotNull(deleteEl);

    List<String> globs = new ArrayList<>();
    for (String name : extractIncludeNames(deleteEl)) {
      if (name.indexOf('*') >= 0 || name.indexOf('?') >= 0) {
        globs.add(name);
      }
    }
    assertTrue(
        globs.isEmpty(),
        "Delete <include> entries must not contain glob/wildcard characters; found: " + globs);
  }

  @Test
  @DisplayName("FR-003: delete set preserves integrator-supplied filenames")
  void deleteSetPreservesIntegratorFilenames() throws Exception {
    Document doc = loadInstallXml();
    Element deleteEl = findInstallJdbcDriversDelete(doc);
    assertNotNull(deleteEl);

    Set<String> actual = extractIncludeNames(deleteEl);
    for (String integratorSample :
        new String[] {
          "mysql-connector-java-9.0.0.jar",
          "ojdbc17-99.99.99.99.jar",
          "derbyshared-9.0.0.jar",
          "derbytools-9.0.0.jar",
          // Hypothetical newer-version drops that share a curated
          // artifactId prefix but differ from any current or prior
          // bundled filename. None of these should be in the delete
          // set because they are not exact matches.
          "mariadb-java-client-9.0.0.jar",
          "derby-9.9.9.9.jar",
          "mssql-jdbc-99.0.0.jar"
        }) {
      assertFalse(
          actual.contains(integratorSample),
          "Integrator-supplied filename must not be in the install script's delete set: "
              + integratorSample);
    }
  }

  @Test
  @DisplayName("FR-003: delete set omits legacy/non-shipped globs that purges integrator drivers")
  void deleteSetOmitsLegacyAndNonShippedGlobs() throws Exception {
    Document doc = loadInstallXml();
    Element deleteEl = findInstallJdbcDriversDelete(doc);
    assertNotNull(deleteEl);

    Set<String> actual = extractIncludeNames(deleteEl);
    for (String legacy :
        new String[] {
          "derbyshared-*.jar",
          "derbytools-*.jar",
          "mysql-connector-java-*.jar",
          "mysql-connector.jar"
        }) {
      assertFalse(
          actual.contains(legacy),
          "Legacy/non-shipped glob must not be in the install script's delete set: " + legacy);
    }
  }

  // --- helpers ----------------------------------------------------------

  private Document loadInstallXml() throws ParserConfigurationException, SAXException, IOException {
    try (InputStream in = getClass().getResourceAsStream(INSTALL_XML_CLASSPATH)) {
      assertNotNull(in, INSTALL_XML_CLASSPATH + " must be on the classpath");
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(new InputSource(in));
    }
  }

  private Element findInstallJdbcDriversDelete(Document doc) throws Exception {
    XPath xp = XPathFactory.newInstance().newXPath();
    Node target =
        (Node)
            xp.evaluate("//target[@name='" + INSTALL_JDBC_TARGET + "']", doc, XPathConstants.NODE);
    if (target == null) {
      return null;
    }
    NodeList children = target.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node c = children.item(i);
      if (c.getNodeType() == Node.ELEMENT_NODE && "delete".equals(((Element) c).getTagName())) {
        return (Element) c;
      }
    }
    return null;
  }

  private static Set<String> extractIncludeNames(Element deleteEl) {
    Set<String> out = new LinkedHashSet<>();
    NodeList children = deleteEl.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node c = children.item(i);
      if (c.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element e = (Element) c;
      if ("fileset".equals(e.getTagName())) {
        NodeList includes = e.getChildNodes();
        for (int j = 0; j < includes.getLength(); j++) {
          Node inc = includes.item(j);
          if (inc.getNodeType() == Node.ELEMENT_NODE
              && "include".equals(((Element) inc).getTagName())) {
            String name = ((Element) inc).getAttribute("name");
            if (name != null && !name.isEmpty()) {
              out.add(name);
            }
          }
        }
      }
    }
    return out;
  }
}
