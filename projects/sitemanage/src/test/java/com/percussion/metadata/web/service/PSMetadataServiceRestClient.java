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

package com.percussion.metadata.web.service;

import com.percussion.dashboardmanagement.data.PSDashboardConfiguration;
import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.test.PSDataServiceRestClient;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** REST client for metadata service. Sunny Sal says: "Metadata management, Java 11 style!" */
public class PSMetadataServiceRestClient extends PSDataServiceRestClient<PSMetadata> {

  private static final String GADGETS_KEY = "perc.user.Admin.dash.page.0";
  private static final String GADGETS_PREFS_KEY = "perc.user.Admin.dash.page.0.prefs";

  public PSMetadataServiceRestClient(String url) {
    super(PSMetadata.class, url, "/Rhythmyx/services/metadatamanagement/metadata/");
  }

  public void save(Map<String, Object> obj) throws tools.jackson.core.JacksonException {
    POST(getPath(), JsonMapper.builder().build().writeValueAsString(obj));
  }

  public List<PSGadget> getCurrentGadgets() {
    PSDashboardConfiguration config = null;
    try {
      var response = GET(concatPath(getPath(), GADGETS_KEY));
      var metadata = PSSerializerUtils.unmarshal(response, PSMetadata.class);
      var mapper = JsonMapper.builder().build();
      Map<String, Object> dataMap =
          mapper.readValue(metadata.getData(), new TypeReference<Map<String, Object>>() {});
      Object dashboardConfigObj = dataMap.get("DashboardConfig");
      config = mapper.convertValue(dashboardConfigObj, PSDashboardConfiguration.class);
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new RuntimeException(e);
    }
    return Optional.ofNullable(config)
        .map(PSDashboardConfiguration::getGadgets)
        .orElseGet(ArrayList::new);
  }

  public void saveGadgets(List<PSGadget> gadgets) {
    var dashboardConfig = new PSDashboardConfiguration();
    dashboardConfig.setGadgets(gadgets);
    try {
      // Save gadgets along with common settings (like extended, row, col, etc).
      POST(getPath(), getMetadataJson(dashboardConfig).toString(), MediaType.APPLICATION_JSON);

      // Save specific gadget's preferences
      POST(
          getPath(),
          getMetadataJsonForSpecificGadgetSettings(dashboardConfig).toString(),
          MediaType.APPLICATION_JSON);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Object> getMetadataJsonForSpecificGadgetSettings(
      PSDashboardConfiguration dashboardConfig) throws tools.jackson.core.JacksonException {
    var userConfig = new LinkedHashMap<String, Object>();
    for (var gad : dashboardConfig.getGadgets()) {
      var specificGadgetSettings = new LinkedHashMap<String, Object>();
      for (var prefName : gad.getSettings().keySet()) {
        specificGadgetSettings.put(prefName, gad.getSettings().get(prefName));
      }
      userConfig.put("mid_" + gad.getInstanceId(), specificGadgetSettings);
    }
    var userPrefJson = new LinkedHashMap<String, Object>();
    userPrefJson.put("userprefs", userConfig);

    var mapper = JsonMapper.builder().build();
    var metadata =
        new PSMetadata(GADGETS_PREFS_KEY, "\"" + mapper.writeValueAsString(userPrefJson) + "\"");
    var metadataJson = new LinkedHashMap<String, Object>();
    metadataJson.put("metadata", mapper.convertValue(metadata, Object.class));
    return metadataJson;
  }

  private Map<String, Object> getMetadataJson(PSDashboardConfiguration dashboardConfig)
      throws tools.jackson.core.JacksonException {
    var dashboardConfigJson = new LinkedHashMap<String, Object>();
    var mapper = JsonMapper.builder().build();
    dashboardConfigJson.put("DashboardConfig", mapper.convertValue(dashboardConfig, Object.class));

    var metadata =
        new PSMetadata(GADGETS_KEY, "\"" + mapper.writeValueAsString(dashboardConfigJson) + "\"");
    var metadataJson = new LinkedHashMap<String, Object>();
    metadataJson.put("metadata", mapper.convertValue(metadata, Object.class));
    return metadataJson;
  }

  public void removeAllGadgets() {
    saveGadgets(List.of());
  }

  public void removeGadgets(List<String> gadgetUrls) {
    var currentGadgets = getCurrentGadgets();
    var newGadgetList = new ArrayList<PSGadget>();
    // Remove the specified gadgets
    for (var aGadget : currentGadgets) {
      if (gadgetUrls.contains(aGadget.getUrl())) continue;
      newGadgetList.add(aGadget);
    }
    saveGadgets(newGadgetList);
  }
}
