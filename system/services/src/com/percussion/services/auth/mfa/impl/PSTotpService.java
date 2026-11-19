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

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.percussion.server.PSServer;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSMfaTotp;
import com.percussion.services.auth.mfa.IPSTotpService;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import com.percussion.system.utils.PSBaseBean;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** RFC 6238 authenticator app service using {@code java-otp}, AES-GCM secrets, and step anti-replay. */
@PSBaseBean("sys_totpService")
public class PSTotpService implements IPSTotpService {

  public static final String PROP_ISSUER = "auth.mfa.issuer";
  public static final String PROP_ALLOWED_DRIFT = "auth.mfa.totpAllowedDriftSteps";

  private static final int SECRET_BYTES = 20;
  private static final int CODE_DIGITS = 6;
  private static final int PERIOD_SECONDS = 30;
  private static final String DEFAULT_ISSUER = "Percussion CMS";
  private static final int DEFAULT_DRIFT_STEPS = 1;
  private static final int MAX_DRIFT_STEPS = 2;

  private final IPSAuthSecurityDao authSecurityDao;
  private final PSTotpSecretCrypto secretCrypto;
  private final Supplier<Properties> serverPropsSupplier;
  private final Clock clock;
  private final TimeBasedOneTimePasswordGenerator totpGenerator;
  private final Base32 base32 = new Base32();
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public PSTotpService(IPSAuthSecurityDao authSecurityDao) {
    this(
        authSecurityDao,
        new PSTotpSecretCrypto(),
        PSTotpService::defaultServerProperties,
        Clock.systemUTC());
  }

  PSTotpService(
      IPSAuthSecurityDao authSecurityDao,
      PSTotpSecretCrypto secretCrypto,
      Supplier<Properties> serverPropsSupplier,
      Clock clock) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.secretCrypto = Objects.requireNonNull(secretCrypto, "secretCrypto");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.totpGenerator = createTotpGenerator();
  }

  @Override
  public PSTotpEnrollmentStart startEnrollment(String userName, String label) {
    String cleanUser = requireUserName(userName);
    String cleanLabel = StringUtils.defaultIfBlank(StringUtils.trimToNull(label), "Authenticator App");
    if (cleanLabel.length() > 120) {
      cleanLabel = cleanLabel.substring(0, 120);
    }

    byte[] secretBytes = new byte[SECRET_BYTES];
    secureRandom.nextBytes(secretBytes);
    try {
      String manualSecret = base32.encodeAsString(secretBytes).replace("=", "");
      String encrypted = secretCrypto.encrypt(cleanUser, secretBytes);

      authSecurityDao.revokeTotpByUser(cleanUser, PSMfaTotp.STATUS_PENDING);

      PSMfaTotp entity = new PSMfaTotp();
      entity.setTotpId(UUID.randomUUID().toString());
      entity.setUserName(cleanUser);
      entity.setLabel(cleanLabel);
      entity.setSecretEnc(encrypted);
      entity.setAlgorithm("SHA1");
      entity.setDigits(CODE_DIGITS);
      entity.setPeriodSeconds(PERIOD_SECONDS);
      entity.setStatus(PSMfaTotp.STATUS_PENDING);
      entity.setCreatedAt(Date.from(clock.instant()));
      authSecurityDao.saveTotp(entity);

      String issuer = resolveIssuer();
      String otpauthUri = buildOtpAuthUri(issuer, cleanUser, manualSecret);
      return new PSTotpEnrollmentStart(
          entity.getTotpId(),
          otpauthUri,
          manualSecret,
          issuer,
          cleanUser,
          CODE_DIGITS,
          PERIOD_SECONDS);
    } finally {
      Arrays.fill(secretBytes, (byte) 0);
    }
  }

  @Override
  public boolean confirmEnrollment(String userName, String totpId, String rawCode) {
    String cleanUser = requireUserName(userName);
    String normalizedCode = normalizeSixDigitCode(rawCode);
    if (StringUtils.isBlank(totpId) || normalizedCode == null) {
      return false;
    }

    Optional<PSMfaTotp> found = authSecurityDao.findTotpById(totpId.trim());
    if (found.isEmpty()) {
      return false;
    }
    PSMfaTotp entity = found.get();
    if (!cleanUser.equalsIgnoreCase(entity.getUserName())
        || !PSMfaTotp.STATUS_PENDING.equals(entity.getStatus())) {
      return false;
    }

    Instant now = clock.instant();
    long matchedStep = findMatchingTimeStep(cleanUser, entity.getSecretEnc(), normalizedCode, now);
    if (matchedStep < 0) {
      return false;
    }

    authSecurityDao.revokeTotpByUser(cleanUser, PSMfaTotp.STATUS_ACTIVE);
    Date nowDate = Date.from(now);
    entity.setStatus(PSMfaTotp.STATUS_ACTIVE);
    entity.setVerifiedAt(nowDate);
    entity.setLastUsedStep(matchedStep);
    entity.setLastUsedAt(nowDate);
    authSecurityDao.saveTotp(entity);

    markAccountEnrolled(cleanUser);
    return true;
  }

  @Override
  public boolean verifyActiveCode(String userName, String rawCode) {
    if (StringUtils.isBlank(userName)) {
      return false;
    }
    String normalizedCode = normalizeSixDigitCode(rawCode);
    if (normalizedCode == null) {
      return false;
    }
    String cleanUser = userName.trim();
    List<PSMfaTotp> activeFactors = authSecurityDao.findActiveTotpByUser(cleanUser);
    Instant now = clock.instant();
    for (PSMfaTotp factor : activeFactors) {
      long matchedStep =
          findMatchingTimeStep(cleanUser, factor.getSecretEnc(), normalizedCode, now);
      if (matchedStep >= 0
          && (factor.getLastUsedStep() == null || matchedStep > factor.getLastUsedStep())) {
        int updated =
            authSecurityDao.advanceTotpLastUsedStepAtomically(
                factor.getTotpId(), matchedStep, Date.from(now));
        if (updated == 1) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int disableTotp(String userName) {
    if (StringUtils.isBlank(userName)) {
      return 0;
    }
    String cleanUser = userName.trim();
    int revoked = authSecurityDao.revokeTotpByUser(cleanUser, PSMfaTotp.STATUS_ACTIVE);
    revoked += authSecurityDao.revokeTotpByUser(cleanUser, PSMfaTotp.STATUS_PENDING);
    return revoked;
  }

  @Override
  public boolean hasActiveTotp(String userName) {
    if (StringUtils.isBlank(userName)) {
      return false;
    }
    return !authSecurityDao.findActiveTotpByUser(userName.trim()).isEmpty();
  }

  private long findMatchingTimeStep(
      String userName, String encryptedSecret, String candidateCode, Instant now) {
    byte[] secretBytes = secretCrypto.decrypt(userName, encryptedSecret);
    try {
      SecretKey key =
          new SecretKeySpec(secretBytes, TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
      int drift = resolveAllowedDriftSteps();
      long currentStep = now.getEpochSecond() / PERIOD_SECONDS;
      byte[] candidateBytes = candidateCode.getBytes(StandardCharsets.US_ASCII);
      for (int offset = -drift; offset <= drift; offset++) {
        long step = currentStep + offset;
        if (step < 0) {
          continue;
        }
        Instant stepInstant = Instant.ofEpochSecond(step * PERIOD_SECONDS);
        String expected = totpGenerator.generateOneTimePasswordString(key, stepInstant);
        if (MessageDigest.isEqual(candidateBytes, expected.getBytes(StandardCharsets.US_ASCII))) {
          return step;
        }
      }
      return -1L;
    } catch (InvalidKeyException ex) {
      throw new IllegalStateException("Invalid TOTP key material", ex);
    } finally {
      Arrays.fill(secretBytes, (byte) 0);
    }
  }

  private void markAccountEnrolled(String userName) {
    PSAccountSecurity sec =
        authSecurityDao
            .findAccountSecurity(userName)
            .orElseGet(
                () -> {
                  PSAccountSecurity created = new PSAccountSecurity();
                  created.setUserName(userName);
                  return created;
                });
    sec.setMfaEnrollmentState(PSAccountSecurity.MFA_STATE_ENROLLED);
    authSecurityDao.saveAccountSecurity(sec);
  }

  private String resolveIssuer() {
    Properties props = safeProps(serverPropsSupplier.get());
    return StringUtils.defaultIfBlank(
        StringUtils.trimToNull(props.getProperty(PROP_ISSUER)), DEFAULT_ISSUER);
  }

  private int resolveAllowedDriftSteps() {
    Properties props = safeProps(serverPropsSupplier.get());
    String raw = StringUtils.trimToNull(props.getProperty(PROP_ALLOWED_DRIFT));
    if (raw == null) {
      return DEFAULT_DRIFT_STEPS;
    }
    try {
      int parsed = Integer.parseInt(raw);
      return Math.max(0, Math.min(MAX_DRIFT_STEPS, parsed));
    } catch (NumberFormatException ex) {
      return DEFAULT_DRIFT_STEPS;
    }
  }

  private static String buildOtpAuthUri(String issuer, String userName, String manualSecret) {
    String label = urlEncode(issuer) + ":" + urlEncode(userName);
    return "otpauth://totp/"
        + label
        + "?secret="
        + manualSecret
        + "&issuer="
        + urlEncode(issuer)
        + "&algorithm=SHA1&digits="
        + CODE_DIGITS
        + "&period="
        + PERIOD_SECONDS;
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  static String normalizeSixDigitCode(String rawCode) {
    if (rawCode == null) {
      return null;
    }
    StringBuilder digits = new StringBuilder(CODE_DIGITS);
    for (int i = 0; i < rawCode.length(); i++) {
      char c = rawCode.charAt(i);
      if (Character.isWhitespace(c) || c == '-') {
        continue;
      }
      if (c < '0' || c > '9') {
        return null;
      }
      digits.append(c);
    }
    return digits.length() == CODE_DIGITS ? digits.toString() : null;
  }

  private static String requireUserName(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    return userName.trim();
  }

  private static TimeBasedOneTimePasswordGenerator createTotpGenerator() {
    try {
      return new TimeBasedOneTimePasswordGenerator(
          Duration.ofSeconds(PERIOD_SECONDS),
          CODE_DIGITS,
          TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("HmacSHA1 unavailable", ex);
    }
  }

  private static Properties defaultServerProperties() {
    Properties props = PSServer.getServerProps();
    return props != null ? props : new Properties();
  }

  private static Properties safeProps(Properties props) {
    return props != null ? props : new Properties();
  }
}
