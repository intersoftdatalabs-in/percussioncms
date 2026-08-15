/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Regression for #3133 / #2989: {@code installSampleSites} must apply sample site rows
 * (including {@code RXSITES}) rather than only RXS_CT_* content-type extension schemas.
 *
 * <p>{@code PSTableAction} iterates the schema collection from {@code tableDef} and looks up
 * matching data by table name. {@code RxffTableDef.xml} only lists RXS_CT_* tables while {@code
 * RxffTableData.xml} holds the sample graph (RXSITES, folders, ACLs, …) with no CT rows. Using
 * RxffTableDef alone therefore "succeeds" without ever inserting RXSITES. Legacy FastForward
 * install used {@code cmsTableDef + RxffTableDef}; installSampleSites must do the same.
 */
@Tag("UnitTest")
class InstallSampleSitesWiringTest {

  private static final Path INSTALLER_DATA =
      Path.of("src/main/resources/distribution/rxconfig/Installer/data");

  private static final Path INSTALL_REPOSITORY_XML =
      Path.of("src/main/resources/distribution/rxconfig/Installer/installRepository.xml");

  private static final Path SAMPLE_DATA = INSTALLER_DATA.resolve("RxffTableData.xml");
  private static final Path SAMPLE_DEF = INSTALLER_DATA.resolve("RxffTableDef.xml");
  private static final Path CMS_TABLE_DEF = INSTALLER_DATA.resolve("cmsTableDef.xml");
  private static final Path SAMPLE_CONTENT = CmsTableDataSampleContentNextNumberTest.SAMPLE_CONTENT;

  /** Matches {@code name} anywhere on a {@code <table ...>} start tag (not only first attr). */
  private static final Pattern TABLE_NAME_ATTR =
      Pattern.compile(
          "<table\\s+[^>]*?name\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

  @Test
  void installSampleSitesTableDefIncludesCmsAndRxffDefs()
      throws IOException, ParserConfigurationException, SAXException {
    assertTrue(Files.isRegularFile(INSTALL_REPOSITORY_XML), "missing " + INSTALL_REPOSITORY_XML);
    Document installDoc = parseXml(INSTALL_REPOSITORY_XML);
    Element sampleSitesTarget = findNamedTarget(installDoc, "installSampleSites");
    if (sampleSitesTarget == null) {
      fail("installSampleSites target must exist in installRepository.xml");
    }
    // Serialize just this target's attributes/children via getTextContent + attribute checks
    // on the Element tree (avoids brittle first-</target> string scan).
    String targetBody = elementTextSnapshot(sampleSitesTarget);

    assertTrue(
        targetBody.contains("RxffTableData.staging.xml"),
        "installSampleSites must load locale-stripped staging data");
    assertTrue(
        targetBody.contains("RxffSampleTableData"),
        "installSampleSites must also load FastForward sample content (RxffSampleTableData);"
            + " types-only seed leaves sites with no navons/pages");
    assertTrue(
        targetBody.contains("cmsTableDef.xml"),
        "installSampleSites tableDef must include cmsTableDef so RXSITES and other sample-graph"
            + " tables are processed (#3133). RxffTableDef alone only covers RXS_CT_*.");
    assertTrue(
        targetBody.contains("RxffTableDef.xml"),
        "installSampleSites tableDef must still include RxffTableDef for CT extension schemas");

    // Order matches legacy FastForward: cms first, then Rxff CT defs.
    Pattern dualDef =
        Pattern.compile(
            "tableDef\\s*=\\s*[\"'][^\"']*cmsTableDef\\.xml\\s*,\\s*[^\"']*RxffTableDef\\.xml",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    assertTrue(
        dualDef.matcher(targetBody).find(),
        "installSampleSites must set tableDef to cmsTableDef.xml,RxffTableDef.xml (comma-separated);"
            + " found target fragment:\n"
            + targetBody);

    Path distFiles = Path.of("src/main/resources/installDistributionFiles.xml");
    String distText = Files.readString(distFiles, StandardCharsets.UTF_8);
    assertTrue(
        distText.contains("RxffSampleTableData.xml")
            && distText.contains("FastForward/SampleContent"),
        "installDistributionFiles must copy FastForward SampleContent into Installer/data");
    assertTrue(
        distText.contains("SampleContent/importFiles")
            && distText.contains("FastForward/importFiles"),
        "installDistributionFiles must copy SampleContent/importFiles (hashed binaries)");
    assertTrue(
        targetBody.contains("autoImportBinaries.txt"),
        "installSampleSites must register importFiles for first-start hashed binary import");
    assertTrue(
        targetBody.contains("psx_cerffGeneric.xml")
            && targetBody.contains("psx_cerffPressRelease.xml")
            && targetBody.contains("ObjectStore"),
        "installSampleSites must copy FastForward rff ObjectStore editors; sample items use"
            + " CONTENTTYPEID 301-316 and PSItemDefManager only registers running apps");
    assertFalse(
        targetBody.contains("psx_cerffNavImage.xml"),
        "do not copy leftover psx_cerffNav* editors; perc.nav owns types 313-315");
    Path importFiles =
        Path.of("..", "..", "system", "FastForward", "SampleContent", "importFiles");
    assertTrue(
        Files.isDirectory(importFiles),
        "system/FastForward/SampleContent/importFiles must exist (7.3.2 hashed binaries)");
    try (var walk = Files.walk(importFiles)) {
      long sha1 =
          walk.filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sha1"))
              .count();
      assertTrue(sha1 >= 80, "expected hashed FastForward binaries (*.sha1); found " + sha1);
    }
  }

  @Test
  void sampleDataDeclaresRxsitesReplaceRows()
      throws IOException, ParserConfigurationException, SAXException {
    assertTrue(Files.isRegularFile(SAMPLE_DATA), "missing " + SAMPLE_DATA);
    Document doc = parseXml(SAMPLE_DATA);
    Element rxsites = findTable(doc, "RXSITES");
    if (rxsites == null) {
      fail("RxffTableData.xml must declare a RXSITES table for demo-sites seed");
    }

    String onCreateOnly = rxsites.getAttribute("onCreateOnly");
    assertFalse(
        "yes".equalsIgnoreCase(onCreateOnly) || "y".equalsIgnoreCase(onCreateOnly),
        "RXSITES sample rows must use onCreateOnly=no so they apply when the table already exists"
            + " from cmsTableDef (actual onCreateOnly="
            + onCreateOnly
            + ")");

    NodeList rows = rxsites.getElementsByTagName("row");
    assertTrue(rows.getLength() >= 1, "RXSITES must have at least one sample row");

    Set<String> siteNames = new LinkedHashSet<>();
    for (int i = 0; i < rows.getLength(); i++) {
      Element row = (Element) rows.item(i);
      String action = row.getAttribute("action");
      assertTrue(
          action == null
              || action.isEmpty()
              || "r".equalsIgnoreCase(action)
              || "i".equalsIgnoreCase(action)
              || "u".equalsIgnoreCase(action),
          "RXSITES row action should be replace/insert/update compatible, was: " + action);
      String name = columnValue(row, "SITENAME");
      if (name != null && !name.isBlank()) {
        siteNames.add(name.trim());
      }
    }

    assertTrue(
        siteNames.stream().anyMatch(n -> n.toLowerCase(Locale.ROOT).contains("enterprise")),
        "expected Enterprise Investments sample site name in RXSITES, found " + siteNames);
    assertTrue(
        siteNames.stream().anyMatch(n -> n.toLowerCase(Locale.ROOT).contains("corporate")),
        "expected Corporate Investments sample site name in RXSITES, found " + siteNames);

    // Sample / Rhythmyx sites are NOT page-based; CM1 sites are. Listing must not require T (#2989).
    for (int i = 0; i < rows.getLength(); i++) {
      Element row = (Element) rows.item(i);
      String pageBased = columnValue(row, "IS_PAGE_BASED");
      assertTrue(
          pageBased != null
              && ("F".equalsIgnoreCase(pageBased.trim())
                  || "false".equalsIgnoreCase(pageBased.trim())
                  || "0".equals(pageBased.trim())
                  || "N".equalsIgnoreCase(pageBased.trim())),
          "RXSITES sample row must set IS_PAGE_BASED false (not CM1 page-based); was: "
              + pageBased
              + " site="
              + columnValue(row, "SITENAME"));
      String folderRoot = columnValue(row, "FOLDER_ROOT");
      assertTrue(
          folderRoot != null && folderRoot.startsWith("//Sites/"),
          "RXSITES FOLDER_ROOT must be under //Sites/; was: " + folderRoot);
    }
  }

  /**
   * Invented CONTENTID 350–355 empty Pages/Files folders collide with FastForward
   * sample pages (350 is "EI Retirement - Category"). Site folders live in
   * {@code RxffSampleTableData} as 301 / 523.
   */
  @Test
  void rxffTableDataDoesNotInventCollidingSiteStubFolders()
      throws IOException, ParserConfigurationException, SAXException {
    assertTrue(Files.isRegularFile(SAMPLE_DATA), "missing " + SAMPLE_DATA);
    Document doc = parseXml(SAMPLE_DATA);
    Element contentStatus = findTable(doc, "CONTENTSTATUS");
    if (contentStatus == null) {
      fail("RxffTableData.xml must declare CONTENTSTATUS");
    }
    Set<String> collidingIds = Set.of("350", "351", "352", "353", "354", "355");
    Set<String> found = new LinkedHashSet<>();
    NodeList rows = contentStatus.getElementsByTagName("row");
    for (int i = 0; i < rows.getLength(); i++) {
      String id = columnValue((Element) rows.item(i), "CONTENTID");
      if (id != null && collidingIds.contains(id.trim())) {
        found.add(id.trim() + "=" + columnValue((Element) rows.item(i), "TITLE"));
      }
    }
    assertTrue(
        found.isEmpty(),
        "RxffTableData must not invent CONTENTID 350-355 folders (they collide with FF sample"
            + " pages). Found: "
            + found);
  }

  @Test
  void sampleContentDeclaresRealSiteFoldersNavAndPages()
      throws IOException, ParserConfigurationException, SAXException {
    assertTrue(Files.isRegularFile(SAMPLE_CONTENT), "missing FastForward sample content " + SAMPLE_CONTENT);
    Document doc = parseXml(SAMPLE_CONTENT);
    Element contentStatus = findTable(doc, "CONTENTSTATUS");
    if (contentStatus == null) {
      fail("RxffSampleTableData.xml must declare CONTENTSTATUS");
    }
    String title301 = null;
    String title523 = null;
    String object350 = null;
    NodeList rows = contentStatus.getElementsByTagName("row");
    for (int i = 0; i < rows.getLength(); i++) {
      Element row = (Element) rows.item(i);
      String id = columnValue(row, "CONTENTID");
      if ("301".equals(id)) {
        title301 = columnValue(row, "TITLE");
      } else if ("523".equals(id)) {
        title523 = columnValue(row, "TITLE");
      } else if ("350".equals(id)) {
        object350 = columnValue(row, "OBJECTTYPE");
      }
    }
    assertTrue(
        title301 != null && title301.contains("EnterpriseInvestments"),
        "sample content 301 must be EnterpriseInvestments folder, was: " + title301);
    assertTrue(
        title523 != null && title523.contains("CorporateInvestments"),
        "sample content 523 must be CorporateInvestments folder, was: " + title523);
    assertTrue(
        "1".equals(object350),
        "sample content 350 must be an item (page), not a folder; OBJECTTYPE=" + object350);

    Element navTree = findTable(doc, "RXS_CT_NAVTREE");
    if (navTree == null) {
      fail("RxffSampleTableData.xml must declare RXS_CT_NAVTREE");
    }
    Set<String> navIds = new LinkedHashSet<>();
    NodeList navRows = navTree.getElementsByTagName("row");
    for (int i = 0; i < navRows.getLength(); i++) {
      String id = columnValue((Element) navRows.item(i), "CONTENTID");
      if (id != null && !id.isBlank()) {
        navIds.add(id.trim());
      }
    }
    assertTrue(navIds.contains("319"), "EI NavTree contentid 319 missing; found " + navIds);
    assertTrue(navIds.contains("553"), "CI NavTree contentid 553 missing; found " + navIds);

    Element rels = findTable(doc, "PSX_OBJECTRELATIONSHIP");
    if (rels == null) {
      fail("RxffSampleTableData.xml must declare PSX_OBJECTRELATIONSHIP");
    }
    Set<String> siteChildren = new LinkedHashSet<>();
    NodeList relRows = rels.getElementsByTagName("row");
    for (int i = 0; i < relRows.getLength(); i++) {
      Element row = (Element) relRows.item(i);
      if ("2".equals(columnValue(row, "OWNER_ID"))) {
        String dep = columnValue(row, "DEPENDENT_ID");
        if (dep != null && !dep.isBlank()) {
          siteChildren.add(dep.trim());
        }
      }
    }
    assertTrue(siteChildren.contains("301"), "Sites must own EI folder 301; found " + siteChildren);
    assertTrue(siteChildren.contains("523"), "Sites must own CI folder 523; found " + siteChildren);
  }

  @Test
  void installSampleSitesDoesNotForcePageBasedTrue()
      throws IOException, ParserConfigurationException, SAXException {
    assertTrue(Files.isRegularFile(INSTALL_REPOSITORY_XML), "missing " + INSTALL_REPOSITORY_XML);
    Document installDoc = parseXml(INSTALL_REPOSITORY_XML);
    Element sampleSitesTarget = findNamedTarget(installDoc, "installSampleSites");
    if (sampleSitesTarget == null) {
      fail("installSampleSites target must exist in installRepository.xml");
    }
    String targetBody = elementTextSnapshot(sampleSitesTarget);
    // Must not force sample / Rhythmyx sites to page-based=true (#2989 product correction).
    assertFalse(
        targetBody.matches("(?s).*IS_PAGE_BASED\\s*=\\s*'T'.*")
            || targetBody.matches("(?s).*IS_PAGE_BASED\\s*=\\s*\"T\".*"),
        "installSampleSites must not UPDATE sample RXSITES IS_PAGE_BASED to T"
            + " (samples are Rhythmyx, not CM1 page-based)");
  }

  @Test
  void everySampleDataTableHasSchemaInCombinedDefs() throws IOException {
    assertTrue(Files.isRegularFile(SAMPLE_DATA), "missing " + SAMPLE_DATA);
    assertTrue(Files.isRegularFile(SAMPLE_DEF), "missing " + SAMPLE_DEF);
    assertTrue(Files.isRegularFile(CMS_TABLE_DEF), "missing " + CMS_TABLE_DEF);

    Set<String> dataTables = tableNamesFromXmlText(Files.readString(SAMPLE_DATA, StandardCharsets.UTF_8));
    Set<String> defTables = tableNamesFromXmlText(Files.readString(SAMPLE_DEF, StandardCharsets.UTF_8));
    Set<String> cmsTables = tableNamesFromXmlText(Files.readString(CMS_TABLE_DEF, StandardCharsets.UTF_8));

    assertTrue(dataTables.contains("RXSITES"), "sample data must include RXSITES");
    assertFalse(
        defTables.contains("RXSITES"),
        "RxffTableDef must not be the sole place RXSITES could live — it historically only has"
            + " RXS_CT_*; cmsTableDef is required for site rows");
    assertTrue(cmsTables.contains("RXSITES"), "cmsTableDef must define RXSITES schema");

    Set<String> missing = new LinkedHashSet<>();
    for (String table : dataTables) {
      if (!defTables.contains(table) && !cmsTables.contains(table)) {
        missing.add(table);
      }
    }
    assertTrue(
        missing.isEmpty(),
        "Every RxffTableData table must appear in cmsTableDef or RxffTableDef so PSTableAction can"
            + " process it. Missing schema for: "
            + missing);

    Set<String> contentTables =
        tableNamesFromXmlText(Files.readString(SAMPLE_CONTENT, StandardCharsets.UTF_8));
    Set<String> contentMissing = new LinkedHashSet<>();
    for (String table : contentTables) {
      if (!defTables.contains(table) && !cmsTables.contains(table)) {
        contentMissing.add(table);
      }
    }
    assertTrue(
        contentMissing.isEmpty(),
        "Every RxffSampleTableData table must appear in cmsTableDef or RxffTableDef. Missing: "
            + contentMissing);
  }

  private static Document parseXml(Path path)
      throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setValidating(false);
    // Avoid XXE; seed files are trusted repo assets but keep parser tight.
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    try (InputStream in = Files.newInputStream(path)) {
      return builder.parse(in);
    }
  }

  private static Element findTable(Document doc, String tableName) {
    NodeList tables = doc.getElementsByTagName("table");
    for (int i = 0; i < tables.getLength(); i++) {
      Element table = (Element) tables.item(i);
      if (tableName.equalsIgnoreCase(table.getAttribute("name"))) {
        return table;
      }
    }
    return null;
  }

  /** Ant project targets are flat; select by {@code name} attribute. */
  private static Element findNamedTarget(Document doc, String targetName) {
    NodeList targets = doc.getElementsByTagName("target");
    for (int i = 0; i < targets.getLength(); i++) {
      Element target = (Element) targets.item(i);
      if (targetName.equals(target.getAttribute("name"))) {
        return target;
      }
    }
    return null;
  }

  /**
   * Flatten element attributes and descendant attributes/text into one string for contains /
   * regex checks. Preferable to string-slicing on the first {@code </target>} in the file.
   */
  private static String elementTextSnapshot(Element root) {
    StringBuilder sb = new StringBuilder();
    appendElementSnapshot(root, sb);
    return sb.toString();
  }

  private static void appendElementSnapshot(Element el, StringBuilder sb) {
    var attrs = el.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      sb.append(' ').append(attrs.item(i).getNodeName()).append('=');
      sb.append('"').append(attrs.item(i).getNodeValue()).append('"');
    }
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      var child = children.item(i);
      if (child instanceof Element childEl) {
        appendElementSnapshot(childEl, sb);
      } else if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE
          || child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE
          || child.getNodeType() == org.w3c.dom.Node.COMMENT_NODE) {
        sb.append(child.getNodeValue());
      }
    }
  }

  private static String columnValue(Element row, String columnName) {
    NodeList cols = row.getElementsByTagName("column");
    for (int i = 0; i < cols.getLength(); i++) {
      Element col = (Element) cols.item(i);
      if (columnName.equalsIgnoreCase(col.getAttribute("name"))) {
        return col.getTextContent();
      }
    }
    return null;
  }

  private static Set<String> tableNamesFromXmlText(String xml) {
    Set<String> names = new LinkedHashSet<>();
    Matcher m = TABLE_NAME_ATTR.matcher(xml);
    while (m.find()) {
      names.add(m.group(1));
    }
    return names;
  }
}
