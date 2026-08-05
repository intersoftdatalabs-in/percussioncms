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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
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
 * Represents associations between transitions and workflow roles. Required because using an
 * @JoinTable annotation doesn't work with PSWORKFLOWAPPID on both sides of the join table due to
 * Hiberate bug HHH-1338 (see http://opensource2.atlassian.com/projects/hibernate/browse/HHH-1338).
 *
 * <p>Design-object nested element is {@code transition-role} (issue #1890 / epic #505).
 */
@Entity
@Table(name = "TRANSITIONROLES")
@IdClass(PSTransitionRolePK.class)
@JacksonXmlRootElement(localName = "transition-role")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PSTransitionRole implements Serializable, IPSCatalogItem {
   private static final long serialVersionUID = 1L;

   @Id
   @Column(name = "TRANSITIONROLEID", nullable = false)
   private long roleId;
   
   @Id
   @Column(name = "TRANSITIONID", nullable = false)
   private long transitionId;
   
   @Id
   @Column(name = "WORKFLOWAPPID", nullable = false)
   private long workflowId;
   
   /**
    * Get the id of the workflow role.
    *
    * @return the id.
    */
   @JsonProperty
   public long getRoleId() {
      return roleId;
   }

   /**
    * Set the id of the workflow role.
    * 
    * @param roleid the id.
    */
   public void setRoleId(long roleid)
   {
      roleId = roleid;
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

   /**
    * Gets the guid of the referenced role id.
    *
    * @see IPSCatalogItem#getGUID()
    */
   @JsonProperty("guid")
   public IPSGuid getGUID() {
      return new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId);
   }

   /**
    * Set the guid fo the referenced role id
    *
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException {
      if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

      // Allow overwrite on design-object XML restore (BeanUtils + Jackson).
      roleId = newguid.longValue();
   }
   
   /**
    * Copy all properties from the source to current object, except the ID.
    * @param src the source object, never <code>null</code>.
    */
   public void copy(PSTransitionRole src)
   {
      notNull(src);
      setTransitionId(src.getTransitionId());
      setWorkflowId(src.getWorkflowId());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSTransitionRole)) return false;
      PSTransitionRole that = (PSTransitionRole) o;
      return getRoleId() == that.getRoleId() && getTransitionId() == that.getTransitionId() && getWorkflowId() == that.getWorkflowId();
   }

   @Override
   public int hashCode() {
      return Objects.hash(getRoleId(), getTransitionId(), getWorkflowId());
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSTransitionRole{");
      sb.append("roleId=").append(roleId);
      sb.append(", transitionId=").append(transitionId);
      sb.append(", workflowId=").append(workflowId);
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

