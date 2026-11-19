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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.security.PSPasswordHandler;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.data.PSMfaTotp;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.services.auth.ratelimit.impl.PSAuthRateLimiter;
import com.percussion.services.auth.recovery.PSAdminRecoveryIssueResult;
import com.percussion.services.auth.recovery.PSAdminRecoveryRedeemResult;
import com.percussion.services.auth.recovery.PSForgotPasswordResult;
import com.percussion.services.auth.recovery.PSPasswordResetResult;
import com.percussion.services.auth.token.IPSAuthTokenService;
import com.percussion.services.auth.token.PSConsumedAuthToken;
import com.percussion.services.auth.token.PSIssuedAuthToken;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PSAccountRecoveryServiceTest {

  private IPSAuthSecurityDao dao;
  private IPSAuthTokenService tokenService;
  private IPSAuthMailService mailService;
  private IPSAuthRateLimiter rateLimiter;
  private IPSMfaService mfaService;
  private Properties props;
  private List<String> revokedUsers;
  private PSAccountRecoveryService service;

  @BeforeEach
  void setUp() {
    dao = mock(IPSAuthSecurityDao.class);
    tokenService = mock(IPSAuthTokenService.class);
    mailService = mock(IPSAuthMailService.class);
    rateLimiter = new PSAuthRateLimiter();
    mfaService = mock(IPSMfaService.class);
    props = new Properties();
    props.setProperty("auth.publicBaseUrl", "https://cms.example.com/Rhythmyx");
    props.setProperty("auth.password.minLength", "12");
    revokedUsers = new ArrayList<>();

    when(mailService.resolveEffectiveConfig())
        .thenReturn(
            new PSAuthMailConfig(
                "smtp.example.com",
                587,
                "",
                "",
                "noreply@example.com",
                "Percussion CMS",
                true,
                true,
                false,
                true,
                10_000,
                10_000,
                PSAuthMailConfig.ConfigSource.SERVER_PROPERTIES,
                true,
                List.of()));
    when(tokenService.resolveTtl(PSAuthToken.TYPE_PASSWORD_RESET))
        .thenReturn(Duration.ofMinutes(20));
    when(tokenService.resolveTtl(PSAuthToken.TYPE_ADMIN_BREAK_GLASS))
        .thenReturn(Duration.ofMinutes(60));

    service =
        new PSAccountRecoveryService(
            dao,
            tokenService,
            mailService,
            rateLimiter,
            mfaService,
            () -> props,
            Clock.fixed(Instant.parse("2026-11-19T02:00:00Z"), ZoneOffset.UTC),
            revokedUsers::add);
  }

  @Test
  void requestPasswordResetSendsLinkForInternalUserAndRedirectsDirectoryUsersGenerically()
      throws Exception {
    PSAccountSecurity internalAcct = verifiedAccount("alice", "alice@example.com");
    when(dao.findAccountSecurity("alice")).thenReturn(Optional.of(internalAcct));
    when(dao.findInternalLoginUserId("alice")).thenReturn(Optional.of("alice"));
    when(tokenService.issueToken(
            eq(PSAuthToken.TYPE_PASSWORD_RESET),
            eq("alice"),
            eq("alice@example.com"),
            eq("alice"),
            eq("10.0.0.5"),
            eq(Duration.ofMinutes(20))))
        .thenReturn(
            new PSIssuedAuthToken(
                "tok-id-1",
                "raw-reset-tok+1",
                PSAuthToken.TYPE_PASSWORD_RESET,
                "alice",
                "alice@example.com",
                Instant.parse("2026-11-19T02:20:00Z")));

    PSForgotPasswordResult internalRes = service.requestPasswordReset("alice", "10.0.0.5");
    assertTrue(internalRes.accepted());

    ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
    verify(mailService)
        .sendTextEmail(
            eq("alice@example.com"), eq("Reset your Percussion CMS password"), bodyCap.capture());
    assertTrue(
        bodyCap
            .getValue()
            .contains("https://cms.example.com/Rhythmyx/auth/reset-password?token=raw-reset-tok%2B1"));
    assertTrue(bodyCap.getValue().contains("two-step verification"));

    PSAccountSecurity dirAcct = verifiedAccount("ldap.bob", "bob@example.com");
    when(dao.findVerifiedByNormalizedEmail("bob@example.com")).thenReturn(List.of(dirAcct));
    when(dao.findInternalLoginUserId("ldap.bob")).thenReturn(Optional.empty());

    PSForgotPasswordResult dirRes =
        service.requestPasswordReset("bob@example.com", "10.0.0.6");
    assertEquals(internalRes.message(), dirRes.message());
    verify(tokenService, never())
        .issueToken(
            eq(PSAuthToken.TYPE_PASSWORD_RESET),
            eq("ldap.bob"),
            anyString(),
            anyString(),
            anyString(),
            any());
    ArgumentCaptor<String> dirBodyCap = ArgumentCaptor.forClass(String.class);
    verify(mailService)
        .sendTextEmail(
            eq("bob@example.com"),
            eq("Percussion CMS password recovery guidance"),
            dirBodyCap.capture());
    assertTrue(dirBodyCap.getValue().contains("external directory"));

    // Ambiguous verified email returns identical message without sending email
    when(dao.findVerifiedByNormalizedEmail("shared@example.com"))
        .thenReturn(
            List.of(
                verifiedAccount("u1", "shared@example.com"),
                verifiedAccount("u2", "shared@example.com")));
    PSForgotPasswordResult ambigRes =
        service.requestPasswordReset("shared@example.com", "10.0.0.7");
    assertEquals(internalRes.message(), ambigRes.message());

    // Unverified account returns identical message without sending email
    PSAccountSecurity unverified = new PSAccountSecurity();
    unverified.setUserName("unverified");
    unverified.setEmailNormalized("u@example.com");
    unverified.setEmailVerified(false);
    when(dao.findAccountSecurity("unverified")).thenReturn(Optional.of(unverified));
    PSForgotPasswordResult unvRes = service.requestPasswordReset("unverified", "10.0.0.8");
    assertEquals(internalRes.message(), unvRes.message());
  }

  @Test
  void completePasswordResetEnforcesPolicyBeforeConsumeHashesUpdatesBumpsVersionAndPreservesMfa()
      throws Exception {
    PSPasswordResetResult shortFail =
        service.completePasswordReset("tok-raw", "TooShort1!", "10.0.0.9");
    assertFalse(shortFail.reset());
    assertEquals(PSPasswordResetResult.STATUS_PASSWORD_POLICY_VIOLATION, shortFail.status());
    verify(tokenService, never()).consumeToken(anyString(), anyString());

    when(tokenService.consumeToken("tok-raw", PSAuthToken.TYPE_PASSWORD_RESET))
        .thenReturn(
            Optional.of(
                new PSConsumedAuthToken(
                    "tid-1",
                    PSAuthToken.TYPE_PASSWORD_RESET,
                    "alice",
                    "alice@example.com",
                    "alice")));
    when(dao.findInternalLoginUserId("alice")).thenReturn(Optional.of("alice"));
    when(dao.updateInternalUserPassword(eq("alice"), anyString())).thenReturn(1);
    when(dao.recordPasswordChangeAndBumpSessionVersion(eq("alice"), any(Date.class)))
        .thenReturn(9L);

    String newPassword = "CorrectHorseBattery123!";
    PSPasswordResetResult ok = service.completePasswordReset("tok-raw", newPassword, "10.0.0.9");
    assertTrue(ok.reset());
    assertEquals(PSPasswordResetResult.STATUS_RESET, ok.status());

    ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
    verify(dao).updateInternalUserPassword(eq("alice"), hashCap.capture());
    assertNotEquals(newPassword, hashCap.getValue());
    assertTrue(PSPasswordHandler.checkHashedPassword(newPassword, hashCap.getValue()));

    verify(dao).recordPasswordChangeAndBumpSessionVersion(eq("alice"), any(Date.class));
    verify(dao)
        .invalidateUnconsumedTokens(
            "alice", PSAuthToken.TYPE_PASSWORD_RESET, Instant.parse("2026-11-19T02:00:00Z"));
    assertEquals(List.of("alice"), revokedUsers);

    // Enrolled MFA factors MUST remain untouched by password reset
    verify(dao, never()).revokeTotpByUser(anyString(), anyString());
    verify(dao, never()).revokeAllPasskeysByUser(anyString());
    verify(dao, never()).invalidateUnusedRecoveryCodesByUser(anyString(), any());
  }

  @Test
  void concurrentPasswordResetTokenConsumptionAllowsExactlyOneWinner() throws Exception {
    AtomicBoolean used = new AtomicBoolean(false);
    when(tokenService.consumeToken("race-token", PSAuthToken.TYPE_PASSWORD_RESET))
        .thenAnswer(
            inv ->
                used.compareAndSet(false, true)
                    ? Optional.of(
                        new PSConsumedAuthToken(
                            "tid-race",
                            PSAuthToken.TYPE_PASSWORD_RESET,
                            "alice",
                            "alice@example.com",
                            "alice"))
                    : Optional.empty());
    when(dao.findInternalLoginUserId("alice")).thenReturn(Optional.of("alice"));
    when(dao.updateInternalUserPassword(eq("alice"), anyString())).thenReturn(1);
    when(dao.recordPasswordChangeAndBumpSessionVersion(eq("alice"), any(Date.class)))
        .thenReturn(2L);

    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Callable<PSPasswordResetResult>> tasks = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        tasks.add(
            () ->
                service.completePasswordReset(
                    "race-token", "StrongPasswordForRace12!", "10.0.0.10"));
      }
      List<Future<PSPasswordResetResult>> futures = pool.invokeAll(tasks);
      AtomicInteger wins = new AtomicInteger();
      AtomicInteger losses = new AtomicInteger();
      for (Future<PSPasswordResetResult> f : futures) {
        PSPasswordResetResult r = f.get();
        if (r.reset()) {
          assertEquals(PSPasswordResetResult.STATUS_RESET, r.status());
          wins.incrementAndGet();
        } else {
          assertEquals(PSPasswordResetResult.STATUS_INVALID_OR_EXPIRED, r.status());
          losses.incrementAndGet();
        }
      }
      assertEquals(1, wins.get());
      assertEquals(threads - 1, losses.get());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void adminBreakGlassRequiresStepUpValidatesTargetAndRevokesFactorsAndSessions() throws Exception {
    HttpSession adminSess = mock(HttpSession.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.issueAdminBreakGlassRecovery(
                "admin1", "admin1", "Verified via helpdesk ticket #44", false, "10.0.0.1", adminSess));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.issueAdminBreakGlassRecovery(
                "admin1", "alice", "short", false, "10.0.0.1", adminSess));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.issueAdminBreakGlassRecovery(
                "admin1", "rxserver", "Verified via helpdesk ticket #44", false, "10.0.0.1", adminSess));

    doThrow(new PSStepUpRequiredException("Step-up required"))
        .when(mfaService)
        .requireStepUpIfEnrolled("admin1", adminSess);
    assertThrows(
        PSStepUpRequiredException.class,
        () ->
            service.issueAdminBreakGlassRecovery(
                "admin1", "alice", "Verified phone call #101", false, "10.0.0.1", adminSess));

    // Step-up passes -> issue recovery link and then redeem it
    org.mockito.Mockito.reset(mfaService);
    PSAccountSecurity aliceSec = verifiedAccount("alice", "alice@example.com");
    aliceSec.setFailedLoginCount(5);
    aliceSec.setLockedUntil(new Date());
    when(dao.findAccountSecurity("alice")).thenReturn(Optional.of(aliceSec));
    when(tokenService.issueToken(
            eq(PSAuthToken.TYPE_ADMIN_BREAK_GLASS),
            eq("alice"),
            eq("alice@example.com"),
            eq("admin1"),
            eq("10.0.0.1"),
            eq(Duration.ofMinutes(60))))
        .thenReturn(
            new PSIssuedAuthToken(
                "bg-1",
                "raw-bg-tok",
                PSAuthToken.TYPE_ADMIN_BREAK_GLASS,
                "alice",
                "alice@example.com",
                Instant.parse("2026-11-19T03:00:00Z")));

    PSAdminRecoveryIssueResult linkRes =
        service.issueAdminBreakGlassRecovery(
            "admin1", "alice", "Verified via helpdesk video call #99", false, "10.0.0.1", adminSess);
    assertTrue(linkRes.issued());
    assertEquals(PSAdminRecoveryIssueResult.MODE_RECOVERY_LINK, linkRes.recoveryMode());
    assertEquals(PSAdminRecoveryIssueResult.DELIVERY_EMAIL, linkRes.delivery());

    when(tokenService.consumeToken("raw-bg-tok", PSAuthToken.TYPE_ADMIN_BREAK_GLASS))
        .thenReturn(
            Optional.of(
                new PSConsumedAuthToken(
                    "bg-1",
                    PSAuthToken.TYPE_ADMIN_BREAK_GLASS,
                    "alice",
                    "alice@example.com",
                    "admin1")));
    when(dao.saveAccountSecurity(any(PSAccountSecurity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(dao.incrementSessionVersion("alice")).thenReturn(5L);

    PSAdminRecoveryRedeemResult redeemed = service.redeemAdminBreakGlass("raw-bg-tok", "10.0.0.2");
    assertTrue(redeemed.redeemed());
    assertEquals(PSAdminRecoveryRedeemResult.STATUS_REDEEMED, redeemed.status());
    verify(dao).revokeTotpByUser("alice", PSMfaTotp.STATUS_ACTIVE);
    verify(dao).revokeTotpByUser("alice", PSMfaTotp.STATUS_PENDING);
    verify(dao).revokeAllPasskeysByUser("alice");
    verify(dao).invalidateUnusedRecoveryCodesByUser(eq("alice"), any(Date.class));
    verify(dao).incrementSessionVersion("alice");
    assertEquals(PSAccountSecurity.MFA_STATE_REQUIRED, aliceSec.getMfaEnrollmentState());
    assertEquals(0, aliceSec.getFailedLoginCount());
    assertNull(aliceSec.getLockedUntil());
    assertEquals(List.of("alice"), revokedUsers);

    // Rate limit throws PSAuthRateLimitException when limiter exhausted
    IPSAuthRateLimiter blocked = mock(IPSAuthRateLimiter.class);
    when(blocked.tryConsume(anyString(), anyString(), anyLong(), any())).thenReturn(false);
    PSAccountRecoveryService limitedService =
        new PSAccountRecoveryService(
            dao,
            tokenService,
            mailService,
            blocked,
            mfaService,
            () -> props,
            Clock.systemUTC(),
            revokedUsers::add);
    assertThrows(
        PSAuthRateLimitException.class,
        () -> limitedService.requestPasswordReset("alice", "10.0.0.5"));
    assertEquals(12, service.resolvePasswordMinLength());
  }

  private static PSAccountSecurity verifiedAccount(String userName, String email) {
    PSAccountSecurity sec = new PSAccountSecurity();
    sec.setUserName(userName);
    sec.setEmailNormalized(email);
    sec.setEmailVerified(true);
    return sec;
  }
}
