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
package com.percussion.pso.restservice.model;

import jakarta.xml.bind.annotation.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of package objects.
 *
 * @author sbolton
 */
@XmlRootElement(name = "Item")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(
    name = "",
    propOrder = {
      "folders",
      "folderInfo",
      "fields",
      "children",
      "relationships",
      "depRelationships",
      "errors"
    })
/**
 * Item class.
 */
public class Item {

  private String forceCheckin; // never (default), always, user;
  private String title;
  private String contentType;
  private String communityName;
  private String state;
  private String workflow;
  private String locale;
  private String updateType;
  private String checkoutUserName;
  private Boolean checkInOnly;
  private String updatedDateField;
  private String keyField;
  private String contextRoot;
  private String transition;
  private String editTransition;
  private Relationships relationships;
  private Relationships depRelationships;
  private Integer revision;
  private Integer contentId;
  private String ETag;
  private String lastModified;
  private List<Field> fields;

  private List<Child> children = null;
  private List<Error> errors = null;

  private List<String> folders = null;
  private FolderInfo folderInfo;

  /**
   * Returns the update type.
   *
   * @return the result
   */
  @XmlAttribute
  public String getUpdateType() {
    return updateType;
  }

  /**
   * Sets the update type.
   *
   * @param updateType the update type
   */
  public void setUpdateType(String updateType) {
    this.updateType = updateType;
  }

  /**
   * Returns the key field.
   *
   * @return the result
   */
  @XmlAttribute
  public String getKeyField() {
    return keyField;
  }

  /**
   * Sets the key field.
   *
   * @param keyField the key field
   */
  public void setKeyField(String keyField) {
    this.keyField = keyField;
  }

  /**
   * Returns the context root.
   *
   * @return the result
   */
  @XmlAttribute
  public String getContextRoot() {
    return contextRoot;
  }

  /**
   * Sets the context root.
   *
   * @param contextRoot the context root
   */
  public void setContextRoot(String contextRoot) {
    this.contextRoot = contextRoot;
  }

  /**
   * Returns the content type.
   *
   * @return the result
   */
  @XmlAttribute
  public String getContentType() {
    return contentType;
  }

  /**
   * Sets the content type.
   *
   * @param contentType the content type
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Returns the community name.
   *
   * @return the result
   */
  @XmlAttribute
  public String getCommunityName() {
    return communityName;
  }

  /**
   * Sets the community name.
   *
   * @param communityName the community name
   */
  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  /**
   * Returns the state.
   *
   * @return the result
   */
  @XmlAttribute
  public String getState() {
    return state;
  }

  /**
   * Sets the state.
   *
   * @param state the state
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Returns the workflow.
   *
   * @return the result
   */
  @XmlAttribute
  public String getWorkflow() {
    return workflow;
  }

  /**
   * Sets the workflow.
   *
   * @param workflow the workflow
   */
  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  /**
   * Returns the checkout user name.
   *
   * @return the result
   */
  @XmlAttribute
  public String getCheckoutUserName() {
    return checkoutUserName;
  }

  /**
   * Sets the checkout user name.
   *
   * @param checkoutUserName the checkout user name
   */
  public void setCheckoutUserName(String checkoutUserName) {
    this.checkoutUserName = checkoutUserName;
  }

  /**
   * Returns the revision.
   *
   * @return the result
   */
  @XmlAttribute
  public Integer getRevision() {
    return revision;
  }

  /**
   * Sets the revision.
   *
   * @param revision the revision
   */
  public void setRevision(Integer revision) {
    this.revision = revision;
  }

  /**
   * Returns the content id.
   *
   * @return the result
   */
  @XmlAttribute
  public Integer getContentId() {
    return contentId;
  }

  /**
   * Sets the content id.
   *
   * @param contentId the content id
   */
  public void setContentId(Integer contentId) {
    this.contentId = contentId;
  }

  /**
   * Sets the locale.
   *
   * @param locale the locale
   */
  public void setLocale(String locale) {
    this.locale = locale;
  }

  /**
   * Returns the locale.
   *
   * @return the result
   */
  @XmlAttribute
  public String getLocale() {
    return locale;
  }

  /**
   * Creates a new Item.
   */
  public Item() {
    super();
  }

  /**
   * Returns the fields.
   *
   * @return the result
   */
  @XmlElement(name = "Field")
  @XmlElementWrapper(name = "Fields")
  public List<Field> getFields() {
    return this.fields;
  }

  /**
   * Sets the fields.
   *
   * @param fields the fields
   */
  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  /**
   * Returns the children.
   *
   * @return the result
   */
  @XmlElement(name = "Child")
  @XmlElementWrapper(name = "Children")
  public List<Child> getChildren() {
    return children;
  }

  /**
   * Sets the children.
   *
   * @param children the children
   */
  public void setChildren(List<Child> children) {
    this.children = children;
  }

  /**
   * Sets the folders.
   *
   * @param folders the folders
   */
  public void setFolders(List<String> folders) {
    this.folders = folders;
  }

  /**
   * Returns the folders.
   *
   * @return the result
   */
  @XmlElement(name = "Path")
  @XmlElementWrapper(name = "Folders")
  public List<String> getFolders() {
    return folders;
  }

  /**
   * Sets the relationships.
   *
   * @param relationships the relationships
   */
  public void setRelationships(Relationships relationships) {
    this.relationships = relationships;
  }

  /**
   * Returns the relationships.
   *
   * @return the result
   */
  @XmlElement(name = "Relationships")
  public Relationships getRelationships() {
    return relationships;
  }

  /**
   * Sets the dep relationships.
   *
   * @param relationships the relationships
   */
  public void setDepRelationships(Relationships relationships) {
    this.depRelationships = relationships;
  }

  /**
   * Returns the dep relationships.
   *
   * @return the result
   */
  @XmlElement(name = "DepRelationships")
  public Relationships getDepRelationships() {
    return depRelationships;
  }

  /**
   * Returns the errors.
   *
   * @return the result
   */
  @XmlElement(name = "Error")
  @XmlElementWrapper(name = "Errors")
  public List<Error> getErrors() {
    return errors;
  }

  /**
   * Sets the errors.
   *
   * @param errors the errors
   */
  public void setErrors(List<Error> errors) {
    this.errors = errors;
  }

  /**
   * addError operation.
   *
   * @param error the error
   * @param message the message
   */
  public void addError(Error.ErrorCode error, String message) {
    if (errors == null) errors = new ArrayList<Error>();
    errors.add(new Error(error, message));
  }

  /**
   * addError operation.
   *
   * @param error the error
   * @param e the e
   */
  public void addError(Error.ErrorCode error, Exception e) {
    if (errors == null) errors = new ArrayList<Error>();
    String message = e.getMessage();
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    errors.add(new Error(error, contentId, message + ":" + sw.toString()));
  }

  /**
   * addError operation.
   *
   * @param error the error
   * @param message the message
   * @param e the e
   */
  public void addError(Error.ErrorCode error, String message, Exception e) {
    if (errors == null) errors = new ArrayList<Error>();
    String messageex = e.getMessage() + "\n";
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    errors.add(new Error(error, contentId, messageex + "\n" + message + "\n" + sw.toString()));
  }

  /**
   * addError operation.
   *
   * @param error the error
   */
  public void addError(Error.ErrorCode error) {
    if (errors == null) errors = new ArrayList<Error>();
    errors.add(new Error(error, contentId, ""));
  }

  /**
   * hasError operation.
   *
   * @param error the error
   * @return the result
   */
  public boolean hasError(Error.ErrorCode error) {
    if (errors != null) {
      for (Error errorTest : errors) {
        if (errorTest.getErrorCode() == error) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Sets the folder info.
   *
   * @param folderInfo the folder info
   */
  @XmlElement
  public void setFolderInfo(FolderInfo folderInfo) {
    this.folderInfo = folderInfo;
  }

  /**
   * Returns the folder info.
   *
   * @return the result
   */
  public FolderInfo getFolderInfo() {
    return folderInfo;
  }

  /**
   * Sets the title.
   *
   * @param title the title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the title.
   *
   * @return the result
   */
  @XmlAttribute
  public String getTitle() {
    return title;
  }

  /**
   * Sets the force checkin.
   *
   * @param forceCheckin the force checkin
   */
  public void setForceCheckin(String forceCheckin) {
    this.forceCheckin = forceCheckin;
  }

  /**
   * Returns the force checkin.
   *
   * @return the result
   */
  @XmlAttribute
  public String getForceCheckin() {
    return forceCheckin;
  }

  /**
   * Sets the key.
   * @param key the key
   * @see java.util.HashMap#containsKey(Object)
   * @return the result
   */
  public boolean containsField(String key) {
    if (fields != null) {
      for (Field field : fields) {
        if (field.getName() != null && field.getName().equals(key)) return true;
      }
    }
    return false;
  }

  /**
   * Sets the newField.
   * @param newField the new field
   * @see java.util.HashMap#put(Object, Object)
   */
  @XmlTransient
  public void setField(Field newField) {
    boolean fieldFound = false;
    for (Field field : fields) {
      if (field.getName().equals(newField.getName())) {
        field = newField;
        fieldFound = true;
      }
      if (!fieldFound) {
        fields.add(field);
      }
    }
  }

  /**
   * Sets the name.
   * @param name the name
   * @see java.util.HashMap#get(Object)
   * @return the result
   */
  public Field getField(String name) {
    if (fields != null) {
      for (Field field : fields) {
        if (field.getName() != null && field.getName().equals(name)) return field;
      }
    }
    return null;
  }

  /**
   * Sets the check in only.
   *
   * @param checkInOnly the check in only
   */
  @XmlAttribute
  public void setCheckInOnly(Boolean checkInOnly) {
    this.checkInOnly = checkInOnly;
  }

  /**
   * Returns the check in only.
   *
   * @return the result
   */
  public Boolean getCheckInOnly() {
    return checkInOnly;
  }

  /**
   * Sets the updatedDateField.
   * @param updatedDateField the updatedDateField to set
   */
  @XmlAttribute
  public void setUpdatedDateField(String updatedDateField) {
    this.updatedDateField = updatedDateField;
  }

  /**
   * Returns the updatedDateField.
   * @return the updatedDateField
   */
  public String getUpdatedDateField() {
    return updatedDateField;
  }

  /**
   * Sets the transition.
   *
   * @param transition the transition
   */
  @XmlAttribute
  public void setTransition(String transition) {
    this.transition = transition;
  }

  /**
   * Returns the transition.
   *
   * @return the result
   */
  public String getTransition() {
    return transition;
  }

  /**
   * Sets the edit transition.
   *
   * @param editTransition the edit transition
   */
  @XmlAttribute
  public void setEditTransition(String editTransition) {
    this.editTransition = editTransition;
  }

  /**
   * Returns the edit transition.
   *
   * @return the result
   */
  public String getEditTransition() {
    return editTransition;
  }
}
