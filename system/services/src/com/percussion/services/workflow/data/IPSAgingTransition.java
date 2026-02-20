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

package com.percussion.services.workflow.data;


/**
 * The interface for aging transition
 */
public interface IPSAgingTransition extends IPSTransitionBase
{
   /**
    * Get the type of this aging transition as a textual name (for compatibility
    * with IPSCatalogIdentifier#getType()).
    * 
    * @return the aging transition type name, may be {@code null}.
    */
   String getType();

   /**
    * Convenience method to get the type as the enum value.
    * Default implementation maps the textual name to the enum, with ABSOLUTE
    * as a safe fallback.
    *
    * @return the enum value, never {@code null}
    */
   /**
    * Convenience method to get the aging transition type as an enum.
    * Renamed to avoid clash with {@code IPSCatalogIdentifier#getTypeEnum()}.
    */
   default PSAgingTransition.PSAgingTypeEnum getAgingTypeEnum() {
      String t = getType();
      if (t == null) return PSAgingTransition.PSAgingTypeEnum.ABSOLUTE;
      try { return PSAgingTransition.PSAgingTypeEnum.valueOf(t); } catch (Exception e) { return PSAgingTransition.PSAgingTypeEnum.ABSOLUTE; }
   }

   /**
    * Set the type of this aging transition.
    * 
    * @param agingType the aging transition type, may not be <code>null</code>.
    */   
   void setType(PSAgingTransition.PSAgingTypeEnum agingType);
   
   /**
    * Get the aging interval in minutes. this is only useful for types
    * absolute and repeated.
    * 
    * @return the aging time in minutes.
    */
   long getInterval();
   
   /**
    * Set the aging interval in minutes. this is only useful for types
    * absolute and repeated.
    * 
    * @param agingInterval The interval in minutes
    */
   void setInterval(long agingInterval);
   
   /**
    * Get the system field from which to get the aging time. This is only 
    * useful for type systemField.
    * 
    * @return the name of the system field from which to get the aging time,
    *    may be <code>null</code> or empty.
    */
   String getSystemField();
   
   /**
    * Set the system field name
    * 
    * @param name The name, may be <code>null</code> or empty.
    */
   void setSystemField(String name);
}
