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
package com.percussion.services.assembly.jexl;

import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Date utilities, primarily for determining spans of time from a passed in date
 * value. Meant to work with the velocity tools date object.
 * 
 * @author dougrand
 */
public class PSDateUtils extends PSJexlUtilBase
{
   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the beginning of the week specified by the passed date, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the week of interest"),
         @IPSJexlParam(name = "offset", description = "the offset is used to determine whether the current week should be returned or a previous or following week. The offset can be positive or negative.")})
   public Date startOfWeek(Calendar input, int offset)
   {
      if (input == null)
      {
         throw new IllegalArgumentException("input may not be null");
      }
      return startOfWeek(input.getTime(), offset);
   }
   
   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the beginning of the week specified by the passed date, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the week of interest"),
         @IPSJexlParam(name = "offset", description = "the offset is used to determine whether the current week should be returned or a previous or following week. The offset can be positive or negative.")})
   public Date startOfWeek(Date input, int offset)
   {
      Calendar cal = new GregorianCalendar();
      cal.setTime(input);
      cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
      cal.set(Calendar.HOUR, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.add(Calendar.WEEK_OF_YEAR, offset);
      return cal.getTime();
   }   

   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the beginning of the month specified by the passed date, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the month of interest"),
         @IPSJexlParam(name = "offset", description = "the offset is used to determine whether the current month should be returned or a previous or following month. The offset can be positive or negative.")})
   public Date startOfMonth(Calendar input, int offset)
   {
      if (input == null)
      {
         throw new IllegalArgumentException("input may not be null");
      }
      return startOfMonth(input.getTime(), offset);
   }
   
   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the beginning of the month specified by the passed date, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the month of interest"),
         @IPSJexlParam(name = "offset", description = "the offset is used to determine whether the current month should be returned or a previous or following month. The offset can be positive or negative.")})
   public Date startOfMonth(Date input, int offset)
   {
      Calendar cal = new GregorianCalendar();
      cal.setTime(input);
      cal.set(Calendar.DAY_OF_MONTH, 1);
      cal.set(Calendar.HOUR, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.add(Calendar.MONTH, offset);
      return cal.getTime();
   }

   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the beginning of the year specified by the passed date, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the month of interest"),
         @IPSJexlParam(name = "offset", description = "the offset is used to determine whether the current year should be returned or a previous or following year. The offset can be positive or negative.")})
   public Date startOfYear(Calendar input, int offset)
   {
      if (input == null)
      {
         throw new IllegalArgumentException("input may not be null");
      }
      return startOfYear(input.getTime(), offset);
   }
   
   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the beginning of the year specified by the passed date, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the month of interest"),
         @IPSJexlParam(name = "offset", description = "the offset is used to determine whether the current year should be returned or a previous or following year. The offset can be positive or negative.")})
   public Date startOfYear(Date input, int offset)
   {
      Calendar cal = new GregorianCalendar();
      cal.setTime(input);
      cal.set(Calendar.DAY_OF_MONTH, 1);
      cal.set(Calendar.HOUR, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.set(Calendar.MONTH, 0);
      cal.add(Calendar.YEAR, offset);
      return cal.getTime();
   }
   
   /**
    * See descriptions
    * @param input
    * @param offset
    * @return the appropriate date
    */
   @IPSJexlMethod(description = "get a date value, representing the day computed using the passed offset relative to the date passed, set at 12:00:00.000AM", params =
   {
         @IPSJexlParam(name = "input", description = "the date, never null, which is contained in the month of interest"),
         @IPSJexlParam(name = "offset", description = "the offset added to the day of the year contained in the passed date. It may be positive or negative.")})
   public Date dayOffset(Date input, int offset)
   {
      Calendar cal = new GregorianCalendar();
      cal.setTime(input);
      cal.set(Calendar.HOUR, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.add(Calendar.DAY_OF_YEAR, offset);
      return cal.getTime();
   }   
}
