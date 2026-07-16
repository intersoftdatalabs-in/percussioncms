/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.designmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFileOperationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Regression tests for PSWebResourcesRestService that verify exception messages thrown by the
 * underlying file system service are not leaked back to the HTTP client (CWE-209 / CodeQL
 * {@code java/error-message-exposure}).
 *
 * <p>These tests follow the Constitution III fail-then-pass contract: on the pre-fix code they
 * assert that the response entity does NOT contain the sensitive exception message, so they fail
 * (because the original code embeds {@code e.getMessage()} in the response body). After the
 * sanitization fix lands they pass.
 */
@DisplayName("PSWebResourcesRestService Error Exposure Prevention Tests")
class PSWebResourcesRestServiceErrorExposureTest {

  /** Sensitive substring that must NEVER appear in any HTTP response entity. */
  private static final String SENSITIVE_TOKEN = "internal-secret-path=/etc/shadow";

  private static final String LEAKY_FILE_OP_MESSAGE =
      "Operation failed: " + SENSITIVE_TOKEN + " (db host db1.internal.example.com)";

  private static final String LEAKY_FILE_ALREADY_EXISTS_MESSAGE =
      "File exists at " + SENSITIVE_TOKEN;

  private static final String LEAKY_FILE_NAME_IN_USE_MESSAGE =
      "Folder name conflict at " + SENSITIVE_TOKEN;

  private static final String LEAKY_RESERVED_NAME_MESSAGE =
      "Reserved name rejected at " + SENSITIVE_TOKEN;

  @Mock private IPSFileSystemService fileSystemService;

  @Mock private IPSUserService userService;

  private PSWebResourcesRestService webResourcesService;

  @BeforeEach
  void setUp() throws PSDataServiceException {
    MockitoAnnotations.openMocks(this);
    webResourcesService = new PSWebResourcesRestService(fileSystemService, userService);
    var adminUser = new PSCurrentUser();
    adminUser.setAdminUser(true);
    adminUser.setDesignerUser(false);
    when(userService.getCurrentUser()).thenReturn(adminUser);
  }

  @Test
  @DisplayName("validateFileUpload must not leak PSFileAlreadyExistsException message")
  void testValidateFileUploadDoesNotLeakAlreadyExists() throws PSFileOperationException {
    doThrow(new PSFileOperationException(LEAKY_FILE_ALREADY_EXISTS_MESSAGE))
        .when(fileSystemService)
        .validateFileUpload(anyString());

    var response = webResourcesService.validateFileUpload("themes/site.css");

    assertNotNull(response);
    assertNotNull(response.getEntity(), "Response entity must not be null");
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("validateFileUpload must not leak PSFileNameInUseByFolderException message")
  void testValidateFileUploadDoesNotLeakNameInUse() throws PSFileOperationException {
    doThrow(new IPSFileSystemService.PSFileNameInUseByFolderException(
            LEAKY_FILE_NAME_IN_USE_MESSAGE))
        .when(fileSystemService)
        .validateFileUpload(anyString());

    var response = webResourcesService.validateFileUpload("themes/site.css");

    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("validateFileUpload must not leak PSReservedFileNameException message")
  void testValidateFileUploadDoesNotLeakReservedName() throws PSFileOperationException {
    doThrow(new IPSFileSystemService.PSReservedFileNameException(LEAKY_RESERVED_NAME_MESSAGE))
        .when(fileSystemService)
        .validateFileUpload(anyString());

    var response = webResourcesService.validateFileUpload("themes/site.css");

    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("validateFileUpload must not leak generic PSFileOperationException message")
  void testValidateFileUploadDoesNotLeakGenericFileOperation() throws PSFileOperationException {
    doThrow(new PSFileOperationException(LEAKY_FILE_OP_MESSAGE))
        .when(fileSystemService)
        .validateFileUpload(anyString());

    var response = webResourcesService.validateFileUpload("themes/site.css");

    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("deleteFile must not leak PSFileOperationException message in 500 response body")
  void testDeleteFileDoesNotLeakFileOperation() throws PSFileOperationException {
    doThrow(new PSFileOperationException(LEAKY_FILE_OP_MESSAGE))
        .when(fileSystemService)
        .deleteFile(anyString());

    var response = webResourcesService.deleteFile("themes/site.css");

    assertNotNull(response);
    assertEquals(500, response.getStatus(), "deleteFile failures should map to HTTP 500");
    assertNotNull(response.getEntity());
    assertNoSensitiveLeak(entityAsString(response));
  }

  @Test
  @DisplayName("deleteFile success path still returns HTTP 200")
  void testDeleteFileSuccessIsUnchanged() throws PSFileOperationException {
    doNothing().when(fileSystemService).deleteFile(anyString());

    var response = webResourcesService.deleteFile("themes/site.css");

    assertNotNull(response);
    assertEquals(200, response.getStatus());
  }

  @Test
  @DisplayName("validateFileUpload success path still returns the success token")
  void testValidateFileUploadSuccessIsUnchanged() throws PSFileOperationException {
    doNothing().when(fileSystemService).validateFileUpload(anyString());

    var response = webResourcesService.validateFileUpload("themes/site.css");

    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals("success", response.getEntity());
  }

  private static String entityAsString(Response response) {
    Object entity = response.getEntity();
    if (entity == null) {
      return "";
    }
    return entity.toString();
  }

  /**
   * Asserts that the response body does not contain the sensitive token. This is the fail-then-pass
   * assertion: on pre-fix code the response body is built from {@code e.getMessage()} and includes
   * the token, so this assertion fails. On post-fix code the response body is a generic message
   * that does not include the token, so this assertion passes.
   */
  private static void assertNoSensitiveLeak(String responseBody) {
    assertFalse(
        responseBody.contains(SENSITIVE_TOKEN),
        "Response body must not contain sensitive exception details; was: " + responseBody);
  }
}
