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
package com.percussion.services.schedule.impl;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.process.PSProcessAction;
import com.percussion.services.schedule.IPSTask;
import com.percussion.services.schedule.IPSTaskResult;
import com.percussion.services.schedule.PSSchedulingException;
import com.percussion.services.schedule.PSSchedulingException.Error;
import com.percussion.services.schedule.data.PSTaskResult;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is used to execute a native command. The command and its associated
 * arguments are expected to be specified by the <code>command</code> parameter
 * which is passed to {@link #perform(Map)}.
 *
 * @author Yu-Bing Chen
 */
public class PSRunCommand implements IPSTask
{
   /**
    * logger for this class.
    */
   private static final Logger ms_log = LogManager.getLogger(PSRunCommand.class);
   
   /**
    * Invokes the native command. The returned task result will contain the
    * following variables, which can be used in notification template: <TABLE
    * BORDER="1">
    * <TR>
    * <TH>Variable Name</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>$command</TD>
    * <TD>The command parameter.</TD>
    * </TR>
    * <TR>
    * <TD>$sys.executionDatetime</TD>
    * <TD>The starting date and time of the execution.</TD>
    * </TR>
    * <TR>
    * <TD>$sys.executionElapsedTime</TD>
    * <TD>The duration of the execution.</TD>
    * </TR>
    * </TABLE>
    * <p>
    * The following context variables will be added by the framework: <TABLE
    * BORDER="1">
    * <TR>
    * <TH>Variable Name</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>$sys.taskName</TD>
    * <TD>The name of the scheduled task.</TD>
    * </TR>
    * <TR>
    * <TD>$sys.completed</TD>
    * <TD>It is true if the command was completed; otherwise false.</TD>
    * </TR>
    * <TR>
    * <TD>$sys.problemDesc</TD>
    * <TD>The problem description in case of execution failure.</TD>
    * </TR>
    * <TR>
    * <TD>$tools.*</TD>
    * <TD>The problem description in case of execution failure.</TD>
    * </TR>
    * </TABLE>
    * 
    * @param parameters It must contains a <code>command</code> parameter with
    * the native command and its associated arguments, never <code>null</code>
    * or empty.
    * 
    * @return the {@link IPSTaskResult#getNotificationVariables()} contains all
    * input parameters, never <code>null</code>.
    */
   public IPSTaskResult perform(Map<String,String> parameters)
   {
      String command = parameters.get("command");
      if (StringUtils.isBlank(command))
      {
         throw new IllegalArgumentException("No command defined");
      }
      String cmd[] = command.split(" ");
      IPSTaskResult result;
      boolean isComplete = true;
      String errorCause = null;
      long startTime = System.currentTimeMillis();
      long endTime;
      
      try
      {
         PSProcessAction action = new PSProcessAction(cmd, null, null);
         if (action.waitFor() != 0)
            throw getException(command, action.getStdErrorText(), action.getStdOutText());

         endTime = System.currentTimeMillis();
      }
      catch (Exception e)
      {
         isComplete = false;
         endTime = System.currentTimeMillis();
         ms_log.error("Failed to execute command: \"" + command + "\"", e);
         errorCause = e.getLocalizedMessage();
      }
      
      result = new PSTaskResult(isComplete, errorCause, 
            PSScheduleUtils.getContextVars(parameters, startTime, endTime));
      
      return result;
   }

   /**
    * Get the exception for the specified command.
    * 
    * @param command the failure command, may be <code>null</code> or empty.
    * @param stdErrorText the standard error text from the failure, may be 
    *    <code>null</code> or empty.
    * @param stdOutText the standard out text from the failure, may be blank.
    */
   private PSSchedulingException getException(String command,
         String stdErrorText, String stdOutText)
   {
      if (isNotBlank(stdOutText))
      {
         ms_log.error("Command \"" + command + "\" failed, standard out: "
               + stdOutText);
      }
      if (isNotBlank(stdErrorText))
      {
         ms_log.error("Command \"" + command + "\" failed, standard error: "
               + stdErrorText);
      }
      
      PSSchedulingException se;
      if (isBlank(stdErrorText))
      {
         if (isNotBlank(stdOutText))
         {
            se = new PSSchedulingException(
                  Error.FAILED_RUN_COMMAND_WITH_STDERROR.ordinal(), command,
                  stdOutText);
         }
         else
         {
            se = new PSSchedulingException(Error.FAILED_RUN_COMMAND.ordinal(),
                  command);
         }
      }
      else
      {
         se = new PSSchedulingException(Error.FAILED_RUN_COMMAND_WITH_STDERROR
               .ordinal(), command, stdErrorText);
      }
      return se;
   }

   /*
    * //see base class method for details
    */
   @SuppressWarnings("unused")
   public void init(@SuppressWarnings("unused") IPSExtensionDef def, 
         @SuppressWarnings("unused") File codeRoot) throws PSExtensionException
   {
      // No initialization
   }
}
