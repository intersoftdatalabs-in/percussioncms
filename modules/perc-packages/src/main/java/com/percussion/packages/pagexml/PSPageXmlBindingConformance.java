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

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * JEXL binding-order conformance for one template (issue #7305 / parent #2626).
 *
 * <p>The epic normalizes the template model on <strong>ordered JEXL bindings</strong> (ADR-001:
 * JEXL remains the binding language). Every pipeline stage preserves document order — {@code
 * PSPageXmlParser} collects {@code <binding>} children in order, {@code
 * PSPageXmlCompiler.buildBindings} copies them in order (skipping blank variables), {@code
 * PSPageXmlTemplateDefEmitter.appendBindings} emits them in order, and {@code
 * PSPageXmlManifestReadback} rebuilds them in order — but no single named check states the
 * contract. This helper is that check: given the ordered bindings of one template it reports
 * the variables in document order plus every conformance defect (null entries, blank
 * variables, duplicate variables).
 *
 * <p>Rules (mirroring what the pipeline already enforces structurally):
 *
 * <ul>
 *   <li>Order is significant: {@link Result#getVariablesInOrder()} returns the non-blank
 *       variables in document order. The helper never sorts.
 *   <li>Variables are unique per template (exact, case-sensitive — JEXL is case-sensitive).
 *       The first occurrence wins; each repeat is reported once in {@link
 *       Result#getDuplicateVariables()} in first-duplicate encounter order.
 *   <li>Blank variables (null, empty, whitespace-only) are defects: the compiler and readback
 *       both skip them, so a surviving blank means the list bypassed the pipeline.
 *   <li>Null entries are defects.
 *   <li>Expressions may be blank (normalized to {@code ""} by the compiler, {@code null} by
 *       readback) and are never defects.
 *   <li>Null or empty input is vacuously conformant with count 0 (same convention as {@code
 *       PSPageXmlMigrationDryRun.Report.isClean()}).
 * </ul>
 */
public final class PSPageXmlBindingConformance {

  private PSPageXmlBindingConformance() {
    // utility
  }

  /**
   * Check the manifest-level bindings of one template.
   *
   * @param templateName template name used for reporting (may be null/blank)
   * @param bindings ordered manifest bindings (may be null)
   * @return conformance result (never null)
   */
  public static Result checkManifestBindings(
      String templateName, List<PSComponentPackageManifest.Binding> bindings) {
    List<String> variables = new ArrayList<>();
    int nullEntries = 0;
    int blankVariables = 0;
    if (bindings != null) {
      for (PSComponentPackageManifest.Binding binding : bindings) {
        if (binding == null) {
          nullEntries++;
          continue;
        }
        String variable = binding.getVariable();
        if (variable == null || variable.isBlank()) {
          blankVariables++;
          continue;
        }
        variables.add(variable);
      }
    }
    return new Result(templateName, variables, nullEntries, blankVariables);
  }

  /**
   * Check the parser-model bindings of one template.
   *
   * @param templateName template name used for reporting (may be null/blank)
   * @param bindings ordered model bindings (may be null)
   * @return conformance result (never null)
   */
  public static Result checkModelBindings(
      String templateName, List<PSPageXmlModel.Binding> bindings) {
    List<String> variables = new ArrayList<>();
    int nullEntries = 0;
    int blankVariables = 0;
    if (bindings != null) {
      for (PSPageXmlModel.Binding binding : bindings) {
        if (binding == null) {
          nullEntries++;
          continue;
        }
        String variable = binding.getVariable();
        if (variable == null || variable.isBlank()) {
          blankVariables++;
          continue;
        }
        variables.add(variable);
      }
    }
    return new Result(templateName, variables, nullEntries, blankVariables);
  }

  /** Conformance verdict for the ordered bindings of one template. */
  public static final class Result {
    private final String templateName;
    private final List<String> variablesInOrder;
    private final List<String> duplicateVariables;
    private final int nullEntryCount;
    private final int blankVariableCount;

    private Result(
        String templateName, List<String> variablesInOrder, int nullEntryCount, int blankVariableCount) {
      this.templateName = templateName;
      this.variablesInOrder = Collections.unmodifiableList(new ArrayList<>(variablesInOrder));
      this.nullEntryCount = nullEntryCount;
      this.blankVariableCount = blankVariableCount;
      this.duplicateVariables = Collections.unmodifiableList(findDuplicates(variablesInOrder));
    }

    private static List<String> findDuplicates(List<String> variablesInOrder) {
      Set<String> seen = new LinkedHashSet<>();
      Set<String> duplicates = new LinkedHashSet<>();
      for (String variable : variablesInOrder) {
        if (!seen.add(variable)) {
          duplicates.add(variable);
        }
      }
      return new ArrayList<>(duplicates);
    }

    /** Template name as supplied to the check (may be null/blank). */
    public String getTemplateName() {
      return templateName;
    }

    /** Non-blank binding variables in document order (never null, never sorted). */
    public List<String> getVariablesInOrder() {
      return variablesInOrder;
    }

    /**
     * Duplicate variables, each reported once in first-duplicate encounter order (never null).
     */
    public List<String> getDuplicateVariables() {
      return duplicateVariables;
    }

    /** Number of null entries in the checked list. */
    public int getNullEntryCount() {
      return nullEntryCount;
    }

    /** Number of blank (null/empty/whitespace-only) variables in the checked list. */
    public int getBlankVariableCount() {
      return blankVariableCount;
    }

    /** Number of non-blank bindings checked (duplicates included — each entry counts once). */
    public int getBindingCount() {
      return variablesInOrder.size();
    }

    /** True when the list has no null entries, no blank variables, and no duplicates. */
    public boolean isConformant() {
      return nullEntryCount == 0 && blankVariableCount == 0 && duplicateVariables.isEmpty();
    }

    /**
     * One-line operator summary, e.g. {@code template 'plain': 6 bindings, conformant} or
     * {@code template 'plain': 7 bindings, NOT conformant (duplicates: [$sys.binary], blank:
     * 1, null: 0)}.
     *
     * @return single-line summary (never null)
     */
    public String summarize() {
      String name = templateName != null && !templateName.isBlank() ? templateName : "?";
      StringBuilder sb = new StringBuilder();
      sb.append("template '").append(name).append("': ");
      sb.append(getBindingCount()).append(" binding");
      if (getBindingCount() != 1) {
        sb.append("s");
      }
      if (isConformant()) {
        sb.append(", conformant");
      } else {
        sb.append(", NOT conformant (duplicates: ").append(duplicateVariables);
        sb.append(", blank: ").append(blankVariableCount);
        sb.append(", null: ").append(nullEntryCount).append(")");
      }
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Result that)) {
        return false;
      }
      return nullEntryCount == that.nullEntryCount
          && blankVariableCount == that.blankVariableCount
          && Objects.equals(templateName, that.templateName)
          && Objects.equals(variablesInOrder, that.variablesInOrder)
          && Objects.equals(duplicateVariables, that.duplicateVariables);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          templateName, variablesInOrder, duplicateVariables, nullEntryCount, blankVariableCount);
    }

    @Override
    public String toString() {
      return summarize();
    }
  }
}
