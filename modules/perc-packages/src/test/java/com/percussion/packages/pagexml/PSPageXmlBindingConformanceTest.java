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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JEXL binding-order conformance of one template (issue #7305 / parent #2626).
 */
class PSPageXmlBindingConformanceTest {

  @Test
  void manifestBindings_orderedUniqueVariablesAreConformant() {
    List<PSComponentPackageManifest.Binding> bindings =
        Arrays.asList(
            manifestBinding("$sys.binary", "$sys.item.getBinary()"),
            manifestBinding("$sys.mimetype", "$sys.item.getMimeType()"),
            manifestBinding("$sys.title", "$sys.item.getTitle()"));

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings("plain", bindings);

    assertTrue(result.isConformant());
    assertEquals(3, result.getBindingCount());
    assertEquals(
        Arrays.asList("$sys.binary", "$sys.mimetype", "$sys.title"), result.getVariablesInOrder());
    assertTrue(result.getDuplicateVariables().isEmpty());
    assertEquals(0, result.getNullEntryCount());
    assertEquals(0, result.getBlankVariableCount());
    assertTrue(result.summarize().contains("conformant"));
  }

  @Test
  void manifestBindings_duplicateVariablesReportedInEncounterOrder() {
    List<PSComponentPackageManifest.Binding> bindings =
        Arrays.asList(
            manifestBinding("$b", "1"),
            manifestBinding("$a", "2"),
            manifestBinding("$b", "3"),
            manifestBinding("$a", "4"),
            manifestBinding("$b", "5"));

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings("dup", bindings);

    assertFalse(result.isConformant());
    assertEquals(5, result.getBindingCount());
    // document order preserved — the helper never sorts
    assertEquals(
        Arrays.asList("$b", "$a", "$b", "$a", "$b"), result.getVariablesInOrder());
    // each duplicate once, in first-duplicate encounter order ($b repeats before $a does)
    assertEquals(Arrays.asList("$b", "$a"), result.getDuplicateVariables());
    assertTrue(result.summarize().contains("NOT conformant"));
    assertTrue(result.summarize().contains("$b"));
  }

  @Test
  void manifestBindings_blankVariablesAndNullEntriesAreDefects() {
    List<PSComponentPackageManifest.Binding> bindings = new ArrayList<>();
    bindings.add(manifestBinding("$sys.title", "expr"));
    bindings.add(manifestBinding("   ", "blank-variable"));
    bindings.add(null);
    bindings.add(manifestBinding("", "empty-variable"));
    bindings.add(manifestBinding(null, "null-variable"));

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings("dirty", bindings);

    assertFalse(result.isConformant());
    assertEquals(3, result.getBlankVariableCount());
    assertEquals(1, result.getNullEntryCount());
    // only the non-blank variable survives in document order
    assertEquals(1, result.getBindingCount());
    assertEquals(List.of("$sys.title"), result.getVariablesInOrder());
  }

  @Test
  void manifestBindings_caseSensitiveVariablesAreDistinct() {
    List<PSComponentPackageManifest.Binding> bindings =
        Arrays.asList(manifestBinding("$Sys.Title", "1"), manifestBinding("$sys.title", "2"));

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings("cased", bindings);

    // JEXL is case-sensitive: different case means different variables
    assertTrue(result.isConformant());
    assertTrue(result.getDuplicateVariables().isEmpty());
  }

  @Test
  void manifestBindings_blankExpressionsAreNotDefects() {
    List<PSComponentPackageManifest.Binding> bindings =
        Arrays.asList(
            manifestBinding("$sys.title", ""), manifestBinding("$sys.body", null));

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings("blank-expr", bindings);

    // blank expressions are normalized by the pipeline ("" / null) — never defects
    assertTrue(result.isConformant());
    assertEquals(2, result.getBindingCount());
  }

  @Test
  void manifestBindings_nullAndEmptyInputsAreVacuouslyConformant() {
    PSPageXmlBindingConformance.Result nullResult =
        PSPageXmlBindingConformance.checkManifestBindings("none", null);
    assertTrue(nullResult.isConformant());
    assertEquals(0, nullResult.getBindingCount());
    assertTrue(nullResult.getVariablesInOrder().isEmpty());

    PSPageXmlBindingConformance.Result emptyResult =
        PSPageXmlBindingConformance.checkManifestBindings("empty", List.of());
    assertTrue(emptyResult.isConformant());
    assertEquals(0, emptyResult.getBindingCount());
  }

  @Test
  void modelBindings_orderPreservedAndDuplicatesDetected() {
    List<PSPageXmlModel.Binding> bindings =
        Arrays.asList(
            modelBinding("$sys.binary", "bin"),
            modelBinding("$sys.mimetype", "mime"),
            modelBinding("$sys.binary", "bin-again"));

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkModelBindings("model", bindings);

    assertFalse(result.isConformant());
    assertEquals(
        Arrays.asList("$sys.binary", "$sys.mimetype", "$sys.binary"),
        result.getVariablesInOrder());
    assertEquals(List.of("$sys.binary"), result.getDuplicateVariables());
    assertEquals("model", result.getTemplateName());
  }

  @Test
  void modelBindings_nullInputIsVacuouslyConformant() {
    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkModelBindings("none", null);

    assertTrue(result.isConformant());
    assertEquals(0, result.getBindingCount());
  }

  private static PSComponentPackageManifest.Binding manifestBinding(
      String variable, String expression) {
    PSComponentPackageManifest.Binding binding = new PSComponentPackageManifest.Binding();
    binding.setVariable(variable);
    binding.setExpression(expression);
    return binding;
  }

  private static PSPageXmlModel.Binding modelBinding(String variable, String expression) {
    PSPageXmlModel.Binding binding = new PSPageXmlModel.Binding();
    binding.setVariable(variable);
    binding.setExpression(expression);
    return binding;
  }
}
