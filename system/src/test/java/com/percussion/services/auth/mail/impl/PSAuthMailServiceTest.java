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
package com.percussion.services.auth.mail.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import com.percussion.services.auth.mail.PSAuthMailException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.junit.jupiter.api.Test;

class PSAuthMailServiceTest {

  @Test
  void serverPropertiesTakePrecedenceOverWorkflowFallback() {
    Properties server = new Properties();
    server.setProperty("auth.mail.host", "smtp.auth.example.com");
    server.setProperty("auth.mail.port", "587");
    server.setProperty("auth.mail.username", "auth-user");
    server.setProperty("auth.mail.password", "ENC(cipher-value)");
    server.setProperty("auth.mail.from", "no-reply@example.com");
    server.setProperty("auth.mail.fromName", "Percussion Auth");
    server.setProperty("auth.mail.startTls", "true");
    server.setProperty("auth.mail.startTlsRequired", "true");

    Properties workflow = new Properties();
    workflow.setProperty("SMTP_HOST", "smtp.workflow.example.com");
    workflow.setProperty("SMTP_BOUNCEADDR", "wf@example.com");

    PSAuthMailService service =
        new PSAuthMailService(
            () -> server, () -> workflow, cipher -> "decrypted-" + cipher, SimpleEmail::new);

    PSAuthMailConfig cfg = service.resolveEffectiveConfig();
    assertTrue(cfg.isConfigured());
    assertEquals(ConfigSource.SERVER_PROPERTIES, cfg.getSource());
    assertEquals("smtp.auth.example.com", cfg.getHost());
    assertEquals(587, cfg.getPort());
    assertEquals("decrypted-cipher-value", cfg.getPassword());
    assertEquals("no-reply@example.com", cfg.getFromAddress());
    assertFalse(cfg.toString().contains("decrypted-cipher-value"));
  }

  @Test
  void workflowFallbackIgnoresEnableTestRunModeAndSendsSyncEmail() throws Exception {
    Properties server = new Properties();
    server.setProperty("auth.mail.fallbackWorkflowSmtp", "true");

    Properties workflow = new Properties();
    workflow.setProperty("SMTP_HOST", "smtp.workflow.example.com");
    workflow.setProperty("SMTP_PORT", "2525");
    workflow.setProperty("SMTP_USERNAME", "wfuser");
    workflow.setProperty("SMTP_PASSWORD", "wfpass");
    workflow.setProperty("SMTP_BOUNCEADDR", "security@example.com");
    workflow.setProperty("SMTP_TLSENABLED", "true");
    // Must NEVER suppress SMTP delivery or route auth tokens to workflow logs
    workflow.setProperty("ENABLE_TEST_RUN_MODE", "true");

    SimpleEmail email = mock(SimpleEmail.class);
    when(email.send()).thenReturn("msg-id-1");

    PSAuthMailService service =
        new PSAuthMailService(() -> server, () -> workflow, c -> c, () -> email);

    PSAuthMailConfig cfg = service.resolveEffectiveConfig();
    assertTrue(cfg.isConfigured());
    assertEquals(ConfigSource.WORKFLOW_FALLBACK, cfg.getSource());
    assertEquals(2525, cfg.getPort());
    assertTrue(cfg.isStartTls());

    service.sendTestEmail("operator@example.com");

    verify(email).setCharset(StandardCharsets.UTF_8.name());
    verify(email).setHostName("smtp.workflow.example.com");
    verify(email).setSmtpPort(2525);
    verify(email).setStartTLSEnabled(true);
    verify(email).setStartTLSRequired(true);
    verify(email).setSSLCheckServerIdentity(true);
    verify(email).setSocketConnectionTimeout(Duration.ofMillis(10_000));
    verify(email).setSocketTimeout(Duration.ofMillis(10_000));
    verify(email).setAuthentication("wfuser", "wfpass");
    verify(email).setFrom("security@example.com", "Percussion CMS");
    verify(email).addTo("operator@example.com");
    verify(email).setSubject("Percussion CMS Authentication Mail Test");
    verify(email).send();
  }

  @Test
  void disablingWorkflowFallbackLeavesAuthMailUnconfigured() {
    Properties server = new Properties();
    server.setProperty("auth.mail.fallbackWorkflowSmtp", "false");

    Properties workflow = new Properties();
    workflow.setProperty("SMTP_HOST", "smtp.workflow.example.com");
    workflow.setProperty("SMTP_BOUNCEADDR", "security@example.com");

    SimpleEmail email = mock(SimpleEmail.class);
    PSAuthMailService service =
        new PSAuthMailService(() -> server, () -> workflow, c -> c, () -> email);

    PSAuthMailConfig cfg = service.resolveEffectiveConfig();
    assertFalse(cfg.isConfigured());
    assertEquals(ConfigSource.UNCONFIGURED, cfg.getSource());

    assertThrows(PSAuthMailException.class, () -> service.sendTestEmail("admin@example.com"));
    verifyNoInteractions(email);
  }

  @Test
  void rejectsHeaderInjectionAndInvalidRecipientsBeforeSmtpLookup() {
    SimpleEmail email = mock(SimpleEmail.class);
    PSAuthMailService service =
        new PSAuthMailService(Properties::new, Properties::new, c -> c, () -> email);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.sendTextEmail("bad\r\nBcc:evil@example.com", "Subject", "Body"));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.sendTextEmail("not-an-email", "Subject", "Body"));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.sendTextEmail("valid@example.com", "Bad\nSubject", "Body"));
    verifyNoInteractions(email);
  }

  @Test
  void wrapsEmailExceptionIntoPSAuthMailException() throws Exception {
    Properties server = new Properties();
    server.setProperty("auth.mail.host", "smtp.example.com");
    server.setProperty("auth.mail.from", "cms@example.com");

    SimpleEmail email = mock(SimpleEmail.class);
    when(email.send()).thenThrow(new EmailException("Connection refused"));

    PSAuthMailService service =
        new PSAuthMailService(() -> server, Properties::new, c -> c, () -> email);

    PSAuthMailException thrown =
        assertThrows(
            PSAuthMailException.class,
            () -> service.sendTextEmail("user@example.com", "Test", "Hello"));
    assertTrue(thrown.getMessage().contains("smtp.example.com:587"));
  }
}
