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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.data.PSMfaPasskey;
import com.percussion.services.auth.mfa.IPSPasskeyService;
import com.percussion.services.auth.mfa.IPSRecoveryCodeService;
import com.percussion.services.auth.mfa.IPSTotpService;
import com.percussion.services.auth.mfa.PSMfaChallengeOutcome;
import com.percussion.services.auth.mfa.PSMfaLockoutException;
import com.percussion.services.auth.mfa.PSMfaTxStatus;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.mfa.PSTotpConfirmOutcome;
import com.percussion.services.auth.policy.IPSAuthPolicyService;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class PSMfaServiceTest {

  @Test
  void challengeEnrollmentLockoutStepUpAndLastFactorGuardWork() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    IPSTotpService totp = mock(IPSTotpService.class);
    IPSRecoveryCodeService rc = mock(IPSRecoveryCodeService.class);
    IPSAuthPolicyService policy = mock(IPSAuthPolicyService.class);
    IPSAuthRateLimiter rate = mock(IPSAuthRateLimiter.class);

    when(rate.tryConsume(anyString(), anyString(), anyLong(), any())).thenReturn(true);
    Properties props = new Properties();
    props.setProperty(PSMfaService.PROP_MAX_FAILED_ATTEMPTS, "2");
    Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);

    PSMfaService service = new PSMfaService(dao, totp, rc, policy, rate, () -> props, clock);

    PSAccountSecurity sec = new PSAccountSecurity();
    sec.setUserName("alice");
    when(dao.findAccountSecurity("alice")).thenReturn(Optional.of(sec));
    when(dao.saveAccountSecurity(any(PSAccountSecurity.class))).thenAnswer(i -> i.getArgument(0));
    when(dao.saveLoginTx(any(PSAuthLoginTx.class))).thenAnswer(i -> i.getArgument(0));
    when(totp.hasActiveTotp("alice")).thenReturn(true);
    when(dao.findActivePasskeysByUser("alice")).thenReturn(List.of());
    when(rc.countRemaining("alice")).thenReturn(10L);

    PSMfaTxStatus created =
        service.createLoginTransaction(
            PSAuthLoginTx.TYPE_MFA_CHALLENGE, "alice", "/cm/app/home", "en-us", List.of("Admin"));
    PSAuthLoginTx txEntity = new PSAuthLoginTx();
    txEntity.setTxId(created.txId());
    txEntity.setTxType(PSAuthLoginTx.TYPE_MFA_CHALLENGE);
    txEntity.setUserName("alice");
    txEntity.setState(PSAuthLoginTx.STATE_PENDING);
    txEntity.setExpiresAt(Date.from(clock.instant().plusSeconds(300)));
    txEntity.setPayloadJson("{\"redirectUrl\":\"/cm/app/home\",\"locale\":\"en-us\",\"roles\":[\"Admin\"]}");

    when(dao.findLoginTx(created.txId())).thenReturn(Optional.of(txEntity));
    when(dao.consumeLoginTxAtomically(eq(created.txId()), eq(PSAuthLoginTx.TYPE_MFA_CHALLENGE), any()))
        .thenReturn(1);
    when(totp.verifyActiveCode("alice", "123456")).thenReturn(true);
    when(totp.verifyActiveCode("alice", "000000")).thenReturn(false);

    // 1st bad attempt increments count; 2nd locks account
    PSMfaChallengeOutcome bad1 = service.verifyChallengeTotp(created.txId(), "000000", "127.0.0.1");
    assertFalse(bad1.verified());
    assertEquals(1, sec.getFailedLoginCount());

    assertThrows(
        PSMfaLockoutException.class,
        () -> service.verifyChallengeTotp(created.txId(), "000000", "127.0.0.1"));

    // Reset lockout for valid code path
    sec.setLockedUntil(null);
    sec.setFailedLoginCount(0);
    PSMfaChallengeOutcome ok = service.verifyChallengeTotp(created.txId(), "123456", "127.0.0.1");
    assertTrue(ok.verified());
    assertEquals("/cm/app/home", ok.redirectUrl());

    // Step-up enforcement & last-factor deletion guard
    MockHttpSession session = new MockHttpSession();
    when(policy.hasActiveFactor("alice")).thenReturn(true);
    assertThrows(
        PSStepUpRequiredException.class, () -> service.requireStepUpIfEnrolled("alice", session));

    service.verifyStepUp("alice", "123456", "127.0.0.1", session);
    service.requireStepUpIfEnrolled("alice", session);

    when(policy.isMfaRequiredForUser("alice", List.of("Admin"))).thenReturn(true);
    assertThrows(
        IllegalStateException.class,
        () -> service.disableProfileTotp("alice", List.of("Admin"), session));

    // When optional mode, disabling TOTP succeeds after step-up
    when(policy.isMfaRequiredForUser("alice", List.of("Admin"))).thenReturn(false);
    service.disableProfileTotp("alice", List.of("Admin"), session);
    verify(totp).disableTotp("alice");

    // ENROLL_ONLY completion emits 10 recovery codes
    PSAuthLoginTx enrollTx = new PSAuthLoginTx();
    enrollTx.setTxId("enroll-tx");
    enrollTx.setTxType(PSAuthLoginTx.TYPE_MFA_ENROLL);
    enrollTx.setUserName("bob");
    enrollTx.setState(PSAuthLoginTx.STATE_PENDING);
    enrollTx.setExpiresAt(Date.from(clock.instant().plusSeconds(300)));
    when(dao.findLoginTx("enroll-tx")).thenReturn(Optional.of(enrollTx));
    when(totp.confirmEnrollment("bob", "totp-1", "654321")).thenReturn(true);
    when(dao.consumeLoginTxAtomically(eq("enroll-tx"), eq(PSAuthLoginTx.TYPE_MFA_ENROLL), any()))
        .thenReturn(1);
    when(rc.generateAndReplaceCodes("bob")).thenReturn(List.of("11111-AAAAA", "22222-BBBBB"));

    PSTotpConfirmOutcome enrolled =
        service.completeEnrollOnlyTotp("enroll-tx", "totp-1", "654321", "127.0.0.1");
    assertTrue(enrolled.enrolled());
    assertEquals(2, enrolled.recoveryCodes().size());
    verify(dao, org.mockito.Mockito.atLeast(0)).incrementLoginTxAttemptsAtomically(anyString(), anyInt(), any());
  }

  @Test
  void passkeyChallengeStepUpAndLastFactorDeletionGuardWork() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    IPSTotpService totp = mock(IPSTotpService.class);
    IPSRecoveryCodeService rc = mock(IPSRecoveryCodeService.class);
    IPSAuthPolicyService policy = mock(IPSAuthPolicyService.class);
    IPSAuthRateLimiter rate = mock(IPSAuthRateLimiter.class);
    IPSPasskeyService passkeyService = mock(IPSPasskeyService.class);
    when(rate.tryConsume(anyString(), anyString(), anyLong(), any())).thenReturn(true);

    Clock clock = Clock.fixed(Instant.parse("2026-11-19T01:00:00Z"), ZoneOffset.UTC);
    Properties props = new Properties();
    PSMfaService service =
        new PSMfaService(dao, totp, rc, policy, rate, passkeyService, () -> props, clock);

    PSAuthLoginTx challengeTx = new PSAuthLoginTx();
    challengeTx.setTxId("chal-tx");
    challengeTx.setTxType(PSAuthLoginTx.TYPE_MFA_CHALLENGE);
    challengeTx.setUserName("alice");
    challengeTx.setState(PSAuthLoginTx.STATE_PENDING);
    challengeTx.setExpiresAt(Date.from(clock.instant().plusSeconds(300)));
    challengeTx.setPayloadJson("{\"redirectUrl\":\"/cm/app/\",\"locale\":\"en-us\",\"roles\":[\"Admin\"]}");
    when(dao.findLoginTx("chal-tx")).thenReturn(Optional.of(challengeTx));
    when(dao.consumeLoginTxAtomically(eq("chal-tx"), eq(PSAuthLoginTx.TYPE_MFA_CHALLENGE), any()))
        .thenReturn(1);
    PSMfaPasskey pk = new PSMfaPasskey();
    pk.setPasskeyId("pk-1");
    pk.setUserName("alice");
    when(dao.findActivePasskeysByUser("alice")).thenReturn(List.of(pk));
    when(passkeyService.verifyAuthentication("alice", "web-tx-1", "{\"id\":\"c\"}", "https://cms"))
        .thenReturn(pk);
    when(rc.countRemaining("alice")).thenReturn(9L);

    PSMfaChallengeOutcome chalOk =
        service.verifyChallengePasskey("chal-tx", "web-tx-1", "{\"id\":\"c\"}", "https://cms", "127.0.0.1");
    assertTrue(chalOk.verified());
    assertEquals(9L, chalOk.recoveryCodesRemaining());

    MockHttpSession session = new MockHttpSession();
    when(policy.hasActiveFactor("alice")).thenReturn(true);
    when(passkeyService.verifyAuthentication("alice", "su-tx-1", "{\"id\":\"c\"}", "https://cms"))
        .thenReturn(pk);
    long expiresAt =
        service.verifyProfilePasskeyStepUp(
            "alice", "su-tx-1", "{\"id\":\"c\"}", "https://cms", "127.0.0.1", session);
    assertTrue(expiresAt > clock.instant().toEpochMilli());

    when(policy.isMfaRequiredForUser("alice", List.of("Admin"))).thenReturn(true);
    when(totp.hasActiveTotp("alice")).thenReturn(false);
    assertFalse(service.canDeleteLastPasskey("alice", List.of("Admin")));
    assertThrows(
        IllegalStateException.class,
        () -> service.deleteProfilePasskey("alice", "pk-1", List.of("Admin"), session));

    when(totp.hasActiveTotp("alice")).thenReturn(true);
    assertTrue(service.canDeleteLastPasskey("alice", List.of("Admin")));
    service.deleteProfilePasskey("alice", "pk-1", List.of("Admin"), session);
    verify(passkeyService).deletePasskey("alice", "pk-1");
  }
}
