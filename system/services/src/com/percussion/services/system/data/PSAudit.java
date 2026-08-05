/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.system.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single audit.
 *
 * <p>Design-object XML root is {@code audit}. Jackson opt-in property surface (issue #1920 / epic
 * #505). Identity uses scalar {@code id}; derived {@code guid} is omitted to avoid dual-write /
 * setGUID one-shot conflicts on restore.
 */
@JacksonXmlRootElement(localName = "audit")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "actor",
  "currentRevision",
  "editRevision",
  "eventTime",
  "id",
  "publishable",
  "stateId",
  "stateName",
  "transitionComment",
  "transitionId",
  "transitionName"
})
public class PSAudit implements Serializable {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = 2299868760195078824L;

  private long id;

  private boolean publishable;

  private Date eventTime;

  private String actor;

  private long stateId;

  private String stateName;

  private long transitionId;

  private String transitionName;

  private String transitionComment;

  private boolean currentRevision;

  private boolean editRevision;

  /** Default constructor. */
  public PSAudit() {}

  @JsonProperty
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @JsonProperty
  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  @JsonProperty("current-revision")
  public boolean isCurrentRevision() {
    return currentRevision;
  }

  public void setCurrentRevision(boolean currentRevision) {
    this.currentRevision = currentRevision;
  }

  @JsonProperty("edit-revision")
  public boolean isEditRevision() {
    return editRevision;
  }

  public void setEditRevision(boolean editRevision) {
    this.editRevision = editRevision;
  }

  /**
   * Event time on the design-object wire. Pattern matches historical Betwixt {@code
   * PSDateFormatISO8601} ({@code yyyyMMdd'T'HHmmssSSS}); timezone fixed to UTC for golden
   * stability.
   */
  @JsonProperty("event-time")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmssSSS", timezone = "UTC")
  public Date getEventTime() {
    return eventTime;
  }

  public void setEventTime(Date eventTime) {
    this.eventTime = eventTime;
  }

  @JsonProperty
  public boolean isPublishable() {
    return publishable;
  }

  public void setPublishable(boolean publishable) {
    this.publishable = publishable;
  }

  @JsonProperty("state-id")
  public long getStateId() {
    return stateId;
  }

  public void setStateId(long stateId) {
    this.stateId = stateId;
  }

  @JsonProperty("state-name")
  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  @JsonProperty("transition-id")
  public long getTransitionId() {
    return transitionId;
  }

  public void setTransitionId(long transitionId) {
    this.transitionId = transitionId;
  }

  @JsonProperty("transition-name")
  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  @JsonProperty("transition-comment")
  public String getTransitionComment() {
    return transitionComment;
  }

  public void setTransitionComment(String transitionComment) {
    this.transitionComment = transitionComment;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getGUID()
   */
  @JsonIgnore
  public IPSGuid getGUID() {
    return new PSGuid(PSTypeEnum.ITEM_HISTORY, id);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#setGUID(IPSGuid)
   */
  @JsonIgnore
  public void setGUID(IPSGuid newguid) throws IllegalStateException {
    if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

    if (id != 0) throw new IllegalStateException("cannot change existing guid");

    id = newguid.longValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSAudit)) return false;
    PSAudit psAudit = (PSAudit) o;
    return getId() == psAudit.getId()
        && isPublishable() == psAudit.isPublishable()
        && getStateId() == psAudit.getStateId()
        && getTransitionId() == psAudit.getTransitionId()
        && isCurrentRevision() == psAudit.isCurrentRevision()
        && isEditRevision() == psAudit.isEditRevision()
        && Objects.equals(getEventTime(), psAudit.getEventTime())
        && Objects.equals(getActor(), psAudit.getActor())
        && Objects.equals(getStateName(), psAudit.getStateName())
        && Objects.equals(getTransitionName(), psAudit.getTransitionName())
        && Objects.equals(getTransitionComment(), psAudit.getTransitionComment());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        isPublishable(),
        getEventTime(),
        getActor(),
        getStateId(),
        getStateName(),
        getTransitionId(),
        getTransitionName(),
        getTransitionComment(),
        isCurrentRevision(),
        isEditRevision());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSAudit{");
    sb.append("id=").append(id);
    sb.append(", publishable=").append(publishable);
    sb.append(", eventTime=").append(eventTime);
    sb.append(", actor='").append(actor).append('\'');
    sb.append(", stateId=").append(stateId);
    sb.append(", stateName='").append(stateName).append('\'');
    sb.append(", transitionId=").append(transitionId);
    sb.append(", transitionName='").append(transitionName).append('\'');
    sb.append(", transitionComment='").append(transitionComment).append('\'');
    sb.append(", currentRevision=").append(currentRevision);
    sb.append(", editRevision=").append(editRevision);
    sb.append('}');
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#fromXML(String)
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#toXML()
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }
}
