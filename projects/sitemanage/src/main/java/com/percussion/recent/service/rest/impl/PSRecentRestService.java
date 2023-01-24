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

package com.percussion.recent.service.rest.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSTemplateSummaryList;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.pagemanagement.data.PSWidgetContentTypeList;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.data.PSPathItemList;
import com.percussion.recent.service.rest.IPSRecentRestService;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSItemPropertiesList;
import com.percussion.share.service.exception.PSDataServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/recent")
@Transactional(propagation=Propagation.REQUIRED)
@Service("recentRestService")
public class PSRecentRestService implements IPSRecentRestService
{
    private IPSRecentService recentService;

    static Logger log = LogManager.getLogger(PSRecentRestService.class);

    @Autowired
    public PSRecentRestService(IPSRecentService recentService)
    {
        this.recentService = recentService;
    }

    @GET
    @Produces(
    {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/item")
    public List<PSItemProperties> findRecentItem()
    {
        return new PSItemPropertiesList(recentService.findRecentItem(false));
    }

    @GET
    @Produces(
            {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/non-archived-item")
    public List<PSItemProperties> findRecentNonArchivedItem()
    {
        return new PSItemPropertiesList(recentService.findRecentItem(true));
    }

    @GET
    @Produces(
    {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/template/{siteName}")
    public List<PSTemplateSummary> findRecentTemplate(@PathParam("siteName")
    String siteName)
    {
        return new PSTemplateSummaryList(recentService.findRecentTemplate(siteName));
    }

    @GET
    @Produces(
    {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/site-folder/{siteName}")
    public List<PSPathItem> findRecentSiteFolder(@PathParam("siteName")
    String siteName)
    {
        return new PSPathItemList(recentService.findRecentSiteFolder(siteName));
    }

    @GET
    @Produces(
    {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/asset-folder")
    public List<PSPathItem> findRecentAssetFolder()
    {
        return new PSPathItemList(recentService.findRecentAssetFolder());
    }

    @GET
    @Produces(
    {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/asset-type")
    public List<PSWidgetContentType> findRecentAssetType()
    {
        try {
            return new PSWidgetContentTypeList(recentService.findRecentAssetType());
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/item")
    public void addRecentItem(@FormParam("value")
    String value)
    {
        recentService.addRecentItem(value);
    }

    @POST
    @Path("/template/{siteName}")
    public void addRecentTemplate(@PathParam("siteName")
    String siteName, @FormParam("value")
    String value)
    {
        recentService.addRecentTemplate(siteName, value);
    }

    @POST
    @Path("/site-folder")
    public void addRecentSiteFolder(@FormParam("value")
    String value)
    {
        recentService.addRecentSiteFolder(value);
    }

    @POST
    @Path("/asset-folder")
    public void addRecentAssetFolder(@FormParam("value")
    String value)
    {
        recentService.addRecentAssetFolder(value);
    }

    @POST
    @Path("/item-by-user/{userName}")
    public void addRecentItemByUser(@PathParam("userName")String userName, @FormParam("value")
    String value)
    {
        recentService.addRecentItemByUser(userName,value);
    }

    @POST
    @Path("/template-by-user/{userName}/{siteName}")
    public void addRecentTemplateByUser(@PathParam("userName")String userName,@PathParam("siteName")
    String siteName, @FormParam("value")
    String value)
    {
        recentService.addRecentTemplateByUser(userName, siteName, value);
    }

    @POST
    @Path("/site-folder-by-user/{userName}")
    public void addRecentSiteFolderByUser(@PathParam("userName")String userName,@FormParam("value")
    String value)
    {
        recentService.addRecentSiteFolderByUser(userName,value);
    }

    @POST
    @Path("/asset-folder-by-user/{userName}")
    public void addRecentAssetFolderByUser(@PathParam("userName")String userName, @FormParam("value")
    String value)
    {
        recentService.addRecentAssetFolderByUser(userName,value);
    }
    
    @DELETE
    @Path("/user/{user}")
    public void deleteUserRecent(@PathParam("user") String user)
    {
        recentService.addRecentAssetFolder(user);
    }

    @DELETE
    @Path("/site/{siteName}")
    public void deleteSiteRecent(@PathParam("siteName") String siteName)
    {
        recentService.deleteSiteRecent(siteName);
    }
    
    @POST
    @Path("/asset-type")
    public void addRecentAssetType(@FormParam("value")
    String value)
    {
        recentService.addRecentAssetType(value);
    }
}
