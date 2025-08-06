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
// REFACTORED: CP-JAVA11
package com.percussion.services.schedule.impl;

import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.error.PSNotFoundException;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionManager;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.security.PSRoleManager;
import com.percussion.security.PSSecurityProvider;
import com.percussion.server.PSServer;
import com.percussion.services.schedule.IPSSchedulingService;
import com.percussion.services.schedule.IPSTask;
import com.percussion.services.schedule.IPSTaskResult;
import com.percussion.services.schedule.PSSchedulingException;
import com.percussion.services.schedule.PSSchedulingException.Error;
import com.percussion.services.schedule.PSSchedulingServiceLocator;
import com.percussion.services.schedule.data.PSNotificationTemplate;
import com.percussion.services.schedule.data.PSNotifyWhen;
import com.percussion.services.schedule.data.PSScheduledTask;
import com.percussion.services.schedule.data.PSScheduledTaskLog;
import com.percussion.services.schedule.data.PSTaskResult;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.utils.jexl.PSServiceJexlEvaluatorBase;
import com.percussion.services.utils.jexl.PSVelocityUtils;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.workflow.PSWorkFlowUtils;
import com.percussion.workflow.mail.PSMailMessageContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.RuntimeServices;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts Rhythmyx scheduler tasks {@link IPSTask} to the Quartz job interface,
 * so Quartz can run them with Java 11 enhancements.
 * <p>
 * Note: The same job can be triggered by Quartz, but this class will prevent
 * the same task from being executed concurrently with thread-safe operations.
 */
public class PSTaskAdapter implements Job {

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(PSTaskAdapter.class);

    @Override
    public void execute(JobExecutionContext context) {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        var job = PSScheduleUtils.getStoredSchedule(context.getJobDetail());

        if (!PSServer.isInitialized()) {
            log.warn("Server is not initialized - skipping job execution: {}", job);
            return;
        }

        runJob(job, context.getScheduler(), false);
    }

    /**
     * Run a supplied job with enhanced error handling and Optional-based safety.
     *
     * @param job the job to be executed, cannot be null
     * @param scheduler the Quartz scheduler, cannot be null
     * @param isRunNow true if the task is manually invoked; false for scheduled execution
     */
    public static void runJob(PSScheduledTask job, Scheduler scheduler, boolean isRunNow) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(scheduler, "scheduler cannot be null");

        var result = isRegisteredServer(job)
            ? executeTask(job, isRunNow)
            : handleUnregisteredServer(job, isRunNow);

        if (result != null) {
            notifyTaskResult(job, result);
        }
    }

    /**
     * Handles execution on unregistered servers with enhanced error reporting.
     */
    private static IPSTaskResult handleUnregisteredServer(PSScheduledTask job, boolean isRunNow) {
        if (!isRunNow) {
            return null; // Skip log/notification for scheduled invocation
        }

        var startTime = System.currentTimeMillis();
        var httpPort = Optional.of(PSServer.getListenerPort())
                .filter(port -> port != -1)
                .map(String::valueOf)
                .orElse("");
        var httpsPort = Optional.of(PSServer.getSslListenerPort())
                .filter(port -> port != 0)
                .map(String::valueOf)
                .orElse("");

        var schedulingException = new PSSchedulingException(
                Error.TASK_NOT_REGISTERED_SERVER.ordinal(),
                job.getId().toString(),
                job.getName(),
                job.getServer(),
                PSServer.getHostName(),
                PSServer.getFullyQualifiedHostName(),
                PSServer.getHostAddress(),
                httpPort,
                httpsPort);

        var skipMessage = schedulingException.getLocalizedMessage();
        var result = getErrorResult(job, skipMessage, startTime);
        logTaskExecution(job, result, startTime, getCurServer());
        return result;
    }

    /**
     * Send notification for a finished job if the event is defined to do so.
     *
     * @param job the finished job, never null
     * @param result the result of the finished job, never null
     */
    private static void notifyTaskResult(PSScheduledTask job, IPSTaskResult result) {
        if (!needToNotify(job, result)) {
            return;
        }

        try {
            var evaluator = getEvaluator(job, result);
            var template = getScheduleService()
                    .findNotificationTemplateById(job.getNotificationTemplateId());

            var subject = getEvaluateSubject(template.getSubject(), evaluator);
            var message = getNotifyMessage(template, evaluator.getVars());

            sendNotification(job, subject, message);
        } catch (Exception e) {
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            log.error("Failed notification for task: {} - {}",
                    job.getName(), PSExceptionUtils.getMessageForLog(e));
        }
    }

    /**
     * Get the JEXL evaluator for the supplied job and its result with enhanced variable binding.
     *
     * @param job the job, never null
     * @param result the result of the job run, never null
     * @return the evaluator, never null
     */
    private static PSJexlEvaluator getEvaluator(PSScheduledTask job, IPSTaskResult result) {
        var evaluator = new PSJexlEvaluator();

        // Bind notification variables using forEach
        result.getNotificationVariables().forEach(evaluator::bind);

        evaluator.bind("$sys.taskName", job.getName());

        Optional.ofNullable(getToolsMap())
                .ifPresent(tools -> evaluator.bind("$tools", tools));

        return evaluator;
    }

   /**
    * Sends a notification for the specified job and message with enhanced null safety.
    *
    * @param job the finished job, never null
    * @param subject the subject of the notification, never null or empty
    * @param message the message body, never null
    */
   private static void sendNotification(PSScheduledTask job, String subject, String message) {
      var recipientAddress = getNotifyTo(job);

      var mailMessage = new PSMailMessageContext(
              PSSecurityProvider.INTERNAL_USER_NAME,
              recipientAddress,
              null,
              subject,
              message,
              null,
              getMailDomain(),
              getSmtpHost(),
              getSmtpUsername(),
              getSmtpPassword(),
              getSmtpIsTLSEnabled(),
              getSmtpPort(),
              getSmtpSSLPort(),
              getSmtpBounceAddr());

      var systemService = PSSystemServiceLocator.getSystemService();
      systemService.sendEmail(mailMessage);
   }

   /**
    * Cache for mail domain configuration.
    */
   private static volatile String ms_mailDomain;

   /**
    * Cache for SMTP host configuration.
    */
   private static volatile String ms_smtpHost;

   /**
    * Cache for SMTP username configuration.
    */
   private static volatile String ms_smtpUsername;

   /**
    * Cache for SMTP password configuration.
    */
   private static volatile String ms_smtpPassword;

   /**
    * Cache for SMTP port configuration.
    */
   private static volatile String ms_smtpPort;

   /**
    * Cache for SMTP TLS enabled configuration.
    */
   private static volatile String ms_smtpIsTLSEnabled;

   /**
    * Cache for SMTP SSL port configuration.
    */
   private static volatile String ms_smtpSSLPort;

   /**
    * Cache for SMTP bounce address configuration.
    */
   private static volatile String ms_smtpBounceAddr;

   /**
    * Get the mail domain that is defined in workflow properties file with caching.
    *
    * @return the mail domain, may be null or empty if not defined
    */
   private static String getMailDomain() {
      if (ms_mailDomain == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_mailDomain == null) {
               ms_mailDomain = Optional.ofNullable(PSWorkFlowUtils.getProperty("MAIL_DOMAIN"))
                       .orElse("");
            }
         }
      }
      return ms_mailDomain;
   }

   /**
    * Get the SMTP Host from the workflow properties file with enhanced validation.
    *
    * @return the SMTP Host property, never null or empty
    * @throws IllegalStateException if the property is not defined
    */
   private static String getSmtpHost() {
      if (ms_smtpHost == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpHost == null) {
               ms_smtpHost = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_HOST"))
                       .filter(StringUtils::isNotBlank)
                       .orElseThrow(() -> new IllegalStateException(
                               "SMTP_HOST does not exist in rxworkflow.properties"));
            }
         }
      }
      return ms_smtpHost;
   }

   /**
    * Get the SMTP username with Optional-based safety.
    *
    * @return the username, may be null or empty if not defined
    */
   private static String getSmtpUsername() {
      if (ms_smtpUsername == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpUsername == null) {
               ms_smtpUsername = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_USERNAME"))
                       .orElse("");
            }
         }
      }
      return ms_smtpUsername;
   }

   /**
    * Get the SMTP bounce address with Optional-based safety.
    *
    * @return the bounce address, may be null or empty if not defined
    */
   private static String getSmtpBounceAddr() {
      if (ms_smtpBounceAddr == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpBounceAddr == null) {
               ms_smtpBounceAddr = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_BOUNCEADDR"))
                       .orElse("");
            }
         }
      }
      return ms_smtpBounceAddr;
   }

   /**
    * Get the SMTP password with Optional-based safety.
    *
    * @return the password, may be null or empty if not defined
    */
   private static String getSmtpPassword() {
      if (ms_smtpPassword == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpPassword == null) {
               ms_smtpPassword = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_PASSWORD"))
                       .orElse("");
            }
         }
      }
      return ms_smtpPassword;
   }

   /**
    * Get the SMTP SSL port with Optional-based safety.
    *
    * @return the SSL port, may be null or empty if not defined
    */
   private static String getSmtpSSLPort() {
      if (ms_smtpSSLPort == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpSSLPort == null) {
               ms_smtpSSLPort = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_SSLPORT"))
                       .orElse("");
            }
         }
      }
      return ms_smtpSSLPort;
   }

   /**
    * Get the SMTP port with Optional-based safety.
    *
    * @return the port, may be null or empty if not defined
    */
   private static String getSmtpPort() {
      if (ms_smtpPort == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpPort == null) {
               ms_smtpPort = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_PORT"))
                       .orElse("");
            }
         }
      }
      return ms_smtpPort;
   }

   /**
    * Determine whether TLS is enabled with Optional-based safety.
    *
    * @return "true" if enabled, may be null or empty if not defined
    */
   private static String getSmtpIsTLSEnabled() {
      if (ms_smtpIsTLSEnabled == null) {
         synchronized (PSTaskAdapter.class) {
            if (ms_smtpIsTLSEnabled == null) {
               ms_smtpIsTLSEnabled = Optional.ofNullable(PSWorkFlowUtils.getProperty("SMTP_TLSENABLED"))
                       .orElse("");
            }
         }
      }
      return ms_smtpIsTLSEnabled;
   }

   /**
    * Get the notification target/to addresses with Stream API processing.
    *
    * @param job the finished job, never null
    * @return the target addresses, never null or empty
    * @throws IllegalStateException if no notification targets are configured
    */
   private static String getNotifyTo(PSScheduledTask job) {
      var roleEmailAddresses = getEmailAddressesFromNotifyRole(job);
      var directEmailAddresses = getEmailAddresses(job);

      var combinedAddresses = Stream.of(roleEmailAddresses, directEmailAddresses)
              .filter(StringUtils::isNotBlank)
              .collect(Collectors.joining(","));

      if (StringUtils.isBlank(combinedAddresses)) {
         throw new IllegalStateException(
                 "Job does not contain a notification target/to address: " + job.getName());
      }

      return combinedAddresses;
   }

   /**
    * Get and normalize email addresses from the task with Stream API processing.
    *
    * @param job the task in question, never null
    * @return the normalized email addresses, may be null if not defined
    */
   private static String getEmailAddresses(PSScheduledTask job) {
      return Optional.ofNullable(job.getEmailAddresses())
              .filter(StringUtils::isNotBlank)
              .map(addresses -> {
                 return Stream.of(addresses.split(","))
                         .map(String::trim)
                         .filter(StringUtils::isNotBlank)
                         .map(PSTaskAdapter::normalizeEmailAddress)
                         .collect(Collectors.joining(","));
              })
              .orElse(null);
   }

   /**
    * Normalize an email address with enhanced domain handling.
    *
    * @param email the email address in question, never null
    * @return the normalized email address
    */
   private static String normalizeEmailAddress(String email) {
      if (email.contains("@")) {
         return email;
      }

      return Optional.ofNullable(getMailDomain())
              .filter(StringUtils::isNotBlank)
              .map(domain -> {
                 return domain.startsWith("@")
                         ? email + domain
                         : email + "@" + domain;
              })
              .orElse(email);
   }
   
   /**
    * Get the email addresses from the notified role of the job.
    * @param job the task in question, assumed not <code>null</code>.
    * @return the email addresses. It may be <code>null</code> or empty if
    *    the notified role is not specified or there is no sys_email property
    *    specified in any of the members.
    */
   @SuppressWarnings("unchecked")
   private static String getEmailAddressesFromNotifyRole(PSScheduledTask job)
   {
      String roleName = job.getNotify();
      if (StringUtils.isBlank(roleName))
         return null;
      
      StringBuilder emails = new StringBuilder();
      boolean isFirst = true;
      PSRoleManager rmgr = PSRoleManager.getInstance();
      Set<PSSubject> users = rmgr.getSubjects(roleName, null);
      for (PSSubject user : users)
      {
         PSAttributeList atts = user.getAttributes();
         PSAttribute attr = atts.getAttribute("sys_email");
         if (attr != null)
         {
            String email = attr.getValues().get(0).toString();
            if (!isFirst)
               emails.append(",");
            isFirst = false;
            emails.append(normalizeEmailAddress(email));
         }
      }
      
      return emails.toString();
   }

   /**
    * Evaluate the supplied subject (in JEXL expression).
    * 
    * @param subject the subject in JEXL expression, 
    *    assumed not <code>null</code>.
    * @param eval the evaluator, assumed not <code>null</code>.
    * 
    * @return the evaluated subject, never <code>null</code>, may be empty.
    */
   private static String getEvaluateSubject(String subject,
         PSJexlEvaluator eval)
   {
      try
      {
         Object v = eval.evaluate(eval.createScript(subject));
         return v.toString();
      }
      catch (Exception e)
      {
         log.error("Failed to evaluate subject: " + subject, e);
         return "";
      }
   }

   /**
    * Get the JEXL utilities / tools, which is loaded rom the tools.xml.
    * @return the map of the tools. It is <code>null</code> if failed to
    *    load the tools.
    */
   private static Map<String, Object> getToolsMap()
   {
      if (ms_toolsMap == null)
      {
         PSServiceJexlEvaluatorBase jexlBase = new PSServiceJexlEvaluatorBase(
               false);
         try
         {
            ms_toolsMap = jexlBase.getVelocityToolBindings();
         }
         catch (Exception e)
         {
            ms_toolsMap = null;
            log.error("Failed to load Velocity Tools", e);
         }
      }
      return ms_toolsMap;
   }
   
   /**
    * The Velocity Tools, initialized by 
    * {@link #getToolsMap()}, never <code>null</code> after that.
    */
   private volatile static Map<String,Object> ms_toolsMap = null;
   
   /**
    * Get the schedule service
    * @return the schedule service, never <code>null</code>.
    */
   private static IPSSchedulingService getScheduleService()
   {
      return PSSchedulingServiceLocator.getSchedulingService();
   }

   /**
    * Render the supplied job result with the specified notification template.
    * 
    * @param nt the notification template, assumed not
    *    <code>null</code>.
    * @param vars the job result, assumed not <code>null</code>.
    * 
    * @return the rendered text, never <code>null</code>, may be empty.
    */
   @SuppressWarnings("unchecked")
   private static String getNotifyMessage(PSNotificationTemplate nt, Map vars)
   {
      VelocityContext ctx = PSVelocityUtils.getContext(vars);
      
      try
      {
         Template t = PSVelocityUtils.compileTemplate(nt.getTemplate(),
               "EventNotification", getVelocityRS());         
         StringWriter writer = new StringWriter();
         t.merge(ctx, writer);
         writer.close();
         String message = writer.toString();
         return message;
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         log.error("Failed to format Notification Template id= {} : {}", nt.getId(),PSExceptionUtils.getMessageForLog(e));
         return null;
      }
   }
   
   /**
    * @return the Velocity Runtime object, never <code>null</code>.
    * 
    * @throws Exception if cannot get the velocity runtime.
    */
   private static RuntimeServices getVelocityRS() throws Exception
   {
      RuntimeServices rs = new RuntimeInstance();
      rs.init();
      
      return rs;
   }
   
   /**
    * Determines if the suppled job need to be notified.
    * @param job the job in question, assumed not <code>null</code>.
    * @param result the job result, assumed not <code>null</code>.
    * @return <code>true</code> if need to be notified.
    */
   private static boolean needToNotify(PSScheduledTask job, IPSTaskResult result)
   {
      if(PSWorkFlowUtils.getProperty(PSWorkFlowUtils.NOTIFICATION_ENABLE).equalsIgnoreCase("N"))
         return false; //Notifications aren't configured so skip it.

      PSNotifyWhen when = job.getNotifyWhen();
      return (when.ordinal() == PSNotifyWhen.ALWAYS.ordinal()
            || (when.ordinal() == PSNotifyWhen.FAILURE.ordinal() 
                  && (!result.wasSuccess())));
   }
   
   /**
    * Determines if the given task needs to be fired or executed.
    *
    * @param task the task to be executed, never null
    * @throws PSSchedulingException if should skip firing the task
    */
   private static void validateExecution(PSScheduledTask task) throws PSSchedulingException {
      if (isJobActive(task.getId())) {
         throw new PSSchedulingException(
               Error.TASK_ALREADY_RUNNING.ordinal(),
               task.getId().toString(),
               task.getName());
      }
   }

   /**
    * Load the task using the extensions manager with enhanced error handling.
    *
    * @param name the name of the task, never null or empty
    * @return the extension instance, never null
    * @throws PSNotFoundException if cannot find the extension
    * @throws PSExtensionException error on preparing the extension
    */
   @SuppressWarnings("unchecked")
   private static IPSTask getTask(String name) throws PSNotFoundException, PSExtensionException {
      if (StringUtils.isBlank(name)) {
         throw new IllegalArgumentException("Task name cannot be null or empty");
      }
      var extensionManager = PSServer.getExtensionManager(null);
      var extensionRef = new PSExtensionRef(name);
      return (IPSTask) extensionManager.prepareExtension(extensionRef, null);
   }

   /**
    * Execute the given task or job.
    *
    * @param curJob the executed job, assumed not <code>null</code>.
    * @param isRunNow <code>true</code> if the task is manually invoked, in
    *    this case, the task will be fired without check if the same task is
    *    already running or not; otherwise, the execution of the task will be
    *    skip if the same task is already running.
    *
    * @return the result of the execution if the job is successfully executed;
    *    return <code>null</code> if failed to execute the job.
    */
   private static IPSTaskResult executeTask(PSScheduledTask curJob,
         boolean isRunNow)
   {
      IPSTaskResult result = null;
      long startTime = System.currentTimeMillis();

      try
      {
         if (! isRunNow)
            validateExecution(curJob);

         addJobId(curJob.getId());
         IPSTask task = getTask(curJob.getExtensionName());
         result = task.perform(curJob.getParameters());
         return result;
      }
      catch (Exception e)
      {
         log.error("Failed to execute job: " + curJob.toString(), e);
         result = getErrorResult(curJob, e, startTime);
         return result;
      }
      finally
      {
         logTaskExecution(curJob, result, startTime, getCurServer());
         removeJobId(curJob.getId());
      }
   }

   /**
    * Gets the current server instance, name / port pair in the format of
    * &lt;host>[:port]. The port is HTTP port if HTTP port is defined, or
    * HTTPS port if HTTP port is not defined, but the HTTPS port is defined.
    * The port may be empty if neither HTTP and HTTPS are not defined.
    *
    * @return the name and/or port pair, never <code>null</code> or empty.
    */
   private static String getCurServer()
   {
      String host = PSServer.getHostName();
      String port = "";
      if (PSServer.getListenerPort() != -1)
         port = String.valueOf(PSServer.getListenerPort());
      else if (PSServer.getSslListenerPort() != 0)
         port = String.valueOf(PSServer.getSslListenerPort());

      return (StringUtils.isBlank(port)) ? host : host + ":" + port;
   }

   /**
    * Gets the host name from a given server instance.
    * @param server the server instance in the format of &lt;host-name>[:port].
    *    Assumed not <code>null</code> or empty.
    * @return the host name part of the server instance.
    *    Never <code>null</code>, but may be empty.
    */
   private static String getServerName(String server)
   {
      String[] result = server.split(":");
      return result[0] == null ? "" : result[0];
   }

   /**
    * Gets the port from a given server instance.
    * @param server the server instance in the format of &lt;host-name>[:port].
    *    Assumed not <code>null</code> or empty.
    * @return the port part of the server instance.
    *    Never <code>null</code>, but may be empty.
    */
   private static String getServerPort(String server)
   {
      String[] result = server.split(":");
      return result.length > 1 ? result[1].trim() : "";
   }

   /**
    * Determines if the given task is registered for the current server.
    * @param curJob the task in question, assumed not <code>null</code>.
    * @return <code>true</code> if the task is registered for the current
    *    server.
    */
   private static boolean isRegisteredServer(PSScheduledTask curJob)
   {
      if (StringUtils.isBlank(curJob.getServer()))
         return true;

      String server = curJob.getServer().trim();
      String host = getServerName(server);
      String port = getServerPort(server);
      boolean isHostMatch = false;

      // compare the (optional) port of the server instance
      boolean isPortMatch = true;
      if (StringUtils.isNotBlank(port))
      {
         String httpPort = null;
         if (PSServer.getListenerPort() != -1)
            httpPort = String.valueOf(PSServer.getListenerPort());
         String httpsPort = null;
         if (PSServer.getSslListenerPort() != 0)
            httpsPort = String.valueOf(PSServer.getSslListenerPort());

         isPortMatch = (httpPort != null && httpPort.equals(port)) ||
               (httpsPort != null && httpsPort.equals(port));
      }

      // compare the host name
      try
      {
         isHostMatch = PSServer.getHostName().equalsIgnoreCase(host)
               || PSServer.getFullyQualifiedHostName().equalsIgnoreCase(host)
               || PSServer.getHostAddress().equalsIgnoreCase(host);
      }
      catch (Exception e)
      {
         log.error("Failed to identify server name or IP address.", e);
      }

      if (log.isDebugEnabled())
      {
         String hostName = PSServer.getHostName();
         if (isHostMatch && isPortMatch)
         {
            log.debug("Task of '" + curJob.getName()
                  + "' is registered for the server '" + hostName + "'.");
         }
         else
         {
            log.debug("Task of '" + curJob.getName()
                  + "' is not registered for the server '" + hostName + "'.");
         }
      }

      return isHostMatch && isPortMatch;
   }

   /**
    * Creates the task result for the failed task and the exception.
    * @param task the failed task, assumed not <code>null</code>.
    * @param e the cause of the failure, assumed not <code>null</code>.
    * @return the created task result, never <code>null</code>.
    */
   private static IPSTaskResult getErrorResult(PSScheduledTask task,
         Exception e, long startTime)
   {
      Throwable cause = e;
      if (e.getCause() != null)
         cause = e.getCause();
      String errorMsg = cause.getLocalizedMessage();
      if (StringUtils.isBlank(errorMsg))
         errorMsg = cause.toString();

      return getErrorResult(task, errorMsg, startTime);
   }

   /**
    * Creates a task result for the given failed or skipped task
    * @param task the failed task, assumed not <code>null</code>.
    * @param errorMsg the error message, assumed not <code>null</code> or empty.
    * @param startTime the start time of the task.
    * @return the created task result, never <code>null</code>.
    */
   private static IPSTaskResult getErrorResult(PSScheduledTask task,
         String errorMsg, long startTime)
   {
      return new PSTaskResult(false, errorMsg, PSScheduleUtils.getContextVars(
            task.getParameters(), startTime, System.currentTimeMillis()));
   }


   /**
    * Log the result of the task execution.
    *
    * @param curJob the current job, assumed not <code>null</code>.
    * @param result the result of the execution, it may be <code>null</code>
    *    if fail or skip to execute the task.
    * @param startTime the start time of the execution.
    * @param server the server invoked or skipped the task, assumed not
    *    <code>null</code>.
    */
   private static void logTaskExecution(PSScheduledTask curJob, IPSTaskResult result,
                                       long startTime, String server) {
        try {
            var endTime = System.currentTimeMillis();
            var logId = getScheduleService().createTaskLogId();
            var wasSuccess = result != null && result.wasSuccess();
            var resultMessage = result != null ? result.getProblemDescription() : "";

            var taskLog = new PSScheduledTaskLog(
                    logId,
                    curJob.getId(),
                    new Date(startTime),
                    new Date(endTime),
                    wasSuccess,
                    resultMessage,
                    server);

            getScheduleService().saveTaskLog(taskLog);
        } catch (Exception e) {
            log.error("Failed to log task execution: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    /**
     * Get the Velocity Runtime Services with proper initialization.
     *
     * @return the Velocity Runtime object, never null
     */
    private static RuntimeServices getVelocityRS() {
        try {
            var runtimeServices = new RuntimeInstance();
            runtimeServices.init();
            return runtimeServices;
        } catch (Exception e) {
            log.error("Failed to initialize Velocity Runtime Services", e);
            throw new RuntimeException("Velocity initialization failed", e);
        }
    }

    /**
     * Determines if the supplied job needs to be notified with enhanced enum handling.
     *
     * @param job the job in question, never null
     * @param result the job result, never null
     * @return true if notification is needed
     */
    private static boolean needToNotify(PSScheduledTask job, IPSTaskResult result) {
        var notificationEnabled = PSWorkFlowUtils.getProperty(PSWorkFlowUtils.NOTIFICATION_ENABLE);
        if ("N".equalsIgnoreCase(notificationEnabled)) {
            return false; // Notifications aren't configured so skip
        }

        var notifyWhen = job.getNotifyWhen();
        return notifyWhen == PSNotifyWhen.ALWAYS ||
               (notifyWhen == PSNotifyWhen.FAILURE && !result.wasSuccess());
    }

   /**
    * Determines if the given task needs to be fired or executed.
    *
    * @param task the to be executed task, assumed not <code>null</code>.
    *
    * @throws PSSchedulingException if should skip firing the task.
    */
   @SuppressWarnings({ "unchecked", "cast" })
   private static void validateExecution(PSScheduledTask task)
      throws PSSchedulingException
   {
      if (isJobActive(task.getId()))
      {
         PSSchedulingException se = new PSSchedulingException(
               Error.SKIP_FIRE_SCHEDULED_TASK.ordinal(), task.getId()
                     .toString(), task.getName());
         throw se;
      }
   }


   /**
    * Load the task using the extensions manager.
    *
    * @param name the name of the task, never <code>null</code> or empty.
    *
    * @return the extension instance, never <code>null</code>.
    *
    * @throws PSNotFoundException if cannot find the extension.
    * @throws PSExtensionException error on prepare the extension.
    */
   @SuppressWarnings("unchecked")
   private static IPSTask getTask(String name)
      throws PSNotFoundException, PSExtensionException
   {
      if (StringUtils.isBlank(name))
      {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      IPSExtensionManager emgr = PSServer.getExtensionManager(null);
      PSExtensionRef ref = new PSExtensionRef(name);
      return (IPSTask) emgr.prepareExtension(ref, null);
   }

   /**
    * Execute the given task or job.
    *
    * @param curJob the executed job, assumed not <code>null</code>.
    * @param isRunNow <code>true</code> if the task is manually invoked, in
    *    this case, the task will be fired without check if the same task is
    *    already running or not; otherwise, the execution of the task will be
    *    skip if the same task is already running.
    *
    * @return the result of the execution if the job is successfully executed;
    *    return <code>null</code> if failed to execute the job.
    */
   private static IPSTaskResult executeTask(PSScheduledTask curJob,
         boolean isRunNow)
   {
      IPSTaskResult result = null;
      long startTime = System.currentTimeMillis();

      try
      {
         if (! isRunNow)
            validateExecution(curJob);

         addJobId(curJob.getId());
         IPSTask task = getTask(curJob.getExtensionName());
         result = task.perform(curJob.getParameters());
         return result;
      }
      catch (Exception e)
      {
         log.error("Failed to execute job: " + curJob.toString(), e);
         result = getErrorResult(curJob, e, startTime);
         return result;
      }
      finally
      {
         logTaskExecution(curJob, result, startTime, getCurServer());
         removeJobId(curJob.getId());
      }
   }

   /**
    * Gets the current server instance, name / port pair in the format of
    * &lt;host>[:port]. The port is HTTP port if HTTP port is defined, or
    * HTTPS port if HTTP port is not defined, but the HTTPS port is defined.
    * The port may be empty if neither HTTP and HTTPS are not defined.
    *
    * @return the name and/or port pair, never <code>null</code> or empty.
    */
   private static String getCurServer()
   {
      String host = PSServer.getHostName();
      String port = "";
      if (PSServer.getListenerPort() != -1)
         port = String.valueOf(PSServer.getListenerPort());
      else if (PSServer.getSslListenerPort() != 0)
         port = String.valueOf(PSServer.getSslListenerPort());

      return (StringUtils.isBlank(port)) ? host : host + ":" + port;
   }
   
   /**
    * Gets the host name from a given server instance.
    * @param server the server instance in the format of &lt;host-name>[:port].
    *    Assumed not <code>null</code> or empty.
    * @return the host name part of the server instance. 
    *    Never <code>null</code>, but may be empty. 
    */
   private static String getServerName(String server)
   {
      String[] result = server.split(":");
      return result[0] == null ? "" : result[0];
   }

   /**
    * Gets the port from a given server instance.
    * @param server the server instance in the format of &lt;host-name>[:port].
    *    Assumed not <code>null</code> or empty.
    * @return the port part of the server instance. 
    *    Never <code>null</code>, but may be empty. 
    */
   private static String getServerPort(String server)
   {
      String[] result = server.split(":");
      return result.length > 1 ? result[1].trim() : "";
   }

   /**
    * Determines if the given task is registered for the current server.
    * @param curJob the task in question, assumed not <code>null</code>.
    * @return <code>true</code> if the task is registered for the current 
    *    server.
    */
   private static boolean isRegisteredServer(PSScheduledTask curJob)
   {
      if (StringUtils.isBlank(curJob.getServer()))
         return true;
      
      String server = curJob.getServer().trim();
      String host = getServerName(server);
      String port = getServerPort(server);
      boolean isHostMatch = false;
      
      // compare the (optional) port of the server instance 
      boolean isPortMatch = true;
      if (StringUtils.isNotBlank(port))
      {
         String httpPort = null;
         if (PSServer.getListenerPort() != -1)
            httpPort = String.valueOf(PSServer.getListenerPort());
         String httpsPort = null;
         if (PSServer.getSslListenerPort() != 0)
            httpsPort = String.valueOf(PSServer.getSslListenerPort());
         
         isPortMatch = (httpPort != null && httpPort.equals(port)) ||
               (httpsPort != null && httpsPort.equals(port));
      }
      
      // compare the host name
      try
      {
         isHostMatch = PSServer.getHostName().equalsIgnoreCase(host)
               || PSServer.getFullyQualifiedHostName().equalsIgnoreCase(host)
               || PSServer.getHostAddress().equalsIgnoreCase(host);
      }
      catch (Exception e)
      {
         log.error("Failed to identify server name or IP address.", e);
      }
      
      if (log.isDebugEnabled())
      {
         String hostName = PSServer.getHostName();
         if (isHostMatch && isPortMatch)
         {
            log.debug("Task of '" + curJob.getName()
                  + "' is registered for the server '" + hostName + "'.");
         }
         else
         {
            log.debug("Task of '" + curJob.getName()
                  + "' is not registered for the server '" + hostName + "'.");
         }
      }

      return isHostMatch && isPortMatch;
   }
   
   /**
    * Creates the task result for the failed task and the exception.
    * @param task the failed task, assumed not <code>null</code>.
    * @param e the cause of the failure, assumed not <code>null</code>.
    * @return the created task result, never <code>null</code>.
    */
   private static IPSTaskResult getErrorResult(PSScheduledTask task,
         Exception e, long startTime)
   {
      Throwable cause = e;
      if (e.getCause() != null)
         cause = e.getCause();
      String errorMsg = cause.getLocalizedMessage();
      if (StringUtils.isBlank(errorMsg))
         errorMsg = cause.toString();

      return getErrorResult(task, errorMsg, startTime);
   }

   /**
    * Creates a task result for the given failed or skipped task
    * @param task the failed task, assumed not <code>null</code>.
    * @param errorMsg the error message, assumed not <code>null</code> or empty.
    * @param startTime the start time of the task.
    * @return the created task result, never <code>null</code>.
    */
   private static IPSTaskResult getErrorResult(PSScheduledTask task,
         String errorMsg, long startTime)
   {
      return new PSTaskResult(false, errorMsg, PSScheduleUtils.getContextVars(
            task.getParameters(), startTime, System.currentTimeMillis()));
   }


   /**
    * Log the result of the task execution.
    *
    * @param curJob the current job, assumed not <code>null</code>.
    * @param result the result of the execution, it may be <code>null</code>
    *    if fail or skip to execute the task.
    * @param startTime the start time of the execution.
    * @param server the server invoked or skipped the task, assumed not
    *    <code>null</code>.
    */
   private static void logTaskExecution(PSScheduledTask curJob, IPSTaskResult result,
                                       long startTime, String server) {
        try {
            var endTime = System.currentTimeMillis();
            var logId = getScheduleService().createTaskLogId();
            var wasSuccess = result != null && result.wasSuccess();
            var resultMessage = result != null ? result.getProblemDescription() : "";

            var taskLog = new PSScheduledTaskLog(
                    logId,
                    curJob.getId(),
                    new Date(startTime),
                    new Date(endTime),
                    wasSuccess,
                    resultMessage,
                    server);

            getScheduleService().saveTaskLog(taskLog);
        } catch (Exception e) {
            log.error("Failed to log task execution: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    /**
     * Add a new job to the active jobs list with thread-safe operations.
     *
     * @param id the ID of the active job, never null
     */
    private static synchronized void addJobId(IPSGuid id) {
        Objects.requireNonNull(id, "Job ID cannot be null");
        ms_activeJobs.add(id);
    }

    /**
     * Remove a job from the active jobs list with thread-safe operations.
     *
     * @param id the job ID to be removed, never null
     */
    private static synchronized void removeJobId(IPSGuid id) {
        Objects.requireNonNull(id, "Job ID cannot be null");
        ms_activeJobs.remove(id);
    }

    /**
     * Determines if a given job is active using Stream API for enhanced performance.
     *
     * @param jobId the ID of the job in question, never null
     * @return true if the supplied job is active
     */
    private static synchronized boolean isJobActive(IPSGuid jobId) {
        Objects.requireNonNull(jobId, "Job ID cannot be null");
        return ms_activeJobs.stream().anyMatch(jobId::equals);
    }

    /**
     * Get email addresses from the notified role using Stream API processing.
     *
     * @param job the task in question, never null
     * @return the email addresses, may be null or empty if no role specified
     */
    private static String getEmailAddressesFromNotifyRole(PSScheduledTask job) {
        var roleName = job.getNotify();
        if (StringUtils.isBlank(roleName)) {
            return null;
        }

        var roleManager = PSRoleManager.getInstance();
        var users = roleManager.getSubjects(roleName, null);

        return users.stream()
                .map(user -> user.getAttributes().getAttribute("sys_email"))
                .filter(Objects::nonNull)
                .map(attr -> attr.getValues().get(0).toString())
                .map(PSTaskAdapter::normalizeEmailAddress)
                .collect(Collectors.joining(","));
    }

    /**
     * Evaluate the supplied subject using JEXL expression with enhanced error handling.
     *
     * @param subject the subject in JEXL expression, never null
     * @param evaluator the JEXL evaluator, never null
     * @return the evaluated subject, never null, may be empty
     */
    private static String getEvaluateSubject(String subject, PSJexlEvaluator evaluator) {
        try {
            var script = PSJexlEvaluator.createScript(subject);
            var result = evaluator.evaluate(script);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            log.error("Failed to evaluate subject: {}", subject, e);
            return "";
        }
    }

    /**
     * Get the JEXL utilities/tools loaded from tools.xml with enhanced caching.
     *
     * @return the map of tools, may be null if failed to load
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getToolsMap() {
        if (ms_toolsMap == null) {
            synchronized (PSTaskAdapter.class) {
                if (ms_toolsMap == null) {
                    var jexlBase = new PSServiceJexlEvaluatorBase(false);
                    try {
                        ms_toolsMap = (Map<String, Object>) jexlBase.getVelocityToolBindings();
                    } catch (Exception e) {
                        ms_toolsMap = null;
                        log.error("Failed to load Velocity Tools", e);
                    }
                }
            }
        }
        return ms_toolsMap;
    }

    /**
     * Render the job result with the specified notification template.
     *
     * @param template the notification template, never null
     * @param vars the job result variables, never null
     * @return the rendered text, may be null if rendering fails
     */
    @SuppressWarnings("unchecked")
    private static String getNotifyMessage(PSNotificationTemplate template, Map<String, Object> vars) {
        var context = PSVelocityUtils.getContext(vars);

        try (var writer = new StringWriter()) {
            var velocityTemplate = PSVelocityUtils.compileTemplate(
                    template.getTemplate(),
                    "EventNotification",
                    getVelocityRS());

            velocityTemplate.merge(context, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Failed to format Notification Template id={}: {}",
                    template.getId(), PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return null;
        }
    }

    /**
     * The Velocity Tools map, initialized lazily with thread-safe access.
     */
    private static volatile Map<String, Object> ms_toolsMap;

    /**
     * Thread-safe list containing IDs of all active jobs (scheduled and manual).
     */
    private static final List<IPSGuid> ms_activeJobs = new ArrayList<>();
}
