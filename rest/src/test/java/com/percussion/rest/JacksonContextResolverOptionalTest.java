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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.Permissions;
import com.percussion.rest.acls.Acl;
import com.percussion.rest.acls.AclEntry;
import com.percussion.rest.acls.AclEntryList;
import com.percussion.rest.acls.AclList;
import com.percussion.rest.acls.TypedPrincipal;
import com.percussion.rest.acls.UserAccessLevel;
import com.percussion.rest.acls.UserAccessLevelList;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import com.percussion.security.IPSTypedPrincipal;
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
  void acl_serializesNameGuidEntriesNotOptionalBeans() {
    ObjectMapper aclMapper = new JacksonContextResolver().getContext(Acl.class);
    Acl acl = sampleAcl();

    String json = aclMapper.writeValueAsString(acl);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("By_Author ACL"), json);
    assertTrue(json.contains("Default"), json);
    assertTrue(json.contains("AnyCommunity"), json);
    assertTrue(json.contains("Admin"), json);
    assertTrue(json.contains("aclEntries") || json.contains("AclEntries"), json);
    assertFalse(json.contains("\"empty\""), json);
    assertFalse(json.contains("\"present\""), json);

    Acl roundTrip = aclMapper.readValue(json, Acl.class);
    assertEquals("By_Author ACL", roundTrip.getName());
    assertEquals(3, roundTrip.getAclEntries().size());
    assertEquals("Default", roundTrip.getAclEntries().get(0).getName());
  }

  @Test
  void aclList_serializesEnvelopeScalarsNotOptionalBeans() {
    ObjectMapper listMapper = new JacksonContextResolver().getContext(AclList.class);
    AclList list = new AclList();
    list.add(sampleAcl());

    String json = listMapper.writeValueAsString(list);
    assertTrue(json.contains("\"AclList\"") || json.contains("By_Author ACL"), json);
    assertTrue(json.contains("\"name\""), json);
    assertFalse(json.contains("\"empty\""), json);
    assertFalse(json.contains("\"present\""), json);

    AclList roundTrip = listMapper.readValue(json, AclList.class);
    assertEquals(1, roundTrip.size());
    assertEquals("By_Author ACL", roundTrip.get(0).getName());
    assertEquals(3, roundTrip.get(0).getAclEntries().size());
  }

  @Test
  void userAccessLevel_serializesPermissionNotOptionalBean() {
    ObjectMapper ualMapper = new JacksonContextResolver().getContext(UserAccessLevel.class);
    UserAccessLevel level = new UserAccessLevel();
    level.setId(9);
    level.setPermission(Permissions.READ);

    String json = ualMapper.writeValueAsString(level);
    assertTrue(json.contains("READ"), json);
    assertTrue(json.contains("\"permission\""), json);
    assertFalse(json.contains("\"empty\""), json);
    assertFalse(json.contains("\"present\""), json);

    UserAccessLevel roundTrip = ualMapper.readValue(json, UserAccessLevel.class);
    assertEquals(Permissions.READ, roundTrip.getPermission());
    assertEquals(9, roundTrip.getId());
  }

  private static Acl sampleAcl() {
    Acl acl = new Acl();
    acl.setId(7);
    acl.setName("By_Author ACL");
    acl.setObjectType(31);
    acl.setObjectId(5);
    Guid objectGuid = new Guid();
    objectGuid.setStringValue("0-31-5");
    acl.setObjectGuid(objectGuid);
    AclEntryList entries = new AclEntryList();
    entries.add(namedAclEntry("Default", IPSTypedPrincipal.PrincipalTypes.USER, Permissions.READ));
    entries.add(
        namedAclEntry(
            "AnyCommunity",
            IPSTypedPrincipal.PrincipalTypes.COMMUNITY,
            Permissions.RUNTIME_VISIBLE));
    entries.add(namedAclEntry("Admin", IPSTypedPrincipal.PrincipalTypes.USER, Permissions.OWNER));
    acl.setAclEntries(entries);
    return acl;
  }

  private static AclEntry namedAclEntry(
      String name, IPSTypedPrincipal.PrincipalTypes type, Permissions permission) {
    AclEntry entry = new AclEntry();
    entry.setName(name);
    entry.setType(new TypedPrincipal(name, type));
    UserAccessLevelList levels = new UserAccessLevelList();
    UserAccessLevel level = new UserAccessLevel();
    level.setPermission(permission);
    levels.add(level);
    entry.setPermissions(levels);
    return entry;
  }
}
