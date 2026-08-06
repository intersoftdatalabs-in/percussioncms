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
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSLocaleLoginSelection}. */
@Tag("UnitTest")
public class PSLocaleLoginSelectionTest {

  @Test
  public void hidesBaseWhenRegionalSiblingsActive() {
    PSLocale es = locale("es", true, PSLocale.STATUS_ACTIVE);
    PSLocale esEs = locale("es-es", false, PSLocale.STATUS_ACTIVE);
    PSLocale esMx = locale("es-mx", false, PSLocale.STATUS_ACTIVE);
    PSLocale ar = locale("ar", true, PSLocale.STATUS_ACTIVE);
    PSLocale enUs = locale("en-us", false, PSLocale.STATUS_ACTIVE);

    List<PSLocale> login =
        PSLocaleLoginSelection.forLoginDropdown(Arrays.asList(enUs, es, esEs, esMx, ar));
    List<String> codes =
        login.stream().map(PSLocale::getLanguageString).collect(Collectors.toList());

    assertTrue(codes.contains("en-us"));
    assertTrue(codes.contains("es-es"));
    assertTrue(codes.contains("es-mx"));
    assertTrue(codes.contains("ar"), "base with no regionals must show");
    assertFalse(codes.contains("es"), "base es hidden when es-* active");
  }

  @Test
  public void showsBaseWhenNoActiveRegionals() {
    PSLocale hi = locale("hi", true, PSLocale.STATUS_ACTIVE);
    PSLocale hiIn = locale("hi-in", false, PSLocale.STATUS_INACTIVE);

    List<PSLocale> login = PSLocaleLoginSelection.forLoginDropdown(Arrays.asList(hi, hiIn));
    List<String> codes =
        login.stream().map(PSLocale::getLanguageString).collect(Collectors.toList());

    assertTrue(codes.contains("hi"), "inactive regional must not hide base");
    assertFalse(codes.contains("hi-in"));
  }

  @Test
  public void hidesBaseWhenRegionalSiblingActive_hi() {
    PSLocale hi = locale("hi", true, PSLocale.STATUS_ACTIVE);
    PSLocale hiIn = locale("hi-in", false, PSLocale.STATUS_ACTIVE);

    List<PSLocale> login = PSLocaleLoginSelection.forLoginDropdown(Arrays.asList(hi, hiIn));
    List<String> codes =
        login.stream().map(PSLocale::getLanguageString).collect(Collectors.toList());

    assertFalse(codes.contains("hi"));
    assertTrue(codes.contains("hi-in"));
  }

  @Test
  public void omitsInactiveLocales() {
    PSLocale ar = locale("ar", true, PSLocale.STATUS_INACTIVE);
    PSLocale enUs = locale("en-us", false, PSLocale.STATUS_ACTIVE);

    List<PSLocale> login = PSLocaleLoginSelection.forLoginDropdown(Arrays.asList(ar, enUs));
    assertEquals(1, login.size());
    assertEquals("en-us", login.get(0).getLanguageString());
  }

  @Test
  public void resolveSelectedPrefersRequestedWhenAllowed() {
    List<PSLocale> login =
        Arrays.asList(
            locale("en-us", false, PSLocale.STATUS_ACTIVE),
            locale("ar", true, PSLocale.STATUS_ACTIVE));
    assertEquals("ar", PSLocaleLoginSelection.resolveSelectedLocale("ar", "en-us", login));
  }

  @Test
  public void resolveSelectedFallsBackToEnUs() {
    List<PSLocale> login =
        Arrays.asList(
            locale("en-us", false, PSLocale.STATUS_ACTIVE),
            locale("de-de", false, PSLocale.STATUS_ACTIVE));
    assertEquals("en-us", PSLocaleLoginSelection.resolveSelectedLocale(null, null, login));
    assertEquals("en-us", PSLocaleLoginSelection.resolveSelectedLocale("xx-yy", "yy-zz", login));
  }

  @Test
  public void resolveSelectedUsesSystemWhenRequestedMissing() {
    List<PSLocale> login =
        Arrays.asList(
            locale("en-us", false, PSLocale.STATUS_ACTIVE),
            locale("de-de", false, PSLocale.STATUS_ACTIVE));
    assertEquals("de-de", PSLocaleLoginSelection.resolveSelectedLocale(null, "de-de", login));
  }

  private static PSLocale locale(String code, boolean base, int status) {
    PSLocale loc = new PSLocale(code, code, null, status);
    loc.setBaseLocale(base);
    return loc;
  }
}
