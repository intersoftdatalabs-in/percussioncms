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
import jakarta.ws.rs.core.Response;
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
          "Template detail including bindings and slots. Source is included when present."
              + " Create is POST /templates. Delete is DELETE /templates/{idOrName}."
              + " Lock remains unsupported (see designGaps).",
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
          "Updates mutable template fields: label, description, templateSource, and/or"
              + " assembler. When assembler is present it must be non-blank. When bindings or"
              + " slots is present (including empty), replaces that collection. Omit fields to"
              + " leave unchanged. Name/id remain unsupported. Delete is DELETE"
              + " /templates/{idOrName}. Lock remains unsupported (see designGaps).",
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

  /**
   * Creates a modern assembly template (no Widget definition XML).
   *
   * @param body TemplateDetail; {@code name} required
   * @return created TemplateDetail
   */
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create assembly template",
      description =
          "Creates a template via the modern assembly catalog (package/manifest). Name is"
              + " required and must be unique with no spaces. Optional label, description,"
              + " assembler (defaults to HTML-first), and templateSource. Does not write Widget"
              + " definition XML.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = TemplateDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate name"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public TemplateDetail createTemplate(TemplateDetail body) {
    try {
      TemplateDetail detail = adaptor.createTemplate(uriInfo.getBaseUri(), body);
      if (detail == null) {
        throw new WebApplicationException("Failed to create template", 500);
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

  /**
   * Deletes an assembly template (no Widget definition XML).
   *
   * @param idOrName template uuid or unique name
   * @return 204 when deleted
   */
  @DELETE
  @Path("/{idOrName}")
  @Operation(
      summary = "Delete assembly template",
      description =
          "Deletes a template from the modern assembly catalog (package/manifest). Does not"
              + " write Widget definition XML.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response deleteTemplate(@PathParam("idOrName") String idOrName) {
    try {
      boolean deleted = adaptor.deleteTemplate(uriInfo.getBaseUri(), idOrName);
      if (!deleted) {
        throw new WebApplicationException("Template not found: " + idOrName, 404);
      }
      return Response.noContent().build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Downloads Workbench-equivalent assembly-template design XML (AS-08 export). Import is a later
   * slice. Read-only: does not acquire or steal design locks.
   *
   * @param idOrName template uuid or unique name
   * @return XML attachment named from the template name
   */
  @GET
  @Path("/{idOrName}/export")
  @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
  @Operation(
      summary = "Export assembly template design XML",
      description =
          "Admin-only AS-08 export of one assembly template as Workbench-equivalent design"
              + " XML (loaded via IPSAssemblyDesignWs). Read-only: does not acquire or steal"
              + " locks. Import is not implemented on this path. Content-Disposition filename"
              + " is derived from the template name.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Design XML",
            content = @Content(mediaType = MediaType.APPLICATION_XML)),
        @ApiResponse(responseCode = "403", description = "Caller is not Admin"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response exportTemplate(@PathParam("idOrName") String idOrName) {
    try {
      TemplateExport exported = adaptor.exportTemplate(uriInfo.getBaseUri(), idOrName);
      if (exported == null || exported.getXml() == null) {
        throw new WebApplicationException("Template not found: " + idOrName, 404);
      }
      String filename = exportFilename(exported.getName());
      return Response.ok(exported.getXml(), MediaType.APPLICATION_XML)
          .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
          .build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Basename for {@code Content-Disposition}. Strips characters that are unsafe in HTTP filenames
   * (quotes, controls, path separators). Not a filesystem path.
   */
  static String exportFilename(String templateName) {
    String raw = templateName == null ? "" : templateName.trim();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c <= 31 || c == 127 || c == '"' || c == '\\' || c == '/' || c == ':') {
        sb.append('_');
      } else {
        sb.append(c);
      }
    }
    String base = sb.toString().trim();
    if (base.isEmpty()) {
      base = "template";
    }
    if (!base.regionMatches(true, base.length() - 4, ".xml", 0, 4)) {
      base = base + ".xml";
    }
    return base;
  }
}
