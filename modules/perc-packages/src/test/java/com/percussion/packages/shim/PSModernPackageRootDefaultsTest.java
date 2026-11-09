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

package com.percussion.packages.shim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for product/H2 default modern package roots (#3130 / parent #2630).
 *
 * <p>Covers modern-present discovery, modern-absent empty list, and explicit path-separator
 * override. Classpath materialize is covered when product Packages resources are on the test
 * classpath (perc-packages module).
 */
class PSModernPackageRootDefaultsTest {

  @TempDir Path tempDir;

  @Test
  void discoverPackageRoots_modernPresent_listsChildPackageRoots() throws Exception {
    Path modern = tempDir.resolve("Packages").resolve("Modern");
    Path pkg = modern.resolve("perc.baseWidgets");
    Path widget = pkg.resolve("widgets").resolve("percSimpleText");
    Files.createDirectories(widget);
    Files.writeString(
        widget.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\"percSimpleText\",\"name\":\"Simple\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);

    List<Path> roots = PSModernPackageRootDefaults.discoverPackageRoots(modern);
    assertEquals(1, roots.size());
    assertEquals(pkg.toAbsolutePath().normalize(), roots.get(0));
    assertTrue(PSModernPackageRootDefaults.isModernPackageRoot(pkg));
  }

  @Test
  void discoverPackageRoots_modernAbsent_returnsEmpty() throws Exception {
    Path modern = tempDir.resolve("Packages").resolve("Modern");
    Files.createDirectories(modern);
    // empty dir and a non-modern child
    Files.createDirectories(modern.resolve("emptyChild"));

    List<Path> roots = PSModernPackageRootDefaults.discoverPackageRoots(modern);
    assertTrue(roots.isEmpty());

    assertTrue(
        PSModernPackageRootDefaults.discoverPackageRoots(tempDir.resolve("missing")).isEmpty());
  }

  @Test
  void resolve_explicitProperty_overridesDefaults() throws Exception {
    Path custom = tempDir.resolve("customRoot");
    Files.createDirectories(custom);
    Files.writeString(
        custom.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\"custom\",\"name\":\"Custom\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);

    List<Path> roots =
        PSModernPackageRootDefaults.resolve(custom.toString(), tempDir, null);
    assertEquals(1, roots.size());
    assertEquals(custom.toAbsolutePath().normalize(), roots.get(0));
  }

  @Test
  void resolve_blankProperty_modernPresentUnderInstall_discoversWithoutClasspath()
      throws Exception {
    Path rx = tempDir.resolve("rx");
    Path modern = rx.resolve(PSModernPackageRootDefaults.RELATIVE_MODERN_ROOTS_DIR);
    Path pkg = modern.resolve("perc.widgets.lists");
    Path widget = pkg.resolve("widgets").resolve("simplePageAutoList");
    Files.createDirectories(widget);
    Files.writeString(
        widget.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\"simplePageAutoList\",\"name\":\"List\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);

    List<Path> roots = PSModernPackageRootDefaults.resolve("  ", rx, null);
    assertEquals(1, roots.size());
    assertTrue(roots.get(0).endsWith(Path.of("perc.widgets.lists")));

    // Selection integration: modern wins when root is discovered
    PSDefinitionSourceSelection selection =
        PSLegacyDefinitionXmlShim.selectDefinition(
            "simplePageAutoList", roots, null, null, null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, selection.getKind());
  }

  @Test
  void resolve_blankProperty_modernAbsent_returnsEmpty_modernOnlyThrowsRetiredError()
      throws Exception {
    Path rx = tempDir.resolve("rxEmpty");
    Files.createDirectories(rx);

    List<Path> roots = PSModernPackageRootDefaults.resolve(null, rx, null);
    assertTrue(roots.isEmpty());

    Path widgets = rx.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgets);
    Path xml = widgets.resolve("customerOnly.xml");
    Files.writeString(xml, "<Widget/>", StandardCharsets.UTF_8);

    // Modern-only (#2852): retired legacy XML is never selected, even when present.
    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () ->
                PSLegacyDefinitionXmlShim.selectDefinition(
                    "customerOnly", roots, widgets, null, null));
    assertEquals("customerOnly", ex.getDefinitionId());
    assertTrue(ex.getMessage().contains("retired"), ex.getMessage());
  }

  @Test
  void materializeFromClasspath_whenProductPackagesPresent_writesModernRoots() throws Exception {
    // perc-packages test classpath includes Packages/** resources
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl.getResource(
            "Packages/perc.baseWidgets/widgets/percSimpleText/"
                + PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME)
        == null) {
      // Not on classpath in this environment — discovery path still covered above
      return;
    }

    Path dest = tempDir.resolve("materialized").resolve("Modern");
    int written = PSModernPackageRootDefaults.materializeFromClasspath(cl, dest);
    assertTrue(written > 0, "expected classpath modern widget files to materialize");
    List<Path> roots = PSModernPackageRootDefaults.discoverPackageRoots(dest);
    assertFalse(roots.isEmpty(), "materialized modern package roots");

    boolean hasBaseWidgets =
        roots.stream().anyMatch(p -> p.getFileName() != null
            && "perc.baseWidgets".equals(p.getFileName().toString()));
    assertTrue(hasBaseWidgets, "expected perc.baseWidgets among " + roots);

    PSDefinitionSourceSelection selection =
        PSLegacyDefinitionXmlShim.selectDefinition(
            "percSimpleText", roots, null, null, null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, selection.getKind());
  }

  @Test
  void parsePathSeparatorList_splitsPortable() {
    Path a = tempDir.resolve("a");
    Path b = tempDir.resolve("b");
    String prop = a + java.io.File.pathSeparator + b;
    List<Path> roots = PSModernPackageRootDefaults.parsePathSeparatorList(prop);
    assertEquals(2, roots.size());
  }

  @Test
  void safeResolveUnder_rejectsZipSlipAndAbsolute() throws Exception {
    Path root = tempDir.resolve("dest").toAbsolutePath().normalize();
    Files.createDirectories(root);

    Path ok = PSModernPackageRootDefaults.safeResolveUnder(root, "widgets/stem/file.json");
    assertTrue(ok.startsWith(root));
    assertTrue(ok.endsWith(Path.of("widgets", "stem", "file.json")));

    assertThrows(
        IOException.class,
        () -> PSModernPackageRootDefaults.safeResolveUnder(root, "../escape.txt"));
    assertThrows(
        IOException.class,
        () -> PSModernPackageRootDefaults.safeResolveUnder(root, "widgets/../../escape.txt"));
    assertThrows(
        IOException.class, () -> PSModernPackageRootDefaults.safeResolveUnder(root, "/abs.txt"));
  }

  @Test
  void listModernPackageNames_jarContainer_detectsWidgetsAndRootManifestPackages()
      throws Exception {
    String manifest = PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME;
    Path jar = tempDir.resolve("prod.jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      writeJarEntry(
          out, "Packages/perc.baseWidgets/widgets/percSimpleText/" + manifest, "{\"id\":\"x\"}");
      writeJarEntry(out, "Packages/perc.other/" + manifest, "{\"id\":\"y\"}");
      writeJarEntry(out, "Packages/perc.other/Definition.xml", "<Widget/>");
    }

    Set<String> names = PSModernPackageRootDefaults.listModernPackageNames(jar);
    assertTrue(names.contains("perc.baseWidgets"), "widgets-rooted package: " + names);
    assertTrue(names.contains("perc.other"), "root-manifest package: " + names);
    assertEquals(2, names.size());
  }

  @Test
  void materializeFromClasspath_jarContainer_copiesOnlyModernEntries() throws Exception {
    String manifest = PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME;
    Path jar = tempDir.resolve("synth-prod.jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      // Sample entry locatePackagesContainer pins to this jar.
      writeJarEntry(
          out,
          "Packages/perc.baseWidgets/widgets/percSimpleText/" + manifest,
          "{\"id\":\"percSimpleText\"}");
      // Legacy definition XML must be skipped by the jar copy filter.
      writeJarEntry(out, "Packages/perc.baseWidgets/Definition.xml", "<Widget/>");
    }

    URLClassLoader cl =
        new URLClassLoader(new URL[] {jar.toUri().toURL()}, null);
    try {
      Path dest = tempDir.resolve("mat").resolve("Modern");
      int written = PSModernPackageRootDefaults.materializeFromClasspath(cl, dest);
      assertTrue(written > 0, "expected modern widget files to materialize");

      List<Path> roots = PSModernPackageRootDefaults.discoverPackageRoots(dest);
      assertEquals(1, roots.size());
      assertTrue(roots.get(0).endsWith(Path.of("perc.baseWidgets")));
      assertTrue(
          Files.isRegularFile(
              dest.resolve("perc.baseWidgets")
                  .resolve("widgets")
                  .resolve("percSimpleText")
                  .resolve(manifest)));
      assertFalse(
          Files.exists(dest.resolve("perc.baseWidgets").resolve("Definition.xml")),
          "legacy Definition.xml must not materialize");
    } finally {
      cl.close();
    }
  }

  private static void writeJarEntry(JarOutputStream out, String name, String content)
      throws IOException {
    JarEntry entry = new JarEntry(name);
    out.putNextEntry(entry);
    out.write(content.getBytes(StandardCharsets.UTF_8));
    out.closeEntry();
  }
}
