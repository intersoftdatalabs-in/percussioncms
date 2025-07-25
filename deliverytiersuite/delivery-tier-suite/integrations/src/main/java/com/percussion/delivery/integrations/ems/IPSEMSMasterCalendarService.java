/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11
package com.percussion.delivery.integrations.ems;

import java.util.List;
import com.percussion.delivery.integrations.ems.model.MCCalendar;
import com.percussion.delivery.integrations.ems.model.MCEventDetail;
import com.percussion.delivery.integrations.ems.model.MCEventType;
import com.percussion.delivery.integrations.ems.model.MCLocation;

/**
 * EMS Master Calendar Service interface for event, calendar, and location queries.
 * <p>
 * Implementations must be thread-safe and support Java 11 features.
 * </p>
 */
public interface IPSEMSMasterCalendarService {
    /**
     * Returns a list of master calendar events matching the given query.
     * @param query event query (must not be null)
     * @return list of event details (never null)
     */
    List<MCEventDetail> getMasterCalendarEvents(PSEventQuery query);

    /**
     * Returns a list of featured master calendar events matching the given query.
     * @param query featured events query (must not be null)
     * @return list of event details (never null)
     */
    List<MCEventDetail> getMasterCalendarFeaturedEvents(PSFeaturedEventsQuery query);

    /**
     * Returns all available master calendar event types.
     * @return list of event types (never null)
     */
    List<MCEventType> getMasterCalendarEventTypes();

    /**
     * Returns all available master calendar locations.
     * @return list of locations (never null)
     */
    List<MCLocation> getMasterCalendarLocations();

    /**
     * Returns all available master calendars.
     * @return list of calendars (never null)
     */
    List<MCCalendar> getMasterCalendarCalendars();
}
