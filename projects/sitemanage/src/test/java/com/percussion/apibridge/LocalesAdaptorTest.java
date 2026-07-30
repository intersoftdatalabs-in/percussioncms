/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.i18n.PSLocale;
import com.percussion.i18n.PSLocaleFormat;
import com.percussion.rest.locales.LocaleDetail;
import com.percussion.rest.locales.LocaleSummary;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class LocalesAdaptorTest {

  @Test
  void mapSummaries_mapsBaseFlagStatusAndFormatPresence() {
    PSLocale en = mock(PSLocale.class);
    when(en.getLocaleId()).thenReturn(1);
    when(en.getLanguageString()).thenReturn("en-us");
    when(en.getDisplayName()).thenReturn("English");
    when(en.getDescription()).thenReturn("US English");
    when(en.getStatus()).thenReturn(PSLocale.STATUS_ACTIVE);
    when(en.isBaseLocale()).thenReturn(false);

    PSLocale ar = mock(PSLocale.class);
    when(ar.getLocaleId()).thenReturn(2);
    when(ar.getLanguageString()).thenReturn("ar");
    when(ar.getDisplayName()).thenReturn("Arabic");
    when(ar.getDescription()).thenReturn(null);
    when(ar.getStatus()).thenReturn(PSLocale.STATUS_ACTIVE);
    when(ar.isBaseLocale()).thenReturn(true);

    List<LocaleSummary> out = LocalesAdaptor.mapSummaries(List.of(en, ar), Set.of("en-us"));
    assertEquals(2, out.size());
    assertEquals("ar", out.get(0).getLanguageString());
    assertEquals(Boolean.TRUE, out.get(0).getBaseLocale());
    assertEquals(Boolean.FALSE, out.get(0).getHasFormatProfile());
    assertEquals("en-us", out.get(1).getLanguageString());
    assertEquals(Boolean.TRUE, out.get(1).getHasFormatProfile());
    assertEquals("active", out.get(1).getStatus());
  }

  @Test
  void toDetail_includesFormatAndDesignGaps() {
    PSLocale en = mock(PSLocale.class);
    when(en.getLocaleId()).thenReturn(1);
    when(en.getLanguageString()).thenReturn("en-us");
    when(en.getDisplayName()).thenReturn("English");
    when(en.getDescription()).thenReturn("desc");
    when(en.getStatus()).thenReturn(PSLocale.STATUS_ACTIVE);
    when(en.isBaseLocale()).thenReturn(false);

    PSLocaleFormat fmt = mock(PSLocaleFormat.class);
    when(fmt.getLanguageString()).thenReturn("en-us");
    when(fmt.getTextDir()).thenReturn(PSLocaleFormat.TEXT_DIR_LTR);
    when(fmt.getDatePattern()).thenReturn("MM/dd/yyyy");
    when(fmt.getCurrencyCode()).thenReturn("USD");

    LocaleDetail d = LocalesAdaptor.toDetail(en, fmt);
    assertEquals("en-us", d.getLanguageString());
    assertEquals(Boolean.TRUE, d.getHasFormatProfile());
    assertNotNull(d.getFormat());
    assertEquals("ltr", d.getFormat().getTextDir());
    assertEquals("MM/dd/yyyy", d.getFormat().getDatePattern());
    assertNotNull(d.getDesignGaps());
    assertFalse(d.getDesignGaps().isEmpty());
  }

  @Test
  void resolveLocale_normalizesLanguageStringAndAcceptsId() {
    PSLocale en = mock(PSLocale.class);
    when(en.getLocaleId()).thenReturn(7);
    when(en.getLanguageString()).thenReturn("en-us");
    List<PSLocale> all = List.of(en);

    assertEquals(en, LocalesAdaptor.resolveLocale(all, "7"));
    assertEquals(en, LocalesAdaptor.resolveLocale(all, "EN_US"));
    assertEquals(en, LocalesAdaptor.resolveLocale(all, "en-us"));
    assertNull(LocalesAdaptor.resolveLocale(all, "fr-fr"));
  }

  @Test
  void normalizeLanguageString_matchesFormatKeying() {
    assertEquals("en-us", LocalesAdaptor.normalizeLanguageString("EN_US"));
    assertEquals("ar", LocalesAdaptor.normalizeLanguageString(" ar "));
    assertNull(LocalesAdaptor.normalizeLanguageString("  "));
  }

  @Test
  void isSafeLocaleKey_rejectsPathTraversal() {
    assertTrue(LocalesAdaptor.isSafeLocaleKey("en-us"));
    assertTrue(LocalesAdaptor.isSafeLocaleKey("1"));
    assertFalse(LocalesAdaptor.isSafeLocaleKey("../x"));
    assertFalse(LocalesAdaptor.isSafeLocaleKey("a/b"));
    assertFalse(LocalesAdaptor.isSafeLocaleKey(null));
  }

  @Test
  void mapStatus_knownAndUnknown() {
    assertEquals("active", LocalesAdaptor.mapStatus(PSLocale.STATUS_ACTIVE));
    assertEquals("inactive", LocalesAdaptor.mapStatus(PSLocale.STATUS_INACTIVE));
    assertEquals("unknown", LocalesAdaptor.mapStatus(-5));
  }
}
