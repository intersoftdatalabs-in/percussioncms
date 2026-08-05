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
package com.percussion.services.workflow.data;

import static org.apache.commons.lang3.Validate.notNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Base class for (non-persistent) transition types. Jackson design-XML property annotations shared
 * by {@link PSTransition} and {@link PSAgingTransition} (issue #1890 / epic #505).
 */
public abstract class PSTransitionBase implements IPSTransitionBase {
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = 1L;
   
   private long transitionId;
   
   private long workflowId;

   private long stateId;

   private String label;
   
   private String description;
   
   private String trigger;
   
   private long toState;
   
   private String transitionAction;

   private List<PSNotification> notifications = new ArrayList<>();

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getGUID()
    */
   @JsonProperty("guid")
   public IPSGuid getGUID() {
      return new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, transitionId);
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException {
      if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

      // Allow overwrite on design-object XML restore (BeanUtils + Jackson).
      transitionId = newguid.longValue();
   }

   /**
    * Get the workflow id
    *
    * @return the workflowid
    */
   @JsonProperty
   public long getWorkflowId() {
      return workflowId;
   }

   public void setWorkflowId(long id) {
      workflowId = id;
   }

   /**
    * Get the from state id.
    *
    * @return the id.
    */
   @JsonProperty
   public long getStateId() {
      return stateId;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setStateId(long)
    */
   public void setStateId(long state)
   {
      stateId = state;
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getName()
    */
   @JsonProperty
   public String getName() {
      return getLabel();
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getLabel()
    */
   @JsonProperty
   public String getLabel() {
      return label;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setLabel(java.lang.String)
    */
   public void setLabel(String lbl)
   {
      label = lbl;
   }

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getDescription()
    */
   @JsonProperty
   public String getDescription() {
      return description;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setDescription(java.lang.String)
    */
   public void setDescription(String desc)
   {
      description = desc;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#getTrigger()
    */
   @JsonProperty
   public String getTrigger() {
      return trigger;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setTrigger(java.lang.String)
    */
   public void setTrigger(String triggerName)
   {
      this.trigger = triggerName;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#getToState()
    */
   @JsonProperty
   public long getToState() {
      return toState;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setToState(long)
    */
   public void setToState(long state)
   {
      toState = state;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#getTransitionAction()
    */
   @JsonProperty
   public String getTransitionAction() {
      return transitionAction;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setTransitionAction(java.lang.String)
    */
   public void setTransitionAction(String transAction)
   {
      transitionAction = transAction;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#getNotifications()
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "notifications")
   @JacksonXmlProperty(localName = "notification")
   public List<PSNotification> getNotifications() {
      return notifications;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.services.workflow.data.IPSTransitionBase#setNotifications(java.util.List)
    */
   public void setNotifications(List<PSNotification> notificationList)
   {
      if (notificationList == null)
         notificationList = new ArrayList<>();
      
      notifications = notificationList;
   }
   
   /**
    * Add a notification to the existing notifications.
    *
    * <p>Ignored by Jackson; use {@link #setNotifications(List)} for XML restore.
    *
    * @param notification the to be added notification, not <code>null</code>.
    */
   @JsonIgnore
   public void addNotification(PSNotification notification) {
      notNull(notification);

      notifications.add(notification);
   }
}

