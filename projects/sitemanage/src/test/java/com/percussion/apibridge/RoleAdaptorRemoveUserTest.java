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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.roles.Role;
import com.percussion.role.data.PSRole;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUserList;
import com.percussion.user.service.IPSUserService;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Remove-one-user keeps description and the other members, and maps 400 / 403 / 404 / 409. */
@Tag("UnitTest")
class RoleAdaptorRemoveUserTest {

  private PSRoleService roleService;
  private IPSSecurityDesignWs securityDesignWs;
  private IPSWorkflowService workflowService;
  private IPSUserService userService;
  private RoleAdaptor adaptor;

  @BeforeEach
  void setUp() {
    roleService = Mockito.mock(PSRoleService.class);
    securityDesignWs = Mockito.mock(IPSSecurityDesignWs.class);
    workflowService = Mockito.mock(IPSWorkflowService.class);
    userService = Mockito.mock(IPSUserService.class);
    adaptor =
        new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> true, userService);
  }

  @Test
  void remove_dropsOneUserAndKeepsDescriptionAndOtherMembers() throws Exception {
    roleNamed("Author");
    catalogUsers("Ada", "Bea");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Author", "Keep me", "Home", List.of("Bea", "Ada")));
    when(roleService.update(any(PSRole.class)))
        .thenReturn(stored("Author", "Keep me", "Home", List.of("Bea")));

    Role input = new Role();
    input.setName(" Author ");
    input.setDescription("Ignored description");
    input.setHomePage("Explorer");
    input.setUsers(List.of(" ada "));

    Role out = adaptor.removeRoleUser(null, input);

    assertEquals("Author", out.getName());
    assertEquals(List.of("Bea"), out.getUsers());
    assertEquals("Keep me", out.getDescription());
    ArgumentCaptor<PSRole> sent = ArgumentCaptor.forClass(PSRole.class);
    verify(roleService).update(sent.capture());
    assertEquals("Author", sent.getValue().getName());
    assertEquals("Keep me", sent.getValue().getDescription());
    assertEquals("Home", sent.getValue().getHomepage());
    assertEquals(List.of("Bea"), sent.getValue().getUsers());
    assertNull(sent.getValue().getOldName());
    verify(roleService).validateDeleteUsersFromRole(any(PSUserList.class));
    verify(roleService, never()).create(any());
    verify(roleService, never()).delete(any());
  }

  @Test
  void remove_blankUserIs400() throws Exception {
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("  "));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("User name is required"));
    verify(roleService, never()).update(any());
    verify(roleService, never()).find(any());
    verify(userService, never()).getUsers();
  }

  @Test
  void remove_moreThanOneUserIs400() throws Exception {
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("Ada", "Bea"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("exactly one"));
    verify(roleService, never()).update(any());
  }

  @Test
  void remove_unknownUserIs400() throws Exception {
    roleNamed("Author");
    catalogUsers("Ada");
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("Mallory"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Unknown user"));
    verify(roleService, never()).update(any());
    verify(roleService, never()).find(any());
  }

  @Test
  void remove_notAMemberIs400() throws Exception {
    roleNamed("Author");
    catalogUsers("Ada", "Bea");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Author", "Keep me", "Home", List.of("Bea")));
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("not a member"));
    verify(roleService, never()).update(any());
    verify(roleService, never()).validateDeleteUsersFromRole(any());
  }

  @Test
  void remove_strandedUserIs409AndDoesNotUpdate() throws Exception {
    roleNamed("Author");
    catalogUsers("Ada", "Bea");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Author", "Keep me", "Home", List.of("Ada", "Bea")));
    doThrow(
            new PSBeanValidationException(
                new PSRole(),
                "validateDeleteUsers",
                "The following users will be unable to login if they are removed from this role:"
                    + " Ada.",
                null))
        .when(roleService)
        .validateDeleteUsersFromRole(any(PSUserList.class));
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("unable to login"));
    verify(roleService, never()).update(any());
    verify(roleService, never()).delete(any());
  }

  @Test
  void remove_selfFromAdminIs409AndDoesNotUpdate() throws Exception {
    roleNamed("Admin");
    catalogUsers("Admin", "Editor");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Admin", "Operators", "Home", List.of("Admin", "Editor")));
    PSCurrentUser current = new PSCurrentUser();
    current.setName("Admin");
    when(userService.getCurrentUser()).thenReturn(current);
    Role input = new Role();
    input.setName("Admin");
    input.setUsers(List.of("admin"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Cannot remove yourself"));
    verify(roleService, never()).update(any());
  }

  @Test
  void remove_otherAdminMemberIsAllowed() throws Exception {
    roleNamed("Admin");
    catalogUsers("Admin", "Editor");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Admin", "Operators", "Home", List.of("Admin", "Editor")));
    when(roleService.update(any(PSRole.class)))
        .thenReturn(stored("Admin", "Operators", "Home", List.of("Admin")));
    PSCurrentUser current = new PSCurrentUser();
    current.setName("Admin");
    when(userService.getCurrentUser()).thenReturn(current);
    Role input = new Role();
    input.setName("Admin");
    input.setUsers(List.of("Editor"));

    Role out = adaptor.removeRoleUser(null, input);

    assertEquals(List.of("Admin"), out.getUsers());
    verify(roleService).update(any(PSRole.class));
  }

  @Test
  void remove_blankRoleNameIs400() throws Exception {
    Role input = new Role();
    input.setName("  ");
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    verify(roleService, never()).update(any());
  }

  @Test
  void remove_missingRoleIs404() throws Exception {
    when(securityDesignWs.findRoles("Missing")).thenReturn(List.of());
    Role input = new Role();
    input.setName("Missing");
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(404, ex.getResponse().getStatus());
    verify(roleService, never()).update(any());
    verify(roleService, never()).create(any());
    verify(userService, never()).getUsers();
  }

  @Test
  void remove_nonAdminIs403() throws Exception {
    adaptor =
        new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> false, userService);
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Admin"));
    verify(roleService, never()).update(any());
    verify(securityDesignWs, never()).findRoles(any());
  }

  @Test
  void remove_updateStrandMessageIs409() throws Exception {
    roleNamed("Author");
    catalogUsers("Ada");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Author", "Keep me", "Home", List.of("Ada")));
    when(roleService.update(any(PSRole.class)))
        .thenThrow(
            new PSBeanValidationException(
                new PSRole(),
                "update",
                "Cannot remove yourself from \"Admin\" role.",
                null));
    Role input = new Role();
    input.setName("Author");
    input.setUsers(List.of("Ada"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.removeRoleUser(null, input));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private void roleNamed(String name) {
    IPSCatalogSummary summary = Mockito.mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn(name);
    when(securityDesignWs.findRoles(name)).thenReturn(List.of(summary));
  }

  private void catalogUsers(String... names) throws Exception {
    PSUserList list = new PSUserList();
    list.setUsers(List.of(names));
    when(userService.getUsers()).thenReturn(list);
  }

  private static PSRole stored(String name, String description, String homepage, List<String> users) {
    PSRole role = new PSRole();
    role.setName(name);
    role.setDescription(description);
    role.setHomepage(homepage);
    role.setUsers(users);
    return role;
  }
}
