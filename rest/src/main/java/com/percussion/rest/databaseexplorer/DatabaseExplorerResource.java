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

package com.percussion.rest.databaseexplorer;

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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Database Explorer catalog browse (Workbench §12.2).
 *
 * <p>Admin-only list of configured allow-listed JDBC datasources and their
 * tables/views. Distinct from File Explorer ({@code /fileexplorer}). Unsafe or
 * non-allow-listed datasource ids are 400 without echoing JDBC URLs, SQL, or
 * credentials. Write/DDL is out of scope for this surface.
 */
@PSSiteManageBean(value = "restDatabaseExplorerResource")
@Path("/databaseexplorer")
@XmlRootElement
@Tag(
    name = "Database Explorer",
    description = "Allow-listed JDBC catalog browse (datasources and tables/views; read-only)")
public class DatabaseExplorerResource {

  static final String DATASOURCE_NOT_FOUND = "Datasource not found";
  static final String INVALID_DATASOURCE = "Invalid datasource";

  private final IDatabaseExplorerAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #DatabaseExplorerResource(IDatabaseExplorerAdaptor)}; catalog methods call {@link
   * #requireAdaptor()}.
   */
  public DatabaseExplorerResource() {
    this.adaptor = null;
  }

  @Autowired
  public DatabaseExplorerResource(IDatabaseExplorerAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List allow-listed Database Explorer datasources",
      description =
          "Admin. Lists configured Database Explorer datasources from server.properties"
              + " (databaseExplorer.allowListedDatasources). Responses contain catalog ids"
              + " only — never JDBC URLs or credentials. Distinct from /fileexplorer. Write/DDL"
              + " is not supported on this surface.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = DatabaseExplorerDatasource.class)))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<DatabaseExplorerDatasource> listDatasources() {
    try {
      List<DatabaseExplorerDatasource> list = requireAdaptor().listDatasources();
      return list != null ? list : List.of();
    } catch (RuntimeException e) {
      throw mapFailure(e);
    }
  }

  @GET
  @Path("/{datasourceId}/tables")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List tables and views for an allow-listed datasource",
      description =
          "Admin. Lists TABLE and VIEW objects from JDBC DatabaseMetaData for an"
              + " allow-listed catalog id. Unsafe or non-allow-listed ids are 400 — responses"
              + " never echo JDBC URLs, SQL, or credentials. Distinct from File Explorer.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = DatabaseExplorerTable.class)))),
        @ApiResponse(responseCode = "400", description = "Unsafe or non-allow-listed datasource id"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Allow-listed datasource unavailable"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<DatabaseExplorerTable> listTables(@PathParam("datasourceId") String datasourceId) {
    try {
      List<DatabaseExplorerTable> list = requireAdaptor().listTables(datasourceId);
      if (list == null) {
        throw new WebApplicationException(DATASOURCE_NOT_FOUND, 404);
      }
      return list;
    } catch (RuntimeException e) {
      throw mapFailure(e);
    }
  }

  /**
   * Map adaptor failures to HTTP status without echoing JDBC URLs or SQL in client
   * messages.
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
      return INVALID_DATASOURCE;
    }
    if (looksLikeRawSecretOrSql(message)) {
      return INVALID_DATASOURCE;
    }
    return message;
  }

  /**
   * True when the message looks like a JDBC URL, SQL fragment, or filesystem path.
   * Adaptor messages such as {@code Invalid datasource} pass through.
   */
  static boolean looksLikeRawSecretOrSql(String message) {
    String lower = message.toLowerCase();
    if (lower.contains("jdbc:") || lower.contains("password") || lower.contains("user=")) {
      return true;
    }
    if (message.contains("..")
        || message.indexOf('\\') >= 0
        || message.indexOf(';') >= 0
        || message.indexOf('\'') >= 0) {
      return true;
    }
    if (message.startsWith("/") || message.startsWith("~")) {
      return true;
    }
    if (message.length() >= 2
        && Character.isLetter(message.charAt(0))
        && message.charAt(1) == ':') {
      return true;
    }
    return false;
  }

  private IDatabaseExplorerAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Database Explorer adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
