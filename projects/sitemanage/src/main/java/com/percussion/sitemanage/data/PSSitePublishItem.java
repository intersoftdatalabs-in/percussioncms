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
package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SitePublishItem")
@JsonRootName("SitePublishItem")
public class PSSitePublishItem extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  @Min(1)
  private long itemStatusId;

  @NotBlank private String status;

  private String fileName;
  private String fileLocation;
  private long folderid;
  private long templateid;
  private String deliveryType;

  @Min(1)
  private long contentid;

  private long revisionid;
  private String assemblyUrl;
  private long elapsedTime;

  @NotBlank private String operation;

  private String errorMessage;

  public long getItemStatusId() {
    return itemStatusId;
  }

  public void setItemStatusId(long itemStatusId) {
    this.itemStatusId = itemStatusId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileLocation() {
    return fileLocation;
  }

  public void setFileLocation(String fileLocation) {
    this.fileLocation = fileLocation;
  }

  public long getContentid() {
    return contentid;
  }

  public void setContentid(long contentid) {
    this.contentid = contentid;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public long getFolderid() {
    return folderid;
  }

  public void setFolderid(long folderid) {
    this.folderid = folderid;
  }

  public long getTemplateid() {
    return templateid;
  }

  public void setTemplateid(long templateid) {
    this.templateid = templateid;
  }

  public String getDeliveryType() {
    return deliveryType;
  }

  public void setDeliveryType(String deliveryType) {
    this.deliveryType = deliveryType;
  }

  public long getRevisionid() {
    return revisionid;
  }

  public void setRevisionid(long revisionid) {
    this.revisionid = revisionid;
  }

  public String getAssemblyUrl() {
    return assemblyUrl;
  }

  public void setAssemblyUrl(String assemblyUrl) {
    this.assemblyUrl = assemblyUrl;
  }
}
