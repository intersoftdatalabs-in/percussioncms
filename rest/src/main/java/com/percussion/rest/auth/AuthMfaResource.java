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

import com.percussion.services.auth.mfa.PSMfaLockoutException;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
 * Self-service authenticated TOTP, recovery-code, and step-up MFA REST resource.
 *
 * <pre>
 *   GET    /Rhythmyx/services/auth/mfa/status
 *   POST   /Rhythmyx/services/auth/mfa/step-up
 *   POST   /Rhythmyx/services/auth/mfa/totp/start
 *   POST   /Rhythmyx/services/auth/mfa/totp/confirm
 *   DELETE /Rhythmyx/services/auth/mfa/totp
 *   POST   /Rhythmyx/services/auth/mfa/recovery-codes/regenerate
 * </pre>
 */
@PSSiteManageBean(value = "restAuthMfaResource")
@Path("/auth/mfa")
@Tag(
    name = "Auth MFA",
    description = "Self-service authenticator app enrollment, recovery codes, and step-up MFA")
public class AuthMfaResource {

  private static final Logger log = LogManager.getLogger(AuthMfaResource.class);

  private final IAuthMfaAdaptor adaptor;

  public AuthMfaResource() {
    this.adaptor = null;
  }

  @Autowired
  public AuthMfaResource(IAuthMfaAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get MFA enrollment and step-up status for the signed-in user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ProfileMfaStatus.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "503", description = "Adaptor unavailable")
      })
  public ProfileMfaStatus getMyMfaStatus() {
    return executeMapped(() -> requireAdaptor().getMyMfaStatus());
  }

  @POST
  @Path("/step-up")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Verify TOTP or recovery code for recent step-up assurance in the current session",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Step-up verified",
            content = @Content(schema = @Schema(implementation = MfaStepUpResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid code"),
        @ApiResponse(responseCode = "423", description = "Account temporarily locked"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
      })
  public MfaStepUpResponse verifyStepUp(MfaStepUpRequest request) {
    return executeMapped(
        () -> {
          if (request == null) {
            throw new IllegalArgumentException("MfaStepUpRequest body is required");
          }
          return requireAdaptor().verifyStepUp(request);
        });
  }

  @POST
  @Path("/totp/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Start TOTP authenticator app enrollment for the current user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Pending TOTP setup URI and secret",
            content = @Content(schema = @Schema(implementation = TotpEnrollStartResponse.class))),
        @ApiResponse(responseCode = "428", description = "Step-up MFA required")
      })
  public TotpEnrollStartResponse startTotpEnrollment(TotpEnrollStartRequest request) {
    return executeMapped(
        () ->
            requireAdaptor()
                .startTotpEnrollment(request != null ? request : new TotpEnrollStartRequest()));
  }

  @POST
  @Path("/totp/confirm")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Confirm pending TOTP enrollment with a 6-digit code",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "TOTP activated (returns initial recovery codes if none exist)",
            content = @Content(schema = @Schema(implementation = TotpEnrollConfirmResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid code or expired setup"),
        @ApiResponse(responseCode = "428", description = "Step-up MFA required")
      })
  public TotpEnrollConfirmResponse confirmTotpEnrollment(TotpEnrollConfirmRequest request) {
    return executeMapped(
        () -> {
          if (request == null) {
            throw new IllegalArgumentException("TotpEnrollConfirmRequest body is required");
          }
          return requireAdaptor().confirmTotpEnrollment(request);
        });
  }

  @DELETE
  @Path("/totp")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Disable TOTP authenticator app when permitted by policy/other factors",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "TOTP disabled",
            content = @Content(schema = @Schema(implementation = MfaActionResult.class))),
        @ApiResponse(responseCode = "409", description = "Last MFA factor cannot be removed"),
        @ApiResponse(responseCode = "428", description = "Step-up MFA required")
      })
  public MfaActionResult disableTotp() {
    return executeMapped(() -> requireAdaptor().disableTotp());
  }

  @POST
  @Path("/recovery-codes/regenerate")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Invalidate prior recovery codes and generate a fresh batch of 10 codes",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Fresh single-use recovery codes",
            content =
                @Content(schema = @Schema(implementation = RecoveryCodesRegenerateResponse.class))),
        @ApiResponse(responseCode = "409", description = "No active MFA factor enrolled"),
        @ApiResponse(responseCode = "428", description = "Step-up MFA required")
      })
  public RecoveryCodesRegenerateResponse regenerateRecoveryCodes() {
    return executeMapped(() -> requireAdaptor().regenerateRecoveryCodes());
  }

  @GET
  @Path("/passkeys")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "List active enrolled passkeys for the signed-in user")
  public PasskeyListResponse listPasskeys() {
    return executeMapped(() -> requireAdaptor().listPasskeys());
  }

  @POST
  @Path("/passkeys/register/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Start WebAuthn passkey registration for the current user")
  public PasskeyOptionsResponse startPasskeyRegistration(PasskeyOptionsRequest request) {
    return executeMapped(
        () ->
            requireAdaptor()
                .startPasskeyRegistration(
                    request != null ? request : new PasskeyOptionsRequest()));
  }

  @POST
  @Path("/passkeys/register/finish")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Finish WebAuthn passkey registration")
  public PasskeyRegisterFinishResponse finishPasskeyRegistration(
      PasskeyRegisterFinishRequest request) {
    return executeMapped(
        () -> {
          if (request == null) {
            throw new IllegalArgumentException("PasskeyRegisterFinishRequest body is required");
          }
          return requireAdaptor().finishPasskeyRegistration(request);
        });
  }

  @PUT
  @Path("/passkeys/{passkeyId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Rename an active passkey")
  public MfaActionResult renamePasskey(
      @PathParam("passkeyId") String passkeyId, PasskeyRenameRequest request) {
    return executeMapped(
        () -> {
          if (request == null) {
            throw new IllegalArgumentException("PasskeyRenameRequest body is required");
          }
          return requireAdaptor().renamePasskey(passkeyId, request);
        });
  }

  @DELETE
  @Path("/passkeys/{passkeyId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Remove an enrolled passkey (enforces step-up and last-factor policy)")
  public MfaActionResult deletePasskey(@PathParam("passkeyId") String passkeyId) {
    return executeMapped(() -> requireAdaptor().deletePasskey(passkeyId));
  }

  @POST
  @Path("/step-up/passkey/start")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Start passkey assertion challenge for session step-up MFA")
  public PasskeyOptionsResponse startPasskeyStepUp() {
    return executeMapped(() -> requireAdaptor().startPasskeyStepUp());
  }

  @POST
  @Path("/step-up/passkey/verify")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Verify passkey assertion to grant step-up MFA assurance")
  public MfaStepUpResponse verifyPasskeyStepUp(PasskeyStepUpVerifyRequest request) {
    return executeMapped(
        () -> {
          if (request == null) {
            throw new IllegalArgumentException("PasskeyStepUpVerifyRequest body is required");
          }
          return requireAdaptor().verifyPasskeyStepUp(request);
        });
  }

  private <T> T executeMapped(Supplier<T> call) {
    try {
      return call.get();
    } catch (PSStepUpRequiredException e) {
      throw jsonError(428, "STEP_UP_REQUIRED", e.getMessage(), e);
    } catch (PSMfaLockoutException e) {
      throw jsonError(423, "MFA_LOCKED", e.getMessage(), e);
    } catch (PSAuthRateLimitException e) {
      throw jsonError(429, "RATE_LIMITED", e.getMessage(), e);
    } catch (SecurityException e) {
      throw jsonError(401, "UNAUTHENTICATED", e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      throw jsonError(400, "INVALID_REQUEST", e.getMessage(), e);
    } catch (IllegalStateException e) {
      throw jsonError(409, "CONFLICT", e.getMessage(), e);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed profile MFA operation", e);
      throw jsonError(500, "MFA_ERROR", "Profile MFA operation failed", e);
    }
  }

  private static WebApplicationException jsonError(
      int status, String code, String message, Throwable cause) {
    Response resp =
        Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of("error", code, "message", StringUtils.defaultString(message)))
            .build();
    return new WebApplicationException(cause, resp);
  }

  private IAuthMfaAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw jsonError(503, "ADAPTOR_UNAVAILABLE", "Auth MFA adaptor not configured", null);
    }
    return adaptor;
  }
}
