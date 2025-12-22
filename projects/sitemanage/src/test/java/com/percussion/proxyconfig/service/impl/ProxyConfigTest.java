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
// REFACTORED: CP-JAVA11
package com.percussion.proxyconfig.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.PSEncryptor;
import com.percussion.share.dao.PSSerializerUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import jakarta.xml.bind.UnmarshalException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link ProxyConfig}. Sunny Sal: "Encrypting passwords like a boss!" */
public class ProxyConfigTest {

  @TempDir Path tempDir;

  private String rxdeploydir;

  @BeforeEach
  void setUp() {
    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", tempDir.toFile().getAbsolutePath());
  }

  @AfterEach
  void teardown() {
    if (rxdeploydir != null) {
      System.setProperty("rxdeploydir", rxdeploydir);
    }
  }

  @Test
  void testLoadXml() throws Exception {
    var configs = getConfigsFromFile("ProxyConfigTest_Empty.xml");
    assertEquals(0, configs.size());

    configs = getConfigsFromFile("ProxyConfigTest.xml");
    assertEquals(3, configs.size());

    var configs2 = getConfigsFromFile("ProxyConfigTest.xml");
    assertTrue(compareConfigs(configs, configs2));

    // Read a commented sample config file, like the one created on a fresh install.
    var configs3 = getConfigsFromFile("ProxyConfigTest_Commented.xml");
    assertEquals(0, configs3.size());

    // Read an invalid file (root element commented out)
    assertThrows(
        UnmarshalException.class, () -> getConfigsFromFile("ProxyConfigTest_Empty_Invalid.xml"));
  }

  @Test
  void testLoadOnlyHostPort() throws Exception {
    var configs = getConfigsFromFile("ProxyConfigTestHostPort.xml");
    assertEquals(1, configs.size());
    assertNotNull(configs.get(0).getHost());
    assertNotNull(configs.get(0).getPort());
    assertNull(configs.get(0).getUser());
    assertNull(configs.get(0).getPassword());
  }

  @Test
  void testConvertToEncryptedPassword() throws Exception {
    var fileContent = encryptPassword("ProxyConfigTest.xml");
    var servers = getConfigs(new ByteArrayInputStream(fileContent.getBytes()));
    var servers2 = getConfigsFromFile("ProxyConfigTest_EncryptedPassword.xml");
    assertTrue(compareConfigs(servers, servers2));
  }

  /**
   * Simulate encrypting the password of the specified file.
   *
   * @param file the file name, assumed not null.
   * @return the file content with the encrypted password and proper flag.
   */
  private String encryptPassword(String file) throws Exception {
    try (InputStream in = getClass().getResourceAsStream(file)) {
      var config = PSSerializerUtils.unmarshalWithValidation(in, ProxyConfigurations.class);
      for (var s : config.getConfigs()) {
        var origPw = s.getPassword();
        var origPwVal = origPw.getValue();
        origPw.setEncrypted(Boolean.TRUE);
        var enc = PSEncryptor.encryptString(rxdeploydir, origPwVal);
        origPw.setValue(enc);
        // make sure password can be decrypted
        var pw = PSEncryptor.decryptString(rxdeploydir, enc);
        assertEquals(origPwVal, pw);
      }
      return PSSerializerUtils.marshal(config);
    }
  }

  private List<ProxyConfig> getConfigs(InputStream in) throws Exception {
    var config = PSSerializerUtils.unmarshalWithValidation(in, ProxyConfigurations.class);
    return config.getConfigs();
  }

  private List<ProxyConfig> getConfigsFromFile(String file) throws Exception {
    try (InputStream in = getClass().getResourceAsStream(file)) {
      return getConfigs(in);
    }
  }

  /**
   * Compares two lists of configs for equality.
   *
   * @param config1 first list
   * @param config2 second list
   * @return true if the lists are equal, false otherwise.
   */
  private boolean compareConfigs(List<ProxyConfig> config1, List<ProxyConfig> config2) {
    if (config1.size() != config2.size()) {
      return false;
    }
    for (var ds1 : config1) {
      boolean match =
          config2.stream()
              .anyMatch(
                  ds2 ->
                      ds2.getHost().equals(ds1.getHost())
                          && ds2.getPort().equals(ds1.getPort())
                          && ((ds2.getUser() == null && ds1.getUser() == null)
                              || (ds2.getUser() != null && ds2.getUser().equals(ds1.getUser()))));
      if (!match) {
        return false;
      }
    }
    return true;
  }
}
