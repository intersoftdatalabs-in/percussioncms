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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.percussion.foldermanagement.data.PSWorkflowAssignment;
import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.foldermanagement.service.IPSFolderService.PSWorkflowAssignmentInProgressException;
import com.percussion.foldermanagement.service.IPSFolderService.PSWorkflowNotFoundException;
import com.percussion.share.dao.IPSGenericDao.LoadException;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Regression tests for PSFolderRestService that verify exception messages are not leaked back to the
 * HTTP client (CWE-209 / CodeQL {@code java/error-message-exposure}).
 *
 * <p>Follows the Constitution III fail-then-pass contract: on pre-fix code the response body is
 * built from {@code e.getMessage()}, so these assertions fail. On post-fix code the body is a
 * static generic message and the assertions pass.
 */
@DisplayName("PSFolderRestService Error Exposure Prevention Tests")
class PSFolderRestServiceErrorExposureTest {

  private static final String SENSITIVE_TOKEN = "internal-secret-path=/etc/shadow";

  private static final String LEAKY_WORKFLOW_NOT_FOUND =
      "Workflow not found at " + SENSITIVE_TOKEN;

  private static final String LEAKY_ILLEGAL_ARG =
      "Illegal argument: bad value at " + SENSITIVE_TOKEN;

  private static final String LEAKY_PATH_NOT_FOUND =
      "Path not found at " + SENSITIVE_TOKEN;

  private static final String LEAKY_WORKFLOW_IN_PROGRESS =
      "Workflow assignment in progress for site db1.internal.example.com " + SENSITIVE_TOKEN;

  private static final String LEAKY_RUNTIME =
      "Internal error connecting to db1.internal.example.com: " + SENSITIVE_TOKEN;

  private PSFolderRestService folderService;

  @Mock private IPSFolderService mockFolderService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    folderRestService = new PSFolderRestService(mockFolderService);
  }

  @Test
  @DisplayName("getAssociatedFolders must not leak PSWorkflowNotFoundException message")
  void testGetAssociatedFoldersDoesNotLeakWorkflowNotFound() throws Exception {
    doThrow(new PSWorkflowNotFoundException(LEAKY_WORKFLOW_NOT_FOUND))
        .when(mockFolderService)
        .getAssignedFolders(anyString(), anyString(), anyBoolean());

    var response = folderService.getAssociatedFolders("blog", "/sites/foo", false);

    assertNotNull(response);
    assertEquals(404, response.getStatus());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("getAssociatedFolders must not leak IllegalArgumentException message")
  void testGetAssociatedFoldersDoesNotLeakIllegalArgument() throws Exception {
    doThrow(new IllegalArgumentException(LEAKY_ILLEGAL_ARG))
        .when(mockFolderService)
        .getAssignedFolders(anyString(), anyString(), anyBoolean());

    var response = folderService.getAssociatedFolders("blog", "/sites/foo", false);

    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("getAssociatedFolders must not leak PSPathNotFoundServiceException message")
  void testGetAssociatedFoldersDoesNotLeakPathNotFound() throws Exception {
    doThrow(new PSPathNotFoundServiceException(LEAKY_PATH_NOT_FOUND))
        .when(mockFolderService)
        .getAssignedFolders(anyString(), anyString(), anyBoolean());

    var response = folderService.getAssociatedFolders("blog", "/sites/foo", false);

    assertNotNull(response);
    assertEquals(404, response.getStatus());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("getAssociatedFolders must not leak LoadException message")
  void testGetAssociatedFoldersDoesNotLeakLoadException() throws Exception {
    doThrow(new LoadException(LEAKY_PATH_NOT_FOUND))
        .when(mockFolderService)
        .getAssignedFolders(anyString(), anyString(), anyBoolean());

    var response = folderService.getAssociatedFolders("blog", "/sites/foo", false);

    assertNotNull(response);
    assertEquals(404, response.getStatus());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("getAssociatedFolders must not leak generic Exception message")
  void testGetAssociatedFoldersDoesNotLeakGenericException() throws Exception {
    doThrow(new RuntimeException(LEAKY_RUNTIME))
        .when(mockFolderService)
        .getAssignedFolders(anyString(), anyString(), anyBoolean());

    var response = folderService.getAssociatedFolders("blog", "/sites/foo", false);

    assertNotNull(response);
    assertEquals(500, response.getStatus());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("assignFoldersToWorkflow must not leak PSWorkflowAssignmentInProgressException")
  void testAssignFoldersDoesNotLeakAssignmentInProgress() throws Exception {
    var wa = new com.percussion.foldermanagement.data.PSWorkflowAssignment();
    wa.setWorkflowName("blog");
    wa.setAssignedFolders(new String[] {"/sites/foo"});
    doThrow(new PSWorkflowAssignmentInProgressException(LEAKY_WORKFLOW_IN_PROGRESS))
        .when(mockFolderService)
        .assignFoldersToWorkflow(org.mockito.ArgumentMatchers.any());

    var response = folderService.assignFoldersToWorkflow(wa);

    assertNotNull(response);
    assertEquals(409, response.getStatus());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("assignFoldersToWorkflow success path returns 204 with no body")
  void testAssignFoldersSuccessIsUnchanged() throws Exception {
    var wa = new com.percussion.foldermanagement.data.PSWorkflowAssignment();
    wa.setWorkflowName("blog");
    wa.setAssignedFolders(new String[] {"/sites/foo"});
    doNothing().when(mockFolderService).assignFoldersToWorkflow(org.mockito.ArgumentMatchers.any());

    var response = folderService.assignFoldersToWorkflow(wa);

    assertNotNull(response);
    assertEquals(204, response.getStatus());
  }

  @Test
  @DisplayName("getAssociatedFolders success path returns 200")
  void testGetAssociatedFoldersSuccessIsUnchanged() throws Exception {
    when(mockFolderService.getAssignedFolders(anyString(), anyString(), anyBoolean()))
        .thenReturn(java.util.List.of());

    var response = folderService.getAssociatedFolders("blog", "/sites/foo", false);

    assertNotNull(response);
    assertEquals(200, response.getStatus());
  }

  private static String entityAsString(Response response) {
    Object entity = response.getEntity();
    if (entity == null) {
      return "";
    }
    return entity.toString();
  }

  private static void assertNoSensitiveLeak(String responseBody) {
    assertFalse(
        responseBody.contains(SENSITIVE_TOKEN),
        "Response body must not contain sensitive exception details; was: " + responseBody);
  }
}