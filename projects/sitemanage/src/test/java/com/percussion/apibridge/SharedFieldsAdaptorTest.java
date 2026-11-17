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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.PSChoices;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.rest.contenttypes.ContentTypeChoiceCatalog;
import com.percussion.rest.contenttypes.ContentTypeChoiceEntry;
import com.percussion.rest.contenttypes.ContentTypeControlProperty;
import com.percussion.rest.sharedfields.SharedFieldControlProperties;
import com.percussion.rest.sharedfields.SharedFieldDesignLockException;
import com.percussion.rest.sharedfields.SharedFieldGroupDetail;
import com.percussion.rest.sharedfields.SharedFieldGroupSummary;
import com.percussion.rest.sharedfields.SharedFieldNotFoundException;
import com.percussion.rest.sharedfields.SharedFieldSummary;
import com.percussion.rest.sharedfields.SharedFieldUsage;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.PSCollection;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class SharedFieldsAdaptorTest {

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
  void mapSummaries_mapsNameFilenameAndFieldCount() {
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("rx_title");
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});

    PSSharedFieldGroup group = mock(PSSharedFieldGroup.class);
    when(group.getName()).thenReturn("shared");
    when(group.getFilename()).thenReturn("shared.xml");
    when(group.getFieldSet()).thenReturn(set);

    PSContentEditorSharedDef def = mock(PSContentEditorSharedDef.class);
    PSCollection<PSSharedFieldGroup> coll = new PSCollection<>(PSSharedFieldGroup.class);
    coll.add(group);
    when(def.getFieldGroups()).thenReturn(coll.iterator());

    List<SharedFieldGroupSummary> out = SharedFieldsAdaptor.mapSummaries(def);
    assertEquals(1, out.size());
    assertEquals("shared", out.get(0).getName());
    assertEquals("shared.xml", out.get(0).getFilename());
    assertEquals(1, out.get(0).getFieldCount());
  }

  @Test
  void toDetail_mapsFieldsAndGaps() {
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("rx_title");
    when(field.getDataType()).thenReturn("text");
    when(field.isUserSearchable()).thenReturn(true);
    when(field.isReadOnly()).thenReturn(false);
    when(field.getOccurrenceDimension(null)).thenReturn(PSField.OCCURRENCE_DIMENSION_REQUIRED);

    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});

    PSSharedFieldGroup group = mock(PSSharedFieldGroup.class);
    when(group.getName()).thenReturn("shared");
    when(group.getFilename()).thenReturn("shared.xml");
    when(group.getFieldSet()).thenReturn(set);

    SharedFieldGroupDetail detail = SharedFieldsAdaptor.toDetail(group);
    assertEquals("shared", detail.getName());
    assertEquals(1, detail.getFields().size());
    assertEquals("rx_title", detail.getFields().get(0).getName());
    assertEquals("text", detail.getFields().get(0).getDataType());
    assertEquals(Boolean.TRUE, detail.getFields().get(0).getRequired());
    assertEquals("required", detail.getFields().get(0).getOccurrence());
    assertNotNull(detail.getDesignGaps());
    assertFalse(detail.getDesignGaps().isEmpty());
  }

  @Test
  void findGroup_isCaseInsensitive() {
    PSSharedFieldGroup group = mock(PSSharedFieldGroup.class);
    when(group.getName()).thenReturn("SharedGroup");
    PSContentEditorSharedDef def = mock(PSContentEditorSharedDef.class);
    PSCollection<PSSharedFieldGroup> coll = new PSCollection<>(PSSharedFieldGroup.class);
    coll.add(group);
    when(def.getFieldGroups()).thenReturn(coll.iterator());

    assertEquals(group, SharedFieldsAdaptor.findGroup(def, "sharedgroup"));
    assertNull(SharedFieldsAdaptor.findGroup(def, "missing"));
  }

  @Test
  void isSafeGroupName_rejectsPathTraversal() {
    assertTrue(SharedFieldsAdaptor.isSafeGroupName("shared"));
    assertFalse(SharedFieldsAdaptor.isSafeGroupName("../etc"));
    assertFalse(SharedFieldsAdaptor.isSafeGroupName("a/b"));
    assertFalse(SharedFieldsAdaptor.isSafeGroupName("a\\b"));
    assertFalse(SharedFieldsAdaptor.isSafeGroupName(null));
  }

  @Test
  void mapOccurrence_mapsKnownDimensions() {
    assertEquals(
        "optional", SharedFieldsAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_OPTIONAL));
    assertEquals(
        "required", SharedFieldsAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_REQUIRED));
    assertEquals("unknown", SharedFieldsAdaptor.mapOccurrence(-99));
  }

  @Test
  void loadSharedDefFromDesignWs_returnsDesignWsResult() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = mock(PSContentEditorSharedDef.class);
    when(designWs.loadContentEditorSharedDef(false, false, "sid", "admin")).thenReturn(def);

    assertSame(def, SharedFieldsAdaptor.loadSharedDefFromDesignWs(designWs, "sid", "admin"));
    verify(designWs).loadContentEditorSharedDef(false, false, "sid", "admin");
  }

  @Test
  void loadSharedDefFromDesignWs_wrapsPsErrorException() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSErrorException cause = new PSErrorException("ws failed");
    when(designWs.loadContentEditorSharedDef(false, false, "sid", "admin")).thenThrow(cause);

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> SharedFieldsAdaptor.loadSharedDefFromDesignWs(designWs, "sid", "admin"));
    assertEquals("Failed to load shared def", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test
  void loadSharedDefFromDesignWs_passesNullSessionAndUserWhenRequestInfoAbsent() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = mock(PSContentEditorSharedDef.class);
    when(designWs.loadContentEditorSharedDef(eq(false), eq(false), isNull(), isNull()))
        .thenReturn(def);

    assertSame(def, SharedFieldsAdaptor.loadSharedDefFromDesignWs(designWs, null, null));
    verify(designWs).loadContentEditorSharedDef(false, false, null, null);
  }

  @Test
  void listGroups_usesInjectedLoaderFromDefaultConstructorShape() {
    PSContentEditorSharedDef def = mock(PSContentEditorSharedDef.class);
    when(def.getFieldGroups())
        .thenReturn(new PSCollection<>(PSSharedFieldGroup.class).iterator());

    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(() -> def);
    assertNotNull(adaptor.listGroups(null));
    assertTrue(adaptor.listGroups(null).isEmpty());
  }

  @Test
  void listGroups_forbiddenWhenNotAdmin() {
    AtomicInteger loads = new AtomicInteger();
    SharedFieldsAdaptor denied =
        new SharedFieldsAdaptor(
            () -> {
              loads.incrementAndGet();
              return mock(PSContentEditorSharedDef.class);
            },
            () -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.listGroups(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(SharedFieldsAdaptor.ADMIN_REQUIRED, ex.getMessage());
    assertEquals(0, loads.get());
  }

  @Test
  void getGroup_forbiddenWhenNotAdmin() {
    AtomicInteger loads = new AtomicInteger();
    SharedFieldsAdaptor denied =
        new SharedFieldsAdaptor(
            () -> {
              loads.incrementAndGet();
              return mock(PSContentEditorSharedDef.class);
            },
            () -> false);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.getGroup(null, "shared"));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(SharedFieldsAdaptor.ADMIN_REQUIRED, ex.getMessage());
    assertEquals(0, loads.get());
  }

  @Test
  void requireAdmin_mapsCheckerFailureTo403() {
    SharedFieldsAdaptor denied =
        new SharedFieldsAdaptor(
            () -> mock(PSContentEditorSharedDef.class),
            () -> {
              throw new IllegalStateException("user service down");
            });

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.listGroups(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(SharedFieldsAdaptor.ADMIN_REQUIRED, ex.getMessage());
  }

  @Test
  void listGroups_forbiddenWhenAdminCheckerFallsBackAndUserServiceMissing() {
    SharedFieldsAdaptor denied =
        new SharedFieldsAdaptor(() -> mock(PSContentEditorSharedDef.class), null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.listGroups(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertFalse(denied.isCurrentUserAdmin());
  }

  @Test
  void createGroup_addsEmptyGroupAndSavesWithLockRelease() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("custom");
    SharedFieldGroupDetail out = adaptor.createGroup(null, body);

    assertEquals("custom", out.getName());
    assertEquals("custom.xml", out.getFilename());
    assertEquals(0, out.getFields().size());
    ArgumentCaptor<PSContentEditorSharedDef> saved =
        ArgumentCaptor.forClass(PSContentEditorSharedDef.class);
    verify(designWs)
        .saveContentEditorSharedDef(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertNotNull(SharedFieldsAdaptor.findGroup(saved.getValue(), "custom"));
  }

  @Test
  void createGroup_duplicateNameIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(SharedFieldsAdaptor.newEmptyGroup("custom", "custom.xml"));
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("custom");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createGroup(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void createGroup_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("custom");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.createGroup(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void createGroup_invalidNameIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createGroup(null, body));
    assertTrue(ex.getMessage().contains("spaces"));
  }

  @Test
  void createGroup_lockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLockErrorException lockErr =
        new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin"))
        .thenThrow(lockErr);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setName("custom");
    SharedFieldDesignLockException ex =
        assertThrows(SharedFieldDesignLockException.class, () -> adaptor.createGroup(null, body));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void updateGroup_patchesSearchableAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSField field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("rx_title");
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.getAllFields()).thenReturn(new PSField[] {field});
    when(set.findFieldByName("rx_title", false)).thenReturn(field);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    group.setFieldSet(set);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldSummary patch = new SharedFieldSummary();
    patch.setName("rx_title");
    patch.setSearchable(true);
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setFields(List.of(patch));

    SharedFieldGroupDetail out = adaptor.updateGroup(null, "shared", body);
    assertEquals("shared", out.getName());
    verify(field).setUserSearchable(true);
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void updateGroup_unknownFieldIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("missing", false)).thenReturn(null);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    group.setFieldSet(set);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldSummary patch = new SharedFieldSummary();
    patch.setName("missing");
    SharedFieldGroupDetail body = new SharedFieldGroupDetail();
    body.setFields(List.of(patch));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateGroup(null, "shared", body));
    assertTrue(ex.getMessage().contains("Unknown field"));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void updateGroup_blankNameIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.updateGroup(null, "  ", new SharedFieldGroupDetail()));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void applyFieldPatches_occurrenceWinsWhenRequiredAgrees() throws Exception {
    PSField field = mock(PSField.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("rx_title", false)).thenReturn(field);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    group.setFieldSet(set);

    SharedFieldSummary patch = new SharedFieldSummary();
    patch.setName("rx_title");
    patch.setOccurrence("oneOrMore");
    patch.setRequired(true);

    SharedFieldsAdaptor.applyFieldPatches(group, List.of(patch));
    verify(field).setOccurrenceDimension(eq(PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE), isNull());
  }

  @Test
  void applyFieldPatches_occurrenceAndRequiredConflictIs400() {
    PSField field = mock(PSField.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("rx_title", false)).thenReturn(field);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    group.setFieldSet(set);

    SharedFieldSummary patch = new SharedFieldSummary();
    patch.setName("rx_title");
    patch.setOccurrence("optional");
    patch.setRequired(true);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> SharedFieldsAdaptor.applyFieldPatches(group, List.of(patch)));
    assertTrue(ex.getMessage().contains("conflict"));
  }

  @Test
  void applyFieldPatches_requiredOnlyWhenOccurrenceOmitted() throws Exception {
    PSField field = mock(PSField.class);
    PSFieldSet set = mock(PSFieldSet.class);
    when(set.findFieldByName("rx_title", false)).thenReturn(field);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    group.setFieldSet(set);

    SharedFieldSummary patch = new SharedFieldSummary();
    patch.setName("rx_title");
    patch.setRequired(true);

    SharedFieldsAdaptor.applyFieldPatches(group, List.of(patch));
    verify(field).setOccurrenceDimension(eq(PSField.OCCURRENCE_DIMENSION_REQUIRED), isNull());
  }

  @Test
  void updateGroup_missingReturnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    assertNull(adaptor.updateGroup(null, "missing", new SharedFieldGroupDetail()));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void updateGroup_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.updateGroup(null, "shared", new SharedFieldGroupDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteGroup_removesAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    adaptor.deleteGroup(null, "shared");

    ArgumentCaptor<PSContentEditorSharedDef> saved =
        ArgumentCaptor.forClass(PSContentEditorSharedDef.class);
    verify(designWs)
        .saveContentEditorSharedDef(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertNull(SharedFieldsAdaptor.findGroup(saved.getValue(), "shared"));
  }

  @Test
  void deleteGroup_blankNameIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.deleteGroup(null, " "));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void deleteGroup_missingThrowsNotFound() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    assertThrows(SharedFieldNotFoundException.class, () -> adaptor.deleteGroup(null, "missing"));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void deleteGroup_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.deleteGroup(null, "shared"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteGroup_saveLockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    doThrow(new PSLockErrorException(1, "not locked", "stack"))
        .when(designWs)
        .saveContentEditorSharedDef(any(), eq(true), eq("test-session"), eq("Admin"));
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldDesignLockException ex =
        assertThrows(SharedFieldDesignLockException.class, () -> adaptor.deleteGroup(null, "shared"));
    assertTrue(ex.getMessage().contains("design lock required"));
  }

  @Test
  void newEmptyGroup_isPersistableXml() {
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("custom", "custom.xml");
    assertEquals("custom", group.getName());
    assertEquals("custom.xml", group.getFilename());
    org.w3c.dom.Document doc =
        com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    org.w3c.dom.Element xml = group.toXml(doc);
    assertEquals("custom", xml.getAttribute("name"));
    assertEquals("custom.xml", xml.getAttribute("filename"));
  }

  @Test
  void newEmptyGroup_usesIncludeReadyChildFieldSet() {
    // #6536: REST-created groups must match the packaged child shape or the
    // content-type save validator (CE_INVALID_SHARED_FIELDSET_TYPE) 500s the include.
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("custom", "custom.xml");
    assertNotNull(group.getFieldSet());
    assertEquals(PSFieldSet.TYPE_MULTI_PROPERTY_SIMPLE_CHILD, group.getFieldSet().getType());
    assertEquals(PSFieldSet.REPEATABILITY_ONE_OR_MORE, group.getFieldSet().getRepeatability());
    assertFalse(group.getFieldSet().isSequencingSupported());
    assertEquals("custom", group.getUIDefinition().getDisplayMapper().getFieldSetRef());
  }

  @Test
  void deleteGroup_409WhenContentTypeStillIncludesGroup() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    IPSCatalogSummary includerSummary = summaryWithGuid(new PSGuid(PSTypeEnum.NODEDEF, 311L));
    when(designWs.findContentTypes("*")).thenReturn(List.of(includerSummary));
    PSItemDefinition includer = stubTypeIncluding("percPage", 311, "shared");
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(includer));
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteGroup(null, "shared"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("shared"), ex.getMessage());
    assertTrue(ex.getMessage().contains("percPage"), ex.getMessage());
    assertNotNull(SharedFieldsAdaptor.findGroup(def, "shared"));
  }

  @Test
  void deleteGroup_succeedsWhenNothingIncludesGroup() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    when(designWs.findContentTypes("*")).thenReturn(List.of());
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    adaptor.deleteGroup(null, "shared");

    ArgumentCaptor<PSContentEditorSharedDef> saved =
        ArgumentCaptor.forClass(PSContentEditorSharedDef.class);
    verify(designWs)
        .saveContentEditorSharedDef(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertNull(SharedFieldsAdaptor.findGroup(saved.getValue(), "shared"));
  }

  @Test
  void groupIncluders_matchesCaseInsensitiveAndSkipsMapperless() {
    PSItemDefinition includer = stubTypeIncluding("percPage", 311, "Shared");
    PSItemDefinition other = stubTypeIncluding("percBlog", 312, "other");
    PSItemDefinition mapperless = mock(PSItemDefinition.class);
    when(mapperless.getName()).thenReturn("percBroken");
    when(mapperless.getContentEditorMapper()).thenReturn(null);
    List<String> out =
        SharedFieldsAdaptor.groupIncluders(
            Arrays.asList(includer, null, other, mapperless), "shared");
    assertEquals(List.of("percPage"), out);
    assertTrue(SharedFieldsAdaptor.groupIncluders(null, "shared").isEmpty());
    assertTrue(SharedFieldsAdaptor.groupIncluders(List.of(includer), " ").isEmpty());
  }

  /**
   * Stored content type definition stub whose editor mapper includes {@code groupName}. Mirrors
   * {@link #stubTypeUsing} but carries a group include instead of field usage.
   */
  private static PSItemDefinition stubTypeIncluding(String typeName, int typeId, String groupName) {
    PSFieldSet parent = new PSFieldSet(typeName);
    PSDisplayMapper display = new PSDisplayMapper(typeName);
    PSContentEditorMapper mapper =
        new PSContentEditorMapper(
            new ArrayList<>(), new ArrayList<>(List.of(groupName)), parent,
            new PSUIDefinition(display));
    mapper.setSharedFieldExcludes(new ArrayList<>());
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(typeName);
    when(def.getTypeId()).thenReturn(typeId);
    when(def.getContentEditorMapper()).thenReturn(mapper);
    return def;
  }

  @Test
  void normalizeFilename_defaultsToNameXml() {
    assertEquals("custom.xml", SharedFieldsAdaptor.normalizeFilename(null, "custom"));
    assertEquals("mine.xml", SharedFieldsAdaptor.normalizeFilename("mine", "custom"));
    assertEquals("Mine.xml", SharedFieldsAdaptor.normalizeFilename("Mine.XML", "custom"));
  }

  @Test
  void mapLockConflict_includesLocker() {
    PSLockErrorException err = new PSLockErrorException(1, "locked", "stack", "alice", 10L);
    SharedFieldDesignLockException mapped = SharedFieldsAdaptor.mapLockConflict(err);
    assertEquals("Could not save shared field group; locked by alice", mapped.getMessage());
  }

  @Test
  void addField_addsPersistableFieldAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    body.setSearchable(true);
    SharedFieldGroupDetail out = adaptor.addField(null, "shared", body);

    assertEquals("shared", out.getName());
    assertEquals(1, out.getFields().size());
    assertEquals("rx_note", out.getFields().get(0).getName());
    assertEquals("text", out.getFields().get(0).getDataType());
    assertEquals(Boolean.TRUE, out.getFields().get(0).getSearchable());
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertNotNull(group.getFieldSet().findFieldByName("rx_note", false));
    assertNotNull(group.getUIDefinition().getMapping("rx_note"));
  }

  @Test
  void addField_duplicateIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(group, existing);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.addField(null, "shared", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void addField_duplicateInOtherGroupIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup other = SharedFieldsAdaptor.newEmptyGroup("other", "other.xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(other, existing);
    PSSharedFieldGroup target = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(other);
    def.addFieldGroup(target);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.addField(null, "shared", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void addField_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.addField(null, "shared", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void addField_missingGroupReturnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    assertNull(adaptor.addField(null, "missing", body));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void addField_lockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLockErrorException lockErr =
        new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin"))
        .thenThrow(lockErr);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    SharedFieldDesignLockException ex =
        assertThrows(SharedFieldDesignLockException.class, () -> adaptor.addField(null, "shared", body));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void addField_invalidNameIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addField(null, "shared", body));
    assertTrue(ex.getMessage().contains("spaces"));
  }

  @Test
  void addField_blankGroupNameIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addField(null, "  ", body));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void addField_invalidDataTypeIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    body.setDataType("not-a-type");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.addField(null, "shared", body));
    assertTrue(ex.getMessage().contains("dataType"));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void deleteField_removesAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(group, existing);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    adaptor.deleteField(null, "shared", "rx_note");

    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertNull(group.getFieldSet().findFieldByName("rx_note", false));
    assertNull(group.getUIDefinition().getMapping("rx_note"));
  }

  @Test
  void deleteField_missingFieldThrowsNotFound() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    assertThrows(
        SharedFieldNotFoundException.class, () -> adaptor.deleteField(null, "shared", "missing"));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void deleteField_missingGroupThrowsNotFound() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldNotFoundException ex =
        assertThrows(
            SharedFieldNotFoundException.class,
            () -> adaptor.deleteField(null, "missing", "rx_note"));
    assertTrue(ex.getMessage().contains("group"));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void deleteField_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.deleteField(null, "shared", "rx_note"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteField_saveLockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(group, existing);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    doThrow(new PSLockErrorException(1, "not locked", "stack"))
        .when(designWs)
        .saveContentEditorSharedDef(any(), eq(true), eq("test-session"), eq("Admin"));
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldDesignLockException ex =
        assertThrows(
            SharedFieldDesignLockException.class,
            () -> adaptor.deleteField(null, "shared", "rx_note"));
    assertTrue(ex.getMessage().contains("design lock required"));
  }

  @Test
  void addPersistableField_isPersistableXml() {
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("custom", "custom.xml");
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(group, body);
    org.w3c.dom.Document doc = com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    org.w3c.dom.Element xml = group.toXml(doc);
    String serialized = com.percussion.xml.PSXmlDocumentBuilder.toString(xml);
    assertTrue(serialized.contains("rx_note"));
    assertTrue(serialized.contains("RX_NOTE") || serialized.contains("rx_note"));
    assertNotNull(group.getUIDefinition().getMapping("rx_note"));
  }

  @Test
  void validateFieldName_rejectsInvalid() {
    assertEquals("rx_note", SharedFieldsAdaptor.validateFieldName("rx_note"));
    assertThrows(IllegalArgumentException.class, () -> SharedFieldsAdaptor.validateFieldName(" "));
    assertThrows(
        IllegalArgumentException.class, () -> SharedFieldsAdaptor.validateFieldName("has space"));
    assertThrows(
        IllegalArgumentException.class, () -> SharedFieldsAdaptor.validateFieldName("a/b"));
    assertThrows(
        IllegalArgumentException.class, () -> SharedFieldsAdaptor.validateFieldName("1start"));
  }

  @Test
  void getFieldControlProperties_returnsValuesAndChoices() {
    PSSharedFieldGroup group = groupWithControlAndChoices("shared", "rx_note");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(() -> def);

    SharedFieldControlProperties out =
        adaptor.getFieldControlProperties(null, "shared", "rx_note");
    assertEquals("rx_note", out.getFieldName());
    assertEquals("sys_EditBox", out.getControl());
    assertEquals(1, out.getProperties().size());
    assertEquals("height", out.getProperties().get(0).getName());
    assertEquals("200", out.getProperties().get(0).getValue());
    assertEquals("local", out.getChoices().getType());
    assertEquals("open", out.getChoices().getEntries().get(0).getValue());
    assertEquals(2, out.getDesignGaps().size());
    assertEquals("SF_SPA_EDITOR", out.getDesignGaps().get(0).getCode());
    assertEquals("SF_SYSTEM_DEF_SEPARATE", out.getDesignGaps().get(1).getCode());
  }

  @Test
  void getFieldControlProperties_unknownGroupReturnsNull() {
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(PSContentEditorSharedDef::new);
    assertNull(adaptor.getFieldControlProperties(null, "missing", "rx_note"));
  }

  @Test
  void getFieldControlProperties_unknownFieldIs404() {
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(group, existing);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(() -> def);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.getFieldControlProperties(null, "shared", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void getFieldControlProperties_forbiddenWhenNotAdmin() {
    AtomicInteger loads = new AtomicInteger();
    SharedFieldsAdaptor denied =
        new SharedFieldsAdaptor(
            () -> {
              loads.incrementAndGet();
              return mock(PSContentEditorSharedDef.class);
            },
            () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.getFieldControlProperties(null, "shared", "rx_note"));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(0, loads.get());
  }

  @Test
  void replaceFieldControlProperties_persistsValuesAndReleasesLock() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = groupWithControlAndChoices("shared", "rx_note");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));
    ContentTypeChoiceCatalog choices = new ContentTypeChoiceCatalog();
    choices.setType("local");
    choices.setEntries(List.of(new ContentTypeChoiceEntry("closed", "Closed")));
    body.setChoices(choices);

    SharedFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "shared", "rx_note", body);

    verify(designWs)
        .saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
    assertEquals("640", out.getProperties().get(0).getValue());
    assertTrue(
        out.getDesignGaps().stream().anyMatch(g -> "SF_SPA_EDITOR".equals(g.getCode())),
        () -> String.valueOf(out.getDesignGaps()));
    assertTrue(
        out.getDesignGaps().stream()
            .anyMatch(g -> "SF_SYSTEM_DEF_SEPARATE".equals(g.getCode())),
        () -> String.valueOf(out.getDesignGaps()));
    assertEquals("closed", out.getChoices().getEntries().get(0).getValue());
    PSControlRef control = group.getUIDefinition().getMapping("rx_note").getUISet().getControl();
    PSParam first = (PSParam) control.getParameters().next();
    assertEquals("width", first.getName());
    assertEquals("640", first.getValue().getValueText());
  }

  @Test
  void replaceFieldControlProperties_emptyPropertiesClears() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = groupWithControlAndChoices("shared", "rx_note");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of());
    SharedFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "shared", "rx_note", body);

    assertTrue(out.getProperties().isEmpty());
    PSControlRef control = group.getUIDefinition().getMapping("rx_note").getUISet().getControl();
    assertTrue(!control.getParameters().hasNext(), "expected empty control parameters");
  }

  @Test
  void replaceFieldControlProperties_omittedChoicesLeaveCatalog() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = groupWithControlAndChoices("shared", "rx_note");
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("height", "12")));
    SharedFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "shared", "rx_note", body);

    assertEquals("local", out.getChoices().getType());
    assertEquals("open", out.getChoices().getEntries().get(0).getValue());
  }

  @Test
  void replaceFieldControlProperties_unknownGroupReturnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of());
    assertNull(adaptor.replaceFieldControlProperties(null, "missing", "rx_note", body));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void replaceFieldControlProperties_unknownFieldIs404() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup("shared", "shared.xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName("rx_note");
    SharedFieldsAdaptor.addPersistableField(group, existing);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin")).thenReturn(def);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of());
    assertThrows(
        SharedFieldNotFoundException.class,
        () -> adaptor.replaceFieldControlProperties(null, "shared", "nope", body));
    verify(designWs).saveContentEditorSharedDef(eq(def), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void replaceFieldControlProperties_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);
    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.replaceFieldControlProperties(null, "shared", "rx_note", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadContentEditorSharedDef(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void replaceFieldControlProperties_lockConflictIs409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLockErrorException lockErr =
        new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    when(designWs.loadContentEditorSharedDef(true, false, "test-session", "Admin"))
        .thenThrow(lockErr);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of());
    SharedFieldDesignLockException ex =
        assertThrows(
            SharedFieldDesignLockException.class,
            () -> adaptor.replaceFieldControlProperties(null, "shared", "rx_note", body));
    assertTrue(ex.getMessage().contains("locked by other"));
  }

  @Test
  void replaceFieldControlProperties_missingPropertiesIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldControlProperties body = new SharedFieldControlProperties();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldControlProperties(null, "shared", "rx_note", body));
    assertTrue(ex.getMessage().contains("properties"));
  }

  @Test
  void replaceFieldControlProperties_blankGroupOrFieldNameIs400() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);
    SharedFieldControlProperties body = new SharedFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));
    IllegalArgumentException blankGroup =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldControlProperties(null, "  ", "rx_note", body));
    assertTrue(blankGroup.getMessage().contains("name is required"));
    IllegalArgumentException blankField =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldControlProperties(null, "shared", "  ", body));
    assertTrue(blankField.getMessage().contains("name is required"));
  }

  @Test
  void getFieldUsage_listsUsingTypesNameSorted() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.loadContentEditorSharedDef(false, false, "test-session", "Admin"))
        .thenReturn(sharedDefWithField("shared", "rx_note"));
    IPSGuid guidB = new PSGuid(PSTypeEnum.NODEDEF, 312L);
    IPSGuid guidA = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    IPSCatalogSummary summaryB = summaryWithGuid(guidB);
    IPSCatalogSummary summaryA = summaryWithGuid(guidA);
    when(designWs.findContentTypes("*")).thenReturn(List.of(summaryB, summaryA));
    PSItemDefinition typeB = stubTypeUsing("percBlog", 312, "rx_note", false, false);
    PSItemDefinition typeA = stubTypeUsing("percPage", 311, "rx_note", false, false);
    List<PSItemDefinition> loaded = List.of(typeB, typeA);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(loaded);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldUsage out = adaptor.getFieldUsage(null, "shared", "rx_note");

    assertEquals("shared", out.getGroupName());
    assertEquals("rx_note", out.getFieldName());
    assertEquals(2, out.getUsedBy().size());
    assertEquals("percBlog", out.getUsedBy().get(0).getContentTypeName());
    assertEquals(312, out.getUsedBy().get(0).getContentTypeId());
    assertEquals("percPage", out.getUsedBy().get(1).getContentTypeName());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void getFieldUsage_emptyWhenNoTypeUsesField() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.loadContentEditorSharedDef(false, false, "test-session", "Admin"))
        .thenReturn(sharedDefWithField("shared", "rx_note"));
    IPSCatalogSummary summary = summaryWithGuid(new PSGuid(PSTypeEnum.NODEDEF, 311L));
    when(designWs.findContentTypes("*")).thenReturn(List.of(summary));
    PSItemDefinition unused = stubTypeUsing("percPage", 311, "other_field", false, false);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(unused));
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldUsage out = adaptor.getFieldUsage(null, "shared", "rx_note");

    assertNotNull(out.getUsedBy());
    assertTrue(out.getUsedBy().isEmpty());
  }

  @Test
  void getFieldUsage_excludedAndShadowedTypesAreNotUsage() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.loadContentEditorSharedDef(false, false, "test-session", "Admin"))
        .thenReturn(sharedDefWithField("shared", "rx_note"));
    IPSCatalogSummary summary311 = summaryWithGuid(new PSGuid(PSTypeEnum.NODEDEF, 311L));
    IPSCatalogSummary summary312 = summaryWithGuid(new PSGuid(PSTypeEnum.NODEDEF, 312L));
    IPSCatalogSummary summary313 = summaryWithGuid(new PSGuid(PSTypeEnum.NODEDEF, 313L));
    when(designWs.findContentTypes("*"))
        .thenReturn(List.of(summary311, summary312, summary313));
    PSItemDefinition excluded = stubTypeUsing("percExcluded", 311, "rx_note", true, false);
    PSItemDefinition shadowed = stubTypeUsing("percShadow", 312, "rx_note", false, true);
    PSItemDefinition user = stubTypeUsing("percPage", 313, "rx_note", false, false);
    List<PSItemDefinition> loadedThree = List.of(excluded, shadowed, user);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(loadedThree);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    SharedFieldUsage out = adaptor.getFieldUsage(null, "shared", "rx_note");

    assertEquals(1, out.getUsedBy().size());
    assertEquals("percPage", out.getUsedBy().get(0).getContentTypeName());
  }

  @Test
  void getFieldUsage_unknownGroupOrFieldIs404() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.loadContentEditorSharedDef(false, false, "test-session", "Admin"))
        .thenReturn(sharedDefWithField("shared", "rx_note"));
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    assertThrows(
        SharedFieldNotFoundException.class, () -> adaptor.getFieldUsage(null, "missing", "rx_note"));
    assertThrows(
        SharedFieldNotFoundException.class, () -> adaptor.getFieldUsage(null, "shared", "nope"));
  }

  @Test
  void getFieldUsage_unsafeNameReturnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    assertNull(adaptor.getFieldUsage(null, "../shared", "rx_note"));
    assertNull(adaptor.getFieldUsage(null, "shared", "../rx_note"));
    verify(designWs, never()).findContentTypes(any());
  }

  @Test
  void getFieldUsage_forbiddenWhenNotAdmin() {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    SharedFieldsAdaptor denied = new SharedFieldsAdaptor(designWs, () -> false);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.getFieldUsage(null, "shared", "rx_note"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).findContentTypes(any());
  }

  @Test
  void getFieldUsage_findFailureIs500() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.loadContentEditorSharedDef(false, false, "test-session", "Admin"))
        .thenReturn(sharedDefWithField("shared", "rx_note"));
    when(designWs.findContentTypes("*")).thenThrow(new IllegalStateException("store down"));
    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(designWs, () -> true);

    assertThrows(IllegalStateException.class, () -> adaptor.getFieldUsage(null, "shared", "rx_note"));
  }

  @Test
  void usesSharedField_nullAndBlankAreFalse() {
    assertFalse(SharedFieldsAdaptor.usesSharedField(null, "rx_note"));
  }

  private static PSContentEditorSharedDef sharedDefWithField(String groupName, String fieldName) {
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup(groupName, groupName + ".xml");
    SharedFieldSummary body = new SharedFieldSummary();
    body.setName(fieldName);
    SharedFieldsAdaptor.addPersistableField(group, body);
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(group);
    return def;
  }

  private static IPSCatalogSummary summaryWithGuid(IPSGuid guid) {
    IPSCatalogSummary summary = mock(IPSCatalogSummary.class);
    when(summary.getGUID()).thenReturn(guid);
    return summary;
  }

  /**
   * Stored content type definition stub. The type uses {@code field} when it carries the display
   * mapping without an exclude; {@code excluded} adds the shared-field exclude and {@code
   * localShadow} adds a same-named local field entry.
   */
  @SuppressWarnings("unchecked")
  private static PSItemDefinition stubTypeUsing(
      String typeName, int typeId, String field, boolean excluded, boolean localShadow) {
    PSFieldSet parent = new PSFieldSet(typeName);
    if (localShadow) {
      parent.add(new PSField(PSField.TYPE_LOCAL, field, null));
    }
    PSDisplayMapper display = new PSDisplayMapper(typeName);
    PSUISet ui = new PSUISet();
    ui.setLabel(new PSDisplayText(field + ":"));
    display.add(new PSDisplayMapping(field, ui));
    PSContentEditorMapper mapper =
        new PSContentEditorMapper(
            new ArrayList<>(), new ArrayList<>(List.of("shared")), parent,
            new PSUIDefinition(display));
    mapper.setSharedFieldExcludes(new ArrayList<>());
    if (excluded) {
      ContentTypeAdaptor.addSharedFieldExclude(mapper, field);
    }
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(typeName);
    when(def.getTypeId()).thenReturn(typeId);
    when(def.getFieldSet()).thenReturn(parent);
    when(def.getComplexChildren()).thenReturn(List.of());
    when(def.getDisplayMapper(typeName)).thenReturn(display);
    when(def.getContentEditorMapper()).thenReturn(mapper);
    return def;
  }

  private static PSSharedFieldGroup groupWithControlAndChoices(String groupName, String fieldName) {
    PSSharedFieldGroup group = SharedFieldsAdaptor.newEmptyGroup(groupName, groupName + ".xml");
    SharedFieldSummary existing = new SharedFieldSummary();
    existing.setName(fieldName);
    SharedFieldsAdaptor.addPersistableField(group, existing);
    PSDisplayMapping mapping = group.getUIDefinition().getMapping(fieldName);
    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("height", new PSTextLiteral("200")));
    mapping.getUISet().getControl().setParameters(params);
    PSCollection local = new PSCollection(PSEntry.class);
    local.add(new PSEntry("open", new PSDisplayText("Open")));
    mapping.getUISet().setChoices(new PSChoices(local));
    return group;
  }
}
