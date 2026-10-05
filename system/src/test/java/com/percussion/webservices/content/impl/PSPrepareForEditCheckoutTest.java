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
package com.percussion.webservices.content.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.cms.objectstore.PSComponentSummary;
import org.junit.jupiter.api.Test;

/** Checkout must not use the pre-transition summary after Quick Edit commits (#5246). */
public class PSPrepareForEditCheckoutTest {

  @Test
  void transitionUsesTheReloadedSummary() {
    PSComponentSummary before = new PSComponentSummary();
    PSComponentSummary reloaded = new PSComponentSummary();

    PSComponentSummary chosen =
        PSPrepareForEditCheckout.summaryForCheckout(true, before, reloaded);

    assertSame(reloaded, chosen);
  }

  @Test
  void noTransitionKeepsTheLoadedSummary() {
    PSComponentSummary before = new PSComponentSummary();
    PSComponentSummary reloaded = new PSComponentSummary();

    PSComponentSummary chosen =
        PSPrepareForEditCheckout.summaryForCheckout(false, before, reloaded);

    assertSame(before, chosen);
  }

  @Test
  void checkoutDecisionUsesTheRefreshedSummary() {
    PSComponentSummary loaded = new PSComponentSummary();
    PSComponentSummary refreshed = new PSComponentSummary();

    PSComponentSummary chosen =
        PSPrepareForEditCheckout.summaryForCheckoutDecision(loaded, refreshed);

    assertSame(refreshed, chosen);
  }

  @Test
  void transitionRequiresAReloadedSummary() {
    PSComponentSummary before = new PSComponentSummary();

    assertThrows(
        IllegalArgumentException.class,
        () -> PSPrepareForEditCheckout.summaryForCheckout(true, before, null));
  }

  @Test
  void missingStateIsZeroWhenTheItemAlreadyHasAState() {
    assertEquals(
        0,
        PSPrepareForEditCheckout.missingStateToAssign(
            4,
            7,
            wf -> {
              fail("must not look up when the state is already set");
              return 0;
            }));
  }

  @Test
  void missingStateUsesTheInitialStateWhenUnset() {
    assertEquals(1, PSPrepareForEditCheckout.missingStateToAssign(4, 0, wf -> 1));
  }

  @Test
  void missingStateIsZeroWhenLookupCannotNameAState() {
    assertEquals(0, PSPrepareForEditCheckout.missingStateToAssign(4, 0, wf -> 0));
    assertEquals(0, PSPrepareForEditCheckout.missingStateToAssign(0, 0, wf -> 9));
  }
}
