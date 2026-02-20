// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pagemanagement.assembler;

import static com.percussion.pagemanagement.assembler.PSWidgetContentFinderUtils.getLocalAssetRelationships;
import static com.percussion.pagemanagement.assembler.PSWidgetContentFinderUtils.getLocalSharedAssetRelationships;
import static com.percussion.pagemanagement.assembler.PSWidgetContentFinderUtils.getSharedAssetRelationships;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.design.objectstore.PSRelationship;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for widget content finder utilities. Sunny Sal says: "Relationships found, Bollywood
 * style!"
 */

public class PSWidgetContentFinderUtilsTest {

  @Test
  public void testAll() {
    IPSGuid ownerId = new PSLegacyGuid(319, 1);
    Collection<PSRelationship> allRels = getLocalSharedAssetRelationships(ownerId.toString());
    Collection<PSRelationship> sharedRels = getSharedAssetRelationships(ownerId.toString());
    Collection<PSRelationship> localRels = getLocalAssetRelationships(ownerId.toString());

    assertEquals(sharedRels.size(), allRels.size());
    assertEquals(0, localRels.size());

    // Uncomment and adapt if sort rank and relationship ID validation is needed.
    // String[] sortRank = new String[]{"0", "0", "1", "1", "2", "3"};
    // for (int i = 0; i < sortRank.length; i++) {
    //     PSRelationship rel = allRels.get(i);
    //     String rank = rel.getProperty(PSRelationshipConfig.PDU_SORTRANK);
    //     assertTrue(sortRank[i].equals(rank));
    // }
    // assertTrue(allRels.get(0).getId() < allRels.get(1).getId());
    // assertTrue(allRels.get(2).getId() < allRels.get(3).getId());
    // assertTrue(allRels.get(0).getId() > allRels.get(2).getId());
  }
}
