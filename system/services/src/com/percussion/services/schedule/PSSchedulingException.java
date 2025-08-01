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

import com.percussion.utils.exceptions.PSBaseException;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Exception thrown on scheduler service errors with modern Java 11 patterns.
 * Provides enhanced error context, factory methods for common scenarios, and
 * Optional-based safe access to error details.
 *
 * @author Andriy Palamarchuk
 */
public class PSSchedulingException extends PSBaseException {

   /**
    * Optional context information about the failed operation.
    */
   private final Optional<String> operationContext;

   /**
    * Optional schedule ID that caused the exception.
    */
   private final Optional<IPSGuid> scheduleId;

   /**
    * Creates a new exception with message code and arguments.
    *
    * @param msgCode the message code
    * @param args error message arguments, may be null
    */
   public PSSchedulingException(int msgCode, Object... args) {
      super(msgCode, args);
      this.operationContext = Optional.empty();
      this.scheduleId = Optional.empty();
   }

   /**
    * Creates a new exception with message code only.
    *
    * @param msgCode the message code
    */
   public PSSchedulingException(int msgCode) {
      super(msgCode);
      this.operationContext = Optional.empty();
      this.scheduleId = Optional.empty();
   }

   /**
    * Creates a new exception with message code, cause, and arguments.
    *
    * @param msgCode the message code
    * @param cause the underlying cause, may be null
    * @param args error message arguments, may be null
    */
   public PSSchedulingException(int msgCode, Throwable cause, Object... args) {
      super(msgCode, cause, args);
      this.operationContext = Optional.empty();
      this.scheduleId = Optional.empty();
   }

   /**
    * Creates a new exception with enhanced context information.
    *
    * @param msgCode the message code
    * @param operationContext optional context about the failed operation
    * @param scheduleId optional schedule ID that caused the exception
    * @param args error message arguments, may be null
    */
   public PSSchedulingException(int msgCode, Optional<String> operationContext,
         Optional<IPSGuid> scheduleId, Object... args) {
      super(msgCode, args);
      this.operationContext = operationContext != null ? operationContext : Optional.empty();
      this.scheduleId = scheduleId != null ? scheduleId : Optional.empty();
   }

   /**
    * Creates a new exception with enhanced context information and cause.
    *
    * @param msgCode the message code
    * @param cause the underlying cause, may be null
    * @param operationContext optional context about the failed operation
    * @param scheduleId optional schedule ID that caused the exception
    * @param args error message arguments, may be null
    */
   public PSSchedulingException(int msgCode, Throwable cause, Optional<String> operationContext,
         Optional<IPSGuid> scheduleId, Object... args) {
      super(msgCode, cause, args);
      this.operationContext = operationContext != null ? operationContext : Optional.empty();
      this.scheduleId = scheduleId != null ? scheduleId : Optional.empty();
   }

   /**
    * Creates a new exception from a simple message for convenience.
    *
    * @param message the error message, never null
    */
   public PSSchedulingException(String message) {
      super(message);
      this.operationContext = Optional.empty();
      this.scheduleId = Optional.empty();
   }

   /**
    * Creates a new exception from a message and cause for convenience.
    *
    * @param message the error message, never null
    * @param cause the underlying cause, may be null
    */
   public PSSchedulingException(String message, Throwable cause) {
      super(message, cause);
      this.operationContext = Optional.empty();
      this.scheduleId = Optional.empty();
   }

   /**
    * Get the operation context that caused this exception.
    *
    * @return Optional containing the operation context, empty if not available
    */
   public Optional<String> getOperationContext() {
      return operationContext;
   }

   /**
    * Get the schedule ID that caused this exception.
    *
    * @return Optional containing the schedule ID, empty if not available
    */
   public Optional<IPSGuid> getScheduleId() {
      return scheduleId;
   }

   @Override
   protected String getResourceBundleBaseName() {
      return "com.percussion.services.schedule.PSSchedulingErrorStringBundle";
   }

   /**
    * Enhanced error message codes with modern enum patterns.
    * The error messages are stored in the resource bundle indicated by
    * {@link PSSchedulingException#getResourceBundleBaseName()}.
    */
   public enum Error {
      /**
       * A scheduler error.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>The original exception message.</TD></TR>
       * </TABLE>
       */
      SCHEDULER(1, "General scheduler error"),

      /**
       * A cron expression parsing error.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>The cron expression.</TD></TR>
       * <TR><TD>1</TD><TD>The original exception message.</TD></TR>
       * </TABLE>
       */
      CRON_FORMAT(2, "Invalid cron expression format"),

      /**
       * Found a job object, but this object does not contain a job schedule.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>Schedule id.</TD></TR>
       * </TABLE>
       */
      JOB_WITHOUT_SCHEDULE(3, "Job found without associated schedule"),

      /**
       * Failed to run an Edition due to an exception.
       */
      FAILED_RUN_EDITION(4, "Failed to run edition"),

      /**
       * Failed to run a specified Edition due to an exception.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>Edition ID</TD></TR>
       * <TR><TD>1</TD><TD>Edition name</TD></TR>
       * <TR><TD>2</TD><TD>Underlying error message</TD></TR>
       * </TABLE>
       */
      FAILED_RUN_SPECIFIED_EDITION(5, "Failed to run specified edition"),

      /**
       * Failed to run a specified command.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>Command</TD></TR>
       * </TABLE>
       */
      FAILED_RUN_COMMAND(6, "Failed to run command"),

      /**
       * Failed to run a specified command with standard error text.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>Command</TD></TR>
       * <TR><TD>1</TD><TD>Standard error text</TD></TR>
       * </TABLE>
       */
      FAILED_RUN_COMMAND_WITH_STDERROR(7, "Failed to run command with stderr"),

      /**
       * Edition canceled by user.
       * <p>
       * <TABLE BORDER="1">
       * <TR><TH>Argument</TH><TH>Description</TH></TR>
       * <TR><TD>0</TD><TD>Edition ID</TD></TR>
       * <TR><TD>1</TD><TD>Edition name</TD></TR>
       * </TABLE>
       */
      EDITION_CANCELED_BY_USER(8, "Edition canceled by user");

      private final int code;
      private final String description;

      Error(int code, String description) {
         this.code = code;
         this.description = description;
      }

      /**
       * Get the error code for this error.
       *
       * @return the error code
       */
      public int getCode() {
         return code;
      }

      /**
       * Get a human-readable description of this error.
       *
       * @return the description, never null
       */
      public String getDescription() {
         return description;
      }

      /**
       * Check if this is a critical error that requires immediate attention.
       *
       * @return true if this is a critical error
       */
      public boolean isCritical() {
         return this == SCHEDULER || this == FAILED_RUN_EDITION;
      }

      /**
       * Check if this is a validation error.
       *
       * @return true if this is a validation error
       */
      public boolean isValidation() {
         return this == CRON_FORMAT || this == JOB_WITHOUT_SCHEDULE;
      }
   }

   // Factory methods for common scheduling exception scenarios

   /**
    * Create an exception for scheduler service errors.
    *
    * @param cause the underlying cause, never null
    * @param message additional error details, never null
    * @return PSSchedulingException with appropriate error context
    */
   public static PSSchedulingException schedulerError(Throwable cause, String message) {
      Objects.requireNonNull(cause, "Cause cannot be null");
      Objects.requireNonNull(message, "Message cannot be null");
      return new PSSchedulingException(
         Error.SCHEDULER.getCode(),
         cause,
         Optional.of("Scheduler operation"),
         Optional.empty(),
         message
      );
   }

   /**
    * Create an exception for cron expression parsing errors.
    *
    * @param cronExpression the invalid cron expression, never null
    * @param cause the parsing error cause, never null
    * @return PSSchedulingException with appropriate error context
    */
   public static PSSchedulingException cronFormatError(String cronExpression, Throwable cause) {
      Objects.requireNonNull(cronExpression, "Cron expression cannot be null");
      Objects.requireNonNull(cause, "Cause cannot be null");
      return new PSSchedulingException(
         Error.CRON_FORMAT.getCode(),
         cause,
         Optional.of("Cron expression parsing"),
         Optional.empty(),
         cronExpression, cause.getMessage()
      );
   }

   /**
    * Create an exception for missing schedule in job.
    *
    * @param scheduleId the schedule ID without associated job, never null
    * @return PSSchedulingException with appropriate error context
    */
   public static PSSchedulingException jobWithoutSchedule(IPSGuid scheduleId) {
      Objects.requireNonNull(scheduleId, "Schedule ID cannot be null");
      return new PSSchedulingException(
         Error.JOB_WITHOUT_SCHEDULE.getCode(),
         Optional.of("Job validation"),
         Optional.of(scheduleId),
         scheduleId.toString()
      );
   }

   /**
    * Create an exception for edition execution failures.
    *
    * @param editionId the edition ID that failed, never null
    * @param editionName the edition name that failed, never null
    * @param cause the execution error cause, never null
    * @return PSSchedulingException with appropriate error context
    */
   public static PSSchedulingException editionExecutionFailed(IPSGuid editionId, String editionName, Throwable cause) {
      Objects.requireNonNull(editionId, "Edition ID cannot be null");
      Objects.requireNonNull(editionName, "Edition name cannot be null");
      Objects.requireNonNull(cause, "Cause cannot be null");
      return new PSSchedulingException(
         Error.FAILED_RUN_SPECIFIED_EDITION.getCode(),
         cause,
         Optional.of("Edition execution"),
         Optional.of(editionId),
         editionId.toString(), editionName, cause.getMessage()
      );
   }

   /**
    * Create an exception for command execution failures.
    *
    * @param command the command that failed, never null
    * @param stderr the standard error output, may be null
    * @return PSSchedulingException with appropriate error context
    */
   public static PSSchedulingException commandExecutionFailed(String command, String stderr) {
      Objects.requireNonNull(command, "Command cannot be null");
      var errorCode = stderr != null ? Error.FAILED_RUN_COMMAND_WITH_STDERROR : Error.FAILED_RUN_COMMAND;
      var args = stderr != null ? new Object[]{command, stderr} : new Object[]{command};
      return new PSSchedulingException(
         errorCode.getCode(),
         Optional.of("Command execution"),
         Optional.empty(),
         args
      );
   }

   @Override
   public String toString() {
      var sb = new StringBuilder(super.toString());

      operationContext.ifPresent(context ->
         sb.append(" [Operation: ").append(context).append("]"));

      scheduleId.ifPresent(id ->
         sb.append(" [Schedule: ").append(id).append("]"));

      return sb.toString();
   }
}
