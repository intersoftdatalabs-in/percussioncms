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
package com.percussion.services.auth;

import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.IPSPasskeyService;
import com.percussion.services.auth.mfa.IPSRecoveryCodeService;
import com.percussion.services.auth.mfa.IPSTotpService;
import com.percussion.services.auth.policy.IPSAuthPolicyService;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.services.auth.readiness.IPSAuthReadinessService;
import com.percussion.services.auth.token.IPSAuthTokenService;

/** Service locator for authentication, MFA, mail, and readiness Spring beans. */
public final class PSAuthServiceLocator extends PSBaseServiceLocator {

  private PSAuthServiceLocator() {}

  public static IPSAuthSecurityDao getAuthSecurityDao()
      throws PSMissingBeanConfigurationException {
    return (IPSAuthSecurityDao) getBean("sys_authSecurityDao");
  }

  public static IPSAuthMailService getAuthMailService()
      throws PSMissingBeanConfigurationException {
    return (IPSAuthMailService) getBean("sys_authMailService");
  }

  public static IPSAuthReadinessService getAuthReadinessService()
      throws PSMissingBeanConfigurationException {
    return (IPSAuthReadinessService) getBean("sys_authReadinessService");
  }

  public static IPSAuthRateLimiter getAuthRateLimiter()
      throws PSMissingBeanConfigurationException {
    return (IPSAuthRateLimiter) getBean("sys_authRateLimiter");
  }

  public static IPSAuthTokenService getAuthTokenService()
      throws PSMissingBeanConfigurationException {
    return (IPSAuthTokenService) getBean("sys_authTokenService");
  }

  public static IPSAccountSecurityService getAccountSecurityService()
      throws PSMissingBeanConfigurationException {
    return (IPSAccountSecurityService) getBean("sys_accountSecurityService");
  }

  public static IPSTotpService getTotpService() throws PSMissingBeanConfigurationException {
    return (IPSTotpService) getBean("sys_totpService");
  }

  public static IPSRecoveryCodeService getRecoveryCodeService()
      throws PSMissingBeanConfigurationException {
    return (IPSRecoveryCodeService) getBean("sys_recoveryCodeService");
  }

  public static IPSAuthPolicyService getAuthPolicyService()
      throws PSMissingBeanConfigurationException {
    return (IPSAuthPolicyService) getBean("sys_authPolicyService");
  }

  public static IPSMfaService getMfaService() throws PSMissingBeanConfigurationException {
    return (IPSMfaService) getBean("sys_mfaService");
  }

  public static IPSPasskeyService getPasskeyService()
      throws PSMissingBeanConfigurationException {
    return (IPSPasskeyService) getBean("sys_passkeyService");
  }
}
