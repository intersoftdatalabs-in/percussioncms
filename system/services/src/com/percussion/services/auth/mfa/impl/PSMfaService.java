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
package com.percussion.services.auth.mfa.impl;

import com.percussion.server.PSServer;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.IPSPasskeyService;
import com.percussion.services.auth.mfa.IPSRecoveryCodeService;
import com.percussion.services.auth.mfa.IPSTotpService;
import com.percussion.services.auth.mfa.PSMfaChallengeOutcome;
import com.percussion.services.auth.mfa.PSMfaLockoutException;
import com.percussion.services.auth.mfa.PSMfaTxStatus;
import com.percussion.services.auth.mfa.PSPasskeyOptions;
import com.percussion.services.auth.mfa.PSPasskeyRegisterOutcome;
import com.percussion.services.auth.mfa.PSPasskeySummary;
import com.percussion.services.auth.mfa.PSProfileMfaStatus;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.mfa.PSTotpConfirmOutcome;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import com.percussion.services.auth.policy.IPSAuthPolicyService;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.system.utils.PSBaseBean;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Coordinates MFA transactions, TOTP/recovery verification, lockouts, and step-up checks. */
@PSBaseBean("sys_mfaService")
public class PSMfaService implements IPSMfaService {

  public static final String PROP_CHALLENGE_TTL_MINUTES = "auth.mfa.challengeTtlMinutes";
  public static final String PROP_MAX_FAILED_ATTEMPTS = "auth.mfa.maxFailedAttempts";
  public static final String PROP_LOCKOUT_MINUTES = "auth.mfa.lockoutMinutes";
  public static final String PROP_STEP_UP_MINUTES = "auth.mfa.stepUpWindowMinutes";
  public static final String PROP_RATE_VERIFY_IP = "auth.rateLimit.mfaVerifyPerIpPerMinute";
  public static final String PROP_RATE_VERIFY_USER = "auth.rateLimit.mfaVerifyPerUserPerMinute";

  private static final int TX_ID_BYTES = 32;
  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final IPSAuthSecurityDao authSecurityDao;
  private final IPSTotpService totpService;
  private final IPSRecoveryCodeService recoveryCodeService;
  private final IPSAuthPolicyService authPolicyService;
  private final IPSAuthRateLimiter rateLimiter;
  private final IPSPasskeyService passkeyService;
  private final Supplier<Properties> serverPropsSupplier;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public PSMfaService(
      IPSAuthSecurityDao authSecurityDao,
      IPSTotpService totpService,
      IPSRecoveryCodeService recoveryCodeService,
      IPSAuthPolicyService authPolicyService,
      IPSAuthRateLimiter rateLimiter,
      IPSPasskeyService passkeyService) {
    this(
        authSecurityDao,
        totpService,
        recoveryCodeService,
        authPolicyService,
        rateLimiter,
        passkeyService,
        PSMfaService::defaultServerProperties,
        Clock.systemUTC());
  }

  public PSMfaService(
      IPSAuthSecurityDao authSecurityDao,
      IPSTotpService totpService,
      IPSRecoveryCodeService recoveryCodeService,
      IPSAuthPolicyService authPolicyService,
      IPSAuthRateLimiter rateLimiter) {
    this(
        authSecurityDao,
        totpService,
        recoveryCodeService,
        authPolicyService,
        rateLimiter,
        null,
        PSMfaService::defaultServerProperties,
        Clock.systemUTC());
  }

  PSMfaService(
      IPSAuthSecurityDao authSecurityDao,
      IPSTotpService totpService,
      IPSRecoveryCodeService recoveryCodeService,
      IPSAuthPolicyService authPolicyService,
      IPSAuthRateLimiter rateLimiter,
      Supplier<Properties> serverPropsSupplier,
      Clock clock) {
    this(
        authSecurityDao,
        totpService,
        recoveryCodeService,
        authPolicyService,
        rateLimiter,
        null,
        serverPropsSupplier,
        clock);
  }

  PSMfaService(
      IPSAuthSecurityDao authSecurityDao,
      IPSTotpService totpService,
      IPSRecoveryCodeService recoveryCodeService,
      IPSAuthPolicyService authPolicyService,
      IPSAuthRateLimiter rateLimiter,
      IPSPasskeyService passkeyService,
      Supplier<Properties> serverPropsSupplier,
      Clock clock) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.totpService = Objects.requireNonNull(totpService, "totpService");
    this.recoveryCodeService = Objects.requireNonNull(recoveryCodeService, "recoveryCodeService");
    this.authPolicyService = Objects.requireNonNull(authPolicyService, "authPolicyService");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.passkeyService = passkeyService;
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public PSMfaTxStatus createLoginTransaction(
      String txType,
      String userName,
      String redirectUrl,
      String locale,
      Collection<String> roles) {
    String cleanUser = requireUserName(userName);
    String cleanType = StringUtils.trimToEmpty(txType);
    if (!PSAuthLoginTx.TYPE_MFA_CHALLENGE.equals(cleanType)
        && !PSAuthLoginTx.TYPE_MFA_ENROLL.equals(cleanType)) {
      throw new IllegalArgumentException("Unsupported login transaction type: " + cleanType);
    }

    Instant now = clock.instant();
    long ttlMinutes = readPositiveLong(PROP_CHALLENGE_TTL_MINUTES, 10L);
    Instant expiresAt = now.plus(Duration.ofMinutes(ttlMinutes));

    PSAuthLoginTx tx = new PSAuthLoginTx();
    tx.setTxId(generateTxId());
    tx.setTxType(cleanType);
    tx.setUserName(cleanUser);
    tx.setState(PSAuthLoginTx.STATE_PENDING);
    tx.setAttemptCount(0);
    tx.setCreatedAt(Date.from(now));
    tx.setExpiresAt(Date.from(expiresAt));
    tx.setPayloadJson(serializePayload(redirectUrl, locale, roles));
    authSecurityDao.saveLoginTx(tx);

    return buildTxStatus(tx, parsePayload(tx.getPayloadJson()));
  }

  @Override
  public PSMfaTxStatus getTransactionStatus(String txId) {
    Optional<PSAuthLoginTx> opt = loadActiveTx(txId, null);
    if (opt.isEmpty()) {
      return invalidTxStatus(txId);
    }
    PSAuthLoginTx tx = opt.get();
    return buildTxStatus(tx, parsePayload(tx.getPayloadJson()));
  }

  @Override
  public PSMfaChallengeOutcome verifyChallengeTotp(String txId, String code, String requestIp) {
    return verifyChallengeFactor(txId, code, requestIp, false);
  }

  @Override
  public PSMfaChallengeOutcome verifyChallengeRecoveryCode(
      String txId, String code, String requestIp) {
    return verifyChallengeFactor(txId, code, requestIp, true);
  }

  @Override
  public PSTotpEnrollmentStart startEnrollOnlyTotp(String txId, String label) {
    PSAuthLoginTx tx =
        loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_ENROLL)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "MFA setup session has expired. Sign in again to continue."));
    return totpService.startEnrollment(tx.getUserName(), label);
  }

  @Override
  public PSTotpConfirmOutcome completeEnrollOnlyTotp(
      String txId, String totpId, String code, String requestIp) {
    PSAuthLoginTx tx =
        loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_ENROLL)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "MFA setup session has expired. Sign in again to continue."));
    String userName = tx.getUserName();
    enforceVerifyRateLimits(userName, requestIp);
    ensureNotLocked(userName);

    boolean confirmed = totpService.confirmEnrollment(userName, totpId, code);
    if (!confirmed) {
      recordFailedAttempt(userName, "TOTP_ENROLL");
      return new PSTotpConfirmOutcome(
          false, List.of(), userName, "", "", List.of(), "Invalid authenticator verification code.");
    }

    Instant now = clock.instant();
    int updated =
        authSecurityDao.consumeLoginTxAtomically(
            tx.getTxId(), PSAuthLoginTx.TYPE_MFA_ENROLL, Date.from(now));
    if (updated != 1) {
      return new PSTotpConfirmOutcome(
          false,
          List.of(),
          userName,
          "",
          "",
          List.of(),
          "MFA setup session is no longer active. Sign in again.");
    }

    resetFailures(userName);
    List<String> recoveryCodes = recoveryCodeService.generateAndReplaceCodes(userName);
    PSSystemAuditLogger.mfaFactorEnrolled(null, userName, "TOTP");
    PSSystemAuditLogger.mfaRecoveryCodesRegenerated(null, userName, recoveryCodes.size());

    TxPayload payload = parsePayload(tx.getPayloadJson());
    return new PSTotpConfirmOutcome(
        true,
        recoveryCodes,
        userName,
        payload.redirectUrl(),
        payload.locale(),
        payload.roles(),
        "Authenticator app and recovery codes saved.");
  }

  @Override
  public boolean cancelTransaction(String txId) {
    if (StringUtils.isBlank(txId)) {
      return false;
    }
    return authSecurityDao.cancelLoginTx(txId.trim()) > 0;
  }

  @Override
  public PSProfileMfaStatus getProfileMfaStatus(
      String userName, Collection<String> roles, HttpSession session) {
    String cleanUser = requireUserName(userName);
    String mode = authPolicyService.getEffectiveMode();
    boolean required = authPolicyService.isMfaRequiredForUser(cleanUser, roles);
    boolean totpEnabled = totpService.hasActiveTotp(cleanUser);
    int passkeyCount = authSecurityDao.findActivePasskeysByUser(cleanUser).size();
    long remainingCodes = recoveryCodeService.countRemaining(cleanUser);
    boolean canDisableTotp = !required || passkeyCount > 0;
    Instant stepUpExp = readActiveStepUpExpiry(session);

    return new PSProfileMfaStatus(
        cleanUser,
        mode,
        required,
        totpEnabled,
        passkeyCount,
        remainingCodes,
        canDisableTotp,
        stepUpExp != null,
        stepUpExp);
  }

  @Override
  public long verifyStepUp(String userName, String code, String requestIp, HttpSession session) {
    return verifyStepUp(userName, code, false, requestIp, session);
  }

  @Override
  public long verifyStepUp(
      String userName, String code, boolean useRecoveryCode, String requestIp, HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (session == null) {
      throw new SecurityException("Authenticated session required");
    }
    enforceVerifyRateLimits(cleanUser, requestIp);
    ensureNotLocked(cleanUser);

    if (useRecoveryCode) {
      if (recoveryCodeService.countRemaining(cleanUser) <= 0) {
        throw new IllegalStateException("No recovery codes are available for step-up verification.");
      }
      if (!recoveryCodeService.consumeCode(cleanUser, code)) {
        recordFailedAttempt(cleanUser, "STEP_UP_RECOVERY_CODE");
        throw new IllegalArgumentException("Invalid or already used recovery code.");
      }
      long remaining = recoveryCodeService.countRemaining(cleanUser);
      PSSystemAuditLogger.mfaRecoveryCodeUsed(null, cleanUser, remaining);
    } else {
      if (!totpService.hasActiveTotp(cleanUser)) {
        throw new IllegalStateException("No authenticator app is enrolled for step-up verification.");
      }
      if (!totpService.verifyActiveCode(cleanUser, code)) {
        recordFailedAttempt(cleanUser, "STEP_UP_TOTP");
        throw new IllegalArgumentException("Invalid verification code.");
      }
      PSSystemAuditLogger.mfaChallengeSuccess(null, cleanUser, "STEP_UP_TOTP");
    }

    resetFailures(cleanUser);
    long windowMinutes = readPositiveLong(PROP_STEP_UP_MINUTES, 5L);
    Instant expiresAt = clock.instant().plus(Duration.ofMinutes(windowMinutes));
    session.setAttribute(SESSION_ATTR_STEP_UP_UNTIL, expiresAt);
    return expiresAt.toEpochMilli();
  }

  @Override
  public void requireStepUpIfEnrolled(String userName, HttpSession session) {
    if (StringUtils.isBlank(userName)) {
      throw new SecurityException("Authenticated session required");
    }
    String cleanUser = userName.trim();
    if (!authPolicyService.hasActiveFactor(cleanUser)) {
      return;
    }
    if (readActiveStepUpExpiry(session) == null) {
      throw new PSStepUpRequiredException(
          "Recent multi-factor verification is required before making this security change.");
    }
  }

  @Override
  public PSTotpEnrollmentStart startProfileTotpEnrollment(
      String userName, String label, HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (authPolicyService.hasActiveFactor(cleanUser)) {
      requireStepUpIfEnrolled(cleanUser, session);
    }
    return totpService.startEnrollment(cleanUser, label);
  }

  @Override
  public PSTotpConfirmOutcome confirmProfileTotpEnrollment(
      String userName, String totpId, String code, HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (authPolicyService.hasActiveFactor(cleanUser)) {
      requireStepUpIfEnrolled(cleanUser, session);
    }
    if (!totpService.confirmEnrollment(cleanUser, totpId, code)) {
      return new PSTotpConfirmOutcome(
          false, List.of(), cleanUser, "", "", List.of(), "Invalid authenticator verification code.");
    }
    List<String> recoveryCodes = List.of();
    if (recoveryCodeService.countRemaining(cleanUser) == 0) {
      recoveryCodes = recoveryCodeService.generateAndReplaceCodes(cleanUser);
      PSSystemAuditLogger.mfaRecoveryCodesRegenerated(null, cleanUser, recoveryCodes.size());
    }
    PSSystemAuditLogger.mfaFactorEnrolled(null, cleanUser, "TOTP");
    return new PSTotpConfirmOutcome(
        true, recoveryCodes, cleanUser, "", "", List.of(), "Authenticator app enabled.");
  }

  @Override
  public void disableProfileTotp(String userName, Collection<String> roles, HttpSession session) {
    String cleanUser = requireUserName(userName);
    requireStepUpIfEnrolled(cleanUser, session);
    int passkeys = authSecurityDao.findActivePasskeysByUser(cleanUser).size();
    if (authPolicyService.isMfaRequiredForUser(cleanUser, roles) && passkeys == 0) {
      throw new IllegalStateException(
          "Cannot remove your last MFA factor while multi-factor authentication is required.");
    }
    totpService.disableTotp(cleanUser);
    refreshAccountEnrollmentState(cleanUser);
    PSSystemAuditLogger.mfaFactorRemoved(null, cleanUser, "TOTP");
  }

  @Override
  public List<String> regenerateRecoveryCodes(String userName, HttpSession session) {
    String cleanUser = requireUserName(userName);
    requireStepUpIfEnrolled(cleanUser, session);
    if (!authPolicyService.hasActiveFactor(cleanUser)) {
      throw new IllegalStateException("Enroll an MFA factor before generating recovery codes.");
    }
    List<String> codes = recoveryCodeService.generateAndReplaceCodes(cleanUser);
    PSSystemAuditLogger.mfaRecoveryCodesRegenerated(null, cleanUser, codes.size());
    return codes;
  }

  @Override
  public PSPasskeyOptions startChallengePasskey(String txId, String requestOrigin) {
    PSAuthLoginTx tx =
        loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_CHALLENGE)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Sign-in verification session has expired. Please sign in again."));
    return requirePasskeyService().startAuthentication(tx.getUserName(), requestOrigin);
  }

  @Override
  public PSMfaChallengeOutcome verifyChallengePasskey(
      String txId,
      String challengeTxId,
      String credentialJson,
      String requestOrigin,
      String requestIp) {
    Optional<PSAuthLoginTx> txOpt = loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_CHALLENGE);
    if (txOpt.isEmpty()) {
      return invalidChallengeOutcome(
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session has expired. Please sign in again.");
    }
    PSAuthLoginTx tx = txOpt.get();
    String userName = tx.getUserName();
    enforceVerifyRateLimits(userName, requestIp);
    ensureNotLocked(userName);

    try {
      requirePasskeyService()
          .verifyAuthentication(userName, challengeTxId, credentialJson, requestOrigin);
    } catch (IllegalArgumentException | IllegalStateException ex) {
      return handleChallengeFailure(tx, userName, "PASSKEY", ex.getMessage());
    }

    int consumed =
        authSecurityDao.consumeLoginTxAtomically(
            tx.getTxId(), PSAuthLoginTx.TYPE_MFA_CHALLENGE, Date.from(clock.instant()));
    if (consumed != 1) {
      return invalidChallengeOutcome(
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session was already used or expired.");
    }
    return finalizeChallengeSuccessFactor(tx, userName, "PASSKEY");
  }

  @Override
  public PSPasskeyOptions startEnrollOnlyPasskey(
      String txId, String label, String requestOrigin) {
    PSAuthLoginTx tx =
        loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_ENROLL)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "MFA setup session has expired. Sign in again to continue."));
    return requirePasskeyService()
        .startRegistration(tx.getUserName(), tx.getUserName(), label, requestOrigin);
  }

  @Override
  public PSTotpConfirmOutcome completeEnrollOnlyPasskey(
      String txId,
      String challengeTxId,
      String label,
      String credentialJson,
      String requestOrigin,
      String requestIp) {
    PSAuthLoginTx tx =
        loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_ENROLL)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "MFA setup session has expired. Sign in again to continue."));
    String userName = tx.getUserName();
    enforceVerifyRateLimits(userName, requestIp);
    ensureNotLocked(userName);

    try {
      requirePasskeyService()
          .finishRegistration(userName, challengeTxId, label, credentialJson, requestOrigin);
    } catch (IllegalArgumentException | IllegalStateException ex) {
      recordFailedAttempt(userName, "PASSKEY_ENROLL");
      return new PSTotpConfirmOutcome(false, List.of(), userName, "", "", List.of(), ex.getMessage());
    }

    int updated =
        authSecurityDao.consumeLoginTxAtomically(
            tx.getTxId(), PSAuthLoginTx.TYPE_MFA_ENROLL, Date.from(clock.instant()));
    if (updated != 1) {
      return new PSTotpConfirmOutcome(
          false,
          List.of(),
          userName,
          "",
          "",
          List.of(),
          "MFA setup session is no longer active. Sign in again.");
    }

    resetFailures(userName);
    refreshAccountEnrollmentState(userName);
    List<String> recoveryCodes = issueInitialRecoveryCodesIfNeeded(userName);
    PSSystemAuditLogger.mfaFactorEnrolled(null, userName, "PASSKEY");

    TxPayload payload = parsePayload(tx.getPayloadJson());
    return new PSTotpConfirmOutcome(
        true,
        recoveryCodes,
        userName,
        payload.redirectUrl(),
        payload.locale(),
        payload.roles(),
        "Passkey and recovery codes saved.");
  }

  @Override
  public PSPasskeyOptions startProfilePasskeyStepUp(
      String userName, String requestOrigin, HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (session == null) {
      throw new SecurityException("Authenticated session required");
    }
    return requirePasskeyService().startAuthentication(cleanUser, requestOrigin);
  }

  @Override
  public long verifyProfilePasskeyStepUp(
      String userName,
      String challengeTxId,
      String credentialJson,
      String requestOrigin,
      String requestIp,
      HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (session == null) {
      throw new SecurityException("Authenticated session required");
    }
    enforceVerifyRateLimits(cleanUser, requestIp);
    ensureNotLocked(cleanUser);

    try {
      requirePasskeyService()
          .verifyAuthentication(cleanUser, challengeTxId, credentialJson, requestOrigin);
    } catch (IllegalArgumentException ex) {
      recordFailedAttempt(cleanUser, "STEP_UP_PASSKEY");
      throw ex;
    }

    PSSystemAuditLogger.mfaChallengeSuccess(null, cleanUser, "STEP_UP_PASSKEY");
    resetFailures(cleanUser);
    long windowMinutes = readPositiveLong(PROP_STEP_UP_MINUTES, 5L);
    Instant expiresAt = clock.instant().plus(Duration.ofMinutes(windowMinutes));
    session.setAttribute(SESSION_ATTR_STEP_UP_UNTIL, expiresAt);
    return expiresAt.toEpochMilli();
  }

  @Override
  public List<PSPasskeySummary> listProfilePasskeys(String userName) {
    String cleanUser = requireUserName(userName);
    return requirePasskeyService().listPasskeys(cleanUser);
  }

  @Override
  public boolean canDeleteLastPasskey(String userName, Collection<String> roles) {
    String cleanUser = requireUserName(userName);
    return !authPolicyService.isMfaRequiredForUser(cleanUser, roles)
        || totpService.hasActiveTotp(cleanUser);
  }

  @Override
  public PSPasskeyOptions startProfilePasskeyRegistration(
      String userName,
      String displayName,
      String label,
      String requestOrigin,
      HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (authPolicyService.hasActiveFactor(cleanUser)) {
      requireStepUpIfEnrolled(cleanUser, session);
    }
    return requirePasskeyService().startRegistration(cleanUser, displayName, label, requestOrigin);
  }

  @Override
  public PSPasskeyRegisterOutcome finishProfilePasskeyRegistration(
      String userName,
      String challengeTxId,
      String label,
      String credentialJson,
      String requestOrigin,
      HttpSession session) {
    String cleanUser = requireUserName(userName);
    if (authPolicyService.hasActiveFactor(cleanUser)) {
      requireStepUpIfEnrolled(cleanUser, session);
    }
    PSPasskeyRegisterOutcome outcome =
        requirePasskeyService()
            .finishRegistration(cleanUser, challengeTxId, label, credentialJson, requestOrigin);
    refreshAccountEnrollmentState(cleanUser);
    List<String> recoveryCodes = issueInitialRecoveryCodesIfNeeded(cleanUser);
    PSSystemAuditLogger.mfaFactorEnrolled(null, cleanUser, "PASSKEY");
    return new PSPasskeyRegisterOutcome(
        outcome.registered(), outcome.passkey(), recoveryCodes, outcome.message());
  }

  @Override
  public void renameProfilePasskey(String userName, String passkeyId, String newLabel) {
    requirePasskeyService().renamePasskey(requireUserName(userName), passkeyId, newLabel);
  }

  @Override
  public void deleteProfilePasskey(
      String userName, String passkeyId, Collection<String> roles, HttpSession session) {
    String cleanUser = requireUserName(userName);
    requireStepUpIfEnrolled(cleanUser, session);
    int activePasskeys = authSecurityDao.findActivePasskeysByUser(cleanUser).size();
    if (authPolicyService.isMfaRequiredForUser(cleanUser, roles)
        && !totpService.hasActiveTotp(cleanUser)
        && activePasskeys <= 1) {
      throw new IllegalStateException(
          "Cannot remove your last MFA factor while multi-factor authentication is required.");
    }
    requirePasskeyService().deletePasskey(cleanUser, passkeyId);
    refreshAccountEnrollmentState(cleanUser);
    PSSystemAuditLogger.mfaFactorRemoved(null, cleanUser, "PASSKEY");
  }

  private List<String> issueInitialRecoveryCodesIfNeeded(String cleanUser) {
    if (recoveryCodeService.countRemaining(cleanUser) > 0) {
      return List.of();
    }
    List<String> codes = recoveryCodeService.generateAndReplaceCodes(cleanUser);
    PSSystemAuditLogger.mfaRecoveryCodesRegenerated(null, cleanUser, codes.size());
    return codes;
  }

  private IPSPasskeyService requirePasskeyService() {
    if (passkeyService == null) {
      throw new IllegalStateException("Passkey service is not configured.");
    }
    return passkeyService;
  }

  private static PSMfaChallengeOutcome invalidChallengeOutcome(String status, String msg) {
    return new PSMfaChallengeOutcome(false, status, msg, "", "", "", List.of(), 0L);
  }

  private PSMfaChallengeOutcome handleChallengeFailure(
      PSAuthLoginTx tx, String userName, String factor, String message) {
    int maxAttempts = (int) readPositiveLong(PROP_MAX_FAILED_ATTEMPTS, 5L);
    authSecurityDao.incrementLoginTxAttemptsAtomically(
        tx.getTxId(), maxAttempts, Date.from(clock.instant()));
    recordFailedAttempt(userName, factor);
    return new PSMfaChallengeOutcome(
        false, PSMfaChallengeOutcome.STATUS_INVALID_CODE, message, userName, "", "", List.of(), 0L);
  }

  private PSMfaChallengeOutcome finalizeChallengeSuccessFactor(
      PSAuthLoginTx tx, String userName, String factor) {
    resetFailures(userName);
    long remaining = recoveryCodeService.countRemaining(userName);
    PSSystemAuditLogger.mfaChallengeSuccess(null, userName, factor);
    TxPayload payload = parsePayload(tx.getPayloadJson());
    return new PSMfaChallengeOutcome(
        true,
        PSMfaChallengeOutcome.STATUS_VERIFIED,
        "Verification successful.",
        userName,
        payload.redirectUrl(),
        payload.locale(),
        payload.roles(),
        remaining);
  }

  private PSMfaChallengeOutcome verifyChallengeFactor(
      String txId, String code, String requestIp, boolean recoveryCodeFlow) {
    Optional<PSAuthLoginTx> txOpt = loadActiveTx(txId, PSAuthLoginTx.TYPE_MFA_CHALLENGE);
    if (txOpt.isEmpty()) {
      return new PSMfaChallengeOutcome(
          false,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session has expired. Please sign in again.",
          "",
          "",
          "",
          List.of(),
          0L);
    }

    PSAuthLoginTx tx = txOpt.get();
    String userName = tx.getUserName();
    enforceVerifyRateLimits(userName, requestIp);
    ensureNotLocked(userName);

    boolean verified =
        recoveryCodeFlow
            ? recoveryCodeService.consumeCode(userName, code)
            : totpService.verifyActiveCode(userName, code);
    if (!verified) {
      return handleChallengeInvalidCode(tx, userName, recoveryCodeFlow);
    }

    int consumed =
        authSecurityDao.consumeLoginTxAtomically(
            tx.getTxId(), PSAuthLoginTx.TYPE_MFA_CHALLENGE, Date.from(clock.instant()));
    if (consumed != 1) {
      return new PSMfaChallengeOutcome(
          false,
          PSMfaChallengeOutcome.STATUS_TX_INVALID,
          "Sign-in verification session was already used or expired.",
          "",
          "",
          "",
          List.of(),
          0L);
    }

    return finalizeChallengeSuccess(tx, userName, recoveryCodeFlow);
  }

  private PSMfaChallengeOutcome handleChallengeInvalidCode(
      PSAuthLoginTx tx, String userName, boolean recoveryCodeFlow) {
    int maxAttempts = (int) readPositiveLong(PROP_MAX_FAILED_ATTEMPTS, 5L);
    authSecurityDao.incrementLoginTxAttemptsAtomically(
        tx.getTxId(), maxAttempts, Date.from(clock.instant()));
    String factor = recoveryCodeFlow ? "RECOVERY_CODE" : "TOTP";
    recordFailedAttempt(userName, factor);
    String msg =
        recoveryCodeFlow
            ? "Invalid or already used recovery code."
            : "Invalid authenticator code.";
    return new PSMfaChallengeOutcome(
        false, PSMfaChallengeOutcome.STATUS_INVALID_CODE, msg, userName, "", "", List.of(), 0L);
  }

  private PSMfaChallengeOutcome finalizeChallengeSuccess(
      PSAuthLoginTx tx, String userName, boolean recoveryCodeFlow) {
    resetFailures(userName);
    long remaining = recoveryCodeService.countRemaining(userName);
    if (recoveryCodeFlow) {
      PSSystemAuditLogger.mfaRecoveryCodeUsed(null, userName, remaining);
    } else {
      PSSystemAuditLogger.mfaChallengeSuccess(null, userName, "TOTP");
    }
    TxPayload payload = parsePayload(tx.getPayloadJson());
    return new PSMfaChallengeOutcome(
        true,
        PSMfaChallengeOutcome.STATUS_VERIFIED,
        "Verification successful.",
        userName,
        payload.redirectUrl(),
        payload.locale(),
        payload.roles(),
        remaining);
  }

  private Optional<PSAuthLoginTx> loadActiveTx(String txId, String expectedType) {
    if (StringUtils.isBlank(txId)) {
      return Optional.empty();
    }
    Optional<PSAuthLoginTx> found = authSecurityDao.findLoginTx(txId.trim());
    if (found.isEmpty()) {
      return Optional.empty();
    }
    PSAuthLoginTx tx = found.get();
    if (!PSAuthLoginTx.STATE_PENDING.equals(tx.getState())) {
      return Optional.empty();
    }
    if (expectedType != null && !expectedType.equals(tx.getTxType())) {
      return Optional.empty();
    }
    if (tx.getExpiresAt() == null || !tx.getExpiresAt().toInstant().isAfter(clock.instant())) {
      return Optional.empty();
    }
    int maxAttempts = (int) readPositiveLong(PROP_MAX_FAILED_ATTEMPTS, 5L);
    if (tx.getAttemptCount() >= maxAttempts) {
      return Optional.empty();
    }
    return Optional.of(tx);
  }

  private void enforceVerifyRateLimits(String userName, String requestIp) {
    long ipLimit = readPositiveLong(PROP_RATE_VERIFY_IP, 20L);
    String ipKey = StringUtils.defaultIfBlank(requestIp, "_unknown");
    if (!rateLimiter.tryConsume("mfa_verify_ip", ipKey, ipLimit, Duration.ofMinutes(1))) {
      throw new PSAuthRateLimitException(
          "Too many verification attempts from this address. Please wait a minute.");
    }
    long userLimit = readPositiveLong(PROP_RATE_VERIFY_USER, 10L);
    String userKey = userName.trim().toLowerCase(Locale.ROOT);
    if (!rateLimiter.tryConsume("mfa_verify_user", userKey, userLimit, Duration.ofMinutes(1))) {
      throw new PSAuthRateLimitException(
          "Too many verification attempts for this account. Please wait a minute.");
    }
  }

  private void ensureNotLocked(String userName) {
    Optional<PSAccountSecurity> secOpt = authSecurityDao.findAccountSecurity(userName);
    if (secOpt.isEmpty()) {
      return;
    }
    Date lockedUntil = secOpt.get().getLockedUntil();
    if (lockedUntil != null && lockedUntil.toInstant().isAfter(clock.instant())) {
      throw new PSMfaLockoutException(
          "Too many failed verification attempts. Please try again later.",
          lockedUntil.toInstant());
    }
  }

  private void recordFailedAttempt(String userName, String factor) {
    PSAccountSecurity sec = loadOrCreateSecurity(userName);
    Instant now = clock.instant();
    int maxAttempts = (int) readPositiveLong(PROP_MAX_FAILED_ATTEMPTS, 5L);
    long lockoutMinutes = readPositiveLong(PROP_LOCKOUT_MINUTES, 15L);

    int nextAttempts = sec.getFailedLoginCount() + 1;
    sec.setFailedLoginCount(nextAttempts);
    if (nextAttempts >= maxAttempts) {
      Instant until = now.plus(Duration.ofMinutes(lockoutMinutes));
      sec.setLockedUntil(Date.from(until));
      authSecurityDao.saveAccountSecurity(sec);
      PSSystemAuditLogger.mfaLockout(null, userName, nextAttempts);
      throw new PSMfaLockoutException(
          "Too many failed verification attempts. Account temporarily locked.", until);
    }
    authSecurityDao.saveAccountSecurity(sec);
    PSSystemAuditLogger.mfaChallengeFailure(null, userName, factor, "INVALID_CODE");
  }

  private void resetFailures(String userName) {
    Optional<PSAccountSecurity> secOpt = authSecurityDao.findAccountSecurity(userName);
    if (secOpt.isEmpty()) {
      return;
    }
    PSAccountSecurity sec = secOpt.get();
    if (sec.getFailedLoginCount() != 0 || sec.getLockedUntil() != null) {
      sec.setFailedLoginCount(0);
      sec.setLockedUntil(null);
      authSecurityDao.saveAccountSecurity(sec);
    }
  }

  private void refreshAccountEnrollmentState(String userName) {
    Optional<PSAccountSecurity> secOpt = authSecurityDao.findAccountSecurity(userName);
    if (secOpt.isEmpty()) {
      return;
    }
    PSAccountSecurity sec = secOpt.get();
    sec.setMfaEnrollmentState(
        authPolicyService.hasActiveFactor(userName)
            ? PSAccountSecurity.MFA_STATE_ENROLLED
            : PSAccountSecurity.MFA_STATE_NONE);
    authSecurityDao.saveAccountSecurity(sec);
  }

  private PSAccountSecurity loadOrCreateSecurity(String userName) {
    return authSecurityDao
        .findAccountSecurity(userName)
        .orElseGet(
            () -> {
              PSAccountSecurity created = new PSAccountSecurity();
              created.setUserName(userName);
              return created;
            });
  }

  private Instant readActiveStepUpExpiry(HttpSession session) {
    if (session == null) {
      return null;
    }
    Object attr = session.getAttribute(SESSION_ATTR_STEP_UP_UNTIL);
    Instant expiry = null;
    if (attr instanceof Instant inst) {
      expiry = inst;
    } else if (attr instanceof Date dt) {
      expiry = dt.toInstant();
    } else if (attr instanceof Long ms) {
      expiry = Instant.ofEpochMilli(ms);
    }
    if (expiry == null || !expiry.isAfter(clock.instant())) {
      return null;
    }
    return expiry;
  }

  private PSMfaTxStatus buildTxStatus(PSAuthLoginTx tx, TxPayload payload) {
    String user = tx.getUserName();
    return new PSMfaTxStatus(
        true,
        tx.getTxId(),
        tx.getTxType(),
        user,
        payload.redirectUrl(),
        payload.locale(),
        payload.roles(),
        totpService.hasActiveTotp(user),
        !authSecurityDao.findActivePasskeysByUser(user).isEmpty(),
        recoveryCodeService.countRemaining(user),
        tx.getExpiresAt() != null ? tx.getExpiresAt().toInstant() : null);
  }

  private static PSMfaTxStatus invalidTxStatus(String txId) {
    return new PSMfaTxStatus(false, txId, "", "", "", "", List.of(), false, false, 0L, null);
  }

  private String generateTxId() {
    byte[] raw = new byte[TX_ID_BYTES];
    secureRandom.nextBytes(raw);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }

  private static String serializePayload(
      String redirectUrl, String locale, Collection<String> roles) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("redirectUrl", StringUtils.defaultString(redirectUrl, "/cm/app/"));
    map.put("locale", StringUtils.defaultIfBlank(locale, "en-us"));
    map.put("roles", roles != null ? List.copyOf(roles) : List.of());
    return JSON.writeValueAsString(map);
  }

  private static TxPayload parsePayload(String payloadJson) {
    if (StringUtils.isBlank(payloadJson)) {
      return new TxPayload("/cm/app/", "en-us", List.of());
    }
    try {
      Map<String, Object> parsed = JSON.readValue(payloadJson, MAP_TYPE);
      if (parsed == null) {
        return new TxPayload("/cm/app/", "en-us", List.of());
      }
      String redirectUrl = stringField(parsed.get("redirectUrl"), "/cm/app/");
      String locale = stringField(parsed.get("locale"), "en-us");
      List<String> roles = extractRoles(parsed.get("roles"));
      return new TxPayload(redirectUrl, locale, roles);
    } catch (RuntimeException ex) {
      return new TxPayload("/cm/app/", "en-us", List.of());
    }
  }

  private static String stringField(Object val, String fallback) {
    return val instanceof String s && StringUtils.isNotBlank(s) ? s : fallback;
  }

  private static List<String> extractRoles(Object raw) {
    if (!(raw instanceof Collection<?> col)) {
      return List.of();
    }
    return col.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .filter(StringUtils::isNotBlank)
        .toList();
  }

  private long readPositiveLong(String key, long defaultValue) {
    Properties props = safeProps(serverPropsSupplier.get());
    String raw = StringUtils.trimToNull(props.getProperty(key));
    if (raw == null) {
      return defaultValue;
    }
    try {
      long parsed = Long.parseLong(raw);
      return parsed > 0 ? parsed : defaultValue;
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private static String requireUserName(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    return userName.trim();
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }

  private record TxPayload(String redirectUrl, String locale, List<String> roles) {}
}
