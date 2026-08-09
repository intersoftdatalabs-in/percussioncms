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
package com.percussion.wizard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Behavioral tests for pure helpers on {@link PSWizardDialog}. */
public class PSWizardDialogTest {

  @Test
  public void resolvePageTypeFirstMidLast() {
    assertEquals(IPSWizardDialog.TYPE_FIRST, PSWizardDialog.resolvePageType(0, 3));
    assertEquals(IPSWizardDialog.TYPE_MID, PSWizardDialog.resolvePageType(1, 3));
    assertEquals(IPSWizardDialog.TYPE_LAST, PSWizardDialog.resolvePageType(2, 3));
  }

  @Test
  public void resolvePageTypeSinglePageIsFirstAndLast() {
    // Single page is both first and last; historical isFirst check wins → TYPE_FIRST.
    assertEquals(IPSWizardDialog.TYPE_FIRST, PSWizardDialog.resolvePageType(0, 1));
  }

  @Test
  public void resolvePageTypeRejectsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> PSWizardDialog.resolvePageType(0, 0));
    assertThrows(IllegalArgumentException.class, () -> PSWizardDialog.resolvePageType(-1, 2));
    assertThrows(IllegalArgumentException.class, () -> PSWizardDialog.resolvePageType(2, 2));
  }

  @Test
  public void isValidPageTypeAcceptsTypeConstantsOnly() {
    assertTrue(PSWizardDialog.isValidPageType(IPSWizardDialog.TYPE_FIRST));
    assertTrue(PSWizardDialog.isValidPageType(IPSWizardDialog.TYPE_MID));
    assertTrue(PSWizardDialog.isValidPageType(IPSWizardDialog.TYPE_LAST));
    assertFalse(PSWizardDialog.isValidPageType(-1));
    assertFalse(PSWizardDialog.isValidPageType(99));
  }

  @Test
  public void collectSummaryBodyPreservesHistoricalAppendRules() {
    // Non-final keys with content append with newline; final key never appends.
    List<String> four = Arrays.asList("A", "B", "", "C");
    assertEquals("A\nB\n", PSWizardDialog.collectSummaryBody(four));

    // Skipped (null) middle page does not break has-more-keys rule for earlier page.
    List<String> withSkip = Arrays.asList("A", null, "B", "C");
    assertEquals("A\nB\n", PSWizardDialog.collectSummaryBody(withSkip));

    // Only final page has content → empty body (historical).
    assertEquals("", PSWizardDialog.collectSummaryBody(Arrays.asList("", "", "OnlyLast")));
  }

  @Test
  public void collectSummaryBodyEmptyAndNullSafe() {
    assertEquals("", PSWizardDialog.collectSummaryBody(null));
    assertEquals("", PSWizardDialog.collectSummaryBody(Collections.emptyList()));
  }

  @Test
  public void prependLastPageInstructionJoinsWithBlankLine() {
    assertEquals("Intro\n\nA\n", PSWizardDialog.prependLastPageInstruction("Intro", "A\n"));
    assertEquals("\n\n", PSWizardDialog.prependLastPageInstruction(null, null));
  }

  @Test
  public void collectOrderedSummariesMarksSkippedAsNull() {
    Map<Integer, IPSWizardPanel> pages = new TreeMap<>();
    pages.put(0, stubPanel("s0", "d0"));
    pages.put(1, stubPanel("s1", "d1"));
    pages.put(2, stubPanel("s2", "d2"));

    Map<Integer, IPSWizardPanel> skipped = new TreeMap<>();
    skipped.put(1, pages.get(1));

    List<String> ordered = PSWizardDialog.collectOrderedSummaries(pages, skipped);
    assertEquals(Arrays.asList("s0", null, "s2"), ordered);
  }

  @Test
  public void collectPageDataInIndexOrder() {
    Map<Integer, IPSWizardPanel> pages = new TreeMap<>();
    pages.put(0, stubPanel("a", "d0"));
    pages.put(1, stubPanel("b", "d1"));

    assertArrayEquals(new Object[] {"d0", "d1"}, PSWizardDialog.collectPageData(pages));
    assertThrows(IllegalArgumentException.class, () -> PSWizardDialog.collectPageData(null));
  }

  private static IPSWizardPanel stubPanel(String summary, Object data) {
    return new IPSWizardPanel() {
      @Override
      public void validatePanel() {
        // no-op
      }

      @Override
      public boolean skipNext() {
        return false;
      }

      @Override
      public String getSummary() {
        return summary;
      }

      @Override
      public Object getData() {
        return data;
      }

      @Override
      public void setData(Object d) {
        // no-op
      }

      @Override
      public String getInstruction() {
        return "";
      }

      @Override
      public void setInstruction(String instruction) {
        // no-op
      }
    };
  }
}
