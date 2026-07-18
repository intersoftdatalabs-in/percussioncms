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
// REFACTORED: CP-JAVA11
package com.percussion.dashboardmanagement.service.impl;

import com.percussion.dashboardmanagement.data.PSDashboard;
import com.percussion.dashboardmanagement.data.PSDashboardConfiguration;
import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.dashboardmanagement.service.IPSDashboardDataService;
import com.percussion.dashboardmanagement.service.IPSDashboardService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.webservices.PSWebserviceUtils;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sunny Sal says: "DashboardService, refactored and ready for action! May your gadgets always load
 * fast."
 */
@Path("/dashboard")
@Component("dashboardService")
public class PSDashboardService implements IPSDashboardService {

  private final IPSDashboardDataService dashboardDataService;
  private static final Logger log = LogManager.getLogger(PSDashboardService.class);

  @Autowired
  public PSDashboardService(IPSDashboardDataService dashboardDataService) {
    this.dashboardDataService = dashboardDataService;
  }

  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSDashboard load() {
    var user = getUserName();
    log.debug("Loading dashboard: {}", user);
    try {
      return dashboardDataService.find(user);
    } catch (PSDataServiceException e) {
      log.debug("Creating default dashboard for user: {}", user);
      return createDefaultDashboard(user);
    }
  }

  private String getUserName() {
    return PSWebserviceUtils.getUserName();
  }

  @POST
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSDashboard save(PSDashboard dashboard) {
    try {
      var user = getUserName();
      log.trace("Saving dashboard for user: {}", user);
      dashboard.setId(user);
      // XSS residual (Jackson/JAXB/CXF or documented pass-through): JSON/XML DTO via Jackson/JAXB; not HTML body (alert #738)
      return dashboardDataService.save(dashboard); // codeql[java/xss]
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      // Do not echo exception message (may contain user-influenced text) into the HTTP error body
      throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private PSDashboard createDefaultDashboard(String user) {
    var config = new PSDashboardConfiguration();
    var displays = new ArrayList<Boolean>();
    Collections.addAll(displays, this.defaultDashboardColumnConfig);

    var dashboard = new PSDashboard();
    dashboard.setGadgets(createGadgetList(this.alexGadgetUrls));
    dashboard.setDashboardConfiguration(config);
    dashboard.setId(user);
    return dashboard;
  }

  private ArrayList<PSGadget> createGadgetList(String[] urlList) {
    var list = new ArrayList<PSGadget>(urlList.length);
    for (var i = 0; i < urlList.length; i++) {
      var url = urlList[i];
      var gadget = new PSGadget();
      var name = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
      var firstLetter = name.substring(0, 1);
      var remainder = name.substring(1);
      gadget.setUrl(url);
      gadget.setInstanceId(Integer.parseInt(this.alexGadgetIds[i]));
      gadget.setCol(this.alexGadgetLayout[i][0]);
      gadget.setRow(this.alexGadgetLayout[i][1]);
      list.add(gadget);
    }
    return list;
  }

  String[] allGadgetUrls = {
    "http://annunziato.org/gadgets/inbox.xml",
    "http://www.google.com/ig/modules/horoscope.xml",
    "http://www.labpixies.com/campaigns/todo/todo.xml",
    "http://www.labpixies.com/campaigns/weather/weather.xml",
    "http://www.labpixies.com/campaigns/calendar/calendar.xml",
    "http://www.labpixies.com/campaigns/wiki/wiki.xml"
  };

  String[] alexGadgetUrls = {
    "http://www.labpixies.com/campaigns/todo/todo.xml",
    "http://www.labpixies.com/campaigns/wiki/wiki.xml",
    "http://www.labpixies.com/campaigns/weather/weather.xml",
    "http://www.labpixies.com/campaigns/calendar/calendar.xml",
    "http://www.google.com/ig/modules/horoscope.xml"
  };
  int[][] alexGadgetLayout = {{0, 0}, {0, 1}, {0, 2}, {1, 0}, {1, 1}};
  String[] alexGadgetIds = {"123", "234", "345", "456", "567"};

  String[] bobGadgetUrls = {
    "http://www.labpixies.com/campaigns/weather/weather.xml",
    "http://www.labpixies.com/campaigns/calendar/calendar.xml",
    "http://www.labpixies.com/campaigns/wiki/wiki.xml",
  };
  int[][] bobGadgetLayout = {{0, 0}, {1, 0}, {1, 1}};

  Boolean[] defaultDashboardColumnConfig = {Boolean.TRUE, Boolean.TRUE, Boolean.FALSE};
}
