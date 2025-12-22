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
package com.percussion.sitemanage.web.service;

import com.percussion.share.data.PSEnumVals;
import com.percussion.share.test.PSDataServiceRestClient;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteCopyRequest;
import com.percussion.sitemanage.data.PSSiteProperties;
import com.percussion.sitemanage.data.PSSitePublishProperties;
import com.percussion.sitemanage.data.PSSiteStatisticsSummary;
import com.percussion.sitemanage.data.PSSiteSummary;
import java.util.List;

/** REST client for site management. // REFACTORED: CP-JAVA11 */
public class PSSiteRestClient extends PSDataServiceRestClient<PSSite> {

  public PSSiteRestClient(String url) {
    super(PSSite.class, url, "/Rhythmyx/services/sitemanage/site/");
  }

  public List<PSSiteSummary> findAll() {
    return getObjectsFromPath(getPath(), PSSiteSummary.class);
  }

  public PSEnumVals getChoices() {
    return getObjectFromPath(concatPath(getPath(), "choices"), PSEnumVals.class);
  }

  public PSSiteSummary find(String id) {
    return getObjectFromPath(concatPath(getPath(), "summary", id));
  }

  public PSSiteProperties getProperties(String siteName) {
    return getObjectFromPath(concatPath(getPath(), "properties", siteName), PSSiteProperties.class);
  }

  public PSSiteProperties updateProperties(PSSiteProperties props) {
    var resp = postObjectToPath(getPath() + "updateProperties", props);
    return objectFromResponseBody(resp, PSSiteProperties.class);
  }

  public PSSitePublishProperties getSitePublishProperties(String siteName) {
    return getObjectFromPath(
        concatPath(getPath(), "publishProperties", siteName), PSSitePublishProperties.class);
  }

  public PSSitePublishProperties updateSitePublishProperties(PSSitePublishProperties publishProps) {
    var resp = postObjectToPath(getPath() + "updatePublishProperties", publishProps);
    return objectFromResponseBody(resp, PSSitePublishProperties.class);
  }

  public PSSite copy(PSSiteCopyRequest req) {
    var resp = postObjectToPath(getPath() + "copy", req);
    return objectFromResponseBody(resp, PSSite.class);
  }

  @Override
  public String POST(String path, String body, String contentType) {
    return super.POST(path, body, contentType);
  }

  public String deleteSite(String id) {
    return super.DELETE(getPath() + id);
  }

  public long importSiteFromUrlAsync(PSSite site) {
    var response = postObjectToPath(concatPath(getPath(), "importFromUrlAsync"), site);
    return Long.parseLong(response);
  }

  public PSSite getImportedSite(Long jobId) {
    return getObjectFromPath(
        concatPath(getPath(), "getImportedSite", jobId.toString()), PSSite.class);
  }

  public PSSiteStatisticsSummary getSiteStatistics(String siteId) {
    return getObjectFromPath(
        concatPath(getPath(), "statistics", siteId), PSSiteStatisticsSummary.class);
  }
}
