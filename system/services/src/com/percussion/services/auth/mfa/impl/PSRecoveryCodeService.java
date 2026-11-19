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

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSMfaRecoveryCode;
import com.percussion.services.auth.mfa.IPSRecoveryCodeService;
import com.percussion.system.utils.PSBaseBean;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** Issues and atomically consumes salted PBKDF2-HMAC-SHA256 Crockford Base32 recovery codes. */
@PSBaseBean("sys_recoveryCodeService")
public class PSRecoveryCodeService implements IPSRecoveryCodeService {

  public static final int DEFAULT_BATCH_SIZE = 10;
  private static final int CODE_LENGTH = 10;
  private static final int SALT_BYTES = 16;
  private static final int HASH_BITS = 256;
  private static final int PROD_ITERATIONS = 210_000;
  private static final char[] CROCKFORD_ALPHABET =
      "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

  private final IPSAuthSecurityDao authSecurityDao;
  private final Clock clock;
  private final int iterations;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public PSRecoveryCodeService(IPSAuthSecurityDao authSecurityDao) {
    this(authSecurityDao, Clock.systemUTC(), PROD_ITERATIONS);
  }

  /** Package-visible constructor allowing lower PBKDF2 cost in fast unit tests. */
  PSRecoveryCodeService(IPSAuthSecurityDao authSecurityDao, Clock clock, int iterations) {
    this.authSecurityDao = Objects.requireNonNull(authSecurityDao, "authSecurityDao");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.iterations = Math.max(1000, iterations);
  }

  @Override
  public List<String> generateAndReplaceCodes(String userName) {
    String cleanUser = requireUserName(userName);
    InstantNow now = new InstantNow(Date.from(clock.instant()));
    authSecurityDao.invalidateUnusedRecoveryCodesByUser(cleanUser, now.date());

    String batchId = UUID.randomUUID().toString();
    List<String> plaintextCodes = new ArrayList<>(DEFAULT_BATCH_SIZE);
    List<PSMfaRecoveryCode> entities = new ArrayList<>(DEFAULT_BATCH_SIZE);

    for (int i = 0; i < DEFAULT_BATCH_SIZE; i++) {
      String rawNormalized = generateCodeWithDigitAndLetter();
      String formatted = rawNormalized.substring(0, 5) + "-" + rawNormalized.substring(5);
      byte[] salt = new byte[SALT_BYTES];
      secureRandom.nextBytes(salt);
      String saltB64 = Base64.getEncoder().encodeToString(salt);
      String hashB64 = hashCode(rawNormalized, salt);

      PSMfaRecoveryCode entity = new PSMfaRecoveryCode();
      entity.setCodeId(UUID.randomUUID().toString());
      entity.setUserName(cleanUser);
      entity.setBatchId(batchId);
      entity.setCodeSalt(saltB64);
      entity.setCodeHash(hashB64);
      entity.setCreatedAt(now.date());

      entities.add(entity);
      plaintextCodes.add(formatted);
    }

    authSecurityDao.saveRecoveryCodes(entities);
    return List.copyOf(plaintextCodes);
  }

  @Override
  public boolean consumeCode(String userName, String rawCode) {
    if (StringUtils.isBlank(userName)) {
      return false;
    }
    String normalized = normalizeCode(rawCode);
    if (normalized == null) {
      return false;
    }
    String cleanUser = userName.trim();
    List<PSMfaRecoveryCode> unused = authSecurityDao.findUnusedRecoveryCodesByUser(cleanUser);
    Date now = Date.from(clock.instant());
    for (PSMfaRecoveryCode row : unused) {
      byte[] salt = Base64.getDecoder().decode(row.getCodeSalt());
      String candidateHash = hashCode(normalized, salt);
      if (java.security.MessageDigest.isEqual(
          candidateHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
          row.getCodeHash().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
        return authSecurityDao.consumeRecoveryCodeAtomically(row.getCodeId(), now) == 1;
      }
    }
    return false;
  }

  @Override
  public long countRemaining(String userName) {
    if (StringUtils.isBlank(userName)) {
      return 0L;
    }
    return authSecurityDao.countUnusedRecoveryCodes(userName.trim());
  }

  private String generateCodeWithDigitAndLetter() {
    while (true) {
      StringBuilder sb = new StringBuilder(CODE_LENGTH);
      boolean hasDigit = false;
      boolean hasLetter = false;
      for (int i = 0; i < CODE_LENGTH; i++) {
        char c = CROCKFORD_ALPHABET[secureRandom.nextInt(CROCKFORD_ALPHABET.length)];
        if (c >= '0' && c <= '9') {
          hasDigit = true;
        } else {
          hasLetter = true;
        }
        sb.append(c);
      }
      if (hasDigit && hasLetter) {
        return sb.toString();
      }
    }
  }

  private String hashCode(String normalizedCode, byte[] salt) {
    PBEKeySpec spec =
        new PBEKeySpec(normalizedCode.toCharArray(), salt, iterations, HASH_BITS);
    try {
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] hash = skf.generateSecret(spec).getEncoded();
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException("Failed to hash recovery code", ex);
    } finally {
      spec.clearPassword();
    }
  }

  static String normalizeCode(String rawCode) {
    if (rawCode == null) {
      return null;
    }
    StringBuilder out = new StringBuilder(CODE_LENGTH);
    String upper = rawCode.toUpperCase(Locale.ROOT);
    for (int i = 0; i < upper.length(); i++) {
      char c = mapCrockfordChar(upper.charAt(i));
      if (c == 0) {
        continue;
      }
      if (c == '?') {
        return null;
      }
      out.append(c);
    }
    return out.length() == CODE_LENGTH ? out.toString() : null;
  }

  private static char mapCrockfordChar(char c) {
    if (Character.isWhitespace(c) || c == '-') {
      return 0;
    }
    if (c == 'I' || c == 'L') {
      return '1';
    }
    if (c == 'O') {
      return '0';
    }
    for (char allowed : CROCKFORD_ALPHABET) {
      if (c == allowed) {
        return c;
      }
    }
    return '?';
  }

  private static String requireUserName(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("userName is required");
    }
    return userName.trim();
  }

  private record InstantNow(Date date) {}
}
