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

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** PUT /roles?addUser=true adds one user and does not create a missing role. */
@Tag("UnitTest")
public class RolesResourceAddUserTest {

  private IRoleAdaptor adaptor;
  private RolesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = Mockito.mock(IRoleAdaptor.class);
    resource = new RolesResource();
    resource.setRoleAdaptor(adaptor);
  }

  @Test
  public void addUserFlagUsesAddNotCreateOrDescriptionUpdate() throws Exception {
    Role input = named("Author", "Ada");
    Role saved = named("Author", "Ada");
    saved.setUsers(List.of("Bea", "Ada"));
    when(adaptor.addRoleUser(isNull(), eq(input))).thenReturn(saved);

    Role out = resource.updateRole(null, null, null, Boolean.TRUE, input);

    assertEquals(List.of("Bea", "Ada"), out.getUsers());
    verify(adaptor).addRoleUser(isNull(), eq(input));
    verify(adaptor, never()).updateRole(any(), any());
    verify(adaptor, never()).updateRoleHomePage(any(), any());
    verify(adaptor, never()).createRole(any(), any());
    verify(adaptor, never()).roleExists(any(), any());
  }

  @Test
  public void unknownUserIs400AndDoesNotCreate() throws Exception {
    Role input = named("Author", "Mallory");
    when(adaptor.addRoleUser(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Unknown user", 400));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, null, Boolean.TRUE, input));

    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
    verify(adaptor, never()).updateRole(any(), any());
  }

  @Test
  public void alreadyMemberIs409() throws Exception {
    Role input = named("Author", "Ada");
    when(adaptor.addRoleUser(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("User is already a member of this role", 409));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, input));

    assertEquals(409, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
    verify(adaptor, never()).updateRole(any(), any());
  }

  @Test
  public void missingRoleIs404AndDoesNotCreate() throws Exception {
    Role input = named("Gone", "Ada");
    when(adaptor.addRoleUser(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Role not found", 404));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, null, Boolean.TRUE, input));

    assertEquals(404, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void forbiddenIs403() throws Exception {
    Role input = named("Author", "Ada");
    when(adaptor.addRoleUser(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, null, Boolean.TRUE, input));

    assertEquals(403, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void blankRoleNameIs400BeforeAdaptor() {
    Role input = new Role();
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, null, Boolean.TRUE, input));
    assertEquals(400, ex.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void addUserCombinedWithOtherFlagsIs400() {
    Role input = named("Author", "Ada");
    WebApplicationException withCreate =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(Boolean.TRUE, null, null, Boolean.TRUE, input));
    assertEquals(400, withCreate.getResponse().getStatus());
    WebApplicationException withUpdate =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, Boolean.TRUE, null, Boolean.TRUE, input));
    assertEquals(400, withUpdate.getResponse().getStatus());
    WebApplicationException withHome =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, Boolean.TRUE, Boolean.TRUE, input));
    assertEquals(400, withHome.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void descriptionUpdateStillIgnoresTheAddUserFlagWhenUnset() throws Exception {
    Role input = new Role();
    input.setName("Author");
    input.setDescription("Editors");
    input.setUsers(List.of("Mallory"));
    when(adaptor.updateRole(isNull(), eq(input))).thenReturn(input);

    resource.updateRole(null, Boolean.TRUE, null, input);

    verify(adaptor).updateRole(isNull(), eq(input));
    verify(adaptor, never()).addRoleUser(any(), any());
    verify(adaptor, never()).createRole(any(), any());
  }

  private static Role named(String name, String userName) {
    Role role = new Role();
    role.setName(name);
    role.setUsers(List.of(userName));
    return role;
  }
}
