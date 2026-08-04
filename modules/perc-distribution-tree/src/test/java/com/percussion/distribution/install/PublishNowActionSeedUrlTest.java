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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Regression guard for RET-06 / issues #1843 and #1884: Publish Now SERVER MENUITEM seed URLs must
 * target the stable demand-publish servlet path, not the legacy Design-tree demand JSP, and ACTION
 * 217 must use upgrade-safe tabledata replace flags so existing DBs are rewritten.
 *
 * <p>Peer seed-data guards: {@link SampleSiteLocaleStripTest}. Target path matches {@code web.xml}
 * mapping {@code /publisher/demandpublishing} ({@code PSDemandPublishServlet}) and the same query
 * params historically used by {@code ui/publishing/publish.jsp} ({@code sys_contentid}, {@code
 * sys_folderid}, {@code sys_siteid} / edition id for FF).
 *
 * <p>Paths use {@link Path#of(String, String...)} / {@link Path#resolve(String)} only (portable
 * across Windows and Unix). Assertions are URL-string shape, never OS file separators.
 */
@Tag("UnitTest")
class PublishNowActionSeedUrlTest {

  /** Relative to module CWD when Surefire runs standalone {@code clean install}. */
  private static final Path CMS_TABLE_DATA =
      Path.of("src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml");

  private static final Path RXFF_TABLE_DATA =
      Path.of("src/main/resources/distribution/rxconfig/Installer/data/RxffTableData.xml");

  /**
   * FastForward MSM/source peer kept in lockstep with the distribution Rxff seed (repo-relative
   * from this module).
   */
  private static final Path FF_RXFF_TABLE_DATA =
      Path.of("..", "..", "system", "FastForward", "Core", "Config", "Data", "RxffTableData.xml");

  /** Context-relative SERVER MENUITEM URL peer of other action seeds ({@code ../…}). */
  static final String EXPECTED_PUBLISH_NOW_URL = "../publisher/demandpublishing";

  private static final String LEGACY_PUBLISH_JSP = "ui/publishing/publish.jsp";

  /** TableFactory replace — peer of FF {@code EI_Publish_Now} and other upgrade rows. */
  private static final String UPGRADE_SAFE_ACTION = "r";

  private static final String UPGRADE_SAFE_ON_CREATE_ONLY = "no";

  @Test
  void cmsPublishNowAction_pointsAtDemandPublishServlet() throws Exception {
    assertTrue(
        Files.isRegularFile(CMS_TABLE_DATA), "missing seed: " + CMS_TABLE_DATA.toAbsolutePath());

    Document doc = parse(CMS_TABLE_DATA);
    Map<String, String> urlByName = menuActionUrlsByName(doc);

    assertTrue(
        urlByName.containsKey("Publish_Now"), "ACTION Publish_Now missing from cmsTableData");
    assertEquals(
        EXPECTED_PUBLISH_NOW_URL,
        urlByName.get("Publish_Now"),
        "Publish_Now must use PSDemandPublishServlet path (issue #1843)");
    assertFalse(
        urlByName.get("Publish_Now").contains(LEGACY_PUBLISH_JSP),
        "Publish_Now must not target legacy publish.jsp");
    assertFalse(urlByName.get("Publish_Now").contains("\\"), "URL path separators must be '/'");
  }

  /**
   * Issue #1884: ACTION 217 must not stay clean-install-only insert. Existing upgraded DBs keep
   * rows with {@code ../ui/publishing/publish.jsp} until tabledata runs a replace.
   */
  @Test
  void cmsPublishNowAction_isUpgradeSafeReplace() throws Exception {
    Document doc = parse(CMS_TABLE_DATA);
    Element row = menuActionRowByName(doc, "Publish_Now");
    assertNotNull(row, "ACTION Publish_Now row missing from cmsTableData RXMENUACTION");

    assertEquals(
        "217",
        columnValue(row, "ACTIONID"),
        "Publish_Now must remain ACTIONID 217 (menu relations / params keyed by id)");
    assertEquals(
        UPGRADE_SAFE_ACTION,
        row.getAttribute("action"),
        "Publish_Now must use action=r replace (issue #1884; peer EI_Publish_Now)");
    assertEquals(
        UPGRADE_SAFE_ON_CREATE_ONLY,
        row.getAttribute("onTableCreateOnly"),
        "Publish_Now must use onTableCreateOnly=no so upgrades rewrite existing URL");
    assertEquals(EXPECTED_PUBLISH_NOW_URL, columnValue(row, "URL"));
  }

  @Test
  void cmsPublishNowAction_retainsDemandParamNames() throws Exception {
    Document doc = parse(CMS_TABLE_DATA);
    Set<String> params = menuActionParamNames(doc, "217");

    assertTrue(params.contains("sys_contentid"), "Publish_Now must pass sys_contentid: " + params);
    assertTrue(params.contains("sys_folderid"), "Publish_Now must pass sys_folderid: " + params);
    assertTrue(params.contains("sys_siteid"), "Publish_Now must pass sys_siteid: " + params);
  }

  @Test
  void rxffEiPublishNowAction_pointsAtDemandPublishServlet() throws Exception {
    assertTrue(
        Files.isRegularFile(RXFF_TABLE_DATA), "missing seed: " + RXFF_TABLE_DATA.toAbsolutePath());

    Document doc = parse(RXFF_TABLE_DATA);
    Map<String, String> urlByName = menuActionUrlsByName(doc);

    assertTrue(
        urlByName.containsKey("EI_Publish_Now"),
        "ACTION EI_Publish_Now missing from RxffTableData");
    assertEquals(EXPECTED_PUBLISH_NOW_URL, urlByName.get("EI_Publish_Now"));
    assertFalse(urlByName.get("EI_Publish_Now").contains(LEGACY_PUBLISH_JSP));
  }

  @Test
  void rxffEiPublishNowAction_isUpgradeSafeReplace() throws Exception {
    Document doc = parse(RXFF_TABLE_DATA);
    Element row = menuActionRowByName(doc, "EI_Publish_Now");
    assertNotNull(row, "ACTION EI_Publish_Now row missing from RxffTableData RXMENUACTION");

    assertEquals("1012", columnValue(row, "ACTIONID"));
    assertEquals(
        UPGRADE_SAFE_ACTION,
        row.getAttribute("action"),
        "EI_Publish_Now peer already uses action=r; keep lockstep with Publish_Now (#1884)");
    assertEquals(UPGRADE_SAFE_ON_CREATE_ONLY, row.getAttribute("onTableCreateOnly"));
    assertEquals(EXPECTED_PUBLISH_NOW_URL, columnValue(row, "URL"));
  }

  @Test
  void rxffEiPublishNowAction_retainsEditionAndItemParams() throws Exception {
    Document doc = parse(RXFF_TABLE_DATA);
    Set<String> params = menuActionParamNames(doc, "1012");

    assertTrue(
        params.contains("sys_editionid"), "EI_Publish_Now must pass sys_editionid: " + params);
    assertTrue(
        params.contains("sys_contentid"), "EI_Publish_Now must pass sys_contentid: " + params);
    assertTrue(params.contains("sys_folderid"), "EI_Publish_Now must pass sys_folderid: " + params);
  }

  @Test
  void fastForwardRxffPeer_matchesDistributionSeedUrl() throws Exception {
    if (!Files.isRegularFile(FF_RXFF_TABLE_DATA)) {
      // Standalone checkout may omit FastForward in some agent trees; skip only if absent.
      return;
    }
    Document doc = parse(FF_RXFF_TABLE_DATA);
    Map<String, String> urlByName = menuActionUrlsByName(doc);
    assertTrue(urlByName.containsKey("EI_Publish_Now"), "FF peer missing EI_Publish_Now");
    assertEquals(
        EXPECTED_PUBLISH_NOW_URL,
        urlByName.get("EI_Publish_Now"),
        "system/FastForward RxffTableData must stay lockstep with dist Rxff seed (#1843)");
    Element row = menuActionRowByName(doc, "EI_Publish_Now");
    assertNotNull(row);
    assertEquals(UPGRADE_SAFE_ACTION, row.getAttribute("action"));
    assertEquals(UPGRADE_SAFE_ON_CREATE_ONLY, row.getAttribute("onTableCreateOnly"));
  }

  @Test
  void noInstallerSeedStillReferencesLegacyPublishJspAsActionUrl() throws Exception {
    for (Path seed : new Path[] {CMS_TABLE_DATA, RXFF_TABLE_DATA}) {
      String text = Files.readString(seed);
      // Column values only: allow HTML comments that mention the legacy path historically.
      Document doc = parse(seed);
      for (Map.Entry<String, String> e : menuActionUrlsByName(doc).entrySet()) {
        assertFalse(
            e.getValue().contains(LEGACY_PUBLISH_JSP),
            seed + " action " + e.getKey() + " still uses legacy publish.jsp URL: " + e.getValue());
      }
      // Silence unused if we only use DOM — keep text load to prove readable encoding
      assertFalse(text.isBlank());
    }
  }

  private static Document parse(Path path)
      throws IOException, ParserConfigurationException, SAXException {
    // Peer: SampleSiteLocaleStripTest — local seed files, no external entities.
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setValidating(false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    try (var in = Files.newInputStream(path)) {
      return builder.parse(in);
    }
  }

  /** Maps RXMENUACTION NAME → URL for every action row under tables named RXMENUACTION. */
  private static Map<String, String> menuActionUrlsByName(Document doc) {
    Map<String, String> out = new HashMap<>();
    for (Element row : menuActionRows(doc)) {
      String name = columnValue(row, "NAME");
      String url = columnValue(row, "URL");
      if (name != null && url != null) {
        out.put(name, url);
      }
    }
    return out;
  }

  /** First RXMENUACTION row whose NAME column matches, or {@code null}. */
  private static Element menuActionRowByName(Document doc, String actionName) {
    for (Element row : menuActionRows(doc)) {
      if (actionName.equals(columnValue(row, "NAME"))) {
        return row;
      }
    }
    return null;
  }

  private static Iterable<Element> menuActionRows(Document doc) {
    java.util.List<Element> rows = new java.util.ArrayList<>();
    NodeList tables = doc.getElementsByTagName("table");
    for (int t = 0; t < tables.getLength(); t++) {
      Element table = (Element) tables.item(t);
      if (!"RXMENUACTION".equals(table.getAttribute("name"))) {
        continue;
      }
      NodeList rowNodes = table.getElementsByTagName("row");
      for (int r = 0; r < rowNodes.getLength(); r++) {
        rows.add((Element) rowNodes.item(r));
      }
    }
    return rows;
  }

  /** PARAMNAME values for the given ACTIONID under RXMENUACTIONPARAM. */
  private static Set<String> menuActionParamNames(Document doc, String actionId) {
    Set<String> out = new HashSet<>();
    NodeList tables = doc.getElementsByTagName("table");
    for (int t = 0; t < tables.getLength(); t++) {
      Element table = (Element) tables.item(t);
      if (!"RXMENUACTIONPARAM".equals(table.getAttribute("name"))) {
        continue;
      }
      NodeList rows = table.getElementsByTagName("row");
      for (int r = 0; r < rows.getLength(); r++) {
        Element row = (Element) rows.item(r);
        if (actionId.equals(columnValue(row, "ACTIONID"))) {
          String param = columnValue(row, "PARAMNAME");
          if (param != null) {
            out.add(param);
          }
        }
      }
    }
    return out;
  }

  private static String columnValue(Element row, String columnName) {
    NodeList columns = row.getChildNodes();
    for (int i = 0; i < columns.getLength(); i++) {
      Node n = columns.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element col = (Element) n;
      if (!"column".equals(col.getTagName())) {
        continue;
      }
      if (columnName.equals(col.getAttribute("name"))) {
        return col.getTextContent() == null ? null : col.getTextContent().trim();
      }
    }
    return null;
  }
}
