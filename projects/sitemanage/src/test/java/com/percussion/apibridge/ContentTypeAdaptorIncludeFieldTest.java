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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.PSCollection;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CD-04 include system/shared fields: POST .../fields/include persists via {@code
 * saveContentTypes} under a held design-session lock. Origin stays system/shared.
 */
@Tag("UnitTest")
class ContentTypeAdaptorIncludeFieldTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
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
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void includeSystemOrSharedField_systemRemovesExcludeAndKeepsOrigin() {
    StubType stub = stubType(List.of("sys_title"), List.of(), List.of());
    PSDisplayMapping source = mapping("sys_title", "Title:");
    PSField field =
        ContentTypeAdaptor.includeSystemOrSharedField(
            stub.def, "sys_title", PSField.TYPE_SYSTEM, source, null, null);
    assertEquals(PSField.TYPE_SYSTEM, field.getType());
    assertEquals("sys_title", field.getSubmitName());
    assertFalse(containsIgnoreCase(stub.mapper.getSystemFieldExcludes(), "sys_title"));
    assertNotNull(ContentTypeAdaptor.findDisplayMapping(stub.display, "sys_title"));
    assertEquals(PSField.TYPE_SYSTEM, stub.parent.findFieldByName("sys_title", false).getType());
  }

  @Test
  void includeSystemOrSharedField_sharedIncludesGroupAndKeepsOrigin() {
    StubType stub = stubType(List.of(), List.of(), List.of());
    PSSharedFieldGroup group = sharedGroup("shared", "displaytitle", "body");
    PSDisplayMapping source = mapping("displaytitle", "Display title:");
    PSField field =
        ContentTypeAdaptor.includeSystemOrSharedField(
            stub.def, "displaytitle", PSField.TYPE_SHARED, source, group, null);
    assertEquals(PSField.TYPE_SHARED, field.getType());
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldIncludes(), "shared"));
    assertFalse(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "displaytitle"));
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "body"));
    assertNotNull(ContentTypeAdaptor.findDisplayMapping(stub.display, "displaytitle"));
  }

  @Test
  void includeField_savesSystemWhenLockHeld() throws Exception {
    stubHeldLock();
    StubType stub = stubType(List.of("sys_title"), List.of(), List.of());
    stubLockedLoad(stub.def);
    stubSystemDef("sys_title");
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_title");
    body.setFieldType("system");
    adaptor.includeField(null, "311", body);
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(
        PSField.TYPE_SYSTEM, stub.parent.findFieldByName("sys_title", false).getType());
    verify(designWs)
        .loadContentEditorSystemDef(false, false, "test-session", "Admin");
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void includeField_savesSharedWhenLockHeld() throws Exception {
    stubHeldLock();
    StubType stub = stubType(List.of(), List.of(), List.of());
    stubLockedLoad(stub.def);
    stubSharedDef("shared", "displaytitle", "body");
    ContentTypeField body = new ContentTypeField();
    body.setName("displaytitle");
    body.setFieldType("shared");
    adaptor.includeField(null, "311", body);
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(
        PSField.TYPE_SHARED, stub.parent.findFieldByName("displaytitle", false).getType());
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldIncludes(), "shared"));
  }

  @Test
  void includeField_duplicateIs409() throws Exception {
    stubHeldLock();
    StubType stub = stubType(List.of(), List.of(), List.of());
    ContentTypeAdaptor.includeSystemOrSharedField(
        stub.def,
        "sys_title",
        PSField.TYPE_SYSTEM,
        mapping("sys_title", "Title:"),
        null,
        null);
    stubLockedLoad(stub.def);
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_title");
    body.setFieldType("system");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.includeField(null, "311", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void includeField_unknownSystemFieldIs404() throws Exception {
    stubHeldLock();
    stubLockedLoad(stubType(List.of(), List.of(), List.of()).def);
    stubSystemDef("sys_title");
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_missing");
    body.setFieldType("system");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.includeField(null, "311", body));
    assertEquals(404, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void includeField_unknownSharedFieldIs404() throws Exception {
    stubHeldLock();
    stubLockedLoad(stubType(List.of(), List.of(), List.of()).def);
    stubSharedDef("shared", "displaytitle");
    ContentTypeField body = new ContentTypeField();
    body.setName("nope");
    body.setFieldType("shared");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.includeField(null, "311", body));
    assertEquals(404, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void includeField_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_title");
    body.setFieldType("system");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.includeField(null, "311", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void includeField_unknownTypeReturnsNull() throws Exception {
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_title");
    body.setFieldType("system");
    assertNull(adaptor.includeField(null, "999", body));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void includeField_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of());
    stubLockedLoad(stubType(List.of(), List.of(), List.of()).def);
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_title");
    body.setFieldType("system");
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.includeField(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void includeField_localOriginIs400() {
    ContentTypeField body = new ContentTypeField();
    body.setName("rx_note");
    body.setFieldType("local");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.includeField(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("local"));
  }

  @Test
  void includeField_missingOriginIs400() {
    ContentTypeField body = new ContentTypeField();
    body.setName("sys_title");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.includeField(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("fieldtype"));
  }

  @Test
  void validateIncludeFieldName_allowsGroupDotField() {
    assertEquals(
        "shared.displaytitle", ContentTypeAdaptor.validateIncludeFieldName(" shared.displaytitle "));
    assertEquals("displaytitle", ContentTypeAdaptor.simpleIncludeFieldName("shared.displaytitle"));
  }

  @Test
  void validateIncludeFieldName_rejectsPath() {
    assertThrows(
        IllegalArgumentException.class, () -> ContentTypeAdaptor.validateIncludeFieldName("../x"));
    assertThrows(
        IllegalArgumentException.class, () -> ContentTypeAdaptor.validateIncludeFieldName("a/b"));
  }

  @Test
  void requireIncludeOrigin_rejectsUnknown() {
    assertEquals("system", ContentTypeAdaptor.requireIncludeOrigin("SYSTEM"));
    assertEquals("shared", ContentTypeAdaptor.requireIncludeOrigin(" shared "));
    assertThrows(IllegalArgumentException.class, () -> ContentTypeAdaptor.requireIncludeOrigin(""));
    assertThrows(
        IllegalArgumentException.class, () -> ContentTypeAdaptor.requireIncludeOrigin("other"));
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

  @SuppressWarnings("unchecked")
  private void stubSystemDef(String fieldName) throws Exception {
    PSContentEditorSystemDef sysDef = mock(PSContentEditorSystemDef.class);
    PSDisplayMapper sysDisplay = new PSDisplayMapper("sys");
    sysDisplay.add(mapping(fieldName, fieldName + ":"));
    when(sysDef.getUIDefinition()).thenReturn(new PSUIDefinition(sysDisplay));
    when(designWs.loadContentEditorSystemDef(false, false, "test-session", "Admin"))
        .thenReturn(sysDef);
  }

  @SuppressWarnings("unchecked")
  private void stubSharedDef(String groupName, String... fieldNames) throws Exception {
    PSSharedFieldGroup group = sharedGroup(groupName, fieldNames);
    PSContentEditorSharedDef sharedDef = mock(PSContentEditorSharedDef.class);
    PSCollection<PSSharedFieldGroup> coll = new PSCollection<>(PSSharedFieldGroup.class);
    coll.add(group);
    when(sharedDef.getFieldGroups()).thenAnswer(inv -> coll.iterator());
    when(designWs.loadContentEditorSharedDef(false, false, "test-session", "Admin"))
        .thenReturn(sharedDef);
  }

  @SuppressWarnings("unchecked")
  private static PSSharedFieldGroup sharedGroup(String groupName, String... fieldNames) {
    PSFieldSet set = new PSFieldSet(groupName);
    PSDisplayMapper display = new PSDisplayMapper(groupName);
    for (String name : fieldNames) {
      set.add(new PSField(PSField.TYPE_SHARED, name, null));
      display.add(mapping(name, name + ":"));
    }
    PSSharedFieldGroup group = new PSSharedFieldGroup(groupName, groupName + ".xml");
    group.setFieldSet(set);
    group.setUIDefinition(new PSUIDefinition(display));
    return group;
  }

  private static PSDisplayMapping mapping(String fieldName, String label) {
    PSUISet ui = new PSUISet();
    ui.setLabel(new PSDisplayText(label));
    return new PSDisplayMapping(fieldName, ui);
  }

  private static boolean containsIgnoreCase(Iterator<?> it, String name) {
    while (it != null && it.hasNext()) {
      Object o = it.next();
      if (o != null && o.toString().equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  private StubType stubType(
      List<String> systemExcludes, List<String> sharedIncludes, List<String> sharedExcludes) {
    PSFieldSet parent = new PSFieldSet("percPage");
    PSDisplayMapper display = new PSDisplayMapper("percPage");
    PSContentEditorMapper mapper =
        new PSContentEditorMapper(
            new ArrayList<>(systemExcludes),
            new ArrayList<>(sharedIncludes),
            parent,
            new PSUIDefinition(display));
    mapper.setSharedFieldExcludes(new ArrayList<>(sharedExcludes));
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
    when(def.getDisplayMapper("percPage")).thenReturn(display);
    when(def.getTypeTables()).thenReturn(List.of(new PSBackEndTable("PERCPAGE")));
    when(def.getContentEditor()).thenReturn(null);
    when(def.getContentEditorMapper()).thenReturn(mapper);
    return new StubType(def, parent, display, mapper);
  }

  private record StubType(
      PSItemDefinition def,
      PSFieldSet parent,
      PSDisplayMapper display,
      PSContentEditorMapper mapper) {}
}
