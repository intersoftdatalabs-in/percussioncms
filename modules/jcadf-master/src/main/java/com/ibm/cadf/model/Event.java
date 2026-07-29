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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.util.TimeStampUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code Event} — the central audit record consumed by the audit-log middleware. Carries the
 * event type URI, an opaque id, the action and outcome labels, an event timestamp, and optional
 * initiator / target / observer resources plus reporter steps, measurements, tags, and attachments.
 * Instances are typically built through {@link com.ibm.cadf.EventFactory}; {@link #isValid()}
 * enforces the structural requirements before an event can be persisted.
 */
public class Event extends CADFType {
  private static final long serialVersionUID = 1L;

  private static final String TYPE_URI_EVENT = CADFType.CADF_VERSION_1_0_0 + "event";

  /** The CADF type URI, may be {@code null}. */
  private String typeURI;

  /** The event type, may be {@code null}. */
  private String eventType;

  /** The unique event id, may be {@code null}. */
  private String id;

  /** The event timestamp formatted as ISO-8601, may be {@code null}. */
  private String eventTime;

  /** The action label, may be {@code null}. */
  private String action;

  /** The outcome label, may be {@code null}. */
  private String outcome;

  /** The initiator resource, may be {@code null}. */
  private Resource initiator;

  /** The alternate initiator id, may be {@code null}. */
  private String initiatorId;

  /** The target resource, may be {@code null}. */
  private Resource target;

  /** The alternate target id, may be {@code null}. */
  private String targetId;

  /** The severity label, may be {@code null}. */
  private String severity;

  /** The reason object, may be {@code null}. */
  private Reason reason;

  /** The observer resource, may be {@code null}. */
  private Resource observer;

  /** The alternate observer id, may be {@code null}. */
  private String observerId;

  /** The reporter chain, may be {@code null}. */
  private List<Reporterstep> reportersteps;

  /** The measurements collection, may be {@code null}. */
  private List<Measurement> measurements;

  /** The tags collection, may be {@code null}. */
  private List<String> tags;

  /** The attachments collection, may be {@code null}. */
  private List<Attachment> attachments;

  // Valid cadf:Event record "types"
  /** Enumerates the field names recognized on a serialized CADF event payload. */
  public enum EVENT_KEYNAME {

    // Event.eventType
    /** JSON key naming the event type URI. */
    EVENT_KEYNAME_TYPEURI("typeURI"),
    /** JSON key naming the event type. */
    EVENT_KEYNAME_EVENTTYPE("eventType"),
    /** JSON key naming the unique event id. */
    EVENT_KEYNAME_ID("id"),
    /** JSON key naming the event timestamp. */
    EVENT_KEYNAME_EVENTTIME("eventTime"),
    /** JSON key naming the initiator resource. */
    EVENT_KEYNAME_INITIATOR("initiator"),
    /** JSON key naming the alternate initiator id. */
    EVENT_KEYNAME_INITIATORID("initiatorId"),
    /** JSON key naming the action label. */
    EVENT_KEYNAME_ACTION("action"),
    /** JSON key naming the target resource. */
    EVENT_KEYNAME_TARGET("target"),
    /** JSON key naming the alternate target id. */
    EVENT_KEYNAME_TARGETID("targetId"),
    /** JSON key naming the outcome label. */
    EVENT_KEYNAME_OUTCOME("outcome"),
    /** JSON key naming the reason object. */
    EVENT_KEYNAME_REASON("reason"),
    /** JSON key naming the severity label. */
    EVENT_KEYNAME_SEVERITY("severity"),
    /** JSON key naming the measurements array. */
    EVENT_KEYNAME_MEASUREMENTS("measurements"),
    /** JSON key naming the tags array. */
    EVENT_KEYNAME_TAGS("tags"),
    /** JSON key naming the attachments array. */
    EVENT_KEYNAME_ATTACHMENTS("attachments"),
    /** JSON key naming the observer resource. */
    EVENT_KEYNAME_OBSERVER("observer"),
    /** JSON key naming the alternate observer id. */
    EVENT_KEYNAME_OBSERVERID("observerId"),
    /** JSON key naming the reporter chain array. */
    EVENT_KEYNAME_REPORTERCHAIN("reporterchain");

    String value;

    private EVENT_KEYNAME(String value) {
      this.value = value;
    }
  }

  /** Default no-argument constructor for {@link Event}. */
  public Event() {}

  /**
   * Constructs an event with the supplied components. The {@code typeURI} is automatically set to
   * {@code http://schemas.dmtf.org/cloud/audit/1.0/event} and {@code eventTime} to the current
   * timestamp.
   *
   * @param eventType the CADF event-type tag (e.g., {@link CADFType.EVENTTYPE#EVENTTYPE_ACTIVITY}),
   *     never {@code null}.
   * @param id the unique event id, may be {@code null} when one will be assigned elsewhere.
   * @param action the action label, may be {@code null}.
   * @param outcome the outcome label, may be {@code null}.
   * @param initiator the initiator resource, may be {@code null}.
   * @param initiatorId alternate initiator id, may be {@code null}.
   * @param target the target resource, may be {@code null}.
   * @param targetId alternate target id, may be {@code null}.
   * @param observer the observer resource, may be {@code null}.
   * @param observerId alternate observer id, may be {@code null}.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Event(
      String eventType,
      String id,
      String action,
      String outcome,
      Resource initiator,
      String initiatorId,
      Resource target,
      String targetId,
      Resource observer,
      String observerId)
      throws CADFException {
    super();
    this.typeURI = TYPE_URI_EVENT;
    this.eventType = eventType;
    this.id = id;
    this.action = action;
    this.outcome = outcome;
    this.initiator = initiator;
    this.initiatorId = initiatorId;
    this.target = target;
    this.targetId = targetId;
    this.observer = observer;
    this.observerId = observerId;
    this.eventTime = TimeStampUtils.getCurrentTime();
  }

  /**
   * Appends a {@link Reporterstep} to the reporter chain, creating the chain on first use.
   *
   * @param reporterstep the reporter step to add, never {@code null}.
   */
  public void addReporterstep(Reporterstep reporterstep) {
    if (reportersteps == null) {
      reportersteps = new ArrayList<>();
    }
    reportersteps.add(reporterstep);
  }

  /**
   * Appends a name/value tag to this event. The tag is only stored when both the name and value are
   * non-empty.
   *
   * @param name the tag name (e.g., a JSON property key), never {@code null} or empty.
   * @param value the tag value, never {@code null} or empty.
   * @throws CADFException when name or value is blank.
   */
  public void addTag(String name, String value) {
    if (this.tags == null) {
      this.tags = new ArrayList();
    }

    String tag = this.generate_name_value_tag(name, value);
    if (this.isValidTag(tag)) {
      this.tags.add(tag);
    }
  }

  /**
   * Indicates whether the supplied tag string is non-empty.
   *
   * @param value the tag string to validate, may be {@code null}.
   * @return {@code true} when {@code value} is non-empty.
   */
  public boolean isValidTag(String value) {
    return StringUtils.isNotEmpty(value);
  }

  private String generate_name_value_tag(String name, String value) throws CADFException {
    if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(value)) {
      String tag = name + "?value=" + value;
      return tag;
    } else {
      throw new CADFException("'Invalid name and/or value. Values cannot be Empty or Null");
    }
  }

  /**
   * Appends a {@link Measurement} to the measurements collection, creating it on first use.
   *
   * @param measurement the measurement to add, never {@code null}.
   */
  public void addMeasurement(Measurement measurement) {
    if (measurements == null) {
      measurements = new ArrayList<>();
    }

    measurements.add(measurement);
  }

  /**
   * Returns the measurements attached to this event.
   *
   * @return the measurements, may be {@code null} or empty when none have been added.
   */
  public List<Measurement> getMeasurements() {
    return measurements;
  }

  /**
   * Appends an {@link Attachment} to the attachments collection, creating it on first use.
   *
   * @param attachment the attachment to add, never {@code null}.
   */
  public void addAttachment(Attachment attachment) {
    if (attachments == null) {
      attachments = new ArrayList<>();
    }
    attachments.add(attachment);
  }

  /**
   * Returns the event type.
   *
   * @return the event type, may be {@code null}.
   */
  public String getEventType() {
    return eventType;
  }

  /**
   * Sets the event type.
   *
   * @param eventType the event type, may be {@code null}.
   */
  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  /**
   * Returns the CADF type URI.
   *
   * @return the type URI, may be {@code null}.
   */
  public String getTypeURI() {
    return typeURI;
  }

  /**
   * Sets the CADF type URI.
   *
   * @param typeURI the type URI, may be {@code null}.
   */
  public void setTypeURI(String typeURI) {
    this.typeURI = typeURI;
  }

  /**
   * Returns the unique event id.
   *
   * @return the id, may be {@code null}.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique event id.
   *
   * @param id the id, may be {@code null}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the event timestamp formatted as ISO-8601.
   *
   * @return the timestamp, may be {@code null}.
   */
  public String getEventTime() {
    return eventTime;
  }

  /**
   * Sets the event timestamp.
   *
   * @param eventTime the timestamp, may be {@code null}.
   */
  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }

  /**
   * Returns the action label.
   *
   * @return the action, may be {@code null}.
   */
  public String getAction() {
    return action;
  }

  /**
   * Sets the action label.
   *
   * @param action the action, may be {@code null}.
   */
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * Returns the outcome label.
   *
   * @return the outcome, may be {@code null}.
   */
  public String getOutcome() {
    return outcome;
  }

  /**
   * Sets the outcome label.
   *
   * @param outcome the outcome, may be {@code null}.
   */
  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  /**
   * Returns the initiator resource.
   *
   * @return the initiator, may be {@code null}.
   */
  public Resource getInitiator() {
    return initiator;
  }

  /**
   * Sets the initiator resource.
   *
   * @param initiator the initiator, may be {@code null}.
   */
  public void setInitiator(Resource initiator) {
    this.initiator = initiator;
  }

  /**
   * Returns the alternate initiator id (used when no full {@link Resource} is attached).
   *
   * @return the initiator id, may be {@code null}.
   */
  public String getInitiatorId() {
    return initiatorId;
  }

  /**
   * Sets the alternate initiator id.
   *
   * @param initiatorId the id, may be {@code null}.
   */
  public void setInitiatorId(String initiatorId) {
    this.initiatorId = initiatorId;
  }

  /**
   * Returns the target resource.
   *
   * @return the target, may be {@code null}.
   */
  public Resource getTarget() {
    return target;
  }

  /**
   * Sets the target resource.
   *
   * @param target the target, may be {@code null}.
   */
  public void setTarget(Resource target) {
    this.target = target;
  }

  /**
   * Returns the alternate target id (used when no full {@link Resource} is attached).
   *
   * @return the target id, may be {@code null}.
   */
  public String getTargetId() {
    return targetId;
  }

  /**
   * Sets the alternate target id.
   *
   * @param targetId the id, may be {@code null}.
   */
  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  /**
   * Returns the severity label for this event.
   *
   * @return the severity, may be {@code null}.
   */
  public String getSeverity() {
    return severity;
  }

  /**
   * Sets the severity label for this event.
   *
   * @param severity the severity, may be {@code null}.
   */
  public void setSeverity(String severity) {
    this.severity = severity;
  }

  /**
   * Returns the reason attached to this event.
   *
   * @return the reason, may be {@code null}.
   */
  public Reason getReason() {
    return reason;
  }

  /**
   * Sets the reason attached to this event.
   *
   * @param reason the reason, may be {@code null}.
   */
  public void setReason(Reason reason) {
    this.reason = reason;
  }

  /**
   * Returns the observer resource.
   *
   * @return the observer, may be {@code null}.
   */
  public Resource getObserver() {
    return observer;
  }

  /**
   * Sets the observer resource.
   *
   * @param observer the observer, may be {@code null}.
   */
  public void setObserver(Resource observer) {
    this.observer = observer;
  }

  /**
   * Returns the alternate observer id.
   *
   * @return the observer id, may be {@code null}.
   */
  public String getObserverId() {
    return observerId;
  }

  /**
   * Sets the alternate observer id.
   *
   * @param observerId the id, may be {@code null}.
   */
  public void setObserverId(String observerId) {
    this.observerId = observerId;
  }

  /**
   * Validates that the structural CADF fields are populated. Required fields are {@code typeURI},
   * {@code eventType}, {@code id}, {@code eventTime}, {@code action}, {@code outcome}; at least one
   * of {@code (initiator, initiatorId)}, {@code (target, targetId)}, and {@code (observer,
   * observerId)} must be non-null / non-empty.
   *
   * @return {@code true} when every required field is populated.
   */
  @Override
  public boolean isValid() {
    return StringUtils.isNotEmpty(this.typeURI)
        && StringUtils.isNotEmpty(this.eventType)
        && StringUtils.isNotEmpty(this.id)
        && StringUtils.isNotEmpty(this.eventTime)
        && StringUtils.isNotEmpty(this.action)
        && StringUtils.isNotEmpty(this.outcome)
        && (this.initiator != null || StringUtils.isNotEmpty(this.initiatorId))
        && (this.target != null || StringUtils.isNotEmpty(this.targetId))
        && (this.observer != null || StringUtils.isNotEmpty(this.observerId));
  }
}
