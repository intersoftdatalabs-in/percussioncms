/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.i18n.PSLocale;
import com.percussion.rest.locales.LocaleSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class LocalesAdaptorDesignWsTest {

  @Test
  void listLocales_usesFindAndLoadOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 1L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);

    PSLocale loc = new PSLocale();
    loc.setLanguageString("en-us");
    loc.setDisplayName("English");
    loc.setLocaleId(1);

    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    LocalesAdaptor adaptor = new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of);
    List<LocaleSummary> list = adaptor.listLocales(null);

    assertEquals(1, list.size());
    assertEquals("en-us", list.get(0).getLanguageString());
    verify(designWs).findLocales(null, null);
    verify(designWs).loadLocales(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void listLocales_emptyFind_skipsLoad() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of());

    LocalesAdaptor adaptor = new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of);
    assertTrue(adaptor.listLocales(null).isEmpty());
    verify(designWs).findLocales(null, null);
  }
}
