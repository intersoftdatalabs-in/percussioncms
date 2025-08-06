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
/*
 * test.percussion.pso.utils PSOSlotContentsTest.java
 *  
 * @author DavidBenua
 *
 */
package test.percussion.pso.utils;

import java.util.ArrayList;
import java.util.List;

// Removed TestCase extension for JUnit 5
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mockito.Mock;
import static org.mockito.Mockito.*;
// ...existing code...
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.utils.PSOSlotContents;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;

/**
 * 
 *
 * @author DavidBenua
 *
 */
@ExtendWith(MockitoExtension.class)
public class PSOSlotContentsTest
{
   private static final Logger log = LogManager.getLogger(PSOSlotContentsTest.class);
   
   @Mock
   private IPSContentWs cws;
   @Mock
   private IPSGuidManager gmgr;
   @Mock
   private PSAaRelationship rel1;
   @Mock
   private PSAaRelationship rel2;
   @Mock
   private PSAaRelationship rel3;
   @Mock
   private PSAaRelationship rel4;

   private List<PSAaRelationship> rels = new ArrayList<>();

   IPSTemplateSlot ourSlot;
   IPSTemplateSlot otherSlot;
   IPSAssemblyTemplate template;
   
   /**
    * @param name
    */
   /**
    * @see junit.framework.TestCase#setUp()
    */
   @BeforeEach
   void setUp()
   {
      // No super.setUp() needed for JUnit 5
   }
   /**
    * Test method for {@link com.percussion.pso.utils.PSOSlotContents#PSOSlotContents()}.
    */
   @Test
   void testPSOSlotContents()
   {
      PSOSlotContents contents = new PSOSlotContents(); 
                
      final PSLocator parent = new PSLocator(1, 1);
      final IPSGuid slot1 = new PSGuid(1L);
      final IPSGuid slot2 = new PSGuid(2L);

      rels.clear();
      rels.add(rel1);
      rels.add(rel2);
      rels.add(rel3);
      rels.add(rel4);

      try {
         // Mockito stubbing
         when(cws.loadContentRelations(any(PSRelationshipFilter.class), eq(true))).thenReturn(rels);
         when(gmgr.makeLocator(any(IPSGuid.class))).thenReturn(parent);
         when(rel1.getSlotId()).thenReturn(slot1);
         when(rel2.getSlotId()).thenReturn(slot2);
         when(rel3.getSlotId()).thenReturn(slot1);
         when(rel4.getSlotId()).thenReturn(slot1);
         when(rel1.getSortRank()).thenReturn(3);
         when(rel2.getSortRank()).thenReturn(1);
         when(rel3.getSortRank()).thenReturn(2);
         when(rel4.getSortRank()).thenReturn(1);

         contents.setCws(cws);
         contents.setGmgr(gmgr);

         List<PSAaRelationship> r2 = contents.getSlotContents(new PSGuid(3L), new PSGuid(1L));

         assertNotNull(r2);
         assertEquals(3, r2.size());
         assertEquals(1, r2.get(0).getSortRank());
         assertEquals(3, r2.get(2).getSortRank());
      } catch (PSErrorException ex) {
         fail("Unexpected Exception");
         log.error("Unexpected Exception " + ex, ex);
      }
   }
}
