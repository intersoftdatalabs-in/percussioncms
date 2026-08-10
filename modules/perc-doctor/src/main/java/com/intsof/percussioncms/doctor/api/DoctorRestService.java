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
package com.intsof.percussioncms.doctor.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

/**
 * Admin-only HTTP mirror of the perc-doctor CLI.
 *
 * <p>Mounted by the host under the maintenance JAX-RS server, e.g. {@code POST
 * /Rhythmyx/services/maintenance/doctor/{command}} with JSON body {@link DoctorRequest}.
 *
 * <p><strong>Hard gate:</strong> only Admin users may invoke any command. Anonymous and non-admin
 * callers receive HTTP 403.
 *
 * <p><strong>Dry-run default:</strong> omitted or null {@code dryRun} is treated as {@code true}
 * (report only; no deletes). Apply requires explicit {@code "dryRun": false}.
 */
@Path("/doctor")
public class DoctorRestService {

  private final DoctorAdminChecker adminChecker;
  private final DoctorApiService apiService;

  /**
   * @param adminChecker host admin gate; never null
   * @param installRootProvider default install root when request omits it; never null
   */
  public DoctorRestService(
      DoctorAdminChecker adminChecker, DoctorInstallRootProvider installRootProvider) {
    this(
        adminChecker,
        new DoctorApiService(Objects.requireNonNull(installRootProvider, "installRootProvider")));
  }

  /**
   * Package-visible for tests that inject a custom {@link DoctorApiService}.
   *
   * @param adminChecker host admin gate
   * @param apiService command runner
   */
  DoctorRestService(DoctorAdminChecker adminChecker, DoctorApiService apiService) {
    this.adminChecker = Objects.requireNonNull(adminChecker, "adminChecker");
    this.apiService = Objects.requireNonNull(apiService, "apiService");
  }

  /**
   * Execute a doctor command.
   *
   * @param command CLI command token ({@code clean-heap-dumps}, {@code clean-install-backups},
   *     {@code clean-logs}, {@code clean-temp})
   * @param request JSON body; may be null (treated as empty dry-run request)
   * @return structured report (same fields as CLI inventory/action report)
   */
  @POST
  @Path("/{command}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public DoctorReportView runCommand(@PathParam("command") String command, DoctorRequest request) {
    requireAdmin();

    String normalized = DoctorApiService.normalizeCommand(command);
    try {
      return apiService.execute(normalized, request);
    } catch (DoctorUnknownCommandException e) {
      throw new WebApplicationException(
          Response.status(Response.Status.NOT_FOUND)
              .entity("Unknown doctor command: " + safeCommand(normalized))
              .type(MediaType.TEXT_PLAIN)
              .build());
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid doctor request: " + safeMessage(e))
              .type(MediaType.TEXT_PLAIN)
              .build());
    } catch (IOException e) {
      throw new WebApplicationException(
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Doctor command failed.")
              .type(MediaType.TEXT_PLAIN)
              .build());
    }
  }

  private void requireAdmin() {
    boolean admin;
    try {
      admin = adminChecker.isCurrentUserAdmin();
    } catch (RuntimeException e) {
      // Treat auth resolution failures as forbidden (do not leak internals).
      throw forbidden();
    }
    if (!admin) {
      throw forbidden();
    }
  }

  private static WebApplicationException forbidden() {
    return new WebApplicationException(
        Response.status(Response.Status.FORBIDDEN)
            .entity("Only Admin users may run doctor commands.")
            .type(MediaType.TEXT_PLAIN)
            .build());
  }

  private static String safeCommand(String command) {
    if (command == null || command.isBlank()) {
      return "(empty)";
    }
    // Bound length so a crafted path segment cannot flood logs/responses.
    String c = command.trim();
    return c.length() > 64 ? c.substring(0, 64) : c;
  }

  private static String safeMessage(IllegalArgumentException e) {
    String msg = e.getMessage();
    if (msg == null || msg.isBlank()) {
      return "bad request";
    }
    // Avoid leaking raw paths in overly long messages.
    return msg.length() > 200 ? msg.substring(0, 200) : msg;
  }
}
