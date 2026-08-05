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
package com.percussion.services.content.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.io.IOException;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single item status at the time it was prepared for edit. This object may
 * be used to release the item later.
 *
 * <p>Design-object XML root is {@code item-status}. Content id is wired as {@code id} (historical
 * Betwixt property name) (issue #1921 / epic #505).
 */
@JacksonXmlRootElement(localName = "item-status")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "didCheckout",
  "didTransition",
  "fromState",
  "fromStateId",
  "id",
  "toState",
  "toStateId"
})
public class PSItemStatus {
  /** The content id. */
  private int contentId;

  /**
   * See {@link #isDidCheckout()} for it description. Default to <code>false</code>.
   */
  private boolean didCheckout = false;

  /**
   * See {@link #isDidTransition()} for its description. Default to <code>false</code>.
   */
  private boolean didTransition = false;

  /**
   * See {@link #getFromState()} for its description. Default to <code>null</code>.
   */
  private String fromState = null;

  /**
   * See {@link #getFromStateId()} for its description. Default to <code>null</code>.
   */
  private Long fromStateId = null;

  /**
   * See {@link #getToState()} for its description. Default to <code>null</code>.
   */
  private String toState = null;

  /**
   * See {@link #getToStateId()} for its description. Default to <code>null</code>.
   */
  private Long toStateId = null;

  /**
   * Default constructor required for XML serialization frameworks (Jackson / BeanUtils property
   * copy).
   */
  public PSItemStatus() {}

  /**
   * Creates an instance with the specified id of a item.
   *
   * @param contentId the id of the item.
   */
  public PSItemStatus(int contentId) {
    this.contentId = contentId;
  }

  /**
   * Creates an instance from the specified parameters.
   *
   * @param contentId the id of the item, may not be <code>null</code>.
   * @param didCheckout <code>true</code> if the item was, <code>false</code> otherwise. see {@link
   *     #isDidCheckout()} for more info.
   * @param didTransition <code>true</code> if the item was, <code>false</code> otherwise. see {@link
   *     #isDidTransition()} for more info.
   * @param fromStateId the id of the from-state, see {@link #getFromStateId()} for more info. It
   *     may be <code>null</code>.
   * @param fromState the name of the from-state, see {@link #getFromState()} for more info. It may
   *     be <code>null</code> or empty.
   * @param toStateId the id of the to-state, see {@link #getToStateId()} for more info. It may be
   *     <code>null</code>.
   * @param toState the name of the to-state, see {@link #getToState()} for more info. It may be
   *     <code>null</code> or empty.
   */
  public PSItemStatus(
      int contentId,
      boolean didCheckout,
      boolean didTransition,
      Long fromStateId,
      String fromState,
      Long toStateId,
      String toState) {
    this(contentId);
    this.didCheckout = didCheckout;
    this.didTransition = didTransition;
    this.fromStateId = fromStateId;
    this.fromState = fromState;
    this.toStateId = toStateId;
    this.toState = toState;
  }

  /**
   * @return the id of the item.
   */
  @JsonProperty("id")
  public int getId() {
    return contentId;
  }

  /**
   * Set the content id (design-object XML property {@code id}).
   *
   * @param id the content id
   */
  public void setId(int id) {
    this.contentId = id;
  }

  /**
   * Was the item checked out to prepare it for edit?
   *
   * @return <code>true</code> if it was, <code>false</code> otherwise.
   */
  @JsonProperty
  public boolean isDidCheckout() {
    return didCheckout;
  }

  /**
   * Set if the item was checked out to prepare it for edit.
   *
   * @param didCheckout <code>true</code> if it was, <code>false</code> otherwise.
   */
  public void setDidCheckout(boolean didCheckout) {
    this.didCheckout = didCheckout;
  }

  /**
   * Was the item transitioned to prepare it for edit?
   *
   * @return <code>true</code> if it was, <code>false</code> otherwise.
   */
  @JsonProperty
  public boolean isDidTransition() {
    return didTransition;
  }

  /**
   * Set if the item ws transitioned to prepare it for edit.
   *
   * @param didTransition <code>true</code> if it was, <code>false</code> otherwise.
   */
  public void setDidTransition(boolean didTransition) {
    this.didTransition = didTransition;
  }

  /**
   * Get the name of the workflow state in which the item was before it was prepared for edit. This
   * is only available if the item had to be transitioned to bring it into edit mode.
   *
   * @return the item state before it was prepared for edit, may be <code>null</code>, never empty.
   */
  @JsonProperty
  public String getFromState() {
    return fromState;
  }

  /**
   * Set the name of the workflow state in which the item was before it was prepared for edit.
   *
   * @param fromState the new item state before it was prepared for edit, may be <code>null</code>,
   *     not empty.
   */
  public void setFromState(String fromState) {
    if (fromState != null && StringUtils.isEmpty(fromState))
      throw new IllegalArgumentException("fromState cannot be empty");

    this.fromState = fromState;
  }

  /**
   * Get the name of the workflow state to which the item was transitioned to prepare it for edit.
   * This is only available if the item had to be transitioned to bring it into edit mode.
   *
   * @return the item state to which the it was transitioned to prepared it for edit, may be <code>
   *     null</code>, never empty.
   */
  @JsonProperty
  public String getToState() {
    return toState;
  }

  /**
   * Set the name of the workflow state to which the item was transitioned to prepare it for edit.
   *
   * @param toState the new item state to which the it was transitioned to prepared it for edit, may
   *     be <code>null</code>, not empty.
   */
  public void setToState(String toState) {
    if (toState != null && StringUtils.isEmpty(toState))
      throw new IllegalArgumentException("toState cannot be empty");

    this.toState = toState;
  }

  /**
   * Get the id of the workflow state in which the item was before it was prepared for edit. This is
   * only available if the item had to be transitioned to bring it into edit mode.
   *
   * @return the state id described above, may be <code>null</code>.
   */
  @JsonProperty
  public Long getFromStateId() {
    return fromStateId;
  }

  /**
   * Set the id of the workflow state in which the item was before it was prepared for edit.
   *
   * @param fromStateId the id of new item state before it was prepared for edit, may be <code>null
   *     </code>.
   */
  public void setFromStateId(Long fromStateId) {
    this.fromStateId = fromStateId;
  }

  /**
   * Get the id of the workflow state to which the item was transitioned to prepare it for edit.
   * This is only available if the item had to be transitioned to bring it into edit mode.
   *
   * @return the state id to which the item was transitioned to prepared it for edit, may be <code>
   *     null</code>.
   */
  @JsonProperty
  public Long getToStateId() {
    return toStateId;
  }

  /**
   * Set the id of the workflow state to which the item was transitioned to prepare it for edit.
   *
   * @param toStateId the new state id to which the item was transitioned to prepared it for edit,
   *     may be <code>null</code>.
   */
  public void setToStateId(Long toStateId) {
    this.toStateId = toStateId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSItemStatus)) return false;
    PSItemStatus that = (PSItemStatus) o;
    return contentId == that.contentId
        && isDidCheckout() == that.isDidCheckout()
        && isDidTransition() == that.isDidTransition()
        && Objects.equals(getFromState(), that.getFromState())
        && Objects.equals(getFromStateId(), that.getFromStateId())
        && Objects.equals(getToState(), that.getToState())
        && Objects.equals(getToStateId(), that.getToStateId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        contentId,
        isDidCheckout(),
        isDidTransition(),
        getFromState(),
        getFromStateId(),
        getToState(),
        getToStateId());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSItemStatus{");
    sb.append("contentId=").append(contentId);
    sb.append(", didCheckout=").append(didCheckout);
    sb.append(", didTransition=").append(didTransition);
    sb.append(", fromState='").append(fromState).append('\'');
    sb.append(", fromStateId=").append(fromStateId);
    sb.append(", toState='").append(toState).append('\'');
    sb.append(", toStateId=").append(toStateId);
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
