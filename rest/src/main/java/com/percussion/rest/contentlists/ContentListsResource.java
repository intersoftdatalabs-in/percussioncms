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

package com.percussion.rest.contentlists;

import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionFilterOptions;
import com.percussion.rest.extensions.ExtensionList;
import com.percussion.rest.extensions.IExtensionAdaptor;
import com.percussion.util.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@PSSiteManageBean(value="restContentListResource")
@Path("/contentlists")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "Content Lists", description = "Content List operations")
public class ContentListsResource {

    private IContentListsAdaptor adaptor;
    private IExtensionAdaptor extensionAdaptor;

    @Context
    private UriInfo uriInfo;

    public ContentListsResource(){}

    @GET
    @Path("/{id}")
    @Produces(
            {MediaType.APPLICATION_JSON})
    @Operation(summary = "Get a ContentList by id. Will return a ContentList if it exists."
            , responses = {
            @ApiResponse(responseCode="200", description="OK", content = @Content(
                   schema=@Schema(implementation = ContentList.class))
            ),
            @ApiResponse(responseCode="500", description = "Error")})
    public ContentList getContentListById(@PathParam("id") @Parameter(name = "id", description = "The id of the Content List to lookup.") long id) {
        return adaptor.getContentListById(id);
    }

    /***
     * Get a list of available ContentListGenerators on the system.
     * @return A list of ContentListGenerators
     */
    @GET
    @Path("/generators")
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Get a list of available Content List Generators.  Will return a list of all Content List Generators registered on the system"
            , responses = {
            @ApiResponse(responseCode="200", description="OK", content=@Content(
                    array = @ArraySchema(schema=@Schema(implementation = Extension.class))
            )),
            @ApiResponse(responseCode = "500", description = "Error")})
    public List<Extension> getContentListGenerators() {
        ExtensionFilterOptions filter = new ExtensionFilterOptions();

        filter.setInterfacePattern("com.percussion.services.publisher.IPSContentListGenerator");
        return new ExtensionList(extensionAdaptor.getExtensions(uriInfo.getBaseUri(), filter));
    }

    /***
     * Get a list of available TemplateExpanders on the system.
     * @return
     */
    @GET
    @Path("/expanders")
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Get a list of available Template Expanders.  Will return a list of all Content List Template Expanders registered on the system",
            responses = {
            @ApiResponse(responseCode="200", description="OK", content=@Content(
                    array = @ArraySchema(schema=@Schema(implementation = Extension.class))
            )),
            @ApiResponse(responseCode = "500", description = "Error")})
    public List<Extension> getTemplateExpanders() {
        ExtensionFilterOptions filter = new ExtensionFilterOptions();
        filter.setInterfacePattern("com.percussion.services.publisher.IPSTemplateExpander");
        return new ExtensionList(extensionAdaptor.getExtensions(uriInfo.getBaseUri(), filter));
    }

    /***
     * Get  a list of content lists for the specified edition.
     * @param editionId
     * @return
     */
    @GET
    @Path("/by-edition/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Get a list of Content Lists defined for the specified Edition.  Will return a list of all Content Lists linked to the specified Edition",
            responses = {
            @ApiResponse(responseCode="200", description="OK", content=@Content(
                    array = @ArraySchema(schema=@Schema(implementation = ContentList.class))
            )),
            @ApiResponse(responseCode = "500", description = "Error")})
    public List<ContentList> getContentListsByEditionId(@PathParam(value = "id") @Parameter(name = "id", description = "The id of the Edition to retrieve content lists for.") long editionId) {
        return new ContentListList(adaptor.getContentListsByEditionId(editionId));
    }

    /***
     * Get a list of content lists that are currently unused.
     * @return A list of unused ContentLists
     */
    @GET
    @Path("/unused/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Get a list of Content Lists defined for the specified Edition.  Will return a list of all Content Lists linked to the specified Edition",
            responses = {
                    @ApiResponse(responseCode="200", description="OK", content=@Content(
                            array = @ArraySchema(schema=@Schema(implementation = ContentList.class))
                    )),
                    @ApiResponse(responseCode = "500", description = "Error")})
    public List<ContentList> getUnusedContentLists(@PathParam(value = "id") @Parameter(name = "id", description = "A valid Site ID") long siteId) {
        return new ContentListList(adaptor.getUnusedContentLists(siteId));
    }

    /***
     * Create or update a ContentList
     * @param cl
     * @return The updated content list
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    @Operation(summary = "Create or Update the specified ContentList.  Will return the updated ContentList",
            responses = {
            @ApiResponse(responseCode="200", description="OK", content = @Content(
                    schema=@Schema(implementation = ContentList.class))
            ),
            @ApiResponse(responseCode="500", description = "Error")})
    public ContentList createOrUpdateContentList(@Parameter(allowEmptyValue = false, name = "body") ContentList cl) {
        return adaptor.createOrUpdateContentList(cl);
    }

    /***
     * Delete the specified content list.
     * @param id
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes the ContentList with the specified id")
    public void deleteContentList(@PathParam(value = "id") @Parameter(name = "id", required = true, description = "The id of the ContentList to delete.  Must exist.") long id) {
        adaptor.deleteContentList(id);
    }


}
