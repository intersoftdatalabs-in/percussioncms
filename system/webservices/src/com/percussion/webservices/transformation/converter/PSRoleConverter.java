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

import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSRole;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.security.data.PSRoleAttributesAttribute;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Converts objects between the classes
 * <code>com.percussion.design.objectstore.PSRole</code> and
 * <code>com.percussion.webservices.security.data.PSRole</code>.
 */
public class PSRoleConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSRoleConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */

   @Override
   public Object convert(Class type, Object value)
   {
      if (value == null)
         return null;

      if (isClientToServer(value))
      {
         com.percussion.webservices.security.data.PSRole source =
            (com.percussion.webservices.security.data.PSRole) value;

         PSRole target = new PSRole(source.getName());

         Long id = source.getId();
         if (id != null)
            target.setId((new PSGuid(id)).getUUID());

         // Attributes is a wrapper with a live list in the generated DTO
         com.percussion.webservices.security.data.PSRole.Attributes sourceAttrs = source.getAttributes();
         if (sourceAttrs != null && sourceAttrs.getAttribute() != null)
         {
            PSAttributeList targetAttrs = new PSAttributeList();
            for (com.percussion.webservices.security.data.PSRole.Attributes.Attribute sourceAttr : sourceAttrs.getAttribute())
            {
               PSAttribute targetAttr = new PSAttribute(sourceAttr.getName());
               targetAttr.setValues(sourceAttr.getValue());
               targetAttrs.add(targetAttr);
            }
         }

         return target;
      }
      else
      {
         PSRole source = (PSRole) value;

         com.percussion.webservices.security.data.PSRole target =
            new com.percussion.webservices.security.data.PSRole();

         IPSGuid guid = source.getGUID();
         if (guid != null)
            target.setId(new PSDesignGuid(guid).getValue());

         target.setName(source.getName());

         PSAttributeList sourceAttrs = source.getAttributes();
         com.percussion.webservices.security.data.PSRole.Attributes attrsWrapper =
            new com.percussion.webservices.security.data.PSRole.Attributes();
         for (int i=0; i<sourceAttrs.size(); i++)
         {
            PSAttribute sourceAttr = (PSAttribute) sourceAttrs.get(i);
            com.percussion.webservices.security.data.PSRole.Attributes.Attribute targetAttr =
               new com.percussion.webservices.security.data.PSRole.Attributes.Attribute();
            targetAttr.setName(sourceAttr.getName());
            targetAttr.getValue().addAll(sourceAttr.getValues());
            attrsWrapper.getAttribute().add(targetAttr);
         }

         target.setAttributes(attrsWrapper);
         return target;
      }
   }
}

