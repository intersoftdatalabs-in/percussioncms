/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import com.percussion.rest.searches.SearchDef;
import com.percussion.utils.guid.IPSGuid;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SearchAdaptorMapTest {

  @Test
  void toDef_mapsMetaAndFields() throws Exception {
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
    when(s.getName()).thenReturn("All Content");
    when(s.getLabel()).thenReturn("All Content");
    when(s.getDescription()).thenReturn("desc");
    when(s.getType()).thenReturn("Search");
    when(s.getDisplayFormatId()).thenReturn("0-1-1");
    when(s.getUrl()).thenReturn("/Rhythmyx/search");
    when(s.getParentCategory()).thenReturn(1);
    when(s.getMaximumResultSize()).thenReturn(100);
    when(s.isUserSearch()).thenReturn(false);
    when(s.isCustomSearch()).thenReturn(false);
    when(s.isStandardSearch()).thenReturn(true);
    when(s.isUserCustomizable()).thenReturn(false);
    when(s.isCaseSensitive()).thenReturn(false);
    when(s.getFieldContainer()).thenReturn(fields);

    SearchDef d = SearchAdaptor.toDef(s);
    assertEquals("All Content", d.getName());
    assertTrue(d.isStandardSearch());
    assertFalse(d.isUserSearch());
    assertEquals(1, d.getFields().size());
    assertEquals("sys_title", d.getFields().get(0).getFieldName());
    assertFalse(d.getDesignGaps().isEmpty());
    assertTrue(
        d.getDesignGaps().stream()
            .noneMatch(g -> g.toLowerCase().contains("create / update / delete not supported")));
  }

  @Test
  void mapFields_nullYieldsEmpty() {
    assertTrue(SearchAdaptor.mapFields(null).isEmpty());
  }
}
