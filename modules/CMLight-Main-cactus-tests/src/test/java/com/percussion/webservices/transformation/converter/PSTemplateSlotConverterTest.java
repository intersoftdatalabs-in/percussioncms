/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.types.PSPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.percussion.webservices.transformation.converter.PSTemplateSlotConverter;
import org.apache.commons.lang.StringUtils;
import org.junit.experimental.categories.Category;

/**
 * Unit tests for the {@link PSTemplateSlotConverter} class.
 */
@Category(IntegrationTest.class)
public class PSTemplateSlotConverterTest extends PSConverterTestBase
{
   /**
    * Tests the conversion from a server to a client object as well as a
    * server array of objects to a client array of objects and back.
    */
   public void testConversion() throws Exception
   {
      PSTemplateSlot source = null;

      try
      {
         source = createSlot("name", getNextId(PSTypeEnum.SLOT));
         
         PSTemplateSlot target = (PSTemplateSlot) roundTripConversion(
            PSTemplateSlot.class, 
            com.percussion.webservices.assembly.data.PSTemplateSlot.class, 
            source);
         
         // verify the the round-trip object is equal to the source object
         assertTrue(source.equals(target));
         
         // create the source array
         PSTemplateSlot[] sourceArray = new PSTemplateSlot[1];
         sourceArray[0] = source;
         
         PSTemplateSlot[] targetArray = (PSTemplateSlot[]) roundTripConversion(
            PSTemplateSlot[].class, 
            com.percussion.webservices.assembly.data.PSTemplateSlot[].class, 
            sourceArray);
         
         // verify the the round-trip array is equal to the source array
         assertTrue(sourceArray.length == targetArray.length);
         assertTrue(sourceArray[0].equals(targetArray[0]));
      }
      finally
      {
         if (source != null)
         {
            IPSAssemblyService service = 
               PSAssemblyServiceLocator.getAssemblyService();
            service.deleteSlot(source.getGUID());
         }
      }
   }
   
   /**
    * Test a list of server object convert to client array, and vice versa.
    * 
    * @throws Exception if an error occurs.
    */
   @SuppressWarnings("unchecked")
   public void testListToArray() throws Exception
   {
      List<PSTemplateSlot> srcList = new ArrayList<PSTemplateSlot>();
      
      try
      {
         srcList.add(createSlot("slot_1", getNextId(PSTypeEnum.SLOT)));
         srcList.add(createSlot("slot_2", getNextId(PSTypeEnum.SLOT)));
         
         List<PSTemplateSlot> srcList2 = roundTripListConversion(
            com.percussion.webservices.assembly.data.PSTemplateSlot[].class, 
            srcList);
   
         assertTrue(srcList.equals(srcList2));
      }
      finally
      {
         for (PSTemplateSlot slot : srcList)
         {
            IPSAssemblyService service = 
               PSAssemblyServiceLocator.getAssemblyService();
            service.deleteSlot(slot.getGUID());
         }
      }
   }
   
   /**
    * Create a test slot for the specified name.
    * 
    * @param name the slot name, not <code>null</code> or empty.
    * @param id the slot id, not <code>null</code>.
    * @return the test slot, never <code>null</code>.
    * @throws PSAssemblyException if we cannot save the created template.
    */
   public static PSTemplateSlot createSlot(String name, IPSGuid id) 
      throws PSAssemblyException
   {
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("name cannot be null");
      
      if (id == null)
         throw new IllegalArgumentException("id cannot be null");
      
      PSTemplateSlot slot = new PSTemplateSlot();
      slot.setName(name);
      slot.setGUID(id);
      slot.setLabel(name + "_label");
      slot.setDescription(name + "_description");
      slot.setFinderName(name + "_findeName");
      slot.setRelationshipName(name + "_relationshipName");
      slot.setSlottype(IPSTemplateSlot.SlotType.INLINE);
      slot.setSystemSlot(true);
      
      Map<String, String> finderParams = new HashMap<String, String>();
      finderParams.put("param_1", "value_1");
      finderParams.put("param_2", "value_2");
      finderParams.put("param_3", "value_3");
      slot.setFinderArguments(finderParams);
      
      Collection<PSPair<IPSGuid, IPSGuid>> slotAssociations = 
         new ArrayList<PSPair<IPSGuid, IPSGuid>>();
      PSPair<IPSGuid, IPSGuid> pair = new PSPair<IPSGuid, IPSGuid>(
         new PSGuid(PSTypeEnum.NODEDEF, 1000), 
         new PSGuid(PSTypeEnum.TEMPLATE, 1001));
      slotAssociations.add(pair);
      pair = new PSPair<IPSGuid, IPSGuid>(
         new PSGuid(PSTypeEnum.NODEDEF, 1002), 
         new PSGuid(PSTypeEnum.TEMPLATE, 1003));
      slotAssociations.add(pair);
      slot.setSlotAssociations(slotAssociations);

      IPSAssemblyService service = 
         PSAssemblyServiceLocator.getAssemblyService();
      service.saveSlot(slot);
      
      return slot;
   }
}

