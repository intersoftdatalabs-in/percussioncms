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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSLocaleFormatResolver}. */
@Tag("UnitTest")
public class PSLocaleFormatResolverTest {

  @Test
  public void lookupChain_regionalThenBaseThenDefault() {
    List<String> chain = PSLocaleFormatResolver.lookupChain("es-mx");
    assertEquals(List.of("es-mx", "es", "en-us"), chain);
  }

  @Test
  public void resolve_customerLocaleInheritsBaseAndFloor() {
    Map<String, PSLocaleFormat> catalog = new HashMap<>();
    PSLocaleFormat es = new PSLocaleFormat("es");
    es.setTextDir(PSLocaleFormat.TEXT_DIR_LTR);
    es.setCurrencyCode("EUR");
    es.setDatePattern("dd/MM/yyyy");
    catalog.put("es", es);

    // Customer-invented locale with no format row
    PSLocaleFormat resolved = PSLocaleFormatResolver.resolve("es-ar", catalog);
    assertEquals("es-ar", resolved.getLanguageString());
    assertEquals("EUR", resolved.getCurrencyCode());
    assertEquals("dd/MM/yyyy", resolved.getDatePattern());
    // floor fills remaining (e.g. time pattern from en-us)
    assertNotNull(resolved.getTimePattern());
    assertEquals(PSLocaleFormat.TEXT_DIR_LTR, resolved.getTextDir());
  }

  @Test
  public void resolve_arabicUsesRtlFromDefaults() {
    PSLocaleFormat ar = PSLocaleFormatResolver.resolve("ar", PSLocaleFormatDefaults.shipped());
    assertEquals(PSLocaleFormat.TEXT_DIR_RTL, ar.getTextDir());
    assertEquals("dd/MM/yyyy", ar.getDatePattern());
  }

  @Test
  public void resolve_hebrewUsesRtlFromDefaults() {
    PSLocaleFormat he = PSLocaleFormatResolver.resolve("he", PSLocaleFormatDefaults.shipped());
    assertEquals(PSLocaleFormat.TEXT_DIR_RTL, he.getTextDir());
    assertEquals("ILS", he.getCurrencyCode());
    assertEquals(PSLocaleFormat.FIRST_DAY_SUNDAY, he.getFirstDayOfWeek());

    PSLocaleFormat heIl = PSLocaleFormatResolver.resolve("he-il", PSLocaleFormatDefaults.shipped());
    assertEquals(PSLocaleFormat.TEXT_DIR_RTL, heIl.getTextDir());
    assertEquals("ILS", heIl.getCurrencyCode());
    assertEquals("Asia/Jerusalem", heIl.getDefaultTz());
    assertEquals(List.of("he-il", "he", "en-us"), PSLocaleFormatResolver.lookupChain("he-il"));
  }

  @Test
  public void resolve_exactRegionalOverridesBase() {
    Map<String, PSLocaleFormat> catalog = new HashMap<>();
    PSLocaleFormat es = new PSLocaleFormat("es");
    es.setCurrencyCode("EUR");
    catalog.put("es", es);

    PSLocaleFormat esMx = new PSLocaleFormat("es-mx");
    esMx.setCurrencyCode("MXN");
    catalog.put("es-mx", esMx);

    PSLocaleFormat resolved = PSLocaleFormatResolver.resolve("es-mx", catalog);
    assertEquals("MXN", resolved.getCurrencyCode());
  }

  @Test
  public void resolve_blankFallsBackToEnUsFloor() {
    PSLocaleFormat resolved = PSLocaleFormatResolver.resolve(null, Map.of());
    assertEquals("en-us", resolved.getLanguageString());
    assertEquals("USD", resolved.getCurrencyCode());
    assertEquals(PSLocaleFormat.TEXT_DIR_LTR, resolved.getTextDir());
  }

  @Test
  public void normalize_collapsesUnderscoreAndCase() {
    assertEquals("en-us", PSLocaleFormatResolver.normalize("EN_US"));
    assertTrue(PSLocaleFormatResolver.lookupChain("FR_FR").contains("fr-fr"));
  }

  @Test
  public void productDefaults_coverShipMatrix() {
    Map<String, PSLocaleFormat> shipped = PSLocaleFormatDefaults.shipped();
    assertTrue(shipped.containsKey("en-us"));
    assertTrue(shipped.containsKey("fr-fr"));
    assertTrue(shipped.containsKey("ar"));
    assertEquals(PSLocaleFormat.TEXT_DIR_RTL, shipped.get("ar").getTextDir());
    assertTrue(shipped.containsKey("he"));
    assertTrue(shipped.containsKey("he-il"));
    assertEquals(PSLocaleFormat.TEXT_DIR_RTL, shipped.get("he").getTextDir());
    // Base language tags used for TMX storage / format inheritance
    assertTrue(shipped.containsKey("de"));
    assertTrue(shipped.containsKey("fr"));
    assertTrue(shipped.containsKey("it"));
    assertTrue(shipped.containsKey("nl"));
    assertTrue(shipped.containsKey("pt"));
    assertTrue(shipped.containsKey("tr"));
    assertEquals("EUR", shipped.get("de").getCurrencyCode());
    assertEquals("EUR", shipped.get("fr").getCurrencyCode());
  }

  @Test
  public void productDefaults_coverUkrainian() {
    Map<String, PSLocaleFormat> shipped = PSLocaleFormatDefaults.shipped();
    // Base uk — no TZ (regional uk-ua carries the only TZ override)
    assertTrue(shipped.containsKey("uk"));
    PSLocaleFormat uk = shipped.get("uk");
    assertEquals("UAH", uk.getCurrencyCode());
    assertEquals(PSLocaleFormat.TEXT_DIR_LTR, uk.getTextDir());
    assertEquals("dd.MM.yyyy", uk.getDatePattern());
    assertEquals("HH:mm", uk.getTimePattern());
    assertEquals(PSLocaleFormat.FIRST_DAY_MONDAY, uk.getFirstDayOfWeek());
    assertEquals(PSLocaleFormat.MEASUREMENT_METRIC, uk.getMeasurementSystem());
    assertNull(uk.getDefaultTz(), "base uk should not declare a timezone");

    // Regional uk-ua inherits base format and overrides only the timezone
    assertTrue(shipped.containsKey("uk-ua"));
    PSLocaleFormat ukUa = shipped.get("uk-ua");
    assertEquals("UAH", ukUa.getCurrencyCode());
    assertEquals(PSLocaleFormat.TEXT_DIR_LTR, ukUa.getTextDir());
    assertEquals("dd.MM.yyyy", ukUa.getDatePattern());
    assertEquals("HH:mm", ukUa.getTimePattern());
    assertEquals("Europe/Kyiv", ukUa.getDefaultTz());
    assertEquals(PSLocaleFormat.FIRST_DAY_MONDAY, ukUa.getFirstDayOfWeek());

    // Regional inherits from base via the standard lookup chain
    assertEquals(List.of("uk-ua", "uk", "en-us"), PSLocaleFormatResolver.lookupChain("uk-ua"));
  }

  @Test
  public void lookupChain_frenchRegionalFallsBackToBaseFr() {
    assertEquals(List.of("fr-fr", "fr", "en-us"), PSLocaleFormatResolver.lookupChain("fr-fr"));
    assertEquals(List.of("de-de", "de", "en-us"), PSLocaleFormatResolver.lookupChain("de-de"));
    assertEquals(List.of("it-it", "it", "en-us"), PSLocaleFormatResolver.lookupChain("it-it"));
  }

  @Test
  public void resolve_regionalInheritsBaseFormatWhenPartial() {
    Map<String, PSLocaleFormat> catalog = new HashMap<>();
    PSLocaleFormat fr = new PSLocaleFormat("fr");
    fr.setCurrencyCode("EUR");
    fr.setDatePattern("dd/MM/yyyy");
    fr.setTextDir(PSLocaleFormat.TEXT_DIR_LTR);
    catalog.put("fr", fr);

    // Country override only for currency/tz-style fields would win; missing
    // fields inherit from base fr then en-us floor.
    PSLocaleFormat frBe = new PSLocaleFormat("fr-be");
    frBe.setDefaultTz("Europe/Brussels");
    catalog.put("fr-be", frBe);

    PSLocaleFormat resolved = PSLocaleFormatResolver.resolve("fr-be", catalog);
    assertEquals("EUR", resolved.getCurrencyCode());
    assertEquals("dd/MM/yyyy", resolved.getDatePattern());
    assertEquals("Europe/Brussels", resolved.getDefaultTz());
  }
}
