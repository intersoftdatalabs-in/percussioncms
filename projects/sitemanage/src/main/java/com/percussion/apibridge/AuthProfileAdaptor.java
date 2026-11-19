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

import com.percussion.rest.auth.IAuthProfileAdaptor;
import com.percussion.rest.auth.ProfileEmailActionResult;
import com.percussion.rest.auth.ProfileEmailChangeRequest;
import com.percussion.rest.auth.ProfileEmailStatus;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailChangeResult;
import com.percussion.services.auth.account.PSEmailVerificationStatus;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * sitemanage apibridge adaptor for authenticated self-service email verification.
 *
 * <p>Always operates strictly on the current signed-in session user (no target username accepted).
 */
@PSSiteManageBean
@Lazy
public class AuthProfileAdaptor implements IAuthProfileAdaptor {

  private final IPSUserService userService;
  private final IPSAccountSecurityService accountSecurityService;
  private final Supplier<String> clientIpSupplier;

  @Autowired
  public AuthProfileAdaptor(
      IPSUserService userService, IPSAccountSecurityService accountSecurityService) {
    this(userService, accountSecurityService, AuthProfileAdaptor::resolveCurrentClientIp);
  }

  /** Package-visible constructor for isolated unit tests. */
  AuthProfileAdaptor(
      IPSUserService userService,
      IPSAccountSecurityService accountSecurityService,
      Supplier<String> clientIpSupplier) {
    this.userService = Objects.requireNonNull(userService, "userService");
    this.accountSecurityService =
        Objects.requireNonNull(accountSecurityService, "accountSecurityService");
    this.clientIpSupplier = Objects.requireNonNull(clientIpSupplier, "clientIpSupplier");
  }

  @Override
  public ProfileEmailStatus getMyEmailStatus() {
    PSCurrentUser current = loadCurrentUser();
    boolean isDirectory = current.getProviderType() != PSUserProviderType.INTERNAL;
    PSEmailVerificationStatus status =
        accountSecurityService.getEmailStatus(current.getName(), current.getEmail(), isDirectory);

    ProfileEmailStatus dto = new ProfileEmailStatus();
    dto.setUserName(status.userName());
    dto.setEmail(status.activeEmail());
    dto.setEmailVerified(status.emailVerified());
    dto.setEmailVerifiedAt(status.emailVerifiedAt());
    dto.setPendingEmail(status.pendingEmail());
    dto.setEmailEditable(!isDirectory);
    dto.setEmailRequired(status.emailRequired());
    dto.setProviderType(
        current.getProviderType() != null ? current.getProviderType().name() : "INTERNAL");
    return dto;
  }

  @Override
  public ProfileEmailActionResult requestMyEmailChange(ProfileEmailChangeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Email change request is required");
    }
    PSCurrentUser current = loadCurrentUser();
    boolean isInternal = current.getProviderType() == PSUserProviderType.INTERNAL;
    PSEmailChangeResult result =
        accountSecurityService.stageEmailChange(
            current.getName(),
            current.getEmail(),
            request.getEmail(),
            clientIpSupplier.get(),
            isInternal);
    return toActionDto(result);
  }

  @Override
  public ProfileEmailActionResult resendMyEmailVerification() {
    PSCurrentUser current = loadCurrentUser();
    PSEmailChangeResult result =
        accountSecurityService.resendVerification(
            current.getName(), current.getEmail(), clientIpSupplier.get());
    return toActionDto(result);
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

  private static ProfileEmailActionResult toActionDto(PSEmailChangeResult result) {
    return new ProfileEmailActionResult(
        result.verificationSent(),
        result.appliedImmediately(),
        result.activeEmail(),
        result.pendingEmail(),
        result.emailVerified(),
        result.message());
  }

  private static String resolveCurrentClientIp() {
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
