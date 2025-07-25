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

import com.percussion.delivery.integrations.ems.model.Booking;
import com.percussion.delivery.integrations.ems.model.Building;
import com.percussion.delivery.integrations.ems.model.EventType;
import com.percussion.delivery.integrations.ems.model.GroupType;
import java.util.List;

/**
 * EMS Event Service interface for event, building, and group queries.
 * <p>
 * Implementations must be thread-safe and support Java 11 features.
 * </p>
 */
public interface IPSEMSEventService {
    /**
     * Date/time format string for EMS queries (Java 11 standard).
     */
    String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
    String TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
    String DATETIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";

    /**
     * Default cache timeout for remote method calls (in milliseconds).
     */
    int DEFAULT_CACHE_TIMEOUT = 60000;

    /**
     * Returns a list of bookings matching the given query.
     * @param query booking query (must not be null)
     * @return list of bookings (never null)
     */
    List<Booking> getBookings(PSBookingsQuery query);

    /**
     * Returns all available event types.
     * @return list of event types (never null)
     */
    List<EventType> getEventTypes();

    /**
     * Returns all available buildings.
     * @return list of buildings (never null)
     */
    List<Building> getBuildings();

    /**
     * Returns all available group types.
     * @return list of group types (never null)
     */
    List<GroupType> getGroupTypes();
}
