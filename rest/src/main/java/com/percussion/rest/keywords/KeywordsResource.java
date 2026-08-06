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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.keywords;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean(value = "restKeywordsResource")
@Path("/keywords")
@XmlRootElement
@Tag(name = "Keywords", description = "Keyword design catalog and edit")
public class KeywordsResource {

  private static final Logger log = LogManager.getLogger(KeywordsResource.class);

  private final IKeywordsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #KeywordsResource(IKeywordsAdaptor)}; methods call {@link #requireAdaptor()}.
   */
  public KeywordsResource() {
    this.adaptor = null;
  }

  @Autowired
  public KeywordsResource(IKeywordsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List keywords",
      description = "Lists keyword definitions. Set includeChoices=true to embed choice lists.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = KeywordSummary.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<KeywordSummary> listKeywords(
      @QueryParam("includeChoices") @DefaultValue("false") boolean includeChoices) {
    try {
      return requireAdaptor().listKeywords(uriInfo.getBaseUri(), includeChoices);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to list keywords ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrValue}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get keyword",
      description = "Load one keyword by uuid or value, including choices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = KeywordSummary.class))),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public KeywordSummary getKeyword(@PathParam("idOrValue") String idOrValue) {
    KeywordSummary kw;
    try {
      kw = requireAdaptor().getKeyword(uriInfo.getBaseUri(), idOrValue);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to load keyword {} ({}): {}",
          idOrValue,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
    if (kw == null) {
      throw new WebApplicationException("Keyword not found: " + idOrValue, 404);
    }
    return kw;
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create keyword",
      description = "Create a keyword. Label is required and must be unique.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = KeywordSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public KeywordSummary createKeyword(KeywordSummary body) {
    try {
      return requireAdaptor().createKeyword(uriInfo.getBaseUri(), body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      log.error("Failed to create keyword ({}): {}", e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{id}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update keyword",
      description = "Update keyword label/description/sequence/choices by uuid.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = KeywordSummary.class))),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public KeywordSummary updateKeyword(@PathParam("id") String id, KeywordSummary body) {
    KeywordSummary kw;
    try {
      kw = requireAdaptor().updateKeyword(uriInfo.getBaseUri(), id, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      log.error(
          "Failed to update keyword {} ({}): {}", id, e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
    if (kw == null) {
      throw new WebApplicationException("Keyword not found: " + id, 404);
    }
    return kw;
  }

  @DELETE
  @Path("/{id}")
  @Operation(
      summary = "Delete keyword",
      description = "Delete keyword and its choices by uuid.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteKeyword(@PathParam("id") String id) {
    try {
      requireAdaptor().deleteKeyword(uriInfo.getBaseUri(), id);
      return Response.noContent().build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (KeywordNotFoundException e) {
      throw new WebApplicationException(e.getMessage(), 404);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid keyword id", 400);
    } catch (Exception e) {
      log.error(
          "Failed to delete keyword {} ({}): {}", id, e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(e, 500);
    }
  }

  private IKeywordsAdaptor requireAdaptor() {
    if (adaptor == null) {
      // Misconfiguration — not a transient handler failure
      throw new WebApplicationException(
          "Keywords adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
