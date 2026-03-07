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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.assembly.data.PSTemplateSlotAllowedContentContent;
import com.percussion.webservices.assembly.data.PSTemplateSlotArgumentsArgument;
import com.percussion.webservices.assembly.data.PSTemplateSlotType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;

/**
 * Converts objects between the classes
 * <code>com.percussion.services.assembly.data.PSTemplateSlot</code> and
 * <code>com.percussion.webservices.assembly.data.PSTemplateSlot</code>.
 */
public class PSTemplateSlotConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSTemplateSlotConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);

      m_specialProperties.add("id");
      m_specialProperties.add("type");
      m_specialProperties.add("slottype");
      m_specialProperties.add("finder");
      m_specialProperties.add("systemslot");
      m_specialProperties.add("finderArguments");
      m_specialProperties.add("arguments");
      m_specialProperties.add("slotAssociations");
      m_specialProperties.add("allowedContent");
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */

   @Override
   public Object convert(Class type, Object value)
   {
      Object result = super.convert(type, value);

      if (isClientToServer(value))
      {
         com.percussion.webservices.assembly.data.PSTemplateSlot orig =
            (com.percussion.webservices.assembly.data.PSTemplateSlot) value;

         PSTemplateSlot dest = (PSTemplateSlot) result;

         // convert id
         Long id = orig.getId();
         if (id != null)
            dest.setGUID(new PSDesignGuid(id));

         // convert type
         Converter converter = getConverter(IPSTemplateSlot.SlotType.class);
         dest.setSlottype((IPSTemplateSlot.SlotType) converter.convert(
            IPSTemplateSlot.SlotType.class, orig.getType()));

         // convert finder
         dest.setFinderName(orig.getFinder());

         // convert system slot
         dest.setSystemSlot(orig.isIsSystemSlot());

         // convert finder arguments (wrapper with a live list)
         if (orig.getArguments() != null && orig.getArguments().getArgument() != null)
         {
            Map<String, String> arguments = new HashMap<String, String>();
            for (com.percussion.webservices.assembly.data.PSTemplateSlot.Arguments.Argument argument : orig.getArguments().getArgument())
               arguments.put(argument.getName(), argument.getValue());
            dest.setFinderArguments(arguments);
         }

         // convert allowed content (wrapper with content list)
         if (orig.getAllowedContent() != null && orig.getAllowedContent().getContent() != null)
         {
            Collection<PSPair<IPSGuid, IPSGuid>> slotAssociations =
               new ArrayList<PSPair<IPSGuid, IPSGuid>>();
            for (com.percussion.webservices.assembly.data.PSTemplateSlot.AllowedContent.Content allowedContent : orig.getAllowedContent().getContent())
            {
               PSPair<IPSGuid, IPSGuid> pair = new PSPair<IPSGuid, IPSGuid>(
                     new PSDesignGuid(allowedContent.getContentTypeId()),
                     new PSDesignGuid(allowedContent.getTemplateId()));
               slotAssociations.add(pair);
            }
            dest.setSlotAssociations(slotAssociations);
         }
      }
      else
      {
         PSTemplateSlot orig = (PSTemplateSlot) value;

         com.percussion.webservices.assembly.data.PSTemplateSlot dest =
            (com.percussion.webservices.assembly.data.PSTemplateSlot) result;

         // convert id
         IPSGuid guid = orig.getGUID();
         if (guid != null)
            dest.setId(new PSDesignGuid(guid).getValue());

         // convert type -> generated DTO expects a String
         Converter converter = getConverter(PSTemplateSlotType.class);
         PSTemplateSlotType slotType = (PSTemplateSlotType) converter.convert(
            PSTemplateSlotType.class, orig.getSlottypeEnum());
         dest.setType(slotType.toString());

         // convert finder
         dest.setFinder(orig.getFinderName());

         // convert system slot
         dest.setIsSystemSlot(orig.isSystemSlot());

         // convert finder arguments into generated wrapper
         Map<String, String> finderArguments = orig.getFinderArguments();
         com.percussion.webservices.assembly.data.PSTemplateSlot.Arguments argsWrapper =
            new com.percussion.webservices.assembly.data.PSTemplateSlot.Arguments();
         for (Map.Entry<String, String> e : finderArguments.entrySet()) {
            com.percussion.webservices.assembly.data.PSTemplateSlot.Arguments.Argument argument =
               new com.percussion.webservices.assembly.data.PSTemplateSlot.Arguments.Argument();
            argument.setName(e.getKey());
            argument.setValue(e.getValue());
            argsWrapper.getArgument().add(argument);
         }
         dest.setArguments(argsWrapper);

         // convert allowed content into generated wrapper
         Collection<PSPair<IPSGuid, IPSGuid>> slotAssociations = orig.getSlotAssociations();
         com.percussion.webservices.assembly.data.PSTemplateSlot.AllowedContent allowedWrapper =
            new com.percussion.webservices.assembly.data.PSTemplateSlot.AllowedContent();
         if (slotAssociations != null)
         {
            for (PSPair<IPSGuid, IPSGuid> pair : slotAssociations)
            {
               com.percussion.webservices.assembly.data.PSTemplateSlot.AllowedContent.Content allowedContent =
                  new com.percussion.webservices.assembly.data.PSTemplateSlot.AllowedContent.Content();
               allowedContent.setContentTypeId(new PSDesignGuid(pair.getFirst()).getValue());
               allowedContent.setTemplateId(new PSDesignGuid(pair.getSecond()).getValue());
               allowedWrapper.getContent().add(allowedContent);
            }
         }
         dest.setAllowedContent(allowedWrapper);
      }

      return result;
   }
}

