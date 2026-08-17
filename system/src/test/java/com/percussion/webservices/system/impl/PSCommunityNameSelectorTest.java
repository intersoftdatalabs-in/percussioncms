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

package com.percussion.webservices.system.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.percussion.services.security.data.PSCommunity;
import java.util.List;
import org.junit.jupiter.api.Test;

class PSCommunityNameSelectorTest {

  @Test
  void prefersExactNameWhenLikeAlsoHitsAdminSuffix() {
    PSCommunity corp = named("Corporate_Investments");
    PSCommunity admin = named("Corporate_Investments_Admin");
    assertSame(
        corp, PSCommunityNameSelector.select(List.of(corp, admin), "Corporate_Investments"));
    assertSame(
        admin,
        PSCommunityNameSelector.select(List.of(corp, admin), "Corporate_Investments_Admin"));
  }

  @Test
  void matchesExactNameIgnoringCase() {
    PSCommunity def = named("Default");
    assertSame(def, PSCommunityNameSelector.select(List.of(def), "default"));
  }

  @Test
  void fallsBackToSoleLikeHitWhenNoExactName() {
    PSCommunity only = named("Default");
    assertSame(only, PSCommunityNameSelector.select(List.of(only), "Def"));
  }

  @Test
  void returnsNullWhenAmbiguousAndNoExactMatch() {
    PSCommunity a = named("Enterprise_Investments");
    PSCommunity b = named("Enterprise_Investments_Admin");
    assertNull(PSCommunityNameSelector.select(List.of(a, b), "Enterprise Investments"));
    assertNull(PSCommunityNameSelector.select(List.of(), "Default"));
    assertNull(PSCommunityNameSelector.select(null, "Default"));
    assertNull(PSCommunityNameSelector.select(List.of(a), "  "));
  }

  @Test
  void returnsNullWhenTwoExactNames() {
    PSCommunity a = named("Default");
    PSCommunity b = named("Default");
    assertNull(PSCommunityNameSelector.select(List.of(a, b), "Default"));
  }

  @Test
  void selectedNameIsTheRequestedCommunity() {
    PSCommunity corp = named("Corporate_Investments");
    PSCommunity admin = named("Corporate_Investments_Admin");
    assertEquals(
        "Corporate_Investments",
        PSCommunityNameSelector.select(List.of(admin, corp), "Corporate_Investments")
            .getName());
  }

  private static PSCommunity named(String name) {
    PSCommunity community = new PSCommunity();
    community.setName(name);
    return community;
  }
}
