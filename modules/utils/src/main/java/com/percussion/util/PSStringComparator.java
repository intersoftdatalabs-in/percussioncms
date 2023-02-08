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
package com.percussion.util;

import java.util.Comparator;

/**
 * This class is used for sorting Strings in various ways.  It is intended to be
 * used with anything that will sort lists of strings based on a supplied
 * comparator.  This class will support different types of string sorts based
 * on how it is constructed.  In order to do the comparison, the
 * <code>toString()</code> method will be called on each object supplied.
 */
public class PSStringComparator implements Comparator
{
   /**
    * Constructor for this class, specifiying type of sorting.
    *
    * @param type One of the SORT_xxx types, that specifies how the list will
    * be sorted.  Currently supported are:
    * <ol>
    * <li>{@link #SORT_CASE_INSENSITIVE_ASC}</li>
    * </ol>
    *
    * @throws IllegalArgumentException if type is a support value.
    */
   public PSStringComparator(int type)
   {
      if (!validateType(type))
         throw new IllegalArgumentException("type is not supported");

      m_type = type;
   }

   /**
    * Compares the String representations of the two objects as specified by the
    * type supplied to the ctor.
    *
    * @param o1 The first object.  May not be <code>null
    * </code>.
    * @param o2 The second object. May not be <code>null
    * </code>.
    *
    * @return a negative integer, zero, or a positive integer as the
    * first object's <code>toString()</code> value is less than, equal to, or
    * greater than that of the second object as indicated by the type of sorting
    * specified in the ctor.
    *
    * @throws IllegalArgumentException if either argument is <code>null
    * </code>.
    */
   public int compare(Object o1, Object o2)
   {
      int result = 0;

      switch (m_type)
      {
         case SORT_CASE_INSENSITIVE_ASC:
            result = o1.toString().compareToIgnoreCase(o2.toString());
            break;
            
         default:
            // this should never happen!
            throw new IllegalStateException("type value (" + m_type +
               ") is not supported");
      }

      return result;
   }

   /**
    * Validates that a valid sort type is supplied.
    *
    * @param type The type to validate.
    *
    * @return <code>true</code> if it is a supported type, <code>false</code> if
    * not.
    */
   private boolean validateType(int type)
   {
      boolean isValid = false;

      if (type == SORT_CASE_INSENSITIVE_ASC)
         isValid = true;

      return isValid;
   }

   /**
    * Constant for constructing this class to sort without regard to case, in
    * ascending aphabetical order.
    */
   public static final int SORT_CASE_INSENSITIVE_ASC = 0;

   /**
    * Constant for constructing this class to sort without regard to case, in
    * ascending aphabetical order.
    * 
    * @deprecated this constant is named incorrectly, use 
    *    <code>SORT_CASE_INSENSITIVE_ASC</code> instead as that does the same 
    *    thing and is correctly named.
    */
   public static final int SORT_CASE_SENSITIVE_ASC = SORT_CASE_INSENSITIVE_ASC;

   /**
    * The type of sorting to perform.  Initialized by the ctor, never modified
    * after that.
    */
   private int m_type;
}
