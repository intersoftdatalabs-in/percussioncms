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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.data.PSItemEditorBinaryMeta;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceEditorBinaryTest {

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
  @Mock private Attachment attachment;
  @Mock private PSComponentSummary summary;
  @Mock private IPSGuid itemGuid;

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
  void saveEditorBinaryRejectsMissingAttachment() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.saveEditorBinary("42", "img", null));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void saveEditorBinaryRejectsMissingContentDisposition() {
    when(attachment.getContentDisposition()).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> service.saveEditorBinary("42", "img", attachment));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void saveEditorBinaryForbiddenWhenCheckedOutToOtherUser() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCheckoutUserName()).thenReturn("other");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(false);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                service.saveEditorBinary(
                    "42",
                    "item_file_attachment",
                    new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)),
                    "note.txt",
                    "text/plain"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void saveEditorBinaryBadRequestOnNonImageForImageField() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                service.saveEditorBinary(
                    "42",
                    "img",
                    new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)),
                    "spec.pdf",
                    "application/pdf"));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void saveEditorBinaryBadRequestOnInvalidFieldName() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                service.saveEditorBinary(
                    "42",
                    "../img",
                    new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)),
                    "note.txt",
                    "text/plain"));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void downloadEditorBinaryReturnsStoredBytesAndFilenameWithoutCheckout() throws Exception {
    PSContentItem item = new PSContentItem();
    item.setId("1-101-42");
    Map<String, Object> fields = new HashMap<>();
    fields.put("item_file_attachment", "stored-bytes".getBytes(StandardCharsets.UTF_8));
    fields.put("item_file_attachment_filename", "note.txt");
    fields.put("item_file_attachment_type", "text/plain");
    item.setFields(fields);
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(systemService.getContentAssignmentTypes(any()))
        .thenReturn(List.of(PSAssignmentTypeEnum.READER));
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    Response response = service.downloadEditorBinary("1-101-42", "item_file_attachment");

    assertEquals(200, response.getStatus());
    assertArrayEquals(
        "stored-bytes".getBytes(StandardCharsets.UTF_8), (byte[]) response.getEntity());
    assertTrue(String.valueOf(response.getHeaderString("Content-Disposition")).contains("note.txt"));
  }

  @Test
  void downloadEditorBinaryNamesMissingFieldInsteadOfEmptyFile() throws Exception {
    PSContentItem item = new PSContentItem();
    item.setFields(new HashMap<>());
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(systemService.getContentAssignmentTypes(any()))
        .thenReturn(List.of(PSAssignmentTypeEnum.READER));
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> service.downloadEditorBinary("1-101-42", "item_file_attachment"));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("item_file_attachment"));
  }

  @Test
  void downloadEditorBinaryForbiddenNamesTheField() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(systemService.getContentAssignmentTypes(any()))
        .thenReturn(List.of(PSAssignmentTypeEnum.NONE));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.downloadEditorBinary("1-101-42", "img"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("img"));
  }

  @Test
  void clearEditorBinaryPersistsBlankFieldAndSiblingNames() throws Exception {
    PSContentItem item = new PSContentItem();
    item.setId("1-101-42");
    Map<String, Object> fields = new HashMap<>();
    fields.put("item_file_attachment", "stored".getBytes(StandardCharsets.UTF_8));
    fields.put("item_file_attachment_filename", "brief.pdf");
    item.setFields(fields);
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCheckoutUserName()).thenReturn("admin");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(true);
    when(idMapper.getGuid(anyString())).thenReturn(itemGuid);
    when(contentWs.prepareForEdit(nullable(IPSGuid.class))).thenReturn(null);
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);
    when(contentItemDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PSItemEditorBinaryMeta meta = service.clearEditorBinary("42", "item_file_attachment");

    assertFalse(meta.isPresent());
    assertEquals("", item.getFields().get("item_file_attachment"));
    assertEquals("", item.getFields().get("item_file_attachment_filename"));
    verify(contentItemDao).save(item);
  }

  @Test
  void clearEditorBinaryForbiddenNamesTheField() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCheckoutUserName()).thenReturn("other");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(false);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> service.clearEditorBinary("42", "item_file_attachment"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("item_file_attachment"));
  }

  @Test
  void clearEditorBinaryBadRequestOnInvalidFieldName() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.clearEditorBinary("42", "../img"));
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
  }
}
