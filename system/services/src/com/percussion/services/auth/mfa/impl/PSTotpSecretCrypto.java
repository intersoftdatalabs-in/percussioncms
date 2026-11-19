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

import com.percussion.security.PSEncryptor;
import com.percussion.server.PSServer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts and decrypts TOTP shared secrets using AES-256-GCM backed by the instance {@code
 * rxconfig/secure/.key} file and per-account AAD binding.
 */
final class PSTotpSecretCrypto {

  private static final String PREFIX_V1 = "v1:";
  private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final Supplier<byte[]> masterKeySupplier;
  private final SecureRandom secureRandom = new SecureRandom();

  PSTotpSecretCrypto() {
    this(PSTotpSecretCrypto::loadInstanceKeyMaterial);
  }

  PSTotpSecretCrypto(Supplier<byte[]> masterKeySupplier) {
    this.masterKeySupplier = Objects.requireNonNull(masterKeySupplier, "masterKeySupplier");
  }

  String encrypt(String userName, byte[] secretBytes) {
    if (secretBytes == null || secretBytes.length == 0) {
      throw new IllegalArgumentException("TOTP secret bytes are required");
    }
    byte[] iv = new byte[IV_BYTES];
    secureRandom.nextBytes(iv);
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(), new GCMParameterSpec(TAG_BITS, iv));
      cipher.updateAAD(buildAad(userName));
      byte[] ciphertext = cipher.doFinal(secretBytes);
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return PREFIX_V1 + Base64.getEncoder().encodeToString(combined);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to encrypt TOTP secret", ex);
    }
  }

  byte[] decrypt(String userName, String encoded) {
    if (encoded == null || !encoded.startsWith(PREFIX_V1)) {
      throw new IllegalStateException("Unsupported TOTP ciphertext envelope");
    }
    byte[] combined;
    try {
      combined = Base64.getDecoder().decode(encoded.substring(PREFIX_V1.length()));
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("Corrupt TOTP ciphertext payload", ex);
    }
    if (combined.length <= IV_BYTES) {
      throw new IllegalStateException("Corrupt TOTP ciphertext payload");
    }
    byte[] iv = new byte[IV_BYTES];
    byte[] ciphertext = new byte[combined.length - IV_BYTES];
    System.arraycopy(combined, 0, iv, 0, IV_BYTES);
    System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
      cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(), new GCMParameterSpec(TAG_BITS, iv));
      cipher.updateAAD(buildAad(userName));
      return cipher.doFinal(ciphertext);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to decrypt TOTP secret", ex);
    }
  }

  private SecretKey deriveAesKey() throws NoSuchAlgorithmException {
    byte[] rawMaster = masterKeySupplier.get();
    if (rawMaster == null || rawMaster.length == 0) {
      throw new IllegalStateException("Instance encryption key material is unavailable");
    }
    byte[] aesKeyBytes = MessageDigest.getInstance("SHA-256").digest(rawMaster);
    return new SecretKeySpec(aesKeyBytes, "AES");
  }

  private static byte[] buildAad(String userName) {
    String clean = userName != null ? userName.trim().toLowerCase(Locale.ROOT) : "";
    return ("totp:v1:" + clean).getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] loadInstanceKeyMaterial() {
    File rxDir = PSServer.getRxDir();
    if (rxDir == null) {
      throw new IllegalStateException("Server root directory is unavailable");
    }
    Path secureDir = rxDir.toPath().resolve("rxconfig").resolve("secure");
    Path keyFile = secureDir.resolve(".key");
    if (!Files.isRegularFile(keyFile)) {
      try {
        Files.createDirectories(secureDir);
      } catch (IOException ex) {
        throw new IllegalStateException("Unable to create secure key directory", ex);
      }
      PSEncryptor.getInstance("AES", secureDir.toString());
    }
    try {
      return Files.readAllBytes(keyFile);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to read instance key file", ex);
    }
  }
}
