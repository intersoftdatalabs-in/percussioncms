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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.percussion.rest.auth.AuthAccountReadinessItem;
import com.percussion.rest.auth.AuthMailTestRequest;
import com.percussion.rest.auth.AuthMailTestResult;
import com.percussion.rest.auth.AuthReadinessReport;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import com.percussion.services.auth.mail.PSAuthMailException;
import com.percussion.services.auth.readiness.IPSAuthReadinessService;
import com.percussion.services.auth.readiness.PSAuthCheckStatus;
import com.percussion.services.auth.readiness.PSAuthInfraCheck;
import com.percussion.services.auth.readiness.PSAuthInfraReport;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserList;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class AuthReadinessAdaptorTest {

  private static PSAuthInfraReport sampleInfra(PSAuthCheckStatus status) {
    return new PSAuthInfraReport(
        Instant.parse("2026-11-19T00:00:00Z"),
        "optional",
        12,
        true,
        ConfigSource.SERVER_PROPERTIES,
        "sha256:0123456789ab",
        List.of(new PSAuthInfraCheck("AUTH_MAIL", "Auth Mail", status, "OK", "")),
        Map.of("PSX_ACCOUNT_SECURITY", true));
  }

  private static PSUser makeUser(String name, PSUserProviderType type, String email) {
    PSUser user = new PSUser();
    user.setName(name);
    user.setProviderType(type);
    user.setEmail(email);
    user.setRoles(List.of("Editor"));
    return user;
  }

  @Test
  void rejectsNonAdminCallersBeforeTouchingServices() {
    IPSAuthReadinessService readiness = mock(IPSAuthReadinessService.class);
    IPSAuthMailService mail = mock(IPSAuthMailService.class);
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    IPSUserService users = mock(IPSUserService.class);

    AuthReadinessAdaptor adaptor =
        new AuthReadinessAdaptor(
            readiness, mail, dao, users, () -> List.of("Editor"), () -> "editor1");

    assertThrows(SecurityException.class, adaptor::getReadinessReport);
    assertThrows(
        SecurityException.class,
        () -> adaptor.sendTestEmail(new AuthMailTestRequest("admin@example.com")));
    verifyNoInteractions(readiness, mail, dao, users);
  }

  @Test
  void classifiesEmailAndMfaReadinessAcrossUsers() throws Exception {
    IPSAuthReadinessService readiness = mock(IPSAuthReadinessService.class);
    when(readiness.evaluateInfrastructure()).thenReturn(sampleInfra(PSAuthCheckStatus.PASS));

    IPSAuthMailService mail = mock(IPSAuthMailService.class);
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);

    PSAccountSecurity readySec = new PSAccountSecurity();
    readySec.setUserName("readyUser");
    readySec.setEmailNormalized("ready@example.com");
    readySec.setEmailVerified(true);
    readySec.setMfaEnrollmentState(PSAccountSecurity.MFA_STATE_ENROLLED);

    PSAccountSecurity unverifiedSec = new PSAccountSecurity();
    unverifiedSec.setUserName("dirUser");
    unverifiedSec.setEmailNormalized("shared@example.com");
    unverifiedSec.setEmailVerified(false);

    when(dao.findAllAccountSecurity()).thenReturn(List.of(readySec, unverifiedSec));
    when(dao.countActiveTotpByUsers()).thenReturn(Map.of("readyUser", 1));
    when(dao.countActivePasskeysByUsers()).thenReturn(Map.of());
    when(dao.countUnusedRecoveryCodesByUsers()).thenReturn(Map.of("readyUser", 10));

    IPSUserService users = mock(IPSUserService.class);
    PSUserList list = new PSUserList();
    list.setUsers(List.of("readyUser", "missingUser", "badEmailUser", "dup1", "dirUser"));
    when(users.getUsers()).thenReturn(list);
    when(users.find("readyUser"))
        .thenReturn(makeUser("readyUser", PSUserProviderType.INTERNAL, "ready@example.com"));
    when(users.find("missingUser"))
        .thenReturn(makeUser("missingUser", PSUserProviderType.INTERNAL, ""));
    when(users.find("badEmailUser"))
        .thenReturn(makeUser("badEmailUser", PSUserProviderType.INTERNAL, "not-an-email"));
    when(users.find("dup1"))
        .thenReturn(makeUser("dup1", PSUserProviderType.INTERNAL, "Shared@example.com"));
    when(users.find("dirUser"))
        .thenReturn(makeUser("dirUser", PSUserProviderType.DIRECTORY, "shared@example.com"));

    AuthReadinessAdaptor adaptor =
        new AuthReadinessAdaptor(
            readiness, mail, dao, users, () -> List.of("Admin"), () -> "admin1");

    AuthReadinessReport report = adaptor.getReadinessReport();
    assertEquals("WARN", report.getOverallStatus());
    assertEquals("sha256:0123456789ab", report.getClusterKeyFingerprint());

    assertEquals(5, report.getAccountSummary().getTotalAccounts());
    assertEquals(4, report.getAccountSummary().getInternalAccounts());
    assertEquals(1, report.getAccountSummary().getDirectoryAccounts());
    assertEquals(1, report.getAccountSummary().getReadyCount());
    assertEquals(1, report.getAccountSummary().getMissingEmailCount());
    assertEquals(1, report.getAccountSummary().getMalformedEmailCount());
    assertEquals(2, report.getAccountSummary().getDuplicateEmailCount());
    assertEquals(2, report.getAccountSummary().getUnverifiedEmailCount());
    assertEquals(1, report.getAccountSummary().getMfaEnrolledCount());
    assertEquals(1, report.getAccountSummary().getTotpEnrolledCount());

    assertEquals(4, report.getAccountIssues().size());
    AuthAccountReadinessItem dirItem =
        report.getAccountIssues().stream()
            .filter(i -> "dirUser".equals(i.getUserName()))
            .findFirst()
            .orElseThrow();
    assertTrue(dirItem.getIssues().contains(AuthReadinessAdaptor.ISSUE_DUPLICATE_EMAIL));
    assertTrue(dirItem.getIssues().contains(AuthReadinessAdaptor.ISSUE_UNVERIFIED_EMAIL));
    assertTrue(dirItem.getIssues().contains(AuthReadinessAdaptor.ISSUE_NO_MFA_ENROLLED));
  }

  @Test
  void sendTestEmailValidatesAndHandlesSuccessAndFailure() throws Exception {
    IPSAuthReadinessService readiness = mock(IPSAuthReadinessService.class);
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    IPSUserService users = mock(IPSUserService.class);
    IPSAuthMailService mail = mock(IPSAuthMailService.class);

    when(mail.resolveEffectiveConfig())
        .thenReturn(
            new PSAuthMailConfig(
                "smtp.example.com",
                587,
                "",
                "",
                "cms@example.com",
                "Percussion CMS",
                true,
                true,
                false,
                true,
                10_000,
                10_000,
                ConfigSource.SERVER_PROPERTIES,
                true,
                List.of()));

    AuthReadinessAdaptor adaptor =
        new AuthReadinessAdaptor(
            readiness, mail, dao, users, () -> List.of("Admin"), () -> "admin1");

    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.sendTestEmail(new AuthMailTestRequest("invalid")));

    AuthMailTestResult ok = adaptor.sendTestEmail(new AuthMailTestRequest("ops@example.com"));
    assertTrue(ok.isSent());
    assertEquals("SERVER_PROPERTIES", ok.getSource());
    verify(mail).sendTestEmail("ops@example.com");

    doThrow(new PSAuthMailException("SMTP timeout"))
        .when(mail)
        .sendTestEmail("ops@example.com");
    AuthMailTestResult fail = adaptor.sendTestEmail(new AuthMailTestRequest("ops@example.com"));
    assertFalse(fail.isSent());
    assertTrue(fail.getMessage().contains("SMTP timeout"));
  }
}
