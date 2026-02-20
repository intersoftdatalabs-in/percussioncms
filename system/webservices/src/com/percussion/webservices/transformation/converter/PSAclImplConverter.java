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

import com.percussion.services.security.IPSAclEntry;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.data.PSAclImpl;
import com.percussion.webservices.transformation.impl.PSTransformerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;

/**
 * Convert between {@link com.percussion.services.security.data.PSAclImpl} and
 * {@link com.percussion.webservices.system.PSAclImpl}
 */
public class PSAclImplConverter extends PSConverter
{

   /**
    * See {@link PSConverter#PSConverter(BeanUtilsBean) super()}
    * @param beanUtils
    */
   public PSAclImplConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
      m_specialProperties.add("entries");
      m_specialProperties.add("guid");
      m_specialProperties.add("permissions");
      m_specialProperties.add("version");
   }

   @Override
   public Object convert(Class type, Object value)
   {
      if (value == null)
         return null;

      Object result = super.convert(type, value);

      if (isClientToServer(value))
      {
         com.percussion.webservices.system.PSAclImpl src =
            (com.percussion.webservices.system.PSAclImpl) value;
         PSAclImpl tgt = (PSAclImpl) result;

         Converter converter = PSTransformerFactory.getInstance().getConverter(
            com.percussion.webservices.system.PSAclEntryImpl.class);

         Object entriesObj = src.getEntries();
         if (entriesObj != null) {
            if (entriesObj instanceof com.percussion.webservices.system.PSAclEntryImpl[]) {
               for (com.percussion.webservices.system.PSAclEntryImpl entry : (com.percussion.webservices.system.PSAclEntryImpl[]) entriesObj) {
                  tgt.addEntry((PSAclEntryImpl) converter.convert(PSAclEntryImpl.class, entry));
               }
            } else if (entriesObj instanceof java.util.List) {
               for (Object entryObj : (java.util.List<?>) entriesObj) {
                  tgt.addEntry((PSAclEntryImpl) converter.convert(PSAclEntryImpl.class, entryObj));
               }
            } else {
               try {
                  Object list = null;
                  try {
                     list = getBeanUtils().getPropertyUtils().getSimpleProperty(entriesObj, "entry");
                  } catch (Exception e1) {
                     try {
                        list = getBeanUtils().getPropertyUtils().getSimpleProperty(entriesObj, "psAclEntryImpl");
                     } catch (Exception e2) {
                        try {
                           list = getBeanUtils().getPropertyUtils().getSimpleProperty(entriesObj, "PSAclEntryImpl");
                        } catch (Exception e3) {
                           // unknown wrapper shape; leave list null
                        }
                     }
                  }

                  if (list instanceof java.util.List) {
                     for (Object entryObj : (java.util.List<?>) list) {
                        tgt.addEntry((PSAclEntryImpl) converter.convert(PSAclEntryImpl.class, entryObj));
                     }
                  } else if (list != null && list.getClass().isArray()) {
                     int len = java.lang.reflect.Array.getLength(list);
                     for (int i = 0; i < len; i++) {
                        Object entryObj = java.lang.reflect.Array.get(list, i);
                        tgt.addEntry((PSAclEntryImpl) converter.convert(PSAclEntryImpl.class, entryObj));
                     }
                  }
               } catch (Exception e) {
                  throw new RuntimeException(e);
               }
            }
         }
      }
      else
      {
         PSAclImpl src = (PSAclImpl) value;
         com.percussion.webservices.system.PSAclImpl tgt =
            (com.percussion.webservices.system.PSAclImpl) result;

         Collection<IPSAclEntry> entrySet = src.getEntries();
         List<com.percussion.webservices.system.PSAclEntryImpl> entries =
            new ArrayList<com.percussion.webservices.system.
               PSAclEntryImpl>();

         Converter converter = PSTransformerFactory.getInstance().getConverter(
            PSAclEntryImpl.class);
         for (IPSAclEntry entry : entrySet)
         {
            entries.add((com.percussion.webservices.system.PSAclEntryImpl)
               converter.convert(
                  com.percussion.webservices.system.PSAclEntryImpl.class,
                  entry));
         }

         // Wrap entries in the generated Entries wrapper (uses live list accessor)
         com.percussion.webservices.system.PSAclImpl.Entries entriesWrapper =
               new com.percussion.webservices.system.PSAclImpl.Entries();
         entriesWrapper.getPSAclEntryImpl().addAll(entries);
         tgt.setEntries(entriesWrapper);
      }

      return result;
   }
}

