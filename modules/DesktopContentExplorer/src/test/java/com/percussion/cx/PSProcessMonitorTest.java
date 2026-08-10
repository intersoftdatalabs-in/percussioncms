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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Behavioral tests for pure helpers on {@link PSProcessMonitor}. */
public class PSProcessMonitorTest {

  @Test
  public void isValidTotalRequiresPositive() {
    assertFalse(PSProcessMonitor.isValidTotal(0));
    assertFalse(PSProcessMonitor.isValidTotal(-1));
    assertTrue(PSProcessMonitor.isValidTotal(1));
  }

  @Test
  public void isValidStatusAcceptsStatusConstantsOnly() {
    assertTrue(PSProcessMonitor.isValidStatus(PSProcessMonitor.STATUS_INIT));
    assertTrue(PSProcessMonitor.isValidStatus(PSProcessMonitor.STATUS_RUN));
    assertTrue(PSProcessMonitor.isValidStatus(PSProcessMonitor.STATUS_PAUSE));
    assertTrue(PSProcessMonitor.isValidStatus(PSProcessMonitor.STATUS_STOP));
    assertTrue(PSProcessMonitor.isValidStatus(PSProcessMonitor.STATUS_COMPLETE));
    assertFalse(PSProcessMonitor.isValidStatus(0));
    assertFalse(PSProcessMonitor.isValidStatus(PSProcessMonitor.STATUS_COMPLETE + 1));
  }

  @Test
  public void computePercentDoneBoundaries() {
    assertEquals(0, PSProcessMonitor.computePercentDone(1, 4));
    assertEquals(25, PSProcessMonitor.computePercentDone(2, 4));
    assertEquals(50, PSProcessMonitor.computePercentDone(3, 4));
    assertEquals(75, PSProcessMonitor.computePercentDone(4, 4));
    assertEquals(0, PSProcessMonitor.computePercentDone(1, 1));
  }

  @Test
  public void computePercentDoneRejectsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> PSProcessMonitor.computePercentDone(0, 4));
    assertThrows(IllegalArgumentException.class, () -> PSProcessMonitor.computePercentDone(5, 4));
    assertThrows(IllegalArgumentException.class, () -> PSProcessMonitor.computePercentDone(1, 0));
  }
}
