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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Site folder workflow read/prepare/commit (#4892). */
@Tag("UnitTest")
class SiteFolderWorkflowAssociationTest {

  private IPSContentWs contentWs;
  private IPSFolderHelper folderHelper;
  private IPSIdMapper idMapper;
  private IPSWorkflowService workflowService;
  private SiteFolderWorkflowAssociation association;
  private IPSGuid folderGuid;

  @BeforeEach
  void setUp() {
    contentWs = mock(IPSContentWs.class);
    folderHelper = mock(IPSFolderHelper.class);
    idMapper = mock(IPSIdMapper.class);
    workflowService = mock(IPSWorkflowService.class);
    association =
        new SiteFolderWorkflowAssociation(contentWs, folderHelper, idMapper, workflowService);
    folderGuid = new PSGuid(PSTypeEnum.LEGACY_CONTENT, 9);
  }

  @Test
  void folderPath_prefersFolderRoot() {
    PSSite site = new PSSite();
    site.setName("Ignored");
    site.setFolderRoot("Sites/Corporate");
    assertEquals("//Sites/Corporate", SiteFolderWorkflowAssociation.folderPath(site));
  }

  @Test
  void prepare_unknownWorkflow_400_doesNotSave() throws Exception {
    PSSite site = site();
    when(workflowService.findWorkflowsByName("Missing WF")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> association.prepare(site, "Missing WF"));
    assertEquals(400, ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any());
  }

  @Test
  void prepare_missingFolder_404() throws Exception {
    PSSite site = site();
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName("Simple Workflow")).thenReturn(List.of(workflow));
    when(contentWs.getIdByPath("//Sites/NightlySite")).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> association.prepare(site, "Simple Workflow"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(folderHelper, never()).saveFolderProperties(any());
  }

  @Test
  void commit_writesWorkflowId() throws Exception {
    PSSite site = site();
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName("Simple Workflow")).thenReturn(List.of(workflow));
    when(contentWs.getIdByPath("//Sites/NightlySite")).thenReturn(folderGuid);
    when(idMapper.getString(folderGuid)).thenReturn("0-101-9");
    PSFolderProperties props = new PSFolderProperties();
    props.setId("0-101-9");
    props.setName("NightlySite");
    when(folderHelper.findFolderProperties("0-101-9")).thenReturn(props);

    SiteFolderWorkflowAssociation.Assignment assignment =
        association.prepare(site, "Simple Workflow");
    association.commit(assignment);

    ArgumentCaptor<PSFolderProperties> captor = ArgumentCaptor.forClass(PSFolderProperties.class);
    verify(folderHelper).saveFolderProperties(captor.capture());
    assertEquals(5, captor.getValue().getWorkflowId());
    assertEquals("Simple Workflow", assignment.workflowName());
  }

  @Test
  void readName_returnsLoadedWorkflow() throws Exception {
    PSSite site = site();
    when(contentWs.getIdByPath("//Sites/NightlySite")).thenReturn(folderGuid);
    when(idMapper.getString(folderGuid)).thenReturn("0-101-9");
    PSFolderProperties props = new PSFolderProperties();
    when(folderHelper.findFolderProperties("0-101-9")).thenReturn(props);
    when(folderHelper.getValidWorkflowId(props)).thenReturn(5);
    PSWorkflow workflow = workflow();
    when(workflowService.loadWorkflow(any())).thenReturn(workflow);
    assertEquals("Simple Workflow", association.readName(site));
  }

  @Test
  void readName_missingFolder_null() throws Exception {
    PSSite site = site();
    when(contentWs.getIdByPath("//Sites/NightlySite")).thenReturn(null);
    assertNull(association.readName(site));
  }

  private static PSSite site() {
    PSSite site = new PSSite();
    site.setName("NightlySite");
    return site;
  }

  private static PSWorkflow workflow() {
    PSWorkflow workflow = mock(PSWorkflow.class);
    when(workflow.getName()).thenReturn("Simple Workflow");
    when(workflow.getGUID()).thenReturn(new PSGuid(PSTypeEnum.WORKFLOW, 5));
    return workflow;
  }
}
