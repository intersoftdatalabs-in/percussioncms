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

package com.percussion.packages.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Native page install packaging parity (issue #2806 / parent #2630).
 *
 * <p>When dual-ship is off, modern {@code pages/} sources stage directly into {@code
 * TemplateDef-N/} archive folders with the same XML/GUID semantics as dual-ship root materialization.
 */
class PSPageXmlNativeInstallTest {

  private static final String FIXTURE_PLAIN = "/pagexml/perc.base.plain.templateDef";

  @TempDir Path tempDir;

  @AfterEach
  void clearSysProps() {
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE);
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP);
  }

  @Test
  void policy_defaultIsDualShip() {
    assertEquals(PSPageXmlInstallMode.DUAL_SHIP, PSPageXmlInstallPolicy.resolve(null));
    assertTrue(PSPageXmlInstallPolicy.isDualShipEnabled(null));
    assertFalse(PSPageXmlInstallPolicy.isNativeInstallEnabled(null));
  }

  @Test
  void policy_packageLocalNativeWinsWhenNoSysProp() throws Exception {
    Path pkg = tempDir.resolve("pkg-native");
    Files.createDirectories(pkg);
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=native\n",
        StandardCharsets.UTF_8);
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pkg));
    assertTrue(PSPageXmlInstallPolicy.isNativeInstallEnabled(pkg));
    assertFalse(PSPageXmlInstallPolicy.isDualShipEnabled(pkg));
  }

  @Test
  void policy_sysPropInstallModeOverridesPackageLocal() throws Exception {
    Path pkg = tempDir.resolve("pkg-override");
    Files.createDirectories(pkg);
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=native\n",
        StandardCharsets.UTF_8);
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE, "dual-ship");
    assertEquals(PSPageXmlInstallMode.DUAL_SHIP, PSPageXmlInstallPolicy.resolve(pkg));
  }

  @Test
  void policy_dualShipFalseSysPropForcesNative() {
    System.setProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP, "false");
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(null));
  }

  @Test
  void stageArchive_fromModernPages_writesTemplateDefFolders() throws Exception {
    Path packageDir = tempDir.resolve("perc.baseTemplates");
    Files.createDirectories(packageDir);
    Files.writeString(
        packageDir.resolve("perc.baseTemplates.mapping.properties"),
        "perc.base.plain.templateDef=TemplateDef-591\n",
        StandardCharsets.UTF_8);

    Path pageDir = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve("perc.base.plain");
    Files.createDirectories(pageDir);
    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled =
        PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    Path archiveRoot = tempDir.resolve("archive");
    Files.createDirectories(archiveRoot);

    int n = PSPageXmlNativeInstall.stageArchiveTemplateDefs(packageDir, archiveRoot);
    assertEquals(1, n);

    // Native mode must NOT require dual-ship roots
    assertFalse(Files.isRegularFile(packageDir.resolve("perc.base.plain.templateDef")));

    Path staged =
        archiveRoot.resolve("TemplateDef-591").resolve("perc.base.plain.templateDef");
    assertTrue(Files.isRegularFile(staged), "archive TemplateDef folder layout");

    PSPageXmlModel install = PSPageXmlParser.parse(staged);
    assertEquals("0-4-591", install.getGuid());
    assertEquals("perc.base.plain", install.getName());
    assertEquals(
        normalizeNewlines(model.getTemplateBody()),
        normalizeNewlines(install.getTemplateBody()));
  }

  @Test
  void nativeAndDualShip_emitIdenticalInstallXml() throws Exception {
    Path packageDir = tempDir.resolve("parity");
    Files.createDirectories(packageDir);
    Files.writeString(
        packageDir.resolve("parity.mapping.properties"),
        "perc.base.plain.templateDef=TemplateDef-591\n",
        StandardCharsets.UTF_8);

    Path pageDir = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve("perc.base.plain");
    Files.createDirectories(pageDir);
    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled =
        PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    Path dualStaging = tempDir.resolve("dual-copy");
    copyTree(packageDir, dualStaging);
    PSPageXmlDualShip.materializeInstallTemplateDefs(dualStaging);
    String dualXml =
        Files.readString(
            dualStaging.resolve("perc.base.plain.templateDef"), StandardCharsets.UTF_8);

    List<PSPageXmlInstallArtifact> artifacts =
        PSPageXmlNativeInstall.listInstallArtifacts(packageDir);
    assertEquals(1, artifacts.size());
    assertEquals("TemplateDef-591", artifacts.get(0).archiveFolder());
    assertEquals(
        normalizeNewlines(dualXml),
        normalizeNewlines(artifacts.get(0).templateDefXml()),
        "native conversion must match dual-ship root emit");
  }

  @Test
  void productBaseTemplates_nativeInstallMode_archiveParityWithoutDualShipRoots() throws Exception {
    Path product = locatePackage("perc.baseTemplates");
    if (product == null) {
      System.err.println("WARN: perc.baseTemplates not found; skipping native product test");
      return;
    }

    assertEquals(
        PSPageXmlInstallMode.NATIVE,
        PSPageXmlInstallPolicy.resolve(product),
        "baseTemplates must opt into native install (#2806)");
    assertFalse(PSPageXmlInstallPolicy.isDualShipEnabled(product));
    assertTrue(PSPageXmlDualShip.hasModernPageSources(product));
    assertTrue(
        PSPageXmlPackageCompiler.listTemplateDefs(product).isEmpty(),
        "must not author root *.templateDef");

    Path staging = tempDir.resolve("base-native-src");
    Path archive = tempDir.resolve("base-native-archive");
    copyTree(product, staging);
    Files.createDirectories(archive);

    // Simulate dual-ship OFF: do not materialize roots
    assertFalse(Files.isRegularFile(staging.resolve("perc.base.plain.templateDef")));

    int written = PSPageXmlNativeInstall.stageArchiveTemplateDefs(staging, archive);
    assertTrue(written >= 20, "expected ≥20 base layout templates, got " + written);

    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    assertEquals("0-4-591", guids.get("perc.base.plain"));

    Path plain =
        archive.resolve("TemplateDef-591").resolve("perc.base.plain.templateDef");
    assertTrue(Files.isRegularFile(plain));
    PSPageXmlModel install = PSPageXmlParser.parse(plain);
    assertEquals("perc.base.plain", install.getName());
    assertEquals("0-4-591", install.getGuid());
    assertEquals("Java/global/percussion/assembly/pageAssembler", install.getAssembler());
    assertEquals("Page", install.getOutputFormat());

    // Still no dual-ship roots on staging
    assertFalse(Files.isRegularFile(staging.resolve("perc.base.plain.templateDef")));
  }

  @Test
  void productResponsiveTemplates_nativeInstallMode() throws Exception {
    Path product = locatePackage("perc.responsiveTemplates");
    if (product == null) {
      System.err.println(
          "WARN: perc.responsiveTemplates not found; skipping native product test");
      return;
    }

    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(product));

    Path staging = tempDir.resolve("resp-native-src");
    Path archive = tempDir.resolve("resp-native-archive");
    copyTree(product, staging);
    Files.createDirectories(archive);

    int written = PSPageXmlNativeInstall.stageArchiveTemplateDefs(staging, archive);
    assertEquals(3, written);

    // GUID map keys are lower-cased (same as dual-ship); on-disk templateDef keeps product case.
    Map<String, String> guids = PSPageXmlDualShip.loadGuidsFromMapping(staging);
    // GUID map keys are Locale.ROOT lowercase (mixed-case product ids).
    assertEquals("0-4-597", guids.get("perc.resp.banded"));
    assertEquals("0-4-599", guids.get("perc.resp.basic"));
    assertEquals("0-4-627", guids.get("perc.resp.plain"));
    assertTrue(
        Files.isRegularFile(
            archive.resolve("TemplateDef-597").resolve("perc.resp.Banded.templateDef")));
    assertTrue(
        Files.isRegularFile(
            archive.resolve("TemplateDef-599").resolve("perc.resp.Basic.templateDef")));
    assertTrue(
        Files.isRegularFile(
            archive.resolve("TemplateDef-627").resolve("perc.resp.plain.templateDef")));
  }

  @Test
  void missingMapping_failsFast() throws Exception {
    Path packageDir = tempDir.resolve("missing-map");
    Files.createDirectories(packageDir);
    Files.writeString(
        packageDir.resolve("missing-map.mapping.properties"),
        "other.templateDef=TemplateDef-1\n",
        StandardCharsets.UTF_8);

    Path pageDir = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve("perc.base.plain");
    Files.createDirectories(pageDir);
    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName("perc.base.plain.templateDef");
    PSPageXmlCompileResult compiled =
        PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    Path archive = tempDir.resolve("missing-map-archive");
    Files.createDirectories(archive);
    PSPageXmlException ex =
        assertThrows(
            PSPageXmlException.class,
            () -> PSPageXmlNativeInstall.stageArchiveTemplateDefs(packageDir, archive));
    assertTrue(ex.getMessage().contains("Missing stable install mapping"));
    assertTrue(ex.getMessage().contains("perc.base.plain"));
  }

  /**
   * Product packages use mixed-case stems (e.g. {@code perc.base.Box}). Mapping loaders must match
   * dual-ship lower-case stem keys so native install does not fail GUID/folder lookup.
   */
  @Test
  void mixedCaseStem_mappingLookupIsCaseInsensitive() throws Exception {
    Path packageDir = tempDir.resolve("mixed-case-pkg");
    Files.createDirectories(packageDir);
    Files.writeString(
        packageDir.resolve("mixed-case-pkg.mapping.properties"),
        "perc.base.Box.templateDef=TemplateDef-557\n",
        StandardCharsets.UTF_8);

    Path pageDir = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve("perc.base.Box");
    Files.createDirectories(pageDir);
    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName("perc.base.Box.templateDef");
    model.setName("perc.base.Box");
    PSPageXmlCompileResult compiled =
        PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    // Force manifest id to mixed case (product Box template).
    compiled.getManifest().setId("perc.base.Box");
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);

    Map<String, String> folders =
        PSPageXmlNativeInstall.loadArchiveFoldersFromMapping(packageDir);
    assertEquals("TemplateDef-557", folders.get("perc.base.box"));
    assertNull(folders.get("perc.base.Box"), "folders map keys are lower-cased");

    Path archive = tempDir.resolve("mixed-case-archive");
    Files.createDirectories(archive);
    int written = PSPageXmlNativeInstall.stageArchiveTemplateDefs(packageDir, archive);
    assertEquals(1, written);
    assertTrue(
        Files.isRegularFile(
            archive.resolve("TemplateDef-557").resolve("perc.base.Box.templateDef")),
        "archive file keeps original mixed-case stem");
  }

  @Test
  void packageInstallProps_isCopiedAsRootFileInProductTree() throws Exception {
    Path product = locatePackage("perc.baseTemplates");
    if (product == null) {
      return;
    }
    Path props = product.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS);
    assertTrue(Files.isRegularFile(props));
    Properties p = new Properties();
    try (var in = Files.newInputStream(props)) {
      p.load(in);
    }
    assertEquals("native", p.getProperty(PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE));
  }

  private static PSPageXmlPackageContext baseTemplatesLikeContext() {
    PSPageXmlPackageContext ctx = new PSPageXmlPackageContext();
    ctx.setPackageId("perc.baseTemplates");
    ctx.setPackageName("perc.baseTemplates");
    ctx.setVersion("1.1.5");
    ctx.setDescription("Percussion base layout templates.");
    ctx.setPublisherName("Percussion Software Inc.");
    ctx.setPublisherUrl("http://www.percussion.com");
    ctx.setCmsMin("1.0.0");
    ctx.setCmsMax("9.0.0");
    return ctx;
  }

  private static String readClasspath(String resource) throws Exception {
    try (var in = PSPageXmlNativeInstallTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }

  private static Path locatePackage(String packageName) {
    Path candidate = Path.of("src", "main", "resources", "Packages", packageName);
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src")
            .resolve("main")
            .resolve("resources")
            .resolve("Packages")
            .resolve(packageName);
    return Files.isDirectory(alt) ? alt : null;
  }

  private static void copyTree(Path source, Path target) throws Exception {
    Files.walk(source)
        .forEach(
            src -> {
              try {
                Path rel = source.relativize(src);
                Path dst = target.resolve(rel);
                if (Files.isDirectory(src)) {
                  Files.createDirectories(dst);
                } else {
                  Files.createDirectories(dst.getParent());
                  Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }
}
