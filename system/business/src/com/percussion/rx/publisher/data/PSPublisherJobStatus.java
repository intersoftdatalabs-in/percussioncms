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
package com.percussion.rx.publisher.data;

import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.utils.guid.IPSGuid;

import java.util.Date;

/**
 * The status of the current job. This object is created and returned when the
 * system is asked for the status of a given job. At the completion of the job
 * the sum of the failed and delivered items will equal the count of items
 * original queued plus the count of additional pages expanded by the pagination
 * process.
 * <p>
 * Note that the set methods are only used internally and are inappropriate for
 * use by the caller. The interface hides these methods.
 * 
 * @author dougrand
 */
public class PSPublisherJobStatus implements IPSPublisherJobStatus
{
   /**
    * The total number of items. 
    */
   private int m_totalItems;

   /**
    * Count of items that failed assembly.
    */
   private int m_failedItems = 0;
   /**
    * Count of items which are submitted for transactional delivery,
    * but not committed yet.
    */
   private int m_itemsPreparedForDelivery;
   /**
    * Count of items that have been delivered to the publisher.
    */
   private int m_deliveredItems = 0;
   /**
    * Count of items queued for assembly.
    */
   private int m_queuedAssemblyItems = 0;
   /**
    * Count of items queued for delivery.
    */
   private int m_assembledDeliveryItems = 0;
   /**
    * Current state.
    */
   private State m_state;
   /**
    * Contains the job's starting date and time.
    */   
   private Date m_startTime;
   
   /**
    * Contains the job's elapsed time.
    */
   private long m_elapsed;
   
   /**
    * Contains the edition id for the job.
    */
   private IPSGuid m_edition;
  
   /**
    *  Should this edition be immediately rerun
    */
   private boolean m_rerunAfter;

   /**
    *  General publishing message 
    */
   private String m_message;
   
   /**
    * The id of the job
    */
   private long m_jobId;
   
   // see base
   public int countTotalItems()
   {
      return m_totalItems;
   }

   public int countFailedItems()
   {
      return m_failedItems;
   }

   // see base
   public int countItemsPreparedForDelivery()
   {
      return m_itemsPreparedForDelivery;
   }

   public int countItemsDelivered()
   {
      return m_deliveredItems;
   }

   public int countItemsQueuedForAssembly()
   {
      return m_queuedAssemblyItems;
   }

   public int countAssembledItems()
   {
      return m_assembledDeliveryItems;
   }

   public State getState()
   {
      return m_state;
   }

   /**
    * Total number of items.
    * @param totalItems the total number of items.
    * @see #countTotalItems
    */
   public void setTotalItems(int totalItems)
   {
      m_totalItems = totalItems;
   }

   /**
    * @param itemsPreparedForDelivery the value returned by
    * {@link #countItemsPreparedForDelivery()}.
    * @see #countItemsPreparedForDelivery()
    */
   public void setItemsPreparedForDelivery(int itemsPreparedForDelivery)
   {
      m_itemsPreparedForDelivery = itemsPreparedForDelivery;
   }

   /**
    * @param deliveredItems the deliveredItems to set
    */
   public void setDeliveredItems(int deliveredItems)
   {
      m_deliveredItems = deliveredItems;
   }

   /**
    * @param failedItems the failedItems to set
    */
   public void setFailedItems(int failedItems)
   {
      m_failedItems = failedItems;
   }

   /**
    * @param queuedAssemblyItems the queuedAssemblyItems to set
    */
   public void setQueuedAssemblyItems(int queuedAssemblyItems)
   {
      m_queuedAssemblyItems = queuedAssemblyItems;
   }

   /**
    * @param assembledDeliveryItems the queuedDeliveryItems to set
    */
   public void setAssembledDeliveryItems(int assembledDeliveryItems)
   {
      m_assembledDeliveryItems = assembledDeliveryItems;
   }

   /**
    * @param state the state to set
    */
   public void setState(State state)
   {
      m_state = state;
   }

   /**
    * @return the startTime
    */
   public Date getStartTime()
   {
      return m_startTime;
   }

   /**
    * @param startTime the startTime to set
    */
   public void setStartTime(Date startTime)
   {
      m_startTime = startTime;
   }

   public long getElapsed()
   {
      return m_elapsed;
   }

   /**
    * @param elapsed the elapsed to set
    */
   public void setElapsed(long elapsed)
   {
      m_elapsed = elapsed;
   }

   /**
    * @return the edition
    */
   public IPSGuid getEditionId()
   {
      return m_edition;
   }

   /**
    * @param edition the edition to set
    */
   public void setEditionId(IPSGuid edition)
   {
      m_edition = edition;
   }

   /** 
    *  @return should this edition be immediately rerun
    */
   public boolean isRerunAfter()
   {
      return m_rerunAfter;
   }
   
   /** 
    *  @param should this edition be immediately rerun
    */
   public void setRerunAfter(boolean rerunAfter)
   {
      m_rerunAfter = rerunAfter;
   }
   /** 
    *  @return general publishing message to user
    */
   public String getMessage()
   {
      return m_message;
   }
   /** 
    *  @param general publishing message to user
    */
   public void setMessage(String message) {
      m_message = message;
   }

     public long getJobId()
   {
      return m_jobId;
   }
   
   /**
    * @param jobId The id of the job
    */
   public void setJobId(long jobId)
   {
      m_jobId = jobId;
   }
}
