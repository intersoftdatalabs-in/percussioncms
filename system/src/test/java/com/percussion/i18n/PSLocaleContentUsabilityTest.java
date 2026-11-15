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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSLocaleContentUsability}. */
@Tag("UnitTest")
public class PSLocaleContentUsabilityTest {

  @Test
  public void excludesNonUsableActiveLocales() {
    PSLocale enUs = locale("en-us", true, PSLocale.STATUS_ACTIVE);
    PSLocale frFr = locale("fr-fr", true, PSLocale.STATUS_ACTIVE);
    PSLocale sysOnly = locale("sys-only", false, PSLocale.STATUS_ACTIVE);

    List<PSLocale> targets =
        PSLocaleContentUsability.forTranslationTargets(Arrays.asList(enUs, frFr, sysOnly));
    List<String> codes =
        targets.stream().map(PSLocale::getLanguageString).collect(Collectors.toList());

    assertEquals(Arrays.asList("en-us", "fr-fr"), codes);
  }

  @Test
  public void excludesInactiveEvenWhenUsable() {
    PSLocale retired = locale("de-de", true, PSLocale.STATUS_INACTIVE);

    List<PSLocale> targets =
        PSLocaleContentUsability.forTranslationTargets(Collections.singletonList(retired));

    assertTrue(targets.isEmpty());
  }

  @Test
  public void nullInputYieldsEmpty() {
    assertTrue(PSLocaleContentUsability.forTranslationTargets(null).isEmpty());
  }

  @Test
  public void usableDefaultsTrue() {
    PSLocale fresh = new PSLocale("en-us", "English", null, PSLocale.STATUS_ACTIVE);
    assertTrue(fresh.isContentUsable(), "new locales default to content-usable");
    fresh.setContentUsable(false);
    assertFalse(fresh.isContentUsable());
    fresh.setContentUsable(true);
    assertTrue(fresh.isContentUsable());
  }

  private static PSLocale locale(String code, boolean usable, int status) {
    PSLocale loc = new PSLocale(code, code, null, status);
    loc.setContentUsable(usable);
    return loc;
  }
}
