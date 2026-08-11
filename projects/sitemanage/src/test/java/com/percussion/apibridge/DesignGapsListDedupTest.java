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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSSFields;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.rest.relationshiptypes.RelationshipType;
import com.percussion.rest.searches.SearchDef;
import com.percussion.rest.views.ViewDef;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * REST-GAPS-02: identical catalog-level designGaps must not be stamped on every list row.
 * Detail projections keep the shared list; list projections omit (null) for payload size.
 */
@Tag("UnitTest")
class DesignGapsListDedupTest {

  private final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  void searchToDef_listOmitsGaps_detailIncludes() throws Exception {
    PSSearch s = minimalSearch("All Content");

    SearchDef listRow = SearchAdaptor.toDef(s, false);
    assertNull(listRow.getDesignGaps());
    String listJson = mapper.writeValueAsString(listRow);
    assertFalse(listJson.contains("designGaps"), listJson);

    SearchDef detail = SearchAdaptor.toDef(s, true);
    assertNotNull(detail.getDesignGaps());
    assertFalse(detail.getDesignGaps().isEmpty());
    assertTrue(detail.getDesignGaps().containsAll(SearchAdaptor.DESIGN_GAPS));
    String detailJson = mapper.writeValueAsString(detail);
    assertTrue(detailJson.contains("designGaps"), detailJson);
  }

  @Test
  void viewToDef_listOmitsGaps_detailIncludes() throws Exception {
    PSSearch s = minimalSearch("My View");

    ViewDef listRow = ViewAdaptor.toDef(s, false);
    assertNull(listRow.getDesignGaps());
    String listJson = mapper.writeValueAsString(listRow);
    assertFalse(listJson.contains("designGaps"), listJson);

    ViewDef detail = ViewAdaptor.toDef(s, true);
    assertNotNull(detail.getDesignGaps());
    assertFalse(detail.getDesignGaps().isEmpty());
    assertTrue(detail.getDesignGaps().containsAll(ViewAdaptor.DESIGN_GAPS));

    ViewDef reattached = ViewAdaptor.withDesignGaps(listRow);
    assertNotNull(reattached.getDesignGaps());
    assertFalse(reattached.getDesignGaps().isEmpty());
  }

  @Test
  void relationshipType_withDesignGaps_reattachesCatalogList() {
    RelationshipType listRow = new RelationshipType();
    listRow.setName("rs_folder");
    listRow.setDesignGaps(null);
    assertNull(listRow.getDesignGaps());

    RelationshipType detail = RelationshipTypeAdaptor.withDesignGaps(listRow);
    assertNotNull(detail.getDesignGaps());
    assertFalse(detail.getDesignGaps().isEmpty());
    assertTrue(detail.getDesignGaps().containsAll(RelationshipTypeAdaptor.DESIGN_GAPS));
  }

  private static PSSearch minimalSearch(String name) {
    PSSFields fields = mock(PSSFields.class);
    when(fields.size()).thenReturn(0);

    PSSearch s = mock(PSSearch.class);
    when(s.getGUID()).thenReturn(null);
    when(s.getId()).thenReturn(10);
    when(s.getName()).thenReturn(name);
    when(s.getLabel()).thenReturn(name);
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
    when(s.isView()).thenReturn(false);
    when(s.isCustomView()).thenReturn(false);
    when(s.isStandardView()).thenReturn(false);
    when(s.getFieldContainer()).thenReturn(fields);
    return s;
  }
}
