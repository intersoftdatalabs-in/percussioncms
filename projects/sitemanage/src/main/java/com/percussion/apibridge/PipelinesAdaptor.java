/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge;

import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.rest.pipelines.ApplicationSummary;
import com.percussion.rest.pipelines.IPipelinesAdaptor;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@PSSiteManageBean
public class PipelinesAdaptor implements IPipelinesAdaptor {

  private static final Logger log = LogManager.getLogger(PipelinesAdaptor.class);

  @Override
  public List<ApplicationSummary> listApplications(URI baseUri) {
    // baseUri reserved for HATEOAS link building (interface contract)
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    if (req == null) {
      throw new IllegalStateException("No current request for application catalog");
    }
    PSSecurityToken tok = req.getSecurityToken();
    PSApplicationSummary[] sums =
        PSServerXmlObjectStore.getInstance().getApplicationSummaryObjects(tok, false);
    List<ApplicationSummary> out = new ArrayList<>();
    if (sums != null) {
      for (PSApplicationSummary sum : sums) {
        if (sum == null) {
          continue;
        }
        try {
          out.add(toSummary(sum));
        } catch (Exception e) {
          log.debug(
              "Skipping application summary {}: {}", sum.getName(), e.getMessage());
        }
      }
    }
    out.sort(
        Comparator.comparing(
            a -> a.getName() != null ? a.getName() : "", String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  private static ApplicationSummary toSummary(PSApplicationSummary sum) {
    ApplicationSummary dto = new ApplicationSummary();
    dto.setId(sum.getId());
    dto.setName(sum.getName());
    dto.setDescription(sum.getDescription());
    dto.setEnabled(sum.isEnabled());
    dto.setAppRoot(sum.getAppRoot());
    if (sum.getAppType() != null) {
      dto.setAppType(sum.getAppType().name());
    }
    dto.setVersion(sum.getVersion());
    dto.setEmpty(sum.isEmpty());
    return dto;
  }
}
