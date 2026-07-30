/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.utils.container.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.container.DefaultConfigurationContextImpl;
import com.percussion.utils.container.IPSJndiDatasource;
import com.percussion.utils.container.PSJettyConnectorsTest;
import com.percussion.utils.io.PathUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JettyDatasourceConfigurationAdapterTest {

  @TempDir public Path temporaryFolder;

  private String rxdeploydir;

  @BeforeEach
  public void setup() {
    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toAbsolutePath().toString());
  }

  @AfterEach
  public void teardown() {
    // Reset the deploy dir property if it was set prior to test
    if (rxdeploydir != null) {
      System.setProperty("rxdeploydir", rxdeploydir);
    } else {
      System.clearProperty("rxdeploydir");
    }
  }

  @Test
  public void load() throws IOException {
    Path root = seedJettyEtc(temporaryFolder);
    JettyDatasourceConfigurationAdapter adapter = new JettyDatasourceConfigurationAdapter();

    PSLegacyEncrypter legacy =
        PSLegacyEncrypter.getInstance(
            PathUtils.getRxPath().toAbsolutePath().toString().concat(PSEncryptor.SECURE_DIR));
    DefaultConfigurationContextImpl fromCtx =
        new DefaultConfigurationContextImpl(root, legacy.getPartTwoKey());
    DefaultConfigurationContextImpl toCtx =
        new DefaultConfigurationContextImpl(root, legacy.getPartTwoKey());
    adapter.load(fromCtx);

    toCtx.copyFrom(fromCtx);

    JettyInstallationPropertiesConfigurationAdapter jettyAdapter =
        new JettyInstallationPropertiesConfigurationAdapter();
    jettyAdapter.save(toCtx);
    adapter.save(toCtx);
  }

  /**
   * Empty repository passwords (H2 default sa/) must be written as plain empty with {@code
   * pwd.encrypted=N}. Encrypting empty produces ciphertext that fails decrypt under a different
   * rxdeploydir/secure-dir and blocks Jetty webapp start (#548 / #1500 matrix).
   */
  @Test
  public void save_emptyPassword_writesUnencryptedEmpty() throws Exception {
    Path root = seedJettyEtc(temporaryFolder);
    JettyDatasourceConfigurationAdapter adapter = new JettyDatasourceConfigurationAdapter();
    DefaultConfigurationContextImpl ctx = newContext(root);
    adapter.load(ctx);

    List<IPSJndiDatasource> datasources = ctx.getConfig().getDatasources();
    assertFalse(datasources.isEmpty(), "fixture must load at least one datasource");
    IPSJndiDatasource ds = datasources.get(0);
    ds.setPassword("");
    ds.setUserId("sa");
    ctx.getConfig().setDatasources(datasources);

    adapter.save(ctx);

    Properties saved = loadPercDsProperties(root);
    assertEquals("", nullToEmpty(saved.getProperty("perc.ds.1.pwd")));
    assertEquals("N", saved.getProperty("perc.ds.1.pwd.encrypted"));
    assertEquals("sa", saved.getProperty("perc.ds.1.uid"));
  }

  /**
   * Non-empty passwords should still be stored encrypted when encrypt succeeds (pwd.encrypted=Y and
   * ciphertext differs from plaintext).
   */
  @Test
  public void save_nonEmptyPassword_encryptsWhenPossible() throws Exception {
    Path root = seedJettyEtc(temporaryFolder);
    // Secure dir under install root so PSEncryptor can materialize keys
    Files.createDirectories(root.resolve(PSEncryptor.SECURE_DIR.replaceFirst("^[/\\\\]+", "")));

    JettyDatasourceConfigurationAdapter adapter = new JettyDatasourceConfigurationAdapter();
    DefaultConfigurationContextImpl ctx = newContext(root);
    adapter.load(ctx);

    List<IPSJndiDatasource> datasources = ctx.getConfig().getDatasources();
    assertFalse(datasources.isEmpty());
    String plain = "test-secret-pwd";
    datasources.get(0).setPassword(plain);
    ctx.getConfig().setDatasources(datasources);

    adapter.save(ctx);

    Properties saved = loadPercDsProperties(root);
    String storedPwd = nullToEmpty(saved.getProperty("perc.ds.1.pwd"));
    String encryptedFlag = saved.getProperty("perc.ds.1.pwd.encrypted");
    assertFalse(storedPwd.isEmpty(), "stored password must not be empty");
    // Prefer encrypted path; if encrypt fails, adapter falls back to plain + N — still non-empty.
    if ("Y".equalsIgnoreCase(encryptedFlag)) {
      assertNotEquals(plain, storedPwd, "encrypted value must not equal plaintext");
    } else {
      assertEquals("N", encryptedFlag);
      assertEquals(plain, storedPwd);
    }
  }

  private static DefaultConfigurationContextImpl newContext(Path root) {
    PSLegacyEncrypter legacy =
        PSLegacyEncrypter.getInstance(
            PathUtils.getRxPath().toAbsolutePath().toString().concat(PSEncryptor.SECURE_DIR));
    return new DefaultConfigurationContextImpl(root, legacy.getPartTwoKey());
  }

  private static Path seedJettyEtc(Path root) throws IOException {
    InputStream srcInstallProps =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/installation.properties");
    InputStream srcLoginConf =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/login.conf");
    InputStream srcPercDsXML =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml");
    InputStream srcPercDsProperties =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties");

    Path etc = root.resolve("jetty").resolve("base").resolve("etc");
    Files.createDirectories(etc);

    Files.copy(srcInstallProps, etc.resolve("installation.properties"));
    Files.copy(srcLoginConf, etc.resolve("login.conf"));
    Files.copy(srcPercDsXML, etc.resolve("perc-ds.xml"));
    Files.copy(srcPercDsProperties, etc.resolve("perc-ds.properties"));
    return root;
  }

  private static Properties loadPercDsProperties(Path root) throws IOException {
    Path propsPath =
        root.resolve("jetty").resolve("base").resolve("etc").resolve("perc-ds.properties");
    assertTrue(Files.isRegularFile(propsPath), "perc-ds.properties must exist after save");
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propsPath)) {
      props.load(in);
    }
    return props;
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v;
  }
}
