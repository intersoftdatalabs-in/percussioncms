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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** PUT /roles?update=true updates a description and does not create a missing role. */
@Tag("UnitTest")
public class RolesResourceUpdateDescriptionTest {

  private IRoleAdaptor adaptor;
  private RolesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = Mockito.mock(IRoleAdaptor.class);
    resource = new RolesResource();
    resource.setRoleAdaptor(adaptor);
  }

  @Test
  public void updateFlagUsesUpdateRoleNotCreate() throws Exception {
    Role input = named("Author", "Authors");
    when(adaptor.updateRole(isNull(), eq(input))).thenReturn(input);

    Role out = resource.updateRole(null, Boolean.TRUE, input);

    assertEquals("Authors", out.getDescription());
    verify(adaptor).updateRole(isNull(), eq(input));
    verify(adaptor, never()).createRole(any(), any());
    verify(adaptor, never()).roleExists(any(), any());
  }

  @Test
  public void missingRoleOnUpdateIs404AndDoesNotCreate() throws Exception {
    Role input = named("Gone", "Nope");
    when(adaptor.updateRole(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Role not found", 404));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateRole(null, Boolean.TRUE, input));

    assertEquals(404, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void updateForbiddenIs403() throws Exception {
    Role input = named("Author", "Nope");
    when(adaptor.updateRole(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateRole(Boolean.FALSE, Boolean.TRUE, input));

    assertEquals(403, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void updateRejectedIs400() throws Exception {
    Role input = named("System", "Nope");
    when(adaptor.updateRole(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Cannot update system role", 400));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateRole(null, Boolean.TRUE, input));

    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void blankNameIs400BeforeAdaptor() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, Boolean.TRUE, new Role()));
    assertEquals(400, ex.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void createAndUpdateTogetherIs400() {
    Role input = named("Author", "Nope");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(Boolean.TRUE, Boolean.TRUE, input));
    assertEquals(400, ex.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void trimsNameBeforeUpdate() {
    Role input = named("  Author  ", "Authors");
    when(adaptor.updateRole(isNull(), eq(input))).thenReturn(named("Author", "Authors"));

    resource.updateRole(null, Boolean.TRUE, input);

    assertEquals("Author", input.getName());
    verify(adaptor).updateRole(isNull(), eq(input));
  }

  private static Role named(String name, String description) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    return role;
  }
}
