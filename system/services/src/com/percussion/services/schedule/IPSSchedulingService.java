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
package com.percussion.services.schedule;

import com.percussion.services.schedule.data.PSNotificationTemplate;
import com.percussion.services.schedule.data.PSScheduledTask;
import com.percussion.services.schedule.data.PSScheduledTaskLog;
import com.percussion.utils.guid.IPSGuid;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * The scheduling service manages task schedules with modern Java 11 patterns.
 * Internally schedules are passed to the Quartz scheduler for storage and management
 * with enhanced type safety, Optional-based safe access, and Stream API integration.
 *
 * @author Doug Rand
 * @author Andriy Palamarchuk
 */
public interface IPSSchedulingService {

   /**
    * Creates a schedule with enhanced validation. The created schedule is not saved yet
    * and will have a fresh GUID assigned.
    *
    * @return the new schedule, never null
    */
   PSScheduledTask createSchedule();

   /**
    * Loads all known schedules with enhanced error handling.
    *
    * @return unmodifiable collection of schedules, never null but may be empty
    * @throws PSSchedulingException on schedule loading error
    */
   Collection<PSScheduledTask> findAllSchedules() throws PSSchedulingException;
   
   /**
    * Gets all schedules as a stream for efficient processing.
    *
    * @return stream of all schedules, never null
    * @throws PSSchedulingException on schedule loading error
    */
   default Stream<PSScheduledTask> findAllSchedulesAsStream() throws PSSchedulingException {
      return findAllSchedules().stream();
   }

   /**
    * Loads a schedule by name with safe access.
    *
    * @param label the label of the schedule, never null or empty
    * @return Optional containing the schedule if found, empty otherwise
    * @throws PSSchedulingException on schedule loading error
    * @throws IllegalArgumentException if label is null or empty
    */
   Optional<PSScheduledTask> findScheduleByName(String label) throws PSSchedulingException;

   /**
    * Loads a schedule by name with guaranteed existence.
    *
    * @param label the label of the schedule, never null or empty
    * @return the schedule, never null
    * @throws PSSchedulingException on schedule loading error or if not found
    * @throws IllegalArgumentException if label is null or empty
    */
   default PSScheduledTask loadScheduleByName(String label) throws PSSchedulingException {
      Objects.requireNonNull(label, "Schedule label cannot be null");
      if (label.trim().isEmpty()) {
         throw new IllegalArgumentException("Schedule label cannot be empty");
      }
      return findScheduleByName(label)
         .orElseThrow(() -> new PSSchedulingException("Schedule not found with label: " + label));
   }

   /**
    * Finds the scheduled task with the specified id using safe access.
    *
    * @param id the scheduled task ID, never null
    * @return Optional containing the schedule if found, empty otherwise
    * @throws PSSchedulingException on schedule loading error
    * @throws IllegalArgumentException if id is null
    */
   Optional<PSScheduledTask> findScheduledTaskById(IPSGuid id) throws PSSchedulingException;

   /**
    * Loads the scheduled task with the specified id with guaranteed existence.
    *
    * @param id the scheduled task ID, never null
    * @return the schedule, never null
    * @throws PSSchedulingException on schedule loading error or if not found
    * @throws IllegalArgumentException if id is null
    */
   default PSScheduledTask loadScheduledTaskById(IPSGuid id) throws PSSchedulingException {
      Objects.requireNonNull(id, "Schedule ID cannot be null");
      return findScheduledTaskById(id)
         .orElseThrow(() -> new PSSchedulingException("Scheduled task not found with ID: " + id));
   }

   /**
    * Checks if a scheduled task exists with the given ID.
    *
    * @param id the scheduled task ID to check, never null
    * @return true if the task exists, false otherwise
    * @throws PSSchedulingException on lookup error
    * @throws IllegalArgumentException if id is null
    */
   default boolean scheduleExists(IPSGuid id) throws PSSchedulingException {
      return findScheduledTaskById(id).isPresent();
   }

   /**
    * Updates the schedule with enhanced validation. All consequent calls will be rescheduled.
    *
    * @param schedule the task schedule, never null
    * @throws PSSchedulingException on update error
    * @throws IllegalArgumentException if schedule is null
    */
   void saveSchedule(PSScheduledTask schedule) throws PSSchedulingException;

   /**
    * Saves multiple schedules efficiently.
    *
    * @param schedules the collection of schedules to save, never null
    * @throws PSSchedulingException if any save operation fails
    * @throws IllegalArgumentException if schedules is null or contains null elements
    */
   default void saveSchedules(Collection<PSScheduledTask> schedules) throws PSSchedulingException {
      Objects.requireNonNull(schedules, "Schedules collection cannot be null");
      for (var schedule : schedules) {
         Objects.requireNonNull(schedule, "Schedule cannot be null");
         saveSchedule(schedule);
      }
   }

   /**
    * Executes the given task now with enhanced validation.
    *
    * @param schedule the task to be executed, never null
    * @throws IllegalArgumentException if schedule is null
    */
   void runNow(PSScheduledTask schedule);

   /**
    * Executes the given task asynchronously and returns a CompletableFuture.
    *
    * @param schedule the task to be executed, never null
    * @return CompletableFuture that completes when the task finishes
    * @throws IllegalArgumentException if schedule is null
    */
   default CompletableFuture<Void> runNowAsync(PSScheduledTask schedule) {
      Objects.requireNonNull(schedule, "Schedule cannot be null");
      return CompletableFuture.runAsync(() -> runNow(schedule));
   }

   /**
    * Removes the schedule from the system and cancels all scheduled calls.
    * Does nothing if the schedule with the specified id does not exist.
    * 
    * @param scheduleId the ID of the schedule to delete, never null
    * @throws PSSchedulingException on deletion failure
    * @throws IllegalArgumentException if scheduleId is null
    */
   void deleteSchedule(IPSGuid scheduleId) throws PSSchedulingException;

   /**
    * Deletes multiple schedules efficiently.
    *
    * @param scheduleIds the collection of schedule IDs to delete, never null
    * @throws PSSchedulingException if any deletion fails
    * @throws IllegalArgumentException if scheduleIds is null or contains null elements
    */
   default void deleteSchedules(Collection<IPSGuid> scheduleIds) throws PSSchedulingException {
      Objects.requireNonNull(scheduleIds, "Schedule IDs collection cannot be null");
      for (var scheduleId : scheduleIds) {
         Objects.requireNonNull(scheduleId, "Schedule ID cannot be null");
         deleteSchedule(scheduleId);
      }
   }

   /**
    * Creates a schedule notification template with enhanced validation.
    * The created template is not saved yet and will have a fresh GUID assigned.
    *
    * @return the new template, never null
    */
   PSNotificationTemplate createNotificationTemplate();
   
   /**
    * Loads all known schedule notification templates.
    * 
    * @return unmodifiable collection of templates, never null but may be empty
    */
   Collection<PSNotificationTemplate> findAllNotificationTemplates();
   
   /**
    * Gets all notification templates as a stream for efficient processing.
    *
    * @return stream of all notification templates, never null
    */
   default Stream<PSNotificationTemplate> findAllNotificationTemplatesAsStream() {
      return findAllNotificationTemplates().stream();
   }

   /**
    * Provides set with the notification labels.
    *
    * @return unmodifiable set of notification labels, never null
    */
   Set<String> findAllNotificationTemplatesNames();

   /**
    * Loads the schedule notification template with the specified id using safe access.
    *
    * @param id the schedule notification template id, never null
    * @return Optional containing the template if found, empty otherwise
    * @throws IllegalArgumentException if id is null
    */
   Optional<PSNotificationTemplate> findNotificationTemplateById(IPSGuid id);

   /**
    * Loads the schedule notification template with the specified id with guaranteed existence.
    *
    * @param id the schedule notification template id, never null
    * @return the notification template, never null
    * @throws IllegalStateException if template not found
    * @throws IllegalArgumentException if id is null
    */
   default PSNotificationTemplate loadNotificationTemplateById(IPSGuid id) {
      Objects.requireNonNull(id, "Template ID cannot be null");
      return findNotificationTemplateById(id)
         .orElseThrow(() -> new IllegalStateException(
            "Notification template not found with ID: " + id));
   }

   /**
    * Loads a schedule notification template by name with safe access.
    *
    * @param name the name of the notification template, never null or empty
    * @return Optional containing the template if found, empty otherwise
    * @throws IllegalArgumentException if name is null or empty
    */
   Optional<PSNotificationTemplate> findNotificationTemplateByName(String name);

   /**
    * Loads a schedule notification template by name with guaranteed existence.
    *
    * @param name the name of the notification template, never null or empty
    * @return the notification template, never null
    * @throws IllegalStateException if template not found
    * @throws IllegalArgumentException if name is null or empty
    */
   default PSNotificationTemplate loadNotificationTemplateByName(String name) {
      Objects.requireNonNull(name, "Template name cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("Template name cannot be empty");
      }
      return findNotificationTemplateByName(name)
         .orElseThrow(() -> new IllegalStateException(
            "Notification template not found with name: " + name));
   }

   /**
    * Updates the schedule notification template with enhanced validation.
    *
    * @param notificationTemplate the notification template, never null
    * @throws IllegalArgumentException if notificationTemplate is null
    */
   void saveNotificationTemplate(PSNotificationTemplate notificationTemplate);

   /**
    * Removes the schedule notification template from the system.
    * Fails if there are schedules using this notification template.
    *
    * @param templateId the notification template id, never null
    * @throws IllegalArgumentException if templateId is null
    */
   void deleteNotificationTemplate(IPSGuid templateId);
   
   /**
    * Creates a new GUID for task log entries.
    *
    * @return a new GUID for the task log, never null
    */
   IPSGuid createTaskLogId();
   
   /**
    * Loads a specified task log with safe access.
    *
    * @param id the ID of the task log, never null
    * @return Optional containing the task log if found, empty otherwise
    * @throws IllegalArgumentException if id is null
    */
   Optional<PSScheduledTaskLog> findTaskLogById(IPSGuid id);

   /**
    * Loads a specified task log with guaranteed existence.
    *
    * @param id the ID of the task log, never null
    * @return the task log, never null
    * @throws IllegalStateException if task log not found
    * @throws IllegalArgumentException if id is null
    */
   default PSScheduledTaskLog loadTaskLogById(IPSGuid id) {
      Objects.requireNonNull(id, "Task log ID cannot be null");
      return findTaskLogById(id)
         .orElseThrow(() -> new IllegalStateException(
            "Task log not found with ID: " + id));
   }

   /**
    * Saves the supplied task log with enhanced validation.
    *
    * @param taskLog the task log to be saved, never null
    * @throws IllegalArgumentException if taskLog is null
    */
   void saveTaskLog(PSScheduledTaskLog taskLog);

   /**
    * Deletes a specified task log.
    *
    * @param id the ID of the task log to delete, never null
    * @throws IllegalArgumentException if id is null
    */
   void deleteTaskLog(IPSGuid id);
   
   /**
    * Deletes multiple task log entries efficiently.
    *
    * @param ids the IDs of the task log entries to delete, never null, may be empty
    * @throws IllegalArgumentException if ids is null or contains null elements
    */
   void deleteTaskLogs(Collection<IPSGuid> ids);
   
   /**
    * Gets all task log entries with enhanced documentation.
    * Note: the problemDesc property will not be loaded for performance reasons.
    *
    * @param maxResult the max number of log entries to retrieve, no limit if <= 0
    * @return unmodifiable list of log entries sorted by end time (descending), never null
    */
   List<PSScheduledTaskLog> findAllTaskLogs(int maxResult);

   /**
    * Gets all task log entries as a stream for efficient processing.
    *
    * @param maxResult the max number of log entries to retrieve, no limit if <= 0
    * @return stream of task log entries, never null
    */
   default Stream<PSScheduledTaskLog> findAllTaskLogsAsStream(int maxResult) {
      return findAllTaskLogs(maxResult).stream();
   }

   /**
    * Remove all task log entries.
    */
   void deleteAllTaskLogs();
   
   /**
    * Delete all task log entries older than the specified date.
    *
    * @param beforeDate the cutoff date for deletion, never null
    * @throws IllegalArgumentException if beforeDate is null
    */
   void deleteTaskLogsByDate(Date beforeDate);

   /**
    * Delete all task log entries older than the specified LocalDateTime.
    *
    * @param beforeDateTime the cutoff date/time for deletion, never null
    * @throws IllegalArgumentException if beforeDateTime is null
    */
   default void deleteTaskLogsByDateTime(LocalDateTime beforeDateTime) {
      Objects.requireNonNull(beforeDateTime, "Before date/time cannot be null");
      var date = java.sql.Timestamp.valueOf(beforeDateTime);
      deleteTaskLogsByDate(date);
   }

   /**
    * Gets the count of all scheduled tasks.
    *
    * @return the total number of scheduled tasks
    * @throws PSSchedulingException on counting error
    */
   default long getScheduleCount() throws PSSchedulingException {
      return findAllSchedules().size();
   }

   /**
    * Gets the count of all notification templates.
    *
    * @return the total number of notification templates
    */
   default long getNotificationTemplateCount() {
      return findAllNotificationTemplates().size();
   }

   /**
    * Gets the count of all task logs.
    *
    * @param maxResult the max number to count, no limit if <= 0
    * @return the number of task log entries
    */
   default long getTaskLogCount(int maxResult) {
      return findAllTaskLogs(maxResult).size();
   }
}
