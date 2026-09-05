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

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.applicationfiles.ApplicationFileSummary;
import com.percussion.rest.cecontrols.ControlDef;
import com.percussion.rest.relationshiptypes.RelationshipType;
import com.percussion.rest.searches.SearchDef;
import com.percussion.rest.serverconfigs.ServerConfigSummary;
import com.percussion.rest.views.ViewDef;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * REST-GAPS-02 wire shape: when designGaps is null/empty on list rows, Jackson must not emit a
 * repeated array; detail rows with a non-empty list still serialize the property.
 */
@Tag("UnitTest")
class DesignGapsPayloadDedupTest {

  private final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  void searchDef_nullGapsOmitted_nonEmptyIncluded() throws Exception {
    SearchDef row = new SearchDef();
    row.setName("list-row");
    row.setDesignGaps(null);
    assertFalse(mapper.writeValueAsString(row).contains("designGaps"));

    row.setDesignGaps(List.of("gap-a", "gap-b"));
    String json = mapper.writeValueAsString(row);
    assertTrue(json.contains("designGaps"));
    assertTrue(json.contains("gap-a"));
  }

  @Test
  void viewDef_nullGapsOmitted_nonEmptyIncluded() throws Exception {
    ViewDef row = new ViewDef();
    row.setName("list-row");
    row.setDesignGaps(null);
    assertFalse(mapper.writeValueAsString(row).contains("designGaps"));

    row.setDesignGaps(List.of("gap-a"));
    assertTrue(mapper.writeValueAsString(row).contains("designGaps"));
  }

  @Test
  void controlDef_nullGapsOmitted_viaNonNull() throws Exception {
    ControlDef row = new ControlDef();
    row.setName("sys_EditBox");
    row.setDesignGaps(null);
    assertFalse(mapper.writeValueAsString(row).contains("designGaps"));

    row.setDesignGaps(List.of("gap-a"));
    assertTrue(mapper.writeValueAsString(row).contains("designGaps"));
  }

  @Test
  void serverConfig_nullGapsOmitted_viaNonNull() throws Exception {
    ServerConfigSummary row = new ServerConfigSummary();
    row.setName("LOG_CONFIG");
    row.setDesignGaps(null);
    assertFalse(mapper.writeValueAsString(row).contains("designGaps"));

    row.setDesignGaps(List.of("gap-a"));
    assertTrue(mapper.writeValueAsString(row).contains("designGaps"));
  }

  @Test
  void relationshipType_nullGapsOmitted_viaNonNull() throws Exception {
    RelationshipType row = new RelationshipType();
    row.setName("rs_folder");
    row.setDesignGaps(null);
    assertFalse(mapper.writeValueAsString(row).contains("designGaps"));

    row.setDesignGaps(List.of("gap-a"));
    assertTrue(mapper.writeValueAsString(row).contains("designGaps"));
  }

  @Test
  void applicationFile_nullGapsOmitted_viaNonNull() throws Exception {
    ApplicationFileSummary row = new ApplicationFileSummary();
    row.setPath("ApplicationFiles/a.css");
    row.setDesignGaps(null);
    assertFalse(mapper.writeValueAsString(row).contains("designGaps"));

    row.setDesignGaps(List.of("gap-a"));
    assertTrue(mapper.writeValueAsString(row).contains("designGaps"));
  }
}
