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

import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFileAlreadyExistsException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFileNameInUseByFolderException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSFileOperationException;
import com.percussion.designmanagement.service.IPSFileSystemService.PSReservedFileNameException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.service.IPSUserService;
import com.percussion.util.PSCharSets;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * This REST Service handles the requests under "Design/webresources". Here we have the
 * corresponding methods for download a file, upload one, delete files, etc. This class has the
 * control for the user roles, accoding to the configured roles.
 *
 * @author miltonpividori
 */
@Path("/webresources")
@Component("webResourcesRestService")
public class PSWebResourcesRestService {

  private static final Logger log = LogManager.getLogger(PSWebResourcesRestService.class);

  private static final String VALIDATE_SUCCESS = "success";

  private static final String UPLOAD_THEME_FILE_PATH = "form-data; name=\"upload-theme-file-path\"";

  // Generic client-facing error messages used to avoid leaking internal exception details
  // (CWE-209 / CodeQL java/error-message-exposure). The detailed exception is always logged
  // server-side via PSExceptionUtils.getMessageForLog before this generic message is returned.
  private static final String GENERIC_INVALID_FILE_PATH = "Invalid file path";

  private static final String GENERIC_FILE_ALREADY_EXISTS = "A file with the same name already exists";

  private static final String GENERIC_FILE_NAME_CONFLICT = "The file name conflicts with an existing folder or reserved name";

  private static final String GENERIC_FILE_OPERATION_ERROR = "An error occurred while processing the file";

  private IPSFileSystemService fileSystemService;
  private IPSUserService userService;

  @Autowired
  public PSWebResourcesRestService(
      @Qualifier("webResourcesService") IPSFileSystemService webResourcesService,
      IPSUserService userService) {
    this.fileSystemService = webResourcesService;
    this.userService = userService;
  }

  /**
   * Handles the download of a file. Forces the browser to show the download dialog, instead of
   * trying to open it in a different browser window or tab.
   *
   * @param path the path of the file the user wants to download.
   * @return The response object that contains the requested file.
   */
  @GET
  @Path("/{path:.*}")
  @Produces("application/octect-stream")
  public Response fileDownload(@PathParam("path") String path) {
    try {
      if (!checkUserPermission()) {
        return buildForbiddenResponse();
      }
      var itemContent = fileSystemService.getFile(path);
      if (!itemContent.exists() || itemContent.isDirectory()) { // codeql[java/path-injection]
        return Response.status(Status.NOT_FOUND).build();
      }
      return Response.ok(itemContent)
          .header("Content-Disposition", "attachment; ")
          .header("Content-Length", itemContent.length())
          .build();
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Handles the deletion of a file, using the filesystem service, and returns the response
   * accordingly.
   *
   * @param path the path to the file the user wants to remove. Cannot be <code>null</code>
   * @return The response object. An ok response if everything went well, or a Server Error if
   *     anything happened.
   */
  @DELETE
  @Path("/{path:.*}")
  @Produces("application/octect-stream")
  public Response deleteFile(@PathParam("path") String path) {
    try {
      if (!checkUserPermission()) {
        return buildForbiddenResponse();
      }
      try {
        fileSystemService.deleteFile(path);
        return Response.ok().build();
      } catch (PSFileOperationException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        return Response.serverError().entity(GENERIC_FILE_OPERATION_ERROR).build();
      }
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Handles the upload of a file, which is made from a POST request. It uses the file system
   * service to accomplish that. NOTE: Setting @Produces("text/html") fixes IE problems interpreting
   * the Content-type of the response, and thus not firing a "load" event when.
   *
   * @param multipartBody the multipart object used to get the stream that corresponds with the file
   *     content. This method requires that the path come in an hidden input field, named
   *     'upload-theme-file-path'.
   * @return the Response object. An ok response if everything went well, or a http error code of
   *     <code>409</code> if an error took place.
   */
  @POST
  @Path("/uploadFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_HTML)
  public Response uploadFile(MultipartBody multipartBody) {
    try {
      if (!checkUserPermission()) {
        return buildForbiddenResponse();
      }
      var response = "";
      try {
        var attachments = multipartBody.getAllAttachments();
        if (attachments == null || attachments.isEmpty()) {
          return Response.ok().entity("An error occurred when uploading the file.").build();
        }
        String path = "";
        InputStream pageContent = null;
        for (var attachment : attachments) {
          if (UPLOAD_THEME_FILE_PATH.equals(attachment.getHeader("content-disposition"))) {
            path =
                IOUtils.toString(
                    attachment.getDataHandler().getInputStream(), PSCharSets.rxJavaEnc());
            path = getDecodedPath(path);
          } else {
            pageContent = attachment.getDataHandler().getInputStream();
          }
        }
        if (StringUtils.isBlank(path)) {
          return Response.ok().entity("An error occurred when uploading the file.").build();
        }
        fileSystemService.fileUpload(path, pageContent);
      } catch (SecurityException e) {
        log.warn("Security violation detected during file upload: {}", e.getMessage());
        return Response.status(Status.BAD_REQUEST).entity(GENERIC_INVALID_FILE_PATH).build();
      } catch (PSFileOperationException | IOException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        response = GENERIC_FILE_OPERATION_ERROR;
        return Response.ok().entity(response).build();
      }
      return Response.ok().entity(response).build();
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Handles the validation request for a file in the given path. It uses the file system service
   * validation method.
   *
   * @param path the path of the file name that needs validation. The file may already exist on the
   *     filesystem or not. Can not be <code>null</code>
   * @return the Response object. An ok response if everything went well, or a http error code of
   *     <code>409</code> if an error took place.
   */
  @GET
  @Path("/validateFileUpload/{path:.*}")
  @Produces("application/octect-stream")
  public Response validateFileUpload(@PathParam("path") String path) {
    try {
      if (!checkUserPermission()) {
        return buildForbiddenResponse();
      }
      var response = "";
      try {
        var decodedPath = getDecodedPath(path);
        fileSystemService.validateFileUpload(decodedPath);
      } catch (SecurityException e) {
        log.warn("Security violation detected during file validation: {}", e.getMessage());
        return Response.status(Status.BAD_REQUEST).entity(GENERIC_INVALID_FILE_PATH).build();
      } catch (PSFileAlreadyExistsException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        response = GENERIC_FILE_ALREADY_EXISTS;
        return Response.ok().entity(response).build();
      } catch (PSFileNameInUseByFolderException | PSReservedFileNameException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        response = GENERIC_FILE_NAME_CONFLICT;
        return Response.status(Status.CONFLICT).entity(response).build();
      } catch (PSFileOperationException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        response = GENERIC_FILE_OPERATION_ERROR;
        return Response.status(Status.CONFLICT).entity(response).build();
      }
      return Response.ok().entity(VALIDATE_SUCCESS).build();
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Calls {@link URLDecoder#decode(String, String)} for the given path, using the encoding . If
   * that encoding is not supported (cannot happen), it calls {@link URLDecoder#decode(String)}
   * (that is deprecated).
   *
   * <p>Security: This method decodes the path and validates it against path traversal attacks
   * (CWE-22). The path must be relative and cannot contain sequences that would escape the web
   * resources directory.
   *
   * @param path the encoded Path. Assumed not blank.
   * @return a {@link String}. Never <code>null</code>
   * @throws SecurityException if the path contains traversal attacks
   */
  private String getDecodedPath(String path) throws SecurityException {
    try {
      var decodedPath = URLDecoder.decode(path, PSCharSets.rxJavaEnc());
      return validatePath(decodedPath);
    } catch (UnsupportedEncodingException e1) {
      // charset provided by PSCharSets should always be available; fallback
      // to UTF-8 using the Charset overload.  Guard the fall-back in its own
      // try/catch in case the compiler binds to the legacy String overload.
      try {
        var decodedPath = URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8.name());
        return validatePath(decodedPath);
      } catch (UnsupportedEncodingException e2) {
        // impossibility, rethrow as runtime
        throw new RuntimeException(e2);
      }
    }
  }

  /**
   * Validates the provided file path to prevent path traversal attacks (CWE-22).
   *
   * <p>Security Checks:
   *
   * <ul>
   *   <li>Path must not be absolute (cannot start with / or drive letter)
   *   <li>Path must not contain .. sequences (path traversal)
   *   <li>Path must not contain null bytes
   *   <li>Path is normalized to canonical form before validation
   * </ul>
   *
   * @param path the path to validate
   * @return the validated path
   * @throws SecurityException if the path contains traversal attacks or invalid characters
   */
  private String validatePath(String path) throws SecurityException {
    if (StringUtils.isBlank(path)) {
      throw new SecurityException("Path cannot be null or empty");
    }

    // Prevent null byte injection
    if (path.contains("\0") || path.contains("%00")) {
      log.warn("Null byte injection attempt detected in path: {}", path);
      throw new SecurityException("Invalid characters in path");
    }

    // Prevent absolute paths
    if (path.startsWith("/") || (path.length() > 2 && path.charAt(1) == ':')) {
      log.warn("Absolute path traversal attempt detected: {}", path);
      throw new SecurityException("Absolute paths are not allowed");
    }

    // Normalize the path using Java NIO
    try {
      java.nio.file.Path normalizedPath = Paths.get(path).normalize();
      String normalizedStr = normalizedPath.toString();

      // Prevent .. sequences even after normalization
      if (normalizedStr.contains("..")) {
        log.warn("Path traversal attempt detected: {}", path);
        throw new SecurityException("Path traversal is not allowed");
      }

      // Additional check: ensure normalized path doesn't go above root
      if (normalizedPath.isAbsolute()) {
        log.warn("Normalization resulted in absolute path: {}", path);
        throw new SecurityException("Invalid path");
      }

      return normalizedStr;
    } catch (SecurityException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Exception during path validation: {}", e.getMessage());
      throw new SecurityException("Invalid path: " + e.getMessage());
    }
  }

  /**
   * Checks if the current user is in the Admin or Designer role.
   *
   * @return <code>true</code> if the user has the Admin role. <code>false</code> otherwise.
   */
  private boolean checkUserPermission() throws PSDataServiceException {
    var user = userService.getCurrentUser();
    return user.isAdminUser() || user.isDesignerUser();
  }

  /**
   * Builds a 403 HTTP response to be returned when the user is not authorized to access a given
   * file operation.
   *
   * @return A <code>Response</code> object with the 403 HTTP code.
   */
  private Response buildForbiddenResponse() {
    return Response.status(Status.FORBIDDEN)
        .entity("You are not authorized to access this operation.")
        .build();
  }
}
