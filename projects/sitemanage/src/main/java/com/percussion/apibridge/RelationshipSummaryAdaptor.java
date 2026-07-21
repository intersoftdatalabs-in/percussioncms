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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.apibridge;

import com.percussion.rest.relationsummary.IRelationshipSummaryAdaptor;
import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import com.percussion.share.relationship.service.IPSRelationshipSummaryService;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default adaptor impl for {@link IRelationshipSummaryAdaptor} (US8 / T098).
 *
 * <p>Lives in sitemanage {@code apibridge} so {@code rest} depends only on its own DTOs/interfaces
 * (no rest → sitemanage reactor edge). Delegates to {@link IPSRelationshipSummaryService}; converts
 * the empty-{@code Optional} AuthZ-denied response into a {@link WebApplicationException} with HTTP
 * 403 so the JAX-RS runtime translates it to a {@code 403 Forbidden} response without an additional
 * exception mapper.
 *
 * @author Kilo (US8 / T098)
 */
@PSSiteManageBean
public class RelationshipSummaryAdaptor implements IRelationshipSummaryAdaptor {

  private final IPSRelationshipSummaryService service;

  @Autowired
  public RelationshipSummaryAdaptor(IPSRelationshipSummaryService service) {
    this.service = service;
  }

  @Override
  public PSRelationshipSummary outgoing(URI baseURI, String itemId) {
    return service
        .summariseOutgoing(itemId)
        .orElseThrow(() -> forbidden("Cannot summarise outgoing for " + itemId));
  }

  @Override
  public PSRelationshipSummary incoming(URI baseURI, String itemId) {
    return service
        .summariseIncoming(itemId)
        .orElseThrow(() -> forbidden("Cannot summarise incoming for " + itemId));
  }

  @Override
  public PSTaxonomySummary taxonomy(URI baseURI, String itemId) {
    return service
        .summariseTaxonomy(itemId)
        .orElseThrow(() -> forbidden("Cannot summarise taxonomy for " + itemId));
  }

  @Override
  public PSLocalDependencySummary local(URI baseURI, String itemId) {
    return service
        .summariseLocal(itemId)
        .orElseThrow(() -> forbidden("Cannot summarise local for " + itemId));
  }

  @Override
  public PSRelationshipSummary reverse(URI baseURI, String itemId) {
    return service
        .summariseReverse(itemId)
        .orElseThrow(() -> forbidden("Cannot summarise reverse for " + itemId));
  }

  @Override
  public PSNodeRelationshipSummary summary(URI baseURI, String itemId) {
    return service
        .summarise(itemId)
        .orElseThrow(() -> forbidden("Cannot summarise node " + itemId));
  }

  /**
   * Build a JAX-RS exception that the framework translates to {@code HTTP 403 Forbidden} with the
   * supplied message body. The sitemanage service returns {@code Optional.empty()} on id-resolution
   * failure or read-access denial; this surfaces that condition in the wire shape.
   */
  private static WebApplicationException forbidden(String message) {
    return new WebApplicationException(message, Response.Status.FORBIDDEN);
  }
}
