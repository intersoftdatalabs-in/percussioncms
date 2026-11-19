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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.rest.auth;

import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Admin readiness and transactional mail diagnostic REST surface.
 *
 * <pre>
 *   GET  /Rhythmyx/services/auth/admin/readiness
 *   POST /Rhythmyx/services/auth/admin/mail/test
 * </pre>
 *
 * <p>Bean id {@code restAuthReadinessResource} is registered on the {@code rest-jax-rs} CXF server
 * in {@code sitemanage-beans.xml}.
 */
@PSSiteManageBean(value = "restAuthReadinessResource")
@Path("/auth/admin")
@Tag(
    name = "Auth Readiness",
    description = "Authentication, MFA, and account recovery readiness administration API")
public class AuthReadinessResource {

  private static final Logger log = LogManager.getLogger(AuthReadinessResource.class);

  private final IAuthReadinessAdaptor adaptor;

  public AuthReadinessResource() {
    this.adaptor = null;
  }

  @Autowired
  public AuthReadinessResource(IAuthReadinessAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/readiness")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Evaluate authentication, MFA, and recovery readiness",
      description =
          "Evaluates auth mail, HTTPS/WebAuthn configuration, cluster key fingerprint, schema"
              + " tables, and account email/MFA coverage. Requires Admin role.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = AuthReadinessReport.class))),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public AuthReadinessReport getReadinessReport() {
    try {
      return requireAdaptor().getReadinessReport();
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to evaluate auth readiness report", e);
      throw new WebApplicationException("Failed to evaluate auth readiness report", e, 500);
    }
  }

  @POST
  @Path("/mail/test")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Send a synchronous transactional authentication test email",
      description =
          "Verifies synchronous SMTP delivery using PSAuthMailService. Requires Admin role.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Test email attempt result",
            content = @Content(schema = @Schema(implementation = AuthMailTestResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid recipient request"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public AuthMailTestResult sendTestEmail(AuthMailTestRequest request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("AuthMailTestRequest body is required");
      }
      return requireAdaptor().sendTestEmail(request);
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to dispatch test auth email", e);
      throw new WebApplicationException("Failed to dispatch test auth email", e, 500);
    }
  }

  @POST
  @Path("/recovery")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Initiate audited Administrator break-glass MFA recovery for an account",
      description =
          "Requires Admin role, recent Admin step-up MFA, and a verification note. Issues a"
              + " short-lived break-glass link or executes immediate MFA factor reset.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Break-glass recovery initiated",
            content = @Content(schema = @Schema(implementation = AdminRecoveryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid recovery request"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "409", description = "Missing verified email or mail config"),
        @ApiResponse(responseCode = "428", description = "Recent Admin step-up MFA required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public AdminRecoveryResponse initiateAdminRecovery(AdminRecoveryRequest request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("AdminRecoveryRequest body is required");
      }
      return requireAdaptor().initiateAdminRecovery(request);
    } catch (PSStepUpRequiredException e) {
      Response resp =
          Response.status(428)
              .type(MediaType.APPLICATION_JSON)
              .entity(
                  Map.of(
                      "error",
                      "STEP_UP_REQUIRED",
                      "message",
                      StringUtils.defaultString(e.getMessage())))
              .build();
      throw new WebApplicationException(e, resp);
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (IllegalStateException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.CONFLICT);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to initiate admin break-glass recovery", e);
      throw new WebApplicationException("Failed to initiate admin break-glass recovery", e, 500);
    }
  }

  private IAuthReadinessAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Auth readiness adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
