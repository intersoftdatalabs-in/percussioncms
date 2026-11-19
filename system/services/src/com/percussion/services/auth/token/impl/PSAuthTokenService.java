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
package com.percussion.services.auth.token.impl;

import com.percussion.server.PSServer;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.token.IPSAuthTokenService;
import com.percussion.services.auth.token.PSConsumedAuthToken;
import com.percussion.services.auth.token.PSIssuedAuthToken;
import com.percussion.system.utils.PSBaseBean;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** Issues 256-bit random tokens, persisting only SHA-256 digests with atomic single consumption. */
@PSBaseBean("sys_authTokenService")
public class PSAuthTokenService implements IPSAuthTokenService {

  public static final String PROP_EMAIL_VERIFY_TTL_MINUTES = "auth.token.emailVerifyTtlMinutes";
  // mkd-code-review[secrets.heuristic]: configuration property key name for reset token TTL (MKD #49), not a credential.
  public static final String PROP_PASSWORD_RESET_TTL_MINUTES = "auth.token.passwordResetTtlMinutes";
  public static final String PROP_BREAK_GLASS_TTL_MINUTES = "auth.token.adminBreakGlassTtlMinutes";

  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final long MIN_TTL_MINUTES = 5L;
  private static final long MAX_TTL_MINUTES = 1440L;
  private static final long DEFAULT_EMAIL_VERIFY_MINUTES = 30L;
  private static final long DEFAULT_PASSWORD_RESET_MINUTES = 20L;
  private static final long DEFAULT_BREAK_GLASS_MINUTES = 60L;

  private final IPSAuthSecurityDao authSecurityDao;
  private final Supplier<Properties> serverPropsSupplier;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public PSAuthTokenService(IPSAuthSecurityDao authSecurityDao) {
    this(authSecurityDao, PSAuthTokenService::defaultServerProperties, Clock.systemUTC());
  }

  /** Package-visible constructor for deterministic unit tests. */
  PSAuthTokenService(
      IPSAuthSecurityDao authSecurityDao,
      Supplier<Properties> serverPropsSupplier,
      Clock clock) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.serverPropsSupplier =
        Objects.requireNonNull(serverPropsSupplier, "serverPropsSupplier");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public PSIssuedAuthToken issueToken(
      String tokenType,
      String userName,
      String targetEmail,
      String createdBy,
      String requestIp,
      Duration ttl) {
    if (StringUtils.isBlank(tokenType)) {
      throw new IllegalArgumentException("tokenType is required");
    }
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    String cleanType = tokenType.trim();
    String cleanUser = userName.trim();
    Duration effectiveTtl = normalizeTtl(cleanType, ttl);

    Instant now = clock.instant();
    Instant expiresAt = now.plus(effectiveTtl);
    authSecurityDao.invalidateUnconsumedTokens(cleanUser, cleanType, now);

    String rawToken = generateRawToken();
    PSAuthToken entity = new PSAuthToken();
    entity.setTokenId(UUID.randomUUID().toString());
    entity.setTokenHash(sha256Hex(rawToken));
    entity.setTokenType(cleanType);
    entity.setUserName(cleanUser);
    entity.setTargetEmail(StringUtils.trimToNull(targetEmail));
    entity.setCreatedBy(StringUtils.trimToNull(createdBy));
    entity.setRequestIp(StringUtils.trimToNull(requestIp));
    entity.setCreatedAt(Date.from(now));
    entity.setExpiresAt(Date.from(expiresAt));
    authSecurityDao.saveToken(entity);

    return new PSIssuedAuthToken(
        entity.getTokenId(),
        rawToken,
        cleanType,
        cleanUser,
        entity.getTargetEmail(),
        expiresAt);
  }

  @Override
  public Optional<PSConsumedAuthToken> consumeToken(String rawToken, String expectedTokenType) {
    if (StringUtils.isBlank(rawToken) || StringUtils.isBlank(expectedTokenType)) {
      return Optional.empty();
    }
    String tokenHash = sha256Hex(rawToken.trim());
    String cleanType = expectedTokenType.trim();
    Instant now = clock.instant();

    int updated = authSecurityDao.consumeAuthTokenAtomically(tokenHash, cleanType, now);
    if (updated != 1) {
      return Optional.empty();
    }
    Optional<PSAuthToken> loaded = authSecurityDao.findTokenByHash(tokenHash);
    if (loaded.isEmpty()) {
      return Optional.empty();
    }
    PSAuthToken entity = loaded.get();
    return Optional.of(
        new PSConsumedAuthToken(
            entity.getTokenId(),
            entity.getTokenType(),
            entity.getUserName(),
            entity.getTargetEmail(),
            entity.getCreatedBy()));
  }

  @Override
  public Duration resolveTtl(String tokenType) {
    Properties props = safeProps(serverPropsSupplier.get());
    if (PSAuthToken.TYPE_PASSWORD_RESET.equals(tokenType)) {
      return Duration.ofMinutes(
          readClampedMinutes(
              props.getProperty(PROP_PASSWORD_RESET_TTL_MINUTES), DEFAULT_PASSWORD_RESET_MINUTES));
    }
    if (PSAuthToken.TYPE_ADMIN_BREAK_GLASS.equals(tokenType)) {
      return Duration.ofMinutes(
          readClampedMinutes(
              props.getProperty(PROP_BREAK_GLASS_TTL_MINUTES), DEFAULT_BREAK_GLASS_MINUTES));
    }
    return Duration.ofMinutes(
        readClampedMinutes(
            props.getProperty(PROP_EMAIL_VERIFY_TTL_MINUTES), DEFAULT_EMAIL_VERIFY_MINUTES));
  }

  private Duration normalizeTtl(String tokenType, Duration requestedTtl) {
    if (requestedTtl == null || requestedTtl.isZero() || requestedTtl.isNegative()) {
      return resolveTtl(tokenType);
    }
    return requestedTtl;
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String sha256Hex(String rawToken) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
    }
  }

  private static long readClampedMinutes(String rawValue, long defaultMinutes) {
    if (StringUtils.isBlank(rawValue)) {
      return defaultMinutes;
    }
    try {
      long parsed = Long.parseLong(rawValue.trim());
      return Math.max(MIN_TTL_MINUTES, Math.min(MAX_TTL_MINUTES, parsed));
    } catch (NumberFormatException ex) {
      return defaultMinutes;
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
