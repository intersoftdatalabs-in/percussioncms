DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing MCCalendar: {}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            Document doc = db.newDocument();
            Node fragmentNode = db.parse(
                    new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            NodeList entries = doc.getElementsByTagName("Data");
            for (var i = 0; i < entries.getLength(); i++) {
                var children = entries.item(i).getChildNodes();
                for (var j = 0; j < children.getLength(); j++) {
                    var nodeName = children.item(j).getNodeName();
                    var textContent = children.item(j).getTextContent();
                    switch (nodeName) {
                        case "Name":
                            ret.setCalendarName(textContent);
                            break;
                        case "CalendarID":
                            ret.setCalendarId(Integer.parseInt(textContent));
                            break;
                        case "Description":
                            ret.setCalendarDescription(textContent);
                            break;
                        case "AdminName":
                            ret.setAdminName(textContent);
                            break;
                        case "AdminEmail":
                            ret.setAdminEmail(textContent);
                            break;
                        case "ApprovalEmail":
                            ret.setApprovalEmail(textContent);
                            break;
                        case "IsPrivate":
                            ret.setPrivateCalendar(Boolean.parseBoolean(textContent));
                            break;
                        case "IsActive":
                            ret.setActiveCalendar(Boolean.parseBoolean(textContent));
                            break;
                        case "ShowCancelledEvents":
                            ret.setShowCancelledEvents(Boolean.parseBoolean(textContent));
                            break;
                        case "DefaultViewID":
                            ret.setDefaultViewId(Integer.parseInt(textContent));
                            break;
                        case "CalendarFormatID":
                            ret.setCalendarFormatId(Integer.parseInt(textContent));
                            break;
                        case "GroupingID":
                            ret.setCalendarGroupingId(Integer.parseInt(textContent));
                            break;
                        case "ShowWeekends":
                            ret.setShowWeekends(Boolean.parseBoolean(textContent));
                            break;
                        case "StartWeekOn":
                            ret.setStartWeekOn(Integer.parseInt(textContent));
                            break;
                        case "AllowPublicSubmission":
                            ret.setAllowPublicSubmission(Boolean.parseBoolean(textContent));
                            break;
                        case "ContactInfoPublic":
                            ret.setShowCalendarContactInfo(Boolean.parseBoolean(textContent));
                            break;
                        case "Subscription":
                            ret.setSubscription(Integer.parseInt(textContent));
                            break;
                        case "ListTypeID":
                            ret.setListTypeId(Integer.parseInt(textContent));
                            break;
                        default:
                            // ignore unknown fields
                    }
                }
            }

        } catch (SAXException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        } catch (IOException e) {
            log.error("Error parsing response: {} Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }

        return ret;
    }

    private List<MCCalendarEntry> parseCalendarListXML(String xml) {
        var ret = new ArrayList<MCCalendarEntry>();
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing MCCalendars: {}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            var doc = db.newDocument();
            var fragmentNode = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            var entries = doc.getElementsByTagName("Data");
            for (var i = 0; i < entries.getLength(); i++) {
                var children = entries.item(i).getChildNodes();
                var e = new MCCalendarEntry();
                for (var j = 0; j < children.getLength(); j++) {
                    var nodeName = children.item(j).getNodeName();
                    if ("Name".equals(nodeName)) {
                        e.setCalendarName(children.item(j).getTextContent());
                    } else if ("CalendarID".equals(nodeName)) {
                        e.setCalendarId(Integer.parseInt(children.item(j).getTextContent()));
                    }
                }
                ret.add(e);
            }
        } catch (SAXException | IOException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }

    public class EMSMasterCalendarSoapEventService implements IPSEMSMasterCalendarService {
        public List<MCGrouping> getMasterCalendarGroupings() {

            List<MCGrouping> ret = new ArrayList<>();

            String xml;
            try {
                xml = soap.getGroupings(mcUserName, mcPassword);

                if (!checkForErrors(xml)) {

                    ret = parseGroupingsXML(xml);

                } else {
                    log.error("An error was returned when getting Groupings:{}", xml);
                }

            } catch (RemoteException e) {
                log.error("An unexpected error was returned by the remote server. Error: {}", PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            return ret;
        }
    }
    private List<MCGrouping> parseGroupingsXML(String xml) {
        var ret = new ArrayList<MCGrouping>();
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing MCGrouping: {}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            var doc = db.newDocument();
            var fragmentNode = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            var entries = doc.getElementsByTagName("Data");
            for (var i = 0; i < entries.getLength(); i++) {
                var children = entries.item(i).getChildNodes();
                var e = new MCGrouping();
                for (var j = 0; j < children.getLength(); j++) {
                    var nodeName = children.item(j).getNodeName();
                    if ("GroupingID".equals(nodeName)) {
                        e.setGroupingId(Integer.parseInt(children.item(j).getTextContent()));
                    } else if ("Name".equals(nodeName)) {
                        e.setName(children.item(j).getTextContent());
                    }
                }
                ret.add(e);
            }
        } catch (SAXException | IOException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }

            if (query.getCalendars() != null && !query.getCalendars().isEmpty()) {
                calendars = ArrayUtils.toPrimitive(query.getCalendars().toArray(new Integer[query.getCalendars().size()]));
            }

            String xml = soap.getEvents(mcUserName, mcPassword, startDate, endDate, eventName, location, calendars, eventTypes, null);
            if (checkForErrors(xml)) {
                log.error("getEvents Service returned the following errors:{}", xml);
            } else {
                ret = parseEventDetailXML(xml);
            }


        } catch (RemoteException e) {
            log.error("An error occurred connecting to the Master Calendar API Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return ret;
        }

        return ret;
    }

    private boolean checkForErrors(String xml) {
        if (xml.contains("<Errors>")) {
            return true;
        } else {
            return false;
        }
    }

    private List<MCEventDetail> parseEventDetailXML(String xml) {
        List<MCEventDetail> ret = new ArrayList<>();

        DocumentBuilderFactory dbf = PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
                new PSXmlSecurityOptions(
                        true,
                        true,
                        true,
                        false,
                        true,
                        false
                )
        );
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing MCEventDetail:{}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            Document doc = db.newDocument();
            Node fragmentNode = db.parse(
                    new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            NodeList entries = doc.getElementsByTagName("Data");
            for (int i = 0; i < entries.getLength(); i++) {
                NodeList children = entries.item(i).getChildNodes();
                MCEventDetail e = new MCEventDetail();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j).getNodeName() == "EventDetailID") {
                        e.setEventDetailID(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "EventID") {
                        e.setEventID(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "Title") {
                        e.setTitle(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "Description") {
                        e.setDescription(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "Location") {
                        e.setLocation(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "LocationUrl") {
                        e.setLocationUrl(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "Canceled") {
                        e.setCancelled(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "NoEndTime") {
                        e.setNoEndTime(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "Priority") {
                        e.setPriority(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "EventDate") {
                        e.setEventDate(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "TimeEventStart") {
                        e.setEventStartTime(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "TimeEventEnds") {
                        e.setEventEndTime(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "IsAllDayEvent") {
                        e.setIsAllDayEvent(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "IsTimedEvent") {
                        e.setIsTimedEvent(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "EventTypeID") {
                        e.setEventTypeId(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "EventTypeName") {
                        e.setEventTypeName(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "Contactname") {
                        e.setContactName(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "ContactEmail") {
                        e.setContactEmail(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "IsReOccurring") {
                        e.setIsReOccuring(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "IsOnMultipleCalendars") {
                        e.setIsOnMultipleCalendars(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "BookingID") {
                        e.setBookingID(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "ReservationID") {
                        e.setReservationID(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "ConnectorID") {
                        e.setConnectorID(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "HideContactName") {
                        e.setHideContactName(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "HideContactEmail") {
                        e.setHideContactName(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "HideContactPhone") {
                        e.setHideContactName(Boolean.parseBoolean(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "CustomFieldLabel1") {
                        e.setCustomLabelField1(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "CustomFieldDescription1") {
                        e.setCustomFieldDescription1(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "CustomUrl1") {
                        e.setCustomUrl1(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "CustomFieldLabel2") {
                        e.setCustomLabelField2(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "CustomFieldDescription2") {
                        e.setCustomFieldDescription2(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "CustomUrl2") {
                        e.setCustomUrl2(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "EventUpdatedBy") {
                        e.setEventUpdatedBy(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "EventUpdatedDate") {
                        e.setEventUpdatedDate(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "EventDetailUpdatedBy") {
                        e.setEventDetailUpdatedBy(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "EventDetailUpdatedDate") {
                        e.setEventDetailUpdatedDate(children.item(j).getTextContent());
                    }

                }
                ret.add(e);
            }

        } catch (SAXException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        } catch (IOException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }

        return ret;
    }

    @Override
    public List<MCEventDetail> getMasterCalendarFeaturedEvents(PSFeaturedEventsQuery query) {
        List<MCEventDetail> ret = new ArrayList<>();
        String eventName = null;
        String location = null;
        int[] eventTypes = null;
        int[] calendars = null;

        try {
            FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd hh:mm:ss");
            Date date = null;
            try {
                date = (Date) format.parseObject(query.getStartDate());
            } catch (ParseException e) {
                log.error("Error processing start date: {} Error: {}",
                        query.getStartDate(),
                        e.getMessage());
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(date);

            try {
                date = (Date) format.parseObject(query.getEndDate());
            } catch (ParseException e) {
                log.error("Error processing end date: {} Error: {}",
                        query.getEndDate(),
                        e.getMessage());
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(date);

            if (query.getEventNameSearch() != null && !query.getEventNameSearch().trim().equals("")) {
                eventName = query.getEventNameSearch();
            }

            if (query.getLocationNameSearch() != null && !query.getLocationNameSearch().trim().equals("")) {
                location = query.getLocationNameSearch();
            }

            if (query.getEventTypesToSearch() != null && !query.getEventTypesToSearch().isEmpty()) {
                eventTypes = ArrayUtils.toPrimitive(query.getEventTypesToSearch().toArray(new Integer[query.getEventTypesToSearch().size()]));
            }

            if (query.getCalendarsToSearch() != null && !query.getCalendarsToSearch().isEmpty()) {
                calendars = ArrayUtils.toPrimitive(query.getCalendarsToSearch().toArray(new Integer[query.getCalendarsToSearch().size()]));
            }

            String xml = soap.getEvents(mcUserName, mcPassword, startDate, endDate, eventName, location, calendars, eventTypes, null);

            ret = parseEventDetailXML(xml);
        } catch (Exception e) {
            log.error("Error while processing Featured Events, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }

        return ret;
    }

    @Override
    public List<MCEventType> getMasterCalendarEventTypes() {
        List<MCEventType> ret = new ArrayList<>();

        try {
            String xml = soap.getEventTypes(mcUserName, mcPassword);
            if (!checkForErrors(xml)) {
                ret = parseEventTypesXML(xml);
            } else {
                log.error("An error was returned when getting EventTypes:{}", xml);
            }

        } catch (RemoteException e) {
            log.error("An error occurred pulling remote Event Types, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }

        return ret;
    }

    private List<MCEventType> parseEventTypesXML(String xml) {
        List<MCEventType> ret = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing MCEventType:{}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            Document doc = db.newDocument();
            Node fragmentNode = db.parse(
                    new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            NodeList entries = doc.getElementsByTagName("Data");
            for (int i = 0; i < entries.getLength(); i++) {
                NodeList children = entries.item(i).getChildNodes();
                MCEventType e = new MCEventType();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j).getNodeName() == "EventTypeID") {
                        e.setEventTypeId(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "Name") {
                        e.setEventTypeLocationName(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "Color") {
                        e.setEventTypeColor(children.item(j).getTextContent());
                    }
                }
                ret.add(e);
            }

        } catch (SAXException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        } catch (IOException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }

    @Override
    public List<MCLocation> getMasterCalendarLocations() {
        List<MCLocation> ret = new ArrayList<>();

        try {
            String xml = soap.getLocations(mcUserName, mcPassword);
            if (!checkForErrors(xml)) {
                ret = parseLocationsXML(xml);
            } else {
                log.error("An error was returned when getting Locations:{}", xml);
            }
        } catch (RemoteException e) {
            log.error("An error occurred pulling remote Locations {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }


        return ret;
    }

    private List<MCLocation> parseLocationsXML(String xml) {
        List<MCLocation> ret = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing MCLocation:{}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            Document doc = db.newDocument();
            Node fragmentNode = db.parse(
                    new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            NodeList entries = doc.getElementsByTagName("Data");
            for (int i = 0; i < entries.getLength(); i++) {
                NodeList children = entries.item(i).getChildNodes();
                MCLocation e = new MCLocation();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j).getNodeName() == "LocationID") {
                        e.setLocationId(Integer.parseInt(children.item(j).getTextContent()));
                    } else if (children.item(j).getNodeName() == "Name") {
                        e.setLocationName(children.item(j).getTextContent());
                    } else if (children.item(j).getNodeName() == "Url") {
                        e.setLocationUrl(children.item(j).getTextContent());
                    }
                }
                ret.add(e);
            }

        } catch (SAXException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        } catch (IOException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }

    @Override
    public List<MCCalendar> getMasterCalendarCalendars() {
        List<MCCalendar> ret = new ArrayList<>();

        try {
            String xml = soap.getCalendars(mcUserName, mcPassword);
            if (!checkForErrors(xml)) {
                List<MCCalendarEntry> entries = parseCalendarListXML(xml);
                for (MCCalendarEntry e : entries) {
                    xml = soap.getCalendar(mcUserName, mcPassword, e.getCalendarId());
                    if (!checkForErrors(xml)) {
                        MCCalendar c = parseCalendarXML(xml);
                        ret.add(c);
                    } else {
                        log.error("An error was returned when getting Calendar:{} : {}", e.getCalendarName(), xml);
                    }
                }
            } else {
                log.error("An error was returned when getting Calendars:{}", xml);
            }

        } catch (RemoteException e) {
            log.error("An error occurred connecting to the Master Calendar API {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return ret;
        }


        // TODO Auto-generated method stub
        return ret;
    }

    private MCCalendar parseCalendarXML(String xml) {
        MCCalendar ret = new MCCalendar();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);

				}
	
			}
			
		} catch (SAXException e) {
			log.error("Error parsing response: {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		} catch (IOException e) {
			log.error("Error parsing response: {} Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		
		return ret;
	}
	
	private List<MCCalendarEntry> parseCalendarListXML(String xml) {
		var ret = new ArrayList<MCCalendarEntry>();
		var dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		dbf.setValidating(false);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			log.error("Error parsing MCCalendars: {}, Error: {}",xml, e1.getMessage());
			log.debug(e1.getMessage(), e1);
		} 
		try {
			var doc = db.newDocument();
			var fragmentNode = db.parse(
				        new InputSource(new StringReader(xml)))
				        .getDocumentElement();
				    fragmentNode = doc.importNode(fragmentNode, true);
				    doc.appendChild(fragmentNode);
			var entries = doc.getElementsByTagName("Data");
			for(var i=0;i<entries.getLength();i++){
				var children = entries.item(i).getChildNodes();
				var e = new MCCalendarEntry();
				for(var j=0;j<children.getLength();j++){
					var nodeName = children.item(j).getNodeName();
					if ("Name".equals(nodeName)) {
                        e.setCalendarName(children.item(j).getTextContent());
                    } else if ("CalendarID".equals(nodeName)) {
                        e.setCalendarId(Integer.parseInt(children.item(j).getTextContent()));
                    }
				}
				ret.add(e);
			}
			
		} catch (SAXException | IOException e) {
			log.error("Error parsing response: {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		return ret;
	}

	public List<MCGrouping>getMasterCalendarGroupings(){
		
		List<MCGrouping> ret = new ArrayList<>();
		
		String xml;
		try {
			xml = soap.getGroupings(mcUserName, mcPassword);
		
			if(!checkForErrors(xml)){
			
				ret = parseGroupingsXML(xml);
				
			}else{
				log.error("An error was returned when getting Groupings:{}", xml);
			}
			
		} catch (RemoteException e) {
			log.error("An unexpected error was returned by the remote server. Error: {}", PSExceptionUtils.getMessageForLog(e));
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		return ret;
	}

	private List<MCGrouping> parseGroupingsXML(String xml) {
		var ret = new ArrayList<MCGrouping>();
		var dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		dbf.setValidating(false);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			log.error("Error parsing MCGrouping: {}, Error: {}",xml, e1.getMessage());
			log.debug(e1.getMessage(), e1);
		} 
		try {
			var doc = db.newDocument();
			var fragmentNode = db.parse(
				        new InputSource(new StringReader(xml)))
				        .getDocumentElement();
				    fragmentNode = doc.importNode(fragmentNode, true);
				    doc.appendChild(fragmentNode);
			var entries = doc.getElementsByTagName("Data");
			for(var i=0;i<entries.getLength();i++){
				var children = entries.item(i).getChildNodes();
				var e = new MCGrouping();
				for(var j=0;j<children.getLength();j++){
					var nodeName = children.item(j).getNodeName();
					if ("GroupingID".equals(nodeName)) {
                        e.setGroupingId(Integer.parseInt(children.item(j).getTextContent()));
                    } else if ("Name".equals(nodeName)) {
                        e.setName(children.item(j).getTextContent());
                    }
				}
				ret.add(e);
			}
			
		} catch (SAXException | IOException e) {
			log.error("Error parsing response: {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		return ret;
	}
	
}
