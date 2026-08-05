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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.string.PSStringUtils;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a transition notification.
 *
 * <p>Recipient lists use Betwixt wire shape {@code <recipients><string/>…} and {@code
 * <ccrecipients><string/>…} (issue #1890 / epic #505).
 */
@Entity
@Table(name = "TRANSITIONNOTIFICATIONS")
@IdClass(PSNotificationPK.class)
@JacksonXmlRootElement(localName = "notification")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PSNotification implements Serializable, IPSCatalogItem {
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = 1L;

   @Id
   @Column(name = "TRANSITIONNOTIFICATIONID", nullable = false)
   private long transNotificationId;
   
   @Id
   @Column(name = "NOTIFICATIONID", nullable = false)
   private long notificationId;
   
   @Id
   @Column(name = "TRANSITIONID", nullable = false)
   private long transitionId;
   
   @Id
   @Column(name = "WORKFLOWAPPID", nullable = false)
   private long workflowId;
   
   @Basic
   @Column(name="STATEROLERECIPIENTTYPES", nullable = false)
   private int stateRoleRecipientType = 
      PSStateRoleRecipientTypeEnum.TO_STATE_RECIPIENTS.getValue();
   
   @Basic
   @Column(name="ADDITIONALRECIPIENTLIST", nullable = true)
   private String recipients = "";
   
   @Basic
   @Column(name="CCLIST", nullable = true)
   private String ccRecipients = "";

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getGUID()
    */
   @JsonProperty("guid")
   public IPSGuid getGUID() {
      return new PSGuid(PSTypeEnum.WORKFLOW_TRANS_NOTIFICATION, transNotificationId);
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException {
      if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

      // Allow overwrite on design-object XML restore (BeanUtils + Jackson).
      transNotificationId = newguid.longValue();
   }

   /**
    * Get the state role recipient type.
    *
    * @return the state role recipient type used for this notification, never <code>null</code>.
    */
   @JsonProperty
   public PSStateRoleRecipientTypeEnum getStateRoleRecipientType() {
      return PSStateRoleRecipientTypeEnum.valueOf(stateRoleRecipientType);
   }
   
   /**
    * Set the state role recipient type.
    * 
    * @param type The type, may not be <code>null</code>.
    */
   public void setStateRoleRecipientType(PSStateRoleRecipientTypeEnum type)
   {
      if (type == null)
         throw new IllegalArgumentException("type may not be null");
      stateRoleRecipientType = type.getValue();
   }
   
   /**
    * Add a recipient to this notification's recipient list.
    * 
    * @param recipient The email address of the recipient, may not be 
    * <code>null</code> or empty.
    */
   @JsonIgnore
   public void addRecipient(String recipient) {
      if (StringUtils.isBlank(recipient))
         throw new IllegalArgumentException("recipient may not be null or empty");

      if (StringUtils.isBlank(recipients)) recipients = recipient;
      else recipients += ("," + recipient);
   }

   /**
    * Get all additional recipients for this notifications.
    *
    * <p>Item element is {@code string} (Betwixt type-mapped {@code String}). Use a dedicated
    * Jackson-facing property name so two list fields can share the same item element name without
    * conflicting (issue #1890).
    *
    * @return all additional recipients email addresses, never <code>null</code>, may be empty.
    */
   @JsonProperty("recipients")
   @JacksonXmlElementWrapper(localName = "recipients")
   public List<String> getRecipients() {
      if (recipients == null) return new ArrayList<>();
      else return new ArrayList<>(Arrays.asList(StringUtils.split(recipients, ',')));
   }
   
   /**
    * Get the recipients list
    * 
    * @param recipientList The list, may be <code>null</code> or empty.
    */
   public void setRecipients(List<String> recipientList)
   {
      if (recipientList == null)
         recipientList = new ArrayList<>();
      
      recipients = PSStringUtils.listToString(recipientList, ",");
   }

   /**
    * Add a CC recipient to this notification's recipient list.
    * 
    * @param recipient The email address of the recipient, may not be 
    * <code>null</code> or empty.
    */
   @JsonIgnore
   public void addCCRecipient(String recipient) {
      if (StringUtils.isBlank(recipient))
         throw new IllegalArgumentException("recipient may not be null or empty");

      if (StringUtils.isBlank(ccRecipients)) ccRecipients = recipient;
      else ccRecipients += ("," + recipient);
   }

   /**
    * Get all additional CC recipients for this notification.
    *
    * <p>Wire wrapper is historical Betwixt name {@code ccrecipients} (not kebab {@code
    * cc-recipients}).
    *
    * @return all additional CC recipients email addresses, never <code>null</code>, may be empty.
    */
   @JsonProperty("ccrecipients")
   @JacksonXmlElementWrapper(localName = "ccrecipients")
   public List<String> getCCRecipients() {
      if (ccRecipients == null) return new ArrayList<>();
      else return new ArrayList<>(Arrays.asList(StringUtils.split(ccRecipients, ',')));
   }
   
   /**
    * Set the list of additional CC recipients for this notification.
    * 
    * @param ccRecipientList The list of recipients, may be <code>null</code> or 
    * empty.
    */
   public void setCCRecipients(List<String> ccRecipientList)
   {
      if (ccRecipientList == null)
         ccRecipientList = new ArrayList<>();
      
      this.ccRecipients = PSStringUtils.listToString(ccRecipientList, ",");
   }
   
   /**
    * Get the id of the transition for which this notification is specified.
    *
    * @return the id.
    */
   @JsonProperty
   public long getTransitionId() {
      return transitionId;
   }

   /**
    * Set the id of the transition for which this notification is specified.
    * 
    * @param transId the id.
    */
   public void setTransitionId(long transId)
   {
      this.transitionId = transId;
   }

   /**
    * Get the id of the workflow for which is notification is specified.
    *
    * @return the id
    */
   @JsonProperty
   public long getWorkflowId() {
      return workflowId;
   }

   /**
    * Set the id of the workflow for which is notification is specified.
    * 
    * @param workflowId the id.
    */
   public void setWorkflowId(long workflowId)
   {
      this.workflowId = workflowId;
   }   

   @JsonProperty
   public long getNotificationId() {
      return notificationId;
   }
   
   public void setNotificationId(long id)
   {
      notificationId = id;
   }
   
   /**
    * Copy all properties from the source to current object, except the ID.
    * @param src the source object, never <code>null</code>.
    */
   public void copy(PSNotification src)
   {
      notNull(src);
      
      setCCRecipients(src.getCCRecipients());
      setNotificationId(getNotificationId());
      setRecipients(src.getRecipients());
      setStateRoleRecipientType(src.getStateRoleRecipientType());
      setTransitionId(src.getTransitionId());
      setWorkflowId(src.getWorkflowId());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSNotification)) return false;
      PSNotification that = (PSNotification) o;
      return transNotificationId == that.transNotificationId && getNotificationId() == that.getNotificationId() && getTransitionId() == that.getTransitionId() && getWorkflowId() == that.getWorkflowId() && getStateRoleRecipientType() == that.getStateRoleRecipientType() && Objects.equals(getRecipients(), that.getRecipients()) && Objects.equals(ccRecipients, that.ccRecipients);
   }

   @Override
   public int hashCode() {
      return Objects.hash(transNotificationId, getNotificationId(), getTransitionId(), getWorkflowId(), getStateRoleRecipientType(), getRecipients(), ccRecipients);
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSNotification{");
      sb.append("transNotificationId=").append(transNotificationId);
      sb.append(", notificationId=").append(notificationId);
      sb.append(", transitionId=").append(transitionId);
      sb.append(", workflowId=").append(workflowId);
      sb.append(", stateRoleRecipientType=").append(stateRoleRecipientType);
      sb.append(", recipients='").append(recipients).append('\'');
      sb.append(", ccRecipients='").append(ccRecipients).append('\'');
      sb.append('}');
      return sb.toString();
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#fromXML(String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }
   
   static {
      // Historical Betwixt adder element names + wire item element used in design XML.
      PSXmlSerializationHelper.addType("recipient", String.class);
      PSXmlSerializationHelper.addType("ccrecipient", String.class);
      PSXmlSerializationHelper.addType("string", String.class);
   }
   
   /**
    * Enumeration of the workflow state role recipient types.
    */
   public enum PSStateRoleRecipientTypeEnum
   {
      /** 
       * Indicates no state role notification recipients 
       */
      NO_STATE_RECIPIENTS(0),

      /** 
       * Indicates only to-state role notification recipients 
       */
      TO_STATE_RECIPIENTS(1),

      /** 
       * Indicates only from-state role notification recipients 
       */
      FROM_STATE_RECIPIENTS(2),

      /** 
       * Indicates both to-state and from-state role notification recipients 
       */
      TO_AND_FROM_STATE_RECIPIENTS(3);
      
      /**
       * Get the enum with the matching value.
       * 
       * @param value The value.
       * 
       * @return The enum, never <code>null</code>.
       */
      public static PSStateRoleRecipientTypeEnum valueOf(int value)
      {
         for (PSStateRoleRecipientTypeEnum type : values())
         {
            if (type.mi_value == value)
            {
               return type;
            }
         }
         
         throw new IllegalArgumentException("invalid value");
      }
      
      /**
       * Get the value of this enum.
       * 
       * @return The value.
       */
      public int getValue()
      {
         return mi_value;
      }
      
      private PSStateRoleRecipientTypeEnum(int value)
      {
         mi_value = value;
      }
      
      /**
       * The enum value
       */
      private int mi_value;      
   }
}

