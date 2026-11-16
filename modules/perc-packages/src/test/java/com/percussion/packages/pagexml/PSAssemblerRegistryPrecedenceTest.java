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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Template assembler registry precedence (issue #6476 / parent #2626).
 *
 * <p>Pins the fallback <strong>order</strong> — not just individual outcomes — across the four
 * registry surfaces behind template normalization, with the template-variant assembler
 * ({@code pageVariantAssembler}) first-class: the catalog registers it, the compiler normalizes
 * to it, the strict modern-only load resolves it, and the operator diagnostics report it OK.
 *
 * <p>Precedence under test, highest first:
 *
 * <ol>
 *   <li>Compiler normalization ({@link PSPageXmlCompiler#mapAssembler}): blank defaults to the
 *       page assembler; otherwise the extension-path leaf (case-insensitive) normalizes to a known
 *       short id; unknown {@code *Assembler} leaves pass through; anything else gains the {@code
 *       Assembler} suffix.
 *   <li>Lenient upgrade emit ({@link PSPageXmlTemplateDefEmitter#toLegacyAssemblerPath}): blank
 *       defaults to the page assembler path; full paths pass through; registered short ids resolve;
 *       unregistered short ids gain the product extension prefix.
 *   <li>Strict modern-only load ({@link
 *       PSPageXmlTemplateDefEmitter#resolveRegisteredAssemblerPath}): short id, full-path leaf, or
 *       the {@code xslAssembler} alias resolve; everything else (blank, unregistered) throws —
 *       there is no fallback.
 *   <li>Operator diagnostics ({@link PSAssemblerCatalogDiagnostics}): blank is BLOCKED as blank;
 *       registered ids (including the variant assembler) are OK; unregistered full paths are
 *       BLOCKED as emit-path-only; unregistered short ids are BLOCKED as unregistered.
 * </ol>
 */
class PSAssemblerRegistryPrecedenceTest {

  private static final String VARIANT_PATH = "Java/global/percussion/assembly/pageVariantAssembler";
  private static final String PAGE_PATH = "Java/global/percussion/assembly/pageAssembler";

  @Test
  void catalogRegistrationOrder_pinsVariantAssemblerRow() {
    List<PSAssemblerCatalogDiagnostics.CatalogEntry> catalog =
        PSAssemblerCatalogDiagnostics.readCatalog();

    Map<String, String> registered = PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS;
    assertEquals(
        new ArrayList<>(registered.keySet()),
        catalog.stream()
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getId)
            .collect(Collectors.toList()),
        "diagnostics catalog must preserve registry registration order");
    assertEquals(
        VARIANT_PATH,
        catalog.stream()
            .filter(e -> e.getId().equals("pageVariantAssembler"))
            .map(PSAssemblerCatalogDiagnostics.CatalogEntry::getExtensionPath)
            .findFirst()
            .orElseThrow(() -> new AssertionError("pageVariantAssembler missing from registry")));
  }

  @Test
  void strictResolution_variantAssemblerAndAliasPrecedence() {
    // Short id, full extension path, and any casing resolve to the variant path.
    assertEquals(
        VARIANT_PATH,
        PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath("pageVariantAssembler"));
    assertEquals(
        VARIANT_PATH, PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath(VARIANT_PATH));
    assertEquals(
        VARIANT_PATH,
        PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath("PAGEVARIANTASSEMBLER"));

    // The legacy xslAssembler alias resolves ahead of the unregistered branch.
    assertEquals(
        "Java/global/percussion/assembly/legacyAssembler",
        PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath("xslAssembler"));

    // No fallback: blank, unregistered short ids, and unregistered full paths all throw.
    assertThrows(
        IllegalArgumentException.class,
        () -> PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath("  "));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath("siteAssembler"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath(
                "Java/global/percussion/assembly/siteAssembler"));
  }

  @Test
  void lenientEmitFallbackOrder_blankThenPassthroughThenRegisteredThenPrefixed() {
    // Blank defaults to the page assembler path — before any passthrough or prefix rule.
    assertEquals(PAGE_PATH, PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath(null));
    assertEquals(PAGE_PATH, PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("  "));

    // Full paths pass through untouched, even when unregistered.
    assertEquals(
        "Java/global/percussion/assembly/siteAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath(
            "Java/global/percussion/assembly/siteAssembler"));

    // Registered short ids (including the variant assembler) resolve against the catalog.
    assertEquals(
        VARIANT_PATH, PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("pageVariantAssembler"));
    assertEquals(
        "Java/global/percussion/assembly/legacyAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("xslAssembler"));

    // Unregistered short ids gain the product extension prefix (site-local custom assemblers).
    assertEquals(
        "Java/global/percussion/assembly/siteAssembler",
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("siteAssembler"));
    assertEquals(
        VARIANT_PATH,
        PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath("  pageVariantAssembler  "));
  }

  @Test
  void compilerNormalizationFallbackOrder_blankThenLeafThenNormalized() {
    // Blank defaults to the page assembler — before any leaf or suffix rule.
    assertEquals("pageAssembler", PSPageXmlCompiler.mapAssembler(null));
    assertEquals("pageAssembler", PSPageXmlCompiler.mapAssembler("   "));

    // Extension-path leaves normalize case-insensitively, variant assembler included.
    assertEquals(
        "pageVariantAssembler",
        PSPageXmlCompiler.mapAssembler("Java/global/percussion/assembly/pageVariantAssembler"));
    assertEquals(
        "pageVariantAssembler",
        PSPageXmlCompiler.mapAssembler("java/global/percussion/assembly/PAGEVARIANTASSEMBLER"));
    assertEquals(
        "legacyAssembler",
        PSPageXmlCompiler.mapAssembler("Java/global/percussion/assembly/xslAssembler"));

    // Unknown *Assembler leaves pass through; non-suffix values gain the suffix.
    assertEquals(
        "siteAssembler",
        PSPageXmlCompiler.mapAssembler("Java/global/percussion/assembly/siteAssembler"));
    assertEquals("customAssembler", PSPageXmlCompiler.mapAssembler("custom"));
  }

  @Test
  void diagnosticsVerdictPrecedence_variantAssemblerOk() {
    PSAssemblerCatalogDiagnostics.Report variant =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("variant-view", "pageVariantAssembler")));

    assertTrue(variant.isClean());
    PSAssemblerCatalogDiagnostics.Entry entry = variant.getEntries().get(0);
    assertTrue(entry.isOk());
    assertEquals(VARIANT_PATH, entry.getResolvedPath());

    // Blank is BLOCKED as blank — it never inherits the page-assembler default.
    PSAssemblerCatalogDiagnostics.Report blank =
        PSAssemblerCatalogDiagnostics.diagnose(manifest("perc.probe", template("plain", "  ")));
    assertTrue(!blank.isClean());
    assertTrue(blank.getEntries().get(0).getReason().contains("blank assembler"));

    // Unregistered full paths are BLOCKED as emit-path-only, ahead of the generic branch.
    PSAssemblerCatalogDiagnostics.Report fullPath =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest(
                "perc.probe",
                template("custom", "Java/global/percussion/assembly/customAssembler")));
    assertTrue(!fullPath.isClean());
    assertTrue(fullPath.getEntries().get(0).getReason().contains("legacy-upgrade emit path"));

    // Unregistered short ids are BLOCKED as unregistered.
    PSAssemblerCatalogDiagnostics.Report shortId =
        PSAssemblerCatalogDiagnostics.diagnose(
            manifest("perc.probe", template("custom", "customAssembler")));
    assertTrue(!shortId.isClean());
    assertTrue(shortId.getEntries().get(0).getReason().contains("unregistered assembler"));
  }

  @Test
  void endToEndRegistryChain_everyRegisteredAssemblerResolvesToOk() {
    // Full registry chain per row: legacy extension path -> compiler id -> strict path ->
    // diagnostics OK. Guards the fallback order against catalog drift.
    for (Map.Entry<String, String> row :
        PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.entrySet()) {
      String normalized = PSPageXmlCompiler.mapAssembler(row.getValue());
      assertEquals(row.getKey(), normalized, "compiler must normalize " + row.getValue());
      assertEquals(
          row.getValue(),
          PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath(normalized),
          "strict load must resolve " + normalized);

      PSAssemblerCatalogDiagnostics.Report report =
          PSAssemblerCatalogDiagnostics.diagnose(
              manifest("perc.chain", template("t-" + normalized, normalized)));
      assertTrue(report.isClean(), "diagnostics must accept registered id " + normalized);
      assertEquals(row.getValue(), report.getEntries().get(0).getResolvedPath());
    }
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
