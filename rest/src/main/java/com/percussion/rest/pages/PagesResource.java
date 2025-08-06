// REFACTORED: CP-JAVA11

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

package com.percussion.rest.pages;

import com.percussion.error.PSExceptionUtils;
import com.percussion.rest.Status;
import com.percussion.rest.assets.PSCSVStreamingOutput;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.errors.LocationMismatchException;
import com.percussion.rest.util.APIUtilities;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST resource for Page operations.
 * Sunny Sal: "Page operations? Bas yahi toh mera kaam hai!"
 */
@PSSiteManageBean(value = "restPagesResource")
@Path("/pages")
@XmlRootElement
@Tag(name = "Pages", description = "Page Operations")
public class PagesResource {

    private static final Logger log = LogManager.getLogger(PagesResource.class);

    @Autowired
    private IPageAdaptor pageAdaptor;

    @Context
    private UriInfo uriInfo;

    private final Pattern pathPattern = Pattern.compile("^\\/?([^\\/]+)(\\/(.*?))??(\\/([^\\/]+))?$");

    /**
     * Retrieves a page by its unique id.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Page getPageById(@PathParam("id") String id) {
        try {
            return pageAdaptor.getPage(uriInfo.getBaseUri(), id);
        } catch (BackendException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Retrieves a page by site, path, and page name.
     */
    @GET
    @Path("/by-path/{pagepath:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve page by site, path, and pagename",
        description = "Get page with site name, path, and page name. Example: /pages/by-path/MySite/FolderA/FolderB/MyPage",
        responses = {
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Page.class)))
        }
    )
    public Page getPage(
            @Parameter(description = "The path from the site to the page.", name = "pagepath")
            @PathParam("pagepath") String path) {
        path = decodePath(path);
        var matcher = pathPattern.matcher(path);
        var siteName = "";
        var pageName = "";
        var apiPath = "";
        if (matcher.matches()) {
            siteName = StringUtils.defaultString(matcher.group(1));
            apiPath = StringUtils.defaultString(matcher.group(3));
            pageName = StringUtils.defaultString(matcher.group(5));
        }
        try {
            return pageAdaptor.getPage(uriInfo.getBaseUri(), siteName, apiPath, pageName);
        } catch (BackendException | PSDataServiceException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Creates or updates a page by site, path, and page name.
     */
    @PUT
    @Path("/by-path/{pagepath:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create or Update page by site, path, and pagename",
        description = "Create or Update page using site name, path, and page name. Example: /pages/by-path/MySite/FolderA/FolderB/MyPage",
        responses = {
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Page.class)))
        }
    )
    public Page updatePage(
            @Parameter(description = "The body containing a JSON payload", name = "body") Page page,
            @Parameter(description = "The path from the site to the page.", name = "pagepath") @PathParam("pagepath") String path) {
        path = decodePath(path);
        var matcher = pathPattern.matcher(path);
        var siteName = "";
        var pageName = "";
        var apiPath = "";
        if (matcher.matches()) {
            siteName = StringUtils.defaultString(matcher.group(1));
            apiPath = StringUtils.defaultString(matcher.group(3));
            pageName = StringUtils.defaultString(matcher.group(5));
        }

        var objectName = page.getName().orElse(null);
        var objectPath = page.getFolderPath().orElse(null);
        var objectSite = page.getSiteName().orElse(null);

        if (pageName == null || (objectName != null && !objectName.equals(pageName))) {
            throw new LocationMismatchException();
        }
        if (objectPath != null && !objectPath.equals(apiPath)) {
            throw new LocationMismatchException();
        }
        if (siteName == null || (objectSite != null && !objectSite.equals(siteName))) {
            throw new LocationMismatchException();
        }
        page.setName(pageName);
        page.setFolderPath(apiPath);
        page.setSiteName(siteName);
        try {
            return pageAdaptor.updatePage(uriInfo.getBaseUri(), page);
        } catch (BackendException | PSDataServiceException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Renames a page.
     */
    @POST
    @Path("/rename/{pagepath:.+}/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Rename a page.",
        description = "Rename a page to a new name using site name, path, and page name. Example: /pages/rename/MySite/FolderA/FolderB/MyPage/NewName",
        responses = {
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Page.class)))
        }
    )
    public Page renamePage(
            @Parameter(description = "The path from the site to the page.", name = "pagepath") @PathParam("pagepath") String path,
            @Parameter(description = "The new name for the Page", name = "name") @PathParam("name") String name) {
        path = decodePath(path);
        var matcher = pathPattern.matcher(path);
        var siteName = "";
        var pageName = "";
        var apiPath = "";
        if (matcher.matches()) {
            siteName = StringUtils.defaultString(matcher.group(1));
            apiPath = StringUtils.defaultString(matcher.group(3));
            pageName = StringUtils.defaultString(matcher.group(5));
        }
        try {
            return pageAdaptor.renamePage(uriInfo.getBaseUri(), siteName, apiPath, pageName, name);
        } catch (BackendException | PSDataServiceException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Deletes a page by site, path, and page name.
     */
    @DELETE
    @Path("/by-path/{pagepath:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete page by site, path and pagename",
        description = "Delete a page below the site. Example: /pages/by-path/MySite/FolderA/FolderB/MyPage",
        responses = {
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Status.class)))
        }
    )
    public Status deletePage(
            @Parameter(description = "The path for the page.", name = "pagepath") @PathParam("pagepath") String path) {
        path = decodePath(path);
        var matcher = pathPattern.matcher(path);
        var siteName = "";
        var pageName = "";
        var apiPath = "";
        if (matcher.matches()) {
            siteName = StringUtils.defaultString(matcher.group(1));
            apiPath = StringUtils.defaultString(matcher.group(3));
            pageName = StringUtils.defaultString(matcher.group(5));
        }
        try {
            pageAdaptor.deletePage(uriInfo.getBaseUri(), siteName, apiPath, pageName);
        } catch (BackendException e) {
            throw new WebApplicationException(e);
        }
        return new Status("Deleted");
    }

    public IPageAdaptor getPageAdaptor() {
        return pageAdaptor;
    }

    public void setPageAdaptor(IPageAdaptor pageAdaptor) {
        this.pageAdaptor = pageAdaptor;
    }

    /**
     * Approves every Page in the specified
