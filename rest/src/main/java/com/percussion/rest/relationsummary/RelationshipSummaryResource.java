/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.rest.relationsummary;

import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST surface (US8 / T098) for the modern Content Explorer's DependencyViewer and
 * RelationshipsView. Five typed GET endpoints + one consolidated summary endpoint:
 *
 * <pre>
 *   GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/outgoing
 *   GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/incoming
 *   GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/taxonomy
 *   GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/local
 *   GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/reverse
 *   GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/summary
 * </pre>
 *
 * <p>All endpoints are read-only (GETs are CSRF-exempt). AuthZ is enforced server-side by the
 * underlying sitemanage service: id-resolution failure or read-access denial returns {@code
 * Optional.empty()}, which the sitemanage apibridge adaptor surfaces as a JAX-RS {@code
 * WebApplicationException} with status {@code 403 FORBIDDEN}. No exception mapper is required — the
 * framework translates the status code automatically.
 *
 * <p>Bean id {@code restRelationshipSummaryResource} matches the CXF server registration at {@code
 * projects/sitemanage-beans.xml}. Wiring is auto-discovered via {@code @PSSiteManageBean} + {@code
 * component-scan}; no manual bean definition is required.
 *
 * @author Kilo (US8 / T098)
 */
@PSSiteManageBean(value = "restRelationshipSummaryResource")
@Path("/content-explorer/relationships")
@Tag(
    name = "Content Explorer Relationships",
    description = "Relationship summary for the modern Content Explorer")
public class RelationshipSummaryResource {

  private final IRelationshipSummaryAdaptor adaptor;

  @Context private UriInfo uriInfo;

  @Autowired
  public RelationshipSummaryResource(IRelationshipSummaryAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /**
   * Setter used by the test harness; the runtime JAX-RS service injects the same field via
   * {@code @Context} and never calls this setter.
   */
  public void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Path("/{itemId}/outgoing")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Outgoing relationships for an item",
      description =
          "Counts the outgoing translation / linkback relationships for the supplied item id.")
  @ApiResponse(responseCode = "200", description = "OK — outgoing summary returned")
  @ApiResponse(responseCode = "403", description = "Caller cannot read the item")
  public Response outgoing(
      @Parameter(name = "itemId", required = true) @PathParam("itemId") String itemId) {
    URI base = uriInfo == null ? null : uriInfo.getBaseUri();
    PSRelationshipSummary body = adaptor.outgoing(base, itemId);
    return Response.ok(body).build();
  }

  @GET
  @Path("/{itemId}/incoming")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Incoming AA / linkback for an item",
      description =
          "Counts the incoming active-assembly / linkback relationships for the supplied item id.")
  @ApiResponse(responseCode = "200", description = "OK — incoming summary returned")
  @ApiResponse(responseCode = "403", description = "Caller cannot read the item")
  public Response incoming(
      @Parameter(name = "itemId", required = true) @PathParam("itemId") String itemId) {
    URI base = uriInfo == null ? null : uriInfo.getBaseUri();
    PSRelationshipSummary body = adaptor.incoming(base, itemId);
    return Response.ok(body).build();
  }

  @GET
  @Path("/{itemId}/taxonomy")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Taxonomy / site edges for an item",
      description = "Counts the taxonomy / site edges incident on the supplied item id.")
  @ApiResponse(responseCode = "200", description = "OK — taxonomy summary returned")
  @ApiResponse(responseCode = "403", description = "Caller cannot read the item")
  public Response taxonomy(
      @Parameter(name = "itemId", required = true) @PathParam("itemId") String itemId) {
    URI base = uriInfo == null ? null : uriInfo.getBaseUri();
    // TODO(site-finder-folder-mapping): the taxonomy dimension passes the supplied id
    // straight through to the sitemanage service. Callers that pass a content id or
    // guid rather than a folder path get a 403 today because the JCR finder resolves
    // paths only. Path resolution is the rest-facade's responsibility (per the PR #1416
    // review); a follow-up commit adds an id-to-path mapping layer here.
    PSTaxonomySummary body = adaptor.taxonomy(base, itemId);
    return Response.ok(body).build();
  }

  @GET
  @Path("/{itemId}/local")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Local page-assembly edges for a page or template",
      description = "Counts the local + linked assets incident on the supplied page / template id.")
  @ApiResponse(responseCode = "200", description = "OK — local-dependency summary returned")
  @ApiResponse(responseCode = "403", description = "Caller cannot read the item")
  public Response local(
      @Parameter(name = "itemId", required = true) @PathParam("itemId") String itemId) {
    URI base = uriInfo == null ? null : uriInfo.getBaseUri();
    PSLocalDependencySummary body = adaptor.local(base, itemId);
    return Response.ok(body).build();
  }

  @GET
  @Path("/{itemId}/reverse")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Reverse-dependency count for an item",
      description = "Counts the reverse (incoming + inline-link parents) for the supplied item id.")
  @ApiResponse(responseCode = "200", description = "OK — reverse-dependency summary returned")
  @ApiResponse(responseCode = "403", description = "Caller cannot read the item")
  public Response reverse(
      @Parameter(name = "itemId", required = true) @PathParam("itemId") String itemId) {
    URI base = uriInfo == null ? null : uriInfo.getBaseUri();
    PSRelationshipSummary body = adaptor.reverse(base, itemId);
    return Response.ok(body).build();
  }

  @GET
  @Path("/{itemId}/summary")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Consolidated per-node relationship summary",
      description =
          "Single-shot summary consumed by the modern Content Explorer's DependencyViewer /"
              + " RelationshipsView.")
  @ApiResponse(responseCode = "200", description = "OK — consolidated summary returned")
  @ApiResponse(responseCode = "403", description = "Caller cannot read the item")
  public Response summary(
      @Parameter(name = "itemId", required = true) @PathParam("itemId") String itemId) {
    URI base = uriInfo == null ? null : uriInfo.getBaseUri();
    PSNodeRelationshipSummary body = adaptor.summary(base, itemId);
    return Response.ok(body).build();
  }
}
