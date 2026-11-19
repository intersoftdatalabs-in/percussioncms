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

import static com.percussion.webservices.PSWebserviceUtils.getUserName;
import static com.percussion.webservices.PSWebserviceUtils.getUserRoles;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.codes.UserManagementErrorCodes;
import com.percussion.rest.auth.AdminRecoveryRequest;
import com.percussion.rest.auth.AdminRecoveryResponse;
import com.percussion.rest.auth.AuthAccountReadinessItem;
import com.percussion.rest.auth.AuthAccountReadinessSummary;
import com.percussion.rest.auth.AuthInfraCheck;
import com.percussion.rest.auth.AuthMailTestRequest;
import com.percussion.rest.auth.AuthMailTestResult;
import com.percussion.rest.auth.AuthReadinessReport;
import com.percussion.rest.auth.IAuthReadinessAdaptor;
import com.percussion.services.audit.PSSystemAuditLogger;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailException;
import com.percussion.services.auth.mail.impl.PSAuthMailService;
import com.percussion.services.auth.readiness.IPSAuthReadinessService;
import com.percussion.services.auth.readiness.PSAuthCheckStatus;
import com.percussion.services.auth.readiness.PSAuthInfraCheck;
import com.percussion.services.auth.readiness.PSAuthInfraReport;
import com.percussion.services.auth.recovery.IPSAccountRecoveryService;
import com.percussion.services.auth.recovery.PSAdminRecoveryIssueResult;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserList;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * sitemanage apibridge adaptor for Admin Authentication, MFA, and Account Recovery Readiness.
 *
 * <p>AuthZ: requires the {@code Admin} role on every method. Unauthorized callers receive {@link
 * SecurityException} (mapped to HTTP 403 by {@code AuthReadinessResource}).
 */
@PSSiteManageBean
@Lazy
public class AuthReadinessAdaptor implements IAuthReadinessAdaptor {

  private static final Logger log = LogManager.getLogger(AuthReadinessAdaptor.class);

  public static final String ISSUE_MISSING_EMAIL = "MISSING_EMAIL";
  public static final String ISSUE_MALFORMED_EMAIL = "MALFORMED_EMAIL";
  public static final String ISSUE_DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
  public static final String ISSUE_UNVERIFIED_EMAIL = "UNVERIFIED_EMAIL";
  public static final String ISSUE_NO_MFA_ENROLLED = "NO_MFA_ENROLLED";
  public static final String ISSUE_ACCOUNT_LOOKUP_FAILED = "ACCOUNT_LOOKUP_FAILED";

  private final IPSAuthReadinessService readinessService;
  private final IPSAuthMailService authMailService;
  private final IPSAuthSecurityDao authSecurityDao;
  private final IPSUserService userService;
  private final IPSAccountRecoveryService accountRecoveryService;
  private final Supplier<List<String>> userRolesSupplier;
  private final Supplier<String> currentUserSupplier;
  private final Supplier<HttpSession> sessionSupplier;
  private final Supplier<String> clientIpSupplier;

  @Autowired
  public AuthReadinessAdaptor(
      IPSAuthReadinessService readinessService,
      IPSAuthMailService authMailService,
      IPSAuthSecurityDao authSecurityDao,
      IPSUserService userService,
      IPSAccountRecoveryService accountRecoveryService) {
    this(
        readinessService,
        authMailService,
        authSecurityDao,
        userService,
        accountRecoveryService,
        () -> getUserRoles(),
        () -> getUserName(),
        AuthMfaAdaptor::resolveCurrentSession,
        AuthMfaAdaptor::resolveCurrentClientIp);
  }

  /** Package-visible constructor for isolated M1 unit tests. */
  AuthReadinessAdaptor(
      IPSAuthReadinessService readinessService,
      IPSAuthMailService authMailService,
      IPSAuthSecurityDao authSecurityDao,
      IPSUserService userService,
      Supplier<List<String>> userRolesSupplier,
      Supplier<String> currentUserSupplier) {
    this(
        readinessService,
        authMailService,
        authSecurityDao,
        userService,
        null,
        userRolesSupplier,
        currentUserSupplier,
        AuthMfaAdaptor::resolveCurrentSession,
        AuthMfaAdaptor::resolveCurrentClientIp);
  }

  /** Package-visible constructor for isolated M5 unit tests (including break-glass recovery). */
  AuthReadinessAdaptor(
      IPSAuthReadinessService readinessService,
      IPSAuthMailService authMailService,
      IPSAuthSecurityDao authSecurityDao,
      IPSUserService userService,
      IPSAccountRecoveryService accountRecoveryService,
      Supplier<List<String>> userRolesSupplier,
      Supplier<String> currentUserSupplier,
      Supplier<HttpSession> sessionSupplier,
      Supplier<String> clientIpSupplier) {
    this.readinessService = Objects.requireNonNull(readinessService, "readinessService");
    this.authMailService = Objects.requireNonNull(authMailService, "authMailService");
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.userService = Objects.requireNonNull(userService, "userService");
    this.accountRecoveryService = accountRecoveryService;
    this.userRolesSupplier = Objects.requireNonNull(userRolesSupplier, "userRolesSupplier");
    this.currentUserSupplier = Objects.requireNonNull(currentUserSupplier, "currentUserSupplier");
    this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");
    this.clientIpSupplier = Objects.requireNonNull(clientIpSupplier, "clientIpSupplier");
  }

  @Override
  public AuthReadinessReport getReadinessReport() {
    requireAdmin();

    PSAuthInfraReport infra = readinessService.evaluateInfrastructure();
    AuthReadinessReport report = new AuthReadinessReport();
    report.setGeneratedAt(infra.getGeneratedAt());
    report.setMfaMode(infra.getMfaMode());
    report.setPasswordMinLength(infra.getPasswordMinLength());
    report.setMailConfigured(infra.isMailConfigured());
    report.setMailSource(infra.getMailSource().name());
    report.setClusterKeyFingerprint(infra.getClusterKeyFingerprint());

    List<AuthInfraCheck> dtoChecks = new ArrayList<>(infra.getChecks().size());
    boolean anyInfraFail = false;
    boolean anyInfraWarn = false;
    for (PSAuthInfraCheck c : infra.getChecks()) {
      if (c.getStatus() == PSAuthCheckStatus.FAIL) {
        anyInfraFail = true;
      } else if (c.getStatus() == PSAuthCheckStatus.WARN) {
        anyInfraWarn = true;
      }
      dtoChecks.add(
          new AuthInfraCheck(
              c.getCode(),
              c.getLabel(),
              c.getStatus().name(),
              c.getSummary(),
              c.getRemediation()));
    }
    report.setInfraChecks(dtoChecks);

    AccountEvaluation eval = evaluateAccounts();
    report.setAccountSummary(eval.summary());
    report.setAccountIssues(eval.issues());

    if (anyInfraFail) {
      report.setOverallStatus(PSAuthCheckStatus.FAIL.name());
    } else if (anyInfraWarn || !eval.issues().isEmpty()) {
      report.setOverallStatus(PSAuthCheckStatus.WARN.name());
    } else {
      report.setOverallStatus(PSAuthCheckStatus.PASS.name());
    }
    return report;
  }

  @Override
  public AuthMailTestResult sendTestEmail(AuthMailTestRequest request) {
    requireAdmin();
    if (request == null || !PSAuthMailService.isValidSingleEmail(request.getRecipient())) {
      throw new IllegalArgumentException("A valid single recipient email address is required.");
    }
    String recipient = request.getRecipient().trim();
    PSAuthMailConfig cfg = authMailService.resolveEffectiveConfig();
    String actor = StringUtils.defaultIfBlank(currentUserSupplier.get(), "Admin");
    try {
      authMailService.sendTestEmail(recipient);
      auditMailTest(actor, recipient, AuditOutcome.SUCCESS);
      return new AuthMailTestResult(
          true,
          recipient,
          cfg.getSource().name(),
          "Test authentication email sent successfully via "
              + cfg.getHost()
              + ":"
              + cfg.getPort()
              + ".");
    } catch (PSAuthMailException ex) {
      auditMailTest(actor, recipient, AuditOutcome.FAILURE);
      return new AuthMailTestResult(false, recipient, cfg.getSource().name(), ex.getMessage());
    }
  }

  @Override
  public AdminRecoveryResponse initiateAdminRecovery(AdminRecoveryRequest request) {
    requireAdmin();
    if (request == null || StringUtils.isBlank(request.getTargetUserName())) {
      throw new IllegalArgumentException("targetUserName is required");
    }
    String cleanTarget = request.getTargetUserName().trim();
    PSUser targetUser;
    try {
      targetUser = userService.find(cleanTarget);
    } catch (PSDataServiceException ex) {
      throw new IllegalArgumentException("Target user not found: " + cleanTarget, ex);
    }
    if (targetUser == null || StringUtils.isBlank(targetUser.getName())) {
      throw new IllegalArgumentException("Target user not found: " + cleanTarget);
    }
    if (accountRecoveryService == null) {
      throw new IllegalStateException("Account recovery service is unavailable");
    }
    String adminActor = StringUtils.defaultIfBlank(currentUserSupplier.get(), "Admin");
    PSAdminRecoveryIssueResult issued =
        accountRecoveryService.issueAdminBreakGlassRecovery(
            adminActor,
            targetUser.getName(),
            request.getVerificationNote(),
            request.isResetImmediately(),
            clientIpSupplier.get(),
            sessionSupplier.get());
    return new AdminRecoveryResponse(
        issued.issued(),
        issued.userName(),
        issued.recoveryMode(),
        issued.delivery(),
        issued.expiresAt(),
        issued.message());
  }

  private void requireAdmin() {
    List<String> roles = userRolesSupplier.get();
    if (roles == null || !roles.contains("Admin")) {
      throw new SecurityException("Admin role required to access auth readiness tools");
    }
  }

  private AccountEvaluation evaluateAccounts() {
    AuthAccountReadinessSummary summary = new AuthAccountReadinessSummary();
    List<AuthAccountReadinessItem> issueItems = new ArrayList<>();

    List<String> sortedNames = new ArrayList<>(loadUserNames());
    sortedNames.sort(Comparator.naturalOrder());

    AccountMaps maps =
        new AccountMaps(
            safeLoadSecurityMap(),
            safeLoadCountMap(authSecurityDao::countActiveTotpByUsers),
            safeLoadCountMap(authSecurityDao::countActivePasskeysByUsers),
            safeLoadCountMap(authSecurityDao::countUnusedRecoveryCodesByUsers));
    UserCatalog catalog = loadUserCatalog(sortedNames);
    summary.setTotalAccounts(catalog.users().size() + catalog.failedNames().size());

    for (String name : sortedNames) {
      if (StringUtils.isBlank(name)) {
        continue;
      }
      if (catalog.failedNames().contains(name)) {
        issueItems.add(buildLookupFailureItem(name));
        continue;
      }
      PSUser user = catalog.users().get(name);
      if (user != null) {
        evaluateSingleUser(name, user, maps, catalog.emailFrequencies(), summary, issueItems);
      }
    }

    return new AccountEvaluation(summary, issueItems);
  }

  private UserCatalog loadUserCatalog(List<String> sortedNames) {
    Map<String, PSUser> loadedUsers = new LinkedHashMap<>();
    Set<String> failedNames = new HashSet<>();
    Map<String, Integer> emailFrequencies = new HashMap<>();

    for (String name : sortedNames) {
      if (StringUtils.isBlank(name)) {
        continue;
      }
      PSUser user = findUserSafely(name);
      if (user == null) {
        failedNames.add(name);
        continue;
      }
      loadedUsers.put(name, user);
      recordEmailFrequency(user.getEmail(), emailFrequencies);
    }
    return new UserCatalog(loadedUsers, failedNames, emailFrequencies);
  }

  private PSUser findUserSafely(String name) {
    try {
      return userService.find(name);
    } catch (PSDataServiceException | RuntimeException ex) {
      log.warn("Readiness user lookup failed for {}: {}", name, ex.getMessage());
      return null;
    }
  }

  private static void recordEmailFrequency(String rawEmail, Map<String, Integer> emailFrequencies) {
    String email = StringUtils.trimToEmpty(rawEmail);
    if (PSAuthMailService.isValidSingleEmail(email)) {
      emailFrequencies.merge(email.toLowerCase(Locale.ROOT), 1, Integer::sum);
    }
  }

  private void evaluateSingleUser(
      String name,
      PSUser user,
      AccountMaps maps,
      Map<String, Integer> emailFrequencies,
      AuthAccountReadinessSummary summary,
      List<AuthAccountReadinessItem> issueItems) {
    if (user.getProviderType() == PSUserProviderType.DIRECTORY) {
      summary.setDirectoryAccounts(summary.getDirectoryAccounts() + 1);
    } else {
      summary.setInternalAccounts(summary.getInternalAccounts() + 1);
    }

    PSAccountSecurity sec = maps.securityByUser().get(name);
    List<String> issues = new ArrayList<>();
    String email = StringUtils.trimToEmpty(user.getEmail());
    boolean emailVerified = evaluateEmailIssues(email, sec, emailFrequencies, summary, issues);

    int totp = maps.totpCounts().getOrDefault(name, 0);
    int passkey = maps.passkeyCounts().getOrDefault(name, 0);
    int recovery = maps.recoveryCounts().getOrDefault(name, 0);
    recordMfaIssues(sec, totp, passkey, summary, issues);

    if (issues.isEmpty()) {
      summary.setReadyCount(summary.getReadyCount() + 1);
    } else {
      issueItems.add(buildIssueItem(name, user, sec, email, emailVerified, totp, passkey, recovery, issues));
    }
  }

  private static boolean evaluateEmailIssues(
      String email,
      PSAccountSecurity sec,
      Map<String, Integer> emailFrequencies,
      AuthAccountReadinessSummary summary,
      List<String> issues) {
    if (email.isEmpty()) {
      summary.setMissingEmailCount(summary.getMissingEmailCount() + 1);
      issues.add(ISSUE_MISSING_EMAIL);
      return false;
    }
    if (!PSAuthMailService.isValidSingleEmail(email)) {
      summary.setMalformedEmailCount(summary.getMalformedEmailCount() + 1);
      issues.add(ISSUE_MALFORMED_EMAIL);
      return false;
    }
    String norm = email.toLowerCase(Locale.ROOT);
    if (emailFrequencies.getOrDefault(norm, 0) > 1) {
      summary.setDuplicateEmailCount(summary.getDuplicateEmailCount() + 1);
      issues.add(ISSUE_DUPLICATE_EMAIL);
    }
    boolean verified =
        sec != null
            && sec.isEmailVerified()
            && norm.equalsIgnoreCase(StringUtils.trimToEmpty(sec.getEmailNormalized()));
    if (!verified) {
      summary.setUnverifiedEmailCount(summary.getUnverifiedEmailCount() + 1);
      issues.add(ISSUE_UNVERIFIED_EMAIL);
    }
    return verified;
  }

  private static void recordMfaIssues(
      PSAccountSecurity sec,
      int totp,
      int passkey,
      AuthAccountReadinessSummary summary,
      List<String> issues) {
    if (totp > 0) {
      summary.setTotpEnrolledCount(summary.getTotpEnrolledCount() + 1);
    }
    if (passkey > 0) {
      summary.setPasskeyEnrolledCount(summary.getPasskeyEnrolledCount() + 1);
    }
    boolean hasMfa = (totp + passkey) > 0;
    if (hasMfa) {
      summary.setMfaEnrolledCount(summary.getMfaEnrolledCount() + 1);
      return;
    }
    boolean serviceAccount =
        sec != null
            && PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE.equals(sec.getAccountPolicy());
    if (!serviceAccount) {
      issues.add(ISSUE_NO_MFA_ENROLLED);
    }
  }

  private static AuthAccountReadinessItem buildLookupFailureItem(String name) {
    AuthAccountReadinessItem item = new AuthAccountReadinessItem();
    item.setUserName(name);
    item.setProviderType("UNKNOWN");
    item.setIssues(List.of(ISSUE_ACCOUNT_LOOKUP_FAILED));
    return item;
  }

  private static AuthAccountReadinessItem buildIssueItem(
      String name,
      PSUser user,
      PSAccountSecurity sec,
      String email,
      boolean emailVerified,
      int totp,
      int passkey,
      int recovery,
      List<String> issues) {
    AuthAccountReadinessItem item = new AuthAccountReadinessItem();
    item.setUserName(name);
    item.setProviderType(
        user.getProviderType() != null ? user.getProviderType().name() : "INTERNAL");
    item.setEmail(email);
    item.setEmailVerified(emailVerified);
    item.setMfaEnrollmentState(
        sec != null ? sec.getMfaEnrollmentState() : PSAccountSecurity.MFA_STATE_NONE);
    item.setTotpCount(totp);
    item.setPasskeyCount(passkey);
    item.setRecoveryCodeCount(recovery);
    item.setIssues(issues);
    return item;
  }

  private record AccountMaps(
      Map<String, PSAccountSecurity> securityByUser,
      Map<String, Integer> totpCounts,
      Map<String, Integer> passkeyCounts,
      Map<String, Integer> recoveryCounts) {}

  private record UserCatalog(
      Map<String, PSUser> users,
      Set<String> failedNames,
      Map<String, Integer> emailFrequencies) {}

  private List<String> loadUserNames() {
    try {
      PSUserList list = userService.getUsers();
      return list != null && list.getUsers() != null ? list.getUsers() : List.of();
    } catch (PSDataServiceException ex) {
      log.warn("Failed to enumerate CMS users for readiness report: {}", ex.getMessage());
      return List.of();
    }
  }

  private Map<String, PSAccountSecurity> safeLoadSecurityMap() {
    try {
      List<PSAccountSecurity> rows = authSecurityDao.findAllAccountSecurity();
      if (rows == null || rows.isEmpty()) {
        return Map.of();
      }
      Map<String, PSAccountSecurity> map = new HashMap<>(rows.size());
      for (PSAccountSecurity row : rows) {
        if (row != null && row.getUserName() != null) {
          map.put(row.getUserName(), row);
        }
      }
      return map;
    } catch (RuntimeException ex) {
      log.debug("PSX_ACCOUNT_SECURITY query failed during readiness check: {}", ex.getMessage());
      return Map.of();
    }
  }

  private Map<String, Integer> safeLoadCountMap(Supplier<Map<String, Integer>> supplier) {
    try {
      Map<String, Integer> res = supplier.get();
      return res != null ? res : Map.of();
    } catch (RuntimeException ex) {
      log.debug("MFA count query failed during readiness check: {}", ex.getMessage());
      return Map.of();
    }
  }

  private static void auditMailTest(String actor, String recipient, AuditOutcome outcome) {
    try {
      String activity = "auth_mail_test_" + outcome.name().toLowerCase(Locale.ROOT);
      AuditContext ctx =
          AuditContext.builder()
              .actor(actor)
              .target(recipient)
              .attribute("activity", activity)
              .build();
      PSSystemAuditLogger.log(
          UserManagementErrorCodes.UPDATE, ctx, outcome, actor, recipient, activity);
    } catch (RuntimeException ex) {
      log.debug("Audit emission skipped for auth mail test: {}", ex.getMessage());
    }
  }

  private record AccountEvaluation(
      AuthAccountReadinessSummary summary, List<AuthAccountReadinessItem> issues) {}
}
