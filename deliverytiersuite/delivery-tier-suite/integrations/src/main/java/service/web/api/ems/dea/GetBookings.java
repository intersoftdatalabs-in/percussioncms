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
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="UserName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="StartDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EndDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="Buildings" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="Statuses" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="EventTypes" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="GroupTypes" type="{http://DEA.EMS.API.Web.Service/}ArrayOfInt" minOccurs="0"/&gt;
 *         &lt;element name="ViewComboRoomComponents" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "userName",
    "password",
    "startDate",
    "endDate",
    "buildings",
    "statuses",
    "eventTypes",
    "groupTypes",
    "viewComboRoomComponents"
})
// REFACTORED: CP-JAVA11
@XmlRootElement(name = "GetBookings")
public class GetBookings {

    @XmlElement(name = "UserName")
    protected String userName;
    @XmlElement(name = "Password")
    protected String password;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endDate;
    @XmlElement(name = "Buildings")
    protected ArrayOfInt buildings;
    @XmlElement(name = "Statuses")
    protected ArrayOfInt statuses;
    @XmlElement(name = "EventTypes")
    protected ArrayOfInt eventTypes;
    @XmlElement(name = "GroupTypes")
    protected ArrayOfInt groupTypes;
    @XmlElement(name = "ViewComboRoomComponents")
    protected boolean viewComboRoomComponents;

    /**
     * Gets the value of the userName property.
     * @return Optional of userName
     */
    public java.util.Optional<String> getUserName() {
        return java.util.Optional.ofNullable(userName);
    }
    /**
     * Sets the value of the userName property.
     * @param value allowed object is {@link String }
     */
    public void setUserName(String value) {
        this.userName = value;
    }
    /**
     * Gets the value of the password property.
     * @return Optional of password
     */
    public java.util.Optional<String> getPassword() {
        return java.util.Optional.ofNullable(password);
    }
    /**
     * Sets the value of the password property.
     * @param value allowed object is {@link String }
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the startDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    /**
     * Sets the value of the startDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setStartDate(XMLGregorianCalendar value) {
        this.startDate = value;
    }

    /**
     * Gets the value of the endDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getEndDate() {
        return endDate;
    }

    /**
     * Sets the value of the endDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setEndDate(XMLGregorianCalendar value) {
        this.endDate = value;
    }

    /**
     * Gets the value of the buildings property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfInt }
     *     
     */
    public ArrayOfInt getBuildings() {
        return buildings;
    }

    /**
     * Sets the value of the buildings property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfInt }
     *     
     */
    public void setBuildings(ArrayOfInt value) {
        this.buildings = value;
    }

    /**
     * Gets the value of the statuses property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfInt }
     *     
     */
    public ArrayOfInt getStatuses() {
        return statuses;
    }

    /**
     * Sets the value of the statuses property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfInt }
     *     
     */
    public void setStatuses(ArrayOfInt value) {
        this.statuses = value;
    }

    /**
     * Gets the value of the eventTypes property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfInt }
     *     
     */
    public ArrayOfInt getEventTypes() {
        return eventTypes;
    }

    /**
     * Sets the value of the eventTypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfInt }
     *     
     */
    public void setEventTypes(ArrayOfInt value) {
        this.eventTypes = value;
    }

    /**
     * Gets the value of the groupTypes property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfInt }
     *     
     */
    public ArrayOfInt getGroupTypes() {
        return groupTypes;
    }

    /**
     * Sets the value of the groupTypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfInt }
     *     
     */
    public void setGroupTypes(ArrayOfInt value) {
        this.groupTypes = value;
    }

    /**
     * Gets the value of the viewComboRoomComponents property.
     * 
     */
    public boolean isViewComboRoomComponents() {
        return viewComboRoomComponents;
    }

    /**
     * Sets the value of the viewComboRoomComponents property.
     * 
     */
    public void setViewComboRoomComponents(boolean value) {
        this.viewComboRoomComponents = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBookings that = (GetBookings) o;
        return viewComboRoomComponents == that.viewComboRoomComponents &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(password, that.password) &&
                java.util.Objects.equals(startDate, that.startDate) &&
                java.util.Objects.equals(endDate, that.endDate) &&
                java.util.Objects.equals(buildings, that.buildings) &&
                java.util.Objects.equals(statuses, that.statuses) &&
                java.util.Objects.equals(eventTypes, that.eventTypes) &&
                java.util.Objects.equals(groupTypes, that.groupTypes);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, startDate, endDate, buildings, statuses, eventTypes, groupTypes, viewComboRoomComponents);
    }
    @Override
    public String toString() {
        return "GetBookings{" +
                "userName='" + userName + '\'' +
                ", password='[PROTECTED]'" +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", buildings=" + buildings +
                ", statuses=" + statuses +
                ", eventTypes=" + eventTypes +
                ", groupTypes=" + groupTypes +
                ", viewComboRoomComponents=" + viewComboRoomComponents +
                '}';
    }

}
