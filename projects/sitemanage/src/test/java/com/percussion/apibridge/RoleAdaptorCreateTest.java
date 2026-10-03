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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.errors.BackendException;
import com.percussion.rest.roles.Role;
import com.percussion.role.data.PSRole;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Create uses role service create, requires Admin, and maps validation to 400. */
@Tag("UnitTest")
class RoleAdaptorCreateTest {

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
  void create_delegatesToRoleServiceCreate() throws Exception {
    PSRole saved = new PSRole();
    saved.setName("NightRole");
    saved.setDescription("Editors");
    when(roleService.create(any(PSRole.class))).thenReturn(saved);

    Role input = new Role();
    input.setName(" NightRole ");
    input.setDescription(" Editors ");

    Role out = adaptor.createRole(null, input);

    assertEquals("NightRole", out.getName());
    assertEquals("Editors", out.getDescription());
    assertEquals("NightRole", input.getName());
    verify(roleService).create(any(PSRole.class));
    verify(roleService, never()).update(any());
  }

  @Test
  void create_blankNameIs400() throws Exception {
    Role input = new Role();
    input.setName("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createRole(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    verify(roleService, never()).create(any());
  }

  @Test
  void create_nonAdminIs403() throws Exception {
    adaptor = new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> false);
    Role input = new Role();
    input.setName("NightRole");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createRole(null, input));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Admin"));
    verify(roleService, never()).create(any());
  }

  @Test
  void create_validationIs400() throws Exception {
    when(roleService.create(any(PSRole.class)))
        .thenThrow(new PSBeanValidationException(new PSRole(), "create"));
    Role input = new Role();
    input.setName("Author");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createRole(null, input));
    assertEquals(400, ex.getResponse().getStatus());
    verify(roleService, never()).update(any());
  }

  @Test
  void create_otherDataServiceFailureIsBackendException() throws Exception {
    when(roleService.create(any(PSRole.class))).thenThrow(new PSDataServiceException("db down"));
    Role input = new Role();
    input.setName("NightRole");
    assertThrows(BackendException.class, () -> adaptor.createRole(null, input));
  }

  @Test
  void roleExists_matchesExactNameOnly() {
    IPSCatalogSummary other = Mockito.mock(IPSCatalogSummary.class);
    when(other.getName()).thenReturn("NightRoleExtra");
    IPSCatalogSummary exact = Mockito.mock(IPSCatalogSummary.class);
    when(exact.getName()).thenReturn("NightRole");
    when(securityDesignWs.findRoles("NightRole")).thenReturn(List.of(other, exact));

    assertTrue(adaptor.roleExists(null, " NightRole "));
    assertFalse(adaptor.roleExists(null, "Missing"));
    assertFalse(adaptor.roleExists(null, "  "));
  }
}
