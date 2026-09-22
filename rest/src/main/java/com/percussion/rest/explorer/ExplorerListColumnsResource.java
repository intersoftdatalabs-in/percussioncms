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
package com.percussion.rest.explorer;

import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfoBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.function.Supplier;

/**
 * Session overlay of Explorer folder-list columns (#4722). Does not rewrite the shared display
 * format. Missing request user is 403. Invalid path or column sources are 400.
 */
@PSSiteManageBean(value = "restExplorerListColumnsResource")
@Path("/explorer/list-columns")
@Tag(name = "Explorer list columns", description = "Session display-format columns for a folder")
public class ExplorerListColumnsResource {

  private final ExplorerListColumnStore store;
  private final Supplier<String> currentUser;

  public ExplorerListColumnsResource() {
    this(new ExplorerListColumnStore(), ExplorerListColumnsResource::requestUser);
  }

  ExplorerListColumnsResource(ExplorerListColumnStore store, Supplier<String> currentUser) {
    this.store = store;
    this.currentUser = currentUser;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Read session list columns for a folder",
      responses = {
        @ApiResponse(responseCode = "200", description = "Saved columns, or empty when unset"),
        @ApiResponse(responseCode = "400", description = "folderPath missing or invalid"),
        @ApiResponse(responseCode = "403", description = "No authenticated user")
      })
  public ExplorerListColumns getColumns(@QueryParam("folderPath") String folderPath) {
    String user = requireUser();
    try {
      String path = ExplorerListColumnRules.normalizeFolderPath(folderPath);
      return new ExplorerListColumns(path, store.get(user, path));
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Save session list columns for a folder",
      description =
          "Replaces the in-memory column list for the current user and folder. sys_title is"
              + " required. Unknown sources are 400. Does not update the display-format design"
              + " object.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Saved"),
        @ApiResponse(responseCode = "400", description = "Invalid folderPath or columns"),
        @ApiResponse(responseCode = "403", description = "No authenticated user")
      })
  public ExplorerListColumns saveColumns(ExplorerListColumns body) {
    String user = requireUser();
    if (body == null) {
      throw new WebApplicationException("body is required", 400);
    }
    try {
      String path = ExplorerListColumnRules.normalizeFolderPath(body.getFolderPath());
      List<String> columns = ExplorerListColumnRules.normalizeColumns(body.getColumns());
      store.put(user, path, columns);
      return new ExplorerListColumns(path, columns);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  private String requireUser() {
    String user = currentUser.get();
    if (user == null || user.isBlank()) {
      throw new WebApplicationException("Not authorized", 403);
    }
    return user.trim();
  }

  private static String requestUser() {
    Object user = PSRequestInfoBase.getRequestInfo(PSRequestInfoBase.KEY_USER);
    return user == null ? null : String.valueOf(user);
  }
}
