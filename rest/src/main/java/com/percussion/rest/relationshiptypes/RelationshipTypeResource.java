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

package com.percussion.rest.relationshiptypes;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only relationship type catalog for the Developer module (SY-03 list/detail).
 *
 * <p>Create/edit/delete remain later slices.
 */
@PSSiteManageBean(value = "restRelationshipTypeResource")
@Path("/relationshiptypes")
@XmlRootElement
@Tag(name = "Relationship Types", description = "Relationship type design catalog (read-only)")
public class RelationshipTypeResource {

  private final IRelationshipTypeAdaptor adaptor;

  public RelationshipTypeResource() {
    this.adaptor = null;
  }

  @Autowired
  public RelationshipTypeResource(IRelationshipTypeAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List relationship types",
      description =
          "Lists system and user relationship types with category, properties, and effects."
              + " Create/edit/delete are later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = RelationshipType.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<RelationshipType> listRelationshipTypes() {
    try {
      List<RelationshipType> list = requireAdaptor().listRelationshipTypes();
      return list != null ? list : List.of();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get relationship type detail",
      description =
          "Loads one relationship type by name or GUID string (type-host-uuid). Write remains"
              + " unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RelationshipType.class))),
        @ApiResponse(responseCode = "404", description = "Relationship type not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public RelationshipType getRelationshipType(@PathParam("idOrName") String idOrName) {
    try {
      RelationshipType type = requireAdaptor().findRelationshipType(idOrName);
      if (type == null) {
        throw new WebApplicationException("Relationship type not found", 404);
      }
      return type;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IRelationshipTypeAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Relationship type adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
