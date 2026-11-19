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

import com.percussion.server.PSServer;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.mail.IPSAuthMailService;
import com.percussion.services.auth.mail.PSAuthMailConfig;
import com.percussion.services.auth.mail.PSAuthMailConfig.ConfigSource;
import com.percussion.services.auth.readiness.IPSAuthReadinessService;
import com.percussion.services.auth.readiness.PSAuthCheckStatus;
import com.percussion.services.auth.readiness.PSAuthInfraCheck;
import com.percussion.services.auth.readiness.PSAuthInfraReport;
import com.percussion.system.utils.PSBaseBean;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** Evaluates infrastructure prerequisites for authentication, MFA, WebAuthn, and recovery. */
@PSBaseBean("sys_authReadinessService")
public class PSAuthReadinessService implements IPSAuthReadinessService {

  public static final String PROP_MFA_MODE = "auth.mfa.mode";
  public static final String PROP_PUBLIC_BASE_URL = "auth.publicBaseUrl";
  public static final String PROP_WEBAUTHN_RP_ID = "auth.webauthn.rpId";
  public static final String PROP_WEBAUTHN_ALLOWED_ORIGINS = "auth.webauthn.allowedOrigins";
  public static final String PROP_PASSWORD_MIN_LENGTH = "auth.password.minLength";

  public static final int DEFAULT_PASSWORD_MIN_LENGTH = 12;
  public static final int MIN_ALLOWED_PASSWORD_LENGTH = 8;
  public static final int MAX_ALLOWED_PASSWORD_LENGTH = 150;

  private static final Set<String> VALID_MFA_MODES =
      Set.of("off", "disabled", "optional", "cohort", "required");

  private final IPSAuthMailService authMailService;
  private final IPSAuthSecurityDao authSecurityDao;
  private final Supplier<Properties> serverPropsSupplier;
  private final Supplier<Path> rxDirSupplier;

  @Autowired
  public PSAuthReadinessService(
      IPSAuthMailService authMailService, IPSAuthSecurityDao authSecurityDao) {
    this(
        authMailService,
        authSecurityDao,
        PSAuthReadinessService::defaultServerProperties,
        PSAuthReadinessService::defaultRxDir);
  }

  /** Package-visible constructor for isolated unit tests. */
  PSAuthReadinessService(
      IPSAuthMailService authMailService,
      IPSAuthSecurityDao authSecurityDao,
      Supplier<Properties> serverPropsSupplier,
      Supplier<Path> rxDirSupplier) {
    this.authMailService = Objects.requireNonNull(authMailService, "authMailService");
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.rxDirSupplier = Objects.requireNonNull(rxDirSupplier, "rxDirSupplier");
  }

  @Override
  public PSAuthInfraReport evaluateInfrastructure() {
    Properties props = safeProps(serverPropsSupplier.get());
    PSAuthMailConfig mailConfig = authMailService.resolveEffectiveConfig();

    String rawMfaMode =
        StringUtils.defaultIfBlank(props.getProperty(PROP_MFA_MODE), "optional")
            .trim()
            .toLowerCase(Locale.ROOT);
    boolean mfaRequired = "required".equals(rawMfaMode);

    List<PSAuthInfraCheck> checks = new ArrayList<>();
    checks.add(checkAuthMail(mailConfig));
    checks.add(checkPublicBaseUrl(props.getProperty(PROP_PUBLIC_BASE_URL), mfaRequired));
    checks.add(
        checkWebAuthn(
            props.getProperty(PROP_WEBAUTHN_RP_ID),
            props.getProperty(PROP_WEBAUTHN_ALLOWED_ORIGINS),
            mfaRequired));
    checks.add(checkMfaMode(rawMfaMode));

    PasswordPolicyEval pwEval = checkPasswordPolicy(props.getProperty(PROP_PASSWORD_MIN_LENGTH));
    checks.add(pwEval.check());

    KeyEval keyEval = checkEncryptionKey(rxDirSupplier.get());
    checks.add(keyEval.check());

    Map<String, Boolean> schemaTables = safeCheckSchema();
    checks.add(checkSchema(schemaTables));

    return new PSAuthInfraReport(
        Instant.now(),
        rawMfaMode,
        pwEval.effectiveMinLength(),
        mailConfig.isConfigured(),
        mailConfig.getSource(),
        keyEval.fingerprint(),
        checks,
        schemaTables);
  }

  private PSAuthInfraCheck checkAuthMail(PSAuthMailConfig cfg) {
    if (cfg.isConfigured()) {
      String summary =
          cfg.getSource() == ConfigSource.WORKFLOW_FALLBACK
              ? "Configured via workflow SMTP fallback ("
                  + cfg.getHost()
                  + ":"
                  + cfg.getPort()
                  + "; workflow test mode ignored)."
              : "Configured via server.properties auth.mail.* ("
                  + cfg.getHost()
                  + ":"
                  + cfg.getPort()
                  + ").";
      return new PSAuthInfraCheck(
          "AUTH_MAIL",
          "Transactional Auth Mail",
          PSAuthCheckStatus.PASS,
          summary,
          cfg.getSource() == ConfigSource.WORKFLOW_FALLBACK
              ? "Configure dedicated auth.mail.* keys in server.properties when isolating auth SMTP."
              : "");
    }
    String reason =
        cfg.getValidationErrors().isEmpty()
            ? "Authentication mail is not configured."
            : String.join(" ", cfg.getValidationErrors());
    return new PSAuthInfraCheck(
        "AUTH_MAIL",
        "Transactional Auth Mail",
        PSAuthCheckStatus.FAIL,
        reason,
        "Set auth.mail.host, auth.mail.port, and auth.mail.from in server.properties (or enable workflow SMTP fallback).");
  }

  private PSAuthInfraCheck checkPublicBaseUrl(String rawUrl, boolean mfaRequired) {
    String trimmed = StringUtils.trimToEmpty(rawUrl);
    if (trimmed.isEmpty()) {
      return new PSAuthInfraCheck(
          "PUBLIC_BASE_URL",
          "Public Base URL",
          mfaRequired ? PSAuthCheckStatus.FAIL : PSAuthCheckStatus.WARN,
          "auth.publicBaseUrl is not set.",
          "Configure auth.publicBaseUrl with the canonical https:// URL (for example https://cms.example.com/Rhythmyx).");
    }
    URI uri;
    try {
      uri = new URI(trimmed);
    } catch (URISyntaxException ex) {
      return new PSAuthInfraCheck(
          "PUBLIC_BASE_URL",
          "Public Base URL",
          PSAuthCheckStatus.FAIL,
          "auth.publicBaseUrl is not a valid URI.",
          "Fix auth.publicBaseUrl to a valid https:// base URL.");
    }
    String scheme = StringUtils.lowerCase(uri.getScheme(), Locale.ROOT);
    String host = StringUtils.lowerCase(uri.getHost(), Locale.ROOT);
    if (!uri.isAbsolute() || StringUtils.isBlank(host)) {
      return new PSAuthInfraCheck(
          "PUBLIC_BASE_URL",
          "Public Base URL",
          PSAuthCheckStatus.FAIL,
          "auth.publicBaseUrl must include scheme and host.",
          "Set auth.publicBaseUrl to a full URL such as https://cms.example.com.");
    }
    if ("https".equals(scheme)) {
      return new PSAuthInfraCheck(
          "PUBLIC_BASE_URL",
          "Public Base URL",
          PSAuthCheckStatus.PASS,
          "Configured for HTTPS (" + uri + ").",
          "");
    }
    if ("http".equals(scheme) && isLoopbackHost(host)) {
      return new PSAuthInfraCheck(
          "PUBLIC_BASE_URL",
          "Public Base URL",
          mfaRequired ? PSAuthCheckStatus.FAIL : PSAuthCheckStatus.WARN,
          "Configured for local HTTP (" + uri + "); suitable for development only.",
          "Use an https:// URL in production deployments.");
    }
    return new PSAuthInfraCheck(
        "PUBLIC_BASE_URL",
        "Public Base URL",
        PSAuthCheckStatus.FAIL,
        "auth.publicBaseUrl must use https:// for non-localhost hosts.",
        "Update auth.publicBaseUrl to https:// behind TLS or proxy termination.");
  }

  private PSAuthInfraCheck checkWebAuthn(
      String rawRpId, String rawOrigins, boolean mfaRequired) {
    String rpId = StringUtils.trimToEmpty(rawRpId).toLowerCase(Locale.ROOT);
    String originsRaw = StringUtils.trimToEmpty(rawOrigins);
    PSAuthInfraCheck inputError = validateWebAuthnInputs(rpId, originsRaw, mfaRequired);
    if (inputError != null) {
      return inputError;
    }

    int originCount = 0;
    boolean hasHttpLocalhost = false;
    for (String part : originsRaw.split(",")) {
      String candidate = part.trim();
      if (candidate.isEmpty()) {
        continue;
      }
      originCount++;
      OriginValidation result = validateSingleOrigin(candidate, rpId);
      if (result.failure() != null) {
        return result.failure();
      }
      hasHttpLocalhost |= result.httpLoopback();
    }

    if (originCount == 0) {
      return webAuthnCheck(
          PSAuthCheckStatus.FAIL,
          "auth.webauthn.allowedOrigins contains no valid entries.",
          "Add at least one HTTPS origin matching auth.webauthn.rpId.");
    }
    if (hasHttpLocalhost) {
      return webAuthnCheck(
          mfaRequired ? PSAuthCheckStatus.FAIL : PSAuthCheckStatus.WARN,
          "WebAuthn configured with localhost HTTP origin (development only).",
          "Replace HTTP localhost origins with HTTPS before production enforcement.");
    }
    return webAuthnCheck(
        PSAuthCheckStatus.PASS,
        "RP ID '" + rpId + "' valid with " + originCount + " HTTPS origin(s).",
        "");
  }

  private static PSAuthInfraCheck validateWebAuthnInputs(
      String rpId, String originsRaw, boolean mfaRequired) {
    if (rpId.isEmpty() && originsRaw.isEmpty()) {
      return webAuthnCheck(
          mfaRequired ? PSAuthCheckStatus.FAIL : PSAuthCheckStatus.WARN,
          "auth.webauthn.rpId and auth.webauthn.allowedOrigins are blank.",
          "Set auth.webauthn.rpId (domain host) and comma-separated HTTPS origins in auth.webauthn.allowedOrigins.");
    }
    if (rpId.isEmpty() || originsRaw.isEmpty()) {
      return webAuthnCheck(
          PSAuthCheckStatus.FAIL,
          "Both auth.webauthn.rpId and auth.webauthn.allowedOrigins must be specified together.",
          "Set both auth.webauthn.rpId and auth.webauthn.allowedOrigins in server.properties.");
    }
    if (rpId.contains("://") || rpId.contains("/") || rpId.contains(":")) {
      return webAuthnCheck(
          PSAuthCheckStatus.FAIL,
          "auth.webauthn.rpId must be a hostname without scheme, port, or path.",
          "Use only the domain name for auth.webauthn.rpId (for example cms.example.com).");
    }
    return null;
  }

  private static OriginValidation validateSingleOrigin(String candidate, String rpId) {
    URI origin = parseOriginUri(candidate);
    if (origin == null) {
      return OriginValidation.fail(
          webAuthnCheck(
              PSAuthCheckStatus.FAIL,
              "Invalid origin URI in auth.webauthn.allowedOrigins: " + candidate,
              "Provide valid origin URLs such as https://cms.example.com."));
    }
    String host = StringUtils.lowerCase(origin.getHost(), Locale.ROOT);
    if (hasMalformedOriginParts(origin, host)) {
      return OriginValidation.fail(
          webAuthnCheck(
              PSAuthCheckStatus.FAIL,
              "Origin must be scheme://host[:port] without path or query: " + candidate,
              "Strip trailing slashes or paths from auth.webauthn.allowedOrigins."));
    }
    String scheme = StringUtils.lowerCase(origin.getScheme(), Locale.ROOT);
    boolean isHttpLoopback = "http".equals(scheme) && isLoopbackHost(host);
    if (!"https".equals(scheme) && !isHttpLoopback) {
      return OriginValidation.fail(
          webAuthnCheck(
              PSAuthCheckStatus.FAIL,
              "WebAuthn requires HTTPS origins for non-localhost hosts: " + candidate,
              "Use https:// origins in auth.webauthn.allowedOrigins."));
    }
    if (!hostMatchesRpId(host, rpId)) {
      return OriginValidation.fail(
          webAuthnCheck(
              PSAuthCheckStatus.FAIL,
              "Origin host '" + host + "' does not match auth.webauthn.rpId '" + rpId + "'.",
              "Align auth.webauthn.rpId with the registrable domain of auth.webauthn.allowedOrigins."));
    }
    return new OriginValidation(null, isHttpLoopback);
  }

  private static URI parseOriginUri(String candidate) {
    try {
      return new URI(candidate);
    } catch (URISyntaxException ex) {
      return null;
    }
  }

  private static boolean hasMalformedOriginParts(URI origin, String host) {
    return StringUtils.isBlank(host)
        || StringUtils.isNotEmpty(origin.getPath())
        || origin.getRawQuery() != null
        || origin.getRawFragment() != null;
  }

  private static PSAuthInfraCheck webAuthnCheck(
      PSAuthCheckStatus status, String summary, String remediation) {
    return new PSAuthInfraCheck(
        "WEBAUTHN_CONFIG", "WebAuthn RP ID & Origins", status, summary, remediation);
  }

  private record OriginValidation(PSAuthInfraCheck failure, boolean httpLoopback) {
    static OriginValidation fail(PSAuthInfraCheck failure) {
      return new OriginValidation(failure, false);
    }
  }

  private PSAuthInfraCheck checkMfaMode(String mfaMode) {
    if (!VALID_MFA_MODES.contains(mfaMode)) {
      return new PSAuthInfraCheck(
          "MFA_MODE",
          "MFA Rollout Mode",
          PSAuthCheckStatus.FAIL,
          "Invalid auth.mfa.mode value '" + mfaMode + "'.",
          "Set auth.mfa.mode to off, optional, cohort, or required.");
    }
    return new PSAuthInfraCheck(
        "MFA_MODE",
        "MFA Rollout Mode",
        PSAuthCheckStatus.PASS,
        "auth.mfa.mode is set to '" + mfaMode + "'.",
        "");
  }

  private PasswordPolicyEval checkPasswordPolicy(String rawMinLength) {
    int effective = DEFAULT_PASSWORD_MIN_LENGTH;
    String trimmed = StringUtils.trimToEmpty(rawMinLength);
    if (!trimmed.isEmpty()) {
      try {
        int parsed = Integer.parseInt(trimmed);
        if (parsed < MIN_ALLOWED_PASSWORD_LENGTH || parsed > MAX_ALLOWED_PASSWORD_LENGTH) {
          return new PasswordPolicyEval(
              DEFAULT_PASSWORD_MIN_LENGTH,
              new PSAuthInfraCheck(
                  "PASSWORD_POLICY",
                  "Server Password Policy",
                  PSAuthCheckStatus.FAIL,
                  "auth.password.minLength ("
                      + parsed
                      + ") must be between "
                      + MIN_ALLOWED_PASSWORD_LENGTH
                      + " and "
                      + MAX_ALLOWED_PASSWORD_LENGTH
                      + ".",
                  "Set auth.password.minLength between 8 and 150 (recommended: 12)."));
        }
        effective = parsed;
      } catch (NumberFormatException ex) {
        return new PasswordPolicyEval(
            DEFAULT_PASSWORD_MIN_LENGTH,
            new PSAuthInfraCheck(
                "PASSWORD_POLICY",
                "Server Password Policy",
                PSAuthCheckStatus.FAIL,
                "auth.password.minLength is not a valid integer.",
                "Set auth.password.minLength to an integer between 8 and 150 (recommended: 12)."));
      }
    }
    return new PasswordPolicyEval(
        effective,
        new PSAuthInfraCheck(
            "PASSWORD_POLICY",
            "Server Password Policy",
            PSAuthCheckStatus.PASS,
            "Minimum password length is " + effective + " characters (max 150).",
            ""));
  }

  private KeyEval checkEncryptionKey(Path rxDir) {
    if (rxDir == null) {
      return new KeyEval(
          "",
          new PSAuthInfraCheck(
              "ENCRYPTION_KEY",
              "Cluster Encryption Key (.key)",
              PSAuthCheckStatus.WARN,
              "Server root directory is unavailable to inspect rxconfig/secure/.key.",
              "Verify server root configuration and rxconfig/secure/.key consistency across cluster nodes."));
    }
    Path keyFile = rxDir.resolve("rxconfig").resolve("secure").resolve(".key");
    if (!Files.isRegularFile(keyFile)) {
      return new KeyEval(
          "",
          new PSAuthInfraCheck(
              "ENCRYPTION_KEY",
              "Cluster Encryption Key (.key)",
              PSAuthCheckStatus.WARN,
              "Instance key file rxconfig/secure/.key does not exist yet.",
              "Initialize or synchronize rxconfig/secure/.key before enabling TOTP across cluster nodes."));
    }
    try {
      byte[] keyBytes = Files.readAllBytes(keyFile);
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(keyBytes);
      String fingerprint = "sha256:" + HexFormat.of().formatHex(digest, 0, 6);
      return new KeyEval(
          fingerprint,
          new PSAuthInfraCheck(
              "ENCRYPTION_KEY",
              "Cluster Encryption Key (.key)",
              PSAuthCheckStatus.PASS,
              "Instance key present (fingerprint " + fingerprint + ").",
              "Ensure all cluster nodes report the same SHA-256 key fingerprint prefix."));
    } catch (IOException | NoSuchAlgorithmException ex) {
      return new KeyEval(
          "",
          new PSAuthInfraCheck(
              "ENCRYPTION_KEY",
              "Cluster Encryption Key (.key)",
              PSAuthCheckStatus.FAIL,
              "Unable to read rxconfig/secure/.key: " + ex.getMessage(),
              "Verify file permissions on rxconfig/secure/.key."));
    }
  }

  private Map<String, Boolean> safeCheckSchema() {
    try {
      Map<String, Boolean> tables = authSecurityDao.checkSchemaTablesPresent();
      return tables != null ? tables : Map.of();
    } catch (RuntimeException ex) {
      return Map.of();
    }
  }

  private PSAuthInfraCheck checkSchema(Map<String, Boolean> schemaTables) {
    if (schemaTables.isEmpty()) {
      return new PSAuthInfraCheck(
          "AUTH_SCHEMA",
          "Auth & MFA Database Schema",
          PSAuthCheckStatus.FAIL,
          "Unable to query authentication/MFA schema tables.",
          "Run database schema upgrade (cmsTableDef.xml) before enabling MFA.");
    }
    List<String> missing = new ArrayList<>();
    for (Map.Entry<String, Boolean> e : schemaTables.entrySet()) {
      if (!Boolean.TRUE.equals(e.getValue())) {
        missing.add(e.getKey());
      }
    }
    if (!missing.isEmpty()) {
      return new PSAuthInfraCheck(
          "AUTH_SCHEMA",
          "Auth & MFA Database Schema",
          PSAuthCheckStatus.FAIL,
          "Missing or inaccessible auth tables: " + String.join(", ", missing),
          "Apply database schema upgrade from cmsTableDef.xml.");
    }
    return new PSAuthInfraCheck(
        "AUTH_SCHEMA",
        "Auth & MFA Database Schema",
        PSAuthCheckStatus.PASS,
        "All " + schemaTables.size() + " authentication & MFA tables verified.",
        "");
  }

  private static boolean isLoopbackHost(String host) {
    return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
  }

  private static boolean hostMatchesRpId(String host, String rpId) {
    return host.equals(rpId) || host.endsWith("." + rpId);
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Path defaultRxDir() {
    return PSServer.getRxDir() != null ? PSServer.getRxDir().toPath() : null;
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }

  private record PasswordPolicyEval(int effectiveMinLength, PSAuthInfraCheck check) {}

  private record KeyEval(String fingerprint, PSAuthInfraCheck check) {}
}
