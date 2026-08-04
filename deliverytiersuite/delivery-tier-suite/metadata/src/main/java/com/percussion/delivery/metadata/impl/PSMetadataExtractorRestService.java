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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataIndexerService;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;
import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import com.percussion.delivery.metadata.extractor.data.PSMetadataProperty;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST/Webservice layer for metadata extractor services.
 *
 * @author miltonpividori
 */
@Path("/indexer")
@Component
public class PSMetadataExtractorRestService {
  /** Class-level logger. */
  public static final Logger log = LogManager.getLogger(PSMetadataExtractorRestService.class);

  private final PSPropertyDatatypeMappings datatypeMappings;
  private IPSMetadataIndexerService indexer;

  /**
   * Resolves the {@code XSRF-TOKEN} cookie on the inbound request and copies the value into the
   * response headers so the browser-side framework can pick it up.
   *
   * @param request the inbound HTTP request; may be {@code null}.
   * @param response the outbound HTTP response; may be {@code null}.
   */
  @HEAD
  @Path("/csrf")
  public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }
    for (Cookie cookie : cookies) {
      if ("XSRF-TOKEN".equals(cookie.getName())) {
        response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
        response.setHeader("X-CSRF-TOKEN", cookie.getValue());
      }
    }
  }

  /**
   * Constructs the REST resource with the supplied dependencies.
   *
   * @param indexer the indexer service used to persist incoming metadata entries; may not be {@code
   *     null}.
   * @param datatypeMappings the property datatype mappings used to resolve each property's {@link
   *     VALUETYPE}; may not be {@code null}.
   */
  @Inject
  @Autowired
  public PSMetadataExtractorRestService(
      IPSMetadataIndexerService indexer, PSPropertyDatatypeMappings datatypeMappings) {
    this.indexer = indexer;
    this.datatypeMappings = datatypeMappings;
  }

  /**
   * Indexes a single metadata entry under the supplied page path.
   *
   * @param headers the inbound request headers; may be {@code null}.
   * @param path the page path under which the entry should be indexed; may not be {@code null}.
   * @param entry the metadata entry to persist; may be {@code null} to skip the request.
   */
  @Path("/entry/{pagePath:.*}")
  @POST
  @RolesAllowed("deliverymanager")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public void index(
      @Context HttpHeaders headers, @PathParam("pagePath") String path, PSMetadataEntry entry) {
    log.debug("Indexing file: {}", path);
    try {
      String contentType = headers.getMediaType().toString();
      log.debug("Content type: {}", contentType);

      if (entry != null) {
        // Get property value type

        for (IPSMetadataProperty ipsMetadataProperty : entry.getProperties()) {
          PSMetadataProperty prop = (PSMetadataProperty) ipsMetadataProperty;

          // Namespace prefix is not being used in datatype lookup.

          String testval = prop.getName();
          int indx = testval.indexOf(':');
          if (indx > 0) testval = testval.substring(indx + 1);

          VALUETYPE propertyValueType = datatypeMappings.getDatatype(testval);
          prop.setValuetype(propertyValueType);
        }
        indexer.save(entry);
      } else log.debug("File has no metadata (no link tags)");
    } catch (Exception e) {
      log.error("An error when saving index {}", PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /**
   * Deletes the metadata entry stored under the supplied page path.
   *
   * @param path the page path whose entry should be removed; may not be {@code null}.
   */
  @Path("/entry/{pagePath:.*}")
  @DELETE
  @RolesAllowed("deliverymanager")
  public void delete(@PathParam("pagePath") String path) {
    log.debug("Deleting file: {}", path);

    try {
      indexer.delete(Collections.singleton(path));
      // TODO: implement blogPostService.delete here
    } catch (Exception e) {
      log.error("An error when deleting the file {}", PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }
}
