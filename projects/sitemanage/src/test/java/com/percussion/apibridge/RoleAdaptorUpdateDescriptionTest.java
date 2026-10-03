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
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Description update keeps members and home page, and maps 400 / 403 / 404. */
@Tag("UnitTest")
class RoleAdaptorUpdateDescriptionTest {

  private PSRoleService roleService;
  private IPSSecurityDesignWs securityDesignWs;
  private IPSWorkflowService workflowService;
  private RoleAdaptor adaptor;

  @BeforeEach
  void setUp() {
    roleService = Mockito.mock(PSRoleService.class);
    securityDesignWs = Mockito.mock(IPSSecurityDesignWs.class);
    workflowService = Mockito.mock(IPSWorkflowService.class);
    adaptor = new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> true);
  }

  @Test
  void update_changesDescriptionAndKeepsMembers() throws Exception {
    roleNamed("Author");
    PSRole existing = stored("Author", "Old", "Editor", List.of("alice", "bob"));
    when(roleService.find(any(PSStringWrapper.class))).thenReturn(existing);
    PSRole saved = stored("Author", "New desc", "Editor", List.of("alice", "bob"));
    when(roleService.update(any(PSRole.class))).thenReturn(saved);

    Role input = new Role();
    input.setName(" Author ");
    input.setDescription(" New desc ");
    input.setUsers(List.of("mallory"));
    input.setHomePage("Dashboard");

    Role out = adaptor.updateRole(null, input);

    assertEquals("Author", out.getName());
    assertEquals("New desc", out.getDescription());
    ArgumentCaptor<PSRole> sent = ArgumentCaptor.forClass(PSRole.class);
    verify(roleService).update(sent.capture());
    assertEquals("Author", sent.getValue().getName());
    assertEquals("New desc", sent.getValue().getDescription());
    assertEquals(List.of("alice", "bob"), sent.getValue().getUsers());
    assertEquals("Editor", sent.getValue().getHomepage());
    assertNull(sent.getValue().getOldName());
    verify(roleService, never()).create(any());
  }

  @Test
  void update_blankDescriptionClears() throws Exception {
    roleNamed("Author");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Author", "Old", "Home", List.of("alice")));
    when(roleService.update(any(PSRole.class)))
        .thenReturn(stored("Author", null, "Home", List.of("alice")));

    Role input = new Role();
    input.setName("Author");
    input.setDescription("   ");
    adaptor.updateRole(null, input);

    ArgumentCaptor<PSRole> sent = ArgumentCaptor.forClass(PSRole.class);
    verify(roleService).update(sent.capture());
    assertNull(sent.getValue().getDescription());
    assertEquals(List.of("alice"), sent.getValue().getUsers());
  }

  @Test
  void update_blankNameIs400() throws Exception {
    Role input = new Role();
    input.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    verify(roleService, never()).update(any());
    verify(roleService, never()).find(any());
  }

  @Test
  void update_descriptionTooLongIs400() throws Exception {
    Role input = new Role();
    input.setName("Author");
    input.setDescription("x".repeat(256));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("255"));
    verify(roleService, never()).update(any());
    verify(securityDesignWs, never()).findRoles(any());
  }

  @Test
  void update_missingRoleIs404() throws Exception {
    when(securityDesignWs.findRoles("Missing")).thenReturn(List.of());
    Role input = new Role();
    input.setName("Missing");
    input.setDescription("Nope");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(404, ex.getResponse().getStatus());
    verify(roleService, never()).update(any());
    verify(roleService, never()).create(any());
  }

  @Test
  void update_nonAdminIs403() throws Exception {
    adaptor = new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> false);
    Role input = new Role();
    input.setName("Author");
    input.setDescription("Nope");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Admin"));
    verify(roleService, never()).update(any());
    verify(securityDesignWs, never()).findRoles(any());
  }

  @Test
  void update_serviceRejectionIs400() throws Exception {
    roleNamed("System");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("System", "Old", "Home", List.of()));
    when(roleService.update(any(PSRole.class)))
        .thenThrow(
            new PSBeanValidationException(
                new PSRole(), "update", "Cannot update system role", null));

    Role input = new Role();
    input.setName("System");
    input.setDescription("Changed");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void update_notFoundFromServiceIs404() throws Exception {
    roleNamed("Author");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenThrow(
            new PSBeanValidationException(new PSRole(), "find", "Role not found Author", null));

    Role input = new Role();
    input.setName("Author");
    input.setDescription("Changed");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(404, ex.getResponse().getStatus());
    verify(roleService, never()).update(any());
  }

  @Test
  void update_otherDataServiceFailureIs500() throws Exception {
    roleNamed("Author");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenReturn(stored("Author", "Old", "Home", List.of("alice")));
    when(roleService.update(any(PSRole.class))).thenThrow(new PSDataServiceException("db down"));

    Role input = new Role();
    input.setName("Author");
    input.setDescription("Changed");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateRole(null, input));
    assertEquals(500, ex.getResponse().getStatus());
  }

  private void roleNamed(String name) {
    IPSCatalogSummary summary = Mockito.mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn(name);
    when(securityDesignWs.findRoles(name)).thenReturn(List.of(summary));
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
