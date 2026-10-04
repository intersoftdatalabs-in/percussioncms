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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.publishingdesign.data.PSContentListSummary;
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
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSDeliveryType;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSEditionContentList;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
