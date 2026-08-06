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

package com.percussion.assetmanagement.forms.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;
import static com.percussion.share.web.service.PSRestServicePathConstants.FIND_ALL_PATH;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.assetmanagement.forms.data.PSFormSummary;
import com.percussion.assetmanagement.forms.data.PSFormSummaryList;
import com.percussion.assetmanagement.forms.service.IPSFormDataService;
import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.PSValidationException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Service for managing form data. */
@Path("/form")
@Component("formDataService")
public class PSFormDataService implements IPSFormDataService {

  private final IPSDeliveryInfoService deliveryService;
  private final PSFormDataJoiner formDataJoiner;

  @Autowired @Lazy private IPSPubServerService pubServerService;

  /** Logger for this service. */
  public static final Logger log = LogManager.getLogger(PSFormDataService.class);

  /**
   * Create an instance of the service.
   *
   * @param deliveryService the delivery service, not {@code null}.
   */
  @Autowired
  public PSFormDataService(IPSDeliveryInfoService deliveryService) {
    notNull(deliveryService);
    this.deliveryService = deliveryService;
    this.formDataJoiner = new PSFormDataJoiner();
  }

  /**
   * Finds a server with the forms service.
   *
   * @return the server, may be {@code null} if cannot find the server.
   */
  private PSDeliveryInfo findServer(String site)
      throws IPSPubServerService.PSPubServerServiceException, PSNotFoundException {
    var adminUrl = pubServerService.getDefaultAdminURL(site);
    var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_FORMS, null, adminUrl);
    if (server == null) {
      log.debug("Cannot find server with service of: {}", PSDeliveryInfo.SERVICE_FORMS);
    }
    return server;
  }

  @Override
  @GET
  @Path(FIND_ALL_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @SuppressWarnings("unchecked")
  public List<PSFormSummary> getAllFormData(String site) {
    String procUrl = null;
    try {
      var server = findServer(site);
      if (server == null) {
        return Collections.emptyList();
      }
      var formDataMap = new HashMap<String, PSFormSummary>();
      procUrl = server.getUrl() + FORM_INFO_URL;

      var result = new ArrayList<PSFormSummary>();
      var deliveryClient = new PSDeliveryClient();
      var getJson =
          deliveryClient.getJsonObject(
              new PSDeliveryActionOptions(
                  server, FORM_INFO_URL + "list", HttpMethodType.GET, true));

      @SuppressWarnings("unchecked")
      Map<String, Object> jsonMap = (Map<String, Object>) getJson;

      @SuppressWarnings("unchecked")
      List<Object> formInfo = (List<Object>) jsonMap.get("formsInfo");

      for (var i = 0; i < formInfo.size(); i++) {
        PSFormSummary sum;

        @SuppressWarnings("unchecked")
        Map<String, Object> formObj = (Map<String, Object>) formInfo.get(i);
        var name = (String) formObj.get(NAME_FIELD);
        if (!formDataMap.containsKey(name)) {
          sum = new PSFormSummary();
          sum.setName(name);
        } else {
          sum = formDataMap.get(name);
        }
        mergeFormData(formObj, sum);
        formDataMap.put(name, sum);
      }
      result.addAll(formDataMap.values());
      result.sort(new CompareFormSummary());
      return new PSFormSummaryList(result);
    } catch (Exception e) {
      log.warn(
          "Error getting all form data from processor at : {} Error: {}",
          procUrl,
          PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /** Utility class, used to sort {@link PSFormSummary} by name. */
  static class CompareFormSummary implements Comparator<PSFormSummary> {
    @Override
    public int compare(PSFormSummary s1, PSFormSummary s2) {
      return s1.getName().compareToIgnoreCase(s2.getName());
    }
  }

  @Override
  @GET
  @Path("/{name}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @SuppressWarnings("unchecked")
  public PSFormSummary getFormData(@PathParam("name") String name) {
    try {
      rejectIfBlank("getFormData", "name", name);
      PSFormSummary sum = null;
      var processor = findServer("");
      if (processor == null) {
        return sum;
      }
      var url = processor.getUrl() + FORM_INFO_URL + name;
      var deliveryClient = new PSDeliveryClient();
      var getJson =
          deliveryClient.getJsonObject(
              new PSDeliveryActionOptions(
                  processor, FORM_INFO_URL + name, HttpMethodType.GET, true));

      @SuppressWarnings("unchecked")
      Map<String, Object> jsonMap = (Map<String, Object>) getJson;

      @SuppressWarnings("unchecked")
      List<Object> formInfo = (List<Object>) jsonMap.get("formsInfo");
      if (!formInfo.isEmpty()) {
        sum = new PSFormSummary();
        sum.setName(name);

        Map<String, Object> firstFormObj = (Map<String, Object>) formInfo.get(0);
        mergeFormData(firstFormObj, sum);
      }
      return sum;
    } catch (PSValidationException
        | IPSPubServerService.PSPubServerServiceException
        | PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @Override
  @DELETE
  @Path("/{name}/{site}")
  public void clearFormData(@PathParam("name") String name, @PathParam("site") String site)
      throws PSFormDataServiceException {
    try {
      var deliveryServer = findServer(site);
      if (deliveryServer == null) {
        throw new RuntimeException("Cannot find service of: " + PSDeliveryInfo.SERVICE_FORMS);
      }
      var deliveryClient = new PSDeliveryClient();
      deliveryClient.push(
          new PSDeliveryActionOptions(
              deliveryServer, FORM_INFO_URL + name, HttpMethodType.DELETE, true),
          null);
    } catch (IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @Override
  @GET
  @Path("submissions/{site}/{name}")
  @Produces("text/csv")
  public String exportFormData(@PathParam("site") String site, @PathParam("name") String name) {
    try {
      rejectIfBlank("exportFormData", "name", name);
      var deliveryServer = findServer(site);
      if (deliveryServer == null) {
        throw new WebApplicationException(
            "Cannot find service of: " + PSDeliveryInfo.SERVICE_FORMS);
      }
      var formPath = createFormNamePath(name);
      var formsData = new ArrayList<String>();
      var deliveryClient = new PSDeliveryClient();
      var response =
          deliveryClient.getString(
              new PSDeliveryActionOptions(
                  deliveryServer, FORM_INFO_URL + formPath, HttpMethodType.GET, true));
      if (response != null) {
        formsData.add(response);
      }
      return formDataJoiner.joinFormData(formsData.toArray(new String[0]));
    } catch (PSValidationException
        | IPSPubServerService.PSPubServerServiceException
        | PSNotFoundException e) {
      throw new WebApplicationException(e.getMessage());
    }
  }

  /**
   * Merges the submission data from a form object into a form summary.
   *
   * @param obj form object map.
   * @param sum form summary.
   */
  private void mergeFormData(Map<String, Object> obj, PSFormSummary sum) {
    var totalSubmissions = ((Number) obj.getOrDefault(TOTALFORMS_FIELD, 0)).intValue();
    var exportedForms = ((Number) obj.getOrDefault(EXPORTEDFORMS_FIELD, 0)).intValue();
    sum.setNewSubmissions(sum.getNewSubmissions() + (totalSubmissions - exportedForms));
    sum.setTotalSubmissions(sum.getTotalSubmissions() + totalSubmissions);
  }

  /**
   * Helper method to create a form name path.
   *
   * @param formName Not blank.
   * @return A path of the form "formName/formName.csv". Never empty.
   */
  private String createFormNamePath(String formName) {
    var formNewName = formName;
    var csvName = "";
    if (formName.endsWith(".csv")) {
      formNewName = formName.substring(0, formName.length() - 4);
      csvName = formName;
    }
    var sb = new StringBuilder();
    sb.append(formNewName);
    sb.append("/");
    if (csvName.isEmpty()) {
      sb.append(formNewName);
      sb.append(".csv");
    } else {
      sb.append(csvName);
    }
    return sb.toString();
  }

  private static final String NAME_FIELD = "name";
  private static final String TOTALFORMS_FIELD = "totalForms";
  private static final String EXPORTEDFORMS_FIELD = "exportedForms";
  private static final String FORM_INFO_URL = "/perc-form-processor/forms/form/cms/";
}
