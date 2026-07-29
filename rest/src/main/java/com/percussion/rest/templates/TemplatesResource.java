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

package com.percussion.rest.templates;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/** REST resource for Template operations. Sunny Sal: "Templates resource, assembly ka force!" */
@PSSiteManageBean(value = "restTemplatesResource")
@Path("/templates")
@XmlRootElement
@Tag(name = "Templates", description = "Template operations")
public class TemplatesResource {

  private final ITemplatesAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public TemplatesResource() {
    this.adaptor = null;
  }

  @Autowired
  public TemplatesResource(ITemplatesAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /**
   * Lists all template summaries for design-time catalog.
   *
   * @return TemplateSummaryList of all templates
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List all templates",
      description = "Lists all assembly templates as summaries (design catalog).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = TemplateSummary.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public TemplateSummaryList listAllTemplates() {
    try {
      var summaries =
          Optional.ofNullable(adaptor.listAllTemplateSummaries(uriInfo.getBaseUri()))
              .orElse(List.of());
      return new TemplateSummaryList(summaries);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Lists available Templates by Filter.
   *
   * @param filter TemplateFilter to use for listing TemplateSummaries.
   * @return TemplateSummaryList of matching templates.
   */
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/summaries-by-filter")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List available Templates by Filter",
      description = "Lists Templates available for a given filter.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = TemplateSummary.class),
                            arraySchema = @Schema(implementation = TemplateSummaryList.class)),
                    examples =
                        @ExampleObject(
                            value =
                                "{\n"
                                    + "  \"TemplateSummaryList\": [\n"
                                    + "    {\n"
                                    + "      \"templateDescription\": \"Template used to render the"
                                    + " page as XML.\",\n"
                                    + "      \"templateId\": 347,\n"
                                    + "      \"templateLabel\": \"perc.pageXml\",\n"
                                    + "      \"templateName\": \"perc.pageXml\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"templateId\": 343,\n"
                                    + "      \"templateLabel\": \"perc.title\",\n"
                                    + "      \"templateName\": \"perc.pageDatabase\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"templateDescription\": \"The dispatch template for"
                                    + " page assembly\",\n"
                                    + "      \"templateId\": 345,\n"
                                    + "      \"templateLabel\": \"Percussion Page Dispatch\",\n"
                                    + "      \"templateName\": \"perc.pageDispatcher\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"templateId\": 335,\n"
                                    + "      \"templateLabel\": \"Plain\",\n"
                                    + "      \"templateName\": \"perc.base.plain\"\n"
                                    + "    }\n"
                                    + "  ]\n"
                                    + "}"))),
        @ApiResponse(responseCode = "404", description = "No Templates found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public TemplateSummaryList listTemplateSummariesByFilter(
      @RequestBody(
              description = "TemplateFilter to use for listing TemplateSummaries.",
              required = true,
              content =
                  @Content(
                      schema = @Schema(implementation = TemplateFilter.class),
                      examples =
                          @ExampleObject(value = "{\"TemplateFilter\":{\"contentId\":\"27308\"}}")))
          @Valid
          TemplateFilter filter) {
    try {
      var summaries =
          Optional.ofNullable(adaptor.listTemplateSummaries(uriInfo.getBaseUri(), filter))
              .orElse(List.of());
      if (summaries.isEmpty()) {
        throw new WebApplicationException("Not Found.", 404);
      }
      return new TemplateSummaryList(summaries);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get template design detail",
      description =
          "Read-only template detail including bindings and slots. Source is included when"
              + " present. Create/update/delete/lock not supported (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = TemplateDetail.class))),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public TemplateDetail getTemplate(@PathParam("idOrName") String idOrName) {
    try {
      TemplateDetail detail = adaptor.getTemplate(uriInfo.getBaseUri(), idOrName);
      if (detail == null) {
        throw new WebApplicationException("Template not found: " + idOrName, 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update template design fields",
      description =
          "Updates mutable template fields: label, description, and/or templateSource."
              + " Name and identity are immutable. Bindings, slots, create/delete/lock remain"
              + " unsupported (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(schema = @Schema(implementation = TemplateDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public TemplateDetail updateTemplate(
      @PathParam("idOrName") String idOrName, TemplateDetail body) {
    try {
      TemplateDetail detail = adaptor.updateTemplate(uriInfo.getBaseUri(), idOrName, body);
      if (detail == null) {
        throw new WebApplicationException("Template not found: " + idOrName, 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }
}
