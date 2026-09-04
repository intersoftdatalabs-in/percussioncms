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
package com.percussion.rest.communities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class CommunityRoleListJsonReaderTest {

  private static final String ENVELOPE =
      "{\"CommunityRoleList\":[{\"roleName\":\"Editor\",\"roleId\":4,"
          + "\"roleGuid\":{\"stringValue\":\"0-16-4\",\"uuid\":4,\"type\":16}}]}";

  @Test
  public void parseAcceptsCommunityRoleListEnvelope() {
    CommunityRoleList list = CommunityRoleListJsonReader.parse(ENVELOPE);
    assertInstanceOf(CommunityRoleList.class, list);
    assertEquals(1, list.size());
    assertEquals("Editor", list.get(0).getRoleName());
    assertEquals(4L, list.get(0).getRoleId());
  }

  @Test
  public void parseAcceptsBareArray() {
    CommunityRoleList list =
        CommunityRoleListJsonReader.parse("[{\"roleName\":\"Admin\",\"roleId\":1}]");
    assertEquals(1, list.size());
    assertEquals("Admin", list.get(0).getRoleName());
  }

  @Test
  public void parseEmptyClearsMembership() {
    assertTrue(CommunityRoleListJsonReader.parse("{\"CommunityRoleList\":[]}").isEmpty());
    assertTrue(CommunityRoleListJsonReader.parse("[]").isEmpty());
    assertTrue(CommunityRoleListJsonReader.parse("  ").isEmpty());
    assertTrue(CommunityRoleListJsonReader.parse(null).isEmpty());
  }

  @Test
  public void parseRejectsNonListObject() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> CommunityRoleListJsonReader.parse("{\"foo\":1}"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void isReadableOnlyForCommunityRoleList() {
    CommunityRoleListJsonReader reader = new CommunityRoleListJsonReader();
    assertTrue(reader.isReadable(CommunityRoleList.class, CommunityRoleList.class, null, null));
    assertTrue(!reader.isReadable(CommunityRole.class, CommunityRole.class, null, null));
  }
}
