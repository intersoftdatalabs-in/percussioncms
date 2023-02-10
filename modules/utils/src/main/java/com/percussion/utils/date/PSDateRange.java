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
package com.percussion.utils.date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class to encapsulate a date range. The granularity calculation functions all
 * ignore the time component and the dates they return have the time component
 * set to 0's, even if the original start and end dates did.
 * 
 * @author paulhoward
 * 
 */
public class PSDateRange
{
   /**
    * Units that the date range can be broken down into.
    */
   public enum Granularity {
      DAY, MONTH, WEEK, YEAR
   }

   /**
    * Convenience ctor that calls
    * {@link #PSDateRange(Date, Date, com.percussion.utils.date.PSDateRange.Granularity)}
    * as <code>PSDateRange(start, end, null)</code>.
    */
   public PSDateRange(Date start, Date end)
   {
      this(start, end, null);
   }

   /**
    * Ctor
    * 
    * @param start the inclusive start date of the range, if <code>null</code>
    *            then it will default to today's date with the time component
    *            set to 0's.
    * @param end the inclusive end date of a range, if <code>null</code> it
    *            will default to today's date with the time component set to
    *            0's.
    * @param granularity value of DAY, MONTH, WEEK, or YEAR. If
    *            <code>null</code> then will default to DAY.
    */
   public PSDateRange(Date start, Date end, Granularity granularity)
   {
      if (start == null)
      {
         DateTime tmp = new DateTime();
         this.start = new DateTime(tmp.getYear(), tmp.getMonthOfYear(), tmp.getDayOfMonth(), 0, 0, 0, 0);
      }
      else
         this.start = new DateTime(start);

      if (end == null)
      {
         DateTime tmp = new DateTime();
         this.end = new DateTime(tmp.getYear(), tmp.getMonthOfYear(), tmp.getDayOfMonth(), 0, 0, 0, 0);
      }
      else
         this.end = new DateTime(end);
      if (this.start.compareTo(this.end) > 0)
         throw new IllegalArgumentException("Start date cannot be greater then the end date.");

      if (granularity == null)
         granularity = Granularity.DAY;
      this.granularity = granularity;
   }

   /**
    * Convenience ctor that calls
    * {@link #PSDateRange(Date, com.percussion.utils.date.PSDateRange.Granularity, int)}
    * as <code>PSDateRange(null, granularity, duration)</code>.
    */
   public PSDateRange(Granularity granularity, int duration)
   {
      this(null, granularity, duration);
   }
   
   /**
    * Constructs a new date range which extends backwards from the
    * specified start date according to the specified granularity
    * and duration.  The end date of the range is always one day
    * prior to the specified start date.  The start and end dates
    * of the range are inclusive.
    * 
    * @param start the date from which the range will extend backward,
    *            if <code>null</code> then it will default to today's
    *            date with the time component set to 0's.
    * @param granularity value of DAY, MONTH, WEEK, or YEAR.  Must not
    *            be <code>null</code>.
    * @param duration number of granularity periods to extend backward.
    *            Must be >= 0.
    */
   public PSDateRange(Date start, Granularity granularity, int duration)
   {
      if (granularity == null)
      {
         throw new IllegalArgumentException("granularity may not be null");
      }
      
      if (duration < 0)
      {
         throw new IllegalArgumentException("duration must be greater than or equal to zero");
      }
      
      DateTime tmp;
      if (start == null)
      {
         tmp = new DateTime();
         tmp = new DateTime(tmp.getYear(), tmp.getMonthOfYear(), tmp.getDayOfMonth(), 0, 0, 0, 0);
      }
      else
         tmp = new DateTime(start);
      
      switch (granularity)
      {
         case DAY :
         {
            this.start = tmp.minusDays(duration);
            break;
         }

         case WEEK :
         {
            this.start = tmp.minusWeeks(duration);
            break;
         }

         case MONTH :
         {
            this.start = tmp.minusMonths(duration);
            break;
         }

         case YEAR :
         {
            this.start = tmp.minusYears(duration);
            break;
         }
      }
     
      end = tmp.minusDays(1);
      
      this.granularity = granularity;
   }
   
   /**
    * @return the start date supplied in ctor or the date the range was created
    *         w/ no time component, never <code>null</code>.
    */
   public Date getStart()
   {
      return start.toDate();
   }

   /**
    * @return the end date supplied in ctor or the date the range was created w/
    *         no time component, never <code>null</code>.
    */
   public Date getEnd()
   {
      return end.toDate();
   }

   /**
    * @return the granularity supplied in ctor, or
    *         {@link PSDateRange.Granularity#DAY}, never <code>null</code>.
    */
   public Granularity getGranularity()
   {
      return granularity;
   }

   /**
    * Calculates the starting date for each full or partial unit of granularity
    * in this range. Each unit's range goes from the current index's date up to,
    * but not including the next date in the list. The last date entry's
    * duration goes to the end date of this range.
    * 
    * @return Never <code>null</code>. The number of entries depend on the
    *         range size and granularity, but will always have at least 1 entry.
    */
   public List<Date> getGranularityBreakdown()
   {
      List<Date> result = new ArrayList<Date>();
      // clear time portion of date
      DateTime tmp = new DateTime(start.getYear(), start.getMonthOfYear(), start.getDayOfMonth(), 0, 0, 0, 0);
      switch (granularity)
      {
         case DAY :
         {
            while (tmp.compareTo(end) <= 0)
            {
               result.add(tmp.toDate());
               tmp = tmp.plusDays(1);
            }
            break;
         }

         case WEEK :
         {
            int day = tmp.getDayOfWeek();
            int leadingDays = day == DateTimeConstants.SUNDAY ? 0 : DAYS_IN_WEEK - day;
            if (leadingDays > 0)
               result.add(tmp.toDate());
            tmp = tmp.plusDays(leadingDays);
            while (tmp.compareTo(end) <= 0)
            {
               result.add(tmp.toDate());
               tmp = tmp.plusWeeks(1);
            }
            break;
         }

         case MONTH :
         {
            while (tmp.compareTo(end) <= 0)
            {
               result.add(tmp.toDate());
               tmp = tmp.plusMonths(1);
               tmp = new DateTime(tmp.getYear(), tmp.getMonthOfYear(), 1, 0, 0, 0, 0);
            }
            break;
         }

         case YEAR :
         {
            while (tmp.compareTo(end) <= 0)
            {
               result.add(tmp.toDate());
               tmp = new DateTime(tmp.getYear() + 1, 1, 1, 0, 0, 0, 0);
            }
            break;
         }
      }

      return result;
   }

   /**
    * @return The number of days spanned by this range, regardless of the granularity setting.
    */
   public int getDaysInRange()
   {
      return Days.daysIn(new Interval(start, end.plusDays(1))).getDays();
   }

   
   /**
    * @return The number of calendar weeks spanned by this range, regardless of
    *         the granularity setting. Partial weeks at the beginning or end of
    *         the range are included. A week is assumed to begin on Sun.
    */
   public int getWeeksInRange()
   {
      // DateTime uses ISO cal, which starts weeks on Mon, adjust so sun=1, sat=7
      int startDay = start.getDayOfWeek();
      startDay += startDay == DateTimeConstants.SUNDAY ? -6 : 1;
      int endDay = end.getDayOfWeek();
      endDay += endDay == DateTimeConstants.SUNDAY ? -6 : 1;

      // calculate any partial weeks at beginning/end of range
      int totalDays = getDaysInRange();
      final int SUN = 1, SAT = 7;
      int leadingDays = startDay == SUN ? 0 : 8 - startDay;
      int traillingDays = endDay == SAT ? 0 : endDay;
      
      assert ((totalDays - leadingDays - traillingDays) % DAYS_IN_WEEK == 0);
      int weeks = (totalDays - leadingDays - traillingDays) / DAYS_IN_WEEK;
      if (leadingDays > 0)
         weeks++;
      if (traillingDays > 0)
         weeks++;

      return weeks;
   }

   /**
    * @return The number of calendar months spanned by this range, regardless of
    *         the granularity setting. Partial months at the beginning or end of
    *         the range are included.
    */
   public int getMonthsInRange()
   {
      int startMonth = start.getMonthOfYear();
      int startYear = start.getYear();

      int endMonth = end.getMonthOfYear();
      int endYear = end.getYear();

      int months;
      if (startYear == endYear)
         months = endMonth - startMonth + 1;
      else
         months = 12 * (endYear - startYear - 1) + (13 - startMonth) + endMonth;

      return months;
   }

   /**
    * @return The number of calendar years spanned by this range, regardless of
    *         the granularity setting. Partial years at the beginning or end of
    *         the range are included.
    */
   public int getYearsInRange()
   {
      int startYear = start.getYear();
      int endYear = end.getYear();
      return endYear - startYear + 1;
   }

   /**
    * Storage for the start date supplied in ctor. Never changed after
    * construction.
    */
   private DateTime start;

   /**
    * Storage for the end date supplied in ctor. Never changed after
    * construction.
    */
   private DateTime end;

   /**
    * Storage for the granularity supplied in ctor. Never changed after
    * construction.
    */
   private Granularity granularity;
   
   private final int DAYS_IN_WEEK = 7;
}
