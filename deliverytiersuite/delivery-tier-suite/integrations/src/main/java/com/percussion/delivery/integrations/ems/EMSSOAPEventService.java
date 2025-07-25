                            b.setTimeBookingStart(textContent);
                            break;
                        case "TimeBookingEnd":
                            b.setTimeBookingEnd(textContent);
                            break;
                        case "GMTStartTime":
                            b.setGmtStartTime(textContent);
                            break;
                        case "GMTEndTime":
                            b.setGmtEndTime(textContent);
                            break;
                        case "TimeZone":
                            b.setTimeZone(textContent);
                            break;
                        case "BuildingCode":
                            b.setBuildingCode(textContent);
                            break;
                        case "Building":
                            b.setBuilding(textContent);
                            break;
                        case "RoomCode":
                            b.setRoomCode(textContent);
                            break;
                        case "Room":
                            b.setRoom(textContent);
                            break;
                        case "RoomID":
                            b.setRoomId(Integer.parseInt(textContent));
                            break;
                        case "BuildingID":
                            b.setBuildingId(Integer.parseInt(textContent));
                            break;
                        case "RoomTypeID":
                            b.setRoomTypeId(Integer.parseInt(textContent));
                            break;
                        case "RoomType":
                            b.setRoomType(textContent);
                            break;
                        case "HVACZone":
                            b.setHvacZone(textContent);
                            break;
                        case "StatusID":
                            b.setStatusID(Integer.parseInt(textContent));
                            break;
                        case "StatusTypeID":
                            b.setStatusTypeId(Integer.parseInt(textContent));
                            break;
                        case "EventTypeID":
                            b.setEventTypeId(Integer.parseInt(textContent));
                            break;
                        case "DateAdded":
                            b.setDateAdded(textContent);
                            break;
                        case "AddedBy":
                            b.setAddedBy(textContent);
                            break;
                        case "DateChanged":
                            b.setDateChanged(textContent);
                            break;
                        case "ChangedBy":
                            b.setChangedBy(textContent);
                            break;
                        case "ContactEmailAddress":
                            b.setContactEmailAddress(textContent);
                            break;
                        case "CheckedIn":
                            b.setCheckedIn(Boolean.parseBoolean(textContent));
                            break;
                        default:
                            break;
                    }
                }
                // Filter events that are cancelled or are on hold.
                if (b.getStatusTypeId() != STATUS_TYPE_CANCEL && b.getStatusTypeId() != STATUS_TYPE_WAIT) {
                    // Defensive: fetch booking details if needed (legacy behavior)
                    soap.getBooking(this.userName, this.password, b.getBookingId());
                    ret.add(b);
                }
            }
        } catch (SAXException | IOException e) {
            log.error("Error parsing bookings:{}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }
/*
    @Override
    public List<EventType> getEventTypes() {
        if (eventTypes == null) {
            var xml = soap.getEventTypes(userName, password);
            if (checkForErrors(xml)) {
                log.error("getEventTypes Service returned the following errors:{}", xml);
            } else {
                eventTypes = parseEventTypeXML(xml);
            }
        }
        return eventTypes;
    }

    private boolean checkForErrors(String xml) {
        return xml.contains("<Errors>");
    }
import com.percussion.delivery.integrations.ems.model.EventType;
    @Override
    public List<Building> getBuildings() {
        if (buildings == null) {
            var xml = soap.getBuildings(userName, password);
            if (checkForErrors(xml)) {
                log.error("Buildings service returned the following errors:{}", xml);
            } else {
                buildings = parseBuildingXML(xml);
            }
        }
        return buildings;
    }
import service.web.api.ems.dea.ArrayOfInt;
    private List<Building> parseBuildingXML(String xml) {
        var ret = new ArrayList<Building>();
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error configuring XML parser for :{}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            var doc = db.newDocument();
            var fragmentNode = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            var buildings = doc.getElementsByTagName("Data");
            for (int i = 0; i < buildings.getLength(); i++) {
                var children = buildings.item(i).getChildNodes();
                var b = new Building();
                for (int j = 0; j < children.getLength(); j++) {
                    switch (children.item(j).getNodeName()) {
                        case "Description":
                            b.setDescription(children.item(j).getTextContent());
                            break;
                        case "ID":
                            b.setId(Integer.parseInt(children.item(j).getTextContent()));
                            break;
                        case "BuildingCode":
                            b.setBuildingCode(children.item(j).getTextContent());
                            break;
                        case "CurrentLocalTime":
                            b.setCurrentLocalTime(children.item(j).getTextContent());
                            break;
                        case "TimeZoneDescription":
                            b.setTimeZoneDescription(children.item(j).getTextContent());
                            break;
                        case "TimeZoneAbbreviation":
                            b.setTimeZoneAbbreviation(children.item(j).getTextContent());
                            break;
                        default:
                            break;
                    }
                }
                ret.add(b);
            }
        } catch (SAXException | IOException e) {
            log.error("Error parsing response: {} Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }
            var xml = soap.getStatuses(userName, password);
    @Override
    public List<GroupType> getGroupTypes() {
        if (groupTypes == null) {
            var xml = soap.getGroupTypes(userName, password);
            if (checkForErrors(xml)) {
                log.error("Group Types service returned the following errors:{}", xml);
            } else {
                groupTypes = parseGroupXML(xml);
            }
        }
        return groupTypes;
    }
                new PSXmlSecurityOptions(true, true, true, false, true, false));
    private List<GroupType> parseGroupXML(String xml) {
        var ret = new ArrayList<GroupType>();
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing Group Types:{} {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            var doc = db.newDocument();
            var fragmentNode = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            var groups = doc.getElementsByTagName("Data");
            for (int i = 0; i < groups.getLength(); i++) {
                var children = groups.item(i).getChildNodes();
                var g = new GroupType();
                for (int j = 0; j < children.getLength(); j++) {
                    switch (children.item(j).getNodeName()) {
                        case "Description":
                            g.setDescription(children.item(j).getTextContent());
                            break;
                        case "ID":
                            g.setId(Integer.parseInt(children.item(j).getTextContent()));
                            break;
                        case "AvailableOnWeb":
                            g.setAvailableOnWeb(Boolean.parseBoolean(children.item(j).getTextContent()));
                            break;
                        default:
                            break;
                    }
                }
                ret.add(g);
            }
        } catch (SAXException | IOException e) {
            log.error("Error parsing response: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }
        var ret = new ArrayList<Booking>();
    private List<EventType> parseEventTypeXML(String xml) {
        var ret = new ArrayList<EventType>();
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing Event Types:: {}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            var doc = db.newDocument();
            var fragmentNode = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            var groups = doc.getElementsByTagName("Data");
            for (int i = 0; i < groups.getLength(); i++) {
                var children = groups.item(i).getChildNodes();
                var e = new EventType();
                for (int j = 0; j < children.getLength(); j++) {
                    switch (children.item(j).getNodeName()) {
                        case "Description":
                            e.setDescription(children.item(j).getTextContent());
                            break;
                        case "ID":
                            e.setId(Integer.parseInt(children.item(j).getTextContent()));
                            break;
                        case "DisplayOnWeb":
                            e.setDisplayOnWeb(Boolean.parseBoolean(children.item(j).getTextContent()));
                            break;
                        default:
                            break;
                    }
                }
                ret.add(e);
            }
        } catch (SAXException | IOException e) {
            log.error("Error parsing response:: {}, Error: {}", xml, e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return ret;
    }
        var xml = soap.getBookings(userName, password, startDate, endDate, buildings, statuses, eventTypes, groups, false);
        if (checkForErrors(xml)) {
            log.error("Bookings service returned the following errors:{}", xml);
            return ret;
        }
        ret = parseBookingXML(xml);
        return ret;
    }

    private List<Booking> parseBookingXML(String xml) {
        var ret = new ArrayList<Booking>();
        var dbf = PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
                new PSXmlSecurityOptions(true, true, true, false, true, false));
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            log.error("Error parsing Buildings:{}, Error: {}", xml, e1.getMessage());
            log.debug(e1.getMessage(), e1);
        }
        try {
            var doc = db.newDocument();
            var fragmentNode = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            fragmentNode = doc.importNode(fragmentNode, true);
            doc.appendChild(fragmentNode);
            var bookings = doc.getElementsByTagName("Data");
            for (int i = 0; i < bookings.getLength(); i++) {
                var children = bookings.item(i).getChildNodes();
                var b = new Booking();
                for (int j = 0; j < children.getLength(); j++) {
                    var nodeName = children.item(j).getNodeName();
                    var textContent = children.item(j).getTextContent();
                    switch (nodeName) {
                        case "BookingDate":
                            b.setBookingDate(textContent);
                            break;
                        case "StartBookingDate":
                            b.setStartBookingDate(textContent);
                            break;
                        case "RoomDescription":
                            b.setRoomDescription(textContent);
                            break;
                        case "TimeEventStart":
                            b.setTimeEventStart(textContent);
                            break;
                        case "TimeEventEnd":
                            b.setTimeEventEnd(textContent);
                            break;
                        case "GroupName":
                            b.setGroupName(textContent);
                            break;
                        case "EventName":
                            b.setEventName(textContent);
                            break;
                        case "SetupTypeDescription":
                            b.setSetupTypeDescription(textContent);
                            break;
                        case "SetupCount":
                            b.setSetupCount(Integer.parseInt(textContent));
                            break;
                        case "ReservationID":
                            b.setReservationID(Integer.parseInt(textContent));
                            break;
                        case "EventCoordinator":
                            b.setEventCoordinator(textContent);
                            break;
                        case "GroupID":
                            b.setGroupID(Integer.parseInt(textContent));
                            break;
                        case "VIP":
                            b.setVip(textContent);
                            break;
                        case "VIPEvent":
                            b.setVipEvent(Boolean.parseBoolean(textContent));
                            break;
                        case "ClosedAllDay":
                            b.setClosedAllDay(Boolean.parseBoolean(textContent));
                            break;
                        case "OpenTime":
                            b.setOpenTime(textContent);
                            break;
                        case "CloseTime":
                            b.setCloseTime(textContent);
                            break;
                        case "GroupTypeDescription":
                            b.setGroupTypeDescription(textContent);
                            break;
                        case "EventTypeDescription":
                            b.setEventTypeDescription(textContent);
                            break;
                        case "Contact":
                            b.setContact(textContent);
                            break;
                        case "AltContact":
                            b.setAltContact(textContent);
                            break;
                        case "BookingID":
                            b.setBookingId(Integer.parseInt(textContent));
                            break;
                        case "TimeBookingStart":

	@Override
	public List<EventType> getEventTypes() {
		if(eventTypes == null){
			String xml = soap.getEventTypes(userName, password);
			
			if(checkForErrors(xml)){
				log.error("getEventTypes Service returned the following errors:{}", xml);
			}else{
				eventTypes = parseEventTypeXML(xml);
			}
		}
		return eventTypes;
	}
	
	private boolean checkForErrors(String xml){
		if(xml.contains("<Errors>")){
			return true;
		}else{
			return false;
		}
		
	}

	@Override
	public List<Building> getBuildings() {
		if(buildings == null){
			String xml = soap.getBuildings(userName, password);
			
			if(checkForErrors(xml)){
				log.error("Buildings service returned the following errors:{}", xml);
			}else{
				buildings = parseBuildingXML(xml);
			}
		}
		return buildings;
	}

	private List<Building> parseBuildingXML(String xml) {
		List<Building> ret = new ArrayList<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		dbf.setValidating(false);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			log.error("Error configuring XML parser for :{}, Error: {}", xml,e1.getMessage());
			log.debug(e1.getMessage(), e1);
		} 
		try {
			 /* <Data>
			    <Description>Z (old) Loker Student Union</Description>
			    <BuildingCode>(old) LSU</BuildingCode>
			    <ID>1</ID>
			    <TimeZoneDescription>Pacific Time (US &amp; Canada); Tijuana</TimeZoneDescription>
			    <TimeZoneAbbreviation>PT</TimeZoneAbbreviation>
			    <CurrentLocalTime>2018-05-14T12:51:08.493</CurrentLocalTime>
			  </Data>*/
			Document doc = db.newDocument();
			Node fragmentNode = db.parse(
				        new InputSource(new StringReader(xml)))
				        .getDocumentElement();
				    fragmentNode = doc.importNode(fragmentNode, true);
				    doc.appendChild(fragmentNode);
			NodeList buildings = doc.getElementsByTagName("Data");
			for(int i=0;i<buildings.getLength();i++){
				NodeList children = buildings.item(i).getChildNodes();
				Building b = new Building();
				for(int j=0;j<children.getLength();j++){
					if(children.item(j).getNodeName() == "Description"){
						b.setDescription(children.item(j).getTextContent());
					}else if(children.item(j).getNodeName() == "ID"){
						b.setId(Integer.parseInt(children.item(j).getTextContent()));
					}else if(children.item(j).getNodeName() == "BuildingCode"){
						b.setBuildingCode(children.item(j).getTextContent());
					}else if(children.item(j).getNodeName() == "CurrentLocalTime"){
						b.setCurrentLocalTime(children.item(j).getTextContent());
					}else if(children.item(j).getNodeName() == "TimeZoneDescription"){
						b.setTimeZoneDescription(children.item(j).getTextContent());
					}else if(children.item(j).getNodeName() == "TimeZoneAbbreviation"){
						b.setTimeZoneAbbreviation(children.item(j).getTextContent());
					}
				}
				ret.add(b);
			}
			
		} catch (SAXException e) {
			log.error("Error parsing response: {} Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		} catch (IOException e) {
			log.error("Error parsing response {} Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		return ret;
	}

	@Override
	public List<GroupType> getGroupTypes() {
		if(groupTypes == null){
			String xml = soap.getGroupTypes(userName, password);
			if(checkForErrors(xml)){
				log.error("Group Types service returned the following errors:{}", xml);
			}else{
				groupTypes = parseGroupXML(xml);
			}
		}
		return groupTypes;
	}

	private List<GroupType> parseGroupXML(String xml) {
		List<GroupType> ret = new ArrayList<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		dbf.setValidating(false);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			log.error("Error parsiing Group Types:{} {}", xml, e1.getMessage());
			log.debug(e1.getMessage(), e1);
		} 
		try {
			Document doc = db.newDocument();
			Node fragmentNode = db.parse(
				        new InputSource(new StringReader(xml)))
				        .getDocumentElement();
				    fragmentNode = doc.importNode(fragmentNode, true);
				    doc.appendChild(fragmentNode);
			NodeList groups = doc.getElementsByTagName("Data");
			for(int i=0;i<groups.getLength();i++){
				NodeList children = groups.item(i).getChildNodes();
				GroupType g = new GroupType();
				for(int j=0;j<children.getLength();j++){
					if(children.item(j).getNodeName() == "Description"){
						g.setDescription(children.item(j).getTextContent());
					}else if(children.item(j).getNodeName() == "ID"){
						g.setId(Integer.parseInt(children.item(j).getTextContent()));
					}else if(children.item(j).getNodeName() == "AvailableOnWeb"){
						g.setAvailableOnWeb(Boolean.parseBoolean(children.item(j).getTextContent()));
					}
				}
				ret.add(g);
			}
			
		} catch (SAXException e) {
			log.error("Error parsing response: {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		} catch (IOException e) {
			log.error("Error parsing response: {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		return ret;
	}

	private List<EventType> parseEventTypeXML(String xml) {
		List<EventType> ret = new ArrayList<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		dbf.setValidating(false);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			log.error("Error parsing Event Types:: {}, Error: {}", xml,e1.getMessage());
			log.debug(e1.getMessage(), e1);
		} 
		try {
			Document doc = db.newDocument();
			Node fragmentNode = db.parse(
				        new InputSource(new StringReader(xml)))
				        .getDocumentElement();
				    fragmentNode = doc.importNode(fragmentNode, true);
				    doc.appendChild(fragmentNode);
			NodeList groups = doc.getElementsByTagName("Data");
			for(int i=0;i<groups.getLength();i++){
				NodeList children = groups.item(i).getChildNodes();
				EventType e = new EventType();
				for(int j=0;j<children.getLength();j++){
					if(children.item(j).getNodeName() == "Description"){
						e.setDescription(children.item(j).getTextContent());
					}else if(children.item(j).getNodeName() == "ID"){
						e.setId(Integer.parseInt(children.item(j).getTextContent()));
					}else if(children.item(j).getNodeName() == "DisplayOnWeb"){
						e.setDisplayOnWeb(Boolean.parseBoolean(children.item(j).getTextContent()));
					}
				}
				ret.add(e);
			}
			
		} catch (SAXException e) {
			log.error("Error parsing response:: {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		} catch (IOException e) {
			log.error("Error parsing response: : {}, Error: {}", xml,e.getMessage());
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));
		}
		return ret;
	}
	
	

}
