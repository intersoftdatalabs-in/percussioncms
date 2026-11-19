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
package com.percussion.services.auth.recovery.impl;

import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSPasswordHandler;
import com.percussion.security.PSPasswordPolicyValidator;
import com.percussion.server.PSServer;
import com.percussion.server.PSUserSessionManager;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.data.PSMfaTotp;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailException;
import com.percussion.services.auth.mail.impl.PSAuthMailService;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.services.auth.recovery.IPSAccountRecoveryService;
import com.percussion.services.auth.recovery.PSAdminRecoveryIssueResult;
import com.percussion.services.auth.recovery.PSAdminRecoveryRedeemResult;
import com.percussion.services.auth.recovery.PSForgotPasswordResult;
import com.percussion.services.auth.recovery.PSPasswordResetResult;
import com.percussion.services.auth.token.IPSAuthTokenService;
import com.percussion.services.auth.token.PSConsumedAuthToken;
import com.percussion.services.auth.token.PSIssuedAuthToken;
import com.percussion.system.utils.PSBaseBean;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Coordinates password policy resolution, enumeration-safe password reset recovery, multi-session
 * invalidation on password updates, and audited Administrator break-glass MFA recovery.
 */
@PSBaseBean("sys_accountRecoveryService")
public class PSAccountRecoveryService implements IPSAccountRecoveryService {

  private static final Logger log = LogManager.getLogger(PSAccountRecoveryService.class);

  public static final String PROP_PUBLIC_BASE_URL = "auth.publicBaseUrl";
  // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #50) - server.properties rate-limit key name constant, not a secret.
  public static final String PROP_RATE_RESET_IP = "auth.rateLimit.passwordResetPerIpPerHour";
  public static final String PROP_RATE_RESET_ACCOUNT =
      "auth.rateLimit.passwordResetPerAccountPerHour";

  static final String GENERIC_FORGOT_PASSWORD_MSG =
      "If an eligible account matches your request, we have sent password reset instructions"
          + " to its verified email address.";

  private static final int MIN_VERIFICATION_NOTE_LEN = 10;
  private static final Set<String> SYSTEM_SERVICE_USERS =
      Set.of("percussionadmin", "rxserver", "internal");

  private final IPSAuthSecurityDao authSecurityDao;
  private final IPSAuthTokenService tokenService;
  private final IPSAuthMailService authMailService;
  private final IPSAuthRateLimiter rateLimiter;
  private final IPSMfaService mfaService;
  private final Supplier<Properties> serverPropsSupplier;
  private final Clock clock;
  private final Consumer<String> sessionRevoker;

  @Autowired
  public PSAccountRecoveryService(
      IPSAuthSecurityDao authSecurityDao,
      IPSAuthTokenService tokenService,
      IPSAuthMailService authMailService,
      IPSAuthRateLimiter rateLimiter,
      IPSMfaService mfaService) {
    this(
        authSecurityDao,
        tokenService,
        authMailService,
        rateLimiter,
        mfaService,
        PSAccountRecoveryService::defaultServerProperties,
        Clock.systemUTC(),
        PSUserSessionManager::releaseSessionsForUser);
  }

  /** Package-visible constructor for deterministic unit and concurrency tests. */
  PSAccountRecoveryService(
      IPSAuthSecurityDao authSecurityDao,
      IPSAuthTokenService tokenService,
      IPSAuthMailService authMailService,
      IPSAuthRateLimiter rateLimiter,
      IPSMfaService mfaService,
      Supplier<Properties> serverPropsSupplier,
      Clock clock,
      Consumer<String> sessionRevoker) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    this.authMailService = Objects.requireNonNull(authMailService, "authMailService");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.mfaService = Objects.requireNonNull(mfaService, "mfaService");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "sessionRevoker");
  }

  @Override
  public int resolvePasswordMinLength() {
    return PSPasswordPolicyValidator.resolveMinLength(safeProps(serverPropsSupplier.get()));
  }

  @Override
  public PSForgotPasswordResult requestPasswordReset(String identifier, String requestIp) {
    enforceRecoveryIpLimit("pw_reset_ip", PROP_RATE_RESET_IP, requestIp);

    String cleanIdentifier = StringUtils.trimToEmpty(identifier);
    if (cleanIdentifier.isEmpty()) {
      return genericForgotAck();
    }

    Optional<PSAccountSecurity> targetOpt = resolveEligibleAccount(cleanIdentifier, requestIp);
    if (targetOpt.isEmpty()) {
      return genericForgotAck();
    }
    PSAccountSecurity target = targetOpt.get();
    if (!allowPasswordResetForAccount(target.getUserName())) {
      return genericForgotAck();
    }
    dispatchResetOrDirectoryGuidance(target, requestIp);
    return genericForgotAck();
  }

  @Override
  public PSPasswordResetResult completePasswordReset(
      String rawToken, String newPassword, String requestIp) {
    enforceRecoveryIpLimit("pw_reset_ip", PROP_RATE_RESET_IP, requestIp);

    Optional<String> policyError =
        PSPasswordPolicyValidator.validate(newPassword, resolvePasswordMinLength());
    if (policyError.isPresent()) {
      return new PSPasswordResetResult(
          false, PSPasswordResetResult.STATUS_PASSWORD_POLICY_VIOLATION, policyError.get());
    }
    if (StringUtils.isBlank(rawToken)) {
      return invalidOrExpiredResetResult();
    }

    Optional<PSConsumedAuthToken> consumedOpt =
        tokenService.consumeToken(rawToken, PSAuthToken.TYPE_PASSWORD_RESET);
    if (consumedOpt.isEmpty()) {
      PSSystemAuditLogger.passwordResetEvent(
          "unknown", "invalid_or_expired_token", requestIp, AuditOutcome.FAILURE);
      return invalidOrExpiredResetResult();
    }

    PSConsumedAuthToken consumed = consumedOpt.get();
    String targetUser = consumed.userName();
    Optional<String> internalUserId = authSecurityDao.findInternalLoginUserId(targetUser);
    if (internalUserId.isEmpty()) {
      PSSystemAuditLogger.passwordResetEvent(
          targetUser, "unsupported_provider", requestIp, AuditOutcome.FAILURE);
      return new PSPasswordResetResult(
          false,
          PSPasswordResetResult.STATUS_UNSUPPORTED_PROVIDER,
          "Password reset is managed by your directory or single sign-on provider.");
    }

    return applyInternalPasswordReset(targetUser, internalUserId.get(), newPassword, requestIp);
  }

  @Override
  public PSAdminRecoveryIssueResult issueAdminBreakGlassRecovery(
      String adminActor,
      String targetUserName,
      String verificationNote,
      boolean resetImmediately,
      String requestIp,
      HttpSession adminSession) {
    String cleanAdmin = requireNonBlank(adminActor, "Authenticated administrator is required.");
    String cleanTarget = requireNonBlank(targetUserName, "Target userName is required.");
    String cleanNote = validateVerificationNote(verificationNote);

    if (cleanAdmin.equalsIgnoreCase(cleanTarget)) {
      throw new IllegalArgumentException(
          "Administrators cannot perform break-glass MFA recovery on their own account.");
    }
    PSAccountSecurity targetSec = authSecurityDao.findAccountSecurity(cleanTarget).orElse(null);
    ensureEligibleBreakGlassTarget(cleanTarget, targetSec);

    mfaService.requireStepUpIfEnrolled(cleanAdmin, adminSession);

    if (resetImmediately) {
      executeImmediateBreakGlassReset(cleanAdmin, cleanTarget, cleanNote, requestIp);
      return new PSAdminRecoveryIssueResult(
          true,
          cleanTarget,
          PSAdminRecoveryIssueResult.MODE_IMMEDIATE_MFA_RESET,
          PSAdminRecoveryIssueResult.DELIVERY_DIRECT,
          "",
          "MFA factors were reset immediately for "
              + cleanTarget
              + ". All active sessions were signed out and MFA re-enrollment is required.");
    }

    return issueBreakGlassRecoveryLink(cleanAdmin, cleanTarget, targetSec, cleanNote, requestIp);
  }

  @Override
  public PSAdminRecoveryRedeemResult redeemAdminBreakGlass(String rawToken, String requestIp) {
    enforceRecoveryIpLimit("admin_recovery_ip", PROP_RATE_RESET_IP, requestIp);
    if (StringUtils.isBlank(rawToken)) {
      return invalidOrExpiredBreakGlassResult();
    }

    Optional<PSConsumedAuthToken> consumedOpt =
        tokenService.consumeToken(rawToken, PSAuthToken.TYPE_ADMIN_BREAK_GLASS);
    if (consumedOpt.isEmpty()) {
      PSSystemAuditLogger.adminRecoveryEvent(
          "unknown", "unknown", "invalid_or_expired_token", requestIp, AuditOutcome.FAILURE);
      return invalidOrExpiredBreakGlassResult();
    }

    PSConsumedAuthToken consumed = consumedOpt.get();
    String targetUser = consumed.userName();
    String actorAdmin = StringUtils.defaultIfBlank(consumed.createdBy(), "Admin");
    Date now = Date.from(clock.instant());

    revokeAllMfaFactorsAndLockout(targetUser, now);
    authSecurityDao.incrementSessionVersion(targetUser);
    sessionRevoker.accept(targetUser);

    PSSystemAuditLogger.adminRecoveryEvent(
        actorAdmin, targetUser, "redeemed", requestIp, AuditOutcome.SUCCESS);
    return new PSAdminRecoveryRedeemResult(
        true,
        PSAdminRecoveryRedeemResult.STATUS_REDEEMED,
        "Your two-step verification factors have been reset and active sessions signed out."
            + " Sign in with your password to set up new two-step verification.");
  }

  private PSPasswordResetResult applyInternalPasswordReset(
      String accountUser, String internalLoginId, String newPassword, String requestIp) {
    String encoded;
    try {
      encoded = PSPasswordHandler.getHashedPassword(newPassword);
    } catch (PSEncryptionException ex) {
      log.error("Password hashing failed during reset for {}: {}", accountUser, ex.getMessage());
      PSSystemAuditLogger.passwordResetEvent(
          accountUser, "hash_failed", requestIp, AuditOutcome.FAILURE);
      return new PSPasswordResetResult(
          false, PSPasswordResetResult.STATUS_FAILED, "Unable to update password.");
    }

    int updated = authSecurityDao.updateInternalUserPassword(internalLoginId, encoded);
    if (updated != 1) {
      log.error("Expected 1 USERLOGIN update during reset for {}, got {}", accountUser, updated);
      PSSystemAuditLogger.passwordResetEvent(
          accountUser, "persist_failed", requestIp, AuditOutcome.FAILURE);
      return new PSPasswordResetResult(
          false, PSPasswordResetResult.STATUS_FAILED, "Unable to update password.");
    }

    Instant now = clock.instant();
    Date nowDate = Date.from(now);
    authSecurityDao.recordPasswordChangeAndBumpSessionVersion(accountUser, nowDate);
    authSecurityDao.invalidateUnconsumedTokens(accountUser, PSAuthToken.TYPE_PASSWORD_RESET, now);
    sessionRevoker.accept(accountUser);

    PSSystemAuditLogger.passwordResetEvent(
        accountUser, "completed", requestIp, AuditOutcome.SUCCESS);
    return new PSPasswordResetResult(
        true,
        PSPasswordResetResult.STATUS_RESET,
        "Your password has been updated and active sessions have been signed out."
            + " Any enrolled two-step verification methods remain active.");
  }

  private PSAdminRecoveryIssueResult issueBreakGlassRecoveryLink(
      String cleanAdmin,
      String cleanTarget,
      PSAccountSecurity targetSec,
      String cleanNote,
      String requestIp) {
    if (targetSec == null
        || !targetSec.isEmailVerified()
        || !PSAuthMailService.isValidSingleEmail(targetSec.getEmailNormalized())) {
      throw new IllegalStateException(
          "Target account does not have a verified email address. Verify the address first or use"
              + " immediate MFA reset after out-of-band identity verification.");
    }
    if (!authMailService.resolveEffectiveConfig().isConfigured()) {
      throw new IllegalStateException(
          "Authentication mail is not configured. Configure auth.mail.* settings first or use"
              + " immediate MFA reset.");
    }
    String baseUrl = resolveOptionalPublicBaseUrl();
    if (StringUtils.isBlank(baseUrl)) {
      throw new IllegalStateException(
          "auth.publicBaseUrl must be configured before sending break-glass recovery links.");
    }

    Duration ttl = tokenService.resolveTtl(PSAuthToken.TYPE_ADMIN_BREAK_GLASS);
    PSIssuedAuthToken issued =
        tokenService.issueToken(
            PSAuthToken.TYPE_ADMIN_BREAK_GLASS,
            cleanTarget,
            targetSec.getEmailNormalized(),
            cleanAdmin,
            requestIp,
            ttl);
    String encodedToken = URLEncoder.encode(issued.rawToken(), StandardCharsets.UTF_8);
    String recoveryUrl = baseUrl + "/auth/admin-recovery?token=" + encodedToken;
    String body = buildBreakGlassEmailBody(cleanTarget, cleanAdmin, recoveryUrl, ttl);

    try {
      authMailService.sendTextEmail(
          targetSec.getEmailNormalized(), "Percussion CMS break-glass MFA recovery", body);
    } catch (PSAuthMailException ex) {
      PSSystemAuditLogger.adminRecoveryEvent(
          cleanAdmin, cleanTarget, "link_send_failed", requestIp, AuditOutcome.FAILURE);
      throw new IllegalStateException(
          "Unable to send break-glass recovery email: " + ex.getMessage(), ex);
    }

    log.info(
        "Admin {} issued break-glass MFA recovery link for {} (verification note length={})",
        cleanAdmin,
        cleanTarget,
        cleanNote.length());
    PSSystemAuditLogger.adminRecoveryEvent(
        cleanAdmin, cleanTarget, "link_issued", requestIp, AuditOutcome.SUCCESS);

    return new PSAdminRecoveryIssueResult(
        true,
        cleanTarget,
        PSAdminRecoveryIssueResult.MODE_RECOVERY_LINK,
        PSAdminRecoveryIssueResult.DELIVERY_EMAIL,
        issued.expiresAt().toString(),
        "Break-glass MFA recovery link sent to the verified email address for "
            + cleanTarget
            + ".");
  }

  private void executeImmediateBreakGlassReset(
      String cleanAdmin, String cleanTarget, String cleanNote, String requestIp) {
    Instant now = clock.instant();
    Date nowDate = Date.from(now);
    revokeAllMfaFactorsAndLockout(cleanTarget, nowDate);
    authSecurityDao.invalidateUnconsumedTokens(cleanTarget, PSAuthToken.TYPE_ADMIN_BREAK_GLASS, now);
    authSecurityDao.incrementSessionVersion(cleanTarget);
    sessionRevoker.accept(cleanTarget);

    log.info(
        "Admin {} executed immediate break-glass MFA reset for {} (verification note length={})",
        cleanAdmin,
        cleanTarget,
        cleanNote.length());
    PSSystemAuditLogger.adminRecoveryEvent(
        cleanAdmin, cleanTarget, "immediate_mfa_reset", requestIp, AuditOutcome.SUCCESS);
  }

  private void revokeAllMfaFactorsAndLockout(String targetUser, Date now) {
    authSecurityDao.revokeTotpByUser(targetUser, PSMfaTotp.STATUS_ACTIVE);
    authSecurityDao.revokeTotpByUser(targetUser, PSMfaTotp.STATUS_PENDING);
    authSecurityDao.revokeAllPasskeysByUser(targetUser);
    authSecurityDao.invalidateUnusedRecoveryCodesByUser(targetUser, now);

    PSAccountSecurity sec =
        authSecurityDao
            .findAccountSecurity(targetUser)
            .orElseGet(
                () -> {
                  PSAccountSecurity created = new PSAccountSecurity();
                  created.setUserName(targetUser);
                  return created;
                });
    sec.setMfaEnrollmentState(PSAccountSecurity.MFA_STATE_REQUIRED);
    sec.setFailedLoginCount(0);
    sec.setLockedUntil(null);
    sec.setRecoveryLockedUntil(null);
    sec.setUpdatedAt(now);
    authSecurityDao.saveAccountSecurity(sec);
  }

  private Optional<PSAccountSecurity> resolveEligibleAccount(
      String cleanIdentifier, String requestIp) {
    if (cleanIdentifier.contains("@")) {
      return resolveAccountByVerifiedEmail(cleanIdentifier, requestIp);
    }
    Optional<PSAccountSecurity> found = authSecurityDao.findAccountSecurity(cleanIdentifier);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    PSAccountSecurity sec = found.get();
    if (!isEligibleRecoveryAccount(sec)) {
      return Optional.empty();
    }
    return Optional.of(sec);
  }

  private Optional<PSAccountSecurity> resolveAccountByVerifiedEmail(
      String rawEmail, String requestIp) {
    if (!PSAuthMailService.isValidSingleEmail(rawEmail)) {
      return Optional.empty();
    }
    List<PSAccountSecurity> matches =
        authSecurityDao.findVerifiedByNormalizedEmail(rawEmail.toLowerCase(Locale.ROOT));
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    if (matches.size() > 1) {
      log.warn("Ambiguous password-reset email request suppressed ({} verified accounts)", matches.size());
      PSSystemAuditLogger.passwordResetEvent(
          "system", "password_reset_ambiguous", requestIp, AuditOutcome.FAILURE);
      return Optional.empty();
    }
    PSAccountSecurity sec = matches.get(0);
    return isEligibleRecoveryAccount(sec) ? Optional.of(sec) : Optional.empty();
  }

  private boolean isEligibleRecoveryAccount(PSAccountSecurity sec) {
    if (sec == null || isServiceAccount(sec.getUserName(), sec)) {
      return false;
    }
    return sec.isEmailVerified()
        && PSAuthMailService.isValidSingleEmail(sec.getEmailNormalized());
  }

  private void dispatchResetOrDirectoryGuidance(PSAccountSecurity target, String requestIp) {
    if (!authMailService.resolveEffectiveConfig().isConfigured()) {
      log.warn("Password reset email skipped for {}: auth.mail is not configured", target.getUserName());
      return;
    }

    Optional<String> internalUid = authSecurityDao.findInternalLoginUserId(target.getUserName());
    if (internalUid.isEmpty()) {
      sendDirectoryGuidanceEmail(target, requestIp);
      return;
    }

    String baseUrl = resolveOptionalPublicBaseUrl();
    if (StringUtils.isBlank(baseUrl)) {
      log.warn(
          "Password reset link skipped for {}: auth.publicBaseUrl is not configured",
          target.getUserName());
      return;
    }

    Duration ttl = tokenService.resolveTtl(PSAuthToken.TYPE_PASSWORD_RESET);
    PSIssuedAuthToken token =
        tokenService.issueToken(
            PSAuthToken.TYPE_PASSWORD_RESET,
            target.getUserName(),
            target.getEmailNormalized(),
            target.getUserName(),
            requestIp,
            ttl);
    String encodedToken = URLEncoder.encode(token.rawToken(), StandardCharsets.UTF_8);
    String resetUrl = baseUrl + "/auth/reset-password?token=" + encodedToken;
    String body =
        "Hello "
            + target.getUserName()
            + ",\n\n"
            + "A password reset was requested for your Percussion CMS account.\n"
            + "Open the link below to choose a new password:\n\n"
            + resetUrl
            + "\n\n"
            + "This link expires in "
            + ttl.toMinutes()
            + " minutes and can only be used once.\n"
            + "Note: Resetting your password signs out active sessions and preserves any"
            + " enrolled two-step verification methods (authenticator app or passkeys).\n"
            + "If you did not request this reset, you can safely ignore this message.\n";
    try {
      authMailService.sendTextEmail(
          target.getEmailNormalized(), "Reset your Percussion CMS password", body);
      PSSystemAuditLogger.passwordResetEvent(
          target.getUserName(), "requested", requestIp, AuditOutcome.SUCCESS);
    } catch (PSAuthMailException ex) {
      log.error("Failed to send password reset email for {}: {}", target.getUserName(), ex.getMessage());
      PSSystemAuditLogger.passwordResetEvent(
          target.getUserName(), "mail_send_failed", requestIp, AuditOutcome.FAILURE);
    }
  }

  private void sendDirectoryGuidanceEmail(PSAccountSecurity target, String requestIp) {
    String baseUrl = resolveOptionalPublicBaseUrl();
    String signInLine =
        StringUtils.isNotBlank(baseUrl) ? "\nSign in: " + baseUrl + "/login\n" : "";
    String body =
        "Hello "
            + target.getUserName()
            + ",\n\n"
            + "A password reset was requested for your Percussion CMS account.\n"
            + "Your account authenticates through your organization's external directory or"
            + " single sign-on (SSO) identity provider rather than a local CMS password.\n"
            + "Use your organization's password recovery portal or contact your IT helpdesk"
            + " to reset your password.\n"
            + signInLine;
    try {
      authMailService.sendTextEmail(
          target.getEmailNormalized(), "Percussion CMS password recovery guidance", body);
      PSSystemAuditLogger.passwordResetEvent(
          target.getUserName(), "directory_redirect", requestIp, AuditOutcome.SUCCESS);
    } catch (PSAuthMailException ex) {
      log.error(
          "Failed to send directory recovery guidance email for {}: {}",
          target.getUserName(),
          ex.getMessage());
      PSSystemAuditLogger.passwordResetEvent(
          target.getUserName(), "directory_redirect_mail_failed", requestIp, AuditOutcome.FAILURE);
    }
  }

  private static String buildBreakGlassEmailBody(
      String targetUser, String adminActor, String recoveryUrl, Duration ttl) {
    return "Hello "
        + targetUser
        + ",\n\n"
        + "An administrator ("
        + adminActor
        + ") has approved a break-glass two-step verification reset for your Percussion CMS"
        + " account.\n"
        + "Open the link below and confirm the reset to clear your existing MFA factors and"
        + " active sessions so you can enroll a new authenticator app or passkey on next"
        + " sign-in:\n\n"
        + recoveryUrl
        + "\n\n"
        + "This single-use link expires in "
        + ttl.toMinutes()
        + " minutes.\n"
        + "If you did not request administrator assistance, contact your administrator"
        + " immediately.\n";
  }

  private void enforceRecoveryIpLimit(String namespace, String propKey, String requestIp) {
    long limit = readPositiveLong(propKey, 15L);
    String key = StringUtils.defaultIfBlank(requestIp, "_unknown");
    if (!rateLimiter.tryConsume(namespace, key, limit, Duration.ofHours(1))) {
      throw new PSAuthRateLimitException();
    }
  }

  private boolean allowPasswordResetForAccount(String userName) {
    long limit = readPositiveLong(PROP_RATE_RESET_ACCOUNT, 5L);
    return rateLimiter.tryConsume(
        "pw_reset_account", userName.trim().toLowerCase(Locale.ROOT), limit, Duration.ofHours(1));
  }

  private static void ensureEligibleBreakGlassTarget(
      String targetUserName, PSAccountSecurity targetSec) {
    if (isServiceAccount(targetUserName, targetSec)) {
      throw new IllegalArgumentException(
          "Non-interactive service accounts do not use break-glass MFA recovery.");
    }
  }

  private static boolean isServiceAccount(String userName, PSAccountSecurity sec) {
    if (StringUtils.isNotBlank(userName)
        && SYSTEM_SERVICE_USERS.contains(userName.trim().toLowerCase(Locale.ROOT))) {
      return true;
    }
    return sec != null
        && PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE.equals(sec.getAccountPolicy());
  }

  private static String validateVerificationNote(String verificationNote) {
    String clean = StringUtils.trimToEmpty(verificationNote);
    if (clean.length() < MIN_VERIFICATION_NOTE_LEN) {
      throw new IllegalArgumentException(
          "A verification note of at least "
              + MIN_VERIFICATION_NOTE_LEN
              + " characters is required for break-glass recovery.");
    }
    return clean;
  }

  private static String requireNonBlank(String value, String message) {
    String clean = StringUtils.trimToEmpty(value);
    if (clean.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return clean;
  }

  private String resolveOptionalPublicBaseUrl() {
    Properties props = safeProps(serverPropsSupplier.get());
    String raw = StringUtils.trimToEmpty(props.getProperty(PROP_PUBLIC_BASE_URL));
    if (raw.isEmpty()) {
      return "";
    }
    try {
      URI uri = new URI(raw);
      String scheme = StringUtils.lowerCase(uri.getScheme(), Locale.ROOT);
      if (!uri.isAbsolute()
          || StringUtils.isBlank(uri.getHost())
          || (!"https".equals(scheme) && !"http".equals(scheme))) {
        return "";
      }
      return StringUtils.stripEnd(raw, "/");
    } catch (URISyntaxException ex) {
      return "";
    }
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

  private static PSForgotPasswordResult genericForgotAck() {
    return new PSForgotPasswordResult(true, GENERIC_FORGOT_PASSWORD_MSG);
  }

  private static PSPasswordResetResult invalidOrExpiredResetResult() {
    return new PSPasswordResetResult(
        false,
        PSPasswordResetResult.STATUS_INVALID_OR_EXPIRED,
        "This password reset link is invalid or has expired. Request a new password reset.");
  }

  private static PSAdminRecoveryRedeemResult invalidOrExpiredBreakGlassResult() {
    return new PSAdminRecoveryRedeemResult(
        false,
        PSAdminRecoveryRedeemResult.STATUS_INVALID_OR_EXPIRED,
        "This break-glass recovery link is invalid or has expired. Request a new recovery"
            + " action from your administrator.");
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }
}
