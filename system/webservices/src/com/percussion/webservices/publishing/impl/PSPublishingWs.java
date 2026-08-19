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
package com.percussion.webservices.publishing.impl;

import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.IPSPublishingJobStatusCallback;
import com.percussion.rx.publisher.IPSRxPublisherServiceInternal;
import com.percussion.rx.publisher.PSRxPubServiceInternalLocator;
import com.percussion.rx.publisher.data.PSDemandWork;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.error.PSRuntimeException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSEditionContentList;
import com.percussion.services.publisher.IPSEditionTaskDef;
import com.percussion.services.publisher.IPSPubStatus;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSWebserviceErrors;
import com.percussion.webservices.publishing.IPSPublishingWs;
import com.percussion.webservices.ExceptionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Wraps various methods of the filter, publisher, and site manager services.
 */
@Transactional
public class PSPublishingWs implements IPSPublishingWs
{
   private IPSPublisherService pubSvc;
   private IPSFilterService filterSvc;
   private IPSSiteManager siteMgr;
   private IPSPubServerDao pubServerDao;

   /**
    * The publisher service used for invoking and retrieving status of
    * publishing jobs.  Initialized in {@link #getRxPubSvc()} , never
    * <code>null</code> after that. Made volatile to support thread-safe
    * double-checked locking for lazy initialization.
    */
   private volatile IPSRxPublisherServiceInternal rxPubSvc;

   /**
    * Default constructor for backwards compatibility with Spring bean wiring.
    * @deprecated Use the constructor with dependency injection instead.
    */
   @Deprecated
   public PSPublishingWs()
   {
      // NOP
   }

   /**
    * Constructs a new publishing web service handler.
    *
    * @param pubSvc The publishing service, not <code>null</code>.
    * @param filterSvc The filter service, not <code>null</code>.
    * @param siteMgr The site manager, not <code>null</code>.
    * @param pubServerDao The publishing server DAO, not <code>null</code>.
    */
   public PSPublishingWs(IPSPublisherService pubSvc, IPSFilterService filterSvc, IPSSiteManager siteMgr, IPSPubServerDao pubServerDao)
   {
      this.pubSvc = Objects.requireNonNull(pubSvc, "pubSvc must not be null");
      this.filterSvc = Objects.requireNonNull(filterSvc, "filterSvc must not be null");
      this.siteMgr = Objects.requireNonNull(siteMgr, "siteMgr must not be null");
      this.pubServerDao = Objects.requireNonNull(pubServerDao, "pubServerDao must not be null");
   }

   @Override
   public IPSContentList createContentList(String name)
   {
      notBlank(name, "name may not be blank");
      return pubSvc.createContentList(name);
   }

   @Override
   public IPSEdition createEdition()
   {
      return pubSvc.createEdition();
   }

   @Override
   public IPSEditionContentList createEditionContentList()
   {
      return pubSvc.createEditionContentList();
   }

   @Override
   public IPSSite createSite()
   {
      return siteMgr.createSite();
   }

   @Override
   public void deleteContentLists(List<IPSContentList> lists)
   {
      notEmpty(lists, "lists may not be null or empty");
      pubSvc.deleteContentLists(lists);
   }

   @Override
   public void deleteEdition(IPSEdition edition)
   {
      notNull(edition, "edition may not be null");
      pubSvc.deleteEdition(edition);
   }

   @Override
   public void deleteSite(IPSSite site)
   {
      notNull(site, "site may not be null");
      siteMgr.deleteSite(site);
   }

   @Override
   public void deleteSiteItems(IPSGuid siteguid)
   {
      notNull(siteguid, "siteguid may not be null");
      pubSvc.deleteSiteItems(siteguid);
   }

   @Override
   public List<IPSEdition> findAllEditionsBySite(IPSGuid siteId)
   {
      notNull(siteId, "siteId may not be null");
      return pubSvc.findAllEditionsBySite(siteId);
   }

   @Override
   public List<IPSEdition> findAllEditionsByPubServer(IPSGuid pubServerId)
   {
      notNull(pubServerId, "pubServerId may not be null");
      return pubSvc.findAllEditionsByPubServer(pubServerId);
   }

   @Override
   public IPSEdition findEditionByName(String name)
   {
      notBlank(name, "name may not be blank");
      return pubSvc.findEditionByName(name);
   }

   @Override
   public IPSContentList findContentListById(IPSGuid contListId)
   {
      notNull(contListId, "contListId may not be null");
      try {
         return pubSvc.findContentListById(contListId);
      } catch (PSNotFoundException e) {
         throw e;
      }
   }

   @Override
   public IPSItemFilter findFilterByName(String name) throws PSErrorException
   {
      notBlank(name, "name may not be blank");
      try
      {
         return filterSvc.findFilterByName(name);
      }
      catch (PSFilterException e)
      {
         var code = WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME;
         throw new PSErrorException(code,
               PSWebserviceErrors.createErrorMessage(code,
                     IPSItemFilter.class.getName(), name),
               ExceptionUtils.getStackTrace(e));
      }
   }

   @Override
   public IPSContentList loadContentList(String name) throws PSErrorException
   {
      notBlank(name, "name may not be blank");
      try
      {
         return pubSvc.loadContentList(name);
      }
      catch (PSNotFoundException e)
      {
         var code = WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME;
         throw new PSErrorException(code,
               PSWebserviceErrors.createErrorMessage(code,
                     IPSContentList.class.getName(), name),
               ExceptionUtils.getStackTrace(e));
      }
   }

   @Override
   public IPSPublishingContext loadContext(String contextname)
         throws PSErrorException
   {
      notBlank(contextname, "contextname may not be blank");
      try
      {
         return siteMgr.loadContext(contextname);
      }
      catch (PSNotFoundException e)
      {
         var code = WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME;
         throw new PSErrorException(code,
               PSWebserviceErrors.createErrorMessage(code,
                     IPSPublishingContext.class.getName(), contextname),
               ExceptionUtils.getStackTrace(e));
      }
   }

   @Override
   public List<IPSEditionContentList> loadEditionContentLists(IPSGuid editionId)
   {
      notNull(editionId, "editionId may not be null");
      return pubSvc.loadEditionContentLists(editionId);
   }

   @Override
   public IPSSite findSite(String sitename) throws PSErrorException
   {
      notBlank(sitename, "sitename may not be blank");

      IPSSite site = siteMgr.findSite(sitename);
      if (site != null)
         return site;

      var code = WebserviceErrorCodes.OBJECT_NOT_FOUND_BY_NAME;
      PSErrorException error = new PSErrorException(code,
            PSWebserviceErrors.createErrorMessage(code,
                  IPSSite.class.getName(), sitename),
            ExceptionUtils.getStackTrace(new PSNotFoundException(sitename + " Not Found.")));
      throw error;
   }

   @Override
   public IPSSite findSiteById(IPSGuid siteId) throws PSErrorException
   {
      notNull(siteId, "siteId may not be null");
      return siteMgr.findSite(siteId);
   }

   @Override
   public List<IPSSite> getItemSites(IPSGuid contentId)
   {
      notNull(contentId, "contentId may not be null");
      return siteMgr.getItemSites(contentId);
   }

   @Override
   public void saveContentList(IPSContentList clist)
   {
      notNull(clist, "clist may not be null");
      pubSvc.saveContentList(clist);
   }

   @Override
   public void saveEdition(IPSEdition edition)
   {
      notNull(edition, "edition may not be null");
      pubSvc.saveEdition(edition);
   }

   @Override
   public void saveEditionContentList(IPSEditionContentList list)
   {
      notNull(list, "list may not be null");
      pubSvc.saveEditionContentList(list);
   }

   @Override
   public void saveSite(IPSSite site)
   {
      notNull(site, "site may not be null");
      siteMgr.saveSite(site);
   }

   @Override
   public List<IPSSite> findAllSites()
   {
      return siteMgr.findAllSites();
   }

   @Override
   public List<IPSPubStatus> findPubStatusBySite(IPSGuid siteId)
   {
      notNull(siteId, "siteId may not be null");
      return pubSvc.findPubStatusBySite(siteId);
   }

   @Override
   public List<IPSPubStatus> findPubStatusByEdition(IPSGuid editionId)
   {
      notNull(editionId, "editionId may not be null");
      return pubSvc.findPubStatusByEdition(editionId);
   }

   @Override
   public void purgeJobLog(long jobid)
   {
      pubSvc.purgeJobLog(jobid);
   }

   @Override
   public Long getDemandRequestJob(long requestid)
   {
      return getRxPubSvc().getDemandRequestJob(requestid);
   }

   @Override
   public IPSPublisherJobStatus getPublishingJobStatus(long jobId)
   {
      return getRxPubSvc().getPublishingJobStatus(jobId);
   }

   @Override
   public long queueDemandWork(int editionid, PSDemandWork work)
   {
      notNull(work, "work may not be null");
      try {
         return getRxPubSvc().queueDemandWork(editionid, work);
      } catch (PSNotFoundException e) {
         throw e;
      }
   }

   @Override
   public long startPublishingJob(IPSGuid edition,
         IPSPublishingJobStatusCallback callback)
   {
      notNull(edition, "edition may not be null");
      return getRxPubSvc().startPublishingJob(edition, callback);
   }

   @Override
   public List<Long> getInProgressPublishingJobs(String siteName)
   {
      notBlank(siteName, "siteName may not be blank");

      return Optional.ofNullable(siteMgr.findSite(siteName))
            .map(site -> getRxPubSvc().getActiveJobIds(site.getGUID()).stream()
                  .filter(jobId -> !getPublishingJobStatus(jobId).getState().isTerminal())
                  .collect(Collectors.toList()))
            .orElse(List.of());
   }

   @Override
   public IPSEditionTaskDef createEditionTask()
   {
      return pubSvc.createEditionTask();
   }

   @Override
   public void deleteEditionTask(IPSEditionTaskDef task)
   {
      notNull(task, "task may not be null");
      pubSvc.deleteEditionTask(task);
   }

   @Override
   public void deleteStatusList(List<IPSPubStatus> statusList)
   {
      notNull(statusList, "statusList may not be null");
      pubSvc.deleteStatusList(statusList);
   }

   @Override
   public void deleteEditionContentList(IPSEditionContentList edtContentList)
   {
      notNull(edtContentList, "edtContentList may not be null");
      pubSvc.deleteEditionContentList(edtContentList);
   }

   @Override
   public IPSEditionTaskDef findEditionTaskById(IPSGuid id)
   {
      notNull(id, "id may not be null");
      try {
         return pubSvc.findEditionTaskById(id);
      } catch (PSNotFoundException e) {
         throw e;
      }
   }

   @Override
   public void saveEditionTask(IPSEditionTaskDef task)
   {
      notNull(task, "task may not be null");
      pubSvc.saveEditionTask(task);
   }

   @Override
   public List<IPSEditionTaskDef> loadEditionTaskByEdition(IPSGuid editionid)
   {
      notNull(editionid, "editionid may not be null");
      return pubSvc.loadEditionTasks(editionid);
   }

   /**
    * @param pubSvc the pubSvc to set
    * @deprecated Use constructor injection.
    */
   @Deprecated
   public void setPubSvc(IPSPublisherService pubSvc)
   {
      this.pubSvc = pubSvc;
   }

   /**
    * @param filterSvc the filterSvc to set
    * @deprecated Use constructor injection.
    */
   @Deprecated
   public void setFilterSvc(IPSFilterService filterSvc)
   {
      this.filterSvc = filterSvc;
   }

   /**
    * @param siteMgr the siteMgr to set
    * @deprecated Use constructor injection.
    */
   @Deprecated
   public void setSiteMgr(IPSSiteManager siteMgr)
   {
      this.siteMgr = siteMgr;
   }

   /**
    * @param pubServerDao the pubServerDao to set
    * @deprecated Use constructor injection.
    */
   @Deprecated
   public void setPubServerDao(IPSPubServerDao pubServerDao)
   {
      this.pubServerDao = pubServerDao;
   }

   private IPSRxPublisherServiceInternal getRxPubSvc()
   {
      IPSRxPublisherServiceInternal result = rxPubSvc;
      if (result == null)
      {
         synchronized(this) {
            result = rxPubSvc;
            if (result == null) {
               rxPubSvc = result = PSRxPubServiceInternalLocator.getRxPublisherService();
            }
         }
      }
      return result;
   }
}
