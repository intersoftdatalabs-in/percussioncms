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

import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemChild;
import com.percussion.cms.objectstore.PSItemChildEntry;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.webservices.content.PSChildEntry;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;

/**
 * Converts objects between the classes
 * {@link com.percussion.cms.objectstore.PSItemChildEntry} and
 * {@link com.percussion.webservices.content.PSChildEntry}
 */
public class PSItemChildEntryConverter extends PSConverter
{
   /**
    * @see PSConverter#PSConverter(BeanUtilsBean)
    */
   public PSItemChildEntryConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */

   @Override
   public Object convert(@SuppressWarnings("unused") Class type, Object value)
   {
      if (value == null)
         return null;

      try
      {
         if (isClientToServer(value))
         {
            PSChildEntry orig = (PSChildEntry) value;

            PSLegacyGuid guid = new PSLegacyGuid(orig.getId());
            PSItemDefinition itemDefinition =
               PSItemConverterUtils.getItemDefinition(guid.getContentTypeId());

            PSCoreItem item = new PSCoreItem(itemDefinition);
            PSItemChild child = item.getChildById(guid.getChildId());
            if (child == null)
               throw new IllegalArgumentException("Invalid child entry id");

            PSItemChildEntry dest = child.createChildEntry();
            dest.setAction(orig.getAction());
            dest.setChildRowId(guid.getUUID());

            java.util.List<com.percussion.webservices.content.PSField> fields = orig.getPSField();
            com.percussion.webservices.content.PSField[] fa = fields == null ? new com.percussion.webservices.content.PSField[0] : fields.toArray(new com.percussion.webservices.content.PSField[fields.size()]);
            PSItemConverterUtils.toServerFields(dest, guid, fa);

            return dest;
         }
         else
         {
            PSItemChildEntry orig = (PSItemChildEntry) value;

            PSLegacyGuid guid = orig.getGUID();
            if (guid == null)
               throw new IllegalArgumentException(
                  "guid must be set on the child entry");
            PSItemDefinition itemDefinition =
               PSItemConverterUtils.getItemDefinition(guid.getContentTypeId());

            PSChildEntry dest = new PSChildEntry();

            dest.setAction(orig.getAction());
            dest.setId((new PSDesignGuid(guid)).getValue());
            com.percussion.webservices.content.PSField[] fields = PSItemConverterUtils.toClientFields(
               orig.getAllFields(), itemDefinition.getName(), this);
            java.util.Collections.addAll(dest.getPSField(), fields);

            return dest;
         }
      }
      catch (Exception e)
      {
         throw new ConversionException(e.getLocalizedMessage());
      }
   }
}

