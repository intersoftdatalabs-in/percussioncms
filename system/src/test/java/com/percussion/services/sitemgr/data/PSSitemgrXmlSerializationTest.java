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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.sitemgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip tests for sitemgr design objects under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issue #1918 / #1892, epic #505). Offline only — no live CMS.
 *
 * <p>Does not exercise {@code template-ids} restore (requires assembly service to load templates).
 */
class PSSitemgrXmlSerializationTest {

  @Test
  void siteWriteEmitsSitePropertyAndSuppressesVersionTemplatesLabel() throws Exception {
    PSSite original = sampleSite();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), "modern write must not emit legacy null root");
    assertTrue(containsTag(xml, "site"), "root site: " + xml);
    assertTrue(containsTag(xml, "site-property"), "nested site-property: " + xml);
    assertTrue(containsTag(xml, "properties"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("DemoSite"), xml);
    assertTrue(xml.contains("sys_pubBase"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertFalse(containsTag(xml, "associated-templates"), xml);
    assertFalse(xml.matches("(?s).*<label(\\s|>).*"), "label alias suppressed: " + xml);
    // circular parent site element must not appear under site-property
    assertFalse(
        xml.matches("(?s).*<site-property>[\\s\\S]*?<site[\\s>].*"),
        "nested site on property: " + xml);
  }

  @Test
  void siteWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleSite().toXML();
    String golden = loadResource("com/percussion/services/sitemgr/data/ps-site-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void siteRoundTripRestoresScalarsAndProperties() throws Exception {
    PSSite original = sampleSite();
    String xml = original.toXML();

    PSSite restored = new PSSite();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getBaseUrl(), restored.getBaseUrl());
    assertEquals(original.getRoot(), restored.getRoot());
    assertEquals(original.getFolderRoot(), restored.getFolderRoot());
    assertEquals(original.getNavTheme(), restored.getNavTheme());
    assertEquals(original.getGlobalTemplate(), restored.getGlobalTemplate());
    assertEquals(original.getPort(), restored.getPort());
    assertEquals(original.getIpAddress(), restored.getIpAddress());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.getSiteId(), restored.getSiteId());
    assertEquals(original.isSecure(), restored.isSecure());
    assertEquals(original.isPageBased(), restored.isPageBased());
    assertEquals(original.isCanonical(), restored.isCanonical());
    assertEquals(original.getSiteProtocol(), restored.getSiteProtocol());
    assertEquals(original.getDefaultDocument(), restored.getDefaultDocument());
    assertEquals(original.getCanonicalDist(), restored.getCanonicalDist());
    assertEquals(original.getUnpublishFlags(), restored.getUnpublishFlags());

    assertEquals(1, restored.getProperties().size());
    PSSiteProperty prop = restored.getProperties().iterator().next();
    assertEquals("sys_pubBase", prop.getName());
    assertEquals("http://example.com/", prop.getValue());
    assertEquals("0-113-1", prop.getContextId().toString());
    assertEquals(501L, prop.getPropertyId());
    // Parent re-linked by setProperties after wire restore
    assertNotNull(prop.getSite());
    assertEquals(restored.getSiteId(), ((PSSite) prop.getSite()).getSiteId());
  }

  @Test
  void siteFromXmlAcceptsLegacyNullRoot() throws Exception {
    // SITE type ordinal is 9 (see PSTypeEnum.SITE); legacy package roots use <null>
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <base-url>http://localhost:9992/Rhythmyx/</base-url>
          <description>Legacy null-root site</description>
          <folder-root>//Sites/Demo</folder-root>
          <guid>0-9-42</guid>
          <name>LegacyDemo</name>
          <root>/</root>
          <site-id>42</site-id>
        </null>
        """;

    PSSite restored = new PSSite();
    restored.fromXML(legacy);

    assertEquals("LegacyDemo", restored.getName());
    assertEquals("Legacy null-root site", restored.getDescription());
    assertEquals(42L, restored.getSiteId().longValue());
    assertEquals("0-9-42", restored.getGUID().toString());
    assertEquals("http://localhost:9992/Rhythmyx/", restored.getBaseUrl());
  }

  @Test
  void publishingContextWriteEmitsElementShapeAndSuppressesVersionScheme() throws Exception {
    PSPublishingContext original = sampleContext();
    String xml = original.toXML();

    assertNotNull(xml);
    assertTrue(containsTag(xml, "publishing-context"), "root: " + xml);
    assertTrue(containsTag(xml, "default-scheme-id"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("Publish"), xml);
    assertFalse(containsTag(xml, "version"), xml);
    // object element default-scheme (not default-scheme-id scalar)
    assertFalse(xml.matches("(?s).*<default-scheme[\\s>].*"), "object scheme suppressed: " + xml);
    assertFalse(xml.contains("PSXPublishingContext"), "historical attr root not used: " + xml);
  }

  @Test
  void publishingContextWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleContext().toXML();
    String golden =
        loadResource("com/percussion/services/sitemgr/data/ps-publishing-context-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void publishingContextRoundTripRestoresScalars() throws Exception {
    PSPublishingContext original = sampleContext();
    String xml = original.toXML();

    PSPublishingContext restored = new PSPublishingContext();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(
        original.getDefaultSchemeId().toString(), restored.getDefaultSchemeId().toString());
    assertNull(restored.getDefaultScheme());
  }

  @Test
  void locationSchemeWriteEmitsParametersAndSuppressesVersion() throws Exception {
    PSLocationScheme original = sampleScheme();
    String xml = original.toXML();

    assertNotNull(xml);
    assertTrue(containsTag(xml, "location-scheme"), "root: " + xml);
    assertTrue(containsTag(xml, "location-scheme-parameter"), xml);
    assertTrue(containsTag(xml, "parameter-set"), xml);
    assertTrue(xml.contains("Generic"), xml);
    assertTrue(xml.contains("sys_pubLocations"), xml);
    assertFalse(containsTag(xml, "version"), xml);
    assertFalse(xml.matches("(?s).*<scheme(\\s|>).*"), "circular scheme: " + xml);
  }

  @Test
  void locationSchemeWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleScheme().toXML();
    String golden =
        loadResource("com/percussion/services/sitemgr/data/ps-location-scheme-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void locationSchemeRoundTripRestoresScalarsAndParameters() throws Exception {
    PSLocationScheme original = sampleScheme();
    String xml = original.toXML();

    PSLocationScheme restored = new PSLocationScheme();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getGenerator(), restored.getGenerator());
    assertEquals(original.getTemplateId(), restored.getTemplateId());
    assertEquals(original.getContentTypeId(), restored.getContentTypeId());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.getContextId().toString(), restored.getContextId().toString());
    assertEquals(original.getParameterNames().size(), restored.getParameterNames().size());
    assertEquals("java.lang.String", restored.getParameterType("ext"));
    assertEquals(".html", restored.getParameterValue("ext"));
    assertEquals(Integer.valueOf(10), restored.getParameterSequence("ext"));
  }

  @Test
  void sitePropertyStandaloneRoundTrip() throws Exception {
    PSSiteProperty original = sampleProperty();
    String xml = original.toXML();
    assertTrue(containsTag(xml, "site-property"), xml);
    assertFalse(containsTag(xml, "version"), xml);
    assertFalse(xml.matches("(?s).*<site(\\s|>).*"), xml);

    PSSiteProperty restored = new PSSiteProperty();
    restored.fromXML(xml);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getValue(), restored.getValue());
    assertEquals(original.getPropertyId(), restored.getPropertyId());
    assertEquals(original.getContextId().toString(), restored.getContextId().toString());
  }

  private static PSSite sampleSite() {
    PSSite site = new PSSite();
    site.setSiteId(100L);
    site.setName("DemoSite");
    site.setDescription("Sample site for Jackson golden/round-trip");
    site.setBaseUrl("http://localhost:9992/Rhythmyx/");
    site.setRoot("/");
    site.setIpAddress("127.0.0.1");
    site.setPort(9992);
    site.setFolderRoot("//Sites/DemoSite");
    site.setNavTheme("Default");
    site.setGlobalTemplate("rc.gtt");
    site.setSecure(false);
    site.setPageBased(true);
    site.setCanonical(true);
    site.setCanonicalReplace(true);
    site.setSiteProtocol("https");
    site.setDefaultDocument("index.html");
    site.setCanonicalDist("sections");
    site.setUnpublishFlags("u");
    site.setGenerateSitemap(false);
    site.setMobilePreviewEnabled(false);
    site.setOverrideSystemJQuery(false);
    site.setOverrideSystemFoundation(false);
    site.setOverrideSystemJQueryUI(false);

    PSSiteProperty prop = sampleProperty();
    prop.setSite(site);
    site.addProperty(prop);
    return site;
  }

  private static PSSiteProperty sampleProperty() {
    PSSiteProperty prop = new PSSiteProperty();
    prop.setPropertyId(501L);
    prop.setName("sys_pubBase");
    prop.setValue("http://example.com/");
    prop.setContextId(new PSGuid(PSTypeEnum.CONTEXT, 1L));
    return prop;
  }

  private static PSPublishingContext sampleContext() {
    PSPublishingContext context = new PSPublishingContext();
    context.setGUID(new PSGuid(PSTypeEnum.CONTEXT, 1L));
    context.setName("Publish");
    context.setDescription("This is a test description");
    context.setDefaultSchemeId(new PSGuid(PSTypeEnum.LOCATION_SCHEME, 314L));
    return context;
  }

  private static PSLocationScheme sampleScheme() {
    PSLocationScheme scheme = new PSLocationScheme();
    scheme.setId(314L);
    scheme.setName("Generic");
    scheme.setDescription("Generic location scheme");
    scheme.setGenerator("Java/global/percussion/system/sys_pubLocations");
    scheme.setTemplateId(501L);
    scheme.setContentTypeId(301L);
    scheme.setContextId(new PSGuid(PSTypeEnum.CONTEXT, 1L));
    scheme.addParameter("ext", 10, "java.lang.String", ".html");
    scheme.addParameter("path", 20, "java.lang.String", "$sys.site.path");
    return scheme;
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSSitemgrXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Compare logical XML trees: ignore XML declaration, Betwixt graph-identity {@code id}
   * attributes, insignificant whitespace, and HTML comments.
   */
  private static void assertLogicalXmlParity(String expectedXml, String actualXml)
      throws Exception {
    Document expected = parseXml(stripXmlDeclaration(expectedXml));
    Document actual = parseXml(stripXmlDeclaration(actualXml));
    assertElementTreeEquals(expected.getDocumentElement(), actual.getDocumentElement(), "/");
  }

  private static String stripXmlDeclaration(String xml) {
    String s = Objects.requireNonNull(xml).trim();
    if (s.startsWith("<?xml")) {
      int end = s.indexOf("?>");
      if (end >= 0) {
        s = s.substring(end + 2).trim();
      }
    }
    while (s.startsWith("<!--")) {
      int end = s.indexOf("-->");
      if (end < 0) {
        break;
      }
      s = s.substring(end + 3).trim();
    }
    return s;
  }

  private static Document parseXml(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new java.io.StringReader(xml)));
  }

  private static void assertElementTreeEquals(Element expected, Element actual, String path) {
    assertEquals(expected.getTagName(), actual.getTagName(), "tag at " + path);
    List<Node> eChildren = significantChildren(expected);
    List<Node> aChildren = significantChildren(actual);
    assertEquals(
        eChildren.size(),
        aChildren.size(),
        "child count at "
            + path
            + " expected="
            + summarize(eChildren)
            + " actual="
            + summarize(aChildren));
    for (int i = 0; i < eChildren.size(); i++) {
      Node en = eChildren.get(i);
      Node an = aChildren.get(i);
      if (en.getNodeType() == Node.TEXT_NODE) {
        assertEquals(en.getTextContent().trim(), an.getTextContent().trim(), "text at " + path);
      } else {
        assertElementTreeEquals(
            (Element) en, (Element) an, path + "/" + ((Element) en).getTagName() + "[" + i + "]");
      }
    }
  }

  private static List<Node> significantChildren(Element el) {
    NodeList nl = el.getChildNodes();
    java.util.ArrayList<Node> out = new java.util.ArrayList<>();
    boolean hasElementChild = false;
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
        hasElementChild = true;
        break;
      }
    }
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        out.add(n);
      } else if (n.getNodeType() == Node.TEXT_NODE && !hasElementChild) {
        String t = n.getTextContent();
        if (t != null && !t.trim().isEmpty()) {
          out.add(n);
        }
      }
    }
    return out;
  }

  private static String summarize(List<Node> nodes) {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < nodes.size(); i++) {
      if (i > 0) {
        b.append(',');
      }
      Node n = nodes.get(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        b.append(((Element) n).getTagName());
      } else {
        b.append("#text");
      }
    }
    return b.append(']').toString();
  }
}
