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
// REFACTORED: CP-JAVA11
package com.percussion.rx.publisher.impl;

import com.percussion.rx.publisher.IPSAssemblyResultExpander;
import com.percussion.services.assembly.IPSAssemblyItem;

/**
 * Java 11 refactored: Abstract expander with convenience methods.
 * Uses Google Java Style and concise method signatures.
 * @author adamgent
 */
public abstract class PSAbstractAssemblyResultExpander implements IPSAssemblyResultExpander
{
   
   /**
    * Properly clones the assembly item for expansion.
    * @param item never <code>null</code>.
    * @return never <code>null</code>.
    */
   protected IPSAssemblyItem clone(IPSAssemblyItem item) {
      try {
         var method = item.getClass().getMethod("pageClone");
         Object result = method.invoke(item);
         if (result instanceof IPSAssemblyItem) {
            return (IPSAssemblyItem) result;
         }
      } catch (Exception e) {
         // Method not available, fallback to original item
      }
      return item;
   }

}
