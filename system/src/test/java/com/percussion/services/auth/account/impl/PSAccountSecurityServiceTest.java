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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.account.PSEmailChangeResult;
import com.percussion.services.auth.account.PSEmailVerificationStatus;
import com.percussion.services.auth.account.PSEmailVerifyResult;
import com.percussion.services.auth.account.PSForgotUsernameResult;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import com.percussion.services.auth.ratelimit.IPSAuthRateLimiter;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.services.auth.token.IPSAuthTokenService;
import com.percussion.services.auth.token.PSConsumedAuthToken;
import com.percussion.services.auth.token.PSIssuedAuthToken;
import com.percussion.services.security.IPSBackEndRoleMgr;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class PSAccountSecurityServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-18T14:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  private IPSAuthSecurityDao dao;
  private IPSAuthTokenService tokenService;
  private IPSAuthMailService mailService;
  private IPSAuthRateLimiter rateLimiter;
  private IPSBackEndRoleMgr backEndRoleMgr;
  private Properties props;
  private PSAccountSecurityService service;

  @BeforeEach
  void setUp() {
    dao = mock(IPSAuthSecurityDao.class);
    tokenService = mock(IPSAuthTokenService.class);
    mailService = mock(IPSAuthMailService.class);
    rateLimiter = mock(IPSAuthRateLimiter.class);
    backEndRoleMgr = mock(IPSBackEndRoleMgr.class);
    props = new Properties();
    props.setProperty(PSAccountSecurityService.PROP_PUBLIC_BASE_URL, "https://cms.example.com/Rhythmyx/");

    PSAuthMailConfig mailCfg =
        new PSAuthMailConfig(
            "smtp.example.com",
            587,
            null,
            null,
            "noreply@example.com",
            "Percussion CMS",
            true,
            true,
            false,
            true,
            5000,
            5000,
            ConfigSource.SERVER_PROPERTIES,
            true,
            List.of());
    when(mailService.resolveEffectiveConfig()).thenReturn(mailCfg);
    when(rateLimiter.tryConsume(anyString(), anyString(), anyLong(), any())).thenReturn(true);
    when(dao.saveAccountSecurity(any())).thenAnswer(inv -> inv.getArgument(0));
    when(tokenService.resolveTtl(PSAuthToken.TYPE_EMAIL_VERIFY)).thenReturn(Duration.ofMinutes(30));

    service =
        new PSAccountSecurityService(
            dao, tokenService, mailService, rateLimiter, backEndRoleMgr, () -> props, FIXED_CLOCK, Runnable::run);
  }

  @Test
  void stageEmailChangePreservesActiveVerifiedEmailUsesPublicBaseUrlAndNotifiesOldAddress()
      throws Exception {
    PSAccountSecurity existing = new PSAccountSecurity();
    existing.setUserName("editor1");
    existing.setEmailNormalized("old@example.com");
    existing.setEmailVerified(true);
    existing.setEmailVerifiedAt(Date.from(FIXED_NOW.minusSeconds(3600)));
    when(dao.findAccountSecurity("editor1")).thenReturn(Optional.of(existing));

    PSIssuedAuthToken issued =
        new PSIssuedAuthToken(
            "t-1",
            "raw+tok/=",
            PSAuthToken.TYPE_EMAIL_VERIFY,
            "editor1",
            "new@example.com",
            FIXED_NOW.plus(Duration.ofMinutes(30)));
    when(tokenService.issueToken(
            eq(PSAuthToken.TYPE_EMAIL_VERIFY),
            eq("editor1"),
            eq("new@example.com"),
            eq("editor1"),
            eq("192.0.2.5"),
            eq(Duration.ofMinutes(30))))
        .thenReturn(issued);

    PSEmailChangeResult result =
        service.stageEmailChange(
            "editor1", "old@example.com", "  new@example.com  ", "192.0.2.5", true);

    assertTrue(result.verificationSent());
    assertFalse(result.appliedImmediately());
    assertEquals("old@example.com", result.activeEmail());
    assertEquals("new@example.com", result.pendingEmail());
    assertTrue(result.emailVerified());
    assertEquals("old@example.com", existing.getEmailNormalized());
    assertEquals("new@example.com", existing.getPendingEmail());
    assertTrue(existing.isEmailVerified());

    verify(backEndRoleMgr, never()).setSubjectEmail(anyString(), anyString());

    ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
    verify(mailService)
        .sendTextEmail(eq("new@example.com"), anyString(), bodyCap.capture());
    assertTrue(
        bodyCap
            .getValue()
            .contains("https://cms.example.com/Rhythmyx/auth/verify-email?token=raw%2Btok%2F%3D"));

    verify(mailService)
        .sendTextEmail(
            eq("old@example.com"),
            contains("email change requested"),
            contains("new@example.com"));
  }

  @Test
  void stageEmailChangeRejectsDirectoryMalformedMissingBaseUrlMissingSmtpAndRateLimit() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.stageEmailChange(
                "ldap.user", "old@example.com", "new@example.com", "192.0.2.5", false));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.stageEmailChange("editor1", "", "bad-email", "192.0.2.5", true));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.stageEmailChange("editor1", "old@example.com", "  ", "192.0.2.5", true));

    when(dao.findAccountSecurity("editor1")).thenReturn(Optional.empty());
    props.remove(PSAccountSecurityService.PROP_PUBLIC_BASE_URL);
    assertThrows(
        IllegalStateException.class,
        () -> service.stageEmailChange("editor1", "", "new@example.com", "192.0.2.5", true));

    props.setProperty(PSAccountSecurityService.PROP_PUBLIC_BASE_URL, "https://cms.example.com");
    when(mailService.resolveEffectiveConfig())
        .thenReturn(
            new PSAuthMailConfig(
                "",
                587,
                "",
                "",
                "",
                "",
                true,
                true,
                false,
                true,
                5000,
                5000,
                ConfigSource.UNCONFIGURED,
                false,
                List.of("SMTP host is missing")));
    assertThrows(
        IllegalStateException.class,
        () -> service.stageEmailChange("editor1", "", "new@example.com", "192.0.2.5", true));

    when(mailService.resolveEffectiveConfig())
        .thenReturn(
            new PSAuthMailConfig(
                "smtp.example.com",
                587,
                null,
                null,
                "noreply@example.com",
                "Percussion CMS",
                true,
                true,
                false,
                true,
                5000,
                5000,
                ConfigSource.SERVER_PROPERTIES,
                true,
                List.of()));
    when(rateLimiter.tryConsume(eq("email_verify_resend"), eq("editor1"), anyLong(), any()))
        .thenReturn(false);
    assertThrows(
        PSAuthRateLimitException.class,
        () -> service.stageEmailChange("editor1", "", "new@example.com", "192.0.2.5", true));
  }

  @Test
  void resendVerificationTargetsPendingOrUnverifiedActiveAndRejectsAlreadyVerified()
      throws Exception {
    PSAccountSecurity verified = new PSAccountSecurity();
    verified.setUserName("alice");
    verified.setEmailNormalized("alice@example.com");
    verified.setEmailVerified(true);
    when(dao.findAccountSecurity("alice")).thenReturn(Optional.of(verified));

    assertThrows(
        IllegalArgumentException.class,
        () -> service.resendVerification("alice", "alice@example.com", "192.0.2.1"));

    PSAccountSecurity unverified = new PSAccountSecurity();
    unverified.setUserName("bob");
    unverified.setEmailNormalized("bob@example.com");
    unverified.setEmailVerified(false);
    when(dao.findAccountSecurity("bob")).thenReturn(Optional.of(unverified));
    when(tokenService.issueToken(anyString(), eq("bob"), eq("bob@example.com"), eq("bob"), any(), any()))
        .thenReturn(
            new PSIssuedAuthToken(
                "tok-bob",
                "bobraw",
                PSAuthToken.TYPE_EMAIL_VERIFY,
                "bob",
                "bob@example.com",
                FIXED_NOW.plusSeconds(1800)));

    PSEmailChangeResult resent = service.resendVerification("bob", "bob@example.com", "192.0.2.1");
    assertTrue(resent.verificationSent());
    assertEquals("bob@example.com", resent.pendingEmail());
    verify(mailService).sendTextEmail(eq("bob@example.com"), anyString(), contains("bobraw"));
  }

  @Test
  void confirmEmailVerificationPromotesPendingEmailAndHandlesBackendFailure() {
    when(tokenService.consumeToken("bad", PSAuthToken.TYPE_EMAIL_VERIFY))
        .thenReturn(Optional.empty());
    PSEmailVerifyResult invalid = service.confirmEmailVerification("bad");
    assertFalse(invalid.verified());
    assertEquals(PSEmailVerifyResult.STATUS_INVALID_OR_EXPIRED, invalid.status());

    PSConsumedAuthToken consumed =
        new PSConsumedAuthToken(
            "t1", PSAuthToken.TYPE_EMAIL_VERIFY, "editor1", "Verified.New@example.com", "editor1");
    when(tokenService.consumeToken("good-token", PSAuthToken.TYPE_EMAIL_VERIFY))
        .thenReturn(Optional.of(consumed));

    PSAccountSecurity sec = new PSAccountSecurity();
    sec.setUserName("editor1");
    sec.setEmailNormalized("old@example.com");
    sec.setEmailVerified(true);
    sec.setPendingEmail("Verified.New@example.com");
    when(dao.findAccountSecurity("editor1")).thenReturn(Optional.of(sec));

    PSEmailVerifyResult ok = service.confirmEmailVerification("good-token");
    assertTrue(ok.verified());
    assertEquals(PSEmailVerifyResult.STATUS_VERIFIED, ok.status());
    verify(backEndRoleMgr).setSubjectEmail("editor1", "Verified.New@example.com");
    assertEquals("verified.new@example.com", sec.getEmailNormalized());
    assertTrue(sec.isEmailVerified());
    assertNotNull(sec.getEmailVerifiedAt());
    assertNull(sec.getPendingEmail());

    doThrow(new RuntimeException("role store locked"))
        .when(backEndRoleMgr)
        .setSubjectEmail("editor2", "fail@example.com");
    when(tokenService.consumeToken("fail-write", PSAuthToken.TYPE_EMAIL_VERIFY))
        .thenReturn(
            Optional.of(
                new PSConsumedAuthToken(
                    "t2", PSAuthToken.TYPE_EMAIL_VERIFY, "editor2", "fail@example.com", "editor2")));

    PSEmailVerifyResult failed = service.confirmEmailVerification("fail-write");
    assertFalse(failed.verified());
    assertEquals(PSEmailVerifyResult.STATUS_PERSIST_FAILED, failed.status());
  }

  @Test
  void requestForgotUsernameIsEnumerationSafeRespectsUniquenessAndRateLimits() throws Exception {
    when(dao.findVerifiedByNormalizedEmail("unknown@example.com")).thenReturn(List.of());
    PSForgotUsernameResult unknown =
        service.requestForgotUsername("unknown@example.com", "198.51.100.1");
    assertTrue(unknown.accepted());

    PSAccountSecurity alice = new PSAccountSecurity();
    alice.setUserName("alice");
    alice.setEmailNormalized("alice@example.com");
    alice.setEmailVerified(true);
    when(dao.findVerifiedByNormalizedEmail("alice@example.com")).thenReturn(List.of(alice));

    PSForgotUsernameResult found =
        service.requestForgotUsername("Alice@Example.com", "198.51.100.1");
    assertEquals(unknown.message(), found.message());
    verify(mailService)
        .sendTextEmail(eq("Alice@Example.com"), anyString(), contains("Username: alice"));

    // Ambiguous verified email suppressed when auth.email.uniqueForRecovery=true
    props.setProperty(PSAccountSecurityService.PROP_UNIQUE_EMAIL_RECOVERY, "true");
    PSAccountSecurity bob = new PSAccountSecurity();
    bob.setUserName("bob");
    bob.setEmailNormalized("shared@example.com");
    bob.setEmailVerified(true);
    when(dao.findVerifiedByNormalizedEmail("shared@example.com"))
        .thenReturn(List.of(alice, bob));

    PSForgotUsernameResult ambiguous =
        service.requestForgotUsername("shared@example.com", "198.51.100.2");
    assertEquals(unknown.message(), ambiguous.message());
    verify(mailService, never()).sendTextEmail(eq("shared@example.com"), anyString(), anyString());

    // Per-email throttle returns silent identical generic message (no email-existence oracle)
    when(rateLimiter.tryConsume(eq("forgot_username_email"), eq("alice@example.com"), anyLong(), any()))
        .thenReturn(false);
    PSForgotUsernameResult emailThrottled =
        service.requestForgotUsername("alice@example.com", "198.51.100.3");
    assertEquals(unknown.message(), emailThrottled.message());

    // Per-IP burst raises PSAuthRateLimitException
    when(rateLimiter.tryConsume(eq("forgot_username_ip"), eq("198.51.100.99"), anyLong(), any()))
        .thenReturn(false);
    assertThrows(
        PSAuthRateLimitException.class,
        () -> service.requestForgotUsername("alice@example.com", "198.51.100.99"));
  }

  @Test
  void directorySyncAndServiceAccountPolicyWorkAccordingToConfig() {
    PSAccountSecurity dirSec = new PSAccountSecurity();
    dirSec.setUserName("ldap.user");
    dirSec.setEmailNormalized("old@corp.example");
    dirSec.setEmailVerified(true);
    when(dao.findAccountSecurity("ldap.user")).thenReturn(Optional.of(dirSec));

    PSEmailVerificationStatus changedStatus =
        service.getEmailStatus("ldap.user", "new@corp.example", true);
    assertFalse(changedStatus.emailVerified());
    assertEquals("new@corp.example", changedStatus.activeEmail());
    assertFalse(dirSec.isEmailVerified());

    props.setProperty(PSAccountSecurityService.PROP_DIRECTORY_TRUST_VERIFIED, "true");
    service.syncAccountEmailOnAdminSave("ldap.user2", "trusted@corp.example", true);
    ArgumentCaptor<PSAccountSecurity> savedCap = ArgumentCaptor.forClass(PSAccountSecurity.class);
    verify(dao, org.mockito.Mockito.times(2)).saveAccountSecurity(savedCap.capture());
    assertTrue(savedCap.getValue().isEmailVerified());

    assertTrue(service.isSystemServiceUser("PercussionAdmin"));
    when(dao.findAccountSecurity("PercussionAdmin")).thenReturn(Optional.empty());
    service.syncAccountEmailOnAdminSave("PercussionAdmin", "", false);
    verify(dao, org.mockito.Mockito.times(3)).saveAccountSecurity(savedCap.capture());
    assertEquals(
        PSAccountSecurity.POLICY_NON_INTERACTIVE_SERVICE,
        savedCap.getValue().getAccountPolicy());
  }
}
