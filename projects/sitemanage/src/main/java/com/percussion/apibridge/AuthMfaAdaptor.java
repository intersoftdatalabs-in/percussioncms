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
package com.percussion.apibridge;

import com.percussion.rest.auth.IAuthMfaAdaptor;
import com.percussion.rest.auth.MfaActionResult;
import com.percussion.rest.auth.MfaStepUpRequest;
import com.percussion.rest.auth.MfaStepUpResponse;
import com.percussion.rest.auth.ProfileMfaStatus;
import com.percussion.rest.auth.RecoveryCodesRegenerateResponse;
import com.percussion.rest.auth.TotpEnrollConfirmRequest;
import com.percussion.rest.auth.TotpEnrollConfirmResponse;
import com.percussion.rest.auth.TotpEnrollStartRequest;
import com.percussion.rest.auth.TotpEnrollStartResponse;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSProfileMfaStatus;
import com.percussion.services.auth.mfa.PSTotpConfirmOutcome;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * sitemanage apibridge adaptor for self-service MFA status, step-up verification, TOTP
 * enrollment/removal, and recovery code regeneration.
 *
 * <p>Always operates strictly on the current signed-in session user (no target username accepted).
 */
@PSSiteManageBean
@Lazy
public class AuthMfaAdaptor implements IAuthMfaAdaptor {

  private final IPSUserService userService;
  private final IPSMfaService mfaService;
  private final Supplier<HttpSession> sessionSupplier;
  private final Supplier<String> clientIpSupplier;

  @Autowired
  public AuthMfaAdaptor(IPSUserService userService, IPSMfaService mfaService) {
    this(
        userService,
        mfaService,
        AuthMfaAdaptor::resolveCurrentSession,
        AuthMfaAdaptor::resolveCurrentClientIp);
  }

  /** Package-visible constructor for isolated unit tests. */
  AuthMfaAdaptor(
      IPSUserService userService,
      IPSMfaService mfaService,
      Supplier<HttpSession> sessionSupplier,
      Supplier<String> clientIpSupplier) {
    this.userService = Objects.requireNonNull(userService, "userService");
    this.mfaService = Objects.requireNonNull(mfaService, "mfaService");
    this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");
    this.clientIpSupplier = Objects.requireNonNull(clientIpSupplier, "clientIpSupplier");
  }

  @Override
  public ProfileMfaStatus getMyMfaStatus() {
    PSCurrentUser current = loadCurrentUser();
    PSProfileMfaStatus status =
        mfaService.getProfileMfaStatus(
            current.getName(), current.getRoles(), sessionSupplier.get());

    ProfileMfaStatus dto = new ProfileMfaStatus();
    dto.setUserName(status.userName());
    dto.setMfaMode(status.mfaMode());
    dto.setMfaRequired(status.mfaRequired());
    dto.setTotpEnabled(status.totpEnabled());
    dto.setPasskeyCount(status.passkeyCount());
    dto.setRecoveryCodesRemaining(status.recoveryCodesRemaining());
    dto.setCanDisableTotp(status.canDisableTotp());
    dto.setStepUpActive(status.stepUpActive());
    dto.setStepUpExpiresAt(status.stepUpExpiresAt());
    return dto;
  }

  @Override
  public MfaStepUpResponse verifyStepUp(MfaStepUpRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Step-up verification request is required");
    }
    PSCurrentUser current = loadCurrentUser();
    long expiresAt =
        mfaService.verifyStepUp(
            current.getName(),
            request.getCode(),
            request.isRecoveryCode(),
            clientIpSupplier.get(),
            requireSession());
    return new MfaStepUpResponse(true, expiresAt, "Multi-factor verification confirmed.");
  }

  @Override
  public TotpEnrollStartResponse startTotpEnrollment(TotpEnrollStartRequest request) {
    PSCurrentUser current = loadCurrentUser();
    String label = request != null ? request.getLabel() : null;
    PSTotpEnrollmentStart start =
        mfaService.startProfileTotpEnrollment(current.getName(), label, sessionSupplier.get());
    return new TotpEnrollStartResponse(
        start.totpId(),
        start.otpauthUri(),
        start.manualSecret(),
        start.issuer(),
        start.accountName(),
        start.digits(),
        start.periodSeconds());
  }

  @Override
  public TotpEnrollConfirmResponse confirmTotpEnrollment(TotpEnrollConfirmRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("TOTP confirmation request is required");
    }
    PSCurrentUser current = loadCurrentUser();
    PSTotpConfirmOutcome outcome =
        mfaService.confirmProfileTotpEnrollment(
            current.getName(), request.getTotpId(), request.getCode(), sessionSupplier.get());
    if (!outcome.enrolled()) {
      throw new IllegalArgumentException(outcome.message());
    }
    return new TotpEnrollConfirmResponse(true, outcome.recoveryCodes(), outcome.message());
  }

  @Override
  public MfaActionResult disableTotp() {
    PSCurrentUser current = loadCurrentUser();
    mfaService.disableProfileTotp(current.getName(), current.getRoles(), sessionSupplier.get());
    return new MfaActionResult(true, "Authenticator app removed.");
  }

  @Override
  public RecoveryCodesRegenerateResponse regenerateRecoveryCodes() {
    PSCurrentUser current = loadCurrentUser();
    List<String> codes =
        mfaService.regenerateRecoveryCodes(current.getName(), sessionSupplier.get());
    return new RecoveryCodesRegenerateResponse(
        codes, "Generated " + codes.size() + " new single-use recovery codes.");
  }

  private PSCurrentUser loadCurrentUser() {
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        throw new SecurityException("Authenticated user session required");
      }
      return current;
    } catch (PSDataServiceException ex) {
      throw new SecurityException("Authenticated user session required", ex);
    }
  }

  private HttpSession requireSession() {
    HttpSession session = sessionSupplier.get();
    if (session == null) {
      throw new SecurityException("Authenticated HTTP session required");
    }
    return session;
  }

  static HttpSession resolveCurrentSession() {
    PSRequest psReq = PSSecurityFilter.getCurrentRequest();
    HttpServletRequest req = psReq != null ? psReq.getServletRequest() : null;
    return req != null ? req.getSession(false) : null;
  }

  static String resolveCurrentClientIp() {
    try {
      PSRequest psReq = PSSecurityFilter.getCurrentRequest();
      HttpServletRequest req = psReq != null ? psReq.getServletRequest() : null;
      if (req == null) {
        return "unknown";
      }
      if (PSServer.getServerProps() != null && PSServer.isRequestBehindProxy(req)) {
        String forwarded = StringUtils.trimToNull(req.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
          int comma = forwarded.indexOf(',');
          String first = comma > 0 ? forwarded.substring(0, comma).trim() : forwarded;
          if (!first.isEmpty()) {
            return first;
          }
        }
      }
      return StringUtils.defaultIfBlank(req.getRemoteAddr(), "unknown");
    } catch (RuntimeException ex) {
      return "unknown";
    }
  }
}
