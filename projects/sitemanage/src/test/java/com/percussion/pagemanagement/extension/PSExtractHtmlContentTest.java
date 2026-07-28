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
package com.percussion.pagemanagement.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Soft-fail contract for FTS HTML extract: render failures must not abort item indexing.
 */
@DisplayName("PSExtractHtmlContent FTS soft-fail")
class PSExtractHtmlContentTest {

  private PSExtractHtmlContent exit;
  private IPSRenderService renderService;
  private IPSGuidManager guidMgr;
  private IPSIdMapper idMapper;
  private IPSPublishingWs publishingWs;
  private IPSRequestContext request;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());

    exit = new PSExtractHtmlContent();
    renderService = mock(IPSRenderService.class);
    guidMgr = mock(IPSGuidManager.class);
    idMapper = mock(IPSIdMapper.class);
    publishingWs = mock(IPSPublishingWs.class);
    request = mock(IPSRequestContext.class);
    guid = mock(IPSGuid.class);

    exit.setRenderService(renderService);
    exit.setGuidMgr(guidMgr);
    exit.setIdMapper(idMapper);
    exit.setPublishingWs(publishingWs);

    when(request.getPrivateObject(IPSConstants.LOAD_FOR_SEARCH_INDEX)).thenReturn(Boolean.TRUE);
    when(guidMgr.makeGuid(anyString(), any(PSTypeEnum.class))).thenReturn(guid);
    when(idMapper.getString(guid)).thenReturn("16777215-101-1");
    when(publishingWs.getItemSites(guid)).thenReturn(List.of(mock(IPSSite.class)));

    PSSubject subject = mock(PSSubject.class);
    when(subject.getName()).thenReturn("Admin");
    when(request.getOriginalSubject()).thenReturn(subject);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void notSearchLoadReturnsEmpty() {
    when(request.getPrivateObject(IPSConstants.LOAD_FOR_SEARCH_INDEX)).thenReturn(Boolean.FALSE);
    assertEquals("", exit.processUdf(new Object[] {"1"}, request));
  }

  @Test
  void missingContentIdReturnsEmpty() {
    assertEquals("", exit.processUdf(new Object[] {}, request));
    assertEquals("", exit.processUdf(null, request));
  }

  @Test
  void pageNotOnSiteReturnsEmpty() {
    when(publishingWs.getItemSites(guid)).thenReturn(Collections.emptyList());
    assertEquals("", exit.processUdf(new Object[] {"1"}, request));
  }

  @Test
  void renderFailureSoftFailsWithEmptyString() {
    when(renderService.renderPageForSearchIndex(anyString()))
        .thenThrow(new RuntimeException("Search-index render rolled back"));
    assertEquals("", exit.processUdf(new Object[] {"1"}, request));
  }

  @Test
  void successfulRenderExtractsTextFromHtml() {
    when(renderService.renderPageForSearchIndex(anyString()))
        .thenReturn("<html><body><p>Hello FTS body</p></body></html>");
    Object result = exit.processUdf(new Object[] {"1"}, request);
    assertTrue(
        result != null && result.toString().toLowerCase().contains("hello fts body"),
        "expected body text in converter output, got: " + result);
  }
}
