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
package com.percussion.rest.auditlog;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST surface for querying and exporting {@code PSX_SYSTEM_AUDIT_LOG} (Phase 3 / #2618,
 * Phase 5 export / #2715).
 *
 * <pre>
 *   GET /Rhythmyx/rest/auditlog/entries
 *   GET /Rhythmyx/rest/auditlog/entries/{auditId}
 *   GET /Rhythmyx/rest/auditlog/export?format=csv|json
 * </pre>
 *
 * <p>AuthZ is enforced in the sitemanage apibridge: Admin role or role property {@code
 * sys_securityAuditLogViewer}. Unauthorized callers receive HTTP 403.
 *
 * <p>Bean id {@code restAuditLogResource} is registered on the {@code rest-jax-rs} CXF server in
 * {@code sitemanage-beans.xml}.
 */
@PSSiteManageBean(value = "restAuditLogResource")
@Path("/auditlog")
@Tag(name = "Audit Log", description = "System security audit log query and export API")
public class AuditLogResource {

  private static final Logger log = LogManager.getLogger(AuditLogResource.class);

  private static final String MEDIA_TEXT_CSV = "text/csv";

  private final IAuditLogAdaptor adaptor;

  public AuditLogResource() {
    this.adaptor = null;
  }

  @Autowired
  public AuditLogResource(IAuditLogAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/entries")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Query system audit log entries",
      description =
          "Returns a page of durable system audit log rows. Requires Admin role or role property"
              + " sys_securityAuditLogViewer=true.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SystemAuditLogPage.class))),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "403", description = "Caller not allowed to view audit log"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public SystemAuditLogPage queryEntries(
      @Parameter(description = "Inclusive lower bound (ISO-8601 instant)") @QueryParam("from")
          String from,
      @Parameter(description = "Exclusive upper bound (ISO-8601 instant)") @QueryParam("to")
          String to,
      @Parameter(description = "Module code filter (e.g. AUTH)") @QueryParam("module")
          String module,
      @Parameter(description = "Event type filter") @QueryParam("eventType") String eventType,
      @Parameter(description = "Outcome filter (SUCCESS, FAILURE, …)") @QueryParam("outcome")
          String outcome,
      @Parameter(description = "Actor (user name) filter, case-insensitive") @QueryParam("actor")
          String actor,
      @Parameter(description = "Zero-based offset") @QueryParam("offset") @DefaultValue("0")
          int offset,
      @Parameter(description = "Page size (server clamps to max)")
          @QueryParam("limit")
          @DefaultValue("50")
          int limit) {
    try {
      return requireAdaptor()
          .query(from, to, module, eventType, outcome, actor, offset, limit);
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Audit log query failed", e);
      throw new WebApplicationException("Failed to query audit log", e, 500);
    }
  }

  @GET
  @Path("/entries/{auditId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get one system audit log entry",
      description =
          "Loads a single durable audit row by AUDIT_ID (UUID). Requires Admin role or role"
              + " property sys_securityAuditLogViewer=true.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = SystemAuditLogEntry.class))),
        @ApiResponse(responseCode = "403", description = "Caller not allowed to view audit log"),
        @ApiResponse(responseCode = "404", description = "Entry not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public SystemAuditLogEntry getEntry(
      @Parameter(name = "auditId", required = true) @PathParam("auditId") String auditId) {
    try {
      SystemAuditLogEntry entry = requireAdaptor().findById(auditId);
      if (entry == null) {
        throw new WebApplicationException("Audit log entry not found", Response.Status.NOT_FOUND);
      }
      return entry;
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Audit log get failed for id={}", auditId, e);
      throw new WebApplicationException("Failed to load audit log entry", e, 500);
    }
  }

  /**
   * Export filtered system audit log rows as CSV or JSON download (Phase 5 / #2715).
   *
   * <p>Uses the same filters and AuthZ as {@link #queryEntries}. Default format is {@code json};
   * max rows default {@link SystemAuditLogExport#DEFAULT_MAX_ROWS}, hard cap {@link
   * SystemAuditLogExport#MAX_ROWS}.
   */
  @GET
  @Path("/export")
  @Operation(
      summary = "Export system audit log entries (CSV or JSON)",
      description =
          "Downloads durable system audit log rows matching the same filters as the query API."
              + " Requires Admin role or role property sys_securityAuditLogViewer=true. format=csv"
              + " or format=json (default json).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Export body (CSV or JSON)",
            content = {
              @Content(mediaType = MediaType.APPLICATION_JSON),
              @Content(mediaType = MEDIA_TEXT_CSV)
            }),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "403", description = "Caller not allowed to view audit log"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public Response exportEntries(
      @Parameter(description = "Export format: csv or json (default json)") @QueryParam("format")
          String format,
      @Parameter(description = "Inclusive lower bound (ISO-8601 instant)") @QueryParam("from")
          String from,
      @Parameter(description = "Exclusive upper bound (ISO-8601 instant)") @QueryParam("to")
          String to,
      @Parameter(description = "Module code filter (e.g. AUTH)") @QueryParam("module")
          String module,
      @Parameter(description = "Event type filter") @QueryParam("eventType") String eventType,
      @Parameter(description = "Outcome filter (SUCCESS, FAILURE, …)") @QueryParam("outcome")
          String outcome,
      @Parameter(description = "Actor (user name) filter, case-insensitive") @QueryParam("actor")
          String actor,
      @Parameter(description = "Max rows (server clamps; default 5000, max 10000)")
          @QueryParam("maxRows")
          @DefaultValue("0")
          int maxRows) {
    try {
      String normalized = SystemAuditLogExport.normalizeFormat(format);
      List<SystemAuditLogEntry> entries =
          requireAdaptor().export(from, to, module, eventType, outcome, actor, maxRows);
      if ("csv".equals(normalized)) {
        String body = SystemAuditLogExport.toCsv(entries);
        return Response.ok(body, MEDIA_TEXT_CSV)
            .header("Content-Disposition", "attachment; filename=\"system-audit-log.csv\"")
            .build();
      }
      String body = SystemAuditLogExport.toJson(entries);
      return Response.ok(body, MediaType.APPLICATION_JSON)
          .header("Content-Disposition", "attachment; filename=\"system-audit-log.json\"")
          .build();
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Audit log export failed", e);
      throw new WebApplicationException("Failed to export audit log", e, 500);
    }
  }

  private IAuditLogAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Audit log adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
