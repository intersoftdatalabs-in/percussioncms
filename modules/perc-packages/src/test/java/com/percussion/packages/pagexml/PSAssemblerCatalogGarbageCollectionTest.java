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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Assembler catalog stale-entry garbage collection (issue #6267 / parent #2626).
 *
 * <p>After template migration, registered catalog rows that no migrated definition references
 * anymore are dead registrations. The collection pass ({@link
 * PSAssemblerCatalogDiagnostics.Report#collectStaleCatalogEntries()} + {@link
 * PSAssemblerCatalogDiagnostics.Report#withoutStaleCatalogEntries()}) must remove
 * registered-but-unreferenced rows while keeping every row at least one diagnosed template
 * resolves to — including case variants and the {@code xslAssembler → legacyAssembler} alias —
 * so operators can retire dead registrations without breaking loadable templates.
 */
class PSAssemblerCatalogGarbageCollectionTest {

  @Test
  void staleScan_listsUnreferencedRowsInRegistrationOrder() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("plain", "htmlAssembler")));

    List<PSAssemblerCatalogDiagnostics.CatalogEntry> stale =
        report.collectStaleCatalogEntries();

    List<String> expectedStaleIds = new ArrayList<>(registeredIds());
    expectedStaleIds.remove("htmlAssembler");
    assertEquals(
        expectedStaleIds,
        stale.stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList()));
    assertEquals(registeredIds().size() - 1, report.getStaleCount());
  }

  @Test
  void collectionPass_removesStaleRowsAndKeepsReferencedRows() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnoseAll(
            Arrays.asList(
                manifest("perc.a", template("plain", "htmlAssembler")),
                manifest("perc.b", template("rich", "velocityAssembler"))));

    PSAssemblerCatalogDiagnostics.Report pruned = report.withoutStaleCatalogEntries();

    // Pruned catalog keeps registration order (velocityAssembler registers before
    // htmlAssembler), not diagnosis order.
    assertEquals(
        List.of("velocityAssembler", "htmlAssembler"),
        pruned.getCatalog().stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList()));
    // The pass only retires registrations: per-template verdicts are unchanged.
    assertEquals(report.getTotalCount(), pruned.getTotalCount());
    assertEquals(report.getOkCount(), pruned.getOkCount());
    assertEquals(report.getBlockedCount(), pruned.getBlockedCount());
    // Nothing left to collect on the pruned report.
    assertTrue(pruned.collectStaleCatalogEntries().isEmpty());
    assertEquals(0, pruned.getStaleCount());
  }

  @Test
  void aliasReference_keepsLegacyAssemblerRow() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("legacy", "xslAssembler")));

    List<String> staleIds =
        report.collectStaleCatalogEntries().stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList());
    assertTrue(!staleIds.contains("legacyAssembler"));
    assertTrue(
        report.withoutStaleCatalogEntries().getCatalog().stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList())
            .contains("legacyAssembler"));
  }

  @Test
  void caseVariantReference_keepsCanonicalRow() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("plain", "HTMLASSEMBLER")));

    assertTrue(report.isClean());
    assertTrue(
        report.collectStaleCatalogEntries().stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList())
            .stream()
            .noneMatch("htmlAssembler"::equals));
  }

  @Test
  void fullyReferencedCatalog_nothingStale() {
    List<PSComponentPackageManifest.TemplateRef> templates = new ArrayList<>();
    int index = 0;
    for (String id : registeredIds()) {
      templates.add(template("template-" + (index++), id));
    }
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(manifest("perc.full", templates));

    assertTrue(report.isClean());
    assertTrue(report.collectStaleCatalogEntries().isEmpty());
    assertEquals(0, report.getStaleCount());
    assertEquals(
        registeredIds().size(), report.withoutStaleCatalogEntries().getCatalog().size());
  }

  @Test
  void blockedDeclarations_referenceNothing() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnoseAll(
            Arrays.asList(
                manifest("perc.a", template("blank", "  ")),
                manifest("perc.b", template("custom", "customAssembler")),
                manifest(
                    "perc.c",
                    template(
                        "pathed",
                        "Java/global/percussion/assembly/customAssembler"))));

    assertEquals(3, report.getBlockedCount());
    // Blocked declarations never resolve to a catalog row, so every row is stale.
    assertEquals(registeredIds().size(), report.getStaleCount());
    assertTrue(report.withoutStaleCatalogEntries().getCatalog().isEmpty());
  }

  @Test
  void emptyManifests_everyRowStale() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(manifest("perc.empty"));

    assertTrue(report.isClean());
    assertEquals(registeredIds().size(), report.getStaleCount());
  }

  @Test
  void renderings_exposeStaleCounts() {
    PSAssemblerCatalogDiagnostics.Report report =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("plain", "htmlAssembler")));

    String markdown = report.toMarkdown();
    assertTrue(markdown.contains("Stale=" + (registeredIds().size() - 1)));
    assertTrue(markdown.contains("Stale catalog entries"));
    assertTrue(markdown.contains("velocityAssembler"));

    String text = report.toText();
    assertTrue(text.contains("stale=" + (registeredIds().size() - 1)));
  }

  private static List<String> registeredIds() {
    return new ArrayList<>(PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.keySet());
  }

  private static PSComponentPackageManifest manifest(
      String id, PSComponentPackageManifest.TemplateRef... templates) {
    return manifest(id, List.of(templates));
  }

  private static PSComponentPackageManifest manifest(
      String id, List<PSComponentPackageManifest.TemplateRef> templates) {
    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setId(id);
    manifest.setTemplates(templates);
    return manifest;
  }

  private static PSComponentPackageManifest.TemplateRef template(String name, String assembler) {
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName(name);
    template.setType("page");
    template.setAssembler(assembler);
    return template;
  }
}
