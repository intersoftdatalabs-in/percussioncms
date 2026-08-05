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
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a workflow.
 *
 * <p>Design-object XML root is {@code workflow}. Nested package/export element names match
 * historical Betwixt writes ({@code state}, {@code role}, {@code notification-def}) — pinned via
 * Jackson annotations and {@link PSXmlSerializationHelper#addType} (issue #1890 / epic #505).
 */
@Entity
@Table(name = "WORKFLOWAPPS")
@JacksonXmlRootElement(localName = "workflow")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PSWorkflow implements Serializable, IPSCatalogSummary, IPSCatalogItem {
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = 3105407723614336921L;

   @Id
   @Column(name = "WORKFLOWAPPID", nullable = false)
   private long id;

   @Basic
   @Column(name = "WORKFLOWAPPNAME")
   private String name;

   @Basic
   @Column(name = "WORKFLOWAPPDESC")
   private String description;

   @Basic
   @Column(name = "ADMINISTRATOR")
   private String administratorRole;

   @Basic
   @Column(name = "INITIALSTATEID")
   private long initialStateId;
   
   /**
    * The object version.
    */
   private Integer version;

   @OneToMany(targetEntity = PSState.class, fetch = FetchType.LAZY, cascade =
   {CascadeType.ALL}, orphanRemoval = true)
   @JoinColumn(name = "WORKFLOWAPPID", insertable = false, updatable = false)
   private List<PSState> states = new ArrayList<>();

   @OneToMany(targetEntity = PSWorkflowRole.class, fetch = FetchType.EAGER, cascade =
   {CascadeType.ALL}, orphanRemoval = true)
   @JoinColumn(name = "WORKFLOWAPPID", insertable = false, updatable = false)
   @Fetch(FetchMode. SUBSELECT)
   private List<PSWorkflowRole> roles = new ArrayList<>();

   @OneToMany(targetEntity = PSNotificationDef.class, fetch = FetchType.LAZY, cascade =
   {CascadeType.ALL}, orphanRemoval = true)
   @JoinColumn(name = "WORKFLOWAPPID", insertable = false, updatable = false)
   private List<PSNotificationDef> notificationDefs = new ArrayList<>();

   /*
    * (non-Javadoc)
    *
    * @see IPSCatalogSummary#getGUID()
    */
   @JsonProperty("guid")
   public IPSGuid getGUID() {
      return new PSGuid(PSTypeEnum.WORKFLOW, id);
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException {
      if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

      // Allow overwrite on design-object XML restore (BeanUtils + Jackson); same pattern as
      // PSKeyword#setGUID (issue #1890).
      id = newguid.longValue();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSCatalogSummary#getName()
    */
   @JsonProperty
   public String getName() {
      return name;
   }

   /**
    * Set the workflow name.
    * 
    * @param wfname The name, may not be <code>null</code> or empty.
    */
   public void setName(String wfname)
   {
      if (StringUtils.isBlank(wfname))
         throw new IllegalArgumentException("wfname may not be null or empty");

      name = wfname;
   }

   /**
    * Get the object version.
    *
    * @return the object version, <code>null</code> if not initialized yet.
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Integer getVersion() {
      return version;
   }

   /**
    * Set the object version. The version can only be set once in the life cycle of this object.
    *
    * @param version the version of the object, must be >= 0.
    */
   public void setVersion(Integer version) {
      if (this.version != null && version != null)
         throw new IllegalStateException("version can only be initialized once");

      if (version != null && version.intValue() < 0)
         throw new IllegalArgumentException("version must be >= 0");

      this.version = version;
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSCatalogSummary#getLabel()
    */
   @JsonProperty
   public String getLabel() {
      return getName();
   }

   /*
    * (non-Javadoc)
    *
    * @see IPSCatalogSummary#getDescription()
    */
   @JsonProperty
   public String getDescription() {
      return description;
   }

   /**
    * Set the description.
    * 
    * @param desc The description, may be <code>null</code> or empty.
    */
   public void setDescription(String desc)
   {
      description = desc;
   }

   /**
    * Get the role of the administrator for this workflow.
    *
    * @return the administrator role, may be <code>null</code> or empty.
    */
   @JsonProperty
   public String getAdministratorRole() {
      return administratorRole;
   }

   /**
    * Set the role of the administrator for this workflow.
    * 
    * @param roleName the administrator role, may be <code>null</code> or
    *           empty.
    */
   public void setAdministratorRole(String roleName)
   {
      administratorRole = roleName;
   }

   /**
    * The id of the initial state into which all items enter this workflow.
    *
    * @return the initial state id.
    */
   @JsonProperty
   public long getInitialStateId() {
      return initialStateId;
   }

   /**
    * Get the intial state object.
    *
    * @return The state, or <code>null</code> if a valid initial state has not been specified.
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public PSState getInitialState() {
      PSState state = null;

      for (PSState test : states) {
         if (test.getStateId() == initialStateId) {
            state = test;
            break;
         }
      }

      return state;
   }

   /**
    * Set the initial state
    * 
    * @param initStateId the id of the initial state.
    */
   public void setInitialStateId(long initStateId)
   {
      initialStateId = initStateId;
   }

   /**
    * Add a state.
    *
    * <p>Ignored by Jackson (conflicts with collection item name {@code state}); use {@link
    * #setStates(List)} for XML restore.
    *
    * @param state The state to add, may not be <code>null</code>.
    */
   @JsonIgnore
   public void addState(PSState state) {
      if (state == null) throw new IllegalArgumentException("state may not be null");

      states.add(state);
   }

   /**
    * Get all workflow states.
    *
    * @return a list with all defined workflow states, never <code>null</code>, may be empty.
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "states")
   @JacksonXmlProperty(localName = "state")
   public List<PSState> getStates() {
      return states;
   }

   /**
    * Scan all states that are part of this workflow and return the one that
    * matches the supplied id.
    * 
    * @param stateId If <code>null</code>, <code>null</code> is returned.
    * 
    * @return The matching state, or <code>null</code> if no state matches.
    */
   public PSState findState(IPSGuid stateId)
   {
      if (null == stateId || states == null)
         return null;
      for (PSState state : states)
      {
         if (state.getGUID().equals(stateId))
            return state;
      }
      return null;
   }
   
   /**
    * Set the states.
    * 
    * @param stateList The states, may be <code>null</code> or empty.
    */
   public void setStates(List<PSState> stateList)
   {
      if (stateList == null)
         stateList = new ArrayList<>();

      states = stateList;
   }

   /**
    * The the supplied role to the collection.
    *
    * <p>Ignored by Jackson (conflicts with collection item name {@code role}); use {@link
    * #setRoles(List)} for XML restore.
    *
    * @param role The role to add, may not be <code>null</code> and the ID and name of the role must
    *     not exist in current role list.
    */
   @JsonIgnore
   public void addRole(PSWorkflowRole role) {
      notNull(role);

      // validate the added role does not exist
      for (PSWorkflowRole r : roles) {
         if (r.getGUID().equals(role.getGUID()))
            throw new IllegalArgumentException(
                "Role ID, \""
                    + role.getGUID()
                    + "\", already exists in workflow \""
                    + getName()
                    + "\".");

         if (r.getName().equalsIgnoreCase(role.getName()))
            throw new IllegalArgumentException(
                "Role name, \""
                    + role.getName()
                    + "\", already exists in workflow \""
                    + getName()
                    + "\".");
      }

      roles.add(role);
   }

   /**
    * Get all workflow roles.
    *
    * @return a list with all defined workflow roles, never <code>null</code>, may be empty.
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "roles")
   @JacksonXmlProperty(localName = "role")
   public List<PSWorkflowRole> getRoles() {
      return roles;
   }

   /**
    * Set the roles.
    * 
    * @param wfroles The roles, may be <code>null</code> or empty.
    */
   public void setRoles(List<PSWorkflowRole> wfroles)
   {
      if (wfroles == null)
         wfroles = new ArrayList<>();

      this.roles = wfroles;
   }

   /**
    * Add a notification to the collection.
    *
    * <p>Ignored by Jackson; use {@link #setNotificationDefs(List)} for XML restore.
    *
    * @param notif The notification to add, may not be <code>null</code>.
    */
   @JsonIgnore
   public void addNotificationDef(PSNotificationDef notif) {
      if (notif == null) throw new IllegalArgumentException("notif may not be null");

      notificationDefs.add(notif);
   }

   /**
    * Get all workflow notification definitions.
    *
    * @return a list with all defined workflow notificcations, never <code>null</code>, may be
    *     empty.
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "notification-defs")
   @JacksonXmlProperty(localName = "notification-def")
   public List<PSNotificationDef> getNotificationDefs() {
      return notificationDefs;
   }

   /**
    * Set the notifications.
    * 
    * @param notificationList The list of notifications, may be
    *           <code>null</code> or empty.
    */
   public void setNotificationDefs(List<PSNotificationDef> notificationList)
   {
      if (notificationList == null)
         notificationList = new ArrayList<>();

      notificationDefs = notificationList;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSWorkflow)) return false;
      PSWorkflow that = (PSWorkflow) o;
      return id == that.id && getInitialStateId() == that.getInitialStateId() && Objects.equals(getName(), that.getName()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getAdministratorRole(), that.getAdministratorRole()) && Objects.equals(getVersion(), that.getVersion()) && Objects.equals(getStates(), that.getStates()) && Objects.equals(getRoles(), that.getRoles()) && Objects.equals(getNotificationDefs(), that.getNotificationDefs());
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, getName(), getDescription(), getAdministratorRole(), getInitialStateId(), getVersion(), getStates(), getRoles(), getNotificationDefs());
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer("PSWorkflow{");
      sb.append("id=").append(id);
      sb.append(", name='").append(name).append('\'');
      sb.append(", description='").append(description).append('\'');
      sb.append(", administratorRole='").append(administratorRole).append('\'');
      sb.append(", initialStateId=").append(initialStateId);
      sb.append(", version=").append(version);
      sb.append(", states=").append(states);
      sb.append(", roles=").append(roles);
      sb.append(", notificationDefs=").append(notificationDefs);
      sb.append('}');
      return sb.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogItem#fromXML(String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }


   /**
    * Get the roles from this workflow that match user roles
    * 
    * @param userRoles the roles, never <code>null</code>, may be empty.
    * 
    * @return a list of corresponding guids, never <code>null</code>
    */
   public Set<Integer> getRoleIds(Collection<String> userRoles)
   {
      if (userRoles == null)
         throw new IllegalArgumentException("userRoles may not be null");
      
      Set<Integer> rids = new HashSet<>();

      for (PSWorkflowRole role : roles)
      {
         if (userRoles.contains(role.getName()))
         {
            rids.add(role.getGUID().getUUID());
         }
      }

      return rids;
   }
   
   /**
    * Get the role names for the specified wf role ids.
    * 
    * @param roleids The role ids to get the matching names for, may not be
    * <code>null</code>, may be empty.
    * 
    * @return A list of role names, never <code>null</code>, may be empty if
    * the supplied list is empty or no matches are found.
    */
   public Set<String> getRoleNames(Collection<Integer> roleids)
   {
      if (roleids == null)
         throw new IllegalArgumentException("roleids may not be null");
      
      Set<String> names = new HashSet<>();
      
      for (PSWorkflowRole role : roles)
      {
         if (roleids.contains(role.getGUID().getUUID()))
         {
            names.add(role.getName());
         }
      }
      
      return names;
   }
   
   static {
      // Register types with XML serializer for read creation of objects.
      // Wire names match design export XML; historical unhyphenated aliases kept for dual-engine.
      PSXmlSerializationHelper.addType("state", PSState.class);
      PSXmlSerializationHelper.addType("role", PSWorkflowRole.class);
      PSXmlSerializationHelper.addType("notification-def", PSNotificationDef.class);
      PSXmlSerializationHelper.addType("notificationdef", PSNotificationDef.class);
   }
}
