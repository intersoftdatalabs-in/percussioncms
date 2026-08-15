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
import com.percussion.rest.roles.Role;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import com.percussion.rest.users.User;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). User / Role /
 * ObjectSummary follow the same plain-getter rule (issue #3388). All use production {@link
 * JacksonContextResolver} under {@code @JsonInclude(NON_NULL)}.
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
  void user_serializesPlainScalarsNotOptionalBeans() {
    User user = new User();
    user.setUserName("admin");
    user.setFirstName("Ada");
    user.setLastName("Lovelace");
    user.setEmailAddress("ada@example.com");
    user.setUserType("INTERNAL");

    ObjectMapper userMapper = new JacksonContextResolver().getContext(User.class);
    String json = userMapper.writeValueAsString(user);
    assertTrue(json.contains("\"userName\""), json);
    assertTrue(json.contains("admin"), json);
    assertTrue(json.contains("\"firstName\""), json);
    assertTrue(json.contains("Ada"), json);
    assertTrue(json.contains("\"lastName\""), json);
    assertTrue(json.contains("\"emailAddress\""), json);
    assertTrue(json.contains("\"userType\""), json);
    assertNoOptionalBeanKeys(json);

    User roundTrip = userMapper.readValue(json, User.class);
    assertEquals("admin", roundTrip.getUserName(), json);
    assertEquals("Ada", roundTrip.getFirstName(), json);
    assertEquals("INTERNAL", roundTrip.getUserType(), json);
  }

  @Test
  void role_serializesPlainScalarsNotOptionalBeans() {
    Role role = new Role();
    role.setName("Editor");
    role.setDescription("Edit content");
    role.setHomePage("Dashboard");

    ObjectMapper roleMapper = new JacksonContextResolver().getContext(Role.class);
    String json = roleMapper.writeValueAsString(role);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Editor"), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"homePage\""), json);
    assertTrue(json.contains("Dashboard"), json);
    assertNoOptionalBeanKeys(json);

    Role roundTrip = roleMapper.readValue(json, Role.class);
    assertEquals("Editor", roundTrip.getName(), json);
    assertEquals("Dashboard", roundTrip.getHomePage(), json);
  }

  @Test
  void objectSummary_serializesNameLabelGuidNotOptionalBeans() {
    ObjectSummary summary = new ObjectSummary();
    summary.setName("Default");
    summary.setLabel("Default ACL");
    summary.setDescription("Site ACL");
    summary.setGuid(new Guid("0-13-10"));

    ObjectMapper summaryMapper = new JacksonContextResolver().getContext(ObjectSummary.class);
    String json = summaryMapper.writeValueAsString(summary);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Default"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-13-10") || json.contains("10"), json);
    assertNoOptionalBeanKeys(json);

    ObjectSummary roundTrip = summaryMapper.readValue(json, ObjectSummary.class);
    assertEquals("Default", roundTrip.getName(), json);
    assertEquals("Default ACL", roundTrip.getLabel(), json);
    assertNotNull(roundTrip.getGuid(), json);
  }

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
