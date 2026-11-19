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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSMfaTotp;
import com.percussion.services.auth.policy.PSLoginPolicyDecision;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PSAuthPolicyServiceTest {

  @Test
  void evaluatesModesCohortsFactorsAndServiceAccountsAccurately() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    IPSAccountSecurityService acct = mock(IPSAccountSecurityService.class);
    Properties props = new Properties();
    PSAuthPolicyService policy = new PSAuthPolicyService(dao, acct, () -> props);

    when(acct.isSystemServiceUser("rxserver")).thenReturn(true);
    when(acct.isSystemServiceUser("alice")).thenReturn(false);
    when(acct.isSystemServiceUser("svc_pub")).thenReturn(false);

    PSAccountSecurity svcAcct = new PSAccountSecurity();
    svcAcct.setUserName("svc_pub");
    svcAcct.setAccountPolicy(PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE);
    when(dao.findAccountSecurity("svc_pub")).thenReturn(Optional.of(svcAcct));

    PSAccountSecurity aliceSec = new PSAccountSecurity();
    aliceSec.setUserName("alice");
    aliceSec.setMfaEnforcementCohort("pilot");
    when(dao.findAccountSecurity("alice")).thenReturn(Optional.of(aliceSec));

    when(dao.findActiveTotpByUser("alice")).thenReturn(List.of());
    when(dao.findActivePasskeysByUser("alice")).thenReturn(List.of());

    // optional + 0 factors => direct login & Basic allowed
    props.setProperty(PSAuthPolicyService.PROP_MFA_MODE, "optional");
    assertEquals(
        PSLoginPolicyDecision.ALLOW_DIRECT_SESSION,
        policy.evaluateInteractiveLogin("alice", List.of("Editor"), false));
    assertTrue(policy.isNonInteractivePasswordChannelAllowed("alice", List.of("Editor"), "BASIC"));

    // optional + enrolled factor => MFA challenge + Basic blocked
    when(dao.findActiveTotpByUser("alice")).thenReturn(List.of(new PSMfaTotp()));
    assertEquals(
        PSLoginPolicyDecision.REQUIRE_MFA_CHALLENGE,
        policy.evaluateInteractiveLogin("alice", List.of("Editor"), false));
    assertFalse(policy.isNonInteractivePasswordChannelAllowed("alice", List.of("Editor"), "BASIC"));

    // cohort + 0 factors => ENROLL_ONLY when cohort matches, ALLOW otherwise
    when(dao.findActiveTotpByUser("alice")).thenReturn(List.of());
    props.setProperty(PSAuthPolicyService.PROP_MFA_MODE, "cohort");
    props.setProperty(PSAuthPolicyService.PROP_REQUIRED_COHORTS, "pilot,Admin");
    assertEquals(
        PSLoginPolicyDecision.REQUIRE_MFA_ENROLLMENT,
        policy.evaluateInteractiveLogin("alice", List.of("Author"), false));
    assertFalse(
        policy.isNonInteractivePasswordChannelAllowed("alice", List.of("Author"), "SOAP_WS"));

    // NON_INTERACTIVE_SERVICE and system service users exempt even in required mode
    props.setProperty(PSAuthPolicyService.PROP_MFA_MODE, "required");
    assertTrue(policy.isNonInteractivePasswordChannelAllowed("rxserver", List.of("Admin"), "SOAP"));
    assertTrue(policy.isNonInteractivePasswordChannelAllowed("svc_pub", List.of("Admin"), "BASIC"));
    assertEquals(
        PSLoginPolicyDecision.ALLOW_DIRECT_SESSION,
        policy.evaluateInteractiveLogin("svc_pub", List.of("Admin"), false));
  }
}
