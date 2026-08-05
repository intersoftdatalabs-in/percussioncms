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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a workflow notification definition.
 *
 * <p>Design-object nested element is {@code notification-def} (issue #1890 / epic #505).
 */
@Entity
@Table(name = "NOTIFICATIONS")
@IdClass(PSNotificationDefPK.class)
@JacksonXmlRootElement(localName = "notification-def")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PSNotificationDef implements Serializable, IPSCatalogItem {
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = 1L;
   @Id
   @Column(name = "WORKFLOWAPPID", nullable = false)
   private long workflowId;
   
   @Id
   @Column(name = "NOTIFICATIONID", nullable = false)
   private long notificationId;
   
   @Basic
   @Column(name="SUBJECT", nullable = true)
   private String subject;
   
   @Basic
   @Column(name="BODY", nullable = true)
   private String body;
   
   @Basic
   @Column(name="DESCRIPTION", nullable = true)
   private String description;

   /* (non-Javadoc)
    * @see IPSCatalogSummary#getGUID()
    */
   @JsonProperty("guid")
   public IPSGuid getGUID() {
      return new PSGuid(PSTypeEnum.WORKFLOW_NOTIFICATION, notificationId);
   }

   /* (non-Javadoc)
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException {
      if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

      // Allow overwrite on design-object XML restore (BeanUtils + Jackson).
      notificationId = newguid.longValue();
   }

   /**
    * Get the subject text for this notification.
    *
    * @return the notification subject text, may be <code>null</code> or empty.
    */
   @JsonProperty
   public String getSubject() {
      return subject;
   }
   
   /**
    * Set the subject of this notification.
    * 
    * @param sub The subject, may be <code>null</code> or empty.
    */
   public void setSubject(String sub)
   {
      subject = sub;
   }

   /**
    * The body text for this notification.
    *
    * @return the notification body text, may be <code>null</code> or empty.
    */
   @JsonProperty
   public String getBody() {
      return body;
   }
   
   /**
    * Set the body text.
    * 
    * @param bodyText The text, may be <code>null</code> or empty.
    */
   public void setBody(String bodyText)
   {
      body = bodyText;
   }

   /**
    * A description for this notification.
    *
    * @return the notification description, may be <code>null</code> or empty.
    */
   @JsonProperty
   public String getDescription() {
      return description;
   }
   
   /**
    * Set the description
    * 
    * @param desc The description, may be <code>null</code>.
    */
   public void setDescription(String desc)
   {
      description = desc;
   }

   /**
    * Get the workflow id of this state
    * 
    * @param id The id.
    */
   public void setWorkflowId(long id)
   {
      workflowId = id;
   }

   /**
    * Get the workflow id
    *
    * @return The id.
    */
   @JsonProperty
   public long getWorkflowId() {
      return workflowId;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSNotificationDef)) return false;
      PSNotificationDef that = (PSNotificationDef) o;
      return getWorkflowId() == that.getWorkflowId() && notificationId == that.notificationId && Objects.equals(getSubject(), that.getSubject()) && Objects.equals(getBody(), that.getBody()) && Objects.equals(getDescription(), that.getDescription());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getWorkflowId(), notificationId, getSubject(), getBody(), getDescription());
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSNotificationDef{");
      sb.append("workflowId=").append(workflowId);
      sb.append(", notificationId=").append(notificationId);
      sb.append(", subject='").append(subject).append('\'');
      sb.append(", body='").append(body).append('\'');
      sb.append(", description='").append(description).append('\'');
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
}

