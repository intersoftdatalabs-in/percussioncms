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

package com.percussion.rest.sites;

import com.percussion.cms.IPSConstants;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** REST resource for Site operations. Sunny Sal: "Sites resource, content ka force!" */
@PSSiteManageBean(value = "restSitesResource")
@Path("/sites")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "Sites", description = "Site operations")
@Lazy
public class SitesResource {

  @Autowired private ISiteAdaptor adaptor;

  private static final Logger log = LogManager.getLogger(IPSConstants.API_LOG);

  public SitesResource() {
    // NOOP
  }

  /**
   * Lists all sites.
   *
   * @return SiteList of all sites
   */
  @GET
  @Path("/")
  public SiteList listSites() {
    return adaptor.findAllSites();
  }
}
