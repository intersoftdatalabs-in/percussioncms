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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class AclListJsonReaderTest {

  private static final String DF_SAVE =
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
          + "\"permissions\":[{\"permission\":\"READ\"}]}"
          + "]}]}";

  @Test
  public void parseAcceptsAclListEnvelope() {
    AclList list = AclListJsonReader.parse(DF_SAVE);
    assertInstanceOf(AclList.class, list);
    assertEquals(1, list.size());
    Acl acl = list.get(0);
    assertEquals("By_Author ACL", acl.getName());
    assertEquals(31, acl.getObjectType());
    assertEquals(5, acl.getObjectId());
    assertEquals(3, acl.getAclEntries() == null ? 0 : acl.getAclEntries().size());
    assertEquals("Default", acl.getAclEntries().get(0).getName());
    assertEquals("AnyCommunity", acl.getAclEntries().get(1).getName());
    assertEquals("Admin", acl.getAclEntries().get(2).getName());
  }

  @Test
  public void parseAcceptsBareArray() {
    AclList list = AclListJsonReader.parse("[{\"name\":\"x\",\"objectType\":31}]");
    assertEquals(1, list.size());
    assertEquals("x", list.get(0).getName());
    assertEquals(31, list.get(0).getObjectType());
  }

  @Test
  public void parseEmptyIsEmptyList() {
    assertTrue(AclListJsonReader.parse("  ").isEmpty());
    assertTrue(AclListJsonReader.parse(null).isEmpty());
  }

  @Test
  public void parseRejectsNonListObject() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> AclListJsonReader.parse("{\"foo\":1}"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void isReadableOnlyForAclList() {
    AclListJsonReader reader = new AclListJsonReader();
    assertTrue(reader.isReadable(AclList.class, AclList.class, null, null));
    assertTrue(
        reader.isReadable(
            AclList.class, AclList.class, null, jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE));
    assertTrue(
        reader.isReadable(
            AclList.class,
            AclList.class,
            null,
            jakarta.ws.rs.core.MediaType.valueOf("application/json; charset=UTF-8")));
    assertTrue(!reader.isReadable(Acl.class, Acl.class, null, null));
  }

  @Test
  public void parseNodeAcceptsEnvelope() {
    AclList list = AclListJsonReader.parseNode(
        tools.jackson.databind.json.JsonMapper.builder().build().readTree(DF_SAVE));
    assertEquals(1, list.size());
    assertEquals(
        3, list.get(0).getAclEntries() == null ? 0 : list.get(0).getAclEntries().size());
  }
}
