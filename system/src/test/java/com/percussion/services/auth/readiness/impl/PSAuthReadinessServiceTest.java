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
package com.percussion.services.auth.readiness.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import com.percussion.services.auth.readiness.PSAuthCheckStatus;
import com.percussion.services.auth.readiness.PSAuthInfraCheck;
import com.percussion.services.auth.readiness.PSAuthInfraReport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PSAuthReadinessServiceTest {

  private static Map<String, Boolean> allTablesPresent() {
    return Map.of(
        "PSX_ACCOUNT_SECURITY", true,
        "PSX_MFA_TOTP", true,
        "PSX_MFA_PASSKEY", true,
        "PSX_MFA_RECOVERY_CODE", true,
        "PSX_AUTH_TOKEN", true,
        "PSX_AUTH_LOGIN_TX", true);
  }

  private static PSAuthMailConfig validMailConfig() {
    return new PSAuthMailConfig(
        "smtp.example.com",
        587,
        "mailer",
        "secret",
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
        List.of());
  }

  private static PSAuthInfraCheck findCheck(PSAuthInfraReport report, String code) {
    return report.getChecks().stream()
        .filter(c -> code.equals(c.getCode()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void passesAllChecksWhenHttpsWebAuthnMailSchemaAndKeyAreConfigured(@TempDir Path rxDir)
      throws Exception {
    Path secureDir = rxDir.resolve("rxconfig").resolve("secure");
    Files.createDirectories(secureDir);
    Files.writeString(secureDir.resolve(".key"), "test-key-bytes", StandardCharsets.UTF_8);

    Properties props = new Properties();
    props.setProperty("auth.mfa.mode", "required");
    props.setProperty("auth.publicBaseUrl", "https://cms.example.com/Rhythmyx");
    props.setProperty("auth.webauthn.rpId", "example.com");
    props.setProperty("auth.webauthn.allowedOrigins", "https://cms.example.com");
    props.setProperty("auth.password.minLength", "14");

    IPSAuthMailService mailService = mock(IPSAuthMailService.class);
    when(mailService.resolveEffectiveConfig()).thenReturn(validMailConfig());

    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    when(dao.checkSchemaTablesPresent()).thenReturn(allTablesPresent());

    PSAuthReadinessService service =
        new PSAuthReadinessService(mailService, dao, () -> props, () -> rxDir);

    PSAuthInfraReport report = service.evaluateInfrastructure();
    assertEquals("required", report.getMfaMode());
    assertEquals(14, report.getPasswordMinLength());
    assertTrue(report.isMailConfigured());
    assertTrue(report.getClusterKeyFingerprint().startsWith("sha256:"));
    assertEquals(19, report.getClusterKeyFingerprint().length());
    assertFalse(report.getClusterKeyFingerprint().contains("test-key-bytes"));

    for (PSAuthInfraCheck check : report.getChecks()) {
      assertEquals(PSAuthCheckStatus.PASS, check.getStatus(), "Check failed: " + check.getCode());
    }
  }

  @Test
  void flagsInvalidWebAuthnOriginsPasswordLengthMissingTablesAndMissingMail(
      @TempDir Path rxDir) {
    Properties props = new Properties();
    props.setProperty("auth.mfa.mode", "bogus");
    props.setProperty("auth.publicBaseUrl", "http://cms.example.com/Rhythmyx");
    props.setProperty("auth.webauthn.rpId", "cms.example.com");
    props.setProperty("auth.webauthn.allowedOrigins", "https://other.org");
    props.setProperty("auth.password.minLength", "4");

    IPSAuthMailService mailService = mock(IPSAuthMailService.class);
    when(mailService.resolveEffectiveConfig())
        .thenReturn(
            new PSAuthMailConfig(
                "",
                587,
                "",
                "",
                "",
                "Percussion CMS",
                true,
                true,
                false,
                true,
                10_000,
                10_000,
                ConfigSource.UNCONFIGURED,
                true,
                List.of("No SMTP host configured")));

    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    when(dao.checkSchemaTablesPresent())
        .thenReturn(Map.of("PSX_ACCOUNT_SECURITY", true, "PSX_MFA_TOTP", false));

    PSAuthReadinessService service =
        new PSAuthReadinessService(mailService, dao, () -> props, () -> rxDir);

    PSAuthInfraReport report = service.evaluateInfrastructure();
    assertFalse(report.isMailConfigured());
    assertEquals("", report.getClusterKeyFingerprint());

    assertEquals(PSAuthCheckStatus.FAIL, findCheck(report, "AUTH_MAIL").getStatus());
    assertEquals(PSAuthCheckStatus.FAIL, findCheck(report, "PUBLIC_BASE_URL").getStatus());
    assertEquals(PSAuthCheckStatus.FAIL, findCheck(report, "WEBAUTHN_CONFIG").getStatus());
    assertEquals(PSAuthCheckStatus.FAIL, findCheck(report, "MFA_MODE").getStatus());
    assertEquals(PSAuthCheckStatus.FAIL, findCheck(report, "PASSWORD_POLICY").getStatus());
    assertEquals(PSAuthCheckStatus.WARN, findCheck(report, "ENCRYPTION_KEY").getStatus());
    assertEquals(PSAuthCheckStatus.FAIL, findCheck(report, "AUTH_SCHEMA").getStatus());
    assertTrue(findCheck(report, "AUTH_SCHEMA").getSummary().contains("PSX_MFA_TOTP"));
  }
}
