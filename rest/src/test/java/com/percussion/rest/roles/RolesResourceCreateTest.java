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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.percussion.rest.errors.BackendException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** PUT /roles create path uses adaptor createRole, not update, for new names. */
@Tag("UnitTest")
public class RolesResourceCreateTest {

  private IRoleAdaptor adaptor;
  private RolesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = Mockito.mock(IRoleAdaptor.class);
    resource = new RolesResource();
    resource.setRoleAdaptor(adaptor);
  }

  @Test
  public void blankNameIs400BeforeAdaptor() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateRole(Boolean.TRUE, new Role()));
    assertEquals(400, ex.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void whitespaceNameIs400BeforeAdaptor() {
    Role role = new Role();
    role.setName("   ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateRole(null, role));
    assertEquals(400, ex.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void explicitCreateUsesCreateRoleNotUpdate() throws Exception {
    Role input = named("NightRole", "Editors");
    Role saved = named("NightRole", "Editors");
    when(adaptor.createRole(isNull(), eq(input))).thenReturn(saved);

    Role out = resource.updateRole(Boolean.TRUE, input);

    assertEquals("NightRole", out.getName());
    verify(adaptor).createRole(isNull(), eq(input));
    verify(adaptor, never()).updateRole(any(), any());
    verify(adaptor, never()).roleExists(any(), any());
  }

  @Test
  public void newNameWithoutFlagUsesCreateRole() throws Exception {
    Role input = named("NightRole", "Editors");
    when(adaptor.roleExists(isNull(), eq("NightRole"))).thenReturn(false);
    when(adaptor.createRole(isNull(), eq(input))).thenReturn(input);

    Role out = resource.updateRole(null, input);

    assertEquals("NightRole", out.getName());
    verify(adaptor).createRole(isNull(), eq(input));
    verify(adaptor, never()).updateRole(any(), any());
  }

  @Test
  public void existingNameWithoutFlagUsesUpdateRole() throws Exception {
    Role input = named("Author", "Authors");
    when(adaptor.roleExists(isNull(), eq("Author"))).thenReturn(true);
    when(adaptor.updateRole(isNull(), eq(input))).thenReturn(input);

    Role out = resource.updateRole(Boolean.FALSE, input);

    assertEquals("Author", out.getName());
    verify(adaptor).updateRole(isNull(), eq(input));
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void duplicateCreateDoesNotUpdate() throws Exception {
    Role input = named("Author", "Changed");
    when(adaptor.createRole(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("already exists", 400));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateRole(Boolean.TRUE, input));

    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateRole(any(), any());
    verify(adaptor, never()).roleExists(any(), any());
  }

  @Test
  public void createForbiddenIs403() throws Exception {
    Role input = named("NightRole", "Editors");
    when(adaptor.createRole(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Admin role required to create a role", 403));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateRole(Boolean.TRUE, input));

    assertEquals(403, ex.getResponse().getStatus());
    verify(adaptor, never()).updateRole(any(), any());
  }

  @Test
  public void createBackendFailureIs500() throws Exception {
    Role input = named("NightRole", "Editors");
    when(adaptor.createRole(isNull(), eq(input)))
        .thenThrow(new BackendException("db down", new Exception("db down")));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.updateRole(Boolean.TRUE, input));

    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void trimsNameBeforeCreate() throws Exception {
    Role input = named("  NightRole  ", "Editors");
    when(adaptor.createRole(isNull(), eq(input))).thenReturn(named("NightRole", "Editors"));

    resource.updateRole(Boolean.TRUE, input);

    assertEquals("NightRole", input.getName());
    verify(adaptor).createRole(isNull(), eq(input));
  }

  private static Role named(String name, String description) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    return role;
  }
}
