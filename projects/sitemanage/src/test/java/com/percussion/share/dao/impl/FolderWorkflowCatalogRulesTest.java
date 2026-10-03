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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pathmanagement.data.PSFolderWorkflowCatalog;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Folder workflow catalog ids are workflow UUIDs, sorted by name (#5104). */
class FolderWorkflowCatalogRulesTest {

  @Test
  void fromSummaries_keepsPositiveIdsSortsByNameAndDropsDuplicates() {
    PSFolderWorkflowCatalog catalog =
        FolderWorkflowCatalogRules.fromSummaries(
            Arrays.asList(
                summary(7, "Local"),
                null,
                summary(4, "Simple"),
                summary(7, "Local again")));

    assertEquals(2, catalog.getChoices().size());
    assertEquals("Local", catalog.getChoices().get(0).getName());
    assertEquals("7", catalog.getChoices().get(0).getId());
    assertEquals("Simple", catalog.getChoices().get(1).getName());
    assertEquals("4", catalog.getChoices().get(1).getId());
    assertTrue(FolderWorkflowCatalogRules.contains(catalog, 4));
    assertFalse(FolderWorkflowCatalogRules.contains(catalog, 9));
    assertFalse(FolderWorkflowCatalogRules.contains(catalog, 0));
    assertFalse(FolderWorkflowCatalogRules.contains(null, 4));
  }

  private static PSObjectSummary summary(int id, String name) {
    return PSObjectSummary.of(new PSGuid(PSTypeEnum.WORKFLOW, id), name, name, "");
  }
}
