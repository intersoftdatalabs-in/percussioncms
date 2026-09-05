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

package com.percussion.rest.velocity;

import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionFilterOptions;
import com.percussion.rest.extensions.ExtensionList;
import com.percussion.rest.extensions.IExtensionAdaptor;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Velocity template authoring REST surface.
 *
 * <p>{@code GET /tools} lists registered Velocity extensions. {@code GET /snippets} returns the
 * built-in macro snippet catalog for template authors (AS-09). SPA insert chrome is a sibling
 * slice; this resource is REST only.
 */
@PSSiteManageBean(value = "restVelocityResource")
@Path("/velocity")
@XmlRootElement
@Tag(name = "Velocity Template Engine", description = "Velocity related operations")
public class VelocityResource {

  private final IExtensionAdaptor extensionAdaptor;
  private final IVelocityAdaptor velocityAdaptor;

  @Context private UriInfo uriInfo;

  /** No-arg constructor for bean-discovery edge cases. */
  public VelocityResource() {
    this.extensionAdaptor = null;
    this.velocityAdaptor = null;
  }

  @Autowired
  public VelocityResource(IExtensionAdaptor extensionAdaptor, IVelocityAdaptor velocityAdaptor) {
    this.extensionAdaptor = extensionAdaptor;
    this.velocityAdaptor = velocityAdaptor;
  }

  /** Returns a list of all registered Velocity extensions on the system. */
  @GET
  @Path("/tools")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Returns a list of all registered Velocity extensions on the System",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = Extension.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<Extension> listVelocityExtensions() {
    try {
      var filter = new ExtensionFilterOptions();
      filter.setContext("global/percussion/velocity/");
      return new ExtensionList(
          requireExtensionAdaptor().getExtensions(uriInfo.getBaseUri(), filter));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Lists the built-in Velocity snippet catalog (Appendix C field/slot/misc macros and samples).
   */
  @GET
  @Path("/snippets")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List built-in Velocity snippets",
      description =
          "Returns the AS-09 snippet library for common Velocity macros (field, slot, and misc)."
              + " Each entry includes a stable id, title, category, and insert text for template"
              + " authors. Does not edit System/User Velocity config files (SY-02).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = VelocitySnippet.class)))),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<VelocitySnippet> listSnippets() {
    try {
      List<VelocitySnippet> list = requireVelocityAdaptor().listSnippets();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /** Loads one built-in Velocity snippet by stable catalog id. */
  @GET
  @Path("/snippets/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get built-in Velocity snippet by id",
      description = "Resolves one AS-09 catalog entry by stable id (case-insensitive).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = VelocitySnippet.class))),
        @ApiResponse(responseCode = "404", description = "Snippet not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VelocitySnippet getSnippet(@PathParam("id") String id) {
    try {
      VelocitySnippet snippet = requireVelocityAdaptor().findSnippetById(id);
      if (snippet == null) {
        throw new WebApplicationException("Snippet not found", 404);
      }
      return snippet;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IExtensionAdaptor requireExtensionAdaptor() {
    if (extensionAdaptor == null) {
      throw new WebApplicationException(
          "Extension adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return extensionAdaptor;
  }

  private IVelocityAdaptor requireVelocityAdaptor() {
    if (velocityAdaptor == null) {
      throw new WebApplicationException(
          "Velocity adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return velocityAdaptor;
  }
}
