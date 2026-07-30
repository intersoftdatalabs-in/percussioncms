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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.systemdef.SystemDefDetail;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SystemDefAdaptorTest {

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
    assertEquals("unknown", SystemDefAdaptor.mapOccurrence(-1));
  }
}
