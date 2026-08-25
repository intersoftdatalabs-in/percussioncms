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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Residual unit coverage for {@link VirtualSiteConfigLoader}. */
class VirtualSiteConfigLoaderTest {

  @TempDir Path tempDir;

  @Test
  void loadsSampleFixtureConfig() throws Exception {
    Path sampleRoot = resolveSampleDocs();
    VirtualSiteConfig config =
        VirtualSiteConfigLoader.load(sampleRoot, null, "sample");

    assertEquals("Sample Docs", config.siteTitle());
    assertEquals("sample", config.siteKey());
    assertEquals("page.html", config.layoutFile());
    assertEquals(1, config.versions().size());
    assertEquals("8.2", config.versions().get(0).id());
    assertTrue(config.versions().get(0).defaultVersion());
    assertEquals(1, config.nav().size());
    assertEquals("getting-started", config.nav().get(0).id());
    assertEquals(sampleRoot, config.root());
  }

  @Test
  void blankConfigFileNameUsesDefault() throws Exception {
    Path sampleRoot = resolveSampleDocs();
    VirtualSiteConfig config =
        VirtualSiteConfigLoader.load(sampleRoot, "  ", "k");
    assertEquals("Sample Docs", config.siteTitle());
  }

  @Test
  void sqlScalarInsteadOfMappingFailsFast() throws Exception {
    Path root = tempDir.resolve("sql-scalar");
    Files.createDirectories(root);
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql: "not-a-mapping"
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualSiteConfigLoader.load(root, null, "sql-docs"));
    assertTrue(ex.getMessage().toLowerCase().contains("sql"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("mapping"), ex.getMessage());
  }

  @Test
  void httpScalarInsteadOfMappingFailsFast() throws Exception {
    Path root = tempDir.resolve("http-scalar");
    Files.createDirectories(root);
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: HTTP Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        http: "not-a-mapping"
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualSiteConfigLoader.load(root, null, "http-docs"));
    assertTrue(ex.getMessage().toLowerCase().contains("http"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("mapping"), ex.getMessage());
  }

  @Test
  void missingConfigFileFails() {
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualSiteConfigLoader.load(tempDir, "_config.yaml", "k"));
  }

  @Test
  void loadOrDefaultInfersVersionFoldersWhenYamlMissing() throws Exception {
    Path root = tempDir.resolve("csv-default");
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    Files.createDirectories(root.resolve("assets"));
    VirtualSiteConfig config = VirtualSiteConfigLoader.loadOrDefault(root, null, "csv-docs");
    assertEquals("csv-docs", config.siteTitle());
    assertEquals(1, config.versions().size());
    assertEquals("8.2", config.versions().get(0).id());
    assertTrue(config.versions().get(0).defaultVersion());
    assertEquals(root.normalize(), config.root());
  }

  @Test
  void loadOrDefaultFailsWhenNoVersionFolders() throws Exception {
    Path root = tempDir.resolve("csv-empty");
    Files.createDirectories(root.resolve("_theme"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualSiteConfigLoader.loadOrDefault(root, "_config.yaml", "k"));
    assertTrue(ex.getMessage().toLowerCase().contains("version"), ex.getMessage());
  }

  @Test
  void loadOrDefaultUsesYamlWhenPresent() throws Exception {
    Path sampleRoot = resolveSampleDocs();
    VirtualSiteConfig config =
        VirtualSiteConfigLoader.loadOrDefault(sampleRoot, null, "sample");
    assertEquals("Sample Docs", config.siteTitle());
    assertEquals("8.2", config.versions().get(0).id());
  }

  @Test
  void secondLoadAfterConfigEditSeesCurrentTitleWithoutCache() throws Exception {
    Path root = tempDir.resolve("live-config");
    Files.createDirectories(root);
    Path yaml = root.resolve("_config.yaml");
    Files.writeString(
        yaml,
        """
        site:
          title: First Config Title
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        """,
        StandardCharsets.UTF_8);
    VirtualSiteConfig first = VirtualSiteConfigLoader.load(root, null, "k");
    assertEquals("First Config Title", first.siteTitle());

    Files.writeString(
        yaml,
        """
        site:
          title: Second Config Title
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        """,
        StandardCharsets.UTF_8);
    VirtualSiteConfig second = VirtualSiteConfigLoader.load(root, null, "k");
    assertEquals("Second Config Title", second.siteTitle());
  }

  @Test
  void nullRootFails() {
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualSiteConfigLoader.load(null, "_config.yaml", "k"));
  }

  @Test
  void requiresAtLeastOneVersion() throws Exception {
    Path root = tempDir.resolve("empty-versions");
    Files.createDirectories(root);
    Files.writeString(
        root.resolve("_config.yaml"),
        "site:\n  title: T\nversions: []\n",
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualSiteConfigLoader.load(root, "_config.yaml", "k"));
    assertTrue(ex.getMessage().toLowerCase().contains("version"), ex.getMessage());
  }

  @Test
  void sqlSpecToStringOmitsPassword() {
    VirtualSiteConfig.SqlSpec spec =
        new VirtualSiteConfig.SqlSpec(
            "jdbc:h2:mem:t",
            "sa",
            "super-secret",
            "select 1",
            "",
            "id",
            "title",
            "body",
            "path",
            "order",
            "version");
    String text = spec.toString();
    assertFalse(text.contains("super-secret"), text);
    assertFalse(text.toLowerCase().contains("password"), text);
    assertTrue(text.contains("jdbc:h2:mem:t"), text);
    assertTrue(text.contains("sa"), text);
  }

  @Test
  void rejectsNonMappingRoot() throws Exception {
    Path root = tempDir.resolve("list-root");
    Files.createDirectories(root);
    Files.writeString(root.resolve("_config.yaml"), "- just\n- a\n- list\n", StandardCharsets.UTF_8);
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualSiteConfigLoader.load(root, "_config.yaml", "k"));
  }

  private static Path resolveSampleDocs() throws Exception {
    URL url =
        VirtualSiteConfigLoaderTest.class
            .getClassLoader()
            .getResource("virtualsite/sample-docs/_config.yaml");
    if (url == null) {
      throw new IllegalStateException("sample-docs fixture missing from test classpath");
    }
    return Path.of(url.toURI()).getParent();
  }
}
