/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.rest.acls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.rest.Guid;
import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.Permissions;
import com.percussion.security.IPSTypedPrincipal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * PUT {@code /services/acls/bulk} wire shape (#3378 / QA #2640).
 *
 * <p>Production JSON uses {@link JacksonContextResolver} WRAP/UNWRAP_ROOT_VALUE so the envelope
 * must be {@code AclList}, not a bare JSON array. Display Format Object ACL Save sent a raw array
 * and received HTTP 400.
 */
@Tag("UnitTest")
public class AclListSerialDeserialTest {

  private static Acl sampleDisplayFormatAcl() {
    Acl acl = new Acl();
    acl.setId(7);
    acl.setName("By_Author ACL");
    acl.setObjectId(5);
    acl.setObjectType(31);
    Guid objectGuid = new Guid();
    objectGuid.setStringValue("0-31-5");
    objectGuid.setType((short) 31);
    objectGuid.setUuid(5);
    acl.setObjectGuid(objectGuid);

    AclEntryList entries = new AclEntryList();
    entries.add(
        entry(
            1,
            "Default",
            IPSTypedPrincipal.PrincipalTypes.USER,
            Permissions.READ,
            Permissions.UPDATE));
    entries.add(
        entry(
            2,
            "AnyCommunity",
            IPSTypedPrincipal.PrincipalTypes.COMMUNITY,
            Permissions.RUNTIME_VISIBLE));
    entries.add(
        entry(3, "Admin", IPSTypedPrincipal.PrincipalTypes.USER, Permissions.READ, Permissions.OWNER));
    acl.setAclEntries(entries);
    return acl;
  }

  private static AclEntry entry(
      long id, String name, IPSTypedPrincipal.PrincipalTypes type, Permissions... perms) {
    AclEntry e = new AclEntry();
    e.setId(id);
    e.setName(name);
    e.setAclId(7);
    e.setPrincipal(new Principal(name));
    e.setType(new TypedPrincipal(name, type));
    UserAccessLevelList levels = new UserAccessLevelList();
    for (Permissions p : perms) {
      UserAccessLevel ual = new UserAccessLevel();
      ual.setPermission(p);
      levels.add(ual);
    }
    e.setPermissions(levels);
    return e;
  }

  private static JsonMapper wrapRootMapper() {
    return JsonMapper.builder()
        .enable(SerializationFeature.WRAP_ROOT_VALUE)
        .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
  }

  @Test
  public void jacksonWrapsAclListRoot() throws JacksonException {
    var mapper = wrapRootMapper();
    AclList list = new AclList();
    list.add(sampleDisplayFormatAcl());

    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("\"AclList\""), "expected WRAP_ROOT_VALUE AclList: " + json);
    assertTrue(json.contains("\"By_Author ACL\""), json);
    assertTrue(json.contains("Default"), json);
    assertTrue(json.contains("AnyCommunity"), json);
    assertTrue(json.contains("Admin"), json);

    AclList roundTrip = mapper.readValue(json, AclList.class);
    assertEquals(1, roundTrip.size());
    Acl acl = roundTrip.get(0);
    assertEquals("By_Author ACL", acl.getName());
    assertEquals(5, acl.getObjectId());
    assertEquals(31, acl.getObjectType());
    assertNotNull(acl.getObjectGuid());
    assertEquals("0-31-5", acl.getObjectGuid().getStringValue().orElse(null));
    assertEquals(3, acl.getAclEntries() == null ? 0 : acl.getAclEntries().size());
  }

  @Test
  public void productionMapperRoundTripsDisplayFormatSaveEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AclList.class);
    AclList list = new AclList();
    list.add(sampleDisplayFormatAcl());

    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("\"AclList\""), json);
    assertFalse(
        json.contains("\"empty\"") && !json.contains("\"name\""),
        "name must not serialize only as an Optional bean: " + json);

    AclList roundTrip = mapper.readValue(json, AclList.class);
    assertEquals(1, roundTrip.size());
    assertEquals(
        3, roundTrip.get(0).getAclEntries() == null ? 0 : roundTrip.get(0).getAclEntries().size());
    assertEquals(31, roundTrip.get(0).getObjectType());
  }

  @Test
  public void productionJavaTypeIsAclListNotRawArrayList() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AclList.class);
    String clientBody =
        "{\"AclList\":[{\"id\":7,\"name\":\"By_Author ACL\",\"objectType\":31,\"aclEntries\":[]}]}";
    Object raw = mapper.readValue(clientBody, mapper.getTypeFactory().constructType(AclList.class));
    assertTrue(raw instanceof AclList, "expected AclList, got " + (raw == null ? "null" : raw.getClass()));
    assertEquals(1, ((AclList) raw).size());
  }

  @Test
  public void productionMapperAcceptsClientAclListEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AclList.class);
    String clientBody =
        "{\"AclList\":[{"
            + "\"id\":7,"
            + "\"name\":\"By_Author ACL\","
            + "\"objectId\":5,"
            + "\"objectType\":31,"
            + "\"objectGuid\":{\"stringValue\":\"0-31-5\"},"
            + "\"aclEntries\":["
            + "{\"name\":\"Default\",\"principal\":{\"name\":\"Default\"},"
            + "\"type\":{\"name\":\"Default\",\"type\":\"USER\"},"
            + "\"permissions\":[{\"permission\":\"READ\"}]},"
            + "{\"name\":\"AnyCommunity\",\"principal\":{\"name\":\"AnyCommunity\"},"
            + "\"type\":{\"name\":\"AnyCommunity\",\"type\":\"COMMUNITY\"},"
            + "\"permissions\":[{\"permission\":\"RUNTIME_VISIBLE\"}]},"
            + "{\"name\":\"Admin\",\"principal\":{\"name\":\"Admin\"},"
            + "\"type\":{\"name\":\"Admin\",\"type\":\"USER\"},"
            + "\"permissions\":[{\"permission\":\"READ\"},{\"permission\":\"OWNER\"}]}"
            + "]}]}";

    AclList list = mapper.readValue(clientBody, AclList.class);
    assertEquals(1, list.size());
    Acl acl = list.get(0);
    assertEquals("By_Author ACL", acl.getName());
    assertEquals(31, acl.getObjectType());
    assertEquals(5, acl.getObjectId());
    assertNotNull(acl.getAclEntries());
    assertEquals(3, acl.getAclEntries().size());
    assertEquals("Default", acl.getAclEntries().get(0).getName());
    assertEquals("AnyCommunity", acl.getAclEntries().get(1).getName());
    assertEquals("Admin", acl.getAclEntries().get(2).getName());
  }

  @Test
  public void productionMapperRejectsBareJsonArray() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AclList.class);
    String bareArray = "[{\"id\":7,\"name\":\"By_Author ACL\",\"aclEntries\":[]}]";
    try {
      AclList result = mapper.readValue(bareArray, AclList.class);
      assertTrue(
          result == null || result.isEmpty(),
          "bare JSON array must not bind as AclList under UNWRAP_ROOT_VALUE; size="
              + (result == null ? "null" : result.size()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("AclList")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("START_ARRAY")
                  || expected.getMessage().contains("from Array value")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  public void productionMapperAcceptsPrincipalWithUnknownTypeField() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AclList.class);
    String body =
        "{\"AclList\":[{\"id\":1,\"name\":\"x\",\"aclEntries\":["
            + "{\"name\":\"Admin\",\"principal\":{\"name\":\"Admin\",\"type\":\"USER\"},"
            + "\"type\":{\"name\":\"Admin\",\"type\":\"USER\"},"
            + "\"permissions\":[{\"permission\":\"READ\"}]}]}]}";
    AclList list = mapper.readValue(body, AclList.class);
    assertEquals(1, list.size());
    assertEquals(
        "Admin", list.get(0).getAclEntries().get(0).getPrincipal().getName());
  }

  @Test
  public void createAclRequestRequiresEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(CreateAclRequest.class);
    String wrapped =
        "{\"CreateAclRequest\":{"
            + "\"objectGuid\":{\"stringValue\":\"0-31-5\"},"
            + "\"owner\":{\"name\":\"Admin\",\"type\":\"USER\"}}}";
    CreateAclRequest req = mapper.readValue(wrapped, CreateAclRequest.class);
    assertEquals("0-31-5", req.getObjectGuid().getStringValue().orElse(null));
    assertEquals("Admin", req.getOwner().getName());
    assertEquals(IPSTypedPrincipal.PrincipalTypes.USER, req.getOwner().getType());

    String flat =
        "{\"objectGuid\":{\"stringValue\":\"0-31-5\"},\"owner\":{\"name\":\"Admin\",\"type\":\"USER\"}}";
    try {
      CreateAclRequest result = mapper.readValue(flat, CreateAclRequest.class);
      assertTrue(
          result == null
              || result.getObjectGuid() == null
              || result.getObjectGuid().getStringValue().orElse("").isBlank(),
          "flat CreateAclRequest must not bind under UNWRAP_ROOT_VALUE");
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("CreateAclRequest")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("objectGuid")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  public void jaxbContextKnowsAclFromList() throws Exception {
    jakarta.xml.bind.JAXBContext ctx = jakarta.xml.bind.JAXBContext.newInstance(AclList.class);
    AclList list = new AclList();
    list.add(sampleDisplayFormatAcl());
    jakarta.xml.bind.Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new java.io.StringWriter();
    try {
      marshaller.marshal(list, writer);
    } catch (Exception e) {
      fail("JAXB must marshal AclList containing Acl (#3378); got: " + e.getMessage(), e);
    }
    String xml = writer.toString();
    assertTrue(xml.contains("AclList") || xml.contains("aclList"), xml);
  }
}
