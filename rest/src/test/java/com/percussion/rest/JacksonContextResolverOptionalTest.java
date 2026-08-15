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

import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityRole;
import com.percussion.rest.communities.CommunityVisibility;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
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
  void community_serializesPlainScalarsNotOptionalBeans() {
    Community community = new Community();
    community.setId(10L);
    community.setName("Default");
    community.setLabel("Default Community");
    community.setDescription("The default community");
    community.setGuid(new Guid("0-13-10"));

    ObjectMapper communityMapper = new JacksonContextResolver().getContext(Community.class);
    String json = communityMapper.writeValueAsString(community);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Default"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("Default Community"), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-13-10") || json.contains("10"), json);
    assertNoOptionalBeanKeys(json);

    Community roundTrip = communityMapper.readValue(json, Community.class);
    assertEquals("Default", roundTrip.getName(), json);
    assertEquals("Default Community", roundTrip.getLabel(), json);
    assertEquals("The default community", roundTrip.getDescription(), json);
    assertEquals(10L, roundTrip.getId(), json);
  }

  @Test
  void communityRole_serializesRoleNameNotOptionalBeans() {
    CommunityRole role = new CommunityRole();
    role.setCommunityId(10L);
    role.setRoleId(2L);
    role.setRoleName("Admin");
    role.setCommunityGuid(new Guid("0-13-10"));
    Guid roleGuid = new Guid();
    roleGuid.setStringValue("0-8-2");
    roleGuid.setType((short) 8);
    roleGuid.setUuid(2);
    role.setRoleGuid(roleGuid);

    ObjectMapper roleMapper = new JacksonContextResolver().getContext(CommunityRole.class);
    String json = roleMapper.writeValueAsString(role);
    assertTrue(json.contains("\"roleName\""), json);
    assertTrue(json.contains("Admin"), json);
    assertTrue(json.contains("\"communityId\""), json);
    assertTrue(json.contains("\"roleId\""), json);
    assertTrue(json.contains("\"communityGuid\""), json);
    assertTrue(json.contains("\"roleGuid\""), json);
    assertNoOptionalBeanKeys(json);

    CommunityRole roundTrip = roleMapper.readValue(json, CommunityRole.class);
    assertEquals("Admin", roundTrip.getRoleName(), json);
    assertEquals(10L, roundTrip.getCommunityId(), json);
    assertEquals(2L, roundTrip.getRoleId(), json);
  }

  @Test
  void communityVisibility_serializesGuidNotOptionalBeans() {
    CommunityVisibility visibility = new CommunityVisibility();
    visibility.setId(10L);
    visibility.setGuid(new Guid("0-13-10"));

    ObjectMapper visibilityMapper =
        new JacksonContextResolver().getContext(CommunityVisibility.class);
    String json = visibilityMapper.writeValueAsString(visibility);
    assertTrue(json.contains("\"id\""), json);
    assertTrue(json.contains("10"), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-13-10") || json.contains("13"), json);
    assertNoOptionalBeanKeys(json);

    CommunityVisibility roundTrip = visibilityMapper.readValue(json, CommunityVisibility.class);
    assertEquals(10L, roundTrip.getId(), json);
    assertNotNull(roundTrip.getGuid(), json);
    assertEquals("0-13-10", roundTrip.getGuid().getStringValue().orElse(null), json);
  }

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
