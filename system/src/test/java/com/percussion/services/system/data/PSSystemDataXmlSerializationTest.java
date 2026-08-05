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
package com.percussion.services.system.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip tests for system design objects under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issues #1920 / #1993 / #1994, epic #505). Offline only — no live CMS.
 */
class PSSystemDataXmlSerializationTest {

  @Test
  void auditWriteShapeAndGolden() throws Exception {
    PSAudit original = sampleAudit();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "audit"), "root: " + xml);
    assertTrue(containsTag(xml, "actor"), xml);
    assertTrue(containsTag(xml, "event-time"), xml);
    assertTrue(containsTag(xml, "state-id"), xml);
    assertTrue(containsTag(xml, "transition-name"), xml);
    assertTrue(containsTag(xml, "current-revision"), xml);
    assertTrue(containsTag(xml, "id"), xml);
    assertFalse(containsTag(xml, "guid"), "derived guid omitted: " + xml);
    assertTrue(xml.contains("editor"), xml);

    String golden = loadResource("com/percussion/services/system/data/ps-audit-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void auditRoundTripRestoresScalars() throws Exception {
    PSAudit original = sampleAudit();
    String xml = original.toXML();

    PSAudit restored = new PSAudit();
    restored.fromXML(xml);

    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getActor(), restored.getActor());
    assertEquals(original.isPublishable(), restored.isPublishable());
    assertEquals(original.isCurrentRevision(), restored.isCurrentRevision());
    assertEquals(original.isEditRevision(), restored.isEditRevision());
    assertEquals(original.getStateId(), restored.getStateId());
    assertEquals(original.getStateName(), restored.getStateName());
    assertEquals(original.getTransitionId(), restored.getTransitionId());
    assertEquals(original.getTransitionName(), restored.getTransitionName());
    assertEquals(original.getTransitionComment(), restored.getTransitionComment());
    assertEquals(original.getEventTime().getTime(), restored.getEventTime().getTime());
  }

  @Test
  void auditFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <actor>legacy-user</actor>
          <current-revision>true</current-revision>
          <edit-revision>false</edit-revision>
          <event-time>20240115T120000000</event-time>
          <id>42</id>
          <publishable>true</publishable>
          <state-id>3</state-id>
          <state-name>Approved</state-name>
          <transition-comment>ok</transition-comment>
          <transition-id>7</transition-id>
          <transition-name>Approve</transition-name>
        </null>
        """;

    PSAudit restored = new PSAudit();
    restored.fromXML(legacy);

    assertEquals(42L, restored.getId());
    assertEquals("legacy-user", restored.getActor());
    assertEquals("Approved", restored.getStateName());
    assertEquals("Approve", restored.getTransitionName());
    assertTrue(restored.isPublishable());
    assertTrue(restored.isCurrentRevision());
    assertFalse(restored.isEditRevision());
  }

  @Test
  void auditTrailWriteShapeAndGolden() throws Exception {
    PSAuditTrail original = sampleAuditTrail();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "audit-trail"), xml);
    assertTrue(containsTag(xml, "audits"), xml);
    assertTrue(containsTag(xml, "audit"), xml);
    assertTrue(containsTag(xml, "current-revision"), xml);
    assertTrue(containsTag(xml, "edit-revision"), xml);
    assertTrue(xml.contains("editor"), xml);

    String golden = loadResource("com/percussion/services/system/data/ps-audit-trail-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void auditTrailRoundTripRestoresNestedAudits() throws Exception {
    PSAuditTrail original = sampleAuditTrail();
    String xml = original.toXML();

    PSAuditTrail restored = new PSAuditTrail();
    restored.fromXML(xml);

    assertEquals(original.getCurrentRevision(), restored.getCurrentRevision());
    assertEquals(original.getEditRevision(), restored.getEditRevision());
    assertEquals(original.getAudits().size(), restored.getAudits().size());
    assertEquals(original.getAudits().get(0).getActor(), restored.getAudits().get(0).getActor());
    assertEquals(original.getAudits().get(0).getId(), restored.getAudits().get(0).getId());
    assertEquals(
        original.getAudits().get(1).getTransitionName(),
        restored.getAudits().get(1).getTransitionName());
  }

  @Test
  void auditTrailFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <audits>
            <audit>
              <actor>a1</actor>
              <current-revision>false</current-revision>
              <edit-revision>true</edit-revision>
              <event-time>20240115T120000000</event-time>
              <id>1</id>
              <publishable>false</publishable>
              <state-id>1</state-id>
              <state-name>Draft</state-name>
              <transition-comment/>
              <transition-id>0</transition-id>
              <transition-name/>
            </audit>
          </audits>
          <current-revision>2</current-revision>
          <edit-revision>3</edit-revision>
        </null>
        """;

    PSAuditTrail restored = new PSAuditTrail();
    restored.fromXML(legacy);

    assertEquals(2, restored.getCurrentRevision());
    assertEquals(3, restored.getEditRevision());
    assertEquals(1, restored.getAudits().size());
    assertEquals("a1", restored.getAudits().get(0).getActor());
    assertEquals(1L, restored.getAudits().get(0).getId());
  }

  @Test
  void sharedPropertyWriteShapeAndGolden() throws Exception {
    PSSharedProperty original = sampleSharedProperty();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "shared-property"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(containsTag(xml, "name"), xml);
    assertTrue(containsTag(xml, "value"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertTrue(xml.contains("sys_prop_sample"), xml);

    String golden =
        loadResource("com/percussion/services/system/data/ps-shared-property-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void sharedPropertyRoundTripRestoresScalars() throws Exception {
    PSSharedProperty original = sampleSharedProperty();
    String xml = original.toXML();

    PSSharedProperty restored = new PSSharedProperty();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getValue(), restored.getValue());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
  }

  @Test
  void sharedPropertyFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <guid>0-29-55</guid>
          <name>legacy_prop</name>
          <value>legacy_value</value>
        </null>
        """;

    PSSharedProperty restored = new PSSharedProperty();
    restored.fromXML(legacy);

    assertEquals("legacy_prop", restored.getName());
    assertEquals("legacy_value", restored.getValue());
    assertEquals(55L, restored.getGUID().getUUID());
  }

  @Test
  void sharedPropertySetVersionOneShotWithNullClear() {
    PSSharedProperty property = new PSSharedProperty("name", "value");
    property.setVersion(0);
    assertEquals(0, property.getVersion());
    assertThrows(IllegalStateException.class, () -> property.setVersion(1));
    property.setVersion(null);
    assertNull(property.getVersion());
    property.setVersion(2);
    assertEquals(2, property.getVersion());
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          property.setVersion(null);
          property.setVersion(-1);
        });
  }

  @Test
  void dependentWriteShapeAndGolden() throws Exception {
    PSDependent original = sampleDependent(2001L, PSTypeEnum.TEMPLATE.name());
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "dependent"), "root: " + xml);
    assertTrue(containsTag(xml, "id"), xml);
    assertTrue(containsTag(xml, "type"), xml);
    assertFalse(containsTag(xml, "display-type"), "derived display-type omitted: " + xml);
    assertTrue(xml.contains("TEMPLATE"), xml);

    String golden = loadResource("com/percussion/services/system/data/ps-dependent-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void dependentRoundTripRestoresScalars() throws Exception {
    PSDependent original = sampleDependent(2001L, PSTypeEnum.TEMPLATE.name());
    String xml = original.toXML();

    PSDependent restored = new PSDependent();
    restored.fromXML(xml);

    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getType(), restored.getType());
    assertEquals(original.getDisplayType(), restored.getDisplayType());
  }

  @Test
  void dependentFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <id>77</id>
          <type>ITEM_FILTER</type>
        </null>
        """;

    PSDependent restored = new PSDependent();
    restored.fromXML(legacy);

    assertEquals(77L, restored.getId());
    assertEquals(PSTypeEnum.ITEM_FILTER.name(), restored.getType());
  }

  @Test
  void dependencyWriteShapeAndGolden() throws Exception {
    PSDependency original = sampleDependency();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "dependency"), xml);
    assertTrue(containsTag(xml, "dependents"), xml);
    assertTrue(containsTag(xml, "dependent"), xml);
    assertTrue(containsTag(xml, "id"), xml);
    assertTrue(containsTag(xml, "type"), xml);
    assertFalse(containsTag(xml, "dependent-types"), "derived dependent-types omitted: " + xml);
    assertTrue(xml.contains("TEMPLATE"), xml);
    assertTrue(xml.contains("ITEM_FILTER"), xml);

    String golden = loadResource("com/percussion/services/system/data/ps-dependency-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void dependencyRoundTripRestoresNestedDependents() throws Exception {
    PSDependency original = sampleDependency();
    String xml = original.toXML();

    PSDependency restored = new PSDependency();
    restored.fromXML(xml);

    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getDependents().size(), restored.getDependents().size());
    assertEquals(original.getDependents().get(0).getId(), restored.getDependents().get(0).getId());
    assertEquals(
        original.getDependents().get(0).getType(), restored.getDependents().get(0).getType());
    assertEquals(original.getDependents().get(1).getId(), restored.getDependents().get(1).getId());
    assertEquals(
        original.getDependents().get(1).getType(), restored.getDependents().get(1).getType());
  }

  @Test
  void dependencyFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <dependents>
            <dependent>
              <id>9</id>
              <type>CONTENT_LIST</type>
            </dependent>
          </dependents>
          <id>5</id>
        </null>
        """;

    PSDependency restored = new PSDependency();
    restored.fromXML(legacy);

    assertEquals(5L, restored.getId());
    assertEquals(1, restored.getDependents().size());
    assertEquals(9L, restored.getDependents().get(0).getId());
    assertEquals(PSTypeEnum.CONTENT_LIST.name(), restored.getDependents().get(0).getType());
  }

  @Test
  void mimeContentAdapterWriteShapeAndGolden() throws Exception {
    PSMimeContentAdapter original = sampleMimeContentInline();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "mime-content-adapter"), "root: " + xml);
    assertTrue(containsTag(xml, "attachment-id"), xml);
    assertTrue(containsTag(xml, "character-encoding"), xml);
    assertTrue(containsTag(xml, "content"), xml);
    assertTrue(containsTag(xml, "content-length"), xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(containsTag(xml, "mime-type"), xml);
    assertTrue(containsTag(xml, "name"), xml);
    assertTrue(containsTag(xml, "transfer-encoding"), xml);
    assertFalse(containsTag(xml, "label"), "label alias suppressed: " + xml);
    assertFalse(containsTag(xml, "content-attached"), "derived flag suppressed: " + xml);
    assertFalse(containsTag(xml, "description"), "always-null description suppressed: " + xml);
    // base64 of UTF-8 "sample-config-body"
    assertTrue(xml.contains("c2FtcGxlLWNvbmZpZy1ib2R5"), xml);
    assertTrue(xml.contains("WORKFLOW_CONFIG"), xml);

    String golden =
        loadResource("com/percussion/services/system/data/ps-mime-content-adapter-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void mimeContentAdapterRoundTripRestoresInlineBase64Content() throws Exception {
    PSMimeContentAdapter original = sampleMimeContentInline();
    byte[] originalBytes = original.getContent().readAllBytes();
    String xml = original.toXML();

    PSMimeContentAdapter restored = new PSMimeContentAdapter();
    restored.fromXML(xml);

    assertEquals(original.getAttachmentId(), restored.getAttachmentId());
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getMimeType(), restored.getMimeType());
    assertEquals(original.getContentLength(), restored.getContentLength());
    assertEquals(original.getCharacterEncoding(), restored.getCharacterEncoding());
    assertEquals(original.getTransferEncoding(), restored.getTransferEncoding());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertFalse(restored.isContentAttached());
    byte[] restoredBytes = restored.getContent().readAllBytes();
    assertEquals(
        new String(originalBytes, StandardCharsets.UTF_8),
        new String(restoredBytes, StandardCharsets.UTF_8));
  }

  @Test
  void mimeContentAdapterRoundTripRestoresAttachmentHref() throws Exception {
    PSMimeContentAdapter original = sampleMimeContentAttached();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "attachment-id"), xml);
    // Null content property is omitted (or empty) when attachment-referenced.
    assertFalse(xml.contains("c2FtcGxl"), "no inline body when attached: " + xml);

    PSMimeContentAdapter restored = new PSMimeContentAdapter();
    restored.fromXML(xml);

    assertEquals(42L, restored.getAttachmentId());
    assertTrue(restored.isContentAttached());
    assertNull(restored.getContent());
    assertEquals("ATTACHED_CONFIG", restored.getName());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
  }

  @Test
  void mimeContentAdapterFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <attachment-id>-1</attachment-id>
          <character-encoding>UTF-8</character-encoding>
          <content>c2FtcGxlLWNvbmZpZy1ib2R5</content>
          <content-length>18</content-length>
          <guid>0-34-100</guid>
          <mime-type>text/xml</mime-type>
          <name>LEGACY_CONFIG</name>
          <transfer-encoding>base64</transfer-encoding>
        </null>
        """;

    PSMimeContentAdapter restored = new PSMimeContentAdapter();
    restored.fromXML(legacy);

    assertEquals(-1L, restored.getAttachmentId());
    assertEquals("LEGACY_CONFIG", restored.getName());
    assertEquals("text/xml", restored.getMimeType());
    assertEquals(18L, restored.getContentLength());
    assertEquals("UTF-8", restored.getCharacterEncoding());
    assertEquals("base64", restored.getTransferEncoding());
    assertEquals(100L, restored.getGUID().getUUID());
    assertEquals(
        "sample-config-body",
        new String(restored.getContent().readAllBytes(), StandardCharsets.UTF_8));
  }

  private static PSAudit sampleAudit() {
    PSAudit audit = new PSAudit();
    audit.setId(1001L);
    audit.setActor("editor");
    audit.setPublishable(true);
    audit.setCurrentRevision(true);
    audit.setEditRevision(false);
    audit.setStateId(5L);
    audit.setStateName("Public");
    audit.setTransitionId(12L);
    audit.setTransitionName("Publish");
    audit.setTransitionComment("ship it");
    // Fixed UTC instant for golden stability (matches @JsonFormat timezone=UTC).
    audit.setEventTime(fixedEventTime());
    return audit;
  }

  private static PSAuditTrail sampleAuditTrail() {
    PSAuditTrail trail = new PSAuditTrail();
    trail.setCurrentRevision(2);
    trail.setEditRevision(3);

    PSAudit a1 = new PSAudit();
    a1.setId(1001L);
    a1.setActor("editor");
    a1.setPublishable(true);
    a1.setCurrentRevision(false);
    a1.setEditRevision(true);
    a1.setStateId(5L);
    a1.setStateName("Public");
    a1.setTransitionId(12L);
    a1.setTransitionName("Publish");
    a1.setTransitionComment("ship it");
    a1.setEventTime(fixedEventTime());

    PSAudit a2 = new PSAudit();
    a2.setId(1002L);
    a2.setActor("reviewer");
    a2.setPublishable(false);
    a2.setCurrentRevision(true);
    a2.setEditRevision(false);
    a2.setStateId(3L);
    a2.setStateName("Review");
    a2.setTransitionId(9L);
    a2.setTransitionName("Submit");
    a2.setTransitionComment("ready");
    a2.setEventTime(fixedEventTime());

    trail.addAudit(a1);
    trail.addAudit(a2);
    return trail;
  }

  private static PSSharedProperty sampleSharedProperty() {
    PSSharedProperty prop = new PSSharedProperty();
    prop.setGUID(new PSGuid(PSTypeEnum.SHARED_PROPERTY, 55L));
    prop.setName("sys_prop_sample");
    prop.setValue("sample-value");
    return prop;
  }

  private static PSDependent sampleDependent(long id, String type) {
    PSDependent dependent = new PSDependent();
    dependent.setId(id);
    dependent.setType(type);
    return dependent;
  }

  private static PSDependency sampleDependency() {
    PSDependency dependency = new PSDependency();
    dependency.setId(100L);
    dependency.addDependent(sampleDependent(2001L, PSTypeEnum.TEMPLATE.name()));
    dependency.addDependent(sampleDependent(2002L, PSTypeEnum.ITEM_FILTER.name()));
    return dependency;
  }

  private static PSMimeContentAdapter sampleMimeContentInline() {
    PSMimeContentAdapter content = new PSMimeContentAdapter();
    content.setGUID(new PSGuid(PSTypeEnum.CONFIGURATION, 100L));
    content.setName("WORKFLOW_CONFIG");
    content.setMimeType("text/xml");
    content.setCharacterEncoding(StandardCharsets.UTF_8.name());
    content.setTransferEncoding("base64");
    byte[] body = "sample-config-body".getBytes(StandardCharsets.UTF_8);
    content.setContent(new ByteArrayInputStream(body));
    content.setContentLength(body.length);
    return content;
  }

  private static PSMimeContentAdapter sampleMimeContentAttached() {
    PSMimeContentAdapter content = new PSMimeContentAdapter();
    content.setGUID(new PSGuid(PSTypeEnum.CONFIGURATION, 200L));
    content.setName("ATTACHED_CONFIG");
    content.setMimeType("application/octet-stream");
    content.setCharacterEncoding(StandardCharsets.UTF_8.name());
    content.setTransferEncoding("base64");
    content.setAttachmentId(42L);
    content.setContentLength(-1);
    return content;
  }

  /** 2024-01-15T12:00:00.000Z as {@link Date}. */
  private static Date fixedEventTime() {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    java.util.Calendar cal = java.util.Calendar.getInstance(utc);
    cal.clear();
    cal.set(2024, java.util.Calendar.JANUARY, 15, 12, 0, 0);
    cal.set(java.util.Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSSystemDataXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

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
    java.util.List<Node> eChildren = significantChildren(expected);
    java.util.List<Node> aChildren = significantChildren(actual);
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

  private static java.util.List<Node> significantChildren(Element el) {
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

  private static String summarize(java.util.List<Node> nodes) {
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
