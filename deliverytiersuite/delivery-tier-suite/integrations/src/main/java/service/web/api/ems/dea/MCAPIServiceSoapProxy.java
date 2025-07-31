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

// REFACTORED: CP-JAVA11

/**
 * MCAPIServiceSoapProxy.java
 *
 * Sunny Sal here! This proxy delegates SOAP calls to the MasterCalendar API.
 * Refactored for Java 11 and Google Java Style.
 */
public class MCAPIServiceSoapProxy implements MCAPIServiceSoap {
    private String endpoint;
    private MCAPIServiceSoap mcapiServiceSoap;

    public MCAPIServiceSoapProxy() {
        initMCAPIServiceSoapProxy();
    }

    public MCAPIServiceSoapProxy(String endpoint) {
        this.endpoint = endpoint;
        initMCAPIServiceSoapProxy();
    }

    private void initMCAPIServiceSoapProxy() {
        try {
            mcapiServiceSoap = (new MCAPIServiceLocator()).getMCAPIServiceSoap();
            if (mcapiServiceSoap != null) {
                if (endpoint != null) {
                    ((javax.xml.rpc.Stub) mcapiServiceSoap)._setProperty(
                        "javax.xml.rpc.service.endpoint.address", endpoint);
                } else {
                    endpoint = (String) ((javax.xml.rpc.Stub) mcapiServiceSoap)
                        ._getProperty("javax.xml.rpc.service.endpoint.address");
                }
            }
        } catch (javax.xml.rpc.ServiceException serviceException) {
            // Sunny Sal: "No endpoint, no party!"
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        if (mcapiServiceSoap != null) {
            ((javax.xml.rpc.Stub) mcapiServiceSoap)._setProperty(
                "javax.xml.rpc.service.endpoint.address", endpoint);
        }
    }

    public MCAPIServiceSoap getMCAPIServiceSoap() {
        if (mcapiServiceSoap == null) {
            initMCAPIServiceSoapProxy();
        }
        return mcapiServiceSoap;
    }

    public java.lang.String getEvents(java.lang.String userName, java.lang.String password, java.util.Calendar startDate, java.util.Calendar endDate, java.lang.String eventName, java.lang.String location, int[] calendars, int[] eventTypes, java.lang.String udqAnswer) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getEvents(userName, password, startDate, endDate, eventName, location, calendars, eventTypes, udqAnswer);
    }

    public java.lang.String getFeaturedEvents(java.lang.String userName, java.lang.String password, java.util.Calendar startDate, java.util.Calendar endDate, java.lang.String eventName, java.lang.String location, int[] calendars, int[] eventTypes, java.lang.String udqAnswer) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getFeaturedEvents(userName, password, startDate, endDate, eventName, location, calendars, eventTypes, udqAnswer);
    }

    public java.lang.String getEvent(java.lang.String userName, java.lang.String password, int eventDetailId) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getEvent(userName, password, eventDetailId);
    }

    public java.lang.String getSpecialDates(java.lang.String userName, java.lang.String password, java.util.Calendar startDate, java.util.Calendar endDate, java.lang.String eventName, int[] calendars) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getSpecialDates(userName, password, startDate, endDate, eventName, calendars);
    }

    public java.lang.String getLocations(java.lang.String userName, java.lang.String password) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getLocations(userName, password);
    }

    public java.lang.String getEventTypes(java.lang.String userName, java.lang.String password) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getEventTypes(userName, password);
    }

    public java.lang.String getCalendars(java.lang.String userName, java.lang.String password) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getCalendars(userName, password);
    }

    public java.lang.String getCalendar(java.lang.String userName, java.lang.String password, int calendarId) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getCalendar(userName, password, calendarId);
    }

    public java.lang.String getGroupings(java.lang.String userName, java.lang.String password) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getGroupings(userName, password);
    }

    public java.lang.String getUdqs(java.lang.String userName, java.lang.String password, int eventId) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getUdqs(userName, password, eventId);
    }

    public java.lang.String getComments(java.lang.String userName, java.lang.String password, int eventId, int reservationId, int bookingid) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.getComments(userName, password, eventId, reservationId, bookingid);
    }

    public java.lang.String addEvent(java.lang.String userName, java.lang.String password, java.util.Calendar eventDate, int[] calendars, java.lang.String title, java.lang.String titleUrl, java.lang.String description, java.util.Calendar timeEventStart, java.util.Calendar timeEventEnd, java.lang.String location, java.lang.String locationUrl, java.lang.String contactName, java.lang.String contactEmail, java.lang.String contactPhone, boolean isAllDayEvent, boolean isUntimed, boolean noEndTime, boolean canceled, java.lang.String customFieldLabel1, java.lang.String customFieldDescription1, java.lang.String customFieldUrl1, java.lang.String customFieldLabel2, java.lang.String customFieldDescription2, java.lang.String customFieldUrl2, int eventTypeID, java.lang.String department, boolean hideContactName, boolean hideContactEmail, boolean hideContactPhone) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.addEvent(userName, password, eventDate, calendars, title, titleUrl, description, timeEventStart, timeEventEnd, location, locationUrl, contactName, contactEmail, contactPhone, isAllDayEvent, isUntimed, noEndTime, canceled, customFieldLabel1, customFieldDescription1, customFieldUrl1, customFieldLabel2, customFieldDescription2, customFieldUrl2, eventTypeID, department, hideContactName, hideContactEmail, hideContactPhone);
    }

    public java.lang.String addEventWithMultipleDates(java.lang.String userName, java.lang.String password, java.util.Calendar[] dates, int[] calendars, java.lang.String title, java.lang.String titleUrl, java.lang.String description, java.util.Calendar timeEventStart, java.util.Calendar timeEventEnd, java.lang.String location, java.lang.String locationUrl, java.lang.String contactName, java.lang.String contactEmail, java.lang.String contactPhone, boolean isAllDayEvent, boolean isUntimed, boolean noEndTime, boolean canceled, java.lang.String customFieldLabel1, java.lang.String customFieldDescription1, java.lang.String customFieldUrl1, java.lang.String customFieldLabel2, java.lang.String customFieldDescription2, java.lang.String customFieldUrl2, int eventTypeID, java.lang.String department, boolean hideContactName, boolean hideContactEmail, boolean hideContactPhone) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.addEventWithMultipleDates(userName, password, dates, calendars, title, titleUrl, description, timeEventStart, timeEventEnd, location, locationUrl, contactName, contactEmail, contactPhone, isAllDayEvent, isUntimed, noEndTime, canceled, customFieldLabel1, customFieldDescription1, customFieldUrl1, customFieldLabel2, customFieldDescription2, customFieldUrl2, eventTypeID, department, hideContactName, hideContactEmail, hideContactPhone);
    }

    public java.lang.String updateEvent(java.lang.String userName, java.lang.String password, int eventID, java.util.Calendar eventDate, int[] calendars, java.lang.String title, java.lang.String titleUrl, java.lang.String description, java.util.Calendar timeEventStart, java.util.Calendar timeEventEnd, java.lang.String location, java.lang.String locationUrl, java.lang.String contactName, java.lang.String contactEmail, java.lang.String contactPhone, boolean isAllDayEvent, boolean isUntimed, boolean noEndTime, boolean canceled, java.lang.String customFieldLabel1, java.lang.String customFieldDescription1, java.lang.String customFieldUrl1, java.lang.String customFieldLabel2, java.lang.String customFieldDescription2, java.lang.String customFieldUrl2, int eventTypeID, java.lang.String department, boolean hideContactName, boolean hideContactEmail, boolean hideContactPhone) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.updateEvent(userName, password, eventID, eventDate, calendars, title, titleUrl, description, timeEventStart, timeEventEnd, location, locationUrl, contactName, contactEmail, contactPhone, isAllDayEvent, isUntimed, noEndTime, canceled, customFieldLabel1, customFieldDescription1, customFieldUrl1, customFieldLabel2, customFieldDescription2, customFieldUrl2, eventTypeID, department, hideContactName, hideContactEmail, hideContactPhone);
    }

    public java.lang.String updateEventDate(java.lang.String userName, java.lang.String password, int eventDetailID, java.util.Calendar eventDate, java.lang.String title, java.lang.String titleUrl, java.lang.String description, java.util.Calendar timeEventStart, java.util.Calendar timeEventEnd, java.lang.String location, java.lang.String locationUrl, boolean isAllDayEvent, boolean isUntimed, boolean noEndTime, boolean canceled, java.lang.String customFieldLabel1, java.lang.String customFieldDescription1, java.lang.String customFieldUrl1, java.lang.String customFieldLabel2, java.lang.String customFieldDescription2, java.lang.String customFieldUrl2, int eventTypeID) throws java.rmi.RemoteException{
        if (mcapiServiceSoap == null)
            initMCAPIServiceSoapProxy();
        return mcapiServiceSoap.updateEventDate(userName, password, eventDetailID, eventDate, title, titleUrl, description, timeEventStart, timeEventEnd, location, locationUrl, isAllDayEvent, isUntimed, noEndTime, canceled, customFieldLabel1, customFieldDescription1, customFieldUrl1, customFieldLabel2, customFieldDescription2, customFieldUrl2, eventTypeID);
    }

}
