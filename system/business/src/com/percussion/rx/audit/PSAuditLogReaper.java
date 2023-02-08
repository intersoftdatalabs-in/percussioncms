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
package com.percussion.rx.audit;

import com.percussion.services.audit.IPSDesignObjectAuditConfig;
import com.percussion.services.audit.IPSDesignObjectAuditService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * Automatically deletes old audit log entries on a daily basis as configured by
 * the {@link IPSDesignObjectAuditConfig}.
 */
public class PSAuditLogReaper extends Thread
{
   /**
    * Default ctor
    */
   public PSAuditLogReaper()
   {
      super();
      setDaemon(true);
      setName("AuditLogReaper");
   }

   /**
    * Called by Spring dependency injection to set the audit service on this 
    * object, used as an event to start the pruning thread.  The thread is not
    * started if pruning is disabled (see 
    * {@link IPSDesignObjectAuditConfig#getLogRetentionDays()}).  Method is 
    * synchronized to protect against multiple calls starting more than one
    * thread.
    * 
    * @param service The service to use, may not be <code>null</code>.
    * 
    * @throws IllegalStateException if the service has already been set.
    */
   public synchronized void setService(IPSDesignObjectAuditService service)
   {
      if (service == null)
         throw new IllegalArgumentException("service may not be null");
      
      if (m_service != null)
         throw new IllegalStateException("service has already been set");
      
      m_service = service;
      
      IPSDesignObjectAuditConfig config = m_service.getConfig();
      m_logRetentionDays = config.getLogRetentionDays();
      if (config.isEnabled())
      {
         ms_log.info("Design object audit Logging is enabled");
         
         if (m_logRetentionDays > 0)
         {
            ms_log.info("Design object audit log pruning is enabled, log " +
               "entries will be deleted after " + m_logRetentionDays +" days.");
            start();
         }
         else 
         {
            ms_log.info("Design object audit log pruning is disabled, log "
               + "entries will be saved indefinitely");
         }
      }
      else
      {
         ms_log.info("Design Audit Logging is disabled");
      }
   }
   
   /**
    * Deletes logs older than the interval specified by 
    * {@link IPSDesignObjectAuditConfig#getLogRetentionDays()}.  Sleeps for 24
    * hours between attempts.
    */
   @Override
   public void run()
   {
      ms_log.info("Starting Design Audit Log Reaper");
      
      // reap now, wait 24 hours and repeat
      try
      {
         while (true)
         {
            deleteLogEntries();
            
            long sleepTime = m_sleepIntervalMins * MINS_TO_MILLIS;
            ms_log.debug("Next audit log pruning will be in " + 
               m_sleepIntervalMins + " minutes");
            sleep(sleepTime);
         }
      }
      catch (InterruptedException e)
      {
         ms_log.info("Interrupted, shutting down");
         Thread.currentThread().interrupt();
      }
      catch (Throwable t)
      {
         ms_log.error("Error deleting design object audit logs", t);
      }
   }
   
   /**
    * Worker method to perform the delete of log entries, synchronizing on 
    * {@link #m_monitor} to prevent {@link #shutdown()} from executing during 
    * the deletion. See {@link #run()} for more details.
    */
   private void deleteLogEntries()
   {
      synchronized (m_monitor)
      {
         Date date = calculatedDeleteDate();
         ms_log.debug("Deleting design object audit logs older than " + date);      
         m_service.deleteAuditLogEntriesByDate(date);
      }
   }
   
   /**
    * Method to nicely interrupt this thread, synchronizes on {@link #m_monitor}
    * to prevent interruption of the {@link #deleteLogEntries()} method.
    */
   public void shutdown()
   {
      try
      {
         synchronized (m_monitor)
         {
            interrupt();
         }
      }
      catch (Exception e)
      {
         // ignore, we tried
      }
   }
   
   /**
    * Set the number of minutes to sleep between reaping.
    * 
    * @param mins The number of minutes, must be > 0.
    */
   public void setSleepIntervalMins(int mins)
   {
      if (mins <= 0)
         throw new IllegalArgumentException("mins must be > 0");
      
      m_sleepIntervalMins = mins;
   }
   
   /**
    * Gets the current date adjusted backwards by the value of 
    * {@link #m_logRetentionDays}.
    * 
    * @return The date, never <code>null</code>.
    */
   private Date calculatedDeleteDate()
   {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_MONTH, (m_logRetentionDays * -1));
      
      return cal.getTime();
   }
   
   /**
    * Number of days to keep logs for, set during first call to 
    * {@link #setService(IPSDesignObjectAuditService)}.
    */
   private int m_logRetentionDays;
   
   /**
    * Service to use to get the configuration and delete audit logs,
    * <code>null</code> until first call to
    * {@link #setService(IPSDesignObjectAuditService)}, never <code>null</code>
    * after that.
    */
   private IPSDesignObjectAuditService m_service = null;
   
   /**
    * Number of minutes to sleep between reaping, defaults to 24 hours.
    */
   private int m_sleepIntervalMins = 1440;
   
   /**
    * Number of milliseconds in a minute.
    */
   private static final long MINS_TO_MILLIS = 60 * 1000;
   
   /**
    * Logger for the reaper, never <code>null</code>.
    */
   private static final Logger ms_log = LogManager.getLogger(PSAuditLogReaper.class);
   
   /**
    * Object for locking synchronized blocks.
    */
   private Object m_monitor = new Object();
}
