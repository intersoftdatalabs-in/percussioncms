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

import com.percussion.cms.objectstore.PSItemFieldMeta;

import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Converts objects between the classes
 * {@link Integer} and 
 * {@link com.percussion.webservices.content.PSFieldSourceType}.
 */
public class PSFieldSourceTypeConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSFieldSourceTypeConverter(BeanUtilsBean beanUtils)
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
         // client likely sends a string or an enum-like object
         String s = value.toString();
         Integer dest = 0;
         if (s.equalsIgnoreCase("local"))
            dest = PSItemFieldMeta.SOURCE_TYPE_LOCAL;
         else if (s.equalsIgnoreCase("shared"))
            dest = PSItemFieldMeta.SOURCE_TYPE_SHARE;
         else if (s.equalsIgnoreCase("system"))
            dest = PSItemFieldMeta.SOURCE_TYPE_SYSTEM;
         else
            dest = PSItemFieldMeta.SOURCE_TYPE_UNKNOWN;

         return dest;
      }
      else
      {
         Integer orig = (Integer) value;

         switch (orig)
         {
            case PSItemFieldMeta.SOURCE_TYPE_LOCAL:
               return "local";
            case PSItemFieldMeta.SOURCE_TYPE_SHARE:
               return "shared";
            case PSItemFieldMeta.SOURCE_TYPE_SYSTEM:
               return "system";
            default:
               return "unknown";
         }
      }
   }
}
