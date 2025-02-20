/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.rest.contenttypes;

import com.percussion.util.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@PSSiteManageBean(value="restContentTypesResource")
@Path("/contenttypes")
@XmlRootElement
@Tag(name = "Content Types", description = "Content Type operations")
public class ContentTypesResource {

    @Autowired
    private IContentTypesAdaptor adaptor;

    @Context
    private UriInfo uriInfo;

    public ContentTypesResource(){}

    @GET
    @Path("/")
    @Produces(
            {MediaType.APPLICATION_JSON})
    @Operation(summary = "List available ContentTypes", description = "Lists all available Content Types on the system.  Not filtered by security."
            , responses = {
             @ApiResponse(responseCode = "200", description = "OK",
             content = @Content(array = @ArraySchema( schema = @Schema(implementation = ContentType.class)))),
             @ApiResponse(responseCode = "404", description = "No Content Types found"),
             @ApiResponse(responseCode = "500", description = "Error")
    })
    public List<ContentType> getContentTypes()
    {
        return new ContentTypeList(adaptor.listContentTypes(uriInfo.getBaseUri()));
    }

    @GET
    @Path("/by-site/{id}")
    @Produces(
            {MediaType.APPLICATION_JSON})
    @Operation(summary = "List available Content Types by Site", description = "Lists Content Types available for a site."
            , responses = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema( schema = @Schema(implementation = ContentType.class)))),
            @ApiResponse(responseCode = "404", description = "No Content Types found"),
            @ApiResponse(responseCode = "500", description = "Error")
    })
    public List<ContentType> getContentTypesBySite( @PathParam("id") int siteId)
    {
        return new ContentTypeList(adaptor.listContentTypes(uriInfo.getBaseUri(), siteId));
    }
}
