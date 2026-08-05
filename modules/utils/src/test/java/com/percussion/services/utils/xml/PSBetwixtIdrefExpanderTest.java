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
package com.percussion.services.utils.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Unit tests for {@link PSBetwixtIdrefExpander} (issue #1899 — expand Betwixt graph idrefs before
 * Jackson bind so package ACL permissions are not silently dropped).
 */
class PSBetwixtIdrefExpanderTest {

  @Test
  void expandIdrefsNoOpWhenNoIdrefSubstring() throws Exception {
    String xml = "<root><ps-permission><permission>READ</permission></ps-permission></root>";
    assertSame(xml, PSBetwixtIdrefExpander.expandIdrefs(xml));
  }

  @Test
  void expandIdrefsNoOpWhenBlank() throws Exception {
    assertSame(null, PSBetwixtIdrefExpander.expandIdrefs(null));
    assertEquals("", PSBetwixtIdrefExpander.expandIdrefs(""));
    assertEquals("   ", PSBetwixtIdrefExpander.expandIdrefs("   "));
  }

  @Test
  void expandsPermissionIdrefToFullElementCopy() throws Exception {
    String xml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <acl-impl>
          <entries>
            <entry>
              <name>Admin</name>
              <ps-permissions>
                <ps-permission id="3">
                  <acl-entry-id>100</acl-entry-id>
                  <id>200</id>
                  <permission>RUNTIME_VISIBLE</permission>
                </ps-permission>
              </ps-permissions>
            </entry>
            <entry>
              <name>Member</name>
              <ps-permissions>
                <ps-permission idref="3"/>
              </ps-permissions>
            </entry>
          </entries>
        </acl-impl>
        """;

    String expanded = PSBetwixtIdrefExpander.expandIdrefs(xml);
    assertFalse(expanded.contains("idref"), "idref should be expanded: " + expanded);

    Document doc = parse(expanded);
    List<Element> perms = elementsByTag(doc.getDocumentElement(), "ps-permission");
    assertEquals(2, perms.size());
    for (Element perm : perms) {
      assertFalse(perm.hasAttribute("idref"), perm.toString());
      assertEquals("RUNTIME_VISIBLE", firstChildText(perm, "permission"));
      assertEquals("200", firstChildText(perm, "id"));
    }
  }

  @Test
  void expandsFirstOwnerIdrefKeepingLocalElementName() throws Exception {
    // Package dumps reference typed-principal via first-owner idref (different element names).
    String xml =
        """
        <acl-impl>
          <entry>
            <typed-principal id="16">
              <name>Default</name>
              <principal-type>USER</principal-type>
              <user>true</user>
            </typed-principal>
          </entry>
          <first-owner idref="16"/>
        </acl-impl>
        """;

    String expanded = PSBetwixtIdrefExpander.expandIdrefs(xml);
    assertFalse(expanded.contains("idref"), expanded);

    Document doc = parse(expanded);
    List<Element> firstOwners = elementsByTag(doc.getDocumentElement(), "first-owner");
    assertEquals(1, firstOwners.size());
    Element fo = firstOwners.get(0);
    assertEquals("first-owner", fo.getTagName());
    assertEquals("Default", firstChildText(fo, "name"));
    assertEquals("USER", firstChildText(fo, "principal-type"));
  }

  @Test
  void unresolvedIdrefLeftAsEmptyStub() throws Exception {
    String xml =
        """
        <root>
          <ps-permission idref="missing"/>
        </root>
        """;
    String expanded = PSBetwixtIdrefExpander.expandIdrefs(xml);
    // No definition → original string returned when expanded count is 0
    assertTrue(expanded.contains("idref=\"missing\"") || expanded.contains("idref='missing'"));
  }

  @Test
  void baselinePackageSnippetExpandsAllPermissionIdrefs() throws Exception {
    // Shape from perc.widget.templateDef.aclDef / percPage.contentType.aclDef
    String snippet =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <acl-impl id="1">
          <entries>
            <entry id="2">
              <name>Corporate_Investments_Admin</name>
              <ps-permissions>
                <ps-permission id="3">
                  <acl-entry-id>-1</acl-entry-id>
                  <id>-2</id>
                  <permission>RUNTIME_VISIBLE</permission>
                </ps-permission>
              </ps-permissions>
            </entry>
            <entry id="5">
              <name>Enterprise_Investments</name>
              <ps-permissions>
                <ps-permission idref="3"/>
              </ps-permissions>
            </entry>
            <entry id="7">
              <name>Corporate_Investments</name>
              <ps-permissions>
                <ps-permission idref="3"/>
              </ps-permissions>
            </entry>
            <entry id="9">
              <name>Enterprise_Investments_Admin</name>
              <ps-permissions>
                <ps-permission idref="3"/>
              </ps-permissions>
            </entry>
            <entry id="11">
              <name>Default</name>
              <ps-permissions>
                <ps-permission id="12">
                  <permission>READ</permission>
                </ps-permission>
                <ps-permission id="13">
                  <permission>OWNER</permission>
                </ps-permission>
              </ps-permissions>
              <typed-principal id="16">
                <name>Default</name>
                <principal-type>USER</principal-type>
              </typed-principal>
            </entry>
          </entries>
          <first-owner idref="16"/>
        </acl-impl>
        """;

    String expanded = PSBetwixtIdrefExpander.expandIdrefs(snippet);
    assertFalse(expanded.contains("idref"), expanded);

    Document doc = parse(expanded);
    List<Element> runtimeVisible = new ArrayList<>();
    for (Element perm : elementsByTag(doc.getDocumentElement(), "ps-permission")) {
      if ("RUNTIME_VISIBLE".equals(firstChildText(perm, "permission"))) {
        runtimeVisible.add(perm);
      }
    }
    assertEquals(4, runtimeVisible.size(), "one inline + three expanded idrefs");

    List<Element> firstOwners = elementsByTag(doc.getDocumentElement(), "first-owner");
    assertEquals(1, firstOwners.size());
    assertEquals("Default", firstChildText(firstOwners.get(0), "name"));
  }

  @Test
  void negativeWithoutExpansionIdrefStubsHaveNoPermissionChild() throws Exception {
    String xml =
        """
        <root>
          <ps-permission id="3">
            <permission>RUNTIME_VISIBLE</permission>
          </ps-permission>
          <ps-permission idref="3"/>
        </root>
        """;
    Document raw = parse(xml);
    List<Element> perms = elementsByTag(raw.getDocumentElement(), "ps-permission");
    assertEquals(2, perms.size());
    assertEquals("RUNTIME_VISIBLE", firstChildText(perms.get(0), "permission"));
    assertTrue(
        firstChildText(perms.get(1), "permission") == null
            || firstChildText(perms.get(1), "permission").isEmpty(),
        "idref stub has no permission child until expansion");
  }

  private static Document parse(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }

  private static List<Element> elementsByTag(Element root, String tag) {
    List<Element> out = new ArrayList<>();
    collectByTag(root, tag, out);
    return out;
  }

  private static void collectByTag(Element el, String tag, List<Element> out) {
    if (tag.equals(el.getTagName())) {
      out.add(el);
    }
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        collectByTag((Element) n, tag, out);
      }
    }
  }

  private static String firstChildText(Element parent, String childTag) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && childTag.equals(n.getNodeName())) {
        return n.getTextContent() == null ? null : n.getTextContent().trim();
      }
    }
    return null;
  }
}
