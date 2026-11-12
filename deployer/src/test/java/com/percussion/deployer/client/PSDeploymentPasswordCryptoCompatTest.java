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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.deployer.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.deployer.objectstore.PSDbmsInfo;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round-trip compatibility proof for the deployer password-crypto call sites (issue #6057).
 *
 * <p>The production call sites ({@code PSDbmsInfo}, {@code PSDeploymentServerConnection}, {@code
 * PSDeploymentHandler}) encrypt and decrypt exclusively via {@code PSEncryptor} (AES/GCM). The
 * legacy lasagna/DES fallback ({@code PSCryptographer}/{@code PSLegacyEncrypter}) was removed
 * because {@code PSEncryptionKeyFactory} unconditionally rejects DES, so the fallback could never
 * decrypt pre-AES artifacts — it only threw {@code IllegalArgumentException} for input the AES
 * path rejects. These tests pin the modern path round-trip plus the documented empty-string
 * contract for empty and undecryptable input.
 */
public class PSDeploymentPasswordCryptoCompatTest {

  private static final String UID = "compat-test-user";
  private static final String PLAIN = "c0mpat-s3cret!";

  @TempDir Path temporaryFolder;
  private String rxdeploydir;

  @BeforeEach
  public void setup() throws IOException {
    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toFile().getAbsolutePath());
  }

  @AfterEach
  public void teardown() {
    if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);
  }

  @Test
  public void aesRoundTripViaDbmsInfo() {
    PSDbmsInfo info =
        new PSDbmsInfo("RhythmyxData", "inetdae7", "server", "database", "dbo", UID, PLAIN, false);

    String stored = info.getPassword(true);
    assertNotNull(stored);
    assertNotEquals(PLAIN, stored, "password must be encrypted at rest");
    assertEquals(PLAIN, info.getPassword(false));
  }

  @Test
  public void dbmsInfoEmptyPasswordStaysEmpty() {
    PSDbmsInfo info =
        new PSDbmsInfo("RhythmyxData", "inetdae7", "server", "database", "dbo", UID, "", false);
    assertEquals("", info.getPassword(false));
  }

  @Test
  public void dbmsInfoUndecryptableInputReturnsEmptyWithoutThrowing() {
    PSDbmsInfo info =
        new PSDbmsInfo(
            "RhythmyxData", "inetdae7", "server", "database", "dbo", UID, "not-aes-ciphertext",
            true);
    assertEquals("", assertDoesNotThrow(() -> info.getPassword(false)));
  }

  @Test
  public void aesRoundTripViaServerConnection() {
    String cipher = PSDeploymentServerConnection.encryptPwd(UID, PLAIN);
    assertNotNull(cipher);
    assertNotEquals(PLAIN, cipher, "password must be encrypted at rest");
    assertEquals(PLAIN, PSDeploymentServerConnection.decryptPwd(UID, cipher));
  }

  @Test
  public void serverConnectionEmptyInputsReturnEmpty() {
    assertEquals("", PSDeploymentServerConnection.decryptPwd(null, null));
    assertEquals("", PSDeploymentServerConnection.decryptPwd(UID, null));
    assertEquals("", PSDeploymentServerConnection.decryptPwd(UID, ""));
    assertEquals("", PSDeploymentServerConnection.decryptPwd(null, ""));
    assertEquals("", PSDeploymentServerConnection.encryptPwd(UID, ""));
    assertEquals("", PSDeploymentServerConnection.encryptPwd(UID, null));
  }

  @Test
  public void serverConnectionUndecryptableInputReturnsEmptyWithoutThrowing() {
    assertEquals(
        "", assertDoesNotThrow(() -> PSDeploymentServerConnection.decryptPwd(UID, "not-aes-ciphertext")));
    assertEquals(
        "", assertDoesNotThrow(() -> PSDeploymentServerConnection.decryptPwd(null, "not-aes-ciphertext")));
  }
}
