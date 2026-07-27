/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.patch.test;

import static com.percussion.test.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.linkmanagement.service.IPSManagedLinkService;
import org.jsoup.Jsoup;
import org.jsoup.select.Selector;
import org.junit.jupiter.api.Test;

/**
 * Unit test for verifying anchor tag rel attributes for managed links.
 *
 * <p>Selector construction must append attribute filters to {@link IPSManagedLinkService#A_HREF}
 * ({@code "a[href]"}), not another {@code a[...]} segment. Broken concatenation {@code
 * a[href]a[target=...]} is not a valid jsoup query. Fix ported from v8.1.7 PR #716.
 */
public class PSSaveAssetsMainetanceProcessUT {

  /**
   * Selector used by {@code PSSaveAssetsMaintenanceProcess#processLinks} for target=_blank anchors
   * missing the safe rel attribute. Kept in lockstep with production code.
   */
  static final String TARGET_BLANK_UNSAFE_REL_SELECTOR =
      IPSManagedLinkService.A_HREF + "[target=\"_blank\"]" + ":not([rel=\"noopener noreferrer\"])";

  @Test
  void testTarget() {
    var doc = Jsoup.parseBodyFragment("<p>This is <a href=\"#\" target=\"_blank\"/>");
    var targetAnchors = doc.select(TARGET_BLANK_UNSAFE_REL_SELECTOR);

    assertFalse(targetAnchors.isEmpty());

    doc =
        Jsoup.parseBodyFragment(
            "<p>This is <a href=\"#\" target=\"_blank\" rel=\"noopener noreferrer\" />");
    targetAnchors = doc.select(TARGET_BLANK_UNSAFE_REL_SELECTOR);

    assertTrue(targetAnchors.isEmpty());
  }

  @Test
  void brokenConcatenatedSelectorIsInvalid() {
    // Regression: old code used A_HREF + "a[target=...]" producing "a[href]a[target=...]"
    // which jsoup rejects (SelectorParseException) rather than matching anchors.
    var broken =
        IPSManagedLinkService.A_HREF
            + "a[target=\"_blank\"]"
            + ":not(a[rel=\"noopener noreferrer\"])";
    var doc = Jsoup.parseBodyFragment("<p><a href=\"#\" target=\"_blank\">x</a></p>");
    assertThrows(
        Selector.SelectorParseException.class,
        () -> doc.select(broken),
        "Broken concatenated selector must not be a valid jsoup query");
    assertFalse(
        doc.select(TARGET_BLANK_UNSAFE_REL_SELECTOR).isEmpty(),
        "Correct selector must match target=_blank anchors without safe rel");
  }
}
