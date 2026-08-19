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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Residual unit coverage for {@link VirtualRedirectsLoader}. */
class VirtualRedirectsLoaderTest {

  private static final String SITE = "https://example.test/docs";

  @TempDir Path tempDir;

  @Test
  void missingFileIsNoOp() throws Exception {
    List<VirtualRedirect> loaded = VirtualRedirectsLoader.loadOptional(tempDir, SITE);
    assertTrue(loaded.isEmpty());
  }

  @Test
  void nullRootIsNoOp() throws Exception {
    assertTrue(VirtualRedirectsLoader.loadOptional(null, SITE).isEmpty());
  }

  @Test
  void loadsSampleFixture() throws Exception {
    Path sampleRoot = resolveSampleDocs();
    List<VirtualRedirect> loaded = VirtualRedirectsLoader.loadOptional(sampleRoot, SITE);
    assertEquals(1, loaded.size());
    assertEquals("/8.2/getting-started/installation.html", loaded.get(0).from());
    assertEquals("/8.2/getting-started/install.html", loaded.get(0).to());
    assertEquals(301, loaded.get(0).status());
  }

  @Test
  void parseEmptyYamlIsNoOp() throws Exception {
    List<VirtualRedirect> parsed = VirtualRedirectsLoader.parse(bytes("# comments only\n"), "empty");
    assertTrue(parsed.isEmpty());
  }

  @Test
  void parseRequiresFromAndTo() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                VirtualRedirectsLoader.parse(
                    bytes("redirects:\n  - from: /a.html\n"), "missing-to"));
    assertTrue(ex.getMessage().toLowerCase().contains("from and to"), ex.getMessage());
  }

  @Test
  void rejectsOpenRedirectHost() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                VirtualRedirectsLoader.requireSafeTarget("https://evil.example/phish", SITE));
    assertTrue(ex.getMessage().toLowerCase().contains("open redirect"), ex.getMessage());
  }

  @Test
  void rejectsProtocolRelativeTarget() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualRedirectsLoader.requireSafeTarget("//evil.example/x", SITE));
    assertTrue(ex.getMessage().toLowerCase().contains("open redirect"), ex.getMessage());
  }

  @Test
  void rejectsJavascriptTarget() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualRedirectsLoader.requireSafeTarget("javascript:alert(1)", SITE));
    assertTrue(ex.getMessage().toLowerCase().contains("open redirect"), ex.getMessage());
  }

  @Test
  void rejectsParentTraversalTarget() {
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualRedirectsLoader.requireSafeTarget("../escape.html", SITE));
    assertThrows(
        VirtualSiteException.class,
        () -> VirtualRedirectsLoader.requireSafeTarget("/a/../../etc/passwd", SITE));
  }

  @Test
  void allowsRelativeAndSameSiteTargets() throws Exception {
    VirtualRedirectsLoader.requireSafeTarget("/8.2/install.html", SITE);
    VirtualRedirectsLoader.requireSafeTarget("8.2/install.html", SITE);
    VirtualRedirectsLoader.requireSafeTarget("https://example.test/docs/8.2/index.html", SITE);
  }

  @Test
  void rejectsAbsoluteWhenSiteUrlMissing() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                VirtualRedirectsLoader.requireSafeTarget(
                    "https://example.test/docs/x.html", ""));
    assertTrue(ex.getMessage().toLowerCase().contains("open redirect"), ex.getMessage());
  }

  @Test
  void toOutputHrefNormalizesDirectoryFrom() {
    assertEquals(
        "8.2/old/index.html", VirtualRedirectsLoader.toOutputHref("/8.2/old/"));
    assertEquals("8.2/old.html", VirtualRedirectsLoader.toOutputHref("8.2/old.html"));
  }

  @Test
  void rejectsDuplicateFrom() {
    List<VirtualRedirect> dups =
        List.of(
            new VirtualRedirect("/a.html", "/b.html", 301),
            new VirtualRedirect("/a.html", "/c.html", 301));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> VirtualRedirectsLoader.validateAll(dups, SITE));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  private static ByteArrayInputStream bytes(String yaml) {
    return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
  }

  private static Path resolveSampleDocs() throws Exception {
    URL url =
        VirtualRedirectsLoaderTest.class
            .getClassLoader()
            .getResource("virtualsite/sample-docs/_config.yaml");
    if (url == null) {
      throw new IllegalStateException("sample-docs fixture missing from test classpath");
    }
    return Path.of(url.toURI()).getParent();
  }
}
