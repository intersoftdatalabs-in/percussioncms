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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.data.PSItemEditorField;
import com.percussion.itemmanagement.data.PSItemEditorFields;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EditorHost field save (#4645): PUT maps a stale revision to HTTP 409 and does not write.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceSaveEditorFieldsTest {

  @Mock private IPSIdMapper idMapper;
  @Mock private IPSSystemService systemService;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSWidgetAssetRelationshipService waRelService;
  @Mock private IPSItemWorkflowService itemWfService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSContentItemDao contentItemDao;
  @Mock private IPSAssetDao assetDao;
  @Mock private IPSTemplateService templateService;
  @Mock private IPSUserItemsDao userItemDao;
  @Mock private IPSNotificationService notificationService;
  @Mock private IPSPublisherService pubService;
  @Mock private IPSManagedLinkDao linkService;
  @Mock private PSComponentSummary summary;
  @Mock private IPSGuid guid;

  private PSItemService service;

  @BeforeEach
  void setUp() {
    service =
        new PSItemService(
            idMapper,
            systemService,
            workflowHelper,
            contentWs,
            waRelService,
            itemWfService,
            folderHelper,
            contentItemDao,
            assetDao,
            templateService,
            userItemDao,
            notificationService,
            pubService,
            linkService);
  }

  @Test
  void staleRevisionMapsToConflictAndDoesNotSave() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 3));
    when(summary.getCheckoutUserName()).thenReturn("");

    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(1);
    req.setFields(List.of(new PSItemEditorField("displaytitle", "Nope")));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.CONFLICT.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
  }

  @Test
  void embeddedNulInLongTextMapsToBadRequestAndDoesNotSave() throws Exception {
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(0);
    req.setFields(List.of(new PSItemEditorField("description", "line\u0000two")));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
    verify(contentWs, never()).prepareForEdit(any(IPSGuid.class));
  }

  @Test
  void nonNumericValueMapsToBadRequestAndDoesNotSave() throws Exception {
    PSItemEditorField qty = new PSItemEditorField("qty", "abc");
    qty.setDataType("integer");
    qty.setMinimum("0");
    qty.setMaximum("10");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(0);
    req.setFields(List.of(qty));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
    verify(contentWs, never()).prepareForEdit(any(IPSGuid.class));
  }

  @Test
  void outOfRangeNumberMapsToBadRequestAndDoesNotSave() throws Exception {
    PSItemEditorField qty = new PSItemEditorField("qty", "11");
    qty.setDataType("number");
    qty.setMinimum("0");
    qty.setMaximum("10");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(0);
    req.setFields(List.of(qty));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
  }

  @Test
  void matchingRevisionSavesAndReturnsLiveRevision() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 2));
    when(summary.getCheckoutUserName()).thenReturn("admin");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(true);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.prepareForEdit(guid)).thenReturn(null);
    PSContentItem item = new PSContentItem();
    item.setId("42");
    item.setType("percPage");
    item.setName("Home");
    item.setFields(new HashMap<>());
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(2);
    req.setFields(List.of(new PSItemEditorField("displaytitle", "Welcome")));

    PSItemEditorFields saved = service.saveEditorFields("42", req);
    assertEquals(2, saved.getRevision());
    assertEquals("Welcome", item.getFields().get("displaytitle"));
    verify(contentItemDao).save(item);
  }

  @Test
  void inRangeIntegerSavesTheSubmittedValue() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 2));
    when(summary.getCheckoutUserName()).thenReturn("admin");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(true);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.prepareForEdit(guid)).thenReturn(null);
    PSContentItem item = new PSContentItem();
    item.setId("42");
    item.setType("percPage");
    item.setName("Home");
    item.setFields(new HashMap<>());
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    PSItemEditorField qty = new PSItemEditorField("qty", "7");
    qty.setDataType("integer");
    qty.setMinimum("0");
    qty.setMaximum("10");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(2);
    req.setFields(List.of(qty));

    service.saveEditorFields("42", req);
    assertEquals("7", item.getFields().get("qty"));
    verify(contentItemDao).save(item);
  }

  @Test
  void invalidLinkSyntaxMapsToBadRequestAndDoesNotSave() throws Exception {
    PSItemEditorField page = new PSItemEditorField("page", "javascript:alert(1)");
    page.setDataType("link");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(0);
    req.setFields(List.of(page));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
    verify(folderHelper, never()).findItem(anyString());
    verify(folderHelper, never()).findItemById(anyString());
    verify(linkService, never()).createLink(anyInt(), anyInt(), anyInt(), any());
  }

  @Test
  void missingLinkTargetMapsToNotFoundAndDoesNotSave() throws Exception {
    when(folderHelper.findItemById("999")).thenThrow(new PSNotFoundException("missing"));
    PSItemEditorField page = new PSItemEditorField("page", "999");
    page.setDataType("link");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(0);
    req.setFields(List.of(page));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
    verify(waRelService, never()).updateLocalRelationshipAsset(anyString());
  }

  @Test
  void forbiddenLinkTargetMapsToForbiddenAndDoesNotSave() throws Exception {
    PSValidationException denied = Mockito.mock(PSValidationException.class);
    when(folderHelper.findItem("//Sites/Hidden/index")).thenThrow(denied);
    PSItemEditorField page = new PSItemEditorField("page", "//Sites/Hidden/index");
    page.setDataType("link");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(0);
    req.setFields(List.of(page));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
  }

  @Test
  void clearedLinkSavesEmptyAndDoesNotLookUpATarget() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 2));
    when(summary.getCheckoutUserName()).thenReturn("admin");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(true);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.prepareForEdit(guid)).thenReturn(null);
    PSContentItem item = new PSContentItem();
    item.setId("42");
    item.setType("percPage");
    item.setName("Home");
    item.setFields(new HashMap<>());
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    PSItemEditorField page = new PSItemEditorField("page", "  ");
    page.setDataType("link");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(2);
    req.setFields(List.of(page));

    service.saveEditorFields("42", req);
    assertEquals("", item.getFields().get("page"));
    verify(folderHelper, never()).findItem(anyString());
    verify(folderHelper, never()).findItemById(anyString());
    verify(contentItemDao).save(item);
  }

  @Test
  void existingLinkIdSavesWithoutCreatingARelationship() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 2));
    when(summary.getCheckoutUserName()).thenReturn("admin");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(true);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.prepareForEdit(guid)).thenReturn(null);
    when(folderHelper.findItemById("594")).thenReturn(null);
    PSContentItem item = new PSContentItem();
    item.setId("42");
    item.setType("percPage");
    item.setName("Home");
    item.setFields(new HashMap<>());
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    PSItemEditorField page = new PSItemEditorField("page", "594");
    page.setDataType("link");
    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(2);
    req.setFields(List.of(page));

    WebApplicationException missing =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), missing.getResponse().getStatus());

    when(folderHelper.findItemById("594")).thenReturn(Mockito.mock(PSPathItem.class));
    service.saveEditorFields("42", req);
    assertEquals("594", item.getFields().get("page"));
    verify(linkService, never()).createLink(anyInt(), anyInt(), anyInt(), any());
    verify(contentItemDao).save(item);
  }

  @Test
  void currentRevisionReadsLocator() {
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(7, 4));
    assertEquals(4, PSItemService.currentRevision(summary));
    assertEquals(0, PSItemService.currentRevision(null));
  }
}
