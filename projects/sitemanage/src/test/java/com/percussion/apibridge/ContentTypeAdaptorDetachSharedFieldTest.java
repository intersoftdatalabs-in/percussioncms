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
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
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
 * CD-04 detach one shared field: DELETE .../fields/include/{field} persists via {@code
 * saveContentTypes} under a held design-session lock. The shared group stays included so other
 * shared fields on the type are untouched; the named field returns to the shared-field excludes
 * and loses its display mapping. Origin-local fields stay (local delete is CD-03); system fields
 * cannot be detached.
 */
@Tag("UnitTest")
class ContentTypeAdaptorDetachSharedFieldTest {

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
  void detachSharedFieldAndMapping_excludesFieldAndRemovesMapping() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    assertTrue(ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "displaytitle"));
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "displaytitle"));
    assertFalse(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "body"));
    assertTrue(
        containsIgnoreCase(stub.mapper.getSharedFieldIncludes(), "shared"),
        "detach must keep the group included for the remaining shared fields");
    assertNull(
        ContentTypeAdaptor.findDisplayMapping(stub.display, "displaytitle"),
        "detached field must lose its display mapping");
  }

  @Test
  @SuppressWarnings("unchecked")
  void detachSharedFieldAndMapping_localIs400() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    stub.parent.add(new PSField(PSField.TYPE_LOCAL, "rx_note", null));
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "rx_note"));
    assertTrue(ex.getMessage().toLowerCase().contains("shared"), ex.getMessage());
  }

  @Test
  @SuppressWarnings("unchecked")
  void detachSharedFieldAndMapping_systemIs400() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    stub.parent.add(new PSField(PSField.TYPE_SYSTEM, "sys_title", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "sys_title"));
  }

  @Test
  void detachSharedFieldAndMapping_unknownReturnsFalse() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    assertFalse(ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "nope"));
  }

  @Test
  void detachSharedFieldAndMapping_alreadyExcludedReturnsFalse() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    assertTrue(ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "displaytitle"));
    assertFalse(
        ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "displaytitle"),
        "re-detach after the mapping is gone is 404, not success");
  }

  @Test
  void detachSharedFieldAndMapping_excludedMappingReturnsFalse() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    ContentTypeAdaptor.addSharedFieldExclude(stub.mapper, "body");
    assertFalse(
        ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "body"),
        "excluded sibling with a stale mapping is unknown, not detached");
  }

  @Test
  @SuppressWarnings("unchecked")
  void detachSharedFieldAndMapping_staleFieldsetEntryIsRemoved() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    stub.parent.add(new PSField(PSField.TYPE_SHARED, "displaytitle", null));
    assertTrue(ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "displaytitle"));
    assertNull(
        stub.parent.findFieldByName("displaytitle", false),
        "stale type-fieldset entry must go or the save collides with the group child set");
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "displaytitle"));
    assertNull(ContentTypeAdaptor.findDisplayMapping(stub.display, "displaytitle"));
  }

  @Test
  void detachSharedFieldAndMapping_groupQualifiedNameDetaches() {    StubType stub = stubTypeWithShared("displaytitle", "body");
    assertTrue(
        ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "shared.displaytitle"));
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "displaytitle"));
    assertNull(ContentTypeAdaptor.findDisplayMapping(stub.display, "displaytitle"));
  }

  @Test
  void detachSharedFieldAndMapping_blankNameIs400() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    assertThrows(
        IllegalArgumentException.class,
        () -> ContentTypeAdaptor.detachSharedFieldAndMapping(stub.def, "has space"));
  }

  @Test
  void addSharedFieldExclude_isIdempotent() {
    StubType stub = stubTypeWithShared("displaytitle", "body");
    ContentTypeAdaptor.addSharedFieldExclude(stub.mapper, "displaytitle");
    ContentTypeAdaptor.addSharedFieldExclude(stub.mapper, "DISPLAYTITLE");
    int count = 0;
    for (Iterator<?> it = stub.mapper.getSharedFieldExcludes(); it.hasNext(); ) {
      if ("displaytitle".equalsIgnoreCase(String.valueOf(it.next()))) {
        count++;
      }
    }
    assertEquals(1, count, "re-detach must not duplicate the exclude entry");
  }

  @Test
  void detachSharedField_savesWhenLockHeld() throws Exception {
    stubHeldLock();
    StubType stub = stubTypeWithShared("displaytitle", "body");
    stubLockedLoad(stub.def);
    assertEquals(Boolean.TRUE, adaptor.detachSharedField(null, "311", "displaytitle"));
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertTrue(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "displaytitle"));
    assertFalse(containsIgnoreCase(stub.mapper.getSharedFieldExcludes(), "body"));
    assertNull(ContentTypeAdaptor.findDisplayMapping(stub.display, "displaytitle"));
  }

  @Test
  void detachSharedField_unknownFieldIs404() throws Exception {
    stubHeldLock();
    StubType stub = stubTypeWithShared("displaytitle", "body");
    stubLockedLoad(stub.def);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.detachSharedField(null, "311", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void detachSharedField_localFieldIs400() throws Exception {
    stubHeldLock();
    StubType stub = stubTypeWithShared("displaytitle", "body");
    stub.parent.add(new PSField(PSField.TYPE_LOCAL, "rx_note", null));
    stubLockedLoad(stub.def);
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.detachSharedField(null, "311", "rx_note"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void detachSharedField_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of());
    stubLockedLoad(stubTypeWithShared("displaytitle", "body").def);
    assertThrows(
        ContentTypeDesignLockException.class,
        () -> adaptor.detachSharedField(null, "311", "displaytitle"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void detachSharedField_unknownTypeReturnsNull() throws Exception {
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());
    assertNull(adaptor.detachSharedField(null, "999", "displaytitle"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void detachSharedField_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.detachSharedField(null, "311", "x"));
    assertEquals(403, ex.getResponse().getStatus());
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

  private static boolean containsIgnoreCase(Iterator<?> it, String name) {
    while (it != null && it.hasNext()) {
      Object o = it.next();
      if (o != null && o.toString().equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Type with one included shared field ({@code included}) plus one sibling shared field ({@code
   * sibling}) from group {@code shared}: group included, neither field excluded, both mapped.
   */
  @SuppressWarnings("unchecked")
  private StubType stubTypeWithShared(String included, String sibling) {
    PSFieldSet parent = new PSFieldSet("percPage");
    PSDisplayMapper display = new PSDisplayMapper("percPage");
    display.add(mapping(included, included + ":"));
    display.add(mapping(sibling, sibling + ":"));
    PSContentEditorMapper mapper =
        new PSContentEditorMapper(
            new ArrayList<>(), new ArrayList<>(List.of("shared")), parent,
            new PSUIDefinition(display));
    mapper.setSharedFieldExcludes(new ArrayList<>());
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

  private static PSDisplayMapping mapping(String fieldName, String label) {
    PSUISet ui = new PSUISet();
    ui.setLabel(new PSDisplayText(label));
    return new PSDisplayMapping(fieldName, ui);
  }

  private record StubType(
      PSItemDefinition def,
      PSFieldSet parent,
      PSDisplayMapper display,
      PSContentEditorMapper mapper) {}
}
