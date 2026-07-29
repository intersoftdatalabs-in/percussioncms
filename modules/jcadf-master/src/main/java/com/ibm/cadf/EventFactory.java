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

package com.ibm.cadf;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.model.CADFType;
import com.ibm.cadf.model.CADFType.EVENTTYPE;
import com.ibm.cadf.model.Event;
import com.ibm.cadf.model.Resource;

/**
 * Static factory that builds a fully-populated {@link Event} from the strings consumed at the
 * middleware boundary. Centralizes event-type validation so callers cannot construct an event with
 * an unknown CADF type URI.
 */
public class EventFactory {

  /** Default no-argument constructor for {@link EventFactory}. */
  public EventFactory() {}

  /** Error message raised when {@link #getEventInstance} receives an unknown event-type tag. */
  public static String ERROR_UNKNOWN_EVENTTYPE =
      "Unknown CADF EventType requested on factory method";

  /**
   * Constructs a new CADF {@link Event} from the supplied components after validating that {@code
   * eventType} matches a known {@link CADFType.EVENTTYPE} tag.
   *
   * @param eventType the event type URI; must match one of {@link CADFType.EVENTTYPE}.
   * @param id the unique event id, may be {@code null} when one will be assigned elsewhere.
   * @param action the action label.
   * @param outcome the outcome label.
   * @param initiator the initiator resource, may be {@code null}.
   * @param initiatorId alternate initiator id, may be {@code null}.
   * @param target the target resource, may be {@code null}.
   * @param targetId alternate target id, may be {@code null}.
   * @param observer the observer resource, may be {@code null}.
   * @param observerId alternate observer id, may be {@code null}.
   * @return the constructed event, never {@code null}.
   * @throws CADFException when {@code eventType} is not a known CADF type.
   */
  public static Event getEventInstance(
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

    if (!CADFType.isValidEventType(eventType)) throw new CADFException(ERROR_UNKNOWN_EVENTTYPE);

    EVENTTYPE eventTypeEnum = CADFType.EVENTTYPE.valueOf(eventType);
    return new Event(
        eventTypeEnum.value,
        id,
        action,
        outcome,
        initiator,
        initiatorId,
        target,
        targetId,
        observer,
        observerId);
  }
}
