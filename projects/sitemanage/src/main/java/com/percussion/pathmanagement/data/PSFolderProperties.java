// REFACTORED: CP-JAVA11
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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Contains all properties of a given folder.
 *
 * @author yubingchen
 */
@XmlRootElement(name = "FolderProperties")
@JsonRootName("FolderProperties")
public class PSFolderProperties extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  /**
   * Runtime/CMS object-store ACL. Not Java-serializable through {@link PSObjectAcl}; JAXB/Jackson
   * still use the accessor pair for wire formats.
   */
  private transient PSObjectAcl acl;
  private String locale = PSI18nUtils.DEFAULT_LANG;
  private String communityName;
  private int communityId;
  private String displayFormatName;
  private String id;
  private String name;
  private PSFolderPermission permission;
  private int workflowId;

  /**
   * Comma-separated list of allowed sites for publishing assets. If null, assets are published to
   * all sites by default.
   */
  private String allowedSites;

  public PSObjectAcl getAcl() {
    return acl;
  }

  public void setAcl(PSObjectAcl acl) {
    this.acl = acl;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getCommunityName() {
    return communityName;
  }

  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public String getDisplayFormatName() {
    return displayFormatName;
  }

  public void setDisplayFormatName(String displayFormatName) {
    this.displayFormatName = displayFormatName;
  }

  @XmlElement
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(int workflowId) {
    this.workflowId = workflowId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PSFolderPermission getPermission() {
    return permission;
  }

  public void setPermission(PSFolderPermission permission) {
    this.permission = permission;
  }

  public String getAllowedSites() {
    return allowedSites;
  }

  public void setAllowedSites(String allowedSites) {
    this.allowedSites = allowedSites;
  }
}
