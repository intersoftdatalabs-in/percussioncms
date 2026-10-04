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

package com.percussion.rest.roles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Status;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class RolesResourceBrowseTest {

  private IRoleAdaptor adaptor;
  private RolesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IRoleAdaptor.class);
    resource = new RolesResource();
    resource.setRoleAdaptor(adaptor);
  }

  @Test
  public void browseFullCatalogIs200() {
    RoleBrowseCatalog catalog = new RoleBrowseCatalog();
    RoleBrowseEntry entry = new RoleBrowseEntry();
    entry.setName("Author");
    entry.setGroups(List.of(RoleBrowseGroup.COMMUNITY.getWireValue()));
    catalog.setRoles(List.of(entry));
    when(adaptor.browseRoles(isNull(), isNull())).thenReturn(catalog);

    RoleBrowseCatalog out = resource.browseRoles(null);
    assertEquals(1, out.getRoles().size());
    assertEquals("Author", out.getRoles().get(0).getName());
  }

  @Test
  public void browsePassesGroupFilter() {
    RoleBrowseCatalog catalog = new RoleBrowseCatalog();
    catalog.setGroup("unassigned");
    catalog.setRoles(List.of());
    when(adaptor.browseRoles(isNull(), eq("unassigned"))).thenReturn(catalog);

    RoleBrowseCatalog out = resource.browseRoles("unassigned");
    assertEquals("unassigned", out.getGroup());
    assertTrue(out.getRoles().isEmpty());
  }

  @Test
  public void browseRethrowsForbidden() {
    WebApplicationException forbidden = new WebApplicationException("Admin role required", 403);
    when(adaptor.browseRoles(isNull(), isNull())).thenThrow(forbidden);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.browseRoles(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertSame(forbidden, ex);
  }

  @Test
  public void browseInvalidGroupIs400() {
    when(adaptor.browseRoles(isNull(), eq("nope")))
        .thenThrow(new IllegalArgumentException("Unknown role browse group 'nope'"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.browseRoles("nope"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("nope"));
  }

  @Test
  public void missingAdaptorIs503() {
    RolesResource bare = new RolesResource();
    WebApplicationException browse =
        assertThrows(WebApplicationException.class, () -> bare.browseRoles(null));
    assertEquals(503, browse.getResponse().getStatus());
    WebApplicationException get =
        assertThrows(WebApplicationException.class, () -> bare.getRoleByName("Author"));
    assertEquals(503, get.getResponse().getStatus());
    Role named = new Role();
    named.setName("Author");
    WebApplicationException update =
        assertThrows(WebApplicationException.class, () -> bare.updateRole(null, null, null, named));
    assertEquals(503, update.getResponse().getStatus());
    WebApplicationException find =
        assertThrows(WebApplicationException.class, () -> bare.findRoles());
    assertEquals(503, find.getResponse().getStatus());
    WebApplicationException delete =
        assertThrows(WebApplicationException.class, () -> bare.deleteRole("Author"));
    assertEquals(503, delete.getResponse().getStatus());
  }

  @Test
  public void deleteReturnsOkStatus() throws Exception {
    Status status = resource.deleteRole("Author");
    assertEquals(200, status.getStatusCode());
    assertEquals("OK", status.getMessage());
    verify(adaptor).deleteRole(isNull(), eq("Author"));
  }

  @Test
  public void deleteRethrows400403409() throws Exception {
    for (int code : new int[] {400, 403, 409}) {
      doThrow(new WebApplicationException("refused", code))
          .when(adaptor)
          .deleteRole(isNull(), eq("Author"));
      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> resource.deleteRole("Author"));
      assertEquals(code, ex.getResponse().getStatus());
    }
  }

  @Test
  public void deleteBlankNameIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteRole("  "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void roleBrowseGroupParsesWireValues() {
    assertEquals(RoleBrowseGroup.COMMUNITY, RoleBrowseGroup.fromWire("community"));
    assertEquals(RoleBrowseGroup.WORKFLOW, RoleBrowseGroup.fromWire("WORKFLOW"));
    assertEquals(RoleBrowseGroup.UNASSIGNED, RoleBrowseGroup.fromWire("Unassigned"));
    assertNull(RoleBrowseGroup.fromWire(" "));
    assertThrows(IllegalArgumentException.class, () -> RoleBrowseGroup.fromWire("other"));
  }
}
