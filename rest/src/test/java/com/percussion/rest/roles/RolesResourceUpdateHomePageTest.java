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

/** PUT /roles?homePage=true updates a home page and does not create a missing role. */
@Tag("UnitTest")
public class RolesResourceUpdateHomePageTest {

  private IRoleAdaptor adaptor;
  private RolesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = Mockito.mock(IRoleAdaptor.class);
    resource = new RolesResource();
    resource.setRoleAdaptor(adaptor);
  }

  @Test
  public void homePageFlagUsesHomePageUpdateNotCreate() throws Exception {
    Role input = named("Author", "Explorer");
    when(adaptor.updateRoleHomePage(isNull(), eq(input))).thenReturn(input);

    Role out = resource.updateRole(null, null, Boolean.TRUE, input);

    assertEquals("Explorer", out.getHomePage());
    verify(adaptor).updateRoleHomePage(isNull(), eq(input));
    verify(adaptor, never()).updateRole(any(), any());
    verify(adaptor, never()).createRole(any(), any());
    verify(adaptor, never()).roleExists(any(), any());
  }

  @Test
  public void blankHomePageIsForwardedSoTheAdaptorCanClear() throws Exception {
    Role input = named("Author", "   ");
    Role cleared = named("Author", null);
    when(adaptor.updateRoleHomePage(isNull(), eq(input))).thenReturn(cleared);

    Role out = resource.updateRole(Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, input);

    assertNull(out.getHomePage());
    verify(adaptor).updateRoleHomePage(isNull(), eq(input));
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void missingRoleOnHomePageUpdateIs404AndDoesNotCreate() throws Exception {
    Role input = named("Gone", "Home");
    when(adaptor.updateRoleHomePage(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Role not found", 404));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, Boolean.TRUE, input));

    assertEquals(404, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
    verify(adaptor, never()).updateRole(any(), any());
  }

  @Test
  public void homePageForbiddenIs403() throws Exception {
    Role input = named("Author", "Home");
    when(adaptor.updateRoleHomePage(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Admin role required", 403));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, Boolean.FALSE, Boolean.TRUE, input));

    assertEquals(403, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void unknownHomePageIs400() throws Exception {
    Role input = named("Author", "NotAPage");
    when(adaptor.updateRoleHomePage(isNull(), eq(input)))
        .thenThrow(new WebApplicationException("Role home page is not a known landing page.", 400));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, Boolean.TRUE, input));

    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).createRole(any(), any());
  }

  @Test
  public void blankNameIs400BeforeAdaptor() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, null, Boolean.TRUE, new Role()));
    assertEquals(400, ex.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void homePageCombinedWithCreateOrUpdateIs400() {
    Role input = named("Author", "Home");
    WebApplicationException withCreate =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(Boolean.TRUE, null, Boolean.TRUE, input));
    assertEquals(400, withCreate.getResponse().getStatus());
    WebApplicationException withUpdate =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateRole(null, Boolean.TRUE, Boolean.TRUE, input));
    assertEquals(400, withUpdate.getResponse().getStatus());
    verifyNoInteractions(adaptor);
  }

  @Test
  public void trimsNameBeforeHomePageUpdate() {
    Role input = named("  Author  ", "Home");
    when(adaptor.updateRoleHomePage(isNull(), eq(input))).thenReturn(named("Author", "Home"));

    resource.updateRole(null, null, Boolean.TRUE, input);

    assertEquals("Author", input.getName());
    verify(adaptor).updateRoleHomePage(isNull(), eq(input));
  }

  private static Role named(String name, String homePage) {
    Role role = new Role();
    role.setName(name);
    role.setHomePage(homePage);
    return role;
  }
}
