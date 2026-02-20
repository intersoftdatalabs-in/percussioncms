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
import org.apache.commons.beanutils.ConversionException;

/**
 * Converts objects between the classes
 * {@link Integer} and 
 * {@link com.percussion.webservices.content.PSFieldSourceType}.
 */
public class PSFieldTransferEncodingConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSFieldTransferEncodingConverter(BeanUtilsBean beanUtils)
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
         String s = value.toString();
         
         Integer dest = 0;
         if (s.equalsIgnoreCase("base64"))
            dest = PSItemFieldMeta.ENCODING_TYPE_BASE64;
         else if (s.equalsIgnoreCase("none"))
            dest = PSItemFieldMeta.ENCODING_TYPE_NONE;
         else
            throw new ConversionException(
               "unknown item field transfer encoding");
         
         return dest;
      }
      else
      {
         Integer orig = (Integer) value;
         
         switch (orig)
         {
            case PSItemFieldMeta.ENCODING_TYPE_BASE64:
               return "base64";
            case PSItemFieldMeta.ENCODING_TYPE_NONE:
               return "none";
            default:
               throw new ConversionException(
                  "unknown item field transfer encoding");
         }
      }
   }
}
