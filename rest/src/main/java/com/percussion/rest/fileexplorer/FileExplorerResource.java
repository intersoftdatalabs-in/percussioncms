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

package com.percussion.rest.fileexplorer;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * File Explorer browse catalog (Workbench §12.1).
 *
 * <p>Admin-only list of configured allow-listed roots and children by relative path. Distinct from
 * {@code /serverconfigs} (SY-02) and application CMS/resource files (SY-05). Parent traversal,
 * absolute/drive/UNC paths, and unknown roots are 400/404 without echoing the raw path. Write and
 * upload are out of scope for this surface.
 */
@PSSiteManageBean(value = "restFileExplorerResource")
@Path("/fileexplorer")
@XmlRootElement
@Tag(
    name = "File Explorer",
    description = "Allow-listed File Explorer browse (roots and children; path-safe)")
public class FileExplorerResource {

  static final String PATH_NOT_FOUND = "Path not found";
  static final String INVALID_PATH = "Invalid path";

  private final IFileExplorerAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #FileExplorerResource(IFileExplorerAdaptor)}; catalog methods call {@link #requireAdaptor()}.
   */
  public FileExplorerResource() {
    this.adaptor = null;
  }

  @Autowired
  public FileExplorerResource(IFileExplorerAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List allow-listed File Explorer roots",
      description =
          "Admin. Lists configured File Explorer roots from server.properties"
              + " (fileExplorer.allowListedRoots). Responses contain catalog ids only — never"
              + " filesystem paths. Distinct from /serverconfigs (SY-02) and application CMS"
              + " resource files (SY-05). Write/upload is not supported on this surface.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FileExplorerRoot.class)))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<FileExplorerRoot> listRoots() {
    try {
      List<FileExplorerRoot> list = requireAdaptor().listRoots();
      return list != null ? list : List.of();
    } catch (RuntimeException e) {
      throw mapFailure(e);
    }
  }

  @GET
  @Path("/{rootId}/children")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List File Explorer children under a root",
      description =
          "Admin. Lists immediate children of an allow-listed root at a relative path (query"
              + " path, / separators; omit or blank for the root). Absolute paths, drive letters,"
              + " UNC, parent traversal, and unknown roots are 400/404 — responses never echo the"
              + " raw path. Distinct from SY-05 application files and SY-02 server configs.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = FileExplorerEntry.class)))),
        @ApiResponse(responseCode = "400", description = "Unsafe path or root id"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Root or path not found / not allow-listed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<FileExplorerEntry> listChildren(
      @PathParam("rootId") String rootId, @QueryParam("path") String path) {
    try {
      List<FileExplorerEntry> list = requireAdaptor().listChildren(rootId, path);
      if (list == null) {
        throw new WebApplicationException(PATH_NOT_FOUND, 404);
      }
      return list;
    } catch (RuntimeException e) {
      throw mapFailure(e);
    }
  }

  /**
   * Map adaptor failures to HTTP status without echoing raw filesystem paths in client messages.
   */
  static WebApplicationException mapFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(safeClientMessage(e.getMessage()), 400);
    }
    return new WebApplicationException(e, 500);
  }

  static String safeClientMessage(String message) {
    if (message == null || message.isBlank()) {
      return INVALID_PATH;
    }
    if (looksLikeRawPath(message)) {
      return INVALID_PATH;
    }
    return message;
  }

  static boolean looksLikeRawPath(String message) {
    return message.contains("..")
        || message.indexOf('/') >= 0
        || message.indexOf('\\') >= 0
        || message.indexOf(':') >= 0;
  }

  private IFileExplorerAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "File Explorer adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
