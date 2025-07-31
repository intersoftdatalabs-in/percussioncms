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

package service.web.api.ems.dea;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Request for fetching available rooms (version 4).
 * Sunny Sal: "Room availability, Java 11 style!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "buildingID",
    "roomTypeID",
    "floorID",
    "webProcessTemplates",
    "setupTypeId",
    "requestedCapacity",
    "startDateTime",
    "endDateTime"
})
@XmlRootElement(name = "GetRoomsAvailable4")
public class GetRoomsAvailable4 {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "BuildingID")
    protected int buildingID;
    @XmlElement(name = "RoomTypeID")
    protected int roomTypeID;
    @XmlElement(name = "FloorID")
    protected int floorID;
    @XmlElement(name = "WebProcessTemplates")
    protected ArrayOfInt webProcessTemplates;
    @XmlElement(name = "SetupTypeId")
    protected int setupTypeId;
    @XmlElement(name = "RequestedCapacity")
    protected int requestedCapacity;
    @XmlElement(name = "StartDateTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startDateTime;
    @XmlElement(name = "EndDateTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endDateTime;

    /**
     * Gets the user name.
     * @return user name or null
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name.
     * @param value user name
     */
    public void setUserName(String value) {
        userName = value;
    }

    /**
     * Gets the password.
     * @return password or null
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * @param value password
     */
    public void setPassword(String value) {
        password = value;
    }

    /**
     * Gets the building ID.
     * @return building ID
     */
    public int getBuildingID() {
        return buildingID;
    }

    /**
     * Sets the building ID.
     * @param value building ID
     */
    public void setBuildingID(int value) {
        buildingID = value;
    }

    /**
     * Gets the room type ID.
     * @return room type ID
     */
    public int getRoomTypeID() {
        return roomTypeID;
    }

    /**
     * Sets the room type ID.
     * @param value room type ID
     */
    public void setRoomTypeID(int value) {
        roomTypeID = value;
    }

    /**
     * Gets the floor ID.
     * @return floor ID
     */
    public int getFloorID() {
        return floorID;
    }

    /**
     * Sets the floor ID.
     * @param value floor ID
     */
    public void setFloorID(int value) {
        floorID = value;
    }

    /**
     * Gets the web process templates.
     * @return web process templates or null
     */
    public ArrayOfInt getWebProcessTemplates() {
        return webProcessTemplates;
    }

    /**
     * Sets the web process templates.
     * @param value web process templates
     */
    public void setWebProcessTemplates(ArrayOfInt value) {
        webProcessTemplates = value;
    }

    /**
     * Gets the setup type ID.
     * @return setup type ID
     */
    public int getSetupTypeId() {
        return setupTypeId;
    }

    /**
     * Sets the setup type ID.
     * @param value setup type ID
     */
    public void setSetupTypeId(int value) {
        setupTypeId = value;
    }

    /**
     * Gets the requested capacity.
     * @return requested capacity
     */
    public int getRequestedCapacity() {
        return requestedCapacity;
    }

    /**
     * Sets the requested capacity.
     * @param value requested capacity
     */
    public void setRequestedCapacity(int value) {
        requestedCapacity = value;
    }

    /**
     * Gets the start date/time.
     * @return start date/time
     */
    public XMLGregorianCalendar getStartDateTime() {
        return startDateTime;
    }

    /**
     * Sets the start date/time.
     * @param value start date/time
     */
    public void setStartDateTime(XMLGregorianCalendar value) {
        startDateTime = value;
    }

    /**
     * Gets the end date/time.
     * @return end date/time
     */
    public XMLGregorianCalendar getEndDateTime() {
        return endDateTime;
    }

    /**
     * Sets the end date/time.
     * @param value end date/time
     */
    public void setEndDateTime(XMLGregorianCalendar value) {
        endDateTime = value;
    }
}
