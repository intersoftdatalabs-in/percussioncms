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

import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.systemdef.SystemDefDesignLockException;
import com.percussion.rest.systemdef.SystemDefDetail;
import com.percussion.rest.systemdef.SystemDefFieldSummary;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.WebApplicationException;
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
    when(designWs.loadContentEditorSystemDef(true, false, "test-session", "Admin")).thenReturn(def);
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
    when(designWs.loadContentEditorSystemDef(true, false, "test-session", "Admin")).thenReturn(def);
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
    when(designWs.loadContentEditorSystemDef(true, false, "test-session", "Admin"))
        .thenThrow(lockErr);
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefDesignLockException ex =
        assertThrows(
            SystemDefDesignLockException.class,
            () -> adaptor.updateSystemDef(null, new SystemDefDetail()));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void updateSystemDef_saveLockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSystemDef def = mock(PSContentEditorSystemDef.class);
    when(def.getFieldSet()).thenReturn(null);
    when(designWs.loadContentEditorSystemDef(true, false, "test-session", "Admin")).thenReturn(def);
    doThrow(new PSLockErrorException(1, "not locked", "stack"))
        .when(designWs)
        .saveContentEditorSystemDef(any(), eq(true), eq("test-session"), eq("Admin"));
    SystemDefAdaptor adaptor = new SystemDefAdaptor(designWs, () -> true);
    SystemDefDesignLockException ex =
        assertThrows(
            SystemDefDesignLockException.class,
            () -> adaptor.updateSystemDef(null, new SystemDefDetail()));
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
}
