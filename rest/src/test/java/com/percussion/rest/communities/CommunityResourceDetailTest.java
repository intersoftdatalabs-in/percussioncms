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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class CommunityResourceDetailTest {

  private ICommunityAdaptor adaptor;
  private CommunityResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ICommunityAdaptor.class);
    resource = new CommunityResource();
    Field f = CommunityResource.class.getDeclaredField("adaptor");
    f.setAccessible(true);
    f.set(resource, adaptor);
  }

  @Test
  public void getCommunityByName() {
    Community c = new Community();
    c.setName("Default");
    c.setLabel("Default");
    when(adaptor.getCommunity(eq("Default"))).thenReturn(c);
    assertEquals("Default", resource.getCommunity("Default").getName());
  }

  @Test
  public void getCommunityByNumericId() {
    Community c = new Community();
    c.setName("ById");
    when(adaptor.getCommunity(eq("10"))).thenReturn(c);
    assertEquals("ById", resource.getCommunity("10").getName());
  }

  @Test
  public void getCommunityByGuidString() {
    Community c = new Community();
    c.setName("ByGuid");
    when(adaptor.getCommunity(eq("0-13-10"))).thenReturn(c);
    assertEquals("ByGuid", resource.getCommunity("0-13-10").getName());
  }

  @Test
  public void getCommunityNotFound() {
    when(adaptor.getCommunity(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getCommunity("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getCommunityServerError() {
    when(adaptor.getCommunity(eq("boom"))).thenThrow(new RuntimeException("db down"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getCommunity("boom"));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void listAvailableRoles() {
    CommunityRoleList list = new CommunityRoleList();
    CommunityRole r = new CommunityRole();
    r.setRoleName("Admin");
    list.add(r);
    when(adaptor.listAvailableRoles()).thenReturn(list);
    assertEquals(1, resource.listAvailableRoles().size());
    assertEquals("Admin", resource.listAvailableRoles().get(0).getRoleName());
  }

  @Test
  public void updateCommunityRolesSuccess() {
    CommunityRoleList body = new CommunityRoleList();
    Community updated = new Community();
    updated.setName("Default");
    updated.setRoleList(body);
    when(adaptor.updateCommunityRoles(eq("Default"), any())).thenReturn(updated);
    assertEquals("Default", resource.updateCommunityRoles("Default", body).getName());
  }

  @Test
  public void updateCommunityRolesJsonParsesEnvelope() {
    Community updated = new Community();
    updated.setName("Default");
    updated.setRoleList(new CommunityRoleList());
    when(adaptor.updateCommunityRoles(eq("Default"), any())).thenReturn(updated);
    String json =
        "{\"CommunityRoleList\":[{\"roleName\":\"Editor\",\"roleId\":4,"
            + "\"roleGuid\":{\"stringValue\":\"0-16-4\"}}]}";
    assertEquals("Default", resource.updateCommunityRolesJson("Default", json).getName());
  }

  @Test
  public void updateCommunityRolesEmptyListUnassignsAll() {
    CommunityRoleList empty = new CommunityRoleList();
    Community updated = new Community();
    updated.setName("Default");
    updated.setRoleList(empty);
    when(adaptor.updateCommunityRoles(eq("Default"), any())).thenReturn(updated);
    Community out = resource.updateCommunityRoles("Default", empty);
    assertEquals("Default", out.getName());
    assertEquals(0, out.getRoleList().size());
  }

  @Test
  public void updateCommunityRolesNotFound() {
    when(adaptor.updateCommunityRoles(eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityRoles("missing", new CommunityRoleList()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateCommunityRolesBadRequest() {
    when(adaptor.updateCommunityRoles(eq("Default"), any()))
        .thenThrow(new IllegalArgumentException("idOrName is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityRoles("Default", new CommunityRoleList()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateCommunityRolesServerError() {
    when(adaptor.updateCommunityRoles(eq("Default"), any()))
        .thenThrow(new RuntimeException("db down"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateCommunityRoles("Default", new CommunityRoleList()));
    assertEquals(500, ex.getResponse().getStatus());
  }
}
