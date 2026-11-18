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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * GET content-type detail lists each field exactly once (#6600 / parent #1690).
 *
 * <p>The parent field set contains its complex-child sets, and {@code
 * PSFieldSet.getAllFields()} recurses into them — so a recursive parent pass lists every child
 * field twice (once parent-flat with no {@code fieldSet}, once with the set name from the child
 * loop). The parent pass must only emit direct {@code PSField} members.
 */
@Tag("UnitTest")
class ContentTypeAdaptorDetailFieldsTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void detail_listsComplexChildFieldExactlyOnceWithSetName() throws Exception {
    stubFolderStyleDefinition();

    ContentTypeDetail detail = adaptor.getContentType(null, "311");

    assertNotNull(detail);
    List<ContentTypeField> fields = detail.getFields();
    assertNotNull(fields);
    assertEquals(1, countByName(fields, "displaytitle"), "parent field listed once");
    assertEquals(1, countByName(fields, "rx_kid_note"), "child field listed once");
    ContentTypeField kid = byName(fields, "rx_kid_note");
    assertNotNull(kid);
    assertEquals("kid", kid.getFieldSet());
    assertEquals(List.of("kid"), detail.getChildFieldSets());
  }

  @Test
  void detail_hasNoParentFlatRowForChildField() throws Exception {
    stubFolderStyleDefinition();

    ContentTypeDetail detail = adaptor.getContentType(null, "311");

    assertNotNull(detail);
    for (ContentTypeField field : detail.getFields()) {
      if ("rx_kid_note".equals(field.getName())) {
        assertEquals(
            "kid",
            field.getFieldSet(),
            "child field must not appear as a parent-flat row (blank fieldSet)");
      }
    }
  }

  private void stubFolderStyleDefinition() throws Exception {
    PSBackEndTable parentTable = new PSBackEndTable("PERCFOLDER");
    parentTable.setTable("PERCFOLDER");
    PSFieldSet parent = new PSFieldSet("percFolder");
    parent.add(
        new PSField(PSField.TYPE_LOCAL, "displaytitle", new PSBackEndColumn(parentTable, "TITLE")));

    PSFieldSet kid = new PSFieldSet("kid");
    kid.setType(PSFieldSet.TYPE_COMPLEX_CHILD);
    kid.setSourceType(PSField.TYPE_LOCAL);
    PSBackEndTable kidTable = new PSBackEndTable("PERCFOLDER_KID");
    kidTable.setTable("PERCFOLDER_KID");
    kid.add(
        new PSField(
            PSField.TYPE_LOCAL, "rx_kid_note", new PSBackEndColumn(kidTable, "RX_KID_NOTE")));
    parent.add(kid);

    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percFolder");
    when(def.getLabel()).thenReturn("Folder");
    when(def.getDescription()).thenReturn("Folder type");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_ceFolder");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_ceFolder/percFolder.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(parent);
    when(def.getComplexChildren()).thenReturn(List.of(kid));
    when(def.getDisplayMapper(anyString())).thenReturn(null);
    when(def.getContentEditor()).thenReturn(null);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenReturn(def);
  }

  private static long countByName(List<ContentTypeField> fields, String name) {
    return fields.stream().filter(f -> f != null && name.equals(f.getName())).count();
  }

  private static ContentTypeField byName(List<ContentTypeField> fields, String name) {
    return fields.stream().filter(f -> f != null && name.equals(f.getName())).findFirst()
        .orElse(null);
  }
}
