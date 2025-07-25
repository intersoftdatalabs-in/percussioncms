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
package com.percussion.delivery.integrations.ems.model;

import com.percussion.delivery.integrations.ems.IPSEMSEventService;
import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Represents a building entry for EMS integration.
 * <p>Refactored to use Java 11 features and java.time API.</p>
 *
 * @author natechadwick, refactored by Sunny Sal
 */
@XmlRootElement(name = "PSXEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class Building {
    private static final Logger log = LogManager.getLogger(Building.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(IPSEMSEventService.TIME_FORMAT_STRING.replace("HH:mm:ss", "HH:mm:ss"));

    private Integer id;
    private String buildingCode;
    @XmlElement(name = "PSXDisplayText")
    private String description;
    private String timeZoneAbbreviation;
    private String timeZoneDescription;
    private LocalDateTime currentLocalTime;

    private static Optional<LocalDateTime> parseDateTime(String value, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(value.replace("T", " ").trim(), formatter));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date/time: {} with format: {}, Error: {}", value, formatter, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Optional.empty();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeZoneAbbreviation() {
        return timeZoneAbbreviation;
    }

    public void setTimeZoneAbbreviation(String timeZoneAbbreviation) {
        this.timeZoneAbbreviation = timeZoneAbbreviation;
    }

    public String getTimeZoneDescription() {
        return timeZoneDescription;
    }

    public void setTimeZoneDescription(String timeZoneDescription) {
        this.timeZoneDescription = timeZoneDescription;
    }

    public Optional<LocalDateTime> getCurrentLocalTime() {
        return Optional.ofNullable(currentLocalTime);
    }

    public void setCurrentLocalTime(String currentLocalTime) {
        this.currentLocalTime = parseDateTime(currentLocalTime, DATE_TIME_FORMAT).orElse(null);
    }
}
