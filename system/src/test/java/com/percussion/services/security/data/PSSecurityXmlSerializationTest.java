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
package com.percussion.services.security.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.IPSTypedPrincipal.PrincipalTypes;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.PSTypedPrincipal;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip / package-fixture smoke for security design objects under the Jackson-backed
 * {@code PSXmlSerializationHelper} (issue #1889, epic #505). Offline only — no live CMS.
 */
class PSSecurityXmlSerializationTest {

  @Test
  void communityWriteHidesRoleAssociationsAndEmitsRoles() throws Exception {
    PSCommunity original = sampleCommunity();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "community"), xml);
    assertTrue(containsTag(xml, "roles"), xml);
    assertTrue(containsTag(xml, "long"), "role long ids: " + xml);
    assertFalse(containsTag(xml, "role-associations"), xml);
    assertFalse(containsTag(xml, "site-associations"), xml);
    assertFalse(containsTag(xml, "version"), xml);
    assertFalse(xml.matches("(?s).*<type(\\s|>).*"), "catalog type suppressed: " + xml);
    assertFalse(xml.matches("(?s).*<label(\\s|>).*"), "label alias suppressed: " + xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("Default"), xml);
  }

  @Test
  void communityWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleCommunity().toXML();
    String golden = loadResource("com/percussion/services/security/data/ps-community-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void communityRoundTripRestoresNameDescriptionRolesAndGuid() throws Exception {
    PSCommunity original = sampleCommunity();
    String xml = original.toXML();

    PSCommunity restored = new PSCommunity();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(sortedLongs(original.getRoles()), sortedLongs(restored.getRoles()));
  }

  @Test
  void communityFromXmlAcceptsLegacyNullRoot() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <description>Default community</description>
          <guid>0-10-1001</guid>
          <name>Default</name>
          <roles>
            <long>2001</long>
            <long>2002</long>
          </roles>
          <type>COMMUNITY_DEF</type>
          <label>Default</label>
        </null>
        """;

    PSCommunity restored = new PSCommunity();
    restored.fromXML(legacy);

    assertEquals("Default", restored.getName());
    assertEquals("Default community", restored.getDescription());
    assertEquals(1001L, restored.getGUID().longValue());
    assertEquals(List.of(2001L, 2002L), sortedLongs(restored.getRoles()));
  }

  @Test
  void aclWriteEmitsEntryAndPsPermissionWithoutDerivedFlags() throws Exception {
    PSAclImpl original = sampleAcl();
    String xml = original.toXML();

    assertNotNull(xml);
    assertTrue(containsTag(xml, "acl-impl"), xml);
    assertTrue(containsTag(xml, "entry"), xml);
    assertTrue(containsTag(xml, "ps-permission"), xml);
    assertTrue(containsTag(xml, "ps-permissions"), xml);
    assertTrue(xml.contains("RUNTIME_VISIBLE") || xml.contains("READ"), xml);
    assertTrue(xml.contains("Editor"), xml);
    // Derived Betwixt booleans / typed-principal omitted on modern write
    assertFalse(containsTag(xml, "typed-principal"), xml);
    assertFalse(containsTag(xml, "system-entry"), xml);
    assertFalse(containsTag(xml, "first-owner"), xml);
    assertFalse(containsTag(xml, "object-guid"), "derived object-guid omitted: " + xml);
    assertFalse(xml.matches("(?s).*<label(\\s|>).*"), xml);
  }

  @Test
  void aclWriteMatchesGoldenFixture() throws Exception {
    String xml = sampleAcl().toXML();
    String golden = loadResource("com/percussion/services/security/data/ps-acl-impl-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void aclRoundTripRestoresGraph() throws Exception {
    PSAclImpl original = sampleAcl();
    String xml = original.toXML();

    PSAclImpl restored = new PSAclImpl();
    restored.fromXML(xml);

    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getObjectId(), restored.getObjectId());
    assertEquals(original.getObjectType(), restored.getObjectType());
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertEquals(original.getEntries().size(), restored.getEntries().size());

    PSAclEntryImpl oEntry = findEntry(original, "Editor");
    PSAclEntryImpl rEntry = findEntry(restored, "Editor");
    assertNotNull(oEntry);
    assertNotNull(rEntry);
    assertEquals(oEntry.getType(), rEntry.getType());
    assertEquals(
        permissionNames(oEntry.getPsPermissions()), permissionNames(rEntry.getPsPermissions()));

    PSAclEntryImpl oOwner = findEntry(original, "Default");
    PSAclEntryImpl rOwner = findEntry(restored, "Default");
    assertNotNull(rOwner);
    assertEquals(oOwner.getType(), rOwner.getType());
    assertTrue(permissionNames(rOwner.getPsPermissions()).contains("OWNER"));
    assertTrue(permissionNames(rOwner.getPsPermissions()).contains("READ"));
  }

  @Test
  void packageFixtureAclDefSmokeRestoresScalarsAndInlinePermissions() throws Exception {
    // Offline package smoke: shipped percPage.contentType.aclDef (Betwixt idref sharing remains a
    // documented deviation — only fully-inlined permission blocks restore under Jackson).
    String packaged =
        loadResource("com/percussion/services/security/data/percPage.contentType.aclDef");
    assertTrue(packaged.contains("<acl-impl"), packaged);
    assertTrue(packaged.contains("<entry"), packaged);

    PSAclImpl restored = new PSAclImpl();
    restored.fromXML(packaged);

    assertEquals("temp", restored.getName());
    assertEquals(317L, restored.getObjectId());
    assertEquals(2, restored.getObjectType());
    assertNotNull(restored.getGUID());
    assertFalse(restored.getEntries().isEmpty());

    PSAclEntryImpl admin = findEntry(restored, "Corporate_Investments_Admin");
    assertNotNull(admin);
    assertEquals(PrincipalTypes.COMMUNITY, admin.getType());
    // First entry has fully-inlined RUNTIME_VISIBLE permission in the package fixture
    assertTrue(
        permissionNames(admin.getPsPermissions()).contains("RUNTIME_VISIBLE"),
        "inline perms on first entry: " + permissionNames(admin.getPsPermissions()));

    PSAclEntryImpl defaultUser = findEntry(restored, "Default");
    assertNotNull(defaultUser);
    assertEquals(PrincipalTypes.USER, defaultUser.getType());
    Collection<String> defaultPerms = permissionNames(defaultUser.getPsPermissions());
    assertTrue(defaultPerms.contains("OWNER"), defaultPerms.toString());
    assertTrue(defaultPerms.contains("READ"), defaultPerms.toString());
  }

  @Test
  void loginWriteRoundTripScalarsAndCommunities() throws Exception {
    PSLogin original = sampleLogin();
    String xml = original.toXML();

    assertTrue(containsTag(xml, "login"), xml);
    assertTrue(containsTag(xml, "session-id"), xml);
    assertTrue(containsTag(xml, "community"), xml);
    // Nested community may emit <roles> role-id longs; login-level PSRole/PSLocale lists are
    // @JsonIgnore (legacy objectstore types).
    assertFalse(containsTag(xml, "locales"), "PSLocale nesting suppressed: " + xml);
    assertFalse(containsTag(xml, "PSXRole") || containsTag(xml, "psx-role"), xml);

    String golden = loadResource("com/percussion/services/security/data/ps-login-golden.xml");
    assertLogicalXmlParity(golden, xml);

    PSLogin restored = new PSLogin();
    restored.fromXML(xml);
    assertEquals(original.getSessionId(), restored.getSessionId());
    assertEquals(original.getSessionTimeout(), restored.getSessionTimeout());
    assertEquals(original.getDefaultLocaleCode(), restored.getDefaultLocaleCode());
    assertEquals(1, restored.getCommunities().size());
    assertEquals("Default", restored.getCommunities().get(0).getName());
    // defaultCommunity is set after communities on write; membership check accepts Default
    assertEquals("Default", restored.getDefaultCommunity());
  }

  @Test
  void communityVisibilityRoundTripGuid() throws Exception {
    PSCommunityVisibility original = new PSCommunityVisibility();
    original.setGUID(new PSGuid(PSTypeEnum.COMMUNITY_DEF, 55L));
    String xml = original.toXML();
    assertTrue(containsTag(xml, "community-visibility"), xml);
    assertTrue(containsTag(xml, "guid"), xml);

    PSCommunityVisibility restored = new PSCommunityVisibility();
    restored.fromXML(xml);
    assertEquals(original.getGUID().toString(), restored.getGUID().toString());
    assertTrue(restored.getVisibleObjects().isEmpty());
  }

  private static PSCommunity sampleCommunity() {
    PSCommunity community = new PSCommunity();
    community.setGUID(new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L));
    community.setName("Default");
    community.setDescription("Default community");
    community.setRoles(List.of(2001L, 2002L));
    return community;
  }

  private static PSAclImpl sampleAcl() {
    PSAclImpl acl = new PSAclImpl();
    acl.setGUID(new PSGuid(PSTypeEnum.ACL, 9001L));
    acl.setName("SampleAcl");
    acl.setDescription("Sample ACL for Jackson golden");
    acl.setObjectId(317L);
    acl.setObjectType(PSTypeEnum.NODEDEF.getOrdinal());
    acl.setVersion(1);

    PSAclEntryImpl editor = new PSAclEntryImpl(new PSTypedPrincipal("Editor", PrincipalTypes.ROLE));
    editor.setId(11L);
    editor.addPermission(PSPermissions.READ);
    editor.addPermission(PSPermissions.UPDATE);
    editor.addPermission(PSPermissions.RUNTIME_VISIBLE);
    acl.addEntry(editor);

    PSAclEntryImpl owner =
        new PSAclEntryImpl(
            new PSTypedPrincipal(PSTypedPrincipal.DEFAULT_USER_ENTRY, PrincipalTypes.USER));
    owner.setId(12L);
    owner.addPermission(PSPermissions.OWNER);
    owner.addPermission(PSPermissions.READ);
    owner.addPermission(PSPermissions.UPDATE);
    owner.addPermission(PSPermissions.DELETE);
    acl.addEntry(owner);

    return acl;
  }

  private static PSLogin sampleLogin() {
    PSLogin login = new PSLogin("sess-abc-123");
    login.setSessionTimeout(1_800_000L);
    login.setDefaultLocaleCode("en-us");
    PSCommunity community = sampleCommunity();
    login.setCommunities(new ArrayList<>(List.of(community)));
    // After communities are present so membership check accepts the name
    login.setDefaultCommunity("Default");
    return login;
  }

  private static PSAclEntryImpl findEntry(PSAclImpl acl, String name) {
    for (var entry : acl.getEntries()) {
      if (entry instanceof PSAclEntryImpl impl && name.equals(impl.getName())) {
        return impl;
      }
    }
    return null;
  }

  private static Collection<String> permissionNames(Collection<PSAccessLevelImpl> perms) {
    return perms.stream()
        .map(PSAccessLevelImpl::getPermission)
        .map(Enum::name)
        .sorted()
        .collect(Collectors.toList());
  }

  private static List<Long> sortedLongs(Collection<Long> values) {
    return values.stream().sorted().collect(Collectors.toList());
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSSecurityXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
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
