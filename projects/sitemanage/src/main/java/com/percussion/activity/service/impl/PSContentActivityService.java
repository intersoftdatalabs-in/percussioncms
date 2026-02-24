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

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;

import com.percussion.activity.data.*;
import com.percussion.activity.service.IPSActivityService;
import com.percussion.activity.service.IPSContentActivityService;
import com.percussion.activity.service.IPSEffectivenessService;
import com.percussion.activity.service.IPSTrafficService;
import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSAbstractBeanValidator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** See interface for details. Sunny Sal: "REST easy, this is Java 11!" */
@Path("/activity")
@Component("contentActivityService")
@Lazy
public class PSContentActivityService implements IPSContentActivityService {

  private final IPSActivityService activityService;
  private final IPSEffectivenessService effectivenessService;
  private final IPSTrafficService trafficService;
  private final IPSAnalyticsProviderService analyticsProviderService;
  private final IPSSystemProperties systemProperties;

  private final PSEffectivenessComparator eComp = new PSEffectivenessComparator();

  @Autowired
  public PSContentActivityService(
      IPSActivityService activityService,
      IPSEffectivenessService effectivenessService,
      IPSTrafficService trafficService,
      IPSAnalyticsProviderService analyticsProviderService,
      IPSSystemProperties systemProperties) {
    this.activityService = activityService;
    this.effectivenessService = effectivenessService;
    this.trafficService = trafficService;
    this.analyticsProviderService = analyticsProviderService;
    this.systemProperties = systemProperties;
  }

  @POST
  @Path("/contentactivity")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Override
  public List<PSContentActivity> getContentActivity(PSContentActivityRequest request) {
    try {
      contentActivityReqvalidator.validate(request);
      String path = request.getPath().orElse(null);
      String durationType = request.getDurationType().orElse(null);
      String duration = request.getDuration().orElse(null);
      return new PSContentActivityList(getContentActivity(path, durationType, duration, true));
    } catch (PSValidationException
        | IPSActivityService.PSActivityServiceException
        | IPSPathService.PSPathServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/effectiveness")
  @SuppressWarnings("unchecked")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Override
  public List<PSEffectiveness> getEffectiveness(PSEffectivenessRequest request) {
    try {
      contentActivityReqvalidator.validate(request);

      PSAnalyticsProviderConfig config = analyticsProviderService.loadConfig(false);
      if (config == null) {
        throw new PSAnalyticsProviderException(
            "Analytics has not been setup yet.", CAUSETYPE.ANALYTICS_NOT_CONFIG);
      }

      List<PSEffectiveness> eList = new ArrayList<>();
      String durationType = request.getDurationType().orElse(null);
      String duration = request.getDuration().orElse(null);
      String path = request.getPath().orElse(null);
      var caList = getContentActivity(path, durationType, duration, false);
      if (caList.isEmpty()) {
        var pathSplit = path.split("/");
        if (pathSplit.length > 2 && StringUtils.isNotEmpty(pathSplit[2])) {
          request.setPath('/' + pathSplit[1] + '/' + pathSplit[2]);
          var nodeName = pathSplit[pathSplit.length - 1];
          var ca = new PSContentActivity(pathSplit[2], path, nodeName, 0, 0, 0, 0, 0);
          caList.add(ca);
          eList = effectivenessService.getEffectiveness(request, caList);
        }
      } else {
        eList = effectivenessService.getEffectiveness(request, caList);
        eList.sort(eComp);
      }
      return new PSEffectivenessList(eList);
    } catch (PSAnalyticsProviderException
        | PSValidationException
        | IPSActivityService.PSActivityServiceException
        | IPSPathService.PSPathServiceException
        | IPSGenericDao.LoadException e) {
      throw new WebApplicationException(e);
    }
  }

  @Override
  @POST
  @Path("/contenttraffic")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentTraffic getContentTraffic(PSContentTrafficRequest request)
      throws PSValidationException {
    try {
      return trafficService.getContentTraffic(request);
    } catch (IPSTrafficService.PSTrafficServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  @Override
  @POST
  @Path("/trafficdetails")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSTrafficDetails> getTrafficDetails(PSTrafficDetailsRequest request) {
    try {
      return new PSTrafficDetailsList(trafficService.getTrafficDetails(request));
    } catch (IPSPathService.PSPathServiceException
        | IPSTrafficService.PSTrafficServiceException
        | PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Get the date before the current date for the given duration and the given type of duration. If
   * the duration type is days, then it gives a date that many days prior to the current date.
   */
  private Date getDurationDate(PSDurationTypeEnum dtype, int duration) {
    var cal = Calendar.getInstance();
    switch (dtype) {
      case days:
        cal.add(Calendar.DATE, -duration);
        break;
      case weeks:
        cal.add(Calendar.DATE, -(duration * 7));
        break;
      case months:
        cal.add(Calendar.MONTH, -duration);
        break;
      case years:
        cal.add(Calendar.YEAR, -duration);
        break;
      default:
        throw new IllegalArgumentException("Invalid duration type.");
    }
    return cal.getTime();
  }

  private List<PSContentActivity> getContentActivity(
      String path, String durationType, String duration, boolean includeSite)
      throws IPSActivityService.PSActivityServiceException, IPSPathService.PSPathServiceException {
    var caList = new ArrayList<PSContentActivity>();
    int timeoutSeconds =
        NumberUtils.toInt(
            systemProperties.getProperty(IPSSystemProperties.CONTENT_ACTIVITY_TIME_OUT),
            DEFAULT_TIMEOUT);
    if (timeoutSeconds <= 0) timeoutSeconds = DEFAULT_TIMEOUT;
    long timeout = timeoutSeconds * 1000L;

    var sw = new StopWatch();
    sw.start();

    var dtype = PSDurationTypeEnum.valueOf(durationType);
    var durationDate = getDurationDate(dtype, Integer.parseInt(duration));
    var nodes = activityService.createActivityNodesByPaths(path, includeSite);
    for (var node : nodes) {
      long remaining = timeout - sw.getTime();
      caList.add(activityService.createActivity(node, durationDate, remaining));
    }
    return caList;
  }

  private final PSAbstractBeanValidator<PSContentActivityRequest> contentActivityReqvalidator =
      new PSContentActivityRequestValidator();

  /** Content Activity request validator, it checks whether the supplied path is not blank. */
  public static class PSContentActivityRequestValidator
      extends PSAbstractBeanValidator<PSContentActivityRequest> {
    @Override
    protected void doValidation(PSContentActivityRequest req, PSBeanValidationException e) {
      String duration = "0";
      try {
        String path = req.getPath().orElse(null);
        String durationType = req.getDurationType().orElse(null);
        duration = req.getDuration().orElse("0");
        rejectIfBlank("contentactivity", "path", path);
        rejectIfBlank("contentactivity", "durationType", durationType);
        rejectIfBlank("contentactivity", "duration", duration);

        var dtype = PSDurationTypeEnum.valueOf(durationType);
        if (dtype == null) {
          e.rejectValue(
              "Duration Type",
              "durationtype",
              "Invalid duration type, valid values are days, weeks, months and years.");
        }
      } catch (Exception ex) {
        e.rejectValue(
            "Duration Type",
            "durationtype",
            "Invalid duration type, valid values are days, weeks, months and years.");
      }
      try {
        Integer.parseInt(duration);
      } catch (NumberFormatException nfe) {
        e.rejectValue("Duration", "Duration", "The duration must be an integer");
      }
    }
  }

  /** Temporary method that fills the test data. */
  private void fillTestData(String path, List<PSContentActivity> caList) {
    if ("/Sites/".equals(path)) {
      caList.add(new PSContentActivity("Site1", "Site1", 357, 20, 45, 20, 10));
      caList.add(new PSContentActivity("Site2", "Site2", 126, 16, 45, 20, 10));
      caList.add(new PSContentActivity("Site2", "Site3", 238, 28, 45, 20, 10));
      caList.add(new PSContentActivity("Site4", "Site4", 18, 129, 2, 3, 1));
      caList.add(new PSContentActivity("Resources", 580, 36, 28, 45, 8));
      caList.add(new PSContentActivity("Non-Resources", 256, 16, 8, 12, 4));
    } else {
      caList.add(new PSContentActivity("Site1", 357, 20, 45, 20, 10));
      caList.add(new PSContentActivity("Section 1", 82, 16, 45, 20, 10));
      caList.add(new PSContentActivity("Section 2", 115, 28, 45, 20, 10));
      caList.add(new PSContentActivity("Section 3", 157, 129, 2, 3, 1));
      caList.add(new PSContentActivity("Resources", 580, 36, 28, 45, 8));
      caList.add(new PSContentActivity("Non-Resources", 256, 16, 8, 12, 4));
    }
  }
}
