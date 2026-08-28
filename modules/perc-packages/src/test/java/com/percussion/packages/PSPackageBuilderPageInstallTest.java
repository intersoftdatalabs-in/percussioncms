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

package com.percussion.packages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.pagexml.PSDualShipPageTemplateDefInventory;
import com.percussion.packages.pagexml.PSPageXmlCompiler;
import com.percussion.packages.pagexml.PSPageXmlCompileResult;
import com.percussion.packages.pagexml.PSPageXmlDualShip;
import com.percussion.packages.pagexml.PSPageXmlInstallPolicy;
import com.percussion.packages.pagexml.PSPageXmlModel;
import com.percussion.packages.pagexml.PSPageXmlPackageContext;
import com.percussion.packages.pagexml.PSPageXmlParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Package-build page install: native archive TemplateDefs only; dual-ship materialize removed
 * (issue #3950 / parent #2630).
 *
 * <p>Cross-platform: {@link Path#resolve(String)} / {@link Files}. Zip entry names use {@code /}
 * (ZIP form), not OS separators.
 */
class PSPackageBuilderPageInstallTest {

  private static final String FIXTURE_PLAIN = "/pagexml/perc.base.plain.templateDef";
  private static final String STEM = "perc.base.plain";
  private static final String TEMPLATE_DEF = STEM + ".templateDef";
  private static final String ACL_DEF = TEMPLATE_DEF + ".aclDef";

  @TempDir Path tempDir;

  @AfterEach
  void clearInstallModeSystemProperties() {
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE);
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP);
  }

  @Test
  void nativeMode_stagesArchiveTemplateDefsAndKeepsAcl_noDualShipLog() throws Exception {
    Path packagesDir = tempDir.resolve("packages-native");
    Path pkg = packagesDir.resolve("perc.baseTemplates");
    writeNativePagePackage(pkg);

    Path outputDir = tempDir.resolve("out-native");
    Path buildTemp = tempDir.resolve("tmp-native");
    String log =
        captureStdout(
            () -> new PSPackageBuilder(packagesDir, outputDir, buildTemp).buildAll());

    assertTrue(
        log.contains("native-install page TemplateDefs for perc.baseTemplates: 1 written"),
        "native staging log: " + log);
    assertFalse(
        log.contains(PSDualShipPageTemplateDefInventory.DUAL_SHIP_LOG_MARKER),
        "must not emit dual-ship log: " + log);
    PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipLogLines(List.of(log.split("\\R")));

    Path ppkg = outputDir.resolve("perc.baseTemplates.ppkg");
    assertTrue(Files.isRegularFile(ppkg));
    Set<String> entries = zipEntryNames(ppkg);
    assertTrue(
        entries.contains("TemplateDef-591/" + TEMPLATE_DEF),
        "native archive TemplateDef-591/" + TEMPLATE_DEF + " entries=" + entries);
    assertTrue(
        entries.contains("AclDef-1/" + ACL_DEF),
        "mapping ACL side-car must remain in archive; entries=" + entries);
    assertFalse(entries.contains(TEMPLATE_DEF), "must not dual-ship root " + TEMPLATE_DEF);
    assertFalse(
        Files.isRegularFile(pkg.resolve(TEMPLATE_DEF)),
        "source tree must not gain a dual-ship root templateDef");
  }

  @Test
  void dualShipMode_failsClosed_doesNotMaterialize() throws Exception {
    Path packagesDir = tempDir.resolve("packages-dual");
    Path pkg = packagesDir.resolve("perc.baseTemplates");
    writeModernPageWithMapping(pkg);
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=dual-ship\n",
        StandardCharsets.UTF_8);

    Path outputDir = tempDir.resolve("out-dual");
    Path buildTemp = tempDir.resolve("tmp-dual");
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    PrintStream orig = System.out;
    System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
    try {
      IOException ex =
          assertThrows(
              IOException.class,
              () -> new PSPackageBuilder(packagesDir, outputDir, buildTemp).buildAll());
      String msg = ex.getMessage() == null ? "" : ex.getMessage();
      assertTrue(
          msg.contains("#3675") || msg.contains("#3950") || msg.contains("Dual-ship"),
          "fail-closed message: " + msg);
    } finally {
      System.setOut(orig);
    }
    String log = buf.toString(StandardCharsets.UTF_8);
    assertFalse(
        log.contains(PSDualShipPageTemplateDefInventory.DUAL_SHIP_LOG_MARKER),
        "must not log dual-ship materialize: " + log);
    assertFalse(
        Files.isRegularFile(outputDir.resolve("perc.baseTemplates.ppkg")),
        "fail-closed dual-ship must not write a .ppkg");
    assertFalse(
        Files.isRegularFile(pkg.resolve(TEMPLATE_DEF)),
        "must not materialize root " + TEMPLATE_DEF + " into the source package");
  }

  @Test
  void packageWithoutModernPages_buildsWithoutPageStaging() throws Exception {
    Path packagesDir = tempDir.resolve("packages-empty");
    Path pkg = packagesDir.resolve("perc.NoPages");
    Files.createDirectories(pkg);
    Files.writeString(pkg.resolve("readme.txt"), "no pages\n", StandardCharsets.UTF_8);

    Path outputDir = tempDir.resolve("out-empty");
    Path buildTemp = tempDir.resolve("tmp-empty");
    String log =
        captureStdout(
            () -> new PSPackageBuilder(packagesDir, outputDir, buildTemp).buildAll());

    assertFalse(log.contains(PSDualShipPageTemplateDefInventory.DUAL_SHIP_LOG_MARKER));
    assertFalse(log.contains("native-install page TemplateDefs for perc.NoPages"));
    assertTrue(Files.isRegularFile(outputDir.resolve("perc.NoPages.ppkg")));
  }

  @Test
  void productResponsiveTemplates_nativePackageBuild_archiveAndAcl_noDualShipLog()
      throws Exception {
    Path product = locatePackage("perc.responsiveTemplates");
    if (product == null) {
      System.err.println(
          "WARN: perc.responsiveTemplates not found; skipping product package-build test");
      return;
    }

    Path packagesDir = tempDir.resolve("packages-product");
    Path pkg = packagesDir.resolve("perc.responsiveTemplates");
    copyTree(product, pkg);

    Path outputDir = tempDir.resolve("out-product");
    Path buildTemp = tempDir.resolve("tmp-product");
    String log =
        captureStdout(
            () -> new PSPackageBuilder(packagesDir, outputDir, buildTemp).buildAll());

    assertTrue(
        log.contains("native-install page TemplateDefs for perc.responsiveTemplates: 3 written"),
        "product native log: " + log);
    assertFalse(
        log.contains(PSDualShipPageTemplateDefInventory.DUAL_SHIP_LOG_MARKER),
        "product package-build must have zero dual-ship log lines: " + log);
    PSDualShipPageTemplateDefInventory.assertNoNonWaivedDualShipLogLines(List.of(log.split("\\R")));

    Path ppkg = outputDir.resolve("perc.responsiveTemplates.ppkg");
    assertTrue(Files.isRegularFile(ppkg));
    Set<String> entries = zipEntryNames(ppkg);
    assertTrue(entries.contains("TemplateDef-597/perc.resp.Banded.templateDef"));
    assertTrue(entries.contains("TemplateDef-599/perc.resp.Basic.templateDef"));
    assertTrue(entries.contains("TemplateDef-627/perc.resp.plain.templateDef"));
    assertTrue(
        entries.contains("AclDef-4472257021923557487/perc.resp.plain.templateDef.aclDef"),
        "plain ACL side-car must remain; entries=" + entries);
    assertFalse(entries.contains("perc.resp.plain.templateDef"));
    assertFalse(entries.contains("perc.resp.Banded.templateDef"));
    assertFalse(entries.contains("perc.resp.Basic.templateDef"));
  }

  private static void writeNativePagePackage(Path pkg) throws Exception {
    writeModernPageWithMapping(pkg);
    Files.writeString(
        pkg.resolve(PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS),
        PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE + "=native\n",
        StandardCharsets.UTF_8);
  }

  private static void writeModernPageWithMapping(Path pkg) throws Exception {
    Files.createDirectories(pkg);
    Files.writeString(
        pkg.resolve("perc.baseTemplates.mapping.properties"),
        TEMPLATE_DEF
            + "=TemplateDef-591\n"
            + ACL_DEF
            + "=AclDef-1\n",
        StandardCharsets.UTF_8);
    Files.writeString(pkg.resolve(ACL_DEF), "<Acl/>\n", StandardCharsets.UTF_8);

    Path pageDir = pkg.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve(STEM);
    Files.createDirectories(pageDir);
    PSPageXmlModel model = PSPageXmlParser.parse(readClasspath(FIXTURE_PLAIN));
    model.setSourceFileName(TEMPLATE_DEF);
    PSPageXmlCompileResult compiled = PSPageXmlCompiler.compile(model, baseTemplatesLikeContext());
    PSPageXmlCompiler.writeArtifacts(compiled, pageDir);
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
    try (var in = PSPackageBuilderPageInstallTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String captureStdout(ThrowingRunnable action) throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    PrintStream orig = System.out;
    System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
    try {
      action.run();
    } finally {
      System.setOut(orig);
    }
    return buf.toString(StandardCharsets.UTF_8);
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

  /** ZIP entry names always use {@code /}, independent of {@code File.separator}. */
  private static Set<String> zipEntryNames(Path zipFile) throws IOException {
    Set<String> names = new LinkedHashSet<>();
    try (InputStream in = Files.newInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        names.add(entry.getName());
      }
    }
    return names;
  }

  private static void copyTree(Path source, Path target) throws IOException {
    try (var walk = Files.walk(source)) {
      walk.forEach(
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
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
