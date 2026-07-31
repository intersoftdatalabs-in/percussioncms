/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.rest.sharedfields.SharedFieldGroupDetail;
import com.percussion.rest.sharedfields.SharedFieldGroupSummary;
import com.percussion.util.PSCollection;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SharedFieldsAdaptorTest {

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
    PSCollection coll = new PSCollection(PSSharedFieldGroup.class);
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
    PSCollection coll = new PSCollection(PSSharedFieldGroup.class);
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
    when(def.getFieldGroups()).thenReturn(new PSCollection(PSSharedFieldGroup.class).iterator());

    SharedFieldsAdaptor adaptor = new SharedFieldsAdaptor(() -> def);
    assertNotNull(adaptor.listGroups(null));
    assertTrue(adaptor.listGroups(null).isEmpty());
  }
}
