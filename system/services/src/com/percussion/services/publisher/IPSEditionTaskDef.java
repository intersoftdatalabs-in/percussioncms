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
package com.percussion.services.publisher;

import com.percussion.utils.guid.IPSGuid;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The Edition Task Definition. It defines a pre or post task that will be
 * executed when publishing an Edition. This interface provides modern Java 11
 * patterns for task management with enhanced validation and safe parameter access.
 */
public interface IPSEditionTaskDef {

   /**
    * Get the task ID, the task ID is the primary key identifying a particular task.
    *
    * @return the task ID, never {@code null} for a persisted task
    */
   IPSGuid getTaskId();

   /**
    * Set the task ID.
    *
    * @param taskId the task ID, never {@code null}
    * @throws IllegalArgumentException if taskId is null
    */
   void setTaskId(IPSGuid taskId);

   /**
    * Get the parent edition ID.
    *
    * @return the edition ID, never {@code null} for a valid task
    */
   IPSGuid getEditionId();

   /**
    * Set the parent edition ID.
    *
    * @param editionId the associated edition, never {@code null}
    * @throws IllegalArgumentException if editionId is null
    */
   void setEditionId(IPSGuid editionId);

   /**
    * Get the sequence number.
    * 
    * @return the sequence of this task, a negative sequence indicates the task
    *         should be executed before the edition, with the smaller value
    *         going first. A positive sequence indicates the task should be
    *         executed after the edition, a smaller value going first.
    */
   int getSequence();

   /**
    * Set the sequence number.
    * 
    * @param sequence the sequence number for task ordering
    */
   void setSequence(int sequence);

   /**
    * Get the extension name to be run. The extension must reference an
    * {@link IPSEditionTaskDef}.
    * 
    * @return the name of the extension to be run, may be {@code null}
    */
   String getExtensionName();

   /**
    * Get the extension name safely with Optional wrapper.
    *
    * @return Optional containing the extension name, or empty if not set
    */
   default Optional<String> getExtensionNameSafely() {
      return Optional.ofNullable(getExtensionName())
                     .filter(name -> !name.trim().isEmpty());
   }

   /**
    * Set the extension name.
    * 
    * @param extensionName the extension to be run, may be {@code null}
    */
   void setExtensionName(String extensionName);

   /**
    * Get the continue on failure flag.
    * 
    * @return if this flag is {@code true} then this task can fail and
    *         the remaining tasks will be run. {@code false} indicates
    *         that if this task fails, then all future tasks should be
    *         cancelled. If a pre-edition task fails and this value is
    *         {@code false} then the edition will not be run.
    */
   boolean getContinueOnFailure();

   /**
    * Set the continue on failure flag.
    * 
    * @param continueOnFailure the failure handling behavior
    */
   void setContinueOnFailure(boolean continueOnFailure);

   /**
    * Get the parameters for this task.
    * 
    * @return the parameters, never {@code null} but could be empty
    */
   Map<String, String> getParams();

   /**
    * If the passed parameter exists then this method updates the value,
    * otherwise this method will add the parameter.
    * 
    * @param parameterName the parameter name, never {@code null} or empty
    * @param value the parameter value, if {@code null} or empty the
    *            parameter will be removed
    * @throws IllegalArgumentException if parameterName is null or empty
    */
   void setParam(String parameterName, String value);

   /**
    * Remove the given parameter.
    *
    * @param name the parameter to remove, never {@code null} or empty
    * @throws IllegalArgumentException if name is null or empty
    */
   void removeParam(String name);

   /**
    * Get a parameter value safely with Optional wrapper.
    *
    * @param parameterName the parameter name to retrieve, never {@code null}
    * @return Optional containing the parameter value, or empty if not present
    * @throws IllegalArgumentException if parameterName is null
    */
   default Optional<String> getParamSafely(String parameterName) {
      Objects.requireNonNull(parameterName, "Parameter name cannot be null");
      return Optional.ofNullable(getParams().get(parameterName))
                     .filter(value -> !value.trim().isEmpty());
   }

   /**
    * Check if a parameter exists and has a non-empty value.
    *
    * @param parameterName the parameter name to check, never {@code null}
    * @return true if the parameter exists and has a non-empty value
    * @throws IllegalArgumentException if parameterName is null
    */
   default boolean hasParam(String parameterName) {
      return getParamSafely(parameterName).isPresent();
   }

   /**
    * Get the number of parameters configured for this task.
    *
    * @return the parameter count, always non-negative
    */
   default int getParamCount() {
      return getParams().size();
   }

   /**
    * Check if this task has any parameters configured.
    *
    * @return true if parameters exist, false otherwise
    */
   default boolean hasParams() {
      return !getParams().isEmpty();
   }

   /**
    * Get the Hibernate version for optimistic locking.
    *
    * @return the version number, {@code null} for unsaved objects
    */
   Integer getVersion();

   /**
    * Get the version safely with Optional wrapper.
    *
    * @return Optional containing the version, or empty if not persisted
    */
   default Optional<Integer> getVersionSafely() {
      return Optional.ofNullable(getVersion());
   }

   /**
    * Check if this is a pre-edition task (executes before the edition).
    *
    * @return true if sequence is negative (pre-edition task)
    */
   default boolean isPreEditionTask() {
      return getSequence() < 0;
   }

   /**
    * Check if this is a post-edition task (executes after the edition).
    *
    * @return true if sequence is positive or zero (post-edition task)
    */
   default boolean isPostEditionTask() {
      return getSequence() >= 0;
   }

   /**
    * Check if this task is fully configured and ready to execute.
    *
    * @return true if all required fields are properly set
    */
   default boolean isValid() {
      return getTaskId() != null &&
             getEditionId() != null &&
             getExtensionNameSafely().isPresent();
   }

   /**
    * Compare this task with another for execution ordering by sequence.
    *
    * @param other the other task to compare to, never {@code null}
    * @return comparison result based on sequence number
    * @throws IllegalArgumentException if other is null
    */
   default int compareBySequence(IPSEditionTaskDef other) {
      Objects.requireNonNull(other, "Other task cannot be null");
      return Integer.compare(getSequence(), other.getSequence());
   }
}
