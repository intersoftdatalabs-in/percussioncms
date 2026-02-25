// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.utils.service;

import static com.percussion.test.TestAssertions.*;

import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.service.impl.PSUtilityService;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for PSUtilityService encryption/decryption. */
public class PSUtilityserviceTest {

  @TempDir File temporaryFolder;

  private String rxdeploydir;

  @BeforeEach
  public void setup() {
    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.getAbsolutePath());
  }

  @AfterEach
  public void teardown() {
    // Reset the deploy dir property if it was set prior to test
    if (rxdeploydir != null) {
      System.setProperty("rxdeploydir", rxdeploydir);
    }
  }

  @Test
  public void encryptDecryptStringTest() {
    var defaultKey =
        PSLegacyEncrypter.getInstance(
                temporaryFolder.getAbsolutePath().concat(PSEncryptor.SECURE_DIR))
            .DEFAULT_KEY();

    var stringToBeEncrypted = "http://www.yahoo.com";

    var service = new PSUtilityService();

    var encryptedString = service.encryptString(stringToBeEncrypted, defaultKey);

    var decryptedString = service.decryptString(encryptedString, defaultKey);
    assertEquals(stringToBeEncrypted, decryptedString);
  }
}
