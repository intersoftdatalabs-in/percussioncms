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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.IPSWebserviceErrors;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * CD-03 local field create/delete: POST/DELETE persist via {@code saveContentTypes} under a held
 * design-session lock.
 */
@Tag("UnitTest")
class ContentTypeAdaptorLocalFieldTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeLocalFieldColumnSchema columnSchema;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    columnSchema = mock(ContentTypeLocalFieldColumnSchema.class);
    adaptor =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true, columnSchema);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void addPersistableLocalField_addsFieldAndMapping() {
    PSItemDefinition def = stubDefinition();
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    body.setSearchable(true);
    PSField field = ContentTypeAdaptor.addPersistableLocalField(def, body);
    assertEquals("rx_note", field.getSubmitName());
    assertEquals(PSField.TYPE_LOCAL, field.getType());
    assertEquals(PSField.DT_TEXT, field.getDataType());
    assertTrue(field.isUserSearchable());
    assertNotNull(def.getFieldSet().findFieldByName("rx_note", false));
    assertNotNull(
        ContentTypeAdaptor.findDisplayMapping(def.getDisplayMapper("percPage"), "rx_note"));
  }

  @Test
  void addPersistableLocalField_namedChildCreatesComplexChild() {
    PSItemDefinition def = stubDefinition();
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_child_note");
    body.setFieldSet("childset");
    ContentTypeAdaptor.addPersistableLocalField(def, body);
    Object nested = def.getFieldSet().get("childset");
    assertTrue(nested instanceof PSFieldSet);
    PSFieldSet child = (PSFieldSet) nested;
    assertEquals(PSFieldSet.TYPE_COMPLEX_CHILD, child.getType());
    assertNotNull(child.findFieldByName("rx_child_note", false));
    assertNotNull(
        ContentTypeAdaptor.findDisplayMapping(def.getDisplayMapper("percPage"), "childset"));
  }

  @Test
  void addLocalField_savesWhenLockHeld() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    stubLockedLoad(def);
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    adaptor.addLocalField(null, "311", body);
    InOrder order = inOrder(columnSchema, designWs);
    order
        .verify(columnSchema)
        .ensureColumn(eq("PERCPAGE"), eq("RX_NOTE"), eq("text"), eq("50"));
    order.verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertNotNull(def.getFieldSet().findFieldByName("rx_note", false));
  }

  @Test
  void addLocalField_duplicateIs409() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    ContentTypeField existing = new ContentTypeField();
    existing.setName("rx_note");
    ContentTypeAdaptor.addPersistableLocalField(def, existing);
    stubLockedLoad(def);
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.addLocalField(null, "311", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(columnSchema, never()).ensureColumn(any(), any(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void addLocalField_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.addLocalField(null, "311", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void addLocalField_unknownTypeReturnsNull() throws Exception {
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    assertNull(adaptor.addLocalField(null, "999", body));
    verify(columnSchema, never()).ensureColumn(any(), any(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void addLocalField_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of());
    stubDefinition();
    stubLockedLoad(stubDefinition());
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.addLocalField(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(columnSchema, never()).ensureColumn(any(), any(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void addLocalField_invalidNameIs400() {
    ContentTypeField body = new ContentTypeField();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addLocalField(null, "311", body));
    assertTrue(ex.getMessage().contains("spaces"));
  }

  @Test
  void addLocalField_nonLocalTypeIs400() {
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    body.setFieldType("shared");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addLocalField(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("local"));
  }

  @Test
  void addLocalField_invalidDataTypeIs400() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    stubLockedLoad(def);
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    body.setDataType("not-a-type");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addLocalField(null, "311", body));
    assertTrue(ex.getMessage().contains("dataType"));
    verify(columnSchema, never()).ensureColumn(any(), any(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void addLocalField_doesNotSaveWhenAlterFails() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    stubLockedLoad(def);
    doThrow(new IllegalStateException("Local-field column DDL failed"))
        .when(columnSchema)
        .ensureColumn(eq("PERCPAGE"), eq("RX_NOTE"), eq("text"), eq("50"));
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.addLocalField(null, "311", body));
    assertTrue(ex.getMessage().contains("Failed to add local field"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void addLocalField_saveFailureIncludesErrorDetails() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    stubLockedLoad(def);
    PSErrorsException saveFailed = new PSErrorsException();
    saveFailed.addError(
        guid,
        new PSErrorException(
            IPSWebserviceErrors.SAVE_FAILED,
            "PSSystemValidationException: field mapping invalid",
            "stack"));
    doThrow(saveFailed)
        .when(designWs)
        .saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    Exception ex =
        assertThrows(Exception.class, () -> adaptor.addLocalField(null, "311", body));
    assertTrue(ex.getMessage().contains("field mapping invalid"), ex.getMessage());
  }

  @Test
  void deleteLocalField_removesAndSaves() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    ContentTypeField existing = new ContentTypeField();
    existing.setName("rx_note");
    ContentTypeAdaptor.addPersistableLocalField(def, existing);
    stubLockedLoad(def);
    Boolean out = adaptor.deleteLocalField(null, "311", "rx_note");
    assertEquals(Boolean.TRUE, out);
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertNull(def.getFieldSet().findFieldByName("rx_note", false));
    assertNull(ContentTypeAdaptor.findDisplayMapping(def.getDisplayMapper("percPage"), "rx_note"));
  }

  @Test
  void deleteLocalField_unknownFieldIs404() throws Exception {
    stubHeldLock();
    stubLockedLoad(stubDefinition());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.deleteLocalField(null, "311", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void deleteLocalField_systemFieldIs400() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    PSField system = new PSField(PSField.TYPE_SYSTEM, "sys_title", null);
    def.getFieldSet().add(system);
    stubLockedLoad(def);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.deleteLocalField(null, "311", "sys_title"));
    assertTrue(ex.getMessage().toLowerCase().contains("local"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void deleteLocalField_unknownTypeReturnsNull() throws Exception {
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());
    assertNull(adaptor.deleteLocalField(null, "999", "rx_note"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void deleteLocalField_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.deleteLocalField(null, "311", "rx_note"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteLocalField_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of());
    stubLockedLoad(stubDefinition());
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.deleteLocalField(null, "311", "rx_note"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void physicalTableName_prefersTableThenAlias() {
    PSBackEndTable aliasOnly = new PSBackEndTable("PERCPAGE");
    assertEquals("PERCPAGE", ContentTypeAdaptor.physicalTableName(aliasOnly));
    aliasOnly.setTable("CT_PAGE");
    assertEquals("CT_PAGE", ContentTypeAdaptor.physicalTableName(aliasOnly));
  }

  @Test
  void deleteLocalField_saveFailureIncludesErrorDetails() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    ContentTypeField existing = new ContentTypeField();
    existing.setName("rx_note");
    ContentTypeAdaptor.addPersistableLocalField(def, existing);
    stubLockedLoad(def);
    PSErrorsException saveFailed = new PSErrorsException();
    saveFailed.addError(
        guid,
        new PSErrorException(
            IPSWebserviceErrors.SAVE_FAILED,
            "PSSystemValidationException: field mapping invalid",
            "stack"));
    doThrow(saveFailed)
        .when(designWs)
        .saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.deleteLocalField(null, "311", "rx_note"));
    assertTrue(ex.getMessage().contains("Could not delete local field"), ex.getMessage());
    assertTrue(ex.getMessage().contains("field mapping invalid"), ex.getMessage());
  }

  @Test
  void deleteLocalField_nonValidationSaveFailureIncludesErrorDetails() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubDefinition();
    ContentTypeField existing = new ContentTypeField();
    existing.setName("rx_note");
    ContentTypeAdaptor.addPersistableLocalField(def, existing);
    stubLockedLoad(def);
    PSErrorsException saveFailed = new PSErrorsException();
    saveFailed.addError(
        guid,
        new PSErrorException(IPSWebserviceErrors.SAVE_FAILED, "java.io.IOException: disk", "stack"));
    doThrow(saveFailed)
        .when(designWs)
        .saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> adaptor.deleteLocalField(null, "311", "rx_note"));
    assertTrue(ex.getMessage().contains("Failed to save local field delete"), ex.getMessage());
    assertTrue(ex.getMessage().contains("java.io.IOException: disk"), ex.getMessage());
  }

  @Test
  void validateFieldName_rejectsPathAndLeadingDigit() {
    assertThrows(IllegalArgumentException.class, () -> ContentTypeAdaptor.validateFieldName("../x"));
    assertThrows(IllegalArgumentException.class, () -> ContentTypeAdaptor.validateFieldName("1abc"));
    assertEquals("rx_note", ContentTypeAdaptor.validateFieldName(" rx_note "));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private void stubLockedLoad(PSItemDefinition def) throws Exception {
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(def));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
  }

  private PSItemDefinition stubDefinition() {
    PSFieldSet parent = new PSFieldSet("percPage");
    PSDisplayMapper mapper = new PSDisplayMapper("percPage");
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getLabel()).thenReturn("Page");
    when(def.getDescription()).thenReturn("");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_cePage");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_cePage/percPage.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(parent);
    when(def.getComplexChildren()).thenReturn(List.of());
    when(def.getDisplayMapper("percPage")).thenReturn(mapper);
    when(def.getTypeTables()).thenReturn(List.of(new PSBackEndTable("PERCPAGE")));
    when(def.getContentEditor()).thenReturn(null);
    return def;
  }
}
