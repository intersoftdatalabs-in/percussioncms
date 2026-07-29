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

package com.percussion.apibridge;

import com.percussion.rest.templates.ITemplatesAdaptor;
import com.percussion.rest.templates.TemplateFilter;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Adaptor for managing templates in Percussion CMS. */
@PSSiteManageBean
public class TemplateAdaptor implements ITemplatesAdaptor {

  private final IPSAssemblyService asmSvc = PSAssemblyServiceLocator.getAssemblyService();
  private final IPSContentWs contentwsService = PSContentWsLocator.getContentWebservice();

  public TemplateAdaptor() {
    // No-op constructor for dependency injection.
  }

  @Override
  public List<TemplateSummary> listAllTemplateSummaries(URI baseUri) {
    try {
      var summaries = asmSvc.getSummaries(PSTypeEnum.TEMPLATE);
      return summaries.stream().map(ApiUtils::convertTemplateSummary).collect(Collectors.toList());
    } catch (PSCatalogException e) {
      throw new WebApplicationException(e.getMessage(), 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }

  /**
   * Returns all template summaries that match the supplied filter. NOTE: Currently only contentId
   * is implemented. TODO: Implement for all filter options.
   */
  @Override
  public List<TemplateSummary> listTemplateSummaries(URI baseUri, TemplateFilter filter) {
    if (filter == null) {
      throw new IllegalArgumentException("TemplateFilter cannot be null");
    }
    var ret = new ArrayList<TemplateSummary>();
    int contentID = filter.getContentId();

    try {
      var guids = new ArrayList<IPSGuid>();
      guids.add(PSGuidManagerLocator.getGuidMgr().makeGuid(contentID, PSTypeEnum.LEGACY_CONTENT));
      var items = contentwsService.loadItems(guids, false, false, false, false);
      if (items != null && !items.isEmpty()) {
        var item = items.get(0);
        long contentTypeId = item.getContentTypeId();
        var ctypeGuid =
            PSGuidManagerLocator.getGuidMgr().makeGuid(contentTypeId, PSTypeEnum.NODEDEF);
        var templates = asmSvc.findTemplatesByContentType(ctypeGuid);

        ret.addAll(
            templates.stream().map(ApiUtils::convertTemplateSummary).collect(Collectors.toList()));
        return ret;
      } else {
        throw new PSNotFoundException("Content Id: " + contentID + " not found.");
      }
    } catch (PSAssemblyException | PSErrorResultsException e) {
      throw new WebApplicationException(e.getMessage(), 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }
}
