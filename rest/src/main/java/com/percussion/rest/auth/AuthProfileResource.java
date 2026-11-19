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
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
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
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Self-service authenticated email verification and staged email-change REST surface.
 *
 * <pre>
 *   GET  /Rhythmyx/services/auth/profile/email
 *   POST /Rhythmyx/services/auth/profile/email/change
 *   POST /Rhythmyx/services/auth/profile/email/resend
 * </pre>
 *
 * <p>Bean id {@code restAuthProfileResource} is registered on {@code rest-jax-rs} in {@code
 * sitemanage-beans.xml}.
 */
@PSSiteManageBean(value = "restAuthProfileResource")
@Path("/auth/profile")
@Tag(
    name = "Auth Profile",
    description = "Self-service email verification and staged email change for signed-in users")
public class AuthProfileResource {

  private static final Logger log = LogManager.getLogger(AuthProfileResource.class);

  private final IAuthProfileAdaptor adaptor;

  public AuthProfileResource() {
    this.adaptor = null;
  }

  @Autowired
  public AuthProfileResource(IAuthProfileAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/email")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get email verification status for the current signed-in user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ProfileEmailStatus.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "503", description = "Adaptor unavailable")
      })
  public ProfileEmailStatus getMyEmailStatus() {
    return executeMapped(() -> requireAdaptor().getMyEmailStatus());
  }

  @POST
  @Path("/email/change")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Stage a new email address and dispatch a verification link",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Verification dispatched or address already verified",
            content = @Content(schema = @Schema(implementation = ProfileEmailActionResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid email or read-only directory account"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "503", description = "Mail or public URL configuration missing")
      })
  public ProfileEmailActionResult requestMyEmailChange(ProfileEmailChangeRequest request) {
    return executeMapped(
        () -> {
          if (request == null) {
            throw new IllegalArgumentException("ProfileEmailChangeRequest body is required");
          }
          return requireAdaptor().requestMyEmailChange(request);
        });
  }

  @POST
  @Path("/email/resend")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Resend verification email for an unverified or pending email address",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Verification email sent",
            content = @Content(schema = @Schema(implementation = ProfileEmailActionResult.class))),
        @ApiResponse(responseCode = "400", description = "Email already verified or missing"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "503", description = "Mail or public URL configuration missing")
      })
  public ProfileEmailActionResult resendMyEmailVerification() {
    return executeMapped(() -> requireAdaptor().resendMyEmailVerification());
  }

  private <T> T executeMapped(Supplier<T> call) {
    try {
      return call.get();
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
    } catch (PSAuthRateLimitException e) {
      throw new WebApplicationException(e.getMessage(), e, 429);
    } catch (SecurityException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.FORBIDDEN);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.BAD_REQUEST);
    } catch (IllegalStateException e) {
      throw new WebApplicationException(e.getMessage(), e, Response.Status.SERVICE_UNAVAILABLE);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed profile email operation", e);
      throw new WebApplicationException("Profile email operation failed", e, 500);
    }
  }

  private IAuthProfileAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Auth profile adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
