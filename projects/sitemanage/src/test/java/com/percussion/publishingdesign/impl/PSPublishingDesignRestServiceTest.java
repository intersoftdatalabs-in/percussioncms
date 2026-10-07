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
package com.percussion.publishingdesign.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.publishingdesign.data.PSContentListSummary;
import com.percussion.publishingdesign.data.PSCopyContentListRequest;
import com.percussion.publishingdesign.data.PSDeliveryTypeSummary;
import com.percussion.publishingdesign.data.PSEditionSummary;
import com.percussion.publishingdesign.data.PSContextSummary;
import com.percussion.publishingdesign.data.PSLocationSchemeSummary;
import com.percussion.publishingdesign.data.PSSchemeParameter;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.sitemgr.data.PSLocationScheme;
import com.percussion.services.sitemgr.data.PSLocationSchemeParameter;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSDeliveryType;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSEditionContentList;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.data.PSEditionType;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSPublishingDesignRestServiceTest {

  @Mock private IPSPublisherService publisherService;
  @Mock private IPSGuidManager guidManager;
  @Mock private IPSGuid siteGuid;
  @Mock private IPSGuid editionGuid;
  @Mock private IPSGuid contentListGuid;
  @Mock private IPSGuid deliveryTypeGuid;
  @Mock private IPSSiteManager siteManager;
  @Mock private IPSGuid contextGuid;
  @Mock private IPSGuid schemeGuid;

  private PSPublishingDesignRestService service;

  @BeforeEach
  void setUp() {
    service = new PSPublishingDesignRestService(publisherService, guidManager);
    service.setDesignWriteAllowed(() -> true);
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
    assertEquals("42", created.getSiteId());
    InOrder order = inOrder(edition);
    order.verify(edition).setComment("c");
    order.verify(edition).setSiteId(siteGuid);
    order.verify(edition).setName("NewEd");
    verify(publisherService).saveEdition(edition);
  }

  @Test
  void createEdition_blankName_400() {
    PSEditionSummary body = new PSEditionSummary();
    body.setName("   ");
    body.setSiteId("42");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createEdition(body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).createEdition();
    verify(publisherService, never()).saveEdition(any());
  }

  @Test
  void createEdition_nameTooLong_400() {
    PSEditionSummary body = new PSEditionSummary();
    body.setName("N".repeat(PSPublishingDesignRestService.MAX_EDITION_NAME_LENGTH + 1));
    body.setSiteId("42");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createEdition(body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.EDITION_NAME_TOO_LONG));
    verify(publisherService, never()).createEdition();
    verify(publisherService, never()).saveEdition(any());
  }

  @Test
  void createEdition_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSEditionSummary body = new PSEditionSummary();
    body.setName("NewEd");
    body.setSiteId("42");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createEdition(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteEdition_idle_deletes() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    service.setEditionRunningJobId(guid -> 0L);
    IPSEdition edition = mock(IPSEdition.class);
    when(publisherService.loadEdition(editionGuid)).thenReturn(edition);

    service.deleteEdition("11");
    verify(publisherService).deleteEdition(edition);
  }

  @Test
  void deleteEdition_running_409() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    service.setEditionRunningJobId(guid -> 55L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteEdition("11"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.EDITION_IN_USE));
    verify(publisherService, never()).loadEdition(any());
    verify(publisherService, never()).deleteEdition(any());
  }

  @Test
  void deleteEdition_finishedJobStillInMemory_deletes() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSRxPublisherService rx = mock(IPSRxPublisherService.class);
    when(rx.getEditionJobId(editionGuid)).thenReturn(55L);
    IPSPublisherJobStatus status = mock(IPSPublisherJobStatus.class);
    when(rx.getPublishingJobStatus(55L)).thenReturn(status);
    when(status.getState()).thenReturn(IPSPublisherJobStatus.State.COMPLETED);
    PSPublishingRuntimeSupport runtime =
        new PSPublishingRuntimeSupport(publisherService, guidManager, rx, null, null);
    PSPublishingDesignRestService wired =
        new PSPublishingDesignRestService(publisherService, guidManager, null, runtime);
    wired.setDesignWriteAllowed(() -> true);
    IPSEdition edition = mock(IPSEdition.class);
    when(publisherService.loadEdition(editionGuid)).thenReturn(edition);

    wired.deleteEdition("11");
    verify(publisherService).deleteEdition(edition);
  }

  @Test
  void deleteEdition_activeJobViaRuntime_409() {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSRxPublisherService rx = mock(IPSRxPublisherService.class);
    when(rx.getEditionJobId(editionGuid)).thenReturn(55L);
    IPSPublisherJobStatus status = mock(IPSPublisherJobStatus.class);
    when(rx.getPublishingJobStatus(55L)).thenReturn(status);
    when(status.getState()).thenReturn(IPSPublisherJobStatus.State.WORKING);
    PSPublishingRuntimeSupport runtime =
        new PSPublishingRuntimeSupport(publisherService, guidManager, rx, null, null);
    PSPublishingDesignRestService wired =
        new PSPublishingDesignRestService(publisherService, guidManager, null, runtime);
    wired.setDesignWriteAllowed(() -> true);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> wired.deleteEdition("11"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.EDITION_IN_USE));
    verify(publisherService, never()).loadEdition(any());
    verify(publisherService, never()).deleteEdition(any());
  }

  @Test
  void deleteEdition_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteEdition("11"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadEdition(any());
    verify(publisherService, never()).deleteEdition(any());
  }

  @Test
  void deleteEdition_blankId_400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteEdition("  "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteEdition(any());
  }

  @Test
  void deleteEdition_missing_404() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(publisherService.loadEdition(editionGuid)).thenThrow(new PSNotFoundException("missing"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteEdition("11"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteEdition(any());
  }

  @Test
  void createEdition_duplicateName_409() {
    IPSEdition existing = mock(IPSEdition.class);
    when(existing.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(7);
    when(publisherService.findEditionByName("DupEd")).thenReturn(existing);

    PSEditionSummary body = new PSEditionSummary();
    body.setName("DupEd");
    body.setSiteId("42");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createEdition(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void updateEdition_duplicateName_409() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSEdition loaded = mock(IPSEdition.class);
    when(publisherService.loadEditionModifiable(editionGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSEdition existing = mock(IPSEdition.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.findEditionByName("Taken")).thenReturn(existing);

    PSEditionSummary body = new PSEditionSummary();
    body.setName("Taken");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateEdition("11", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void updateEdition_nameTooLong_400() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSEdition loaded = mock(IPSEdition.class);
    when(publisherService.loadEditionModifiable(editionGuid)).thenReturn(loaded);

    PSEditionSummary body = new PSEditionSummary();
    body.setName("N".repeat(PSPublishingDesignRestService.MAX_EDITION_NAME_LENGTH + 1));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateEdition("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).findEditionByName(any());
    verify(publisherService, never()).saveEdition(any());
  }

  @Test
  void updateEdition_priorityOnly_setsPriorityAndLeavesNameCommentAndLists() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSEdition loaded = mock(IPSEdition.class);
    when(publisherService.loadEditionModifiable(editionGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(11);
    when(loaded.getName()).thenReturn("NightEd");
    when(loaded.getComment()).thenReturn("keep-me");
    when(loaded.getSiteId()).thenReturn(null);
    AtomicReference<IPSEdition.Priority> stored = new AtomicReference<>();
    doAnswer(
            invocation -> {
              stored.set(invocation.getArgument(0));
              return null;
            })
        .when(loaded)
        .setPriority(any(IPSEdition.Priority.class));
    when(loaded.getPriority()).thenAnswer(invocation -> stored.get());

    PSEditionSummary low = new PSEditionSummary();
    low.setPriority(1);
    PSEditionSummary savedLow = service.updateEdition("11", low);
    assertEquals("NightEd", savedLow.getName());
    assertEquals("keep-me", savedLow.getComment());
    assertEquals(1, savedLow.getPriority());

    PSEditionSummary high = new PSEditionSummary();
    high.setPriority(5);
    PSEditionSummary savedHigh = service.updateEdition("11", high);
    assertEquals("NightEd", savedHigh.getName());
    assertEquals("keep-me", savedHigh.getComment());
    assertEquals(5, savedHigh.getPriority());

    verify(loaded).setPriority(IPSEdition.Priority.LOWEST);
    verify(loaded).setPriority(IPSEdition.Priority.HIGHEST);
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setComment(any());
    verify(publisherService, times(2)).saveEdition(loaded);
    verify(publisherService, never()).loadEditionContentLists(any());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void updateEdition_priorityOutOfRange_400() {
    PSEditionSummary body = new PSEditionSummary();
    body.setPriority(0);
    WebApplicationException low =
        assertThrows(WebApplicationException.class, () -> service.updateEdition("11", body));
    assertEquals(400, low.getResponse().getStatus());
    assertTrue(
        low.getMessage()
            .contains(PSPublishingDesignRestService.EDITION_PRIORITY_OUT_OF_RANGE));

    body.setPriority(6);
    WebApplicationException high =
        assertThrows(WebApplicationException.class, () -> service.updateEdition("11", body));
    assertEquals(400, high.getResponse().getStatus());
    verify(publisherService, never()).loadEditionModifiable(any());
    verify(publisherService, never()).saveEdition(any());
    verify(publisherService, never()).loadEditionContentLists(any());
  }

  @Test
  void updateEdition_priority_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSEditionSummary body = new PSEditionSummary();
    body.setPriority(4);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateEdition("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadEditionModifiable(any());
    verify(publisherService, never()).saveEdition(any());
  }

  @Test
  void createEdition_priorityOutOfRange_400() {
    PSEditionSummary body = new PSEditionSummary();
    body.setName("NightEd");
    body.setSiteId("42");
    body.setPriority(9);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createEdition(body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        ex.getMessage()
            .contains(PSPublishingDesignRestService.EDITION_PRIORITY_OUT_OF_RANGE));
    verify(publisherService, never()).createEdition();
    verify(publisherService, never()).saveEdition(any());
  }

  @Test
  void createContentList_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSContentListSummary body = new PSContentListSummary();
    body.setName("HomePages");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createContentList(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void createContentList_duplicateName_409() {
    IPSContentList existing = mock(IPSContentList.class);
    when(existing.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(7);
    when(publisherService.findContentListByName("DupCl")).thenReturn(Optional.of(existing));

    PSContentListSummary body = new PSContentListSummary();
    body.setName("DupCl");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createContentList(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void updateContentList_duplicateName_409() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSContentList existing = mock(IPSContentList.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.findContentListByName("Taken")).thenReturn(Optional.of(existing));

    PSContentListSummary body = new PSContentListSummary();
    body.setName("Taken");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(loaded, never()).setName(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_rename_setsNameAndKeepsType() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("Renamed");
    when(loaded.getDescription()).thenReturn("kept");
    when(loaded.isLegacy()).thenReturn(false);
    when(publisherService.findContentListByName("Renamed")).thenReturn(Optional.empty());

    PSContentListSummary body = new PSContentListSummary();
    body.setName("  Renamed  ");
    body.setDescription("kept");
    body.setListType("legacy");

    PSContentListSummary saved = service.updateContentList("5", body);
    verify(loaded).setName("Renamed");
    verify(loaded).setDescription("kept");
    verify(loaded, never()).setContentListType(any());
    verify(publisherService).saveContentList(loaded);
    assertEquals("Renamed", saved.getName());
    assertEquals("modern", saved.getListType());
  }

  @Test
  void updateContentList_sameName_updatesDescription() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("NightCl");
    when(loaded.getDescription()).thenReturn("notes");
    when(loaded.isLegacy()).thenReturn(false);
    when(publisherService.findContentListByName("NightCl")).thenReturn(Optional.of(loaded));

    PSContentListSummary body = new PSContentListSummary();
    body.setName("NightCl");
    body.setDescription("notes");
    body.setListType("legacy");

    PSContentListSummary saved = service.updateContentList("5", body);
    verify(loaded).setName("NightCl");
    verify(loaded).setDescription("notes");
    verify(loaded, never()).setContentListType(any());
    verify(publisherService).saveContentList(loaded);
    assertEquals("NightCl", saved.getName());
    assertEquals("modern", saved.getListType());
  }

  @Test
  void updateContentList_setsItemFilterByUuid() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSGuid filterGuid = mock(IPSGuid.class);
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.ITEM_FILTER))).thenReturn(filterGuid);
    when(filterGuid.getUUID()).thenReturn(42);
    IPSFilterService filters = mock(IPSFilterService.class);
    IPSItemFilter filter = mock(IPSItemFilter.class);
    when(filters.findFilterByID(filterGuid)).thenReturn(filter);
    when(filter.getGUID()).thenReturn(filterGuid);
    when(filter.getName()).thenReturn("public");
    service.setFilterService(filters);

    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getFilterId()).thenReturn(filterGuid);

    PSContentListSummary body = new PSContentListSummary();
    body.setItemFilterId("42");

    PSContentListSummary saved = service.updateContentList("5", body);
    verify(loaded).setFilterId(filterGuid);
    verify(publisherService).saveContentList(loaded);
    assertEquals("42", saved.getItemFilterId());
    assertEquals("public", saved.getItemFilterName());
    assertEquals("NightCl", saved.getName());
  }

  @Test
  void updateContentList_setsItemFilterByName() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSGuid filterGuid = mock(IPSGuid.class);
    when(filterGuid.getUUID()).thenReturn(42);
    IPSFilterService filters = mock(IPSFilterService.class);
    IPSItemFilter filter = mock(IPSItemFilter.class);
    when(filters.findFilterByName("preview")).thenReturn(filter);
    when(filter.getGUID()).thenReturn(filterGuid);
    when(filter.getName()).thenReturn("preview");
    when(filters.findFilterByID(filterGuid)).thenReturn(filter);
    service.setFilterService(filters);

    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getFilterId()).thenReturn(filterGuid);

    PSContentListSummary body = new PSContentListSummary();
    body.setItemFilterId("preview");

    PSContentListSummary saved = service.updateContentList("5", body);
    verify(loaded).setFilterId(filterGuid);
    verify(publisherService).saveContentList(loaded);
    assertEquals("preview", saved.getItemFilterName());
  }

  @Test
  void updateContentList_blankItemFilterClearsIt() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);

    PSContentListSummary body = new PSContentListSummary();
    body.setItemFilterId("  ");

    PSContentListSummary saved = service.updateContentList("5", body);
    verify(loaded).setFilterId(null);
    verify(publisherService).saveContentList(loaded);
    assertNull(saved.getItemFilterId());
    assertNull(saved.getItemFilterName());
  }

  @Test
  void updateContentList_omittedItemFilterLeavesStoredFilter() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getDescription()).thenReturn("notes");

    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("notes");

    service.updateContentList("5", body);
    verify(loaded, never()).setFilterId(any());
    verify(publisherService).saveContentList(loaded);
  }

  @Test
  void updateContentList_descriptionOnly_keepsNameTypeGeneratorUrlAndFilter() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getDescription()).thenReturn("Night notes");
    when(loaded.getGenerator()).thenReturn("sys_Search");
    when(loaded.getUrl()).thenReturn("/Rhythmyx/contentlist");

    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("  Night notes  ");

    PSContentListSummary saved = service.updateContentList("5", body);
    assertEquals("NightCl", saved.getName());
    assertEquals("modern", saved.getListType());
    assertEquals("Night notes", saved.getDescription());
    assertEquals("sys_Search", saved.getGenerator());
    verify(loaded).setDescription("Night notes");
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setGenerator(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setFilterId(any());
    verify(loaded, never()).setContentListType(any());
    verify(publisherService).saveContentList(loaded);
  }

  @Test
  void updateContentList_descriptionOnly_legacyUrlStays() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = contentListForSummary("LegacyCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.isLegacy()).thenReturn(true);
    when(loaded.getDescription()).thenReturn("next");
    when(loaded.getUrl()).thenReturn("/Rhythmyx/legacyList");

    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("next");

    PSContentListSummary saved = service.updateContentList("5", body);
    assertEquals("legacy", saved.getListType());
    assertEquals("/Rhythmyx/legacyList", saved.getUrl());
    verify(loaded).setDescription("next");
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setGenerator(any());
    verify(publisherService).saveContentList(loaded);
  }

  @Test
  void updateContentList_blankDescription_clearsAndKeepsNameAndGenerator() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getDescription()).thenReturn(null);
    when(loaded.getGenerator()).thenReturn("sys_Search");

    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("   ");

    PSContentListSummary saved = service.updateContentList("5", body);
    assertEquals("NightCl", saved.getName());
    assertEquals("sys_Search", saved.getGenerator());
    assertNull(saved.getDescription());
    verify(loaded).setDescription(isNull());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setGenerator(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setFilterId(any());
    verify(publisherService).saveContentList(loaded);
  }

  @Test
  void updateContentList_descriptionTooLong_400_doesNotChangeFields() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(publisherService.findContentListByName("Renamed")).thenReturn(Optional.empty());

    PSContentListSummary body = new PSContentListSummary();
    body.setName("Renamed");
    body.setDescription(
        "d".repeat(PSPublishingDesignRestService.MAX_CONTENT_LIST_DESCRIPTION_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.CONTENT_LIST_DESCRIPTION_TOO_LONG, ex.getMessage());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setGenerator(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setFilterId(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_description_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("Night notes");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadContentListModifiable(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_duplicateNameWithDescription_409_doesNotChangeDescription()
      throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSContentList existing = mock(IPSContentList.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.findContentListByName("Taken")).thenReturn(Optional.of(existing));

    PSContentListSummary body = new PSContentListSummary();
    body.setName("Taken");
    body.setDescription("new notes");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_generatorOnly_keepsNameDescriptionTypeUrlAndFilter() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = contentListForSummary("NightCl");
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.getDescription()).thenReturn("Night notes");
    when(loaded.getGenerator()).thenReturn("sys_Changed");
    when(loaded.getUrl()).thenReturn("/Rhythmyx/contentlist");

    PSContentListSummary body = new PSContentListSummary();
    body.setGenerator("  sys_Changed  ");

    PSContentListSummary saved = service.updateContentList("5", body);
    assertEquals("NightCl", saved.getName());
    assertEquals("modern", saved.getListType());
    assertEquals("Night notes", saved.getDescription());
    assertEquals("sys_Changed", saved.getGenerator());
    assertEquals("/Rhythmyx/contentlist", saved.getUrl());
    verify(loaded).setGenerator("sys_Changed");
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setFilterId(any());
    verify(loaded, never()).setContentListType(any());
    verify(publisherService).saveContentList(loaded);
  }

  @Test
  void updateContentList_blankGenerator_400_doesNotChangeFields() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);

    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("Night notes");
    body.setGenerator("   ");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.CONTENT_LIST_GENERATOR_REQUIRED, ex.getMessage());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setGenerator(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setFilterId(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_generatorTooLong_400_doesNotChangeFields() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);

    PSContentListSummary body = new PSContentListSummary();
    body.setDescription("Night notes");
    body.setGenerator(
        "g".repeat(PSPublishingDesignRestService.MAX_CONTENT_LIST_GENERATOR_LENGTH + 1));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.CONTENT_LIST_GENERATOR_TOO_LONG, ex.getMessage());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setGenerator(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setFilterId(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_generatorOnLegacy_400_doesNotChangeUrl() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);
    when(loaded.isLegacy()).thenReturn(true);

    PSContentListSummary body = new PSContentListSummary();
    body.setGenerator("sys_Search");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.CONTENT_LIST_GENERATOR_LEGACY, ex.getMessage());
    verify(loaded, never()).setGenerator(any());
    verify(loaded, never()).setUrl(any());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setFilterId(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_generator_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSContentListSummary body = new PSContentListSummary();
    body.setGenerator("sys_Changed");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadContentListModifiable(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_duplicateNameWithGenerator_409_doesNotChangeGenerator() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList loaded = mock(IPSContentList.class);
    when(publisherService.loadContentListModifiable(contentListGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSContentList existing = mock(IPSContentList.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.findContentListByName("Taken")).thenReturn(Optional.of(existing));

    PSContentListSummary body = new PSContentListSummary();
    body.setName("Taken");
    body.setGenerator("sys_Changed");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setGenerator(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void updateContentList_unknownItemFilter_400() throws Exception {
    IPSGuid missing = mock(IPSGuid.class);
    when(guidManager.makeGuid(eq("999"), eq(PSTypeEnum.ITEM_FILTER))).thenReturn(missing);
    IPSFilterService filters = mock(IPSFilterService.class);
    when(filters.findFilterByID(missing)).thenReturn(null);
    when(filters.findFilterByName("999")).thenReturn(null);
    service.setFilterService(filters);

    PSContentListSummary body = new PSContentListSummary();
    body.setItemFilterId("999");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateContentList("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.UNKNOWN_ITEM_FILTER));
    verify(publisherService, never()).loadContentListModifiable(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void createContentList_unknownItemFilter_400() throws Exception {
    IPSFilterService filters = mock(IPSFilterService.class);
    when(filters.findFilterByName("no-such-filter")).thenReturn(null);
    service.setFilterService(filters);
    when(publisherService.findContentListByName("HomePages")).thenReturn(Optional.empty());

    PSContentListSummary body = new PSContentListSummary();
    body.setName("HomePages");
    body.setItemFilterId("no-such-filter");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createContentList(body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).createContentList(any());
    verify(publisherService, never()).saveContentList(any());
  }

  private IPSContentList contentListForSummary(String name) {
    IPSContentList loaded = mock(IPSContentList.class);
    when(loaded.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn(name);
    when(loaded.isLegacy()).thenReturn(false);
    return loaded;
  }

  @Test
  void copyContentList_blankName_400() {
    PSCopyContentListRequest request = new PSCopyContentListRequest();
    request.setSourceContentListId("5");
    request.setNewName("  ");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyContentList(request));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).loadContentList(any(IPSGuid.class));
    verify(publisherService, never()).createContentList(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void copyContentList_nameTooLong_400() {
    PSCopyContentListRequest request = new PSCopyContentListRequest();
    request.setSourceContentListId("5");
    request.setNewName("N".repeat(PSPublishingDesignRestService.MAX_CONTENT_LIST_NAME_LENGTH + 1));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyContentList(request));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.CONTENT_LIST_NAME_TOO_LONG));
    verify(publisherService, never()).loadContentList(any(IPSGuid.class));
    verify(publisherService, never()).createContentList(any());
  }

  @Test
  void copyContentList_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSCopyContentListRequest request = new PSCopyContentListRequest();
    request.setSourceContentListId("5");
    request.setNewName("NightCl copy");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyContentList(request));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).createContentList(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void copyContentList_missingSource_404() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(publisherService.loadContentList(contentListGuid))
        .thenThrow(new PSNotFoundException("missing"));

    PSCopyContentListRequest request = new PSCopyContentListRequest();
    request.setSourceContentListId("5");
    request.setNewName("NightCl copy");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyContentList(request));
    assertEquals(404, ex.getResponse().getStatus());
    verify(publisherService, never()).createContentList(any());
    verify(publisherService, never()).saveContentList(any());
  }

  @Test
  void copyContentList_duplicateName_409() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList source = mock(IPSContentList.class);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(source);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSContentList existing = mock(IPSContentList.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.findContentListByName("Taken")).thenReturn(Optional.of(existing));

    PSCopyContentListRequest request = new PSCopyContentListRequest();
    request.setSourceContentListId("5");
    request.setNewName("Taken");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyContentList(request));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.CONTENT_LIST_NAME_CONFLICT));
    verify(publisherService, never()).createContentList(any());
    verify(publisherService, never()).saveContentList(any());
    verify(source, never()).setName(any());
  }

  @Test
  void copyContentList_copiesDefinitionOntoNewIdAndLeavesSource() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList source = mock(IPSContentList.class);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(source);
    when(source.getDescription()).thenReturn("kept");
    when(source.getGenerator()).thenReturn("sys_Search");
    when(source.getExpander()).thenReturn("sys_Expander");
    when(source.getUrl()).thenReturn(" rx_Search ");
    when(source.getEditionType()).thenReturn(PSEditionType.AUTOMATIC);
    IPSGuid filterGuid = mock(IPSGuid.class);
    when(source.getFilterId()).thenReturn(filterGuid);
    when(source.getContentListType()).thenReturn(IPSContentList.Type.NORMAL);
    when(source.getGeneratorParams()).thenReturn(Map.of("query", "select 1"));
    when(source.getExpanderParams()).thenReturn(Map.of("template", "page"));
    when(publisherService.findContentListByName("NightCl copy")).thenReturn(Optional.empty());

    IPSGuid copyGuid = mock(IPSGuid.class);
    IPSContentList copy = mock(IPSContentList.class);
    when(publisherService.createContentList("NightCl copy")).thenReturn(copy);
    when(copy.getGUID()).thenReturn(copyGuid);
    when(copyGuid.getUUID()).thenReturn(88);
    when(copy.getName()).thenReturn("NightCl copy");
    when(copy.getDescription()).thenReturn("kept");
    when(copy.isLegacy()).thenReturn(false);
    when(copy.getGenerator()).thenReturn("sys_Search");
    when(copy.getUrl()).thenReturn("rx_Search");

    PSCopyContentListRequest request = new PSCopyContentListRequest();
    request.setSourceContentListId("5");
    request.setNewName("  NightCl copy  ");

    PSContentListSummary saved = service.copyContentList(request);
    assertEquals("88", saved.getContentListId());
    assertEquals("NightCl copy", saved.getName());
    assertEquals("kept", saved.getDescription());
    assertEquals("modern", saved.getListType());
    assertEquals("sys_Search", saved.getGenerator());

    verify(copy).setDescription("kept");
    verify(copy).setGenerator("sys_Search");
    verify(copy).setExpander("sys_Expander");
    verify(copy).setEditionType(PSEditionType.AUTOMATIC);
    verify(copy).setFilterId(filterGuid);
    verify(copy).setUrl("rx_Search");
    verify(copy).setContentListType(IPSContentList.Type.NORMAL);
    verify(copy).setGeneratorParams(argThat(params -> "select 1".equals(params.get("query"))));
    verify(copy).setExpanderParams(argThat(params -> "page".equals(params.get("template"))));
    InOrder order = inOrder(copy);
    order.verify(copy).setDescription("kept");
    order.verify(copy).setName("NightCl copy");
    verify(publisherService).saveContentList(copy);
    verify(publisherService, never()).saveContentList(source);
    verify(publisherService, never()).saveEditionContentList(any());
    verify(publisherService, never()).loadEditionContentLists(any());
    verify(source, never()).setName(any());
    verify(source, never()).setDescription(any());
    verify(source, never()).setGenerator(any());
    verify(source, never()).setFilterId(any());
  }

  @Test
  void createDeliveryType_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("filesystem");
    body.setBeanName("sys_fileDeliveryType");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createDeliveryType(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void createDeliveryType_nameTooLong_400() {
    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("n".repeat(PSPublishingDesignRestService.MAX_DELIVERY_TYPE_NAME_LENGTH + 1));
    body.setBeanName("sys_fileDeliveryHandler");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createDeliveryType(body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.DELIVERY_TYPE_NAME_TOO_LONG, ex.getMessage());
    verify(publisherService, never()).createDeliveryType();
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void createDeliveryType_copiesBeanDescriptionAndAssemblyFlag() throws Exception {
    when(publisherService.loadDeliveryType("NightDt")).thenThrow(new PSNotFoundException("free"));
    IPSDeliveryType created = mock(IPSDeliveryType.class);
    when(publisherService.createDeliveryType()).thenReturn(created);
    when(created.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(12);
    when(created.getName()).thenReturn("NightDt");
    when(created.getBeanName()).thenReturn("sys_fileDeliveryHandler");
    when(created.getDescription()).thenReturn("Publish content to the filesystem");
    when(created.isUnpublishingRequiresAssembly()).thenReturn(true);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("  NightDt  ");
    body.setBeanName(" sys_fileDeliveryHandler ");
    body.setDescription("Publish content to the filesystem");
    body.setUnpublishingRequiresAssembly(true);

    PSDeliveryTypeSummary saved = service.createDeliveryType(body);
    assertEquals("12", saved.getDeliveryTypeId());
    assertEquals("NightDt", saved.getName());
    assertEquals("sys_fileDeliveryHandler", saved.getBeanName());
    assertEquals("Publish content to the filesystem", saved.getDescription());
    assertTrue(saved.isUnpublishingRequiresAssembly());
    verify(created).setName("NightDt");
    verify(created).setBeanName("sys_fileDeliveryHandler");
    verify(created).setDescription("Publish content to the filesystem");
    verify(created).setUnpublishingRequiresAssembly(true);
    verify(publisherService).saveDeliveryType(created);
  }

  @Test
  void updateDeliveryType_nameTooLong_400() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid))
        .thenReturn(mock(IPSDeliveryType.class));
    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("n".repeat(PSPublishingDesignRestService.MAX_DELIVERY_TYPE_NAME_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.DELIVERY_TYPE_NAME_TOO_LONG, ex.getMessage());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void createDeliveryType_duplicateName_409() throws Exception {
    IPSDeliveryType existing = mock(IPSDeliveryType.class);
    when(existing.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(7);
    when(publisherService.loadDeliveryType("DupDt")).thenReturn(existing);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("DupDt");
    body.setBeanName("sys_fileDeliveryType");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.createDeliveryType(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void updateDeliveryType_duplicateName_409() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSDeliveryType existing = mock(IPSDeliveryType.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.loadDeliveryType("Taken")).thenReturn(existing);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("Taken");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setUnpublishingRequiresAssembly(anyBoolean());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_nameOnly_keepsBeanDescriptionAndAssemblyFlag() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);
    when(publisherService.loadDeliveryType("Renamed")).thenThrow(new PSNotFoundException("free"));
    when(loaded.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("Renamed");
    when(loaded.getBeanName()).thenReturn("sys_fileDeliveryHandler");
    when(loaded.getDescription()).thenReturn("kept description");
    when(loaded.isUnpublishingRequiresAssembly()).thenReturn(true);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("  Renamed  ");
    assertFalse(body.isUnpublishingRequiresAssemblySpecified());

    PSDeliveryTypeSummary saved = service.updateDeliveryType("5", body);
    assertEquals("5", saved.getDeliveryTypeId());
    assertEquals("Renamed", saved.getName());
    assertEquals("sys_fileDeliveryHandler", saved.getBeanName());
    assertEquals("kept description", saved.getDescription());
    assertTrue(saved.isUnpublishingRequiresAssembly());
    verify(loaded).setName("Renamed");
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setUnpublishingRequiresAssembly(anyBoolean());
    verify(publisherService).saveDeliveryType(loaded);
  }

  @Test
  void updateDeliveryType_descriptionOnly_keepsNameAndBean() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("filesystem");
    when(loaded.getBeanName()).thenReturn("sys_fileDeliveryHandler");
    when(loaded.getDescription()).thenReturn("Night notes");
    when(loaded.isUnpublishingRequiresAssembly()).thenReturn(true);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setDescription("  Night notes  ");

    PSDeliveryTypeSummary saved = service.updateDeliveryType("5", body);
    assertEquals("5", saved.getDeliveryTypeId());
    assertEquals("filesystem", saved.getName());
    assertEquals("sys_fileDeliveryHandler", saved.getBeanName());
    assertEquals("Night notes", saved.getDescription());
    assertTrue(saved.isUnpublishingRequiresAssembly());
    verify(loaded).setDescription("Night notes");
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setUnpublishingRequiresAssembly(anyBoolean());
    verify(publisherService).saveDeliveryType(loaded);
  }

  @Test
  void updateDeliveryType_blankDescription_clearsAndKeepsNameAndBean() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("filesystem");
    when(loaded.getBeanName()).thenReturn("sys_fileDeliveryHandler");
    when(loaded.getDescription()).thenReturn(null);
    when(loaded.isUnpublishingRequiresAssembly()).thenReturn(true);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setDescription("   ");

    PSDeliveryTypeSummary saved = service.updateDeliveryType("5", body);
    assertEquals("filesystem", saved.getName());
    assertEquals("sys_fileDeliveryHandler", saved.getBeanName());
    assertNull(saved.getDescription());
    verify(loaded).setDescription(isNull());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(publisherService).saveDeliveryType(loaded);
  }

  @Test
  void updateDeliveryType_descriptionTooLong_400_doesNotChangeNameOrBean() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("Renamed");
    body.setDescription(
        "d".repeat(PSPublishingDesignRestService.MAX_DELIVERY_TYPE_DESCRIPTION_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.DELIVERY_TYPE_DESCRIPTION_TOO_LONG, ex.getMessage());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setDescription(any());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_description_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setDescription("Night notes");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadDeliveryTypeModifiable(any());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_beanOnly_keepsNameAndDescription() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("filesystem");
    when(loaded.getBeanName()).thenReturn("sys_ftpDeliveryHandler");
    when(loaded.getDescription()).thenReturn("kept description");
    when(loaded.isUnpublishingRequiresAssembly()).thenReturn(true);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setBeanName("  sys_ftpDeliveryHandler  ");

    PSDeliveryTypeSummary saved = service.updateDeliveryType("5", body);
    assertEquals("5", saved.getDeliveryTypeId());
    assertEquals("filesystem", saved.getName());
    assertEquals("sys_ftpDeliveryHandler", saved.getBeanName());
    assertEquals("kept description", saved.getDescription());
    assertTrue(saved.isUnpublishingRequiresAssembly());
    verify(loaded).setBeanName("sys_ftpDeliveryHandler");
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setUnpublishingRequiresAssembly(anyBoolean());
    verify(publisherService, never()).loadDeliveryType(any(String.class));
    verify(publisherService).saveDeliveryType(loaded);
  }

  @Test
  void updateDeliveryType_blankBean_400_doesNotWrite() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("Renamed");
    body.setBeanName("   ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.DELIVERY_TYPE_BEAN_NAME_REQUIRED, ex.getMessage());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setDescription(any());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_beanTooLong_400_doesNotChangeNameOrDescription() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("Renamed");
    body.setDescription("new notes");
    body.setBeanName(
        "b".repeat(PSPublishingDesignRestService.MAX_DELIVERY_TYPE_BEAN_NAME_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.DELIVERY_TYPE_BEAN_NAME_TOO_LONG, ex.getMessage());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setDescription(any());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_bean_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setBeanName("sys_ftpDeliveryHandler");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadDeliveryTypeModifiable(any());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_duplicateNameWithDescription_409_doesNotChangeDescription()
      throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSDeliveryType existing = mock(IPSDeliveryType.class);
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(publisherService.loadDeliveryType("Taken")).thenReturn(existing);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("Taken");
    body.setDescription("new notes");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.updateDeliveryType("5", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setBeanName(any());
    verify(loaded, never()).setDescription(any());
    verify(publisherService, never()).saveDeliveryType(any());
  }

  @Test
  void updateDeliveryType_explicitAssemblyFlag_isApplied() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType loaded = mock(IPSDeliveryType.class);
    when(publisherService.loadDeliveryTypeModifiable(deliveryTypeGuid)).thenReturn(loaded);
    when(publisherService.loadDeliveryType("Renamed")).thenThrow(new PSNotFoundException("free"));
    when(loaded.getGUID()).thenReturn(deliveryTypeGuid);
    when(deliveryTypeGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("Renamed");
    when(loaded.getBeanName()).thenReturn("sys_fileDeliveryHandler");
    when(loaded.getDescription()).thenReturn("kept description");
    when(loaded.isUnpublishingRequiresAssembly()).thenReturn(false);

    PSDeliveryTypeSummary body = new PSDeliveryTypeSummary();
    body.setName("Renamed");
    body.setUnpublishingRequiresAssembly(false);
    assertTrue(body.isUnpublishingRequiresAssemblySpecified());

    PSDeliveryTypeSummary saved = service.updateDeliveryType("5", body);
    assertFalse(saved.isUnpublishingRequiresAssembly());
    verify(loaded).setName("Renamed");
    verify(loaded).setUnpublishingRequiresAssembly(false);
    verify(publisherService).saveDeliveryType(loaded);
  }

  @Test
  void deleteDeliveryType_unused_deletes() throws Exception {
    IPSDeliveryType type = stubLoadedDeliveryType("nightonly");
    IPSContentList other = mock(IPSContentList.class);
    when(other.getUrl())
        .thenReturn(
            "/Rhythmyx/contentlist?sys_contentlist=nightonly&sys_deliverytype=filesystem");
    when(publisherService.findAllContentLists("")).thenReturn(List.of(other));

    service.deleteDeliveryType("5");
    verify(publisherService).deleteDeliveryType(type);
  }

  @Test
  void deleteDeliveryType_referencedByContentList_409() throws Exception {
    stubLoadedDeliveryType("nightonly");
    IPSContentList referenced = mock(IPSContentList.class);
    when(referenced.getUrl())
        .thenReturn("/Rhythmyx/contentlist?sys_deliverytype=nightonly&sys_contentlist=x");
    when(publisherService.findAllContentLists("")).thenReturn(List.of(referenced));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteDeliveryType("5"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.DELIVERY_TYPE_IN_USE));
    verify(publisherService, never()).deleteDeliveryType(any());
  }

  @Test
  void deleteDeliveryType_encodedContentListReference_409() throws Exception {
    stubLoadedDeliveryType("night only");
    IPSContentList referenced = mock(IPSContentList.class);
    when(referenced.getUrl())
        .thenReturn("/Rhythmyx/contentlist?sys_deliverytype=night%20only&sys_contentlist=x");
    when(publisherService.findAllContentLists("")).thenReturn(List.of(referenced));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteDeliveryType("5"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteDeliveryType(any());
  }

  @Test
  void deleteDeliveryType_notFound_404() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    when(publisherService.loadDeliveryType(deliveryTypeGuid))
        .thenThrow(new PSNotFoundException("missing"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteDeliveryType("5"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteDeliveryType(any());
    verify(publisherService, never()).findAllContentLists(any());
  }

  @Test
  void deleteDeliveryType_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteDeliveryType("5"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadDeliveryType(any(IPSGuid.class));
    verify(publisherService, never()).deleteDeliveryType(any());
  }

  @Test
  void deleteDeliveryType_blankId_400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteDeliveryType("  "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(guidManager, never()).makeGuid(any(String.class), any(PSTypeEnum.class));
    verify(publisherService, never()).deleteDeliveryType(any());
  }

  private IPSDeliveryType stubLoadedDeliveryType(String name) throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.DELIVERY_TYPE))).thenReturn(deliveryTypeGuid);
    IPSDeliveryType type = mock(IPSDeliveryType.class);
    when(type.getName()).thenReturn(name);
    when(publisherService.loadDeliveryType(deliveryTypeGuid)).thenReturn(type);
    return type;
  }

  @Test
  void createScheme_forbidden_403() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Scheme");
    body.setGenerator("Java/global/percussion/contentassembler/sys_JexlAssemblyLocation");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.createScheme("3", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void createScheme_duplicateName_409() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSLocationScheme existing = mock(IPSLocationScheme.class);
    when(existing.getName()).thenReturn("DupScheme");
    when(existing.getGUID()).thenReturn(schemeGuid);
    when(schemeGuid.getUUID()).thenReturn(11);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(existing));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("DupScheme");
    body.setGenerator("Java/global/percussion/contentassembler/sys_JexlAssemblyLocation");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.createScheme("3", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void createScheme_nameTooLong_400() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("n".repeat(PSPublishingDesignRestService.MAX_LOCATION_SCHEME_NAME_LENGTH + 1));
    body.setGenerator("Java/global/percussion/contentassembler/sys_JexlAssemblyLocation");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.createScheme("3", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_NAME_TOO_LONG, ex.getMessage());
    verify(siteManager, never()).createScheme();
  }

  @Test
  void createScheme_copiesPathParameter_atNameLimit() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(3);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of());
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(siteManager.createScheme()).thenReturn(scheme);
    when(scheme.getParameterNames()).thenReturn(List.of());
    when(scheme.getGUID()).thenReturn(schemeGuid);
    when(schemeGuid.getUUID()).thenReturn(12);
    when(scheme.getName()).thenReturn("n".repeat(50));
    when(scheme.getContextId()).thenReturn(contextGuid);

    String name = "n".repeat(PSPublishingDesignRestService.MAX_LOCATION_SCHEME_NAME_LENGTH);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName(name);
    body.setGenerator("Java/global/percussion/contentassembler/sys_JexlAssemblyLocation");
    PSSchemeParameter path = new PSSchemeParameter();
    path.setName("path");
    path.setType("String");
    path.setValue("$sys.site.path");
    path.setSequence(0);
    body.setParameters(List.of(path));

    PSLocationSchemeSummary saved = design.createScheme("3", body);
    assertEquals("12", saved.getSchemeId());
    verify(scheme).setName(name);
    verify(scheme).addParameter("path", 0, "String", "$sys.site.path");
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void createScheme_copyWhenTripleTaken_assignsSchemeIdAsTemplate() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(3);
    IPSLocationScheme existing = mock(IPSLocationScheme.class);
    when(existing.getName()).thenReturn("Article");
    when(existing.getTemplateId()).thenReturn(8L);
    when(existing.getContentTypeId()).thenReturn(4L);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(existing));
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(siteManager.createScheme()).thenReturn(scheme);
    IPSGuid createdGuid = mock(IPSGuid.class);
    when(scheme.getGUID()).thenReturn(createdGuid);
    when(createdGuid.getUUID()).thenReturn(12);
    when(scheme.getParameterNames()).thenReturn(List.of());
    when(scheme.getName()).thenReturn("Article copy");
    when(scheme.getContextId()).thenReturn(contextGuid);

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setCopy(true);
    body.setName("Article copy");
    body.setGenerator("Java/global/percussion/contentassembler/sys_JexlAssemblyLocation");
    body.setContentTypeId(4L);
    body.setTemplateId(8L);
    PSSchemeParameter path = new PSSchemeParameter();
    path.setName("path");
    path.setType("String");
    path.setValue("$sys.site.path");
    path.setSequence(0);
    body.setParameters(List.of(path));

    PSLocationSchemeSummary saved = design.createScheme("3", body);
    assertEquals("12", saved.getSchemeId());
    verify(scheme).setContentTypeId(4L);
    verify(scheme).setTemplateId(12L);
    verify(scheme).addParameter("path", 0, "String", "$sys.site.path");
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_generatorOnly_keepsNameDescriptionTypeTemplateAndParameters() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Changed");

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setGenerator("  sys_Changed  ");

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("Pages", saved.getDescription());
    assertEquals("sys_Changed", saved.getGenerator());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    assertTrue(saved.getParameters() == null || saved.getParameters().isEmpty());
    verify(scheme).setGenerator("sys_Changed");
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).setContextId(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager).saveScheme(scheme);
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_nameOnly_keepsGenerator() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getName()).thenReturn("Renamed");
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of());

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Renamed");

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Renamed", saved.getName());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals("Pages", saved.getDescription());
    verify(scheme).setName("Renamed");
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_blankGenerator_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Renamed");
    body.setDescription("Pages");
    body.setGenerator("   ");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_GENERATOR_REQUIRED, ex.getMessage());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_generatorTooLong_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setGenerator(
        "g".repeat(PSPublishingDesignRestService.MAX_LOCATION_SCHEME_GENERATOR_LENGTH + 1));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_GENERATOR_TOO_LONG, ex.getMessage());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setName(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_generator_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setGenerator("sys_Changed");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_duplicateNameWithGenerator_409_doesNotChangeGenerator() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(3);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSLocationScheme other = mock(IPSLocationScheme.class);
    when(other.getName()).thenReturn("Taken");
    when(other.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(other));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Taken");
    body.setGenerator("sys_Changed");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_NAME_CONFLICT, ex.getMessage());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_descriptionOnly_keepsNameGeneratorTypeTemplateAndParameters()
      throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getDescription()).thenReturn("Night notes");

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setDescription("  Night notes  ");

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("Night notes", saved.getDescription());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    assertTrue(saved.getParameters() == null || saved.getParameters().isEmpty());
    verify(scheme).setDescription("Night notes");
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).setContextId(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager).saveScheme(scheme);
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_blankDescription_clearsAndKeepsOtherFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getDescription()).thenReturn(null);

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setDescription("   ");

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertNull(saved.getDescription());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    verify(scheme).setDescription(isNull());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_descriptionTooLong_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Renamed");
    body.setGenerator("sys_Changed");
    body.setDescription(
        "d".repeat(PSPublishingDesignRestService.MAX_LOCATION_SCHEME_DESCRIPTION_LENGTH + 1));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_DESCRIPTION_TOO_LONG, ex.getMessage());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_description_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setDescription("Night notes");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_duplicateNameWithDescription_409_doesNotChangeDescription() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(3);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSLocationScheme other = mock(IPSLocationScheme.class);
    when(other.getName()).thenReturn("Taken");
    when(other.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(other));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Taken");
    body.setDescription("Night notes");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_NAME_CONFLICT, ex.getMessage());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setGenerator(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_contentTypeOnly_keepsNameGeneratorDescriptionTemplateAndParameters()
      throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getContentTypeId()).thenReturn(9L);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of());

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setContentTypeId(9L);

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("Pages", saved.getDescription());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals(9L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    assertTrue(saved.getParameters() == null || saved.getParameters().isEmpty());
    verify(scheme).setContentTypeId(9L);
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).setContextId(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_contentTypeSameScheme_doesNotConflictWithItself() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getContentTypeId()).thenReturn(9L);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(scheme));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setContentTypeId(9L);

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals(9L, saved.getContentTypeId());
    assertEquals("Article", saved.getName());
    assertEquals(8L, saved.getTemplateId());
    verify(scheme).setContentTypeId(9L);
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_contentTypeNotPositive_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Renamed");
    body.setContentTypeId(0L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_CONTENT_TYPE_INVALID, ex.getMessage());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setTemplateId(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_contentTypeNegative_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setContentTypeId(-4L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_CONTENT_TYPE_INVALID, ex.getMessage());
    verify(scheme, never()).setContentTypeId(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_contentType_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setContentTypeId(9L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_contentTypeAssignmentTaken_409_doesNotChangeContentType() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(scheme.getTemplateId()).thenReturn(8L);
    when(contextGuid.getUUID()).thenReturn(3);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSLocationScheme other = mock(IPSLocationScheme.class);
    when(other.getTemplateId()).thenReturn(8L);
    when(other.getContentTypeId()).thenReturn(9L);
    when(other.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(other));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setContentTypeId(9L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_ASSIGNMENT_CONFLICT, ex.getMessage());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setTemplateId(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_templateOnly_keepsNameGeneratorDescriptionContentTypeAndParameters()
      throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getTemplateId()).thenReturn(12L);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of());

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setTemplateId(12L);

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("Pages", saved.getDescription());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(12L, saved.getTemplateId());
    assertTrue(saved.getParameters() == null || saved.getParameters().isEmpty());
    verify(scheme).setTemplateId(12L);
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setContextId(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_templateSameScheme_doesNotConflictWithItself() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(scheme));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setTemplateId(8L);

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals(8L, saved.getTemplateId());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals("Article", saved.getName());
    verify(scheme).setTemplateId(8L);
    verify(scheme, never()).setContentTypeId(any());
    verify(siteManager).saveScheme(scheme);
  }

  @Test
  void updateScheme_templateNotPositive_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Renamed");
    body.setTemplateId(0L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_TEMPLATE_INVALID, ex.getMessage());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_templateNegative_400_doesNotChangeFields() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setTemplateId(-4L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_TEMPLATE_INVALID, ex.getMessage());
    verify(scheme, never()).setTemplateId(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_template_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setTemplateId(12L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_templateAssignmentTaken_409_doesNotChangeTemplate() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(scheme.getContentTypeId()).thenReturn(4L);
    when(contextGuid.getUUID()).thenReturn(3);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSLocationScheme other = mock(IPSLocationScheme.class);
    when(other.getTemplateId()).thenReturn(12L);
    when(other.getContentTypeId()).thenReturn(4L);
    when(other.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(other));

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setTemplateId(12L);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_ASSIGNMENT_CONFLICT, ex.getMessage());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_keepsOtherFieldsAndExistingParameter() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeSummary("sys_Jexl");
    when(scheme.getParameterNames()).thenReturn(List.of("path"));
    when(scheme.getParameterType("path")).thenReturn("String");
    when(scheme.getParameterValue("path")).thenReturn("$sys.site.path");
    when(scheme.getParameterSequence("path")).thenReturn(0);

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName(" suffix ");
    added.setType(" BackendColumn ");
    added.setValue(" Contentstatus.contentid ");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("Pages", saved.getDescription());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    assertEquals(1, saved.getParameters().size());
    assertEquals("path", saved.getParameters().get(0).getName());
    assertEquals("$sys.site.path", saved.getParameters().get(0).getValue());
    verify(scheme).addParameter("suffix", 1, "BackendColumn", "Contentstatus.contentid");
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(scheme, never()).setDescription(any());
    verify(scheme, never()).setContentTypeId(any());
    verify(scheme, never()).setTemplateId(any());
    verify(scheme, never()).setContextId(any());
    verify(siteManager).saveScheme(scheme);
    verify(siteManager, never()).findSchemesByContextId(any());
  }

  @Test
  void updateScheme_addParameter_assignsSchemeParamId() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.LOCATION_SCHEME))).thenReturn(schemeGuid);
    PSLocationScheme scheme = new PSLocationScheme();
    when(siteManager.loadSchemeModifiable(schemeGuid)).thenReturn(scheme);

    IPSGuidManager guidMgr = mock(IPSGuidManager.class);
    IPSGuid paramGuid = mock(IPSGuid.class);
    when(paramGuid.getUUID()).thenReturn(4242);
    when(guidMgr.createGuid(PSTypeEnum.LOCATION_PROPERTY)).thenReturn(paramGuid);
    AtomicReference<IPSGuidManager> ref = guidManagerRef();
    IPSGuidManager previous = ref.get();
    ref.set(guidMgr);
    try {
      PSSchemeParameter added = new PSSchemeParameter();
      added.setName("suffix");
      added.setType("String");
      added.setValue("article");
      PSLocationSchemeSummary body = new PSLocationSchemeSummary();
      body.setAddParameter(Boolean.TRUE);
      body.setParameters(List.of(added));

      PSLocationSchemeSummary saved = design.updateScheme("11", body);
      assertEquals(1, saved.getParameters().size());
      assertEquals("suffix", saved.getParameters().get(0).getName());
      assertEquals("article", saved.getParameters().get(0).getValue());
      PSLocationSchemeParameter stored =
          scheme.getParameterSet().stream().findFirst().orElseThrow();
      assertEquals(4242, stored.getParameterId());
      verify(siteManager).saveScheme(scheme);
    } finally {
      ref.set(previous);
    }
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<IPSGuidManager> guidManagerRef() throws Exception {
    Field field = PSGuidManagerLocator.class.getDeclaredField("GUID_MANAGER_REF");
    field.setAccessible(true);
    return (AtomicReference<IPSGuidManager>) field.get(null);
  }

  @Test
  void updateScheme_addParameter_blankName_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("   ");
    added.setType("String");
    added.setValue("article");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_NAME_REQUIRED, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).setName(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_blankValue_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("suffix");
    added.setType("String");
    added.setValue("   ");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_blankType_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("suffix");
    added.setType("   ");
    added.setValue("article");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_nameTooLong_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("n".repeat(51));
    added.setType("String");
    added.setValue("article");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_typeTooLong_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("suffix");
    added.setType("t".repeat(51));
    added.setValue("article");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_notExactlyOne_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter first = new PSSchemeParameter();
    first.setName("suffix");
    first.setType("String");
    first.setValue("article");
    PSSchemeParameter second = new PSSchemeParameter();
    second.setName("other");
    second.setType("String");
    second.setValue("page");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(first, second));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_ONE_REQUIRED, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_duplicateName_409_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getParameterNames()).thenReturn(List.of("path"));

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName(" path ");
    added.setType("String");
    added.setValue("other");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_EXISTS, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("suffix");
    added.setType("String");
    added.setValue("article");
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addParameter_duplicateSchemeName_409_doesNotAdd() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(scheme.getParameterNames()).thenReturn(List.of("path"));
    when(scheme.getParameterSequence("path")).thenReturn(0);
    when(contextGuid.getUUID()).thenReturn(3);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSLocationScheme other = mock(IPSLocationScheme.class);
    when(other.getName()).thenReturn("Taken");
    when(other.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(other));

    PSSchemeParameter added = new PSSchemeParameter();
    added.setName("suffix");
    added.setType("String");
    added.setValue("article");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Taken");
    body.setAddParameter(Boolean.TRUE);
    body.setParameters(List.of(added));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_NAME_CONFLICT, ex.getMessage());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).setName(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_removeParameter_keepsOtherParameterAndIdentity() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.LOCATION_SCHEME))).thenReturn(schemeGuid);
    PSLocationScheme scheme = storedScheme();
    scheme.addParameter("path", 0, "String", "$sys.site.path");
    scheme.addParameter("suffix", 1, "BackendColumn", "article");
    parameterId(scheme, "path", 77);
    parameterId(scheme, "suffix", 88);
    when(siteManager.loadSchemeModifiable(schemeGuid)).thenReturn(scheme);

    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName(" suffix ");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of(removed));

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("Pages", saved.getDescription());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    assertEquals(1, saved.getParameters().size());
    assertEquals("path", saved.getParameters().get(0).getName());
    assertEquals("String", saved.getParameters().get(0).getType());
    assertEquals("$sys.site.path", saved.getParameters().get(0).getValue());
    assertEquals(0, saved.getParameters().get(0).getSequence());
    assertEquals(1, scheme.getParameterNames().size());
    assertEquals("path", scheme.getParameterNames().get(0));
    assertEquals(77, parameterId(scheme, "path", null));
    assertEquals(0, scheme.getParameterSequence("path"));
    verify(siteManager).saveScheme(scheme);
    verify(siteManager, never()).deleteScheme(any());
  }

  @Test
  void updateScheme_removeParameter_lastParameterLeavesEmptyListAndKeepsScheme() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.LOCATION_SCHEME))).thenReturn(schemeGuid);
    PSLocationScheme scheme = storedScheme();
    scheme.addParameter("path", 0, "String", "$sys.site.path");
    when(siteManager.loadSchemeModifiable(schemeGuid)).thenReturn(scheme);

    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName("path");
    removed.setType("String");
    removed.setValue("$sys.site.path");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of(removed));

    PSLocationSchemeSummary saved = design.updateScheme("11", body);
    assertEquals("Article", saved.getName());
    assertEquals("sys_Jexl", saved.getGenerator());
    assertEquals("Pages", saved.getDescription());
    assertEquals(4L, saved.getContentTypeId());
    assertEquals(8L, saved.getTemplateId());
    assertNotNull(saved.getParameters());
    assertEquals(0, saved.getParameters().size());
    assertTrue(scheme.getParameterNames().isEmpty());
    verify(siteManager).saveScheme(scheme);
    verify(siteManager, never()).deleteScheme(any());
  }

  @Test
  void updateScheme_removeParameter_blankName_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName("   ");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of(removed));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_NAME_REQUIRED, ex.getMessage());
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).addParameter(any(), any(int.class), any(), any());
    verify(scheme, never()).setName(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_removeParameter_nameTooLong_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName("n".repeat(51));
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of(removed));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG, ex.getMessage());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_removeParameter_notExactlyOne_400_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();

    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_REMOVE_ONE_REQUIRED,
        ex.getMessage());
    verify(scheme, never()).removeParameter(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_removeParameter_missingName_409_doesNotWrite() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getParameterNames()).thenReturn(List.of("path"));

    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName("suffix");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of(removed));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME, ex.getMessage());
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).setName(any());
    verify(scheme, never()).setGenerator(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_removeParameter_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setRemoveParameter(Boolean.TRUE);
    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName("path");
    body.setParameters(List.of(removed));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).deleteScheme(any());
  }

  @Test
  void updateScheme_removeParameter_duplicateSchemeName_409_doesNotRemove() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(scheme.getParameterNames()).thenReturn(List.of("path", "suffix"));
    when(contextGuid.getUUID()).thenReturn(3);
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSLocationScheme other = mock(IPSLocationScheme.class);
    when(other.getName()).thenReturn("Taken");
    when(other.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(other));

    PSSchemeParameter removed = new PSSchemeParameter();
    removed.setName("suffix");
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setName("Taken");
    body.setRemoveParameter(Boolean.TRUE);
    body.setParameters(List.of(removed));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.LOCATION_SCHEME_NAME_CONFLICT, ex.getMessage());
    verify(scheme, never()).removeParameter(any());
    verify(scheme, never()).setName(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateScheme_addAndRemoveParameter_400_doesNotLoad() {
    PSPublishingDesignRestService design = contextDesign();
    PSLocationSchemeSummary body = new PSLocationSchemeSummary();
    body.setAddParameter(Boolean.TRUE);
    body.setRemoveParameter(Boolean.TRUE);
    PSSchemeParameter parameter = new PSSchemeParameter();
    parameter.setName("path");
    body.setParameters(List.of(parameter));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateScheme("11", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.LOCATION_SCHEME_PARAMETER_ADD_AND_REMOVE, ex.getMessage());
    verify(siteManager, never()).loadSchemeModifiable(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void listSchemesForContext_includesParameters() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(scheme.getGUID()).thenReturn(schemeGuid);
    when(schemeGuid.getUUID()).thenReturn(11);
    when(scheme.getName()).thenReturn("Article");
    when(scheme.getGenerator()).thenReturn("sys_Jexl");
    when(scheme.getParameterNames()).thenReturn(List.of("path"));
    when(scheme.getParameterType("path")).thenReturn("String");
    when(scheme.getParameterValue("path")).thenReturn("$sys.site.path");
    when(scheme.getParameterSequence("path")).thenReturn(0);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of(scheme));

    List<PSLocationSchemeSummary> listed = design.listSchemesForContext("3");
    assertEquals(1, listed.size());
    assertEquals("Article", listed.get(0).getName());
    assertNotNull(listed.get(0).getParameters());
    assertEquals("path", listed.get(0).getParameters().get(0).getName());
    assertEquals("$sys.site.path", listed.get(0).getParameters().get(0).getValue());
  }

  private static PSLocationScheme storedScheme() {
    PSLocationScheme scheme = new PSLocationScheme();
    scheme.setId(11);
    scheme.setName("Article");
    scheme.setDescription("Pages");
    scheme.setGenerator("sys_Jexl");
    scheme.setContentTypeId(4L);
    scheme.setTemplateId(8L);
    return scheme;
  }

  /** Sets {@code id} when non-null and returns the stored id, or {@code -1} when the name is gone. */
  private static int parameterId(PSLocationScheme scheme, String name, Integer id) {
    for (PSLocationSchemeParameter parameter : scheme.getParameterSet()) {
      if (parameter != null && name.equals(parameter.getName())) {
        if (id != null) {
          parameter.setParameterId(id);
        }
        return parameter.getParameterId() == null ? -1 : parameter.getParameterId();
      }
    }
    return -1;
  }

  private IPSLocationScheme stubSchemeLoadOnly() throws Exception {
    when(guidManager.makeGuid(eq("11"), eq(PSTypeEnum.LOCATION_SCHEME))).thenReturn(schemeGuid);
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(siteManager.loadSchemeModifiable(schemeGuid)).thenReturn(scheme);
    return scheme;
  }

  private IPSLocationScheme stubSchemeSummary(String generator) throws Exception {
    IPSLocationScheme scheme = stubSchemeLoadOnly();
    when(schemeGuid.getUUID()).thenReturn(11);
    when(scheme.getGUID()).thenReturn(schemeGuid);
    when(scheme.getName()).thenReturn("Article");
    when(scheme.getDescription()).thenReturn("Pages");
    when(scheme.getGenerator()).thenReturn(generator);
    when(scheme.getContextId()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(3);
    when(scheme.getContentTypeId()).thenReturn(4L);
    when(scheme.getTemplateId()).thenReturn(8L);
    when(scheme.getParameterNames()).thenReturn(List.of());
    return scheme;
  }

  @Test
  void createContext_forbidden_403() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> false);
    PSContextSummary body = new PSContextSummary();
    body.setName("Preview");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.createContext(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void createContext_nameTooLong_400() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    PSContextSummary body = new PSContextSummary();
    body.setName("n".repeat(PSPublishingDesignRestService.MAX_CONTEXT_NAME_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.createContext(body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.CONTEXT_NAME_TOO_LONG, ex.getMessage());
    verify(siteManager, never()).createContext();
    verify(siteManager, never()).saveContext(any());
  }

  @Test
  void createContext_keepsDescriptionAndDoesNotAttachScheme() throws Exception {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    when(siteManager.findAllContexts()).thenReturn(List.of());
    IPSPublishingContext created = mock(IPSPublishingContext.class);
    when(siteManager.createContext()).thenReturn(created);
    when(created.getGUID()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(12);
    when(created.getName()).thenReturn("Publish copy");
    when(created.getDescription()).thenReturn("Public site");
    when(created.getDefaultScheme()).thenReturn(null);

    PSContextSummary body = new PSContextSummary();
    body.setName("  Publish copy  ");
    body.setDescription("Public site");

    PSContextSummary saved = design.createContext(body);
    assertEquals("12", saved.getContextId());
    assertEquals("Publish copy", saved.getName());
    assertEquals("Public site", saved.getDescription());
    assertNull(saved.getDefaultSchemeId());
    verify(created).setName("Publish copy");
    verify(created).setDescription("Public site");
    verify(created, never()).setDefaultSchemeId(any());
    verify(siteManager).saveContext(created);
    verify(siteManager, never()).createScheme();
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateContext_nameTooLong_400() throws Exception {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(mock(IPSPublishingContext.class));

    PSContextSummary body = new PSContextSummary();
    body.setName("n".repeat(PSPublishingDesignRestService.MAX_CONTEXT_NAME_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateContext("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.CONTEXT_NAME_TOO_LONG, ex.getMessage());
    verify(siteManager, never()).saveContext(any());
  }

  @Test
  void createContext_duplicateName_409() throws Exception {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    IPSPublishingContext existing = mock(IPSPublishingContext.class);
    when(existing.getName()).thenReturn("Publish");
    when(existing.getGUID()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(3);
    when(siteManager.findAllContexts()).thenReturn(List.of(existing));

    PSContextSummary body = new PSContextSummary();
    body.setName("Publish");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.createContext(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void updateContext_duplicateName_409() throws Exception {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext current = mock(IPSPublishingContext.class);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(current);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSPublishingContext existing = mock(IPSPublishingContext.class);
    when(existing.getName()).thenReturn("Taken");
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findAllContexts()).thenReturn(List.of(existing));

    PSContextSummary body = new PSContextSummary();
    body.setName("Taken");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateContext("5", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void updateContext_nameOnly_keepsDescriptionDefaultSchemeAndSchemes() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext loaded = mock(IPSPublishingContext.class);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(loaded);
    when(siteManager.findAllContexts()).thenReturn(List.of());
    when(loaded.getGUID()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("Renamed");
    when(loaded.getDescription()).thenReturn("kept description");
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(loaded.getDefaultScheme()).thenReturn(scheme);
    when(scheme.getGUID()).thenReturn(schemeGuid);
    when(schemeGuid.getUUID()).thenReturn(11);

    PSContextSummary body = new PSContextSummary();
    body.setName("  Renamed  ");

    PSContextSummary saved = design.updateContext("5", body);
    assertEquals("5", saved.getContextId());
    assertEquals("Renamed", saved.getName());
    assertEquals("kept description", saved.getDescription());
    assertEquals("11", saved.getDefaultSchemeId());
    verify(loaded).setName("Renamed");
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setDefaultSchemeId(any());
    verify(siteManager).saveContext(loaded);
    verify(siteManager, never()).findSchemesByContextId(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).createScheme();
  }

  @Test
  void updateContext_descriptionOnly_keepsNameDefaultSchemeAndSchemes() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext loaded = mock(IPSPublishingContext.class);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("Publish");
    when(loaded.getDescription()).thenReturn("Night notes");
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(loaded.getDefaultScheme()).thenReturn(scheme);
    when(scheme.getGUID()).thenReturn(schemeGuid);
    when(schemeGuid.getUUID()).thenReturn(11);

    PSContextSummary body = new PSContextSummary();
    body.setDescription("  Night notes  ");

    PSContextSummary saved = design.updateContext("5", body);
    assertEquals("5", saved.getContextId());
    assertEquals("Publish", saved.getName());
    assertEquals("Night notes", saved.getDescription());
    assertEquals("11", saved.getDefaultSchemeId());
    verify(loaded).setDescription("Night notes");
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDefaultSchemeId(any());
    verify(siteManager).saveContext(loaded);
    verify(siteManager, never()).findAllContexts();
    verify(siteManager, never()).findSchemesByContextId(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).createScheme();
  }

  @Test
  void updateContext_blankDescription_clearsAndKeepsNameAndSchemes() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext loaded = mock(IPSPublishingContext.class);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(loaded);
    when(loaded.getGUID()).thenReturn(contextGuid);
    when(contextGuid.getUUID()).thenReturn(5);
    when(loaded.getName()).thenReturn("Publish");
    when(loaded.getDescription()).thenReturn(null);
    IPSLocationScheme scheme = mock(IPSLocationScheme.class);
    when(loaded.getDefaultScheme()).thenReturn(scheme);
    when(scheme.getGUID()).thenReturn(schemeGuid);
    when(schemeGuid.getUUID()).thenReturn(11);

    PSContextSummary body = new PSContextSummary();
    body.setDescription("   ");

    PSContextSummary saved = design.updateContext("5", body);
    assertEquals("Publish", saved.getName());
    assertNull(saved.getDescription());
    assertEquals("11", saved.getDefaultSchemeId());
    verify(loaded).setDescription(isNull());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDefaultSchemeId(any());
    verify(siteManager).saveContext(loaded);
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).createScheme();
  }

  @Test
  void updateContext_descriptionTooLong_400_doesNotChangeNameOrSchemes() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext loaded = mock(IPSPublishingContext.class);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(loaded);

    PSContextSummary body = new PSContextSummary();
    body.setName("Renamed");
    body.setDescription(
        "d".repeat(PSPublishingDesignRestService.MAX_CONTEXT_DESCRIPTION_LENGTH + 1));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateContext("5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(PSPublishingDesignRestService.CONTEXT_DESCRIPTION_TOO_LONG, ex.getMessage());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setDefaultSchemeId(any());
    verify(siteManager, never()).saveContext(any());
    verify(siteManager, never()).saveScheme(any());
    verify(siteManager, never()).createScheme();
  }

  @Test
  void updateContext_description_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);
    PSContextSummary body = new PSContextSummary();
    body.setDescription("Night notes");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateContext("5", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadContextModifiable(any());
    verify(siteManager, never()).saveContext(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void updateContext_duplicateNameWithDescription_409_doesNotChangeDescription() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext loaded = mock(IPSPublishingContext.class);
    when(siteManager.loadContextModifiable(contextGuid)).thenReturn(loaded);

    IPSGuid otherGuid = mock(IPSGuid.class);
    IPSPublishingContext existing = mock(IPSPublishingContext.class);
    when(existing.getName()).thenReturn("Taken");
    when(existing.getGUID()).thenReturn(otherGuid);
    when(otherGuid.getUUID()).thenReturn(99);
    when(siteManager.findAllContexts()).thenReturn(List.of(existing));

    PSContextSummary body = new PSContextSummary();
    body.setName("Taken");
    body.setDescription("new notes");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.updateContext("5", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(loaded, never()).setName(any());
    verify(loaded, never()).setDescription(any());
    verify(loaded, never()).setDefaultSchemeId(any());
    verify(siteManager, never()).saveContext(any());
    verify(siteManager, never()).saveScheme(any());
  }

  @Test
  void deleteContentList_notFound_404() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(publisherService.loadContentList(contentListGuid))
        .thenThrow(new PSNotFoundException("missing"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteContentList("5"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteContentLists(any());
  }

  @Test
  void deleteContentList_unassociated_deletes() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSContentList cl = mock(IPSContentList.class);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);
    when(publisherService.findAllEditions("")).thenReturn(List.of());

    service.deleteContentList("5");
    verify(publisherService).deleteContentLists(List.of(cl));
  }

  @Test
  void deleteContentList_associated_409() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(5L);
    IPSContentList cl = mock(IPSContentList.class);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);

    IPSEdition edition = mock(IPSEdition.class);
    when(edition.getGUID()).thenReturn(editionGuid);
    when(publisherService.findAllEditions("")).thenReturn(List.of(edition));

    IPSEditionContentList link = mock(IPSEditionContentList.class);
    IPSGuid linkedList = mock(IPSGuid.class);
    when(link.getContentListId()).thenReturn(linkedList);
    when(linkedList.longValue()).thenReturn(5L);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(link));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteContentList("5"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.CONTENT_LIST_IN_USE));
    verify(publisherService, never()).deleteContentLists(any());
  }

  @Test
  void deleteContentList_otherAssociation_deletes() throws Exception {
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(5L);
    IPSContentList cl = mock(IPSContentList.class);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);

    IPSEdition edition = mock(IPSEdition.class);
    when(edition.getGUID()).thenReturn(editionGuid);
    when(publisherService.findAllEditions("")).thenReturn(List.of(edition));

    IPSEditionContentList link = mock(IPSEditionContentList.class);
    IPSGuid linkedList = mock(IPSGuid.class);
    when(link.getContentListId()).thenReturn(linkedList);
    when(linkedList.longValue()).thenReturn(8L);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(link));

    service.deleteContentList("5");
    verify(publisherService).deleteContentLists(List.of(cl));
  }

  @Test
  void deleteContentList_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteContentList("5"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadContentList(any(IPSGuid.class));
    verify(publisherService, never()).deleteContentLists(any());
  }

  @Test
  void deleteContentList_blankId_400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.deleteContentList("  "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).loadContentList(any(IPSGuid.class));
    verify(publisherService, never()).deleteContentLists(any());
  }

  private PSPublishingDesignRestService contextDesign() {
    PSPublishingDesignRestService design =
        new PSPublishingDesignRestService(publisherService, guidManager, siteManager);
    design.setDesignWriteAllowed(() -> true);
    return design;
  }

  @Test
  void deleteContext_noSchemes_deletes() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext ctx = mock(IPSPublishingContext.class);
    when(siteManager.loadContext(contextGuid)).thenReturn(ctx);
    when(siteManager.findSchemesByContextId(contextGuid)).thenReturn(List.of());

    design.deleteContext("3");
    verify(siteManager).deleteContext(ctx);
  }

  @Test
  void deleteContext_hasSchemes_409() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    IPSPublishingContext ctx = mock(IPSPublishingContext.class);
    when(siteManager.loadContext(contextGuid)).thenReturn(ctx);
    when(siteManager.findSchemesByContextId(contextGuid))
        .thenReturn(List.of(mock(IPSLocationScheme.class)));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.deleteContext("3"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(
        ex.getMessage().contains(PSPublishingDesignRestService.CONTEXT_HAS_LOCATION_SCHEMES));
    verify(siteManager, never()).deleteContext(any(IPSPublishingContext.class));
  }

  @Test
  void deleteContext_notFound_404() throws Exception {
    PSPublishingDesignRestService design = contextDesign();
    when(guidManager.makeGuid(eq("3"), eq(PSTypeEnum.CONTEXT))).thenReturn(contextGuid);
    when(siteManager.loadContext(contextGuid)).thenThrow(new PSNotFoundException("missing"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.deleteContext("3"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(siteManager, never()).deleteContext(any(IPSPublishingContext.class));
    verify(siteManager, never()).findSchemesByContextId(any(IPSGuid.class));
  }

  @Test
  void deleteContext_forbidden_403() {
    PSPublishingDesignRestService design = contextDesign();
    design.setDesignWriteAllowed(() -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.deleteContext("3"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(siteManager, never()).loadContext(any(IPSGuid.class));
    verify(siteManager, never()).deleteContext(any(IPSPublishingContext.class));
  }

  @Test
  void deleteContext_blankId_400() {
    PSPublishingDesignRestService design = contextDesign();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> design.deleteContext("  "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(guidManager, never()).makeGuid(any(String.class), any(PSTypeEnum.class));
    verify(siteManager, never()).loadContext(any(IPSGuid.class));
    verify(siteManager, never()).deleteContext(any(IPSPublishingContext.class));
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
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of());

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
  void associateContentList_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setContentListId("5");
    body.setDeliveryContextId("9");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.associateContentList("1", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(publisherService, never()).loadEdition(any());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void associateContentList_alreadyAssociated_409() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(publisherService.loadEdition(editionGuid)).thenReturn(mock(IPSEdition.class));

    IPSContentList cl = mock(IPSContentList.class);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);
    when(contentListGuid.longValue()).thenReturn(5L);

    IPSEditionContentList existing = mock(IPSEditionContentList.class);
    IPSGuid existingCl = mock(IPSGuid.class);
    when(existing.getContentListId()).thenReturn(existingCl);
    when(existingCl.longValue()).thenReturn(5L);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(existing));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setContentListId("5");
    body.setDeliveryContextId("9");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.associateContentList("1", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(
        PSPublishingDesignRestService.CONTENT_LIST_ALREADY_ASSOCIATED, ex.getMessage());
    verify(publisherService, never()).saveEditionContentList(any());
    verify(publisherService, never()).createEditionContentList();
  }

  @Test
  void disassociateContentList_removesOnlyMatchingLink() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    service.setEditionRunningJobId(guid -> 0L);
    when(contentListGuid.longValue()).thenReturn(5L);

    IPSEditionContentList match = mock(IPSEditionContentList.class);
    IPSGuid matchId = mock(IPSGuid.class);
    when(match.getContentListId()).thenReturn(matchId);
    when(matchId.longValue()).thenReturn(5L);

    IPSEditionContentList other = mock(IPSEditionContentList.class);
    IPSGuid otherId = mock(IPSGuid.class);
    when(other.getContentListId()).thenReturn(otherId);
    when(otherId.longValue()).thenReturn(8L);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(match, other));

    service.disassociateContentList("1", "5");
    verify(publisherService).deleteEditionContentList(match);
    verify(publisherService, never()).deleteEditionContentList(other);
  }

  @Test
  void disassociateContentList_blankId_400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.disassociateContentList("1", " "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteEditionContentList(any());
    verify(publisherService, never()).loadEditionContentLists(any());
  }

  @Test
  void disassociateContentList_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.disassociateContentList("1", "5"));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.DESIGN_WRITE_FORBIDDEN));
    verify(publisherService, never()).deleteEditionContentList(any());
    verify(publisherService, never()).loadEditionContentLists(any());
  }

  @Test
  void disassociateContentList_running_409() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    service.setEditionRunningJobId(guid -> 55L);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.disassociateContentList("1", "5"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.EDITION_IN_USE));
    verify(publisherService, never()).deleteEditionContentList(any());
    verify(publisherService, never()).loadEditionContentLists(any());
  }

  @Test
  void disassociateContentList_missing_404() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    service.setEditionRunningJobId(guid -> 0L);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.disassociateContentList("1", "5"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(publisherService, never()).deleteEditionContentList(any());
  }

  @Test
  void associateContentList_omittedSequence_appendsAfterHighest() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    IPSGuid ctxGuid = mock(IPSGuid.class);
    when(guidManager.makeGuid(eq("9"), eq(PSTypeEnum.CONTEXT))).thenReturn(ctxGuid);
    when(publisherService.loadEdition(editionGuid)).thenReturn(mock(IPSEdition.class));

    IPSContentList cl = mock(IPSContentList.class);
    when(cl.getGUID()).thenReturn(contentListGuid);
    when(contentListGuid.getUUID()).thenReturn(5);
    when(cl.getName()).thenReturn("News");
    when(cl.isLegacy()).thenReturn(false);
    when(publisherService.loadContentList(contentListGuid)).thenReturn(cl);

    IPSGuid linkId = mock(IPSGuid.class);
    when(linkId.longValue()).thenReturn(99L);
    com.percussion.services.publisher.data.PSEditionContentList link =
        new com.percussion.services.publisher.data.PSEditionContentList(linkId);
    when(publisherService.createEditionContentList()).thenReturn(link);
    when(editionGuid.longValue()).thenReturn(1L);
    when(contentListGuid.longValue()).thenReturn(5L);
    IPSEditionContentList existing = association(8L, 4);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(existing));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setContentListId("5");
    body.setDeliveryContextId("9");

    service.associateContentList("1", body);
    assertEquals(5, link.getSequence());
  }

  @Test
  void listEditionContentLists_ordersBySequenceThenId() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSEditionContentList later = association(5L, 2);
    IPSEditionContentList earlier = association(8L, 1);
    IPSEditionContentList unsequenced = association(3L, null);
    when(publisherService.loadEditionContentLists(editionGuid))
        .thenReturn(List.of(later, unsequenced, earlier));
    when(publisherService.loadContentList(any(IPSGuid.class))).thenAnswer(invocation -> {
      IPSGuid id = invocation.getArgument(0);
      IPSContentList cl = mock(IPSContentList.class);
      long value = id.longValue();
      String name = value == 8L ? "Earlier" : value == 5L ? "Middle" : "Last";
      when(cl.getName()).thenReturn(name);
      when(cl.isLegacy()).thenReturn(false);
      return cl;
    });

    List<PSContentListSummary> rows = service.listEditionContentLists("1");
    assertEquals(List.of("Earlier", "Middle", "Last"), rows.stream().map(PSContentListSummary::getName).toList());
  }

  @Test
  void reorderEditionContentList_swapsAdjacentSequences() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("8"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(8L);
    service.setEditionRunningJobId(guid -> 0L);
    IPSEditionContentList first = association(5L, 1);
    IPSEditionContentList second = association(8L, 2);
    IPSEditionContentList third = association(9L, 3);
    when(publisherService.loadEditionContentLists(editionGuid))
        .thenReturn(List.of(third, first, second));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setContentListId("8");
    body.setSequence(2);

    service.reorderEditionContentList("1", "8", body);

    assertEquals(1, first.getSequence());
    assertEquals(3, second.getSequence());
    assertEquals(2, third.getSequence());
    verify(publisherService, never()).saveEditionContentList(first);
    verify(publisherService).saveEditionContentList(second);
    verify(publisherService).saveEditionContentList(third);
  }

  @Test
  void reorderEditionContentList_nullSequences_becomeOneBasedOrder() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("8"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(8L);
    service.setEditionRunningJobId(guid -> 0L);
    IPSEditionContentList lowId = association(5L, null);
    IPSEditionContentList highId = association(8L, null);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(highId, lowId));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setSequence(0);

    service.reorderEditionContentList("1", "8", body);

    assertEquals(1, highId.getSequence());
    assertEquals(2, lowId.getSequence());
    verify(publisherService).saveEditionContentList(highId);
    verify(publisherService).saveEditionContentList(lowId);
  }

  @Test
  void reorderEditionContentList_notAdjacent_400() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(5L);
    service.setEditionRunningJobId(guid -> 0L);
    IPSEditionContentList first = association(5L, 1);
    IPSEditionContentList middle = association(8L, 2);
    IPSEditionContentList last = association(9L, 3);
    when(publisherService.loadEditionContentLists(editionGuid))
        .thenReturn(List.of(first, middle, last));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setSequence(2);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", "5", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        ex.getMessage()
            .contains(PSPublishingDesignRestService.CONTENT_LIST_SEQUENCE_NOT_ADJACENT));
    assertEquals(1, first.getSequence());
    assertEquals(2, middle.getSequence());
    assertEquals(3, last.getSequence());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void reorderEditionContentList_endsAndMissingSequence_400() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(5L);
    service.setEditionRunningJobId(guid -> 0L);
    IPSEditionContentList first = association(5L, 1);
    IPSEditionContentList last = association(8L, 2);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(first, last));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc up =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    up.setSequence(-1);
    WebApplicationException moveUp =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", "5", up));
    assertEquals(400, moveUp.getResponse().getStatus());

    when(guidManager.makeGuid(eq("8"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(8L);
    com.percussion.publishingdesign.data.PSEditionContentListAssoc down =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    down.setSequence(2);
    WebApplicationException moveDown =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", "8", down));
    assertEquals(400, moveDown.getResponse().getStatus());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () ->
                service.reorderEditionContentList(
                    "1", "8", new com.percussion.publishingdesign.data.PSEditionContentListAssoc()));
    assertEquals(400, missing.getResponse().getStatus());
    assertTrue(missing.getMessage().contains("sequence is required"));
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void reorderEditionContentList_blankId_400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", " ", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(publisherService, never()).loadEditionContentLists(any());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void reorderEditionContentList_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setSequence(1);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", "5", body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.DESIGN_WRITE_FORBIDDEN));
    verify(publisherService, never()).loadEditionContentLists(any());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void reorderEditionContentList_running_409() {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    service.setEditionRunningJobId(guid -> 55L);
    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setSequence(1);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", "8", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains(PSPublishingDesignRestService.EDITION_IN_USE));
    verify(publisherService, never()).loadEditionContentLists(any());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  @Test
  void reorderEditionContentList_missing_404() throws Exception {
    when(guidManager.makeGuid(eq("1"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("5"), eq(PSTypeEnum.CONTENT_LIST))).thenReturn(contentListGuid);
    when(contentListGuid.longValue()).thenReturn(5L);
    service.setEditionRunningJobId(guid -> 0L);
    IPSEditionContentList present = association(8L, 1);
    when(publisherService.loadEditionContentLists(editionGuid)).thenReturn(List.of(present));

    com.percussion.publishingdesign.data.PSEditionContentListAssoc body =
        new com.percussion.publishingdesign.data.PSEditionContentListAssoc();
    body.setSequence(0);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.reorderEditionContentList("1", "5", body));
    assertEquals(404, ex.getResponse().getStatus());
    verify(publisherService, never()).saveEditionContentList(any());
  }

  private IPSEditionContentList association(long contentListId, Integer sequence) {
    IPSEditionContentList link = mock(IPSEditionContentList.class);
    IPSGuid id = mock(IPSGuid.class);
    AtomicReference<Integer> stored = new AtomicReference<>(sequence);
    lenient().when(id.longValue()).thenReturn(contentListId);
    lenient().when(link.getContentListId()).thenReturn(id);
    lenient().when(link.getSequence()).thenAnswer(invocation -> stored.get());
    lenient()
        .doAnswer(
            invocation -> {
              stored.set(invocation.getArgument(0));
              return null;
            })
        .when(link)
        .setSequence(any());
    return link;
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
  void copyEdition_forbidden_403() {
    service.setDesignWriteAllowed(() -> false);
    com.percussion.publishingdesign.data.PSCopyEditionRequest req =
        new com.percussion.publishingdesign.data.PSCopyEditionRequest();
    req.setSourceEditionId("7");
    req.setTargetSiteId("42");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyEdition(req));
    assertEquals(403, ex.getResponse().getStatus());
    org.mockito.Mockito.verify(publisherService, org.mockito.Mockito.never()).createEdition();
  }

  @Test
  void copyEdition_duplicateName_409() throws Exception {
    when(guidManager.makeGuid(eq("7"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    IPSEdition source = mock(IPSEdition.class);
    when(publisherService.loadEdition(editionGuid)).thenReturn(source);
    IPSEdition existing = mock(IPSEdition.class);
    IPSGuid existingGuid = mock(IPSGuid.class);
    when(existing.getGUID()).thenReturn(existingGuid);
    when(existingGuid.getUUID()).thenReturn(99);
    when(publisherService.findEditionByName("Taken")).thenReturn(existing);

    com.percussion.publishingdesign.data.PSCopyEditionRequest req =
        new com.percussion.publishingdesign.data.PSCopyEditionRequest();
    req.setSourceEditionId("7");
    req.setTargetSiteId("42");
    req.setNewName("Taken");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.copyEdition(req));
    assertEquals(409, ex.getResponse().getStatus());
    org.mockito.Mockito.verify(publisherService, org.mockito.Mockito.never()).createEdition();
    org.mockito.Mockito.verify(publisherService, org.mockito.Mockito.never()).saveEdition(any());
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
  void copyEdition_keepsRequestedNameWhenSourceHasDisplayTitle() throws Exception {
    when(guidManager.makeGuid(eq("7"), eq(PSTypeEnum.EDITION))).thenReturn(editionGuid);
    when(guidManager.makeGuid(eq("42"), eq(PSTypeEnum.SITE))).thenReturn(siteGuid);

    IPSEdition source = mock(IPSEdition.class);
    org.mockito.Mockito.lenient().when(source.getDisplayTitle()).thenReturn("SourceEd");
    when(source.getComment()).thenReturn("c");
    when(publisherService.loadEdition(editionGuid)).thenReturn(source);

    IPSEdition copy = mock(IPSEdition.class);
    when(publisherService.createEdition()).thenReturn(copy);
    when(copy.getGUID()).thenReturn(editionGuid);
    when(editionGuid.getUUID()).thenReturn(11);
    when(copy.getName()).thenReturn("CopiedName");

    com.percussion.publishingdesign.data.PSCopyEditionRequest req =
        new com.percussion.publishingdesign.data.PSCopyEditionRequest();
    req.setSourceEditionId("7");
    req.setTargetSiteId("42");
    req.setNewName("CopiedName");
    req.setCopyContentLists(false);

    PSEditionSummary result = service.copyEdition(req);
    assertEquals("CopiedName", result.getName());
    org.mockito.Mockito.verify(copy).setName("CopiedName");
    org.mockito.Mockito.verify(copy, org.mockito.Mockito.never()).setDisplayTitle("SourceEd");
    org.mockito.Mockito.verify(publisherService).saveEdition(copy);
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
