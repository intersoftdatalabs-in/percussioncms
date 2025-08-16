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
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.rx.jsf.PSNavigation;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.rx.publisher.PSRxPublisherServiceLocator;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPubStatus;
import com.percussion.services.publisher.IPSPubStatus.EndingState;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;

import java.util.List;

/**
 * Represents a running edition. The running edition jsp uses AJAX to query the
 * runtime status of the edition. The shown log data is built with straight JSF,
 * so it is not dynamic.
 * 
 * @author dougrand
 * 
 */
public class PSRuntimeEditionNode extends PSLogNode
{
   /**
    * The edition represented by this runtime node.
    */
   private IPSEdition m_edition;
   
   /**
    * If this element is selected.
    */
   private boolean m_selected = false;

   /**
    * The business publishing service.
    */
   private IPSRxPublisherService m_rxpub = PSRxPublisherServiceLocator
         .getRxPublisherService();

   /**
    * Constructor.
    * @param e the edition, never <code>null</code> or empty.
    */
   /**
    * Constructs a runtime edition node for an edition.
    * @param e the edition, never null
    */
   /**
    * Constructs a runtime edition node for an edition.
    * @param e the edition, never null
    */
   public PSRuntimeEditionNode(IPSEdition e) {
      super(e.getDisplayTitle(), "pub-runtime-edition");
      m_edition = e;
      setOutcome("pub-runtime-edition");
   }

   /**
    * Determines if the site column need to be rendered or not.
    * @return <code>true</code> if the site column need to be rendered.
    */
   /**
    * Determines if the site column should be rendered.
    */
   @Override
   public boolean isShowSiteColumn() {
      return false;
   }

   /**
    * Get the edition id 
    * 
    * @return the edition id
    */
   public long getEditionId()
   {
      return m_edition.getGUID().longValue();
   }
   
   /**
    * @return the job id for the running edition or <code>0</code> if no 
    * edition is running.
    */
   public long getJobId()
   {
      return m_rxpub.getEditionJobId(m_edition.getGUID());
   }

   /**
    * @return <code>true</code> if this element is selected.
    */
   /**
    * Returns true if this element is selected.
    */
   @Override
   public boolean getSelected() {
      return m_selected;
   }

   /**
    * @param selected the selected to set
    */
   public void setSelected(boolean selected)
   {
      m_selected = selected;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.rx.publisher.jsf.nodes.PSLogNode#getStatusLogs()
    */
   /**
    * Gets the status logs for this edition.
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<IPSPubStatus> getStatusLogs() {
      var psvc = PSPublisherServiceLocator.getPublisherService();
      List<IPSPubStatus> logs = java.util.Collections.emptyList();
      try {
         var method = psvc.getClass().getMethod("findPubStatusByEdition", com.percussion.utils.guid.IPSGuid.class);
         Object result = method.invoke(psvc, m_edition.getGUID());
         if (result instanceof List) {
            logs = (List<IPSPubStatus>) result;
         }
      } catch (Exception e) {
         // Method not available, fallback to empty list
      }
      return logs;
   }

   /**
    * Get the running job of this Edition.
    * @return the currently running job of this Edition. It may be 
    * <code>null</code> if there is no running job for this Edition.
    */
   private IPSPublisherJobStatus getJobStatus()
   {
      long jobid = m_rxpub.getEditionJobId(m_edition.getGUID());
      if (jobid == 0)
         return null;
      try
      {
         IPSPublisherJobStatus stat = m_rxpub.getPublishingJobStatus(jobid);
         return stat;
      }
      catch(Exception e)
      {
         return null;
      }
   }

   /**
    * @return <code>true</code> if this edition is currently running.
    */
   public boolean getRunning()
   {
      IPSPublisherJobStatus stat = getJobStatus();
      return stat != null && (! stat.getState().isTerminal());
   }

   /**
    * Get the state of the job of the Edition.
    * @return the state of the Edition job. It is blank if there is no running
    *    job for this Edition.
    */
   public String getStatus()
   {
      IPSPublisherJobStatus status = getJobStatus();
      if (status == null)
         return "";
      else
         return status.getState().getDisplayName();
   }
   
   /**
    * Gets the image URL of the current status.
    * 
    * @return the image URL. It may be <code>null</code> if there is no 
    *    running job with this edition. 
    */
   public String getStatusImage()
   {
      IPSPublisherJobStatus status = getJobStatus();
      if (status == null)
         return null;
      
      EndingState endState = PSPublishingStatusHelper.getEndingState(status
            .getState());
      String[] imgSrc = PSPublishingStatusHelper.getStatusImage(endState,
            true, true);
      return imgSrc[0];
   }

   /**
    * Use animated icon because this page automatically refreshes itself.
    */
   @Override
   protected boolean useAnimatedIcon() {
      return true;
   }

   /**
    * Get the percent of the progress for the Edition job.
    * @return the progress. It is 100 if there is no running job for this
    *    Edition.
    */
   public String getProgress()
   {
      IPSPublisherJobStatus status = getJobStatus();
      if (status == null)
         return "100";
      else
         return "" + PSPublishingStatusHelper.getJobCompletionPercent(status);
   }

   
   /**
    * @return get the edition, never <code>null</code>.
    */
   public IPSEdition getEdition()
   {
      return m_edition;
   }

   public String getEditionTypeName()
   {
      if(m_edition != null){
         return m_edition.getName();
      }
      return null;
   }
   
   /**
    * Start the edition if it isn't running.
    * @return the outcome, <code>null</code>.
    */
   public String start()
   {
      if (! getRunning())
      {
         m_rxpub.startPublishingJob(m_edition.getGUID(), null);
      }
      
      return perform();
   }
   
   /**
    * Start the Edition from its parent node.
    * @return the outcome of the parent node. 
    */
   public String startFromParent()
   {
      start();

      /**
       * Note, assume this is called from the current parent node.
       * it is expected the navigation of the parent page defined
       * the <redirect> to itself. Cannot return null here; otherwise 
       * refresh browser may restart the last started Edition.
       */
      return getParent().perform();
   }
   
   /**
    * Stop the edition if it is running.
    * @return the outcome, <code>null</code>.
    */
   public String stop()
   {
      if (getRunning())
      {
         long jobid = m_rxpub.getEditionJobId(m_edition.getGUID());
         m_rxpub.cancelPublishingJob(jobid);
         try
         {
            /**
             * Give the job some time to change to cancel status
             */
            Thread.sleep(3000);
         }
         catch (InterruptedException ignore){
            Thread.currentThread().interrupt();
         }
      }
      
      return null;
   }
   
   @SuppressWarnings("cast")
   /**
    * Performs navigation for this edition node.
    */
   @Override
   public String perform() {
      var navigator = (PSNavigation) getModel().getNavigator();
      navigator.setCurrentItemGuid(m_edition.getGUID());
      return super.perform();
   }
   
   /**
    * Gets the CSS class for navigation link.
    */
   @Override
   public String getNavLinkClass() {
      return "pubruntime-nav-edition-display";
   }
   
   /**
    * Get the name of the current site.
    * @return the site name, never <code>null</code> or empty.
    */
   public String getSiteName()
   {
      return getParent().getParent().getLabel();
   }
   
   /**
    * Gets the help topic for this node.
    */
   @Override
   public String getHelpTopic() {
      return "RuntimeEdition";
   }
}
