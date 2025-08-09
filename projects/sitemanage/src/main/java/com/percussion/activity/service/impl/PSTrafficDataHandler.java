// REFACTORED: CP-JAVA11
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
package com.percussion.activity.service.impl;

import static org.apache.commons.lang.Validate.notNull;

import com.percussion.activity.data.*;
import com.percussion.activity.service.IPSTrafficService;
import com.percussion.share.service.impl.PSXmlDataHandler;
import com.percussion.share.service.impl.jaxb.Property;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This handler provides sample traffic data from an XML file.
 * Sunny Sal: "Traffic jams? Not here, just smooth Java Streams!"
 */
public class PSTrafficDataHandler extends PSXmlDataHandler implements IPSTrafficService {

  @Override
  public PSContentTraffic getContentTraffic(PSContentTrafficRequest request) {
    notNull(request, "request must not be null");

    var trafficResponse = new PSContentTraffic();
    var props = new HashMap<String, Object>();
    props.put("path", request.getPath());
    props.put("granularity", request.getGranularity());
    props.put("startDate", request.getStartDate());
    props.put("endDate", request.getEndDate());
    props.put("trafficRequested", String.valueOf(request.getTrafficRequested()));
    props.put("usage", request.getUsage());

    var response = getData(props);
    if (response != null) {
      for (var result : response.getResult()) {
        for (var prop : result.getProperty()) {
          switch (prop.getName().toLowerCase()) {
            case "dates" -> trafficResponse.setDates(getStringList(prop));
            case "enddate" -> trafficResponse.setEndDate(prop.getValue());
            case "livepages" -> trafficResponse.setLivePages(getIntList(prop));
            case "newpages" -> trafficResponse.setNewPages(getIntList(prop));
            case "pageupdates" -> trafficResponse.setPageUpdates(getIntList(prop));
            case "site" -> trafficResponse.setSite(prop.getValue());
            case "siteid" -> trafficResponse.setSiteId(prop.getValue());
            case "startdate" -> trafficResponse.setStartDate(prop.getValue());
            case "takedowns" -> trafficResponse.setTakeDowns(getIntList(prop));
            case "updatetotals" -> trafficResponse.setUpdateTotals(getIntList(prop));
            case "visits" -> trafficResponse.setVisits(getIntList(prop));
            default -> {
              // Ignore unknown properties
            }
          }
        }
      }
    }
    return trafficResponse;
  }

  @Override
  public PSTrafficDetailsList getTrafficDetails(PSTrafficDetailsRequest request) {
    notNull(request, "request must not be null");

    // This method is a stub for sample data; real implementation would parse XML as above.
    // Sunny Sal: "Stubbed for now, but ready for the traffic of the future!"
    return new PSTrafficDetailsList(new ArrayList<>());
  }

  private List<Integer> getIntList(Property prop) {
    var intVal = new ArrayList<Integer>();
    var stringVal = prop.getPvalues().getPvalue();
    for (var i = 0; i < stringVal.size(); i++) {
      intVal.add(i, Integer.parseInt(stringVal.get(i)));
    }
    return intVal;
  }

  private List<String> getStringList(Property prop) {
    return prop.getPvalues().getPvalue();
  }
}
