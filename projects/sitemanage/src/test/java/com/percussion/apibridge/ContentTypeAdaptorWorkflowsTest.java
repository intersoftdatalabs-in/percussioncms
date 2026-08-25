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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSWorkflowInfo;
import com.percussion.rest.Guid;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * CD-08 allowed-workflow PUT requires a held design-session lock, validates workflow ids, and
 * persists without releasing the lock.
 */
@Tag("UnitTest")
class ContentTypeAdaptorWorkflowsTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private IPSWorkflowService workflowService;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;
  private PSWorkflow wf4;
  private PSWorkflow wf5;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    workflowService = mock(IPSWorkflowService.class);
    adaptor =
        new ContentTypeAdaptor(
            designWs, itemDefManager, systemDesign, () -> true, workflowService);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    wf4 = stubWorkflow(4, "Simple Workflow");
    wf5 = stubWorkflow(5, "Standard Workflow");
    when(workflowService.findWorkflow(any()))
        .thenAnswer(
            inv -> {
              IPSGuid g = inv.getArgument(0);
              if (g == null) {
                return Optional.empty();
              }
              if (g.getUUID() == 4) {
                return Optional.of(wf4);
              }
              if (g.getUUID() == 5) {
                return Optional.of(wf5);
              }
              return Optional.empty();
            });
    when(workflowService.findWorkflowsByName("Simple Workflow")).thenReturn(List.of(wf4));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void put_persistsWorkflowsWhenLockHeld() throws Exception {
    stubHeldLock();
    PSContentEditor editor = stubDefinition().getContentEditor();

    NamedObjectRef simple = namedWorkflow("Simple Workflow", 4);
    NamedObjectRef standard = namedWorkflow("Standard Workflow", 5);
    ContentTypeDetail out =
        adaptor.setAllowedWorkflows(null, "311", List.of(simple, standard), simple);

    assertEquals(2, out.getAllowedWorkflows().size());
    assertEquals("Simple Workflow", out.getDefaultWorkflow().getName());
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    ArgumentCaptor<PSWorkflowInfo> info = ArgumentCaptor.forClass(PSWorkflowInfo.class);
    verify(editor).setWorkflowInfo(info.capture());
    assertEquals(List.of(4, 5), workflowIds(info.getValue()));
    verify(editor).setWorkflowId(4);
    verify(systemDesign).isLocked(anyList(), eq("Admin"));
  }

  @Test
  void get_listsNewSetAfterPut() throws Exception {
    stubHeldLock();
    stubDefinition();

    NamedObjectRef simple = namedWorkflow("Simple Workflow", 4);
    ContentTypeDetail put = adaptor.setAllowedWorkflows(null, "311", List.of(simple), simple);
    assertEquals(1, put.getAllowedWorkflows().size());
    assertEquals("Simple Workflow", put.getAllowedWorkflows().get(0).getName());

    ContentTypeDetail get = adaptor.getContentType(null, "311");
    assertEquals(1, get.getAllowedWorkflows().size());
    assertEquals("Simple Workflow", get.getAllowedWorkflows().get(0).getName());
    assertEquals("Simple Workflow", get.getDefaultWorkflow().getName());
  }

  @Test
  void put_emptyListClearsAssociations() throws Exception {
    stubHeldLock();
    PSContentEditor editor = stubDefinition().getContentEditor();

    ContentTypeDetail out = adaptor.setAllowedWorkflows(null, "311", List.of(), null);

    ArgumentCaptor<PSWorkflowInfo> info = ArgumentCaptor.forClass(PSWorkflowInfo.class);
    verify(editor).setWorkflowInfo(info.capture());
    assertTrue(workflowIds(info.getValue()).isEmpty());
    assertTrue(out.getAllowedWorkflows().isEmpty());
  }

  @Test
  void put_rejectsUnknownWorkflowName() throws Exception {
    stubHeldLock();
    stubDefinition();
    when(workflowService.findWorkflowsByName("No Such Workflow")).thenReturn(List.of());

    NamedObjectRef missing = new NamedObjectRef();
    missing.setName("No Such Workflow");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.setAllowedWorkflows(null, "311", List.of(missing), null));
    assertTrue(ex.getMessage().contains("No Such Workflow"), ex.getMessage());
    verify(workflowService).findWorkflowsByName("No Such Workflow");
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_missingSessionIsIllegalStateNot403() {
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, null);
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, null);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> adaptor.setAllowedWorkflows(null, "311", List.of(), null));
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void put_rejectsUnknownWorkflowId() throws Exception {
    stubHeldLock();
    stubDefinition();

    NamedObjectRef missing = namedWorkflow("missing", 99);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.setAllowedWorkflows(null, "311", List.of(missing), null));
    assertTrue(ex.getMessage().contains("99"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));
    stubDefinition();

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () ->
                adaptor.setAllowedWorkflows(
                    null, "311", List.of(namedWorkflow("Simple Workflow", 4)), null));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubDefinition();

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () ->
                adaptor.setAllowedWorkflows(
                    null, "311", List.of(namedWorkflow("Simple Workflow", 4)), null));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockedInAnotherSession() throws Exception {
    stubHeldLock();
    stubDefinition();
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(
        guid, new PSLockErrorException(1, "LOCK_EXTENSION_INVALID_SESSION", "stack", "Admin", 12));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenThrow(errors);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () ->
                adaptor.setAllowedWorkflows(
                    null, "311", List.of(namedWorkflow("Simple Workflow", 4)), null));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.setAllowedWorkflows(null, "missing", List.of(), null));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false, workflowService);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.setAllowedWorkflows(null, "311", List.of(), null));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_wildcardName_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.setAllowedWorkflows(null, "perc*", List.of(), null));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private PSItemDefinition stubDefinition() throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    PSContentEditor editor = mock(PSContentEditor.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getLabel()).thenReturn("Page");
    when(def.getDescription()).thenReturn("desc");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_cePage");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_cePage/percPage.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getContentEditor()).thenReturn(editor);
    when(editor.getWorkflowId()).thenReturn(4);
    when(editor.getWorkflowInfo()).thenReturn(null);
    when(editor.getPipe()).thenReturn(null);
    doAnswer(
            inv -> {
              when(editor.getWorkflowId()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(editor)
        .setWorkflowId(anyInt());
    doAnswer(
            inv -> {
              when(editor.getWorkflowInfo()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(editor)
        .setWorkflowInfo(any());
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def;
  }

  private static NamedObjectRef namedWorkflow(String name, int uuid) {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName(name);
    Guid g = new Guid();
    g.setType((short) 23);
    g.setUuid(uuid);
    ref.setGuid(g);
    return ref;
  }

  private static PSWorkflow stubWorkflow(int uuid, String name) {
    PSWorkflow wf = mock(PSWorkflow.class);
    IPSGuid g = new PSGuid(PSTypeEnum.WORKFLOW, uuid);
    when(wf.getGUID()).thenReturn(g);
    when(wf.getName()).thenReturn(name);
    when(wf.getLabel()).thenReturn(name);
    return wf;
  }

  private static List<Integer> workflowIds(PSWorkflowInfo info) {
    List<Integer> ids = new ArrayList<>();
    if (info == null || info.getValues() == null) {
      return ids;
    }
    for (Iterator<Integer> it = info.getValues(); it.hasNext(); ) {
      ids.add(it.next());
    }
    return ids;
  }
}
