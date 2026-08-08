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
package com.percussion.cx.catalogers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSEntry;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral tests for typed cataloger APIs after Collection/Iterator rawtypes cleanup (#2384).
 */
public class PSCatalogerTypingTest {

  @Test
  public void communityCreateAndCatalogFromXml() throws Exception {
    PSCommunityCataloger.Community all =
        PSCommunityCataloger.createCommunity(-1, "All communities", "all");
    assertEquals(-1, all.getId());
    assertEquals("All communities", all.getName());
    assertEquals("all", all.getDesc());

    PSCommunityCataloger cataloger = new PSCommunityCataloger();
    cataloger.fromXml(
        parse(
                "<communities>"
                    + "<list>"
                    + "<communityname>Default</communityname>"
                    + "<communityid>1001</communityid>"
                    + "<communitydesc>Default community</communitydesc>"
                    + "</list>"
                    + "<list>"
                    + "<communityname>Editor</communityname>"
                    + "<communityid>1002</communityid>"
                    + "<communitydesc>Editors</communitydesc>"
                    + "</list>"
                    + "</communities>")
            .getDocumentElement());

    Collection<PSCommunityCataloger.Community> communities = cataloger.getCommunities();
    assertEquals(2, communities.size());
    Iterator<PSCommunityCataloger.Community> it = communities.iterator();
    PSCommunityCataloger.Community first = it.next();
    assertEquals(1001, first.getId());
    assertEquals("Default", first.getName());
    assertTrue(first.getDesc().contains("Default"));
  }

  @Test
  public void roleCatalogFromXml() throws Exception {
    PSRoleCataloger cataloger = new PSRoleCataloger();
    cataloger.fromXml(
        parse(
                "<getRole>"
                    + "<PSXRole><name>Admin</name></PSXRole>"
                    + "<PSXRole><name>Author</name></PSXRole>"
                    + "</getRole>")
            .getDocumentElement());

    Collection<PSRoleCataloger.Role> roles = cataloger.getRoles();
    assertEquals(2, roles.size());
    assertEquals("Admin", roles.iterator().next().getName());
  }

  @Test
  public void subjectCatalogFromXml() throws Exception {
    PSSubjectCataloger cataloger = new PSSubjectCataloger();
    cataloger.fromXml(
        parse(
                "<getSubject>"
                    + "<PSXSubject>"
                    + "<name>admin</name>"
                    + "<securityProviderType>0</securityProviderType>"
                    + "<securityProviderInstance>rxbackend</securityProviderInstance>"
                    + "</PSXSubject>"
                    + "</getSubject>")
            .getDocumentElement());

    Collection<PSSubjectCataloger.Subject> subjects = cataloger.getSubjects();
    assertEquals(1, subjects.size());
    PSSubjectCataloger.Subject subject = subjects.iterator().next();
    assertEquals("admin", subject.getName());
    assertEquals(0, subject.getSecurityProviderTypeId());
    assertEquals("rxbackend", subject.getSecurityProviderInstance());
  }

  @Test
  public void localeCatalogFromXml() throws Exception {
    PSLocaleCataloger cataloger = new PSLocaleCataloger();
    // PSEntry expects PSXDisplayText child then Value sibling
    cataloger.fromXml(
        parse(
                "<locales>"
                    + "<PSXEntry>"
                    + "<PSXDisplayText>English</PSXDisplayText>"
                    + "<Value>en-us</Value>"
                    + "</PSXEntry>"
                    + "</locales>")
            .getDocumentElement());

    Iterator<PSEntry> locales = cataloger.getLocales();
    assertTrue(locales.hasNext());
    PSEntry locale = locales.next();
    assertEquals("en-us", locale.getValue());
  }

  @Test
  public void globalTemplateCatalogFromXml() throws Exception {
    PSGlobalTemplateCataloger cataloger = new PSGlobalTemplateCataloger();
    cataloger.fromXml(
        parse(
                "<GlobalTemplates>"
                    + "<Template name=\"rffGiFin\"/>"
                    + "<Template name=\"rffGiCal\"/>"
                    + "</GlobalTemplates>")
            .getDocumentElement());

    Collection<String> templates = cataloger.getGlobalTemplates();
    assertEquals(2, templates.size());
    assertTrue(templates.contains("rffGiFin"));
    assertTrue(templates.contains("rffGiCal"));
  }

  @Test
  public void communityContentTypeMapperCompatibleCommunities() throws Exception {
    PSCommunityContentTypeMapperCataloger mapper = new PSCommunityContentTypeMapperCataloger();
    mapper.fromXml(
        parse(
                "<CommunityContentTypeMapper>"
                    + "<CommunityContentTypeMapping communityName=\"Src\" communityId=\"1\">"
                    + "<ContentType name=\"Page\" id=\"10\"/>"
                    + "</CommunityContentTypeMapping>"
                    + "<CommunityContentTypeMapping communityName=\"Superset\" communityId=\"2\">"
                    + "<ContentType name=\"Page\" id=\"10\"/>"
                    + "<ContentType name=\"Asset\" id=\"20\"/>"
                    + "</CommunityContentTypeMapping>"
                    + "<CommunityContentTypeMapping communityName=\"Other\" communityId=\"3\">"
                    + "<ContentType name=\"Asset\" id=\"20\"/>"
                    + "</CommunityContentTypeMapping>"
                    + "</CommunityContentTypeMapper>")
            .getDocumentElement());

    Collection<PSCommunityCataloger.Community> compatible =
        mapper.getCompatibleCommunities(Integer.valueOf(1));
    assertNotNull(compatible);
    // Src itself + Superset (has Page); Other lacks Page
    assertEquals(2, compatible.size());
    assertTrue(compatible.stream().anyMatch(c -> c.getId() == 1));
    assertTrue(compatible.stream().anyMatch(c -> c.getId() == 2));

    assertNull(mapper.getCompatibleCommunities(Integer.valueOf(99)));
    assertThrows(
        IllegalArgumentException.class, () -> mapper.getCompatibleCommunities(null));
  }

  private static Document parse(String xml) throws Exception {
    return PSXmlDocumentBuilder.createXmlDocument(new StringReader(xml), false);
  }
}
