/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.keywords.KeywordSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Keywords REST adaptor must call content <em>design</em> web service (Workbench path), not only
 * {@code IPSContentService}.
 */
@Tag("UnitTest")
class KeywordsAdaptorDesignWsTest {

  @Test
  void listKeywords_usesFindAndLoadOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 42L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);

    PSKeyword kw = new PSKeyword();
    kw.setLabel("Colors");
    kw.setValue("colors");
    // GUID on PSKeyword is typically set by service; use reflection-free: mock load return only

    when(designWs.findKeywords(isNull())).thenReturn(List.of(sum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    List<KeywordSummary> list = adaptor.listKeywords(null, false);

    assertEquals(1, list.size());
    assertEquals("Colors", list.get(0).getLabel());
    verify(designWs).findKeywords(null);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> ids = ArgumentCaptor.forClass(List.class);
    verify(designWs).loadKeywords(ids.capture(), eq(false), eq(false), any(), any());
    assertEquals(1, ids.getValue().size());
    assertEquals(guid, ids.getValue().get(0));
  }

  @Test
  void listKeywords_emptyFind_returnsEmptyWithoutLoad() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findKeywords(isNull())).thenReturn(Collections.emptyList());

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    List<KeywordSummary> list = adaptor.listKeywords(null, true);

    assertNotNull(list);
    assertTrue(list.isEmpty());
    verify(designWs).findKeywords(null);
  }
}
