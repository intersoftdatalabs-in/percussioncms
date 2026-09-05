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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Guid;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.workflows.WorkflowContentTypesDesignLockException;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SY-06 workflow → content-type associations: Admin GET/PUT, path validation, lock acquire/release.
 */
@Tag("UnitTest")
class WorkflowsAdaptorAllowedContentTypesTest {

  private IPSContentDesignWs designWs;
  private IPSWorkflowService workflowService;
  private IPSContentMgr contentMgr;
  private WorkflowsAdaptor adaptor;
  private PSWorkflow simpleWf;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    workflowService = mock(IPSWorkflowService.class);
    contentMgr = mock(IPSContentMgr.class);
    adaptor = new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> true);
    simpleWf = stubWorkflow(4, "Simple Workflow");
    when(workflowService.findWorkflowsByName("Simple Workflow")).thenReturn(List.of(simpleWf));
    when(workflowService.findWorkflow(any()))
        .thenAnswer(
            inv -> {
              IPSGuid g = inv.getArgument(0);
              if (g != null && g.getUUID() == 4) {
                return Optional.of(simpleWf);
              }
              return Optional.empty();
            });
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void get_listsAssociatedContentTypes() throws Exception {
    IPSNodeDefinition page = stubNodeDef(311, "percPage", "Page");
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID())).thenReturn(List.of(page));

    List<NamedObjectRef> out = adaptor.getAllowedContentTypes(null, "Simple Workflow");
    assertEquals(1, out.size());
    assertEquals("percPage", out.get(0).getName());
    assertEquals("Page", out.get(0).getLabel());
  }

  @Test
  void get_emptyWhenNone() throws Exception {
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID())).thenReturn(List.of());
    assertTrue(adaptor.getAllowedContentTypes(null, "Simple Workflow").isEmpty());
  }

  @Test
  void get_unknownWorkflowReturnsNull() {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    assertNull(adaptor.getAllowedContentTypes(null, "missing"));
  }

  @Test
  void get_rejectsWildcard() {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.getAllowedContentTypes(null, "perc*"));
  }

  @Test
  void get_forbiddenWhenNotAdmin() {
    WorkflowsAdaptor denied =
        new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.getAllowedContentTypes(null, "Simple Workflow"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_addsAndRemovesAssociations() throws Exception {
    IPSNodeDefinition existing = stubNodeDef(311, "percPage", "Page");
    IPSNodeDefinition desired = stubNodeDef(312, "percBlog", "Blog");
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID()))
        .thenReturn(List.of(existing))
        .thenReturn(List.of(desired));

    IPSCatalogSummary blogSum = stubCtSummary(312, "percBlog");
    when(designWs.findContentTypes("percBlog")).thenReturn(List.of(blogSum));

    PSContentTypeWorkflow relOther = stubCtWf(5);
    when(designWs.loadAssociatedWorkflows(eq(existing.getGUID()), eq(true), eq(true)))
        .thenReturn(List.of(stubCtWf(4), relOther));
    when(designWs.loadAssociatedWorkflows(eq(desired.getGUID()), eq(true), eq(true)))
        .thenReturn(List.of());

    NamedObjectRef blog = new NamedObjectRef();
    blog.setName("percBlog");
    List<NamedObjectRef> out =
        adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of(blog));

    ArgumentCaptor<List<IPSGuid>> saveCaptor = ArgumentCaptor.forClass(List.class);
    verify(designWs)
        .saveAssociatedWorkflows(eq(existing.getGUID()), saveCaptor.capture(), eq(true));
    // removed workflow 4; kept 5
    assertEquals(1, saveCaptor.getValue().size());
    assertEquals(5, saveCaptor.getValue().get(0).getUUID());

    verify(designWs)
        .saveAssociatedWorkflows(eq(desired.getGUID()), anyList(), eq(true));
    assertEquals(1, out.size());
    assertEquals("percBlog", out.get(0).getName());
  }

  @Test
  void put_emptyClearsAll() throws Exception {
    IPSNodeDefinition existing = stubNodeDef(311, "percPage", "Page");
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID()))
        .thenReturn(List.of(existing))
        .thenReturn(List.of());
    when(designWs.loadAssociatedWorkflows(eq(existing.getGUID()), eq(true), eq(true)))
        .thenReturn(List.of(stubCtWf(4)));

    List<NamedObjectRef> out =
        adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of());
    assertTrue(out.isEmpty());
    verify(designWs).saveAssociatedWorkflows(eq(existing.getGUID()), eq(List.of()), eq(true));
  }

  @Test
  void put_unknownContentTypeIs400() throws Exception {
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID())).thenReturn(List.of());
    when(designWs.findContentTypes("nope")).thenReturn(List.of());
    NamedObjectRef missing = new NamedObjectRef();
    missing.setName("nope");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of(missing)));
    assertTrue(ex.getMessage().contains("content type not found"));
    verify(designWs, never()).saveAssociatedWorkflows(any(), anyList(), anyBoolean());
  }

  @Test
  void put_lockConflictOnLoad() throws Exception {
    IPSNodeDefinition existing = stubNodeDef(311, "percPage", "Page");
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID())).thenReturn(List.of(existing));
    when(designWs.loadAssociatedWorkflows(eq(existing.getGUID()), eq(true), eq(true)))
        .thenThrow(new PSErrorResultsException());

    assertThrows(
        WorkflowContentTypesDesignLockException.class,
        () -> adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of()));
  }

  @Test
  void put_lockConflictOnSave() throws Exception {
    IPSNodeDefinition existing = stubNodeDef(311, "percPage", "Page");
    IPSGuid ctGuid = existing.getGUID();
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID())).thenReturn(List.of(existing));
    when(designWs.loadAssociatedWorkflows(eq(ctGuid), eq(true), eq(true)))
        .thenReturn(List.of(stubCtWf(4)));
    doThrow(new PSErrorsException())
        .when(designWs)
        .saveAssociatedWorkflows(eq(ctGuid), anyList(), eq(true));

    assertThrows(
        WorkflowContentTypesDesignLockException.class,
        () -> adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of()));
  }

  @Test
  void put_requiresSessionUser() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    assertThrows(
        WebApplicationException.class,
        () -> adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of()));
  }

  @Test
  void put_resolvesContentTypeByGuid() throws Exception {
    when(contentMgr.findNodeDefinitionsByWorkflow(simpleWf.getGUID()))
        .thenReturn(List.of())
        .thenReturn(List.of(stubNodeDef(311, "percPage", "Page")));
    IPSCatalogSummary pageSum = stubCtSummary(311, "percPage");
    when(designWs.findContentTypes("*")).thenReturn(List.of(pageSum));
    when(designWs.loadAssociatedWorkflows(any(), eq(true), eq(true))).thenReturn(List.of());

    NamedObjectRef byGuid = new NamedObjectRef();
    Guid g = new Guid();
    g.setType(PSTypeEnum.NODEDEF.getOrdinal());
    g.setUuid(311);
    byGuid.setGuid(g);

    adaptor.setAllowedContentTypes(null, "Simple Workflow", List.of(byGuid));
    verify(designWs).saveAssociatedWorkflows(any(), anyList(), eq(true));
  }

  private static PSWorkflow stubWorkflow(int uuid, String name) {
    PSWorkflow wf = mock(PSWorkflow.class);
    IPSGuid g = new PSGuid(PSTypeEnum.WORKFLOW, uuid);
    when(wf.getGUID()).thenReturn(g);
    when(wf.getName()).thenReturn(name);
    when(wf.getLabel()).thenReturn(name);
    return wf;
  }

  private static IPSNodeDefinition stubNodeDef(int uuid, String name, String label) {
    IPSNodeDefinition def = mock(IPSNodeDefinition.class);
    IPSGuid g = new PSGuid(PSTypeEnum.NODEDEF, uuid);
    when(def.getGUID()).thenReturn(g);
    when(def.getName()).thenReturn(name);
    when(def.getLabel()).thenReturn(label);
    return def;
  }

  private static IPSCatalogSummary stubCtSummary(int uuid, String name) {
    PSObjectSummary sum = new PSObjectSummary(new PSGuid(PSTypeEnum.NODEDEF, uuid), name);
    sum.setLabel(name);
    return sum;
  }

  private static PSContentTypeWorkflow stubCtWf(int workflowUuid) {
    PSContentTypeWorkflow rel = new PSContentTypeWorkflow();
    rel.setWorkflowId(new PSGuid(PSTypeEnum.WORKFLOW, workflowUuid));
    return rel;
  }
}
