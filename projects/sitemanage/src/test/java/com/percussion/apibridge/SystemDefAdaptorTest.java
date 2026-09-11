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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSChoices;
import com.percussion.design.objectstore.PSCommandHandlerStylesheets;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSStylesheet;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUrlRequest;
import com.percussion.rest.contenttypes.ContentTypeChoiceCatalog;
import com.percussion.rest.contenttypes.ContentTypeChoiceEntry;
import com.percussion.rest.contenttypes.ContentTypeControlProperty;
import com.percussion.rest.systemdef.SystemDefCommandHandlerStylesheet;
import com.percussion.rest.systemdef.SystemDefControlProperties;
import com.percussion.rest.systemdef.SystemDefDesignLockException;
import com.percussion.rest.systemdef.SystemDefDetail;
import com.percussion.rest.systemdef.SystemDefFieldNotFoundException;
import com.percussion.rest.systemdef.SystemDefFieldSummary;
import com.percussion.rest.systemdef.SystemDefStylesheets;
import com.percussion.util.PSCollection;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SystemDefAdaptorTest {

  @BeforeEach
  void setRequestInfo() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
  }

  @AfterEach
  void clearRequestInfo() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void toDetail_mapsFieldsAndMeta() {
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_title");
    when(field.getDataType()).thenReturn("text");
    when(field.isUserSearchable()).thenReturn(true);
    when(field.isReadOnly()).thenReturn(false);
    when(field.getOccurrenceDimension(null)).thenReturn(PSField.OCCURRENCE_DIMENSION_REQUIRED);

    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});

    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(def.getCacheTimeout()).thenReturn(30);

    SystemDefDetail detail = SystemDefAdaptor.toDetail(def);
    assertEquals(30, detail.getCacheTimeoutMinutes());
    assertEquals(1, detail.getFieldCount());
    assertEquals(1, detail.getFields().size());
    assertEquals("sys_title", detail.getFields().get(0).getName());
    assertEquals(Boolean.TRUE, detail.getFields().get(0).getRequired());
    assertEquals("required", detail.getFields().get(0).getOccurrence());
    assertNotNull(detail.getDesignGaps());
    assertFalse(detail.getDesignGaps().isEmpty());
  }

  @Test
  void toDetail_nullDefYieldsEmptyCatalog() {
    SystemDefDetail detail = SystemDefAdaptor.toDetail(null);
    assertEquals(0, detail.getFieldCount());
    assertTrue(detail.getFields().isEmpty());
    assertFalse(detail.getDesignGaps().isEmpty());
  }

  @Test
  void mapOccurrence_mapsKnownDimensions() {
    assertEquals("optional", SystemDefAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_OPTIONAL));
    assertEquals("required", SystemDefAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_REQUIRED));
    assertEquals(
        "oneOrMore", SystemDefAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE));
    assertEquals(
        "zeroOrMore", SystemDefAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE));
    assertEquals("count", SystemDefAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_COUNT));
    assertEquals("unknown", SystemDefAdaptor.mapOccurrence(-1));
  }

  @Test
  void loadSystemDefFromDesignWs_returnsDesignWsResult() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(designWs.loadContentEditorSystemDef(false, false, "sid", "admin")).thenReturn(def);

    assertSame(def, SystemDefAdaptor.loadSystemDefFromDesignWs(designWs, "sid", "admin"));
    verify(designWs).loadContentEditorSystemDef(false, false, "sid", "admin");
  }

  @Test
  void loadSystemDefFromDesignWs_wrapsPsErrorException() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSErrorException cause = new PSErrorException("ws failed");
    when(designWs.loadContentEditorSystemDef(false, false, "sid", "admin")).thenThrow(cause);

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> SystemDefAdaptor.loadSystemDefFromDesignWs(designWs, "sid", "admin"));
    assertEquals("Failed to load system def", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test
  void loadSystemDefFromDesignWs_preservesGenuineNpe() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    NullPointerException npe = new NullPointerException("design ws bug");
    when(designWs.loadContentEditorSystemDef(false, false, "sid", "admin")).thenThrow(npe);

    NullPointerException thrown =
        assertThrows(
            NullPointerException.class,
            () -> SystemDefAdaptor.loadSystemDefFromDesignWs(designWs, "sid", "admin"));
    assertSame(npe, thrown);
  }

  @Test
  void loadSystemDefFromDesignWs_retriesMissingColumnNpe() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(designWs.loadContentEditorSystemDef(false, false, "sid", "admin"))
        .thenThrow(new NullPointerException("no such column QA4030PROBE"))
        .thenReturn(def);

    assertSame(def, SystemDefAdaptor.loadSystemDefFromDesignWs(designWs, "sid", "admin"));
  }

  @Test
  void loadSystemDefFromDesignWs_passesNullSessionAndUserWhenRequestInfoAbsent() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(designWs.loadContentEditorSystemDef(eq(false), eq(false), isNull(), isNull()))
        .thenReturn(def);

    assertSame(def, SystemDefAdaptor.loadSystemDefFromDesignWs(designWs, null, null));
    verify(designWs).loadContentEditorSystemDef(false, false, null, null);
  }

  @Test
  void getSystemDef_usesInjectedLoaderFromDefaultConstructorShape() {
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getCacheTimeout()).thenReturn(0);

    SystemDefAdaptor adaptor = new SystemDefAdaptor(() -> def);
    SystemDefDetail detail = adaptor.getSystemDef(null);
    assertNotNull(detail);
    assertEquals(0, detail.getFieldCount());
  }

  @Test
  void getSystemDef_forbiddenWhenNotAdmin() {
    AtomicInteger loads = new AtomicInteger();
    SystemDefAdaptor denied =
        new SystemDefAdaptor(
            () -> {
              loads.incrementAndGet();
              return mock(PSContentEditorSystemDef.class);
            },
            () -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.getSystemDef(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(SystemDefAdaptor.ADMIN_REQUIRED, ex.getMessage());
    assertEquals(0, loads.get());
  }

  @Test
  void updateSystemDef_patchesSearchableAndSavesWithLockRelease() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_title");
    when(field.getDataType()).thenReturn("text");
    when(field.isUserSearchable()).thenReturn(true);
    when(field.isReadOnly()).thenReturn(false);
    when(field.getOccurrenceDimension(null)).thenReturn(PSField.OCCURRENCE_DIMENSION_OPTIONAL);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});
    when(set.findFieldByName("sys_title", false)).thenReturn(field);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(def.getCacheTimeout()).thenReturn(15);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    patch.setSearchable(true);
    SystemDefDetail body = new SystemDefDetail();
    body.setFields(List.of(patch));

    SystemDefDetail out = adaptor.updateSystemDef(null, body);
    assertEquals(1, out.getFieldCount());
    verify(field).setUserSearchable(true);
    verify(designWs).saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void updateSystemDef_unknownFieldIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("missing", false)).thenReturn(null);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("missing");
    SystemDefDetail body = new SystemDefDetail();
    body.setFields(List.of(patch));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateSystemDef(null, body));
    assertTrue(ex.getMessage().contains("Unknown field"));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void updateSystemDef_nullBodyIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateSystemDef(null, null));
    assertTrue(ex.getMessage().contains("body is required"));
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void updateSystemDef_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor denied = new SystemDefAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.updateSystemDef(null, new SystemDefDetail()));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void updateSystemDef_lockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLockErrorException lockErr = new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin"))
        .thenThrow(lockErr);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    SystemDefDetail body = new SystemDefDetail();
    body.setFields(List.of(patch));
    SystemDefDesignLockException ex =
        assertThrows(
            SystemDefDesignLockException.class, () -> adaptor.updateSystemDef(null, body));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void updateSystemDef_saveLockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_title");
    when(field.getDataType()).thenReturn("text");
    when(field.isUserSearchable()).thenReturn(true);
    when(field.isReadOnly()).thenReturn(false);
    when(field.getOccurrenceDimension(null)).thenReturn(PSField.OCCURRENCE_DIMENSION_OPTIONAL);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});
    when(set.findFieldByName("sys_title", false)).thenReturn(field);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(def.getCacheTimeout()).thenReturn(15);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    doThrow(new PSLockErrorException(1, "not locked", "stack"))
        .when(designWs)
        .saveContentEditorSystemDef(any(), eq(true), eq("test-session"), eq("Admin"));
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    patch.setSearchable(true);
    SystemDefDetail body = new SystemDefDetail();
    body.setFields(List.of(patch));
    SystemDefDesignLockException ex =
        assertThrows(
            SystemDefDesignLockException.class, () -> adaptor.updateSystemDef(null, body));
    assertTrue(ex.getMessage().contains("design lock required"));
  }

  @Test
  void applyFieldPatches_occurrenceWinsWhenRequiredAgrees() throws Exception {
    PSField field = mock(PSField.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("sys_title", false)).thenReturn(field);

    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    patch.setOccurrence("oneOrMore");
    patch.setRequired(true);

    SystemDefAdaptor.applyFieldPatches(set, List.of(patch));
    verify(field).setOccurrenceDimension(eq(PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE), isNull());
  }

  @Test
  void applyFieldPatches_occurrenceAndRequiredConflictIs400() {
    PSField field = mock(PSField.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("sys_title", false)).thenReturn(field);

    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    patch.setOccurrence("optional");
    patch.setRequired(true);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> SystemDefAdaptor.applyFieldPatches(set, List.of(patch)));
    assertTrue(ex.getMessage().contains("conflict"));
  }

  @Test
  void applyFieldPatches_requiredOnlyWhenOccurrenceOmitted() throws Exception {
    PSField field = mock(PSField.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("sys_title", false)).thenReturn(field);

    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    patch.setRequired(true);

    SystemDefAdaptor.applyFieldPatches(set, List.of(patch));
    verify(field).setOccurrenceDimension(eq(PSField.OCCURRENCE_DIMENSION_REQUIRED), isNull());
  }

  @Test
  void mapLockConflict_includesLocker() {
    PSLockErrorException err = new PSLockErrorException(1, "locked", "stack", "alice", 10L);
    SystemDefDesignLockException mapped = SystemDefAdaptor.mapLockConflict(err);
    assertEquals("Could not save system definition; locked by alice", mapped.getMessage());
  }

  @Test
  void addField_addsPersistableFieldAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    body.setSearchable(true);
    SystemDefDetail out = adaptor.addField(null, body);

    assertEquals(1, out.getFieldCount());
    assertEquals("sys_custom", out.getFields().get(0).getName());
    assertEquals("text", out.getFields().get(0).getDataType());
    assertEquals(Boolean.TRUE, out.getFields().get(0).getSearchable());
    verify(designWs).saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertNotNull(def.getFieldSet().getFieldByName("sys_custom"));
    assertNotNull(def.getUIDefinition().getMapping("sys_custom"));
  }

  @Test
  void addField_duplicateIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_custom");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.addField(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void addField_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor denied = new SystemDefAdaptor(designWs, () -> false);
    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.addField(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void addField_lockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLockErrorException lockErr = new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin"))
        .thenThrow(lockErr);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    SystemDefDesignLockException ex =
        assertThrows(SystemDefDesignLockException.class, () -> adaptor.addField(null, body));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void addField_invalidNameIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addField(null, body));
    assertTrue(ex.getMessage().contains("spaces"));
  }

  @Test
  void addField_nullBodyIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addField(null, null));
    assertTrue(ex.getMessage().contains("body is required"));
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void addField_invalidDataTypeIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    body.setDataType("not-a-type");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addField(null, body));
    assertTrue(ex.getMessage().contains("dataType"));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void deleteField_removesAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_custom");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    adaptor.deleteField(null, "sys_custom");

    verify(designWs).saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertNull(def.getFieldSet().getFieldByName("sys_custom"));
    assertNull(def.getUIDefinition().getMapping("sys_custom"));
  }

  @Test
  void deleteField_unknownFieldIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.deleteField(null, "missing"));
    assertTrue(ex.getMessage().contains("Unknown field"));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void deleteField_systemMandatoryIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_title");
    when(field.isSystemMandatory()).thenReturn(true);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getFieldByName("sys_title")).thenReturn(field);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.deleteField(null, "sys_title"));
    assertTrue(ex.getMessage().contains("mandatory"));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void deleteField_systemInternalIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_internal");
    when(field.isSystemMandatory()).thenReturn(false);
    when(field.isSystemInternal()).thenReturn(true);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getFieldByName("sys_internal")).thenReturn(field);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.deleteField(null, "sys_internal"));
    assertTrue(ex.getMessage().contains("internal"));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void deleteField_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor denied = new SystemDefAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.deleteField(null, "sys_custom"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteField_saveLockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_custom");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    doThrow(new PSLockErrorException(1, "not locked", "stack"))
        .when(designWs)
        .saveContentEditorSystemDef(any(), eq(true), eq("test-session"), eq("Admin"));
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefDesignLockException ex =
        assertThrows(
            SystemDefDesignLockException.class, () -> adaptor.deleteField(null, "sys_custom"));
    assertTrue(ex.getMessage().contains("design lock required"));
  }

  @Test
  void addPersistableField_isPersistableXml() {
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    SystemDefAdaptor.addPersistableField(def, body);
    org.w3c.dom.Document doc = com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    org.w3c.dom.Element xml = def.getFieldSet().toXml(doc);
    String serialized = com.percussion.xml.PSXmlDocumentBuilder.toString(xml);
    assertTrue(serialized.contains("sys_custom"));
    assertTrue(serialized.contains("SYS_CUSTOM") || serialized.contains("sys_custom"));
    assertNotNull(def.getUIDefinition().getMapping("sys_custom"));
  }

  @Test
  void hasFieldPatches_emptyIsNoOp() {
    assertFalse(SystemDefAdaptor.hasFieldPatches(null));
    assertFalse(SystemDefAdaptor.hasFieldPatches(List.of()));
    SystemDefFieldSummary blank = new SystemDefFieldSummary();
    blank.setName("  ");
    assertFalse(SystemDefAdaptor.hasFieldPatches(List.of(blank)));
    SystemDefFieldSummary named = new SystemDefFieldSummary();
    named.setName("sys_title");
    assertTrue(SystemDefAdaptor.hasFieldPatches(List.of(named)));
  }

  @Test
  void isMissingColumnFailure_recognizesH2Messages() {
    assertTrue(
        SystemDefAdaptor.isMissingColumnFailure(new RuntimeException("no such column QA4030PROBE")));
    assertTrue(
        SystemDefAdaptor.isMissingColumnFailure(
            new RuntimeException("Column \"QA4030PROBE\" not found")));
    assertTrue(
        SystemDefAdaptor.isMissingColumnFailure(
            new IllegalStateException("wrap", new SQLException("invalid column name"))));
    assertTrue(SystemDefAdaptor.isMissingColumnFailure(new SQLException("x", "42122")));
    assertTrue(SystemDefAdaptor.isMissingColumnFailure(new SQLException("x", "42S22")));
    assertTrue(
        SystemDefAdaptor.isMissingColumnFailure(
            new IllegalStateException("wrap", new SQLException("x", "42703"))));
    assertTrue(SystemDefAdaptor.isMissingColumnFailure(new SQLException("ORA-00904", "42000", 904)));
    assertTrue(SystemDefAdaptor.isMissingColumnFailure(new SQLException("invalid column", "S0001", 207)));
    assertFalse(SystemDefAdaptor.isMissingColumnFailure(new NullPointerException()));
    assertFalse(SystemDefAdaptor.isMissingColumnFailure(new RuntimeException("unrelated")));
    assertFalse(
        SystemDefAdaptor.isMissingColumnFailure(new RuntimeException("column mapping not found")));
    assertFalse(
        SystemDefAdaptor.isMissingColumnFailure(
            new RuntimeException("field column not found in schema")));
  }

  @Test
  void updateSystemDef_emptyFieldsDoesNotSave() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getCacheTimeout()).thenReturn(15);
    when(designWs.loadContentEditorSystemDef(false, false, "test-session", "Admin"))
        .thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefDetail first = adaptor.updateSystemDef(null, new SystemDefDetail());
    SystemDefDetail second = adaptor.updateSystemDef(null, new SystemDefDetail());

    assertEquals(0, first.getFieldCount());
    assertEquals(0, second.getFieldCount());
    verify(designWs, never()).loadContentEditorSystemDef(eq(true), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void updateSystemDef_secondPutAfterFirstSaveDoesNotNpe() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_title");
    when(field.getDataType()).thenReturn("text");
    when(field.isUserSearchable()).thenReturn(true);
    when(field.isReadOnly()).thenReturn(false);
    when(field.getOccurrenceDimension(null)).thenReturn(PSField.OCCURRENCE_DIMENSION_OPTIONAL);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});
    when(set.findFieldByName("sys_title", false)).thenReturn(field);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(set);
    when(def.getCacheTimeout()).thenReturn(15);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    when(designWs.loadContentEditorSystemDef(false, false, "test-session", "Admin"))
        .thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefFieldSummary patch = new SystemDefFieldSummary();
    patch.setName("sys_title");
    patch.setSearchable(true);
    SystemDefDetail body = new SystemDefDetail();
    body.setFields(List.of(patch));
    adaptor.updateSystemDef(null, body);
    SystemDefDetail after = adaptor.updateSystemDef(null, new SystemDefDetail());

    assertEquals(1, after.getFieldCount());
    verify(designWs).saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void addField_ensuresContentStatusColumn() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefColumnSchema columns = mock(SystemDefColumnSchema.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true, columns);

    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_custom");
    adaptor.addField(null, body);

    verify(columns).ensureColumn(eq("CONTENTSTATUS"), eq("SYS_CUSTOM"), eq("text"), eq("50"));
  }

  @Test
  void addField_duplicateAfterMissingColumnLoadIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_title");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin"))
        .thenThrow(new RuntimeException("no such column QA4030PROBE"))
        .thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefFieldSummary body = new SystemDefFieldSummary();
    body.setName("sys_title");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.addField(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void deleteField_missingColumnStillRemovesAndSkipsDrop() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefColumnSchema columns = mock(SystemDefColumnSchema.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("qa4030probe");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin"))
        .thenThrow(new RuntimeException("no such column QA4030PROBE"))
        .thenReturn(def);
    doThrow(new IllegalStateException("no such column QA4030PROBE"))
        .when(columns)
        .dropColumnIfPresent("CONTENTSTATUS", "QA4030PROBE");
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true, columns);

    adaptor.deleteField(null, "qa4030probe");

    verify(designWs).saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertNull(def.getFieldSet().getFieldByName("qa4030probe"));
    verify(columns).dropColumnIfPresent("CONTENTSTATUS", "QA4030PROBE");
  }

  @Test
  void deleteField_dropsContentStatusColumn() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefColumnSchema columns = mock(SystemDefColumnSchema.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_custom");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true, columns);

    adaptor.deleteField(null, "sys_custom");

    verify(columns).dropColumnIfPresent("CONTENTSTATUS", "SYS_CUSTOM");
  }

  @Test
  void deleteField_genuineDropFailureDoesNotSave() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefColumnSchema columns = mock(SystemDefColumnSchema.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_custom");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    doThrow(new IllegalStateException("permissions denied"))
        .when(columns)
        .dropColumnIfPresent("CONTENTSTATUS", "SYS_CUSTOM");
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true, columns);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.deleteField(null, "sys_custom"));
    assertTrue(ex.getMessage().contains("Failed to drop backend column"));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void validateFieldName_rejectsInvalid() {
    assertEquals("sys_custom", SystemDefAdaptor.validateFieldName("sys_custom"));
    assertThrows(IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName(" "));
    assertThrows(
        IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("has space"));
    assertThrows(IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("a/b"));
    assertThrows(IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("a\\b"));
    assertThrows(IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("a..b"));
    assertThrows(
        IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("a\0b"));
    assertThrows(IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("1start"));
    assertThrows(
        IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("SELECT"));
    assertThrows(IllegalArgumentException.class, () -> SystemDefAdaptor.validateFieldName("user"));
  }

  @Test
  void getFieldControlProperties_returnsValuesAndChoices() {
    PSContentEditorSystemDef def = defWithControlAndChoices("sys_title");
    SystemDefAdaptor adaptor = new SystemDefAdaptor(() -> def);

    SystemDefControlProperties out = adaptor.getFieldControlProperties(null, "sys_title");
    assertEquals("sys_title", out.getFieldName());
    assertEquals("sys_EditBox", out.getControl());
    assertEquals(1, out.getProperties().size());
    assertEquals("height", out.getProperties().get(0).getName());
    assertEquals("200", out.getProperties().get(0).getValue());
    assertEquals("local", out.getChoices().getType());
    assertEquals("open", out.getChoices().getEntries().get(0).getValue());
    assertEquals(1, out.getDesignGaps().size());
    assertEquals("SYS_APP_FLOW", out.getDesignGaps().get(0).getCode());
  }

  @Test
  void getFieldControlProperties_unknownFieldIs404() {
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_title");
    SystemDefAdaptor.addPersistableField(def, existing);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(() -> def);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getFieldControlProperties(null, "nope"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void getFieldControlProperties_unsafeNameIs404() {
    SystemDefAdaptor adaptor = new SystemDefAdaptor(() -> newSystemDefWithEmptyFields());
    WebApplicationException slash =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getFieldControlProperties(null, "a/b"));
    assertEquals(404, slash.getResponse().getStatus());
    WebApplicationException dots =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getFieldControlProperties(null, ".."));
    assertEquals(404, dots.getResponse().getStatus());
  }

  @Test
  void getFieldControlProperties_forbiddenWhenNotAdmin() {
    AtomicInteger loads = new AtomicInteger();
    SystemDefAdaptor denied =
        new SystemDefAdaptor(
            () -> {
              loads.incrementAndGet();
              return mock(PSContentEditorSystemDef.class);
            },
            () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.getFieldControlProperties(null, "sys_title"));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(0, loads.get());
  }

  @Test
  void replaceFieldControlProperties_persistsValuesAndReleasesLock() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithControlAndChoices("sys_title");
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));
    ContentTypeChoiceCatalog choices = new ContentTypeChoiceCatalog();
    choices.setType("local");
    choices.setEntries(List.of(new ContentTypeChoiceEntry("closed", "Closed")));
    body.setChoices(choices);

    SystemDefControlProperties out =
        adaptor.replaceFieldControlProperties(null, "sys_title", body);

    verify(designWs)
        .saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertEquals("640", out.getProperties().get(0).getValue());
    assertTrue(
        out.getDesignGaps().stream().anyMatch(g -> "SYS_APP_FLOW".equals(g.getCode())),
        () -> String.valueOf(out.getDesignGaps()));
    assertTrue(
        out.getDesignGaps().stream().noneMatch(g -> "SYS_STYLESHEET".equals(g.getCode())),
        () -> String.valueOf(out.getDesignGaps()));
    assertEquals("closed", out.getChoices().getEntries().get(0).getValue());
    PSControlRef control = def.getUIDefinition().getMapping("sys_title").getUISet().getControl();
    PSParam first = (PSParam) control.getParameters().next();
    assertEquals("width", first.getName());
    assertEquals("640", first.getValue().getValueText());
  }

  @Test
  void replaceFieldControlProperties_emptyPropertiesClears() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithControlAndChoices("sys_title");
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of());
    SystemDefControlProperties out =
        adaptor.replaceFieldControlProperties(null, "sys_title", body);

    assertTrue(out.getProperties().isEmpty());
    PSControlRef control = def.getUIDefinition().getMapping("sys_title").getUISet().getControl();
    assertTrue(!control.getParameters().hasNext(), "expected empty control parameters");
  }

  @Test
  void replaceFieldControlProperties_omittedChoicesLeaveCatalog() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithControlAndChoices("sys_title");
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("height", "12")));
    SystemDefControlProperties out =
        adaptor.replaceFieldControlProperties(null, "sys_title", body);

    assertEquals("local", out.getChoices().getType());
    assertEquals("open", out.getChoices().getEntries().get(0).getValue());
  }

  @Test
  void replaceFieldControlProperties_unknownFieldIs404() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName("sys_title");
    SystemDefAdaptor.addPersistableField(def, existing);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of());
    assertThrows(
        SystemDefFieldNotFoundException.class,
        () -> adaptor.replaceFieldControlProperties(null, "nope", body));
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void replaceFieldControlProperties_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor denied = new SystemDefAdaptor(designWs, () -> false);
    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.replaceFieldControlProperties(null, "sys_title", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void replaceFieldControlProperties_lockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLockErrorException lockErr =
        new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin"))
        .thenThrow(lockErr);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of());
    SystemDefDesignLockException ex =
        assertThrows(
            SystemDefDesignLockException.class,
            () -> adaptor.replaceFieldControlProperties(null, "sys_title", body));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void replaceFieldControlProperties_missingPropertiesIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefControlProperties body = new SystemDefControlProperties();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldControlProperties(null, "sys_title", body));
    assertTrue(ex.getMessage().contains("properties"));
  }

  @Test
  void replaceFieldControlProperties_blankFieldNameIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefControlProperties body = new SystemDefControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));
    IllegalArgumentException blank =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldControlProperties(null, "  ", body));
    assertTrue(blank.getMessage().contains("name is required"));
  }

  @Test
  void getStylesheets_mapsDefaultHref() {
    PSContentEditorSystemDef def = defWithStylesheets();
    SystemDefAdaptor adaptor = new SystemDefAdaptor(() -> def);
    SystemDefStylesheets out = adaptor.getStylesheets(null);
    assertEquals(1, out.getHandlers().size());
    assertEquals("preview", out.getHandlers().get(0).getCommandHandler());
    assertEquals(
        "file:../sys_resources/stylesheets/activeEdit.xsl",
        out.getHandlers().get(0).getHref());
    assertEquals(1, out.getDesignGaps().size());
    assertEquals("SYS_APP_FLOW", out.getDesignGaps().get(0).getCode());
  }

  @Test
  void getStylesheets_forbiddenWhenNotAdmin() {
    AtomicInteger loads = new AtomicInteger();
    SystemDefAdaptor denied =
        new SystemDefAdaptor(
            () -> {
              loads.incrementAndGet();
              return mock(PSContentEditorSystemDef.class);
            },
            () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.getStylesheets(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(0, loads.get());
  }

  @Test
  void replaceStylesheets_persistsHrefAndReleasesLock() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithStylesheets();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefStylesheets body = new SystemDefStylesheets();
    SystemDefCommandHandlerStylesheet row = new SystemDefCommandHandlerStylesheet();
    row.setCommandHandler("preview");
    row.setHref("file:../sys_resources/stylesheets/contentEdit.xsl");
    body.setHandlers(List.of(row));

    SystemDefStylesheets out = adaptor.replaceStylesheets(null, body);
    verify(designWs)
        .saveContentEditorSystemDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertEquals("file:../sys_resources/stylesheets/contentEdit.xsl", out.getHandlers().get(0).getHref());
    assertEquals(
        "file:../sys_resources/stylesheets/contentEdit.xsl",
        def.getStyleSheetSet().getDefaultStylesheet("preview").getRequest().getHref());
  }

  @Test
  void replaceStylesheets_addsAndRemovesHandler() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithStylesheets();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefStylesheets addBody = new SystemDefStylesheets();
    addBody.setHandlers(
        List.of(
            handler("preview", "file:../sys_resources/stylesheets/activeEdit.xsl"),
            handler("qa4452", "file:../rx_resources/stylesheets/qa4452.xsl")));
    SystemDefStylesheets added = adaptor.replaceStylesheets(null, addBody);
    assertEquals(2, added.getHandlers().size());
    assertTrue(
        added.getHandlers().stream().anyMatch(h -> "qa4452".equals(h.getCommandHandler())));

    SystemDefStylesheets removeBody = new SystemDefStylesheets();
    removeBody.setHandlers(
        List.of(handler("preview", "file:../sys_resources/stylesheets/activeEdit.xsl")));
    SystemDefStylesheets removed = adaptor.replaceStylesheets(null, removeBody);
    assertEquals(1, removed.getHandlers().size());
    assertEquals("preview", removed.getHandlers().get(0).getCommandHandler());
    assertNull(def.getStyleSheetSet().getDefaultStylesheet("qa4452"));
  }

  @Test
  void replaceStylesheets_blankHrefRemovesHandler() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithStylesheets();
    def.getStyleSheetSet()
        .setDefaultStylesheet(
            "qa4452",
            new PSStylesheet(
                new PSUrlRequest(
                    null,
                    "file:../sys_resources/stylesheets/singleFieldEdit.xsl",
                    new PSCollection(PSParam.class))));
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefStylesheets body = new SystemDefStylesheets();
    body.setHandlers(
        List.of(
            handler("preview", "file:../sys_resources/stylesheets/activeEdit.xsl"),
            handler("qa4452", "")));
    SystemDefStylesheets out = adaptor.replaceStylesheets(null, body);
    assertEquals(1, out.getHandlers().size());
    assertEquals("preview", out.getHandlers().get(0).getCommandHandler());
  }

  @Test
  void replaceStylesheets_emptyKeepIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithStylesheets();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefStylesheets body = new SystemDefStylesheets();
    body.setHandlers(List.of());
    IllegalArgumentException empty =
        assertThrows(IllegalArgumentException.class, () -> adaptor.replaceStylesheets(null, body));
    assertTrue(empty.getMessage().contains("At least one"));
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void replaceStylesheets_invalidHrefIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = defWithStylesheets();
    when(designWs.loadContentEditorSystemDef(true, true, "test-session", "Admin")).thenReturn(def);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);

    SystemDefStylesheets body = new SystemDefStylesheets();
    body.setHandlers(List.of(handler("preview", "https://evil.example/x.xsl")));
    assertThrows(IllegalArgumentException.class, () -> adaptor.replaceStylesheets(null, body));
    verify(designWs, never()).loadContentEditorSystemDef(anyBoolean(), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentEditorSystemDef(any(), anyBoolean(), any(), any());
  }

  @Test
  void replaceStylesheets_forbiddenWhenNotAdmin() {
    SystemDefAdaptor denied = new SystemDefAdaptor(() -> defWithStylesheets(), () -> false);
    SystemDefStylesheets body = new SystemDefStylesheets();
    body.setHandlers(List.of(handler("preview", "file:../sys_resources/stylesheets/activeEdit.xsl")));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.replaceStylesheets(null, body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void requireSafeStylesheetHref_acceptsWorkbenchDefaults() {
    assertEquals(
        "file:../sys_resources/stylesheets/activeEdit.xsl",
        SystemDefAdaptor.requireSafeStylesheetHref(
            "file:../sys_resources/stylesheets/activeEdit.xsl"));
    assertEquals(
        "file:../rx_resources/stylesheets/custom.xsl",
        SystemDefAdaptor.requireSafeStylesheetHref("file:../rx_resources/stylesheets/custom.xsl"));
  }

  @Test
  void requireSafeStylesheetHref_rejectsTraversalAndSchemes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemDefAdaptor.requireSafeStylesheetHref("file:../sys_resources/stylesheets/../x.xsl"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemDefAdaptor.requireSafeStylesheetHref("file:/C:/temp/x.xsl"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemDefAdaptor.requireSafeStylesheetHref("http://example/x.xsl"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemDefAdaptor.requireSafeStylesheetHref("file:../sys_resources/stylesheets/x.xslt"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SystemDefAdaptor.requireSafeStylesheetHref(
                "file:../sys_resources/stylesheets/x\\y.xsl"));
  }

  @Test
  void isSafeFieldName_rejectsPathInjection() {
    assertTrue(SystemDefAdaptor.isSafeFieldName("sys_title"));
    assertFalse(SystemDefAdaptor.isSafeFieldName("a/b"));
    assertFalse(SystemDefAdaptor.isSafeFieldName("a\\b"));
    assertFalse(SystemDefAdaptor.isSafeFieldName(".."));
    assertFalse(SystemDefAdaptor.isSafeFieldName("a\0b"));
    assertFalse(SystemDefAdaptor.isSafeFieldName(" "));
  }

  /**
   * Mocked system def with a real empty field set and display mapper so persist/remove can run
   * without loading ContentEditorSystemDef XML.
   */
  private static PSContentEditorSystemDef newSystemDefWithEmptyFields() {
    PSFieldSet fieldSet = new PSFieldSet("systemFieldset");
    PSUIDefinition ui = new PSUIDefinition(new PSDisplayMapper("systemFieldset"));
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(fieldSet);
    when(def.getUIDefinition()).thenReturn(ui);
    when(def.getContainerLocator()).thenReturn(null);
    when(def.getCacheTimeout()).thenReturn(15);
    return def;
  }

  private static PSContentEditorSystemDef defWithStylesheets() {
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    PSUrlRequest request =
        new PSUrlRequest(
            null,
            "file:../sys_resources/stylesheets/activeEdit.xsl",
            new PSCollection(PSParam.class));
    PSCommandHandlerStylesheets sheets =
        new PSCommandHandlerStylesheets("preview", new PSStylesheet(request));
    when(def.getStyleSheetSet()).thenReturn(sheets);
    return def;
  }

  private static SystemDefCommandHandlerStylesheet handler(String name, String href) {
    SystemDefCommandHandlerStylesheet row = new SystemDefCommandHandlerStylesheet();
    row.setCommandHandler(name);
    row.setHref(href);
    return row;
  }

  private static PSContentEditorSystemDef defWithControlAndChoices(String fieldName) {
    PSContentEditorSystemDef def = newSystemDefWithEmptyFields();
    SystemDefFieldSummary existing = new SystemDefFieldSummary();
    existing.setName(fieldName);
    SystemDefAdaptor.addPersistableField(def, existing);
    PSDisplayMapping mapping = def.getUIDefinition().getMapping(fieldName);
    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("height", new PSTextLiteral("200")));
    mapping.getUISet().getControl().setParameters(params);
    PSCollection local = new PSCollection(PSEntry.class);
    local.add(new PSEntry("open", new PSDisplayText("Open")));
    mapping.getUISet().setChoices(new PSChoices(local));
    return def;
  }
}
