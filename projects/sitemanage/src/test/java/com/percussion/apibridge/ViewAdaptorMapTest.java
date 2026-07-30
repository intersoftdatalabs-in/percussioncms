/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSSFields;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.rest.views.ViewDef;
import com.percussion.utils.guid.IPSGuid;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ViewAdaptorMapTest {

  @Test
  void toDef_mapsMetaAndFields() {
    PSSearchField field = mock(PSSearchField.class);
    when(field.getFieldName()).thenReturn("sys_title");
    when(field.getDisplayName()).thenReturn("Title");
    when(field.getOperator()).thenReturn("=");
    when(field.getFieldValue()).thenReturn("x");
    when(field.getFieldType()).thenReturn("Text");
    when(field.getPosition()).thenReturn(0);

    PSSFields fields = mock(PSSFields.class);
    when(fields.size()).thenReturn(1);
    when(fields.get(0)).thenReturn(field);

    IPSGuid guid = mock(IPSGuid.class);
    when(guid.getHostId()).thenReturn(0L);
    when(guid.longValue()).thenReturn(123L);
    when(guid.toString()).thenReturn("0-301-123");
    when(guid.getType()).thenReturn((short) 301);
    when(guid.getUUID()).thenReturn(123);
    when(guid.toStringUntyped()).thenReturn("123");

    PSSearch s = mock(PSSearch.class);
    when(s.getGUID()).thenReturn(guid);
    when(s.getId()).thenReturn(10);
    when(s.getName()).thenReturn("My View");
    when(s.getLabel()).thenReturn("My View");
    when(s.getDescription()).thenReturn("desc");
    when(s.getType()).thenReturn("View");
    when(s.getDisplayFormatId()).thenReturn("0-1-1");
    when(s.getUrl()).thenReturn("/Rhythmyx/view");
    when(s.getParentCategory()).thenReturn(1);
    when(s.getMaximumResultSize()).thenReturn(100);
    when(s.isView()).thenReturn(true);
    when(s.isCustomView()).thenReturn(false);
    when(s.isStandardView()).thenReturn(true);
    when(s.isUserCustomizable()).thenReturn(false);
    when(s.isCaseSensitive()).thenReturn(false);
    when(s.getFieldContainer()).thenReturn(fields);

    ViewDef d = ViewAdaptor.toDef(s);
    assertEquals("My View", d.getName());
    assertTrue(d.isView());
    assertTrue(d.isStandardView());
    assertFalse(d.isCustomView());
    assertEquals(1, d.getFields().size());
    assertEquals("sys_title", d.getFields().get(0).getFieldName());
    assertFalse(d.getDesignGaps().isEmpty());
  }

  @Test
  void mapFields_nullYieldsEmpty() {
    assertTrue(ViewAdaptor.mapFields(null).isEmpty());
  }
}
