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

import com.percussion.security.PSEncryptor;
import com.percussion.security.PSEncryptionException;
import com.percussion.server.PSServer;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import com.percussion.services.auth.mail.PSAuthMailException;
import com.percussion.system.utils.PSBaseBean;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Synchronous transactional authentication mail service.
 *
 * <p>Reads {@code auth.mail.*} properties from {@code server.properties}, with an optional fallback
 * to {@code rxconfig/Workflow/rxworkflow.properties} {@code SMTP_*} keys when {@code
 * auth.mail.fallbackWorkflowSmtp=true} and {@code auth.mail.host} is unset. Explicitly ignores {@code
 * ENABLE_TEST_RUN_MODE} so authentication tokens and codes are never written to {@code
 * notifications.log}.
 */
@PSBaseBean("sys_authMailService")
public class PSAuthMailService implements IPSAuthMailService {

  private static final Logger log = LogManager.getLogger(PSAuthMailService.class);

  public static final String PROP_HOST = "auth.mail.host";
  public static final String PROP_PORT = "auth.mail.port";
  public static final String PROP_USERNAME = "auth.mail.username";
  public static final String PROP_PASSWORD = "auth.mail.password";
  public static final String PROP_FROM = "auth.mail.from";
  public static final String PROP_FROM_NAME = "auth.mail.fromName";
  public static final String PROP_STARTTLS = "auth.mail.startTls";
  public static final String PROP_STARTTLS_REQUIRED = "auth.mail.startTlsRequired";
  public static final String PROP_SSL = "auth.mail.ssl";
  public static final String PROP_SSL_CHECK_IDENTITY = "auth.mail.sslCheckServerIdentity";
  public static final String PROP_CONNECT_TIMEOUT_MS = "auth.mail.connectTimeoutMs";
  public static final String PROP_SOCKET_TIMEOUT_MS = "auth.mail.socketTimeoutMs";
  public static final String PROP_FALLBACK_WORKFLOW_SMTP = "auth.mail.fallbackWorkflowSmtp";

  private static final String ENC_PREFIX = "ENC(";
  private static final String ENC_SUFFIX = ")";
  private static final int DEFAULT_PORT = 587;
  private static final int DEFAULT_TIMEOUT_MS = 10_000;
  private static final String DEFAULT_FROM_NAME = "Percussion CMS";

  private final Supplier<Properties> serverPropsSupplier;
  private final Supplier<Properties> workflowPropsSupplier;
  private final UnaryOperator<String> secretDecryptor;
  private final Supplier<SimpleEmail> emailSupplier;

  @Autowired
  public PSAuthMailService() {
    this(
        PSAuthMailService::defaultServerProperties,
        PSAuthMailService::loadWorkflowPropertiesFromDisk,
        PSAuthMailService::decryptWithServerKey,
        SimpleEmail::new);
  }

  /** Package-visible constructor for isolated unit tests. */
  PSAuthMailService(
      Supplier<Properties> serverPropsSupplier,
      Supplier<Properties> workflowPropsSupplier,
      UnaryOperator<String> secretDecryptor,
      Supplier<SimpleEmail> emailSupplier) {
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.workflowPropsSupplier =
        Objects.requireNonNull(workflowPropsSupplier, "workflowPropsSupplier");
    this.secretDecryptor = Objects.requireNonNull(secretDecryptor, "secretDecryptor");
    this.emailSupplier = Objects.requireNonNull(emailSupplier, "emailSupplier");
  }

  @Override
  public PSAuthMailConfig resolveEffectiveConfig() {
    Properties serverProps = safeProps(serverPropsSupplier.get());
    boolean fallbackEnabled =
        parseBoolean(serverProps.getProperty(PROP_FALLBACK_WORKFLOW_SMTP), true);
    List<String> errors = new ArrayList<>();

    boolean sslCheckIdentity =
        parseBoolean(serverProps.getProperty(PROP_SSL_CHECK_IDENTITY), true);
    int connectTimeoutMs =
        parsePositiveInt(
            serverProps.getProperty(PROP_CONNECT_TIMEOUT_MS),
            DEFAULT_TIMEOUT_MS,
            PROP_CONNECT_TIMEOUT_MS,
            errors);
    int socketTimeoutMs =
        parsePositiveInt(
            serverProps.getProperty(PROP_SOCKET_TIMEOUT_MS),
            DEFAULT_TIMEOUT_MS,
            PROP_SOCKET_TIMEOUT_MS,
            errors);

    MailDraft draft = resolveDraft(serverProps, fallbackEnabled);
    int port = parsePort(draft.rawPort(), draft.ssl() ? 465 : DEFAULT_PORT, errors);
    validateSenderAndHeaders(draft, errors);
    String password = resolvePassword(draft.rawPassword(), errors);

    return new PSAuthMailConfig(
        draft.host(),
        port,
        draft.username(),
        password,
        draft.from(),
        draft.fromName(),
        draft.startTls(),
        draft.startTlsRequired(),
        draft.ssl(),
        sslCheckIdentity,
        connectTimeoutMs,
        socketTimeoutMs,
        draft.source(),
        fallbackEnabled,
        errors);
  }

  private MailDraft resolveDraft(Properties serverProps, boolean fallbackEnabled) {
    String explicitHost = StringUtils.trimToEmpty(serverProps.getProperty(PROP_HOST));
    if (StringUtils.isNotBlank(explicitHost)) {
      return draftFromServerProperties(serverProps, explicitHost);
    }
    if (fallbackEnabled) {
      Properties workflowProps = safeProps(workflowPropsSupplier.get());
      String wfHost = StringUtils.trimToEmpty(workflowProps.getProperty("SMTP_HOST"));
      if (StringUtils.isNotBlank(wfHost)) {
        return draftFromWorkflowFallback(serverProps, workflowProps, wfHost);
      }
    }
    return unconfiguredDraft(serverProps);
  }

  private static MailDraft draftFromServerProperties(Properties serverProps, String host) {
    boolean ssl = parseBoolean(serverProps.getProperty(PROP_SSL), false);
    boolean startTls = parseBoolean(serverProps.getProperty(PROP_STARTTLS), !ssl);
    boolean startTlsRequired =
        parseBoolean(serverProps.getProperty(PROP_STARTTLS_REQUIRED), startTls);
    return new MailDraft(
        ConfigSource.SERVER_PROPERTIES,
        host,
        serverProps.getProperty(PROP_PORT),
        StringUtils.trimToEmpty(serverProps.getProperty(PROP_USERNAME)),
        StringUtils.defaultString(serverProps.getProperty(PROP_PASSWORD)),
        StringUtils.trimToEmpty(serverProps.getProperty(PROP_FROM)),
        resolveFromName(serverProps),
        ssl,
        startTls,
        startTlsRequired);
  }

  private static MailDraft draftFromWorkflowFallback(
      Properties serverProps, Properties workflowProps, String wfHost) {
    String wfSslPort = StringUtils.trimToEmpty(workflowProps.getProperty("SMTP_SSLPORT"));
    boolean hasSslPort = StringUtils.isNotBlank(wfSslPort);
    boolean ssl = hasSslPort || parseBoolean(serverProps.getProperty(PROP_SSL), false);
    String rawPort = hasSslPort ? wfSslPort : StringUtils.trimToEmpty(workflowProps.getProperty("SMTP_PORT"));

    String serverFrom = StringUtils.trimToEmpty(serverProps.getProperty(PROP_FROM));
    String from =
        StringUtils.isNotBlank(serverFrom)
            ? serverFrom
            : StringUtils.trimToEmpty(workflowProps.getProperty("SMTP_BOUNCEADDR"));

    String wfTls = StringUtils.trimToEmpty(workflowProps.getProperty("SMTP_TLSENABLED"));
    boolean defaultTls = !ssl && parseBoolean(serverProps.getProperty(PROP_STARTTLS), true);
    boolean startTls = StringUtils.isNotBlank(wfTls) ? parseBoolean(wfTls, defaultTls) : defaultTls;
    boolean startTlsRequired =
        startTls && parseBoolean(serverProps.getProperty(PROP_STARTTLS_REQUIRED), true);

    return new MailDraft(
        ConfigSource.WORKFLOW_FALLBACK,
        wfHost,
        rawPort,
        StringUtils.trimToEmpty(workflowProps.getProperty("SMTP_USERNAME")),
        StringUtils.defaultString(workflowProps.getProperty("SMTP_PASSWORD")),
        from,
        resolveFromName(serverProps),
        ssl,
        startTls,
        startTlsRequired);
  }

  private static MailDraft unconfiguredDraft(Properties serverProps) {
    return new MailDraft(
        ConfigSource.UNCONFIGURED,
        "",
        String.valueOf(DEFAULT_PORT),
        "",
        "",
        StringUtils.trimToEmpty(serverProps.getProperty(PROP_FROM)),
        DEFAULT_FROM_NAME,
        false,
        true,
        true);
  }

  private static String resolveFromName(Properties serverProps) {
    return StringUtils.defaultIfBlank(
        StringUtils.trimToNull(serverProps.getProperty(PROP_FROM_NAME)), DEFAULT_FROM_NAME);
  }

  private static void validateSenderAndHeaders(MailDraft draft, List<String> errors) {
    if (draft.source() == ConfigSource.UNCONFIGURED) {
      errors.add("No SMTP host configured in auth.mail.host or workflow SMTP fallback.");
      return;
    }
    if (StringUtils.isBlank(draft.from())) {
      errors.add("Sender address (auth.mail.from) is required for transactional auth email.");
    } else if (!isValidSingleEmail(draft.from())) {
      errors.add("Sender address (auth.mail.from) is not a valid email address.");
    }
    if (containsCrLf(draft.host())
        || containsCrLf(draft.username())
        || containsCrLf(draft.fromName())) {
      errors.add("Mail header/host configuration contains disallowed control characters.");
    }
  }

  private String resolvePassword(String rawPassword, List<String> errors) {
    if (StringUtils.isBlank(rawPassword)) {
      return "";
    }
    try {
      return decryptMaybeEncrypted(rawPassword, secretDecryptor);
    } catch (RuntimeException ex) {
      errors.add("Configured SMTP password could not be decrypted.");
      log.warn("Auth mail SMTP password decryption failed: {}", ex.getClass().getSimpleName());
      return "";
    }
  }

  private record MailDraft(
      ConfigSource source,
      String host,
      String rawPort,
      String username,
      String rawPassword,
      String from,
      String fromName,
      boolean ssl,
      boolean startTls,
      boolean startTlsRequired) {}

  @Override
  public void sendTextEmail(String toAddress, String subject, String plainTextBody)
      throws PSAuthMailException {
    if (!isValidSingleEmail(toAddress)) {
      throw new IllegalArgumentException("Recipient email address is invalid.");
    }
    if (StringUtils.isBlank(subject) || containsCrLf(subject)) {
      throw new IllegalArgumentException("Email subject is invalid.");
    }
    if (plainTextBody == null) {
      throw new IllegalArgumentException("Email body must not be null.");
    }

    PSAuthMailConfig config = resolveEffectiveConfig();
    if (!config.isConfigured()) {
      String reason =
          config.getValidationErrors().isEmpty()
              ? "Authentication mail is not configured."
              : String.join("; ", config.getValidationErrors());
      throw new PSAuthMailException(reason);
    }

    String cleanTo = toAddress.trim();
    String cleanSubject = subject.trim();
    SimpleEmail email = emailSupplier.get();
    try {
      email.setCharset(StandardCharsets.UTF_8.name());
      email.setHostName(config.getHost());
      email.setSmtpPort(config.getPort());
      if (config.isSsl()) {
        email.setSSLOnConnect(true);
        email.setSslSmtpPort(String.valueOf(config.getPort()));
      } else {
        email.setSSLOnConnect(false);
      }
      email.setStartTLSEnabled(config.isStartTls());
      email.setStartTLSRequired(config.isStartTlsRequired());
      email.setSSLCheckServerIdentity(config.isSslCheckServerIdentity());
      email.setSocketConnectionTimeout(Duration.ofMillis(config.getConnectTimeoutMs()));
      email.setSocketTimeout(Duration.ofMillis(config.getSocketTimeoutMs()));
      if (config.hasCredentials()) {
        email.setAuthentication(config.getUsername(), config.getPassword());
      }
      if (StringUtils.isNotBlank(config.getFromName())) {
        email.setFrom(config.getFromAddress(), config.getFromName());
      } else {
        email.setFrom(config.getFromAddress());
      }
      email.addTo(cleanTo);
      email.setSubject(cleanSubject);
      email.setMsg(plainTextBody);
      email.send();
      log.info(
          "Sent synchronous auth email via host={} port={} source={}",
          config.getHost(),
          config.getPort(),
          config.getSource());
    } catch (EmailException | RuntimeException ex) {
      log.warn(
          "Failed to send synchronous auth email via host={} port={} source={}: {}",
          config.getHost(),
          config.getPort(),
          config.getSource(),
          ex.getMessage());
      throw new PSAuthMailException(
          "Failed to send authentication email via "
              + config.getHost()
              + ":"
              + config.getPort(),
          ex);
    }
  }

  @Override
  public void sendTestEmail(String toAddress) throws PSAuthMailException {
    PSAuthMailConfig cfg = resolveEffectiveConfig();
    String body =
        "This is a synchronous test message from Percussion CMS Authentication Mail.\n\n"
            + "Mail configuration source: "
            + cfg.getSource()
            + "\nSMTP host: "
            + cfg.getHost()
            + ":"
            + cfg.getPort()
            + "\n";
    sendTextEmail(toAddress, "Percussion CMS Authentication Mail Test", body);
  }

  /** Validates single email syntax strictly and rejects header injection characters. */
  public static boolean isValidSingleEmail(String email) {
    if (StringUtils.isBlank(email) || containsCrLf(email)) {
      return false;
    }
    String trimmed = email.trim();
    if (trimmed.length() > 320 || trimmed.indexOf(' ') >= 0 || trimmed.indexOf('\t') >= 0) {
      return false;
    }
    int atIdx = trimmed.indexOf('@');
    if (atIdx <= 0 || atIdx == trimmed.length() - 1 || trimmed.indexOf('@', atIdx + 1) >= 0) {
      return false;
    }
    String domain = trimmed.substring(atIdx + 1);
    if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
      return false;
    }
    try {
      InternetAddress[] parsed = InternetAddress.parse(trimmed, true);
      return parsed.length == 1 && trimmed.equals(parsed[0].getAddress());
    } catch (AddressException ex) {
      return false;
    }
  }

  private static String decryptMaybeEncrypted(String raw, UnaryOperator<String> decryptor) {
    String trimmed = raw.trim();
    if (trimmed.startsWith(ENC_PREFIX) && trimmed.endsWith(ENC_SUFFIX)) {
      String inner = trimmed.substring(ENC_PREFIX.length(), trimmed.length() - ENC_SUFFIX.length());
      return decryptor.apply(inner);
    }
    return raw;
  }

  private static String decryptWithServerKey(String encryptedInner) {
    Path secureDir = PSServer.getRxDir().toPath().resolve("rxconfig").resolve("secure");
    try {
      return PSEncryptor.decryptString(secureDir.toString(), encryptedInner);
    } catch (PSEncryptionException ex) {
      throw new IllegalStateException("Failed to decrypt SMTP secret", ex);
    }
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties loadWorkflowPropertiesFromDisk() {
    Properties props = new Properties();
    if (PSServer.getRxDir() == null) {
      return props;
    }
    Path wfFile =
        PSServer.getRxDir()
            .toPath()
            .resolve("rxconfig")
            .resolve("Workflow")
            .resolve("rxworkflow.properties");
    if (!Files.isRegularFile(wfFile)) {
      return props;
    }
    try (InputStream in = Files.newInputStream(wfFile)) {
      props.load(in);
    } catch (IOException ex) {
      log.warn("Unable to read workflow SMTP properties for fallback: {}", ex.getMessage());
    }
    return props;
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }

  private static boolean parseBoolean(String value, boolean defaultVal) {
    if (StringUtils.isBlank(value)) {
      return defaultVal;
    }
    return Boolean.parseBoolean(value.trim());
  }

  private static int parsePort(String raw, int defaultPort, List<String> errors) {
    if (StringUtils.isBlank(raw)) {
      return defaultPort;
    }
    try {
      int port = Integer.parseInt(raw.trim());
      if (port < 1 || port > 65535) {
        errors.add("SMTP port must be between 1 and 65535.");
        return defaultPort;
      }
      return port;
    } catch (NumberFormatException ex) {
      errors.add("SMTP port is not a valid integer.");
      return defaultPort;
    }
  }

  private static int parsePositiveInt(
      String raw, int defaultVal, String propName, List<String> errors) {
    if (StringUtils.isBlank(raw)) {
      return defaultVal;
    }
    try {
      int val = Integer.parseInt(raw.trim());
      if (val <= 0) {
        errors.add(propName + " must be a positive integer.");
        return defaultVal;
      }
      return val;
    } catch (NumberFormatException ex) {
      errors.add(propName + " must be a valid integer.");
      return defaultVal;
    }
  }

  private static boolean containsCrLf(String val) {
    return val != null && (val.indexOf('\r') >= 0 || val.indexOf('\n') >= 0);
  }
}
