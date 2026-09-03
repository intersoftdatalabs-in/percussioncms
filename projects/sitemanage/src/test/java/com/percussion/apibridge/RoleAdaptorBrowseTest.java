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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.rest.roles.RoleBrowseCatalog;
import com.percussion.rest.roles.RoleBrowseEntry;
import com.percussion.rest.roles.RoleBrowseGroup;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * SE-03 Admin roles browse: community / workflow / unassigned grouping. Non-Admin is 403.
 */
@Tag("UnitTest")
class RoleAdaptorBrowseTest {

  private PSRoleService roleService;
  private IPSSecurityDesignWs securityDesignWs;
  private IPSWorkflowService workflowService;
  private RoleAdaptor adaptor;

  @BeforeEach
  void setUp() {
    roleService = mock(PSRoleService.class);
    securityDesignWs = mock(IPSSecurityDesignWs.class);
    workflowService = mock(IPSWorkflowService.class);
    adaptor = new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> true);
  }

  @Test
  void browse_groupsCommunityWorkflowAndUnassigned() throws Exception {
    IPSGuid authorGuid = guid(101);
    IPSGuid editorGuid = guid(102);
    IPSGuid orphanGuid = guid(103);
    IPSCatalogSummary authorSum = roleSummary("Author", "Authors", authorGuid);
    IPSCatalogSummary editorSum = roleSummary("Editor", "Editors", editorGuid);
    IPSCatalogSummary orphanSum = roleSummary("Orphan", null, orphanGuid);
    when(securityDesignWs.findRoles(isNull()))
        .thenReturn(List.of(authorSum, editorSum, orphanSum));

    IPSGuid communityGuid = guid(201);
    IPSCatalogSummary communitySum = communitySummary("Default", communityGuid);
    when(securityDesignWs.findCommunities(isNull())).thenReturn(List.of(communitySum));
    PSCommunity community = mock(PSCommunity.class);
    when(community.getName()).thenReturn("Default");
    when(community.getGUID()).thenReturn(communityGuid);
    when(community.getRoleAssociations()).thenReturn(List.of(authorGuid));
    when(securityDesignWs.loadCommunities(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(community));

    PSWorkflowRole wfAuthor = mock(PSWorkflowRole.class);
    when(wfAuthor.getName()).thenReturn("Author");
    PSWorkflowRole wfEditor = mock(PSWorkflowRole.class);
    when(wfEditor.getName()).thenReturn("Editor");
    PSWorkflow simple = mock(PSWorkflow.class);
    when(simple.getName()).thenReturn("Simple Workflow");
    when(simple.getRoles()).thenReturn(List.of(wfAuthor, wfEditor));
    PSWorkflow local = mock(PSWorkflow.class);
    when(local.getName()).thenReturn(PSWorkflowHelper.LOCAL_WORKFLOW_NAME);
    when(workflowService.findWorkflowsByName("")).thenReturn(List.of(simple, local));

    RoleBrowseCatalog catalog = adaptor.browseRoles(null, null);
    assertNull(catalog.getGroup());
    assertEquals(3, catalog.getRoles().size());

    RoleBrowseEntry author = byName(catalog, "Author");
    assertEquals(
        List.of(
            RoleBrowseGroup.COMMUNITY.getWireValue(), RoleBrowseGroup.WORKFLOW.getWireValue()),
        author.getGroups());
    assertEquals(List.of("Default"), author.getCommunities());
    assertEquals(List.of("Simple Workflow"), author.getWorkflows());
    assertEquals("Authors", author.getDescription());

    RoleBrowseEntry editor = byName(catalog, "Editor");
    assertEquals(List.of(RoleBrowseGroup.WORKFLOW.getWireValue()), editor.getGroups());
    assertTrue(editor.getCommunities().isEmpty());
    assertEquals(List.of("Simple Workflow"), editor.getWorkflows());

    RoleBrowseEntry orphan = byName(catalog, "Orphan");
    assertEquals(List.of(RoleBrowseGroup.UNASSIGNED.getWireValue()), orphan.getGroups());
    assertTrue(orphan.getCommunities().isEmpty());
    assertTrue(orphan.getWorkflows().isEmpty());
  }

  @Test
  void browse_filtersByGroup() throws Exception {
    IPSGuid orphanGuid = guid(103);
    IPSCatalogSummary orphanSum = roleSummary("Orphan", null, orphanGuid);
    when(securityDesignWs.findRoles(isNull())).thenReturn(List.of(orphanSum));
    when(securityDesignWs.findCommunities(isNull())).thenReturn(List.of());
    when(workflowService.findWorkflowsByName("")).thenReturn(List.of());

    RoleBrowseCatalog unassigned = adaptor.browseRoles(null, "unassigned");
    assertEquals("unassigned", unassigned.getGroup());
    assertEquals(1, unassigned.getRoles().size());

    RoleBrowseCatalog community = adaptor.browseRoles(null, "community");
    assertEquals("community", community.getGroup());
    assertTrue(community.getRoles().isEmpty());
  }

  @Test
  void browse_invalidGroupIsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.browseRoles(null, "nope"));
  }

  @Test
  void browse_nonAdminIs403() {
    adaptor = new RoleAdaptor(roleService, securityDesignWs, workflowService, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.browseRoles(null, null));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Admin"));
  }

  @Test
  void browse_emptyCatalogIs200Shape() throws Exception {
    when(securityDesignWs.findRoles(isNull())).thenReturn(List.of());
    when(securityDesignWs.findCommunities(isNull())).thenReturn(List.of());
    when(workflowService.findWorkflowsByName("")).thenReturn(List.of());

    RoleBrowseCatalog catalog = adaptor.browseRoles(null, null);
    assertTrue(catalog.getRoles().isEmpty());
    assertFalse(catalog.getRoles() == null);
  }

  private static RoleBrowseEntry byName(RoleBrowseCatalog catalog, String name) {
    return catalog.getRoles().stream()
        .filter(e -> name.equals(e.getName()))
        .findFirst()
        .orElseThrow();
  }

  private static IPSCatalogSummary roleSummary(String name, String description, IPSGuid guid) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn(name);
    when(sum.getDescription()).thenReturn(description);
    when(sum.getGUID()).thenReturn(guid);
    return sum;
  }

  private static IPSCatalogSummary communitySummary(String name, IPSGuid guid) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn(name);
    when(sum.getGUID()).thenReturn(guid);
    return sum;
  }

  private static IPSGuid guid(long value) {
    IPSGuid g = mock(IPSGuid.class);
    when(g.longValue()).thenReturn(value);
    return g;
  }
}
