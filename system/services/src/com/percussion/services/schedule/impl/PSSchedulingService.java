// REFACTORED: CP-JAVA11
// JAVA_11_REFACTORED: This class has been modernized with Java 11 features by Sunny Sal
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
package com.percussion.services.schedule.impl;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.schedule.IPSSchedulingService;
import com.percussion.services.schedule.PSSchedulingException;
import com.percussion.services.schedule.PSSchedulingException.Error;
import com.percussion.services.schedule.data.PSNotificationTemplate;
import com.percussion.services.schedule.data.PSScheduledTask;
import com.percussion.services.schedule.data.PSScheduledTaskLog;
import com.percussion.utils.guid.IPSGuid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * Manage scheduled jobs for Rhythmyx. Uses the OpenSymphony Quartz scheduler
 * library to provide comprehensive task scheduling capabilities.
 * <p>
 * Quartz {@code scheduler} and Hibernate {@code sessionFactory}
 * properties must be initialized before the service can be used.
 * This service has been modernized with Java 11 features for better
 * maintainability and performance.
 *
 * @author Doug Rand
 * @author Andriy Palamarchuk
 * @since Java 11 Modernization
 */
@Transactional
public class PSSchedulingService implements IPSSchedulingService {

   @PersistenceContext
   private EntityManager entityManager;

   private Session getSession() {
      return entityManager.unwrap(Session.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   @Transactional
   public PSScheduledTask createSchedule() {
      var schedule = new PSScheduledTask();
      schedule.setId(getGuidManager().createGuid(PSTypeEnum.SCHEDULED_TASK));
      return schedule;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Optional<PSScheduledTask> findScheduledTaskById(IPSGuid id) throws PSSchedulingException {
      if (id == null) {
         throw new IllegalArgumentException("Schedule id may not be null");
      }

      try {
         return Optional.ofNullable(maybeCreateScheduleFromJob(id));
      } catch (SchedulerException e) {
         throw new PSSchedulingException(
               Error.SCHEDULER.ordinal(), e, e.getLocalizedMessage());
      }
   }

   /**
    * Creates a schedule from a quartz job for the provided id,
    * if the job exists.
    *
    * @param id the schedule id, assumed not {@code null}
    * @return the schedule, {@code null} if the schedule with the provided
    *         id does not exist
    * @throws SchedulerException on Quartz error
    * @throws PSSchedulingException on scheduling service error
    */
   private PSScheduledTask maybeCreateScheduleFromJob(IPSGuid id)
         throws SchedulerException, PSSchedulingException {
      var jobDetail = getScheduler().getJobDetail(new JobKey(id.toString(), JOB_GROUP));
      if (jobDetail == null) {
         return null;
      }

      var storedSchedule = PSScheduleUtils.getStoredSchedule(jobDetail);
      if (storedSchedule == null) {
         throw new PSSchedulingException(
               Error.JOB_WITHOUT_SCHEDULE.ordinal(), id.toString());
      }

      var schedule = new PSScheduledTask();
      schedule.apply(storedSchedule);
      return schedule;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<PSScheduledTask> findAllSchedules() throws PSSchedulingException {
      var schedules = new ArrayList<PSScheduledTask>();

      try {
         GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(JOB_GROUP);

         for (var jobKey : getScheduler().getJobKeys(groupMatcher)) {
            var id = new PSGuid(jobKey.getName());
            var scheduleOpt = findScheduledTaskById(id);

            // Only add if schedule exists - there might be orphaned jobs
            scheduleOpt.ifPresent(schedules::add);
         }
      } catch (SchedulerException e) {
         throw new PSSchedulingException(
               Error.SCHEDULER.ordinal(), e, e.getLocalizedMessage());
      }

      return schedules;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Optional<PSScheduledTask> findScheduleByName(String label) throws PSSchedulingException {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

      return findAllSchedules().stream()
         .filter(schedule -> schedule.getName().equals(label))
         .findFirst();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   @Transactional(noRollbackFor = Exception.class)
   public void saveSchedule(PSScheduledTask schedule) throws PSSchedulingException {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

      if (schedule == null) {
         throw new IllegalArgumentException(SCHEDULE_NOT_NULL);
      }

      try {
         apply(schedule);
      } catch (SchedulerException e) {
         throw new PSSchedulingException(
               Error.SCHEDULER.ordinal(), e, e.getLocalizedMessage());
      } catch (ParseException e) {
         throw new PSSchedulingException(Error.CRON_FORMAT.ordinal(),
               e, schedule.getCronSpecification(), e.getLocalizedMessage());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   @Transactional
   public void runNow(PSScheduledTask schedule) {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

      if (schedule == null) {
         throw new IllegalArgumentException("schedule may not be null.");
      }

      try {
         if (!m_scheduler.isStarted()) {
            m_scheduler.start();
         }

         PSTaskAdapter.runJob(schedule, m_scheduler, true);

      } catch (SchedulerException e) {
         ms_log.error("An unexpected error occurred while running job: {} Error: {}",
                 schedule.getName(),
                 PSExceptionUtils.getMessageForLog(e));
         ms_log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }

   }

   /**
    * It fires a non-scheduled job in a separate thread.
    */
   class PSRunNow extends Thread
   {
      /**
       * Creates an instance for an given job.
       * @param job the executed job, never <code>null</code>.
       */
      public PSRunNow(PSScheduledTask job)
      {
         super("RunNow");

         if (job == null)
            throw new IllegalArgumentException("job may not be null.");

         m_job = job;
      }

      @Override
      public void run()
      {
         PSTaskAdapter.runJob(m_job, getScheduler(), true);
      }

      /**
       * The executed job, init by ctor, never <code>null</code> after that.
       */
      private PSScheduledTask m_job;
   }
   /**
    * Applies the provided schedule by configuring Quartz to run it.
    * @param schedule the schedule. Assumed not <code>null</code>.
    * @throws SchedulerException on Quartz error.
    * @throws ParseException on failure to parse the schedule cron
    * specification.
    * @throws PSSchedulingException
    */
   private void apply(PSScheduledTask schedule) throws SchedulerException,
         ParseException, PSSchedulingException
   {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

      // create these first, so in case of an error here
      // the old definition is not touched
      final Trigger trigger = createTrigger(schedule);
      final JobDetail jobDetail = createJobDetail(schedule);

      // save previous job/trigger for recovering in the case of failure
      var previousSchedOpt = findScheduledTaskById(schedule.getId());
      PSScheduledTask previousSched = previousSchedOpt.orElse(null);

      if (previousSched != null)
      {
         deleteSchedule(previousSched.getId());
      }
      try
      {
         getScheduler().scheduleJob(jobDetail, trigger);
      }
      finally
      {
         if (previousSched != null && findScheduledTaskById(schedule.getId()).isEmpty())
         {
            // In the case of failure, try to restore the old job.
            // Note, this still resets the last firing time for the job.
            saveSchedule(previousSched);
         }
      }
   }

   /**
    * Creates a Quartz trigger from a Rhythmyx schedule.
    * @param schedule the schedule to generate trigger for.
    * Assumed not <code>null</code>.
    * @return a trigger generated from the schedule data.
    */

   private Trigger createTrigger(PSScheduledTask schedule)
   {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
      final String id = schedule.getId().toString();

      CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(id,TRIGGER_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronSpecification()).withMisfireHandlingInstructionDoNothing()).build();

      // Workaround bug http://jira.opensymphony.com/browse/QUARTZ-566
      final String d = "dummy";
      trigger.getJobDataMap().put(d, d);
      return trigger;
   }

   /**
    * Creates a Quartz job detail object from a Rhythmyx schedule object.
    * @param schedule the Rhythmyx schedule to generate job detail for.
    * Assumed not null.
    * @return a job detail generated from the schedule data.
    * Not <code>null</code>.
    */
   @Transactional(noRollbackFor = Exception.class)
   public JobDetail createJobDetail(PSScheduledTask schedule)
   {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
      final String id = schedule.getId().toString();

      //final JobDetail jobDetail =
      //      new JobDetail(id, JOB_GROUP, PSTaskAdapter.class);

      JobDetail jobDetail = JobBuilder.newJob(PSTaskAdapter.class)
            .withIdentity(id, JOB_GROUP).build();

      PSScheduleUtils.storeScheduleInJob(schedule, jobDetail);
      return jobDetail;
   }

   // see base
   @Transactional(noRollbackFor = Exception.class)
   public void deleteSchedule(IPSGuid scheduleId) throws PSSchedulingException
   {
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
      if (scheduleId == null)
         throw new IllegalArgumentException(SCHEDULE_NOT_NULL);

      try
      {
         // delete the job and its associated trigger(s).
         getScheduler().deleteJob(new JobKey(scheduleId.toString(), JOB_GROUP));
      }
      catch (SchedulerException e)
      {
         throw new PSSchedulingException(
               Error.SCHEDULER.ordinal(), e, e.getLocalizedMessage());
      }
   }

   // see base
   @Transactional
   public PSNotificationTemplate createNotificationTemplate()
   {
      final PSNotificationTemplate t = new PSNotificationTemplate();
      t.setId(getGuidManager().createGuid(
            PSTypeEnum.SCHEDULE_NOTIFICATION_TEMPLATE));
      t.setName("SetNewName");
      t.setSubject("'Set a new subject'");
      return t;
   }

   // see base
   public Optional<PSNotificationTemplate> findNotificationTemplateById(IPSGuid id)
   {
      if (id == null)
      {
         throw new IllegalArgumentException(
               "Notification template id may not be null");
      }
      return Optional.ofNullable(getSession().get(
            PSNotificationTemplate.class, id.longValue()));
   }

   // see base

   public Optional<PSNotificationTemplate> findNotificationTemplateByName(String name)
   {
      final List<PSNotificationTemplate> results =
              getSession().createQuery(
                  "from PSNotificationTemplate where name = :name").setParameter(
                  "name", name).list();
      return results.stream().findFirst();
   }

   // see base

   public Collection<PSNotificationTemplate> findAllNotificationTemplates()
   {
      return getSession().createQuery("from PSNotificationTemplate", PSNotificationTemplate.class).list();
   }

   // see base
   public Set<String> findAllNotificationTemplatesNames()
   {
      final Set<String> labels = new HashSet<>();
      for (PSNotificationTemplate n : findAllNotificationTemplates())
      {
         labels.add(n.getName());
      }
      return labels;
   }

   // see base
   @Transactional(noRollbackFor = Exception.class)
   public void saveNotificationTemplate(
         PSNotificationTemplate notificationTemplate)
   {
      if (notificationTemplate == null)
      {
         throw new IllegalArgumentException(
               "Notification template may not be null");
      }
      getSession().merge(notificationTemplate);
   }

   // see base
   @Transactional(noRollbackFor = Exception.class)
   public void deleteNotificationTemplate(IPSGuid templateId)
   {
      if (templateId == null)
         throw new IllegalArgumentException("templateId may not be null");

      Session sess = getSession();

         MutationQuery hql = sess.createMutationQuery(DELETE_NOTIFICATION_TEMPLATE_HQL);
         hql.setParameter("id", templateId.longValue());
         hql.executeUpdate();

   }

   /**
    * Current Guid manager.
    * @return guid manager. Never <code>null</code>.
    */
   private IPSGuidManager getGuidManager()
   {
      return PSGuidManagerLocator.getGuidMgr();
   }

   /**
    * @return the Quartz scheduler. Never <code>null</code>.
    */
   private Scheduler getScheduler()
   {
      assert m_scheduler != null;
      return m_scheduler;
   }

   /**
    * Sets the Quart scheduler used by the service. This should be called
    * be the Spring framework.
    *
    * @param scheduler the scheduler to assign. Not <code>null</code>.
    */
   public void setScheduler(Scheduler scheduler)
   {
      if (scheduler == null)
      {
         throw new IllegalArgumentException(
               "Quartz scheduler should not be null.");
      }
      m_scheduler = scheduler;
      try
      {
         m_scheduler.start();
      }
      catch (Exception e)
      {
         ms_log.error("Failed to start the Quartz scheduler", e);
      }
   }

   // see base
   @Transactional
   public IPSGuid createTaskLogId()
   {
      return getGuidManager().createGuid(PSTypeEnum.SCHEDULE_TASK_LOG);
   }

   // see base
   public Optional<PSScheduledTaskLog> findTaskLogById(IPSGuid id)
   {
      if (id == null)
         throw new IllegalArgumentException("Event log id may not be null");

      return Optional.ofNullable(getSession().get(
            PSScheduledTaskLog.class, id.longValue()));

   }

   // see base
   @Transactional(noRollbackFor = Exception.class)
   public void saveTaskLog(PSScheduledTaskLog taskLog)
   {
      if (taskLog == null)
         throw new IllegalArgumentException("taskLog may not be null");

      getSession().merge(taskLog);
   }

   // see base
   @Transactional(noRollbackFor = Exception.class)
   public void deleteTaskLog(IPSGuid id)
   {
      if (id == null)
         throw new IllegalArgumentException("id must not be null.");

      deleteTaskLogEntries(Collections.singleton(id));
   }

   /*
    * //see base class method for details
    */
   @Transactional(noRollbackFor = Exception.class)
   public void deleteTaskLogs(Collection<IPSGuid> ids)
   {
      if (ids == null)
         throw new IllegalArgumentException("ids may not be null.");

      deleteTaskLogEntries(ids);
   }

   /**
    * The same as {@link #deleteTaskLogs(Collection)}, except this method does
    * the real work.
    */
   @Transactional(noRollbackFor = Exception.class)
   public void deleteTaskLogEntries(Collection<IPSGuid> ids)
   {
      Session sess = getSession();

         MutationQuery hql = sess.createMutationQuery(DELETE_TASK_LOG_HQL);
         for (IPSGuid id : ids)
         {
            hql.setParameter("logid", id.longValue());
            hql.executeUpdate();
         }

   }

   /*
    * //see base class method for details
    */
   @SuppressWarnings({ "cast", "unchecked" })
   public List<PSScheduledTaskLog> findAllTaskLogs(int maxResult)
   {
      Session s = getSession();

         String hql = "select e.log_id, e.task_id, e.start_time, e.end_time, e.is_success from PSScheduledTaskLog e order by e.end_time desc";
         Query<Object[]> q = s.createQuery(hql, Object[].class);
         if (maxResult > 0)
            q.setMaxResults(maxResult);

         List<Object[]> results = q.list();

         List<PSScheduledTaskLog> retval = new ArrayList<>();
         for (Object[] props : results)
         {
            retval.add(getScheduledTask(props));
         }
         return retval;

   }

   /**
    * Creates a task log entry from the given properties.
    *
    * @param props the properties of the created log entry, assumed not
    * <code>null</code>.
    *
    * @return the created log entry, which does not include the problem
    * description property, e.i, {@link PSScheduledTaskLog#getProblemDesc()}
    * will be <code>null</code> for the returned log entries. It is not
    * <code>null</code>, but may be empty.
    */
   public PSScheduledTaskLog getScheduledTask(Object[] props)
   {
      Long logId = (Long) props[0];
      IPSGuid logGuid = getGuidManager().makeGuid(logId,
            PSTypeEnum.SCHEDULE_TASK_LOG);
      Long taskId = (Long) props[1];
      IPSGuid taskGuid = getGuidManager().makeGuid(taskId,
            PSTypeEnum.SCHEDULED_TASK);
      Date startTime = (Date) props[2];
      Date endTime = (Date) props[3];
      boolean isSuccess = ((Character) props[4]) == 'Y';

      return new PSScheduledTaskLog(logGuid, taskGuid,
            startTime, endTime, isSuccess);
   }

   /*
    * //see base class method for details
    */
   @Transactional(noRollbackFor = Exception.class)
   public void deleteAllTaskLogs()
   {
      Session sess = getSession();

         sess.createMutationQuery(DELETE_ALL_TASK_LOGS_HQL).executeUpdate();

   }

   /*
    * //see base class method for details
    */
   @Transactional(noRollbackFor = Exception.class)
   public void deleteTaskLogsByDate(Date beforeDate)
   {
      if (beforeDate == null)
         throw new IllegalArgumentException("beforeDate may not be null");

      Session session = getSession();

         MutationQuery hql = session.createMutationQuery(DELETE_TASK_LOGS_BY_DATE_HQL);
         hql.setParameter("endTime", beforeDate);
         hql.executeUpdate();

   }

   /** HQL for typed unit tests (issue #3265). */
   public static final String DELETE_NOTIFICATION_TEMPLATE_HQL =
         "delete from PSNotificationTemplate t where t.id = :id";

   public static final String DELETE_TASK_LOG_HQL =
         "delete from PSScheduledTaskLog e where e.log_id = :logid";

   public static final String DELETE_ALL_TASK_LOGS_HQL = "delete from PSScheduledTaskLog";

   public static final String DELETE_TASK_LOGS_BY_DATE_HQL =
         "delete from PSScheduledTaskLog t where t.end_time < :endTime";

   /**
    * Quartz job group name for the quartz jobs used by this class.
    */
   private static final String JOB_GROUP = "rx";

   /**
    * Quartz trigger group name for the quartz triggers used by this class.
    */
   private static final String TRIGGER_GROUP = JOB_GROUP;

   /**
    * Validation exception message that the provided schedule is not null.
    */
   private static final String SCHEDULE_NOT_NULL = "Schedule may not be null";

   /**
    * @see #setScheduler(Scheduler)
    */
   private Scheduler m_scheduler;

   /**
    * The logger for this class.
    */
   private static final Logger ms_log = LogManager.getLogger(PSSchedulingService.class);
}
