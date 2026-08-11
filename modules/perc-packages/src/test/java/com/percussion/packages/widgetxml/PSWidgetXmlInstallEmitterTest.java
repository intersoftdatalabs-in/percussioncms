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

package com.percussion.packages.widgetxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for modern → install Widget XML materialization (ship-exit #2883 / #2884 / #2885).
 */
class PSWidgetXmlInstallEmitterTest {

  @TempDir Path tempDir;

  @Test
  void emitFromModernSimpleText_roundTripsCoreFieldsAndTemplate() throws Exception {
    // Compile upgrade-input fixture → modern, then reverse-emit install XML → recompile.
    Path fixture = tempDir.resolve("percSimpleText.xml");
    try (var in =
        PSWidgetXmlInstallEmitterTest.class.getResourceAsStream("/widgetxml/percSimpleText.xml")) {
      assertNotNull(in);
      Files.write(fixture, in.readAllBytes());
    }

    PSWidgetXmlCompileResult modern =
        PSWidgetXmlCompiler.compile(fixture, null);
    PSComponentPackageManifestValidator.validate(modern.getManifest());

    PSWidgetXmlModel installModel = PSWidgetXmlInstallEmitter.modelFromModern(modern);
    String xml = PSWidgetXmlInstallEmitter.emitWidgetXml(installModel);
    assertTrue(xml.contains("<WidgetPrefs"));
    assertTrue(xml.contains("title=\"Simple Text\""));
    assertTrue(xml.contains("contenttype_name=\"percSimpleTextAsset\""));
    assertTrue(xml.contains("<CssPref"));
    assertTrue(xml.contains("<Code type=\"jexl\">"));
    assertTrue(xml.contains("<Content type=\"velocity\">"));

    // Stem is derived from the source file name (must match modern component id).
    Path emitted = tempDir.resolve("percSimpleText.xml");
    Files.writeString(emitted, xml, StandardCharsets.UTF_8);
    PSWidgetXmlCompileResult recompiled = PSWidgetXmlCompiler.compile(emitted, null);
    PSComponentPackageManifestValidator.validate(recompiled.getManifest());

    assertEquals(modern.getManifest().getId(), recompiled.getManifest().getId());
    assertEquals(modern.getManifest().getName(), recompiled.getManifest().getName());
    assertEquals(
        modern.getManifest().getContentTypes().get(0).getName(),
        recompiled.getManifest().getContentTypes().get(0).getName());
    assertEquals(
        modern.getManifest().getCssPreferences().size(),
        recompiled.getManifest().getCssPreferences().size());

    String templateKey = modern.getManifest().getTemplates().get(0).getSourceRef();
    assertEquals(
        normalize(modern.getTextArtifacts().get(templateKey)),
        normalize(recompiled.getTextArtifacts().get(templateKey)));
  }

  @Test
  void materializeInstall_skipsWhenCommittedXmlPresent() throws Exception {
    Path pkg = tempDir.resolve("dualShipPkg");
    Path widgetsXml = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(widgetsXml);
    Files.writeString(widgetsXml.resolve("keep.xml"), "<Widget/>", StandardCharsets.UTF_8);

    Path modern =
        pkg.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME).resolve("keep");
    Files.createDirectories(modern);
    Files.writeString(
        modern.resolve("component-package.json"),
        """
        {"schemaVersion":"1.0","id":"keep","name":"Keep","version":"1.0.0",
         "catalog":{"kind":"component","title":"Keep"},
         "templates":[{"name":"keepSnippet","type":"snippet","assembler":"velocityAssembler",
           "sourceRef":"templates/keepSnippet.vm","bindings":[]}],
         "contentTypes":[],"slots":[],"resources":[],"userPreferences":[],"cssPreferences":[]}
        """,
        StandardCharsets.UTF_8);
    Files.createDirectories(modern.resolve("templates"));
    Files.writeString(
        modern.resolve("templates").resolve("keepSnippet.vm"),
        "#loadRelatedWidgetContents()",
        StandardCharsets.UTF_8);

    assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(pkg));
    assertEquals(0, PSWidgetXmlInstallEmitter.materializeInstallWidgetXml(pkg));
  }

  @Test
  void materializeInstall_writesWhenModernOnly() throws Exception {
    Path pkg = tempDir.resolve("modernOnly");
    // Seed modern package by compiling fixture then writing artifacts.
    // File name must be the widget stem so component id is percSimpleText.
    Path fixture = tempDir.resolve("percSimpleText.xml");
    try (var in =
        PSWidgetXmlInstallEmitterTest.class.getResourceAsStream("/widgetxml/percSimpleText.xml")) {
      assertNotNull(in);
      Files.write(fixture, in.readAllBytes());
    }
    PSWidgetXmlCompileResult modern = PSWidgetXmlCompiler.compile(fixture, null);
    Path modernRoot =
        pkg.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME).resolve("percSimpleText");
    PSWidgetXmlCompiler.writeArtifacts(modern, modernRoot);

    assertFalse(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(pkg));
    int written = PSWidgetXmlInstallEmitter.materializeInstallWidgetXml(pkg);
    assertEquals(1, written);
    assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(pkg));
    Path xml =
        PSWidgetXmlPackageCompiler.resolveWidgetsDir(pkg).resolve("percSimpleText.xml");
    assertTrue(Files.isRegularFile(xml));
    assertTrue(Files.readString(xml, StandardCharsets.UTF_8).contains("Simple Text"));
  }

  @Test
  void toInstallHref_prefixesSlash() {
    assertEquals(
        "/rx_resources/widgets/x.png",
        PSWidgetXmlInstallEmitter.toInstallHref("rx_resources/widgets/x.png"));
    assertEquals(
        "/rx_resources/widgets/x.png",
        PSWidgetXmlInstallEmitter.toInstallHref("/rx_resources/widgets/x.png"));
  }

  private static String normalize(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }
}
