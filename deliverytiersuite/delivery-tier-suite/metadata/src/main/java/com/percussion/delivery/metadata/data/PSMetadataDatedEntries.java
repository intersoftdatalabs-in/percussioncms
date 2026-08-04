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
package com.percussion.delivery.metadata.data;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the structure of the object returned by the Rest method. The object is
 * composed of a list of {@link PSMetadataDatedEvent}.
 *
 * @author rafaelsalis
 */
public class PSMetadataDatedEntries {
  private List<PSMetadataDatedEvent> events;

  /**
   * No-arg constructor required by the JSON binding layer. Initialises the events list so that
   * {@link #add(PSMetadataDatedEvent)} can be called immediately.
   */
  public PSMetadataDatedEntries() {
    events = new ArrayList<>();
  }

  /**
   * Add an event to the entries list.
   *
   * @param event a {@link PSMetadataDatedEvent} object.
   */
  public void add(PSMetadataDatedEvent event) {
    if (event.getTitle() != null) events.add(event);
  }

  /**
   * Returns the list of events currently held by this instance.
   *
   * @return the events list, may be empty but never {@code null}.
   */
  public List<PSMetadataDatedEvent> getEvents() {
    return events;
  }

  /**
   * Replaces the list of events.
   *
   * @param events the events to set; may be {@code null}.
   */
  public void setEvents(List<PSMetadataDatedEvent> events) {
    this.events = events;
  }
}
