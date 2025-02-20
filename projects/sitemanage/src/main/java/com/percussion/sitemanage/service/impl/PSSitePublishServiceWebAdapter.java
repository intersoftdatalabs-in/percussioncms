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
package com.percussion.sitemanage.service.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.PSRelationshipChangeEvent;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.error.PSException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.itemmanagement.data.PSPageLinkedToItem;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeEvent;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationServiceLocator;
import com.percussion.services.relationship.IPSRelationshipService;
import com.percussion.services.relationship.PSRelationshipServiceLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSPublishingActionList;
import com.percussion.sitemanage.data.PSSitePublishResponse;
import com.percussion.sitemanage.service.IPSSitePublishService;
import com.percussion.sitemanage.service.IPSSitePublishService.PubType;
import com.percussion.webservices.content.IPSContentWs;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the rest service layer which delegates to the
 * {@link IPSSitePublishService} for publishing operations.
 */
@Path("/publish")
@Component("sitePublishWebAdapter")
@Lazy
public class PSSitePublishServiceWebAdapter
{

   @Autowired
   private IPSWorkflowHelper workflowHelper;

   @Autowired
   private IPSIdMapper idMapper;

   @Autowired
   private IPSContentWs contentWs;

   @Autowired
   private IPSContentChangeService changeSvc;

   @Autowired
   private IPSFolderHelper folderHelper;

    @Autowired
    IPSRelationshipService relationshipService;

   private static final Logger log = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);

   /**
    * Constructs a PSSitePublishServiceWebAdapter object.
    * 
    * @param sitePublishService the site publish service used for publishing
    * operations.
    */
   @Autowired
   public PSSitePublishServiceWebAdapter(
         IPSSitePublishService sitePublishService)
   {
      this.sitePublishService = sitePublishService;
   }

   /**
    * Publishes the specified site.
    * 
    * @param name the name of the site to be published, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/{name}/{server}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publish(@PathParam("name") String name, @PathParam("server") String server)
   {
      try {
         if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("name may not be blank");

         return sitePublishService.publish(name, PubType.FULL, null, false, server);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }

   
   /**
    * Publishes the specified page.
    * 
    * @param id of the page to be published, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/page/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publishPage(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");

         return sitePublishService.publish(null, PubType.PUBLISH_NOW, id, false, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }

   /**
    * Publishes the specified resource.
    * 
    * @param id of the resource to be published, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/resource/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publishResource(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("name may not be blank");

         return sitePublishService.publish(null, PubType.PUBLISH_NOW, id, true, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }

   /**
    * Publishes the specified page to staging server.
    * 
    * @param id of the page to be published, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/page/staging/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publishPageToStaging(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");

         return sitePublishService.publish(null, PubType.STAGE_NOW, id, false, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }

   /**
    * Publishes the specified resource to staging server.
    * 
    * @param id of the resource to be published, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/resource/staging/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publishResourceToStaging(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("name may not be blank");

         return sitePublishService.publish(null, PubType.STAGE_NOW, id, true, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e.getMessage());
      }
   }

   /**
    * Returns the list of PSPublishingActionProperties objects and each object contains
    * the publish action.
    * @param id of the guid representation of the id of the page or resource to be published, may not be blank.
    * @return The list of actions, may be empty never null.
    */
   @GET
   @Path("/publishingActions/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSPublishingActionList getPublishingActions(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");
         return new PSPublishingActionList(sitePublishService.getPublishingActions(id));
      } catch (PSDataServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e);
      }
   }

   @PUT
   @Path("/takedown/page/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse takeDownPage(@PathParam("id") String id, ArrayList<PSPageLinkedToItem> linkedPages) {
      if (linkedPages != null) {
         IPSRelationshipService relsvc = PSRelationshipServiceLocator.getRelationshipService();
         List<Integer> contentIds = new ArrayList<>();
         PSRelationshipSet relationshipSet = new PSRelationshipSet();

          for (PSPageLinkedToItem item : linkedPages) {

            if(item.getRelationshipId() != null) {
                  PSRelationship relationship = null;
                  try {
                     relationship = relsvc.loadRelationship(idMapper.getGuid(item.getRelationshipId()).getUUID());

                     if(relationship.isInlineRelationship()) {
                         PSRelationshipFilter filter = new PSRelationshipFilter();
                         filter.setDependent(relationship.getOwner());
                         filter.setName(PSRelationshipConfig.TYPE_LOCAL_CONTENT);

                         final List<PSItemSummary> owners = contentWs.findOwners(idMapper.getGuid(relationship.getOwner()), filter, false);
                         for(PSItemSummary owner:owners){
                            contentIds.add(owner.getGUID().getUUID());
                         }
                     }else{
                        if (relationship != null) {
                           relationshipSet.add(relationship);
                        }
                     }
                  } catch (PSException e) {
                      log.error("Unable to load related Relationships",e);
                  }
                }
         }
         if(!relationshipSet.isEmpty()) {
            PSRelationshipChangeEvent event = new PSRelationshipChangeEvent(
                    PSRelationshipChangeEvent.ACTION_MODIFY, relationshipSet);
            PSNotificationEvent notifyEvent = new PSNotificationEvent(
                    PSNotificationEvent.EventType.RELATIONSHIP_CHANGED, event);
            IPSNotificationService srv = PSNotificationServiceLocator
                    .getNotificationService();
            srv.notifyEvent(notifyEvent);
         }
         for(Integer contentId:contentIds){
            PSContentChangeEvent changeEvent = new PSContentChangeEvent();
            changeEvent.setChangeType(PSContentChangeType.PENDING_LIVE);
            changeEvent.setContentId(contentId);
             List<IPSSite> siteIds =  folderHelper.getItemSites(idMapper.getGuidFromContentId(contentId).toString());
             //Because if owner of relationship is already deleted, before child, then it will not return site.
             if(siteIds != null && !siteIds.isEmpty()) {
                try {
                   changeEvent.setSiteId(siteIds.get(0).getSiteId());
                   changeSvc.contentChanged(changeEvent);
                } catch (IPSGenericDao.SaveException e) {
                   log.warn("Error creating change event for takedown change. Error: {}", PSExceptionUtils.getMessageForLog(e));
                }
             }
         }
      }
      return takeDownPage(id);
   }

   /**
    * Takes the specified page down from its site.
    * 
    * @param id of the page to be taken down, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/takedown/page/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse takeDownPage(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");

         return sitePublishService.publish(null, PubType.TAKEDOWN_NOW, id, false, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e.getMessage());
      }
   }
   
   /**
    * Takes the specified resource down from all sites.
    * 
    * @param id of the resource to be taken down, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/takedown/resource/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse takeDownResource(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");

         return sitePublishService.publish(null, PubType.TAKEDOWN_NOW, id, true, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e.getMessage());
      }
   }
   
   /**
    * Takes the specified page down from its staging site.
    * 
    * @param id of the page to be taken down, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/takedown/page/staging/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse takeDownPageFromStaging(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");

         return sitePublishService.publish(null, PubType.REMOVE_FROM_STAGING_NOW, id, false, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e.getMessage());
      }
   }
   
   /**
    * Takes the specified resource down from all staging sites.
    * 
    * @param id of the resource to be taken down, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/takedown/resource/staging/{id}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse takeDownResourceFromStaging(@PathParam("id") String id)
   {
      try {
         if (StringUtils.isBlank(id))
            throw new IllegalArgumentException("id may not be blank");

         return sitePublishService.publish(null, PubType.REMOVE_FROM_STAGING_NOW, id, true, null);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         throw new WebApplicationException(e.getMessage());
      }
   }
   
   
   /**
    * Get a paged list of items that are queued for incremental publish
    * 
    * @param siteName The name of the site that will be published, not <code>null<code/> or empty.
    * @param serverName The name of the server that will be published to, not <code>null<code/> or empty.
    * @param startIndex The starting index into the list to determine the page to return, if <= 0,  
    * the first page of results is returned
    * @param pageSize The max number of items to return per page, if <=0, all items will be returned in a single page
    * 
    * @return A paged item list with the specified page of items, not <code>null</code>, may be empty.
    */
   @GET
   @Path("/incremental/content/{name}/{server}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSPagedItemList getQueuedIncrementalContent(@PathParam("name") String siteName, @PathParam("server") String serverName, @QueryParam("startIndex") int startIndex, @QueryParam("pageSize") int pageSize)
   {
      try {
         return sitePublishService.getQueuedIncrementalContent(siteName, serverName, startIndex, pageSize);
      } catch (IPSSitePublishService.PSSitePublishException e) {
         throw new WebApplicationException(e.getMessage());
      }
   }
   
   /**
    * Get a paged list of items that are unapproved but related to the items that are queued for incremental publish items
    * 
    * @param siteName The name of the site that will be published, not <code>null<code/> or empty.
    * @param serverName The name of the server that will be published to, not <code>null<code/> or empty.
    * @param startIndex The starting index into the list to determine the page to return, if <= 0,  
    * the first page of results is returned
    * @param pageSize The max number of items to return per page, if <=0, all items will be returned in a single page
    * 
    * @return A paged item list with the specified page of items, not <code>null</code>, may be empty.
    */
   @GET
   @Path("/incremental/relatedcontent/{name}/{server}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSPagedItemList getQueuedIncrementalRelatedContent(@PathParam("name") String siteName, @PathParam("server") String serverName, @QueryParam("startIndex") int startIndex, @QueryParam("pageSize") int pageSize)
   {
      try {
         return sitePublishService.getQueuedIncrementalRelatedContent(siteName, serverName, startIndex, pageSize);
      } catch (IPSSitePublishService.PSSitePublishException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }
   
   
   /**
    * Publishes the specified site.
    * 
    * @param name the name of the site to be published, may not be blank.
    * 
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/incremental/publish/{name}/{server}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publishIncremental(@PathParam("name") String name, @PathParam("server") String server)
   {
      try {
         Validate.notEmpty(name);
         Validate.notEmpty(server);

         return sitePublishService.publishIncremental(name, null, false, server);
      } catch (PSDataServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }

   /**
    * Publishes the specified site.
    *
    * @param name the name of the site to be published, may not be blank.
    *
    * @return response from the publishing request, never <code>null</code>.
    */
   @GET
   @Path("/incremental/publish/{name}/{server}/{itemsToApprove}")
   @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
   public PSSitePublishResponse publishIncrementalWithApproval(@PathParam("name") String name, @PathParam("server") String server, @PathParam("itemsToApprove") String itemsToApprove)
   {
      try {
         Validate.notEmpty(name);
         Validate.notEmpty(server);

         return sitePublishService.publishIncrementalWithApproval(name, null, false, server, itemsToApprove);
      } catch (PSDataServiceException | IPSItemWorkflowService.PSItemWorkflowServiceException | IPSPubServerService.PSPubServerServiceException | IPSItemService.PSItemServiceException | PSNotFoundException e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new WebApplicationException(e.getMessage());
      }
   }

   /**
    * The site publish service.  Initialized in constructor, never
    * <code>null</code> after that.
    */
   private IPSSitePublishService sitePublishService;


}
