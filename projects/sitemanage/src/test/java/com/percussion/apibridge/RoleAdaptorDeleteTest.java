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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Mockito;

/** Delete requires Admin, refuses system roles, and maps in-use validation to 409. */
@Tag("UnitTest")
class RoleAdaptorDeleteTest {

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
  void delete_delegatesAfterValidate() throws Exception {
    roleNamed("NightRole");
    when(roleService.find(any(PSStringWrapper.class))).thenReturn(stored("NightRole"));

    adaptor.deleteRole(null, " NightRole ");

    verify(roleService).validateForDelete(any(PSRole.class));
    verify(roleService).delete(any(PSStringWrapper.class));
  }

  @Test
  void delete_blankNameIs400() throws Exception {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, "  "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(roleService, never()).delete(any());
    verify(securityDesignWs, never()).findRoles(any());
  }

  @Test
  void delete_systemRoleIs400() throws Exception {
    for (String name : List.of("System", "default", "DEFAULT")) {
      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, name));
      assertEquals(400, ex.getResponse().getStatus());
      assertTrue(ex.getMessage().toLowerCase().contains("system"));
    }
    verify(roleService, never()).delete(any());
    verify(roleService, never()).validateForDelete(any());
  }

  @Test
  void delete_nonAdminIs403() throws Exception {
    adaptor = new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, "Author"));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Admin"));
    verify(roleService, never()).delete(any());
  }

  @Test
  void delete_missingRoleIs404() throws Exception {
    when(securityDesignWs.findRoles("Missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, "Missing"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(roleService, never()).delete(any());
    verify(roleService, never()).validateForDelete(any());
  }

  @Test
  void delete_inUseIs409AndDoesNotDelete() throws Exception {
    roleNamed("Author");
    when(roleService.find(any(PSStringWrapper.class))).thenReturn(stored("Author"));
    doThrow(
            new PSBeanValidationException(
                new PSRole(),
                "validateForDelete",
                "Role 'Author' is used by the following workflows: 'Simple'.",
                null))
        .when(roleService)
        .validateForDelete(any(PSRole.class));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, "Author"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(roleService, never()).delete(any());
  }

  @Test
  void delete_notFoundFromServiceIs404() throws Exception {
    roleNamed("Author");
    when(roleService.find(any(PSStringWrapper.class)))
        .thenThrow(
            new PSBeanValidationException(new PSRole(), "find", "Role not found Author", null));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, "Author"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(roleService, never()).delete(any());
  }

  @Test
  void delete_otherDataServiceFailureIs500() throws Exception {
    roleNamed("Author");
    when(roleService.find(any(PSStringWrapper.class))).thenReturn(stored("Author"));
    doThrow(new PSDataServiceException("db down"))
        .when(roleService)
        .validateForDelete(any(PSRole.class));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteRole(null, "Author"));
    assertEquals(500, ex.getResponse().getStatus());
    verify(roleService, never()).delete(any());
  }

  private void roleNamed(String name) {
    IPSCatalogSummary summary = Mockito.mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn(name);
    when(securityDesignWs.findRoles(name)).thenReturn(List.of(summary));
  }

  private static PSRole stored(String name) {
    PSRole role = new PSRole();
    role.setName(name);
    return role;
  }
}
