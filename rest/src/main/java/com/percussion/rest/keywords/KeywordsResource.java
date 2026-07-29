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

package com.percussion.rest.keywords;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean(value = "restKeywordsResource")
@Path("/keywords")
@XmlRootElement
@Tag(name = "Keywords", description = "Keyword design catalog")
public class KeywordsResource {

  @Autowired private IKeywordsAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public KeywordsResource() {}

  @GET
  @Path("/")
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
      return adaptor.listKeywords(uriInfo.getBaseUri(), includeChoices);
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
      throw new WebApplicationException(e, 500);
    }
  }
}
