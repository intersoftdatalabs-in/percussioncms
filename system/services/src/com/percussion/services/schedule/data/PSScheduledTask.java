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
package com.percussion.services.schedule.data;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;

/**
 * This represents a single recorded periodic task. The tasks are loaded at
 * system start to create jobs that are periodically run using Quartz scheduler.
 * <p>
 * This schedule data is stored by Quartz in serialized form with Quartz job 
 * properties. The class has been modernized with Java 11 features for better
 * maintainability and type safety.
 *
 * @author Doug Rand
 * @since Java 11 Modernization
 */
public class PSScheduledTask extends PSJob {

   /**
    * Creates a new scheduled task with default values.
    */
   public PSScheduledTask() {
      // Default constructor
   }
   
   /**
    * Note, this implementation copies only properties common for this schedule
    * and the provided one.
    * {@inheritDoc}
    */
   @Override
   public void apply(PSJob schedule) {
      super.apply(schedule);
      if (schedule instanceof PSScheduledTask scheduledTask) {
         setCronSpecification(scheduledTask.getCronSpecification());
      }
   }

   /**
    * The cron specification is a blank separated list of cron specifications.
    * This is documented as part of the Quartz {@link CronTrigger} class as well
    * as most UNIX documentation.
    *
    * @return the cron specification, never {@code null} or blank after it is set
    */
   public String getCronSpecification() {
      return m_cronSpecification;
   }

   /**
    * Sets the cron specification for this scheduled task.
    *
    * @param cronSpecification the cron specification to set, must not be {@code null} or blank
    * @throws IllegalArgumentException if cronSpecification is {@code null} or blank
    * @see #getCronSpecification()
    */
   public void setCronSpecification(String cronSpecification) {
      if (StringUtils.isBlank(cronSpecification)) {
         throw new IllegalArgumentException("cronSpecification must not be null or blank");
      }
      m_cronSpecification = cronSpecification;
   }

   /**
    * Compares schedules by label using a modern comparator implementation.
    */
   public static class ByLabelComparator implements Comparator<PSScheduledTask> {

      /**
       * {@inheritDoc}
       */
      @Override
      public int compare(PSScheduledTask s1, PSScheduledTask s2) {
         if (s1 == null && s2 == null) {
            return 0;
         }
         if (s1 == null) {
            return -1;
         }
         if (s2 == null) {
            return 1;
         }

         var name1 = Optional.ofNullable(s1.getName()).orElse("");
         var name2 = Optional.ofNullable(s2.getName()).orElse("");
         return name1.compareTo(name2);
      }
   }

   /**
    * Returns a string representation of this scheduled task.
    *
    * @return a descriptive string containing the task name, ID, cron specification,
    *         and optionally the server name
    */
   @Override
   public String toString() {
      var baseInfo = String.format("%s(%s) Cron=%s",
         getName(),
         getId().getUUID(),
         getCronSpecification());

      return Optional.ofNullable(getServer())
         .filter(server -> !StringUtils.isBlank(server))
         .map(server -> baseInfo + " Server=" + server)
         .orElse(baseInfo);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSScheduledTask that)) return false;
      if (!super.equals(obj)) return false;

      return Objects.equals(m_cronSpecification, that.m_cronSpecification);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), m_cronSpecification);
   }
   
   /**
    * Serialized class version number.
    */
   private static final long serialVersionUID = 1L;

   /**
    * The cron specification for this scheduled task.
    *
    * @see #getCronSpecification()
    * @see #setCronSpecification(String)
    */
   private String m_cronSpecification;
}
