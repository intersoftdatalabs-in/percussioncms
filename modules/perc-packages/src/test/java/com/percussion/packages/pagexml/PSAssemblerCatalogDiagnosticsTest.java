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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Assembler catalog operator diagnostics readback (issue #6081 / parent #2626).
 *
 * <p>Operators troubleshooting a modern package that will not load need a readable verdict per
 * template — which registered extension path the assembler resolves to, or exactly why it is
 * blocked plus the fix action — rather than a fail-fast stack trace alone.
 */
class PSAssemblerCatalogDiagnosticsTest {

  @Test
  void catalogReadback_matchesRegisteredProductCatalog() {
    List<PSAssemblerCatalogDiagnostics.CatalogEntry> catalog =
        PSAssemblerCatalogDiagnostics.readCatalog();

    Map<String, String> expected = PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS;
    assertEquals(expected.size(), catalog.size());
    assertEquals(
        new ArrayList<>(expected.keySet()),
        catalog.stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList()));
    Map<String, String> readBack =
        catalog.stream()
            .collect(
                Collectors.toMap(
                    PSAssemblerCatalogDiagnostics.CatalogEntry::getId,
                    PSAssemblerCatalogDiagnostics.CatalogEntry::getExtensionPath));
    assertEquals(expected, readBack);
    assertEquals(
        "Java/global/percussion/assembly/htmlAssembler", readBack.get("htmlAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/markdownAssembler", readBack.get("markdownAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/velocityAssembler", readBack.get("velocityAssembler"));
  }

  @Test
  void registeredIds_diagnoseOkWithResolvedPaths() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest(
                "perc.probe",
                template("plain", "htmlAssembler"),
                template("rich", "velocityAssembler")));

    assertEquals(2, report.getTotalCount());
    assertEquals(2, report.getOkCount());
    assertEquals(0, report.getBlockedCount());
    assertTrue(report.isClean());

    PSAssemblerCatalogDiagnostics.Entry plain = report.getEntries().get(0);
    assertEquals("perc.probe", plain.getManifestId());
    assertEquals("plain", plain.getTemplateName());
    assertEquals("htmlAssembler", plain.getDeclaredAssembler());
    assertTrue(plain.isOk());
    assertEquals(
        "Java/global/percussion/assembly/htmlAssembler", plain.getResolvedPath());
    assertNull(plain.getReason());
    assertNull(plain.getFixHint());
  }

  @Test
  void legacyAlias_diagnosesOkWithAliasNote() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("legacy", "xslAssembler")));

    assertTrue(report.isClean());
    PSAssemblerCatalogDiagnostics.Entry entry = report.getEntries().get(0);
    assertTrue(entry.isOk());
    assertEquals(
        "Java/global/percussion/assembly/legacyAssembler", entry.getResolvedPath());
    assertNotNull(entry.getNote());
    assertTrue(entry.getNote().contains("legacyAssembler"));
  }

  @Test
  void blankAssembler_blockedWithFixHint() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(manifest("perc.probe", template("plain", "  ")));

    assertEquals(1, report.getTotalCount());
    assertEquals(0, report.getOkCount());
    assertEquals(1, report.getBlockedCount());
    assertTrue(!report.isClean());

    PSAssemblerCatalogDiagnostics.Entry entry = report.getEntries().get(0);
    assertTrue(!entry.isOk());
    assertNull(entry.getResolvedPath());
    assertNotNull(entry.getReason());
    assertTrue(entry.getReason().contains("blank assembler"));
    assertNotNull(entry.getFixHint());
    assertTrue(entry.getFixHint().contains("htmlAssembler"));
  }

  @Test
  void unregisteredShortId_blockedWithFixHint() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("custom", "customAssembler")));

    assertTrue(!report.isClean());
    PSAssemblerCatalogDiagnostics.Entry entry = report.getEntries().get(0);
    assertTrue(!entry.isOk());
    assertTrue(entry.getReason().contains("unregistered assembler"));
    assertTrue(entry.getReason().contains("customAssembler"));
    assertTrue(entry.getFixHint().contains("registered catalog id"));
  }

  @Test
  void unregisteredFullPath_blockedWithEmitPathHint() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest(
                "perc.probe",
                template("custom", "Java/global/percussion/assembly/customAssembler")));

    assertTrue(!report.isClean());
    PSAssemblerCatalogDiagnostics.Entry entry = report.getEntries().get(0);
    assertTrue(!entry.isOk());
    assertTrue(entry.getReason().contains("legacy-upgrade emit path"));
    assertTrue(entry.getFixHint().contains("short registered catalog id"));
  }

  @Test
  void diagnoseAll_aggregatesManifestsAndSkipsNulls() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnoseAll(
            Arrays.asList(
                manifest("perc.a", template("one", "htmlAssembler")),
                null,
                manifest("perc.b", template("two", "nopeAssembler"))));

    assertEquals(2, report.getTotalCount());
    assertEquals(1, report.getOkCount());
    assertEquals(1, report.getBlockedCount());
    assertEquals("perc.a", report.getEntries().get(0).getManifestId());
    assertEquals("perc.b", report.getEntries().get(1).getManifestId());
    // Every report carries the catalog readback for operator reference.
    assertEquals(
        PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.size(),
        report.getCatalog().size());
  }

  @Test
  void markdownReport_containsCatalogVerdictsAndFixes() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest(
                "perc.probe",
                template("good", "markdownAssembler"),
                template("bad", "nopeAssembler")));

    String markdown = report.toMarkdown();
    assertTrue(markdown.contains("# Assembler catalog diagnostics"));
    assertTrue(
        markdown.contains(
            "| markdownAssembler | Java/global/percussion/assembly/markdownAssembler |"));
    assertTrue(markdown.contains("perc.probe"));
    assertTrue(markdown.contains("OK"));
    assertTrue(markdown.contains("BLOCKED"));
    assertTrue(markdown.contains("Fix:"));
    assertTrue(markdown.contains("Templates: OK=1 BLOCKED=1 Total=2"));
  }

  @Test
  void textReport_singleLinePerTemplateWithSummary() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest(
                "perc.probe",
                template("good", "markdownAssembler"),
                template("bad", "nopeAssembler")));

    String text = report.toText();
    assertTrue(text.startsWith("Assembler catalog diagnostics: ok=1 blocked=1 total=2"));
    assertTrue(text.contains("catalog=["));
    assertTrue(text.contains("markdownAssembler"));
    assertTrue(
        text.contains(
            "OK perc.probe / good -> Java/global/percussion/assembly/markdownAssembler"));
    assertTrue(text.contains("BLOCKED perc.probe / bad : unregistered assembler"));
  }

  @Test
  void manifestWithoutTemplates_vacuouslyClean() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(manifest("perc.empty"));

    assertEquals(0, report.getTotalCount());
    assertTrue(report.isClean());
    assertTrue(!report.getCatalog().isEmpty());
  }

  private static PSComponentPackageManifest manifest(
      String id, PSComponentPackageManifest.TemplateRef... templates) {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId(id);
    manifest.setTemplates(List.of(templates));
    return manifest;
  }

  private static PSComponentPackageManifest.TemplateRef template(String name, String assembler) {
    PSComponentPackageManifest.TemplateRef template = new PSComponentPackageManifest.TemplateRef();
    template.setName(name);
    template.setType("page");
    template.setAssembler(assembler);
    return template;
  }
}
