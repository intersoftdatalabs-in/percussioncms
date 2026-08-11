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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** REST resource for Site operations. Sunny Sal: "Sites resource, content ka force!" */
@PSSiteManageBean(value = "restSitesResource")
@Path("/sites")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "Sites", description = "Site operations including Virtual Site properties")
@Lazy
public class SitesResource {

  /**
   * Package-private and non-final so unit tests can install a mock {@link Logger}.
   */
  static Logger log = LogManager.getLogger(IPSConstants.API_LOG);

  private final ISiteAdaptor adaptor;

  public SitesResource() {
    this.adaptor = null;
  }

  @Autowired
  public SitesResource(ISiteAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /**
   * Lists all sites.
   *
   * @return SiteList of all sites
   */
  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "List sites",
      description = "Lists CMS Sites. Summary payloads may omit virtual property details.",
      responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SiteList listSites() {
    try {
      SiteList list = requireAdaptor().findAllSites();
      return list != null ? list : new SiteList();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to list sites ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Loads a site by name or GUID string, including Virtual Site properties when set.
   *
   * @param nameOrId site name or GUID
   * @return site detail
   */
  @GET
  @Path("/{nameOrId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Get site by name or GUID",
      description =
          "Returns site detail including virtual.* properties (sourceKind, rootPath, configFile,"
              + " siteKey) when configured.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Site.class))),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Site getSite(@PathParam("nameOrId") String nameOrId) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      Site site = resolveSite(nameOrId);
      if (site == null) {
        throw new WebApplicationException("Site not found: " + nameOrId, Response.Status.NOT_FOUND);
      }
      return site;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to get site '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Loads Virtual Site properties for a site.
   *
   * @param nameOrId site name or GUID
   * @return virtual properties (fields empty for traditional Sites)
   */
  @GET
  @Path("/{nameOrId}/virtual")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Get Virtual Site properties",
      description =
          "Returns virtual.sourceKind, virtual.rootPath, virtual.configFile, and virtual.siteKey"
              + " for the site. Traditional Sites return empty/default values with virtual=false.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = VirtualSiteProperties.class))),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSiteProperties getVirtualProperties(@PathParam("nameOrId") String nameOrId) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      return requireAdaptor().getVirtualSiteProperties(nameOrId);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to get virtual properties for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Creates or updates Virtual Site properties for a site.
   *
   * @param nameOrId site name or GUID
   * @param props properties to apply
   * @return persisted properties
   */
  @PUT
  @Path("/{nameOrId}/virtual")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Update Virtual Site properties",
      description =
          "Persists virtual.* properties. Validation aligns with PSVirtualSiteHelper: Phase 1"
              + " sourceKind allow-list (git-filesystem), required non-blank rootPath when"
              + " virtual, safe NIO path (no '..' after normalize), simple configFile name."
              + " Blank/repository sourceKind clears virtual configuration.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = VirtualSiteProperties.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSiteProperties updateVirtualProperties(
      @PathParam("nameOrId") String nameOrId, VirtualSiteProperties props) {
    requireNonBlank(nameOrId, "nameOrId");
    if (props == null) {
      throw new WebApplicationException(
          "VirtualSiteProperties body is required", Response.Status.BAD_REQUEST);
    }
    try {
      return requireAdaptor().updateVirtualSiteProperties(nameOrId, props);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to update virtual properties for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private Site resolveSite(String nameOrId) {
    ISiteAdaptor a = requireAdaptor();
    Site byName = a.findByName(nameOrId);
    if (byName != null) {
      return byName;
    }
    return a.findByGuid(nameOrId);
  }

  private ISiteAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Site adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }

  private static void requireNonBlank(String value, String field) {
    if (StringUtils.isBlank(value)) {
      throw new WebApplicationException(field + " is required", Response.Status.BAD_REQUEST);
    }
  }
}
