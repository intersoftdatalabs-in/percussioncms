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

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.LinkRef;
import com.percussion.rest.MoveFolderItem;
import com.percussion.rest.folders.CopyFolderItemRequest;
import com.percussion.rest.folders.Folder;
import com.percussion.rest.folders.SectionInfo;
import com.percussion.rest.folders.SectionLinkRef;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). Both now use plain
 * getters under {@code @JsonInclude(NON_NULL)}.
 */
@Tag("UnitTest")
class JacksonContextResolverOptionalTest {

  private final ObjectMapper mapper = new JacksonContextResolver().getContext(ContentType.class);

  @Test
  void contentType_serializesNameLabelGuidNotOnlyHideFromMenu() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setHideFromMenu(false);
    ct.setGuid(new Guid("0-2-311"));

    String json = mapper.writeValueAsString(ct);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-2-311") || json.contains("311"), json);
  }

  @Test
  void contentTypeList_serializesNamesNotHideFromMenuOnly() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setGuid(new Guid("0-2-311"));
    ct.setHideFromMenu(false);

    ContentTypeList list = new ContentTypeList(List.of(ct));
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"label\""), json);
    // List root wrap uses ContentType (XmlRootElement on ContentTypeList) or plain array
    assertTrue(json.contains("ContentType") || json.startsWith("["), json);
  }

  @Test
  void templateSummary_serializesNameNotOnlyId() {
    TemplateSummary t = new TemplateSummary();
    t.setTemplateId(1018);
    t.setTemplateName("perc.page");
    t.setTemplateLabel("Page");
    t.setTemplateDescription("Page template");

    String json = mapper.writeValueAsString(t);
    assertTrue(json.contains("perc.page"), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("1018"), json);
    // Property names (not only values) — live H2 list was templateId-only (issue #2189)
    assertTrue(json.contains("\"templateName\""), json);
    assertTrue(json.contains("\"templateLabel\""), json);
    assertTrue(json.contains("\"templateDescription\""), json);
    assertTrue(json.contains("\"templateId\""), json);
    assertFalse(
        json.replaceAll("\\s", "").matches(".*\\{\"templateId\":1018\\}.*")
            && !json.contains("\"templateName\""),
        "Summary JSON must not be templateId-only: " + json);
  }

  @Test
  void templateSummaryList_serializesNamesNotIdOnly() {
    TemplateSummary t = new TemplateSummary();
    t.setTemplateId(1037);
    t.setTemplateName("perc.page");
    t.setTemplateLabel("Page");

    TemplateSummaryList list = new TemplateSummaryList(List.of(t));
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("perc.page"), json);
    assertTrue(json.contains("\"templateName\""), json);
    assertTrue(json.contains("\"templateLabel\""), json);
    assertTrue(json.contains("1037"), json);
    assertTrue(json.contains("TemplateSummary") || json.startsWith("["), json);
  }

  @Test
  void folder_serializesNamePathSiteNotOptionalBeans() {
    Folder folder = new Folder();
    folder.setId("guid-1");
    folder.setName("News");
    folder.setSiteName("Corporate");
    folder.setPath("sections");
    folder.setWorkflow("default");
    folder.setAccessLevel(Folder.ACCESS_LEVEL_WRITE);
    folder.setCommunityName("Default");
    folder.setDefaultDisplayFormatName("Related Content");
    folder.setLocale("en-us");
    folder.setEditUsers(List.of("Admin"));

    SectionInfo info = new SectionInfo();
    info.setType("section");
    info.setDisplayTitle("News Section");
    info.setTargetWindow("_self");
    info.setNavClass("nav-news");
    info.setTemplateName("perc.page");
    info.setLandingPage(new LinkRef("index.html", "http://example.com/index.html"));
    folder.setSectionInfo(info);

    folder.setPages(List.of(new LinkRef("index.html", "http://example.com/index.html")));
    folder.setSubfolders(List.of(new LinkRef("archive", "http://example.com/archive")));
    folder.setSubsections(
        List.of(new SectionLinkRef("Press", "http://example.com/press", SectionLinkRef.TYPE_INTERNAL)));

    ObjectMapper folderMapper = new JacksonContextResolver().getContext(Folder.class);
    String json = folderMapper.writeValueAsString(folder);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("News"), json);
    assertTrue(json.contains("\"siteName\""), json);
    assertTrue(json.contains("Corporate"), json);
    assertTrue(json.contains("\"path\""), json);
    assertTrue(json.contains("sections"), json);
    assertTrue(json.contains("\"sectionInfo\""), json);
    assertTrue(json.contains("News Section"), json);
    assertTrue(json.contains("\"subsections\""), json);
    assertNoOptionalBeanKeys(json);

    Folder roundTrip = folderMapper.readValue(json, Folder.class);
    assertEquals("News", roundTrip.getName(), json);
    assertEquals("Corporate", roundTrip.getSiteName(), json);
    assertEquals("sections", roundTrip.getPath(), json);
    assertNotNull(roundTrip.getSectionInfo(), json);
    assertEquals("News Section", roundTrip.getSectionInfo().getDisplayTitle(), json);
    assertEquals("perc.page", roundTrip.getSectionInfo().getTemplateName(), json);
  }

  @Test
  void sectionInfo_serializesScalarsNotOptionalBeans() {
    SectionInfo info = new SectionInfo();
    info.setType("externallink");
    info.setDisplayTitle("Partner");
    info.setTargetWindow("_blank");
    info.setNavClass("nav-ext");
    info.setTemplateName("perc.page");
    info.setExternalLinkUrl("https://partner.example");
    info.setLandingPage(new LinkRef("home", "https://partner.example/"));

    ObjectMapper infoMapper = new JacksonContextResolver().getContext(SectionInfo.class);
    String json = infoMapper.writeValueAsString(info);
    assertTrue(json.contains("\"displayTitle\""), json);
    assertTrue(json.contains("Partner"), json);
    assertTrue(json.contains("\"externalLinkUrl\""), json);
    assertTrue(json.contains("https://partner.example"), json);
    assertTrue(json.contains("\"type\""), json);
    assertNoOptionalBeanKeys(json);

    SectionInfo roundTrip = infoMapper.readValue(json, SectionInfo.class);
    assertEquals("externallink", roundTrip.getType(), json);
    assertEquals("Partner", roundTrip.getDisplayTitle(), json);
    assertEquals("https://partner.example", roundTrip.getExternalLinkUrl(), json);
    assertNotNull(roundTrip.getLandingPage(), json);
  }

  @Test
  void sectionLinkRef_serializesTypeNotOptionalBeans() {
    SectionLinkRef ref =
        new SectionLinkRef("Press", "http://example.com/press", SectionLinkRef.TYPE_EXTERNAL);

    ObjectMapper refMapper = new JacksonContextResolver().getContext(SectionLinkRef.class);
    String json = refMapper.writeValueAsString(ref);
    assertTrue(json.contains("\"type\""), json);
    assertTrue(json.contains(SectionLinkRef.TYPE_EXTERNAL), json);
    assertTrue(json.contains("Press"), json);
    assertNoOptionalBeanKeys(json);

    SectionLinkRef roundTrip = refMapper.readValue(json, SectionLinkRef.class);
    assertEquals(SectionLinkRef.TYPE_EXTERNAL, roundTrip.getType(), json);
    assertEquals("Press", roundTrip.getName().orElse(null), json);
  }

  @Test
  void copyFolderItemRequest_serializesPathsNotOptionalBeans() {
    CopyFolderItemRequest req =
        new CopyFolderItemRequest("/Sites/A/dest", "/Sites/A/src/page.html");

    ObjectMapper reqMapper = new JacksonContextResolver().getContext(CopyFolderItemRequest.class);
    String json = reqMapper.writeValueAsString(req);
    assertTrue(json.contains("\"targetFolderPath\""), json);
    assertTrue(json.contains("/Sites/A/dest"), json);
    assertTrue(json.contains("\"itemPath\""), json);
    assertTrue(json.contains("/Sites/A/src/page.html"), json);
    assertNoOptionalBeanKeys(json);

    CopyFolderItemRequest roundTrip = reqMapper.readValue(json, CopyFolderItemRequest.class);
    assertEquals("/Sites/A/dest", roundTrip.getTargetFolderPath(), json);
    assertEquals("/Sites/A/src/page.html", roundTrip.getItemPath(), json);
  }

  @Test
  void moveFolderItem_serializesPathsNotOptionalBeans() {
    MoveFolderItem req = new MoveFolderItem("/Sites/A/src/page.html", "/Sites/A/dest");

    ObjectMapper reqMapper = new JacksonContextResolver().getContext(MoveFolderItem.class);
    String json = reqMapper.writeValueAsString(req);
    assertTrue(json.contains("\"targetFolderPath\""), json);
    assertTrue(json.contains("/Sites/A/dest"), json);
    assertTrue(json.contains("\"itemPath\""), json);
    assertTrue(json.contains("/Sites/A/src/page.html"), json);
    assertNoOptionalBeanKeys(json);

    MoveFolderItem roundTrip = reqMapper.readValue(json, MoveFolderItem.class);
    assertEquals("/Sites/A/dest", roundTrip.getTargetFolderPath(), json);
    assertEquals("/Sites/A/src/page.html", roundTrip.getItemPath(), json);
  }

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
