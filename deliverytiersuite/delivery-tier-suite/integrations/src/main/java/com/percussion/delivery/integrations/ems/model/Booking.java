        this.bookingDate = parseDateTime(bookingDate, DATE_FORMAT).orElse(null);
    }
    public Optional<LocalDateTime> getStartBookingDate() {
        return Optional.ofNullable(startBookingDate);
    }
    public void setStartBookingDate(String startBookingDate) {
        this.startBookingDate = parseDateTime(startBookingDate, DATE_FORMAT).orElse(null);
    }
    public String getRoomDescription() {
        return roomDescription;
    }
    public void setRoomDescription(String roomDescription) {
        this.roomDescription = roomDescription;
    }
    public Optional<LocalDateTime> getTimeEventStart() {
        return Optional.ofNullable(timeEventStart);
    }
    public void setTimeEventStart(String timeEventStart) {
        this.timeEventStart = parseDateTime(timeEventStart, TIME_FORMAT).orElse(null);
    }
    public Optional<LocalDateTime> getTimeEventEnd() {
        return Optional.ofNullable(timeEventEnd);
    }
    public void setTimeEventEnd(String timeEventEnd) {
        this.timeEventEnd = parseDateTime(timeEventEnd, TIME_FORMAT).orElse(null);
    }
    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public String getEventName() {
        return eventName;
    }
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    public String getSetupTypeDescription() {
        return setupTypeDescription;
    }
    public void setSetupTypeDescription(String setupTypeDescription) {
        this.setupTypeDescription = setupTypeDescription;
    }
    public int getSetupCount() {
        return setupCount;
    }
    public void setSetupCount(int setupCount) {
        this.setupCount = setupCount;
    }
    public int getReservationID() {
        return reservationID;
    }
    public void setReservationID(int reservationID) {
        this.reservationID = reservationID;
    }
    public int getGroupID() {
        return groupID;
    }
    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }
    public String getVip() {
        return vip;
    }
    public void setVip(String vip) {
        this.vip = vip;
    }
    public boolean isVipEvent() {
        return vipEvent;
    }
    public void setVipEvent(boolean vipEvent) {
        this.vipEvent = vipEvent;
    }
    public boolean isClosedAllDay() {
        return closedAllDay;
    }
    public void setClosedAllDay(boolean closedAllDay) {
        this.closedAllDay = closedAllDay;
    }
    public Optional<LocalDateTime> getOpenTime() {
        return Optional.ofNullable(openTime);
    }
    public void setOpenTime(String openTime) {
        this.openTime = parseDateTime(openTime, TIME_FORMAT).orElse(null);
    }
    public Optional<LocalDateTime> getCloseTime() {
        return Optional.ofNullable(closeTime);
    }
    public void setCloseTime(String closeTime) {
        this.closeTime = parseDateTime(closeTime, TIME_FORMAT).orElse(null);
    }
    public String getGroupTypeDescription() {
        return groupTypeDescription;
    }
    public void setGroupTypeDescription(String groupTypeDescription) {
        this.groupTypeDescription = groupTypeDescription;
    }
    public String getEventTypeDescription() {
        return eventTypeDescription;
    }
    public void setEventTypeDescription(String eventTypeDescription) {
        this.eventTypeDescription = eventTypeDescription;
    }
    public String getContact() {
        return contact;
    }
    public void setContact(String contact) {
        this.contact = contact;
    }
    public String getAltContact() {
        return altContact;
    }
    public void setAltContact(String altContact) {
        this.altContact = altContact;
    }
    public int getBookingId() {
        return bookingId;
    }
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
    public Optional<LocalDateTime> getTimeBookingStart() {
        return Optional.ofNullable(timeBookingStart);
    }
    public void setTimeBookingStart(String timeBookingStart) {
        this.timeBookingStart = parseDateTime(timeBookingStart, TIME_FORMAT).orElse(null);
    }
    public Optional<LocalDateTime> getTimeBookingEnd() {
        return Optional.ofNullable(timeBookingEnd);
    }
    public void setTimeBookingEnd(String timeBookingEnd) {
        this.timeBookingEnd = parseDateTime(timeBookingEnd, TIME_FORMAT).orElse(null);
    }
    public Optional<LocalDateTime> getGmtStartTime() {
        return Optional.ofNullable(gmtStartTime);
    }
    public void setGmtStartTime(String gmtStartTime) {
        this.gmtStartTime = parseDateTime(gmtStartTime, TIME_FORMAT).orElse(null);
    }
    public Optional<LocalDateTime> getGmtEndTime() {
        return Optional.ofNullable(gmtEndTime);
    }
    public void setGmtEndTime(String gmtEndTime) {
        this.gmtEndTime = parseDateTime(gmtEndTime, TIME_FORMAT).orElse(null);
    }
    public String getTimeZone() {
        return timeZone;
    }
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    public String getBuildingCode() {
        return buildingCode;
    }
    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }
    public String getBuilding() {
        return building;
    }
    public void setBuilding(String building) {
        this.building = building;
    }
    public String getRoomCode() {
        return roomCode;
    }
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
    public String getRoom() {
        return room;
    }
    public void setRoom(String room) {
        this.room = room;
    }
    public int getRoomId() {
        return roomId;
    }
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
    public int getBuildingId() {
        return buildingId;
    }
    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }
    public int getRoomTypeId() {
        return roomTypeId;
    }
    public void setRoomTypeId(int roomTypeId) {
        this.roomTypeId = roomTypeId;
    }
    public String getRoomType() {
        return roomType;
    }
    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }
    public String getHvacZone() {
        return hvacZone;
    }
    public void setHvacZone(String hvacZone) {
        this.hvacZone = hvacZone;
    }
    public int getStatusID() {
        return statusID;
    }
    public void setStatusID(int statusID) {
        this.statusID = statusID;
    }
    public int getStatusTypeId() {
        return statusTypeId;
    }
    public void setStatusTypeId(int statusTypeId) {
        this.statusTypeId = statusTypeId;
    }
    public int getEventTypeId() {
        return eventTypeId;
    }
    public void setEventTypeId(int eventTypeId) {
        this.eventTypeId = eventTypeId;
    }
    public int getGroupTypeId() {
        return groupTypeId;
    }
    public void setGroupTypeId(int groupTypeId) {
        this.groupTypeId = groupTypeId;
    }
    public Optional<LocalDateTime> getDateAdded() {
        return Optional.ofNullable(dateAdded);
    }
    public void setDateAdded(String dateAdded) {
        this.dateAdded = parseDateTime(dateAdded, DATE_FORMAT).orElse(null);
    }
    public String getAddedBy() {
        return addedBy;
    }
    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }
    public Optional<LocalDateTime> getDateChanged() {
        return Optional.ofNullable(dateChanged);
    }
    public void setDateChanged(String dateChanged) {
        this.dateChanged = parseDateTime(dateChanged, DATE_FORMAT).orElse(null);
    }
    public String getChangedBy() {
        return changedBy;
    }
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    public String getContactEmailAddress() {
        return contactEmailAddress;
    }
    public void setContactEmailAddress(String contactEmailAddress) {
        this.contactEmailAddress = contactEmailAddress;
    }
    public boolean isCheckedIn() {
        return checkedIn;
    }
    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }
}
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
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Represents a booking entry for EMS integration.
 * <p>Refactored to use Java 11 features and java.time API.</p>
 * @author natechadwick, refactored by Sunny Sal
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Booking {
    private static final Logger log = LogManager.getLogger(Booking.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(IPSEMSEventService.DATE_FORMAT_STRING.replace("HH:mm:ss", "HH:mm:ss"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(IPSEMSEventService.TIME_FORMAT_STRING.replace("HH:mm:ss", "HH:mm:ss"));

    private LocalDateTime bookingDate;
    private LocalDateTime startBookingDate;
    private String roomDescription;
    private LocalDateTime timeEventStart;
    private LocalDateTime timeEventEnd;
    private String groupName;
    private String eventName;
    private String setupTypeDescription;
    private int setupCount;
    private int reservationID;
    private String eventCoordinator;
    private int groupID;
    private String vip;
    private boolean vipEvent;
    private boolean closedAllDay;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private String groupTypeDescription;
    private String eventTypeDescription;
    private String contact;
    private String altContact;
    private int bookingId;
    private LocalDateTime timeBookingStart;
    private LocalDateTime timeBookingEnd;
    private LocalDateTime gmtStartTime;
    private LocalDateTime gmtEndTime;
    private String timeZone;
    private String buildingCode;
    private String building;
    private String roomCode;
    private String room;
    private int roomId;
    private int buildingId;
    private int roomTypeId;
    private String roomType;
    private String hvacZone;
    private int statusID;
    private int statusTypeId;
    private int eventTypeId;
    private int groupTypeId;
    private LocalDateTime dateAdded;
    private String addedBy;
    private LocalDateTime dateChanged;
    private String changedBy;
    private String contactEmailAddress;
    private boolean checkedIn;

    // Utility for parsing date/time strings
    private static Optional<LocalDateTime> parseDateTime(String value, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(value.replace("T", " ").trim(), formatter));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date/time: {} with format: {}, Error: {}", value, formatter, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Optional.empty();
        }
    }

    public Optional<LocalDateTime> getBookingDate() {
        return Optional.ofNullable(bookingDate);
    }
    public void setBookingDate(String bookingDate) {
