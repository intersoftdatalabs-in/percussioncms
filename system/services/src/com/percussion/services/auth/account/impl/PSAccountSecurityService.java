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
package com.percussion.services.auth.account.impl;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.codes.UserManagementErrorCodes;
import com.percussion.security.PSThreadRequestUtils;
import com.percussion.server.PSServer;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailChangeResult;
import com.percussion.services.auth.account.PSEmailVerificationStatus;
import com.percussion.services.auth.account.PSEmailVerifyResult;
import com.percussion.services.auth.account.PSForgotUsernameResult;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailException;
import com.percussion.services.auth.mail.impl.PSAuthMailService;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.services.auth.token.IPSAuthTokenService;
import com.percussion.services.auth.token.PSConsumedAuthToken;
import com.percussion.services.auth.token.PSIssuedAuthToken;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.utils.request.PSRequestInfo;
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

/** Implements verified account email lifecycles, staged changes, and username recovery. */
@PSBaseBean("sys_accountSecurityService")
public class PSAccountSecurityService implements IPSAccountSecurityService {

  private static final Logger log = LogManager.getLogger(PSAccountSecurityService.class);

  public static final String PROP_EMAIL_REQUIRED = "auth.email.required";
  public static final String PROP_UNIQUE_EMAIL_RECOVERY = "auth.email.uniqueForRecovery";
  public static final String PROP_DIRECTORY_TRUST_VERIFIED = "auth.directory.trustEmailVerified";
  public static final String PROP_PUBLIC_BASE_URL = "auth.publicBaseUrl";
  public static final String PROP_RATE_FORGOT_USER_IP = "auth.rateLimit.forgotUsernamePerIpPerHour";
  public static final String PROP_RATE_FORGOT_USER_EMAIL =
      "auth.rateLimit.forgotUsernamePerEmailPerHour";
  public static final String PROP_RATE_EMAIL_RESEND =
      "auth.rateLimit.emailVerifyResendPerUserPerHour";

  static final String GENERIC_FORGOT_USERNAME_MSG =
      "If a verified account matches that email address, we have sent a username reminder.";

  private static final Set<String> SYSTEM_SERVICE_USERS =
      Set.of("percussionadmin", "rxserver", "internal");

  private final IPSAuthSecurityDao authSecurityDao;
  private final IPSAuthTokenService tokenService;
  private final IPSAuthMailService authMailService;
  private final IPSAuthRateLimiter rateLimiter;
  private final IPSBackEndRoleMgr backEndRoleMgr;
  private final Supplier<Properties> serverPropsSupplier;
  private final Clock clock;
  private final Consumer<Runnable> privilegedRoleRunner;

  @Autowired
  public PSAccountSecurityService(
      IPSAuthSecurityDao authSecurityDao,
      IPSAuthTokenService tokenService,
      IPSAuthMailService authMailService,
      IPSAuthRateLimiter rateLimiter,
      IPSBackEndRoleMgr backEndRoleMgr) {
    this(
        authSecurityDao,
        tokenService,
        authMailService,
        rateLimiter,
        backEndRoleMgr,
        PSAccountSecurityService::defaultServerProperties,
        Clock.systemUTC(),
        PSAccountSecurityService::runPrivilegedRoleUpdate);
  }

  /** Package-visible constructor for deterministic unit tests. */
  PSAccountSecurityService(
      IPSAuthSecurityDao authSecurityDao,
      IPSAuthTokenService tokenService,
      IPSAuthMailService authMailService,
      IPSAuthRateLimiter rateLimiter,
      IPSBackEndRoleMgr backEndRoleMgr,
      Supplier<Properties> serverPropsSupplier,
      Clock clock,
      Consumer<Runnable> privilegedRoleRunner) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    this.authMailService = Objects.requireNonNull(authMailService, "authMailService");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.backEndRoleMgr = Objects.requireNonNull(backEndRoleMgr, "backEndRoleMgr");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.privilegedRoleRunner =
        Objects.requireNonNull(privilegedRoleRunner, "privilegedRoleRunner");
  }

  @Override
  public boolean isEmailRequired() {
    return readBoolean(PROP_EMAIL_REQUIRED, true);
  }

  @Override
  public boolean isSystemServiceUser(String userName) {
    if (StringUtils.isBlank(userName)) {
      return false;
    }
    return SYSTEM_SERVICE_USERS.contains(userName.trim().toLowerCase(Locale.ROOT));
  }

  @Override
  public PSEmailVerificationStatus getEmailStatus(
      String userName, String currentSubjectEmail, boolean isDirectoryUser) {
    String cleanUser = requireUserName(userName);
    String subjectEmail = StringUtils.trimToEmpty(currentSubjectEmail);
    PSAccountSecurity sec = authSecurityDao.findAccountSecurity(cleanUser).orElse(null);

    if (isDirectoryUser) {
      sec = syncDirectoryViewIfChanged(cleanUser, subjectEmail, sec);
    }

    String activeEmail = resolveActiveEmail(subjectEmail, sec);
    boolean verified = isMatchingVerifiedEmail(activeEmail, sec);
    Instant verifiedAt =
        verified && sec.getEmailVerifiedAt() != null ? sec.getEmailVerifiedAt().toInstant() : null;
    String pendingEmail =
        sec != null && !isDirectoryUser ? StringUtils.trimToEmpty(sec.getPendingEmail()) : "";
    String policy =
        sec != null
            ? sec.getAccountPolicy()
            : PSAccountSecurity.POLICY_INTERACTIVE_MFA_REQUIRED;

    return new PSEmailVerificationStatus(
        cleanUser, activeEmail, verified, verifiedAt, pendingEmail, policy, isEmailRequired());
  }

  @Override
  public PSEmailChangeResult stageEmailChange(
      String userName,
      String currentSubjectEmail,
      String newEmail,
      String requestIp,
      boolean isInternalUser) {
    String cleanUser = requireUserName(userName);
    if (!isInternalUser) {
      throw new IllegalArgumentException(
          "Email is managed by the directory service and cannot be changed here.");
    }
    String cleanNewEmail = StringUtils.trimToEmpty(newEmail);
    if (!PSAuthMailService.isValidSingleEmail(cleanNewEmail)) {
      throw new IllegalArgumentException("Enter a valid email address.");
    }
    String normNew = cleanNewEmail.toLowerCase(Locale.ROOT);
    PSAccountSecurity sec = loadOrCreateSecurity(cleanUser);
    String currentVerifiedEmail = resolveCurrentVerifiedEmail(currentSubjectEmail, sec);
    if (currentVerifiedEmail != null
        && normNew.equalsIgnoreCase(currentVerifiedEmail)
        && StringUtils.isBlank(sec.getPendingEmail())) {
      return new PSEmailChangeResult(
          false,
          false,
          resolveActiveEmail(currentSubjectEmail, sec),
          "",
          true,
          "This email address is already verified.");
    }

    String baseUrl = requirePublicBaseUrl();
    requireConfiguredAuthMail();
    enforceResendLimit(cleanUser);

    sec.setPendingEmail(cleanNewEmail);
    authSecurityDao.saveAccountSecurity(sec);

    dispatchVerificationEmail(cleanUser, cleanNewEmail, requestIp, baseUrl);
    sendOldEmailNoticeIfChangingVerified(cleanUser, currentVerifiedEmail, cleanNewEmail);
    emitAudit(cleanUser, cleanNewEmail, "email_verification_staged", AuditOutcome.SUCCESS);

    String activeDisplay = resolveActiveEmail(currentSubjectEmail, sec);
    return new PSEmailChangeResult(
        true,
        false,
        activeDisplay,
        cleanNewEmail,
        isMatchingVerifiedEmail(activeDisplay, sec),
        buildStagedMessage(cleanNewEmail, currentVerifiedEmail != null));
  }

  @Override
  public PSEmailChangeResult resendVerification(
      String userName, String currentSubjectEmail, String requestIp) {
    String cleanUser = requireUserName(userName);
    PSAccountSecurity sec = loadOrCreateSecurity(cleanUser);
    String targetEmail = selectResendTarget(currentSubjectEmail, sec);
    if (StringUtils.isBlank(targetEmail)) {
      throw new IllegalArgumentException(
          "Your email address is already verified or no email address is set.");
    }

    String baseUrl = requirePublicBaseUrl();
    requireConfiguredAuthMail();
    enforceResendLimit(cleanUser);

    if (!targetEmail.equals(sec.getPendingEmail())) {
      sec.setPendingEmail(targetEmail);
      authSecurityDao.saveAccountSecurity(sec);
    }

    dispatchVerificationEmail(cleanUser, targetEmail, requestIp, baseUrl);
    emitAudit(cleanUser, targetEmail, "email_verification_resent", AuditOutcome.SUCCESS);

    String activeDisplay = resolveActiveEmail(currentSubjectEmail, sec);
    return new PSEmailChangeResult(
        true,
        false,
        activeDisplay,
        targetEmail,
        isMatchingVerifiedEmail(activeDisplay, sec),
        "Verification email sent to " + targetEmail + ".");
  }

  @Override
  public PSEmailVerifyResult confirmEmailVerification(String rawToken) {
    Optional<PSConsumedAuthToken> consumed =
        tokenService.consumeToken(rawToken, PSAuthToken.TYPE_EMAIL_VERIFY);
    if (consumed.isEmpty()) {
      return new PSEmailVerifyResult(
          false,
          PSEmailVerifyResult.STATUS_INVALID_OR_EXPIRED,
          "This verification link is invalid or has expired. Request a new verification email from your profile.");
    }
    PSConsumedAuthToken token = consumed.get();
    String targetEmail = StringUtils.trimToEmpty(token.targetEmail());
    if (!PSAuthMailService.isValidSingleEmail(targetEmail)) {
      return new PSEmailVerifyResult(
          false,
          PSEmailVerifyResult.STATUS_INVALID_OR_EXPIRED,
          "This verification token does not contain a valid email address.");
    }

    try {
      privilegedRoleRunner.accept(() -> backEndRoleMgr.setSubjectEmail(token.userName(), targetEmail));
    } catch (RuntimeException ex) {
      log.error(
          "Failed to persist verified email for user {}: {}",
          token.userName(),
          ex.getMessage());
      emitAudit(token.userName(), targetEmail, "email_verification_confirm", AuditOutcome.FAILURE);
      return new PSEmailVerifyResult(
          false,
          PSEmailVerifyResult.STATUS_PERSIST_FAILED,
          "Unable to save your verified email address. Please contact an administrator.");
    }

    PSAccountSecurity sec = loadOrCreateSecurity(token.userName());
    sec.setEmailNormalized(targetEmail.toLowerCase(Locale.ROOT));
    sec.setEmailVerified(true);
    sec.setEmailVerifiedAt(Date.from(clock.instant()));
    sec.setPendingEmail(null);
    authSecurityDao.saveAccountSecurity(sec);

    emitAudit(token.userName(), targetEmail, "email_verification_confirm", AuditOutcome.SUCCESS);
    return new PSEmailVerifyResult(
        true, PSEmailVerifyResult.STATUS_VERIFIED, "Your email address has been verified.");
  }

  @Override
  public PSForgotUsernameResult requestForgotUsername(String rawEmail, String requestIp) {
    enforceForgotUsernameIpLimit(requestIp);

    String cleanEmail = StringUtils.trimToEmpty(rawEmail);
    if (!PSAuthMailService.isValidSingleEmail(cleanEmail)) {
      return new PSForgotUsernameResult(true, GENERIC_FORGOT_USERNAME_MSG);
    }
    String normalized = cleanEmail.toLowerCase(Locale.ROOT);
    if (!allowForgotUsernameForEmail(normalized)) {
      return new PSForgotUsernameResult(true, GENERIC_FORGOT_USERNAME_MSG);
    }

    List<PSAccountSecurity> matches = authSecurityDao.findVerifiedByNormalizedEmail(normalized);
    if (matches.isEmpty()) {
      return new PSForgotUsernameResult(true, GENERIC_FORGOT_USERNAME_MSG);
    }
    if (isUniqueEmailForRecovery() && matches.size() > 1) {
      log.warn("Ambiguous forgot-username request suppressed ({} verified accounts)", matches.size());
      emitAudit("system", normalized, "forgot_username_ambiguous", AuditOutcome.FAILURE);
      return new PSForgotUsernameResult(true, GENERIC_FORGOT_USERNAME_MSG);
    }

    sendUsernameReminderSafely(cleanEmail, matches);
    return new PSForgotUsernameResult(true, GENERIC_FORGOT_USERNAME_MSG);
  }

  @Override
  public void syncAccountEmailOnAdminSave(
      String userName, String newEmail, boolean isDirectoryUser) {
    if (StringUtils.isBlank(userName)) {
      return;
    }
    String cleanUser = userName.trim();
    PSAccountSecurity sec = loadOrCreateSecurity(cleanUser);
    if (isSystemServiceUser(cleanUser)) {
      sec.setAccountPolicy(PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE);
    }
    String cleanEmail = StringUtils.trimToEmpty(newEmail);
    String newNorm =
        PSAuthMailService.isValidSingleEmail(cleanEmail) ? cleanEmail.toLowerCase(Locale.ROOT) : null;
    if (!Objects.equals(newNorm, sec.getEmailNormalized())) {
      applySyncedEmail(sec, newNorm, isDirectoryUser);
    }
    authSecurityDao.saveAccountSecurity(sec);
  }

  private PSAccountSecurity syncDirectoryViewIfChanged(
      String userName, String subjectEmail, PSAccountSecurity existing) {
    String cleanEmail = StringUtils.trimToEmpty(subjectEmail);
    String newNorm =
        PSAuthMailService.isValidSingleEmail(cleanEmail) ? cleanEmail.toLowerCase(Locale.ROOT) : null;
    if (existing == null && newNorm == null) {
      return null;
    }
    if (existing != null && Objects.equals(newNorm, existing.getEmailNormalized())) {
      return existing;
    }
    PSAccountSecurity sec = existing != null ? existing : newSecurityRecord(userName);
    applySyncedEmail(sec, newNorm, true);
    return authSecurityDao.saveAccountSecurity(sec);
  }

  private void applySyncedEmail(PSAccountSecurity sec, String newNorm, boolean isDirectoryUser) {
    sec.setEmailNormalized(newNorm);
    boolean trustDir = isDirectoryUser && newNorm != null && isTrustDirectoryEmailVerified();
    sec.setEmailVerified(trustDir);
    sec.setEmailVerifiedAt(trustDir ? Date.from(clock.instant()) : null);
    sec.setPendingEmail(null);
  }

  private static String resolveActiveEmail(String subjectEmail, PSAccountSecurity sec) {
    if (StringUtils.isNotBlank(subjectEmail)) {
      return subjectEmail.trim();
    }
    if (sec != null && StringUtils.isNotBlank(sec.getEmailNormalized())) {
      return sec.getEmailNormalized().trim();
    }
    return "";
  }

  private static boolean isMatchingVerifiedEmail(String activeEmail, PSAccountSecurity sec) {
    return sec != null
        && sec.isEmailVerified()
        && StringUtils.isNotBlank(activeEmail)
        && activeEmail.trim().equalsIgnoreCase(StringUtils.trimToEmpty(sec.getEmailNormalized()));
  }

  private static String resolveCurrentVerifiedEmail(
      String currentSubjectEmail, PSAccountSecurity sec) {
    String active = resolveActiveEmail(currentSubjectEmail, sec);
    return isMatchingVerifiedEmail(active, sec) ? active : null;
  }

  private static String selectResendTarget(String currentSubjectEmail, PSAccountSecurity sec) {
    String pending = StringUtils.trimToEmpty(sec.getPendingEmail());
    if (PSAuthMailService.isValidSingleEmail(pending)) {
      return pending;
    }
    String active = resolveActiveEmail(currentSubjectEmail, sec);
    if (PSAuthMailService.isValidSingleEmail(active) && !isMatchingVerifiedEmail(active, sec)) {
      return active;
    }
    return "";
  }

  private static String buildStagedMessage(String newEmail, boolean hadVerifiedEmail) {
    if (hadVerifiedEmail) {
      return "Verification email sent to "
          + newEmail
          + ". Your current verified email remains active until confirmed.";
    }
    return "Verification email sent to " + newEmail + ". Confirm the link to verify your email.";
  }

  private void dispatchVerificationEmail(
      String userName, String targetEmail, String requestIp, String baseUrl) {
    Duration ttl = tokenService.resolveTtl(PSAuthToken.TYPE_EMAIL_VERIFY);
    PSIssuedAuthToken token =
        tokenService.issueToken(
            PSAuthToken.TYPE_EMAIL_VERIFY, userName, targetEmail, userName, requestIp, ttl);
    String encodedToken = URLEncoder.encode(token.rawToken(), StandardCharsets.UTF_8);
    String verifyUrl = baseUrl + "/auth/verify-email?token=" + encodedToken;
    String body =
        "Hello "
            + userName
            + ",\n\n"
            + "Confirm your Percussion CMS email address by opening the link below:\n\n"
            + verifyUrl
            + "\n\n"
            + "This link expires in "
            + ttl.toMinutes()
            + " minutes.\n"
            + "If you did not request this email change, contact your administrator.\n";
    try {
      authMailService.sendTextEmail(targetEmail, "Confirm your Percussion CMS email address", body);
    } catch (PSAuthMailException ex) {
      throw new IllegalStateException(
          "Unable to send verification email: " + ex.getMessage(), ex);
    }
  }

  private void sendOldEmailNoticeIfChangingVerified(
      String userName, String oldVerifiedEmail, String newPendingEmail) {
    if (StringUtils.isBlank(oldVerifiedEmail)
        || oldVerifiedEmail.equalsIgnoreCase(newPendingEmail)) {
      return;
    }
    String body =
        "Hello "
            + userName
            + ",\n\n"
            + "A request was made to change the email address on your Percussion CMS account to "
            + newPendingEmail
            + ".\n"
            + "Your current verified email address remains active until the new address is confirmed.\n"
            + "If you did not initiate this request, sign in immediately or contact your administrator.\n";
    try {
      authMailService.sendTextEmail(
          oldVerifiedEmail, "Percussion CMS email change requested", body);
    } catch (PSAuthMailException ex) {
      log.warn(
          "Failed to send email-change security notice to previous verified address for {}: {}",
          userName,
          ex.getMessage());
    }
  }

  private void sendUsernameReminderSafely(
      String recipient, List<PSAccountSecurity> verifiedAccounts) {
    StringBuilder body = new StringBuilder();
    body.append("Hello,\n\n");
    body.append("A username reminder was requested for your Percussion CMS account.\n");
    if (verifiedAccounts.size() == 1) {
      body.append("Username: ").append(verifiedAccounts.get(0).getUserName()).append('\n');
    } else {
      body.append("Matching usernames:\n");
      for (PSAccountSecurity acct : verifiedAccounts) {
        body.append("- ").append(acct.getUserName()).append('\n');
      }
    }
    String optionalBase = resolveOptionalPublicBaseUrl();
    if (StringUtils.isNotBlank(optionalBase)) {
      body.append("\nSign in: ").append(optionalBase).append("/login\n");
    }
    try {
      authMailService.sendTextEmail(recipient, "Your Percussion CMS username", body.toString());
      emitAudit("system", recipient, "forgot_username_sent", AuditOutcome.SUCCESS);
    } catch (PSAuthMailException ex) {
      log.error("Failed sending username reminder email: {}", ex.getMessage());
      emitAudit("system", recipient, "forgot_username_send_failed", AuditOutcome.FAILURE);
    }
  }

  private void enforceResendLimit(String userName) {
    long limit = readPositiveLong(PROP_RATE_EMAIL_RESEND, 5L);
    boolean allowed =
        rateLimiter.tryConsume(
            "email_verify_resend", userName.toLowerCase(Locale.ROOT), limit, Duration.ofHours(1));
    if (!allowed) {
      throw new PSAuthRateLimitException(
          "Too many verification email requests. Please wait before trying again.");
    }
  }

  private void enforceForgotUsernameIpLimit(String requestIp) {
    long limit = readPositiveLong(PROP_RATE_FORGOT_USER_IP, 15L);
    String key = StringUtils.defaultIfBlank(requestIp, "_unknown");
    if (!rateLimiter.tryConsume("forgot_username_ip", key, limit, Duration.ofHours(1))) {
      throw new PSAuthRateLimitException();
    }
  }

  private boolean allowForgotUsernameForEmail(String normalizedEmail) {
    long limit = readPositiveLong(PROP_RATE_FORGOT_USER_EMAIL, 5L);
    return rateLimiter.tryConsume(
        "forgot_username_email", normalizedEmail, limit, Duration.ofHours(1));
  }

  private void requireConfiguredAuthMail() {
    if (!authMailService.resolveEffectiveConfig().isConfigured()) {
      throw new IllegalStateException(
          "Authentication mail is not configured. Configure auth.mail.* settings first.");
    }
  }

  private String requirePublicBaseUrl() {
    String base = resolveOptionalPublicBaseUrl();
    if (StringUtils.isBlank(base)) {
      throw new IllegalStateException(
          "auth.publicBaseUrl must be configured with a valid http(s) URL before sending verification links.");
    }
    return base;
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

  private boolean isUniqueEmailForRecovery() {
    return readBoolean(PROP_UNIQUE_EMAIL_RECOVERY, false);
  }

  private boolean isTrustDirectoryEmailVerified() {
    return readBoolean(PROP_DIRECTORY_TRUST_VERIFIED, false);
  }

  private boolean readBoolean(String key, boolean defaultValue) {
    Properties props = safeProps(serverPropsSupplier.get());
    String raw = StringUtils.trimToNull(props.getProperty(key));
    return raw == null ? defaultValue : Boolean.parseBoolean(raw);
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

  private PSAccountSecurity loadOrCreateSecurity(String userName) {
    return authSecurityDao.findAccountSecurity(userName).orElseGet(() -> newSecurityRecord(userName));
  }

  private PSAccountSecurity newSecurityRecord(String userName) {
    PSAccountSecurity rec = new PSAccountSecurity();
    rec.setUserName(userName);
    if (isSystemServiceUser(userName)) {
      rec.setAccountPolicy(PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE);
    }
    return rec;
  }

  private static String requireUserName(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    return userName.trim();
  }

  private static void emitAudit(
      String actor, String target, String activity, AuditOutcome outcome) {
    try {
      AuditContext ctx =
          AuditContext.builder()
              .actor(actor)
              .target(target)
              .attribute("activity", activity)
              .build();
      PSSystemAuditLogger.log(
          UserManagementErrorCodes.UPDATE, ctx, outcome, actor, target, activity);
    } catch (RuntimeException ex) {
      log.debug("Audit emission skipped for {}: {}", activity, ex.getMessage());
    }
  }

  private static void runPrivilegedRoleUpdate(Runnable task) {
    if (!PSServer.isInitialized()) {
      task.run();
      return;
    }
    boolean wasInited = PSRequestInfo.isInited();
    PSThreadRequestUtils.changeToInternalRequest(true);
    try {
      task.run();
    } finally {
      PSThreadRequestUtils.restoreOriginalRequest();
      if (!wasInited) {
        PSRequestInfo.resetRequestInfo();
      }
    }
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }
}
