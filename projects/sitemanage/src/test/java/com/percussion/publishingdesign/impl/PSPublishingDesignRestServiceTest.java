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
package com.percussion.publishingdesign.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.publishingdesign.data.PSContentListSummary;
import com.percussion.publishingdesign.data.PSEditionSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSPublishingDesignRestServiceTest {

  @Mock private IPSPublisherService publisherService;
  @Mock private IPSGuidManager guidManager;
  @Mock private IPSGuid siteGuid;
  @Mock private IPSGuid editionGuid;
  @Mock private IPSGuid contentListGuid;

  private PSPublishingDesignRestService service;

  @BeforeEach
  void setUp() {
    service = new PSPublishingDesignRestService(publisherService, guidManager);
  }

  @Test
  void listEditionsBySite_happyPath() {
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.SITE))).thenReturn(siteGuid);
    IPSEdition edition = mock(IPSEdition.class);
    when(edition.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(99);
    when(edition.getName()).thenReturn("Full Publish");
    when(edition.getComment()).thenReturn("c");
    when(edition.getPriority()).thenReturn(IPSEdition.Priority.MEDIUM);
    when(publisherService.findAllEditionsBySite(siteGuid))
        .thenReturn(Collections.singletonList(edition));

    List<PSEditionSummary> list = service.listEditionsBySite("42");
    assertEquals(1, list.size());
    assertEquals("Full Publish", list.get(0).getName());
    assertEquals("99", list.get(0).getEditionId());
    assertEquals("42", list.get(0).getSiteId());
  }

  @Test
  void listEditionsBySite_missingSiteId_400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.listEditionsBySite(""));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void getEdition_notFound_404() throws Exception {
    when(guidManager.makeGuid(eq("7"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(publisherService.loadEdition(editionGuid)).thenThrow(new PSNotFoundException("missing"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.getEdition("7"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void getContentList_happyPath() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList cl = mock(IPSContentList.class);
    when(cl.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(5);
    when(cl.getName()).thenReturn("Home Pages");
    when(cl.getDescription()).thenReturn("desc");
    when(cl.isLegacy()).thenReturn(false);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);

    PSContentListSummary s = service.getContentList("5");
    assertEquals("Home Pages", s.getName());
    assertEquals("modern", s.getListType());
  }

  @Test
  void listContentLists_empty() {
    when(publisherService.findAllContentLists("")).thenReturn(List.of());
    assertTrue(service.listContentLists().isEmpty());
  }

  @Test
  void createEdition_requiresNameAndSite() {
    PSEditionSummary body = new PSEditionSummary();
    body.setName("E1");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createEdition(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void createEdition_happyPath() {
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.SITE))).thenReturn(siteGuid);
    IPSEdition edition = mock(IPSEdition.class);
    when(publisherService.createEdition()).thenReturn(edition);
    when(edition.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(11);
    when(edition.getName()).thenReturn("NewEd");
    when(edition.getPriority()).thenReturn(IPSEdition.Priority.MEDIUM);

    PSEditionSummary body = new PSEditionSummary();
    body.setName("NewEd");
    body.setSiteId("42");
    body.setComment("c");

    PSEditionSummary created = service.createEdition(body);
    assertEquals("NewEd", created.getName());
    assertEquals("11", created.getEditionId());
    org.mockito.Mockito.verify(publisherService).saveEdition(edition);
  }

  @Test
  void deleteContentList_notFound_404() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(publisherService.loadContentList(contentListGuid))
        .thenThrow(new PSNotFoundException("missing"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteContentList("5"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void associateContentList_requiresIds() {
    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setContentListId("5");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.associateContentList("1", body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void associateContentList_happyPath() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSGuid ctxGuid = mock(IPSGuid.class);
    when(guidManager.makeGuid(eq("9"), eq(PSTypeEnum.CONTEXT))).thenReturn(ctxGuid);
    when(publisherService.loadEdition(editionGuid)).thenReturn(mock(IPSEdition.class));

    IPSContentList cl = mock(IPSContentList.class);
    when(cl.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(5);
    when(cl.getName()).thenReturn("Home Pages");
    when(cl.isLegacy()).thenReturn(false);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);

    IPSGuid linkId = mock(IPSGuid.class);
    when(linkId.longValue()).thenReturn(99L);
    com.percussion.services.publisher.data.PSEditionContentList link =
        new com.percussion.services.publisher.data.PSEditionContentList(linkId);
    when(publisherService.createEditionContentList()).thenReturn(link);
    when(editionGuid.longValue()).thenReturn(1L);
    when(contentListGuid.longValue()).thenReturn(5L);

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setContentListId("5");
    body.setDeliveryContextId("9");
    body.setSequence(1);

    PSContentListSummary result = service.associateContentList("1", body);
    assertEquals("Home Pages", result.getName());
    org.mockito.Mockito.verify(publisherService).saveEditionContentList(link);
  }

  @Test
  void copyEdition_requiresSourceAndTarget() {
    com.percussion.publishingdesign.data.PSCopyEditionRequest req =
        new com.percussion.publishingdesign.data.PSCopyEditionRequest();
    req.setSourceEditionId("1");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyEdition(req));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void copyEdition_happyPathWithoutContentLists() throws Exception {
    when(guidManager.makeGuid(eq("7"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.SITE))).thenReturn(siteGuid);

    IPSEdition source = mock(IPSEdition.class);
    when(source.getName()).thenReturn("SourceEd");
    when(source.getComment()).thenReturn("c");
    when(source.getPriority()).thenReturn(IPSEdition.Priority.MEDIUM);
    when(publisherService.loadEdition(editionGuid)).thenReturn(source);

    IPSEdition copy = mock(IPSEdition.class);
    when(publisherService.createEdition()).thenReturn(copy);
    when(copy.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(11);
    when(copy.getName()).thenReturn("SourceEd_copy");
    when(copy.getPriority()).thenReturn(IPSEdition.Priority.MEDIUM);

    com.percussion.publishingdesign.data.PSCopyEditionRequest req =
        new com.percussion.publishingdesign.data.PSCopyEditionRequest();
    req.setSourceEditionId("7");
    req.setTargetSiteId("42");
    req.setCopyContentLists(false);

    PSEditionSummary result = service.copyEdition(req);
    assertEquals("SourceEd_copy", result.getName());
    assertEquals("42", result.getSiteId());
    org.mockito.Mockito.verify(publisherService).saveEdition(copy);
    org.mockito.Mockito.verify(publisherService, org.mockito.Mockito.never())
        .loadEditionContentLists(any());
  }

  @Test
  void listDesignSites_requiresSiteManager() {
    // service constructed without site manager
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.listDesignSites());
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  void createContext_requiresName_whenSiteManagerMissing_returns503() {
    com.percussion.publishingdesign.data.PSContextSummary body =
        new com.percussion.publishingdesign.data.PSContextSummary();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createContext(body));
    // Without site manager the service fails closed with 503
    assertEquals(503, ex.getResponse().getStatus());
  }
}
