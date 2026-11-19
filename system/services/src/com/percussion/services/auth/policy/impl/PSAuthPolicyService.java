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
package com.percussion.services.auth.policy.impl;

import com.percussion.server.PSServer;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.policy.IPSAuthPolicyService;
import com.percussion.services.auth.policy.PSLoginPolicyDecision;
import com.percussion.system.utils.PSBaseBean;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** Implements cohort/global MFA enforcement and non-interactive service policy checks. */
@PSBaseBean("sys_authPolicyService")
public class PSAuthPolicyService implements IPSAuthPolicyService {

  public static final String PROP_MFA_MODE = "auth.mfa.mode";
  public static final String PROP_REQUIRED_COHORTS = "auth.mfa.requiredCohorts";

  public static final String MODE_OFF = "off";
  public static final String MODE_DISABLED = "disabled";
  public static final String MODE_OPTIONAL = "optional";
  public static final String MODE_COHORT = "cohort";
  public static final String MODE_REQUIRED = "required";

  private final IPSAuthSecurityDao authSecurityDao;
  private final IPSAccountSecurityService accountSecurityService;
  private final Supplier<Properties> serverPropsSupplier;

  @Autowired
  public PSAuthPolicyService(
      IPSAuthSecurityDao authSecurityDao, IPSAccountSecurityService accountSecurityService) {
    this(authSecurityDao, accountSecurityService, PSAuthPolicyService::defaultServerProperties);
  }

  PSAuthPolicyService(
      IPSAuthSecurityDao authSecurityDao,
      IPSAccountSecurityService accountSecurityService,
      Supplier<Properties> serverPropsSupplier) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.accountSecurityService =
        Objects.requireNonNull(accountSecurityService, "accountSecurityService");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
  }

  @Override
  public String getEffectiveMode() {
    Properties props = safeProps(serverPropsSupplier.get());
    String raw = StringUtils.trimToEmpty(props.getProperty(PROP_MFA_MODE)).toLowerCase(Locale.ROOT);
    return switch (raw) {
      case MODE_OFF, MODE_DISABLED -> MODE_DISABLED;
      case MODE_OPTIONAL, MODE_COHORT, MODE_REQUIRED -> raw;
      default -> MODE_OPTIONAL;
    };
  }

  @Override
  public boolean isMfaRequiredForUser(String userName, Collection<String> roles) {
    if (StringUtils.isBlank(userName) || accountSecurityService.isSystemServiceUser(userName)) {
      return false;
    }
    String mode = getEffectiveMode();
    if (MODE_DISABLED.equals(mode)) {
      return false;
    }
    PSAccountSecurity sec = authSecurityDao.findAccountSecurity(userName.trim()).orElse(null);
    if (sec != null
        && PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE.equals(sec.getAccountPolicy())) {
      return false;
    }
    if (MODE_REQUIRED.equals(mode)) {
      return true;
    }
    if (MODE_COHORT.equals(mode)) {
      return matchesRequiredCohort(userName.trim(), sec, roles);
    }
    return false;
  }

  @Override
  public PSLoginPolicyDecision evaluateInteractiveLogin(
      String userName, Collection<String> roles, boolean isExternalSso) {
    if (StringUtils.isBlank(userName) || accountSecurityService.isSystemServiceUser(userName)) {
      return PSLoginPolicyDecision.ALLOW_DIRECT_SESSION;
    }
    String cleanUser = userName.trim();
    PSAccountSecurity sec = authSecurityDao.findAccountSecurity(cleanUser).orElse(null);
    String policy =
        sec != null
            ? sec.getAccountPolicy()
            : PSAccountSecurity.POLICY_INTERACTIVE_MFA_REQUIRED;

    if (PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE.equals(policy)) {
      return PSLoginPolicyDecision.ALLOW_DIRECT_SESSION;
    }
    if (isExternalSso && PSAccountSecurity.POLICY_SSO_EXTERNAL_MFA.equals(policy)) {
      return PSLoginPolicyDecision.ALLOW_DIRECT_SESSION;
    }
    if (MODE_DISABLED.equals(getEffectiveMode())) {
      return PSLoginPolicyDecision.ALLOW_DIRECT_SESSION;
    }
    if (hasActiveFactor(cleanUser)) {
      return PSLoginPolicyDecision.REQUIRE_MFA_CHALLENGE;
    }
    if (isMfaRequiredForUser(cleanUser, roles)) {
      return PSLoginPolicyDecision.REQUIRE_MFA_ENROLLMENT;
    }
    return PSLoginPolicyDecision.ALLOW_DIRECT_SESSION;
  }

  @Override
  public boolean isNonInteractivePasswordChannelAllowed(
      String userName, Collection<String> roles, String channel) {
    if (StringUtils.isBlank(userName)) {
      return false;
    }
    if (accountSecurityService.isSystemServiceUser(userName)) {
      return true;
    }
    String cleanUser = userName.trim();
    PSAccountSecurity sec = authSecurityDao.findAccountSecurity(cleanUser).orElse(null);
    if (sec != null
        && PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE.equals(sec.getAccountPolicy())) {
      return true;
    }
    if (MODE_DISABLED.equals(getEffectiveMode())) {
      return true;
    }
    return !hasActiveFactor(cleanUser) && !isMfaRequiredForUser(cleanUser, roles);
  }

  @Override
  public boolean hasActiveFactor(String userName) {
    if (StringUtils.isBlank(userName)) {
      return false;
    }
    String cleanUser = userName.trim();
    return !authSecurityDao.findActiveTotpByUser(cleanUser).isEmpty()
        || !authSecurityDao.findActivePasskeysByUser(cleanUser).isEmpty();
  }

  private boolean matchesRequiredCohort(
      String userName, PSAccountSecurity sec, Collection<String> roles) {
    Set<String> requiredCohorts = parseCohortSet();
    if (requiredCohorts.isEmpty()) {
      return false;
    }
    if (sec != null && StringUtils.isNotBlank(sec.getMfaEnforcementCohort())) {
      String cohort = sec.getMfaEnforcementCohort().trim().toLowerCase(Locale.ROOT);
      if (requiredCohorts.contains(cohort)) {
        return true;
      }
    }
    if (requiredCohorts.contains(userName.toLowerCase(Locale.ROOT))) {
      return true;
    }
    return rolesContainAny(roles, requiredCohorts);
  }

  private static boolean rolesContainAny(Collection<String> roles, Set<String> requiredCohorts) {
    if (roles == null || roles.isEmpty()) {
      return false;
    }
    for (String role : roles) {
      if (StringUtils.isNotBlank(role)
          && requiredCohorts.contains(role.trim().toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private Set<String> parseCohortSet() {
    Properties props = safeProps(serverPropsSupplier.get());
    String raw = StringUtils.trimToEmpty(props.getProperty(PROP_REQUIRED_COHORTS));
    if (raw.isEmpty()) {
      return Set.of();
    }
    Set<String> out = new HashSet<>();
    for (String part : raw.split(",")) {
      String clean = part.trim().toLowerCase(Locale.ROOT);
      if (!clean.isEmpty()) {
        out.add(clean);
      }
    }
    return out;
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }
}
