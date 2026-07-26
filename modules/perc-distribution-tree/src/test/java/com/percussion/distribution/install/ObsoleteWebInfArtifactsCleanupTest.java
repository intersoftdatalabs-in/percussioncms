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
package com.percussion.distribution.install;

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
 * Guards the upgrade-time cleanup of retired JSF/javax WEB-INF artifacts and Jetty-conflicting
 * webapp trees in {@code install.xml}.
 *
 * <p>Without this target, upgrades leave MyFaces/Trinidad/Tomahawk JARs (and their embedded
 * META-INF TLDs/listeners) on the Rhythmyx classpath, which breaks Jetty EE11 with {@code
 * javax.servlet.ServletContextListener} failures during Jasper TLD scan.
 */
class ObsoleteWebInfArtifactsCleanupTest {

  private static final String INSTALL_XML = "/distribution/rxconfig/Installer/install.xml";

  private static final String TARGET = "deleteObsoleteRhythmyxWebInfArtifacts";

  private static final String UPGRADE_CHAIN = "upgrade.chain";

  /** Product-shipped JARs that must be purged on upgrade (family globs). */
  private static final List<String> REQUIRED_LIB_INCLUDES =
      List.of(
          "myfaces-api-*.jar",
          "myfaces-impl-*.jar",
          "trinidad-api-*.jar",
          "trinidad-impl-*.jar",
          "tomahawk-*.jar",
          "spring-test-*.jar",
          "servlet-3.*.jar");

  /** Jakarta TwelveMonkeys servlet must be preserved. */
  private static final String JAKARTA_SERVLET_EXCLUDE = "servlet-*-jakarta.jar";

  /** Loose TLD patterns (defensive; primary TLDs live inside the JARs). */
  private static final List<String> REQUIRED_TLD_INCLUDES =
      List.of(
          "**/*trinidad*.tld",
          "**/*tomahawk*.tld",
          "**/*myfaces*.tld",
          "**/*jsf*.tld",
          "**/html_basic.tld",
          "**/html_basic_el.tld",
          "**/jsf_core.tld");

  @Test
  @DisplayName("upgrade.chain invokes deleteObsoleteRhythmyxWebInfArtifacts after upgrade_server")
  void upgradeChainCallsObsoleteCleanup() throws Exception {
    Document doc = loadInstallXml();
    Element chain = findTarget(doc, UPGRADE_CHAIN);
    assertNotNull(chain, "upgrade.chain target must exist");

    List<String> antcallTargets = new ArrayList<>();
    collectAntcallTargets(chain, antcallTargets);
    assertTrue(
        antcallTargets.contains(TARGET),
        "upgrade.chain must antcall " + TARGET + "; found: " + antcallTargets);

    int upgradeServerIdx = antcallTargets.indexOf("upgrade_server");
    int cleanupIdx = antcallTargets.indexOf(TARGET);
    assertTrue(upgradeServerIdx >= 0, "upgrade.chain must call upgrade_server");
    assertTrue(
        cleanupIdx > upgradeServerIdx,
        TARGET + " must run after upgrade_server so post-overwrite leftovers are removed");
  }

  @Test
  @DisplayName("cleanup deletes retired JSF/javax lib families and excludes jakarta servlet")
  void libDeleteSetCoversRetiredArtifacts() throws Exception {
    Document doc = loadInstallXml();
    Element target = findTarget(doc, TARGET);
    assertNotNull(target, TARGET + " target must exist");

    Set<String> includes = new LinkedHashSet<>();
    Set<String> excludes = new LinkedHashSet<>();
    collectFilesetIncludesExcludes(target, "WEB-INF/lib", includes, excludes);

    for (String required : REQUIRED_LIB_INCLUDES) {
      assertTrue(
          includes.contains(required),
          "WEB-INF/lib delete must include " + required + "; actual includes=" + includes);
    }
    assertTrue(
        excludes.contains(JAKARTA_SERVLET_EXCLUDE),
        "Must exclude " + JAKARTA_SERVLET_EXCLUDE + " so TwelveMonkeys jakarta stays");
  }

  @Test
  @DisplayName("cleanup removes faces/trinidad config, user/faces, and loose JSF TLDs")
  void configAndTldCleanupPresent() throws Exception {
    Document doc = loadInstallXml();
    Element target = findTarget(doc, TARGET);
    assertNotNull(target);

    String xml = serializeTargetSnippet(target);

    assertTrue(xml.contains("faces-config.xml"), "must delete faces-config.xml");
    assertTrue(xml.contains("admin-faces-config.xml"), "must delete admin-faces-config.xml");
    assertTrue(
        xml.contains("publishing-faces-config.xml"), "must delete publishing-faces-config.xml");
    assertTrue(xml.contains("trinidad-config.xml"), "must delete trinidad-config.xml");
    assertTrue(xml.contains("trinidad-skins.xml"), "must delete trinidad-skins.xml");
    assertTrue(xml.contains("WEB-INF/trinidad"), "must delete WEB-INF/trinidad dir");
    assertTrue(
        xml.contains("config/user/faces"),
        "must delete config/user/faces (preserved by upgrade.excludes otherwise)");
    assertTrue(xml.contains("CI_Home.war"), "must delete CI_Home.war deployable conflict");
    assertTrue(xml.contains("EI_Home.war"), "must delete EI_Home.war deployable conflict");

    // Product TLDs must not be listed for deletion
    assertFalse(xml.contains("rxcomp.tld"), "must not delete product rxcomp.tld");
    assertFalse(xml.contains("csrfguard.tld"), "must not delete product csrfguard.tld");
    assertFalse(xml.contains("tmxtags.tld"), "must not delete product tmxtags.tld");

    Set<String> tldIncludes = new LinkedHashSet<>();
    Set<String> ignored = new LinkedHashSet<>();
    collectFilesetIncludesExcludes(target, "WEB-INF", tldIncludes, ignored);
    for (String required : REQUIRED_TLD_INCLUDES) {
      assertTrue(
          tldIncludes.contains(required),
          "Loose TLD delete must include " + required + "; actual=" + tldIncludes);
    }
  }

  @Test
  @DisplayName("upgrade.delete includes CI_Home.war and EI_Home.war trees")
  void upgradeDeleteIncludesConflictingWarDirs() throws Exception {
    Document doc = loadInstallXml();
    Element patternset = findPatternset(doc, "upgrade.delete");
    assertNotNull(patternset, "upgrade.delete patternset must exist");

    Set<String> includes = extractIncludeNames(patternset);
    assertTrue(
        includes.contains("jetty/base/webapps/CI_Home.war/**"),
        "upgrade.delete must purge CI_Home.war/**; actual=" + includes);
    assertTrue(
        includes.contains("jetty/base/webapps/EI_Home.war/**"),
        "upgrade.delete must purge EI_Home.war/**; actual=" + includes);
  }

  // --- helpers ----------------------------------------------------------

  private Document loadInstallXml() throws ParserConfigurationException, SAXException, IOException {
    try (InputStream in = getClass().getResourceAsStream(INSTALL_XML)) {
      assertNotNull(in, INSTALL_XML + " must be on the classpath");
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(false);
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setXIncludeAware(false);
      dbf.setExpandEntityReferences(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(new InputSource(in));
    }
  }

  private static Element findTarget(Document doc, String name) throws Exception {
    XPath xp = XPathFactory.newInstance().newXPath();
    Node n = (Node) xp.evaluate("//target[@name='" + name + "']", doc, XPathConstants.NODE);
    return n instanceof Element ? (Element) n : null;
  }

  private static Element findPatternset(Document doc, String id) throws Exception {
    XPath xp = XPathFactory.newInstance().newXPath();
    Node n = (Node) xp.evaluate("//patternset[@id='" + id + "']", doc, XPathConstants.NODE);
    return n instanceof Element ? (Element) n : null;
  }

  private static void collectAntcallTargets(Element parent, List<String> out) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node c = children.item(i);
      if (c.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element e = (Element) c;
      if ("antcall".equals(e.getTagName())) {
        String t = e.getAttribute("target");
        if (t != null && !t.isEmpty()) {
          out.add(t);
        }
      }
      collectAntcallTargets(e, out);
    }
  }

  /**
   * Collect include/exclude names from filesets under {@code target} whose {@code dir} attribute
   * contains {@code dirSubstring}.
   */
  private static void collectFilesetIncludesExcludes(
      Element target, String dirSubstring, Set<String> includes, Set<String> excludes) {
    NodeList filesets = target.getElementsByTagName("fileset");
    for (int i = 0; i < filesets.getLength(); i++) {
      Element fs = (Element) filesets.item(i);
      String dir = fs.getAttribute("dir");
      if (dir == null || !dir.contains(dirSubstring)) {
        continue;
      }
      NodeList kids = fs.getChildNodes();
      for (int j = 0; j < kids.getLength(); j++) {
        Node k = kids.item(j);
        if (k.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        Element e = (Element) k;
        String name = e.getAttribute("name");
        if (name == null || name.isEmpty()) {
          continue;
        }
        if ("include".equals(e.getTagName())) {
          includes.add(name);
        } else if ("exclude".equals(e.getTagName())) {
          excludes.add(name);
        }
      }
    }
  }

  private static Set<String> extractIncludeNames(Element parent) {
    Set<String> out = new LinkedHashSet<>();
    NodeList includes = parent.getElementsByTagName("include");
    for (int i = 0; i < includes.getLength(); i++) {
      Element e = (Element) includes.item(i);
      String name = e.getAttribute("name");
      if (name != null && !name.isEmpty()) {
        out.add(name);
      }
    }
    return out;
  }

  /** Lightweight string view of the target for path/file assertions. */
  private static String serializeTargetSnippet(Element target) {
    StringBuilder sb = new StringBuilder();
    appendElementText(target, sb);
    return sb.toString();
  }

  private static void appendElementText(Element el, StringBuilder sb) {
    String dir = el.getAttribute("dir");
    if (dir != null && !dir.isEmpty()) {
      sb.append(dir).append('\n');
    }
    String file = el.getAttribute("file");
    if (file != null && !file.isEmpty()) {
      sb.append(file).append('\n');
    }
    String name = el.getAttribute("name");
    if (name != null && !name.isEmpty()) {
      sb.append(name).append('\n');
    }
    NodeList kids = el.getChildNodes();
    for (int i = 0; i < kids.getLength(); i++) {
      Node n = kids.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        appendElementText((Element) n, sb);
      }
    }
  }
}
