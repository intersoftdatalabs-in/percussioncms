/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.foldermanagement.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.foldermanagement.data.PSWorkflowAssignment;
import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.foldermanagement.service.IPSFolderService.PSWorkflowAssignmentInProgressException;
import com.percussion.foldermanagement.service.IPSFolderService.PSWorkflowNotFoundException;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("PSFolderRestService Error Exposure Prevention Tests")
class PSFolderRestServiceErrorExposureTest {

  private PSFolderRestService folderRestService;

  @Mock private IPSFolderService mockFolderService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    folderRestService = new PSFolderRestService(mockFolderService);
  }

  @Test
  @DisplayName("Should not expose PSWorkflowNotFoundException details in startGetAssociatedFoldersJob")
  void testStartGetAssociatedFoldersJobWorkflowNotFound() throws Exception {
    when(mockFolderService.startGetAssignedFoldersJob("wf", "path", false))
        .thenThrow(new PSWorkflowNotFoundException("Internal DB Error details"));

    try {
      folderRestService.startGetAssociatedFoldersJob("wf", "path", false);
      fail("Expected WebApplicationException");
    } catch (WebApplicationException e) {
      Response response = e.getResponse();
      assertEquals(404, response.getStatus());
      String entity = (String) response.getEntity();
      assertTrue(entity.contains("wf"));
      assertFalse(entity.contains("Internal DB Error details"));
    }
  }

  @Test
  @DisplayName("Should not expose PSWorkflowNotFoundException details to client in getAssociatedFolders")
  void testWorkflowNotFoundExceptionMessageHidden() throws Exception {
    when(mockFolderService.getAssignedFolders("wf", "/path", false))
        .thenThrow(new PSWorkflowNotFoundException("Secret SQL Exception details"));

    Response response = folderRestService.getAssociatedFolders("wf", "path", false);
    assertEquals(404, response.getStatus());
    String entity = (String) response.getEntity();
    assertFalse(entity.contains("Secret SQL Exception details"));
    assertTrue(entity.contains("error getting the associated folders"));
  }

  @Test
  @DisplayName("Should not expose IllegalArgumentException details to client")
  void testIllegalArgumentExceptionMessageHidden() throws Exception {
    when(mockFolderService.getAssignedFolders("wf", "/path", false))
        .thenThrow(new IllegalArgumentException("Null arguments rejected"));

    Response response = folderRestService.getAssociatedFolders("wf", "path", false);
    assertEquals(400, response.getStatus());
    String entity = (String) response.getEntity();
    assertFalse(entity.contains("Null arguments rejected"));
  }

  @Test
  @DisplayName("Should not expose PSPathNotFoundServiceException details to client")
  void testPathNotFoundExceptionMessageHidden() throws Exception {
    when(mockFolderService.getAssignedFolders("wf", "/path", false))
        .thenThrow(new PSPathNotFoundServiceException("Secret Path Details"));

    Response response = folderRestService.getAssociatedFolders("wf", "path", false);
    assertEquals(404, response.getStatus());
    String entity = (String) response.getEntity();
    assertFalse(entity.contains("Secret Path Details"));
  }

  @Test
  @DisplayName("Should not expose generic exception details to client")
  void testGenericExceptionMessageHidden() throws Exception {
    when(mockFolderService.getAssignedFolders("wf", "/path", false))
        .thenThrow(new RuntimeException("Critical Null Pointer"));

    Response response = folderRestService.getAssociatedFolders("wf", "path", false);
    assertEquals(500, response.getStatus());
    String entity = (String) response.getEntity();
    assertFalse(entity.contains("Critical Null Pointer"));
  }

  @Test
  @DisplayName("Should return generic message for in-progress assignment exception")
  void testWorkflowAssignmentInProgressExceptionHidden() throws Exception {
    PSWorkflowAssignment assignment = new PSWorkflowAssignment();
    assignment.setWorkflowName("wf");
    assignment.setAssignedFolders(new String[]{"folder"});

    doThrow(new PSWorkflowAssignmentInProgressException("Job 123 in progress"))
        .when(mockFolderService).assignFoldersToWorkflow(assignment);

    Response response = folderRestService.assignFoldersToWorkflow(assignment);
    assertEquals(409, response.getStatus());
    String entity = (String) response.getEntity();
    assertFalse(entity.contains("Job 123 in progress"));
    assertEquals("Workflow assignment is already in progress.", entity);
  }
}
