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
import com.percussion.utils.container.PSStaticContainerUtils;
import com.percussion.utils.io.PathUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

  /**
   * 8.1.x Derby-era {@code perc-ds.xml} files build the URL from mixed content (literal {@code
   * jdbc:} / {@code :} text around {@code Property} refs). Jetty 12 concatenates that whitespace
   * verbatim, so after a Derby to H2 migration the driver receives {@code "jdbc:\n h2\n :\n
   * file:..."}, rejects it, and the server fails to start (issue #5656). Normalization must
   * rewrite the legacy shape to the {@code buildConnectionUrl} {@code Call} form and be
   * idempotent.
   */
  @Test
  public void normalizeLegacyJdbcUrlSets_rewritesMixedContentToCallForm() throws Exception {
    String legacy;
    try (InputStream in =
        PSJettyConnectorsTest.class.getResourceAsStream(
            "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml")) {
      legacy = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertTrue(legacy.contains("jdbc:"), "fixture must use the legacy mixed-content jdbcUrl form");

    Document doc;
    try (InputStream in = new ByteArrayInputStream(legacy.getBytes(StandardCharsets.UTF_8))) {
      doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
    }

    assertTrue(
        JettyDatasourceConfigurationAdapter.normalizeLegacyJdbcUrlSets(doc),
        "legacy mixed-content jdbcUrl must be rewritten");
    assertFalse(
        JettyDatasourceConfigurationAdapter.normalizeLegacyJdbcUrlSets(doc),
        "normalization must be idempotent");

    Element jdbcUrl = findJdbcUrlSet(doc);
    assertTrue(hasBuildConnectionUrlCall(jdbcUrl), "jdbcUrl must use the buildConnectionUrl Call");
    assertFalse(
        hasLiteralJdbcText(jdbcUrl), "no literal jdbc: text may remain in the jdbcUrl Set");
    // Existing Property refs (names + defaults) must survive the rewrite.
    assertTrue(
        PSXmlDocumentBuilder.toString(doc).contains("perc.ds.1.driver.name"),
        "driver Property ref must be preserved");
    assertTrue(
        PSXmlDocumentBuilder.toString(doc).contains("perc.ds.1.server"),
        "server Property ref must be preserved");
  }

  /**
   * End-to-end: saving a configuration seeded from a legacy 8.1.x {@code perc-ds.xml} must
   * persist the normalized {@code buildConnectionUrl} form so a post-upgrade boot resolves a
   * clean URL (issue #5656).
   */
  @Test
  public void save_legacyPercDsXml_persistsNormalizedJdbcUrl() throws Exception {
    Path root = seedJettyEtc(temporaryFolder);
    JettyDatasourceConfigurationAdapter adapter = new JettyDatasourceConfigurationAdapter();
    DefaultConfigurationContextImpl ctx = newContext(root);
    adapter.load(ctx);
    assertFalse(ctx.getConfig().getDatasources().isEmpty());

    adapter.save(ctx);

    Path dsXml = root.resolve("jetty").resolve("base").resolve("etc").resolve("perc-ds.xml");
    Document saved;
    try (InputStream in = Files.newInputStream(dsXml)) {
      saved = PSXmlDocumentBuilder.createXmlDocument(in, false);
    }
    Element jdbcUrl = findJdbcUrlSet(saved);
    assertTrue(hasBuildConnectionUrlCall(jdbcUrl), "saved jdbcUrl must use buildConnectionUrl");
    assertFalse(hasLiteralJdbcText(jdbcUrl), "saved jdbcUrl must not keep literal jdbc: text");
  }

  /**
   * Installer/migration property values may carry surrounding whitespace; the built URL must
   * still be exact (issue #5656).
   */
  @Test
  public void buildConnectionUrl_trimsFragments() {
    assertEquals(
        "jdbc:h2:file:C:/INSTALLS/cms/Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE",
        PSStaticContainerUtils.buildConnectionUrl(
            "\n h2\n ", " file:C:/INSTALLS/cms/Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE\n "));
    assertEquals(
        "jdbc:derby:CMDB", PSStaticContainerUtils.buildConnectionUrl("derby", "CMDB"));
  }

  private static Element findJdbcUrlSet(Document doc) {
    NodeList sets = doc.getElementsByTagName("Set");
    for (int i = 0; i < sets.getLength(); i++) {
      Element set = (Element) sets.item(i);
      if ("jdbcUrl".equals(set.getAttribute("name"))) {
        return set;
      }
    }
    throw new AssertionError("no <Set name=\"jdbcUrl\"> found");
  }

  private static boolean hasBuildConnectionUrlCall(Element set) {
    NodeList calls = set.getElementsByTagName("Call");
    for (int i = 0; i < calls.getLength(); i++) {
      Node name = calls.item(i).getAttributes().getNamedItem("name");
      if (name != null && "buildConnectionUrl".equals(name.getNodeValue())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasLiteralJdbcText(Element set) {
    NodeList children = set.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().contains("jdbc:")) {
        return true;
      }
    }
    return false;
  }

  private static DefaultConfigurationContextImpl newContext(Path root) {    PSLegacyEncrypter legacy =
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
