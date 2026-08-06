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
package com.percussion.publishingdesign.impl;

import com.percussion.publishingdesign.data.PSDemandPublishRequest;
import com.percussion.publishingdesign.data.PSRuntimeEditionStatus;
import com.percussion.publishingdesign.data.PSRuntimeJobResponse;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.rx.publisher.PSRxPublisherServiceLocator;
import com.percussion.rx.publisher.data.PSDemandWork;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.publishing.IPSPublishingWs;
import com.percussion.webservices.publishing.PSPublishingWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime publishing operations for US5 — thin delegates to Rx publisher and publishing web
 * services. Extracted for unit testing without Spring.
 */
public class PSPublishingRuntimeSupport {
  private static final Logger log = LogManager.getLogger(PSPublishingRuntimeSupport.class);

  private final IPSPublisherService publisherService;
  private final IPSGuidManager guidManager;
  private final IPSRxPublisherService rxPublisherService;
  private final IPSPublishingWs publishingWs;
  private final IPSContentWs contentWs;

  public PSPublishingRuntimeSupport() {
    this(
        PSPublisherServiceLocator.getPublisherService(),
        PSGuidManagerLocator.getGuidMgr(),
        PSRxPublisherServiceLocator.getRxPublisherService(),
        PSPublishingWsLocator.getPublishingWebservice(),
        PSContentWsLocator.getContentWebservice());
  }

  public PSPublishingRuntimeSupport(
      IPSPublisherService publisherService,
      IPSGuidManager guidManager,
      IPSRxPublisherService rxPublisherService,
      IPSPublishingWs publishingWs,
      IPSContentWs contentWs) {
    this.publisherService = publisherService;
    this.guidManager = guidManager;
    this.rxPublisherService = rxPublisherService;
    this.publishingWs = publishingWs;
    this.contentWs = contentWs;
  }

  public List<PSRuntimeEditionStatus> listRuntimeEditions(String siteId) {
    requireNonBlank(siteId, "siteId");
    requireRx();
    IPSGuid siteGuid = guidManager.makeGuid(siteId, PSTypeEnum.SITE);
    List<IPSEdition> editions = publisherService.findAllEditionsBySite(siteGuid);
    List<PSRuntimeEditionStatus> out = new ArrayList<>();
    for (IPSEdition edition : editions) {
      PSRuntimeEditionStatus row = new PSRuntimeEditionStatus();
      if (edition.getGUID() != null) {
        row.setEditionId(String.valueOf(edition.getGUID().getUUID()));
        long jobId = rxPublisherService.getEditionJobId(edition.getGUID());
        row.setRunningJobId(jobId);
        if (jobId > 0) {
          try {
            IPSPublisherJobStatus st = rxPublisherService.getPublishingJobStatus(jobId);
            if (st != null && st.getState() != null) {
              row.setJobStatus(st.getState().getDisplayName());
            } else {
              row.setJobStatus("unknown");
            }
          } catch (Exception e) {
            // Do not claim "running" when status fetch failed — job id is still set.
            log.debug(
                "Unable to load status for publish job {}: {}",
                jobId,
                PSExceptionUtils.getMessageForLog(e));
            row.setJobStatus("unknown");
          }
        }
      }
      row.setName(edition.getName());
      row.setSiteId(siteId);
      row.setComment(edition.getComment());
      out.add(row);
    }
    return out;
  }

  public PSRuntimeJobResponse startEdition(String editionId) {
    requireNonBlank(editionId, "editionId");
    requireRx();
    try {
      IPSGuid edGuid = guidManager.makeGuid(editionId, PSTypeEnum.EDITION);
      long jobId = rxPublisherService.startPublishingJob(edGuid, null);
      return jobResponse(jobId, editionId, null);
    } catch (IllegalStateException e) {
      throw conflict(e.getMessage() != null ? e.getMessage() : "Edition already running");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internal(e.getMessage());
    }
  }

  public PSRuntimeJobResponse stopJob(String jobId) {
    requireNonBlank(jobId, "jobId");
    requireRx();
    try {
      long id = Long.parseLong(jobId.trim());
      rxPublisherService.cancelPublishingJob(id);
      PSRuntimeJobResponse r = new PSRuntimeJobResponse();
      r.setJobId(id);
      r.setStatus("cancelled");
      return r;
    } catch (NumberFormatException e) {
      throw badRequest("jobId must be numeric");
    } catch (IllegalStateException e) {
      throw conflict(e.getMessage() != null ? e.getMessage() : "Job not active");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internal(e.getMessage());
    }
  }

  public PSRuntimeJobResponse getJobStatus(String jobId) {
    requireNonBlank(jobId, "jobId");
    requireRx();
    try {
      long id = Long.parseLong(jobId.trim());
      IPSPublisherJobStatus st = rxPublisherService.getPublishingJobStatus(id);
      return fromStatus(st, null);
    } catch (NumberFormatException e) {
      throw badRequest("jobId must be numeric");
    } catch (IllegalStateException e) {
      throw notFound("Job not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internal(e.getMessage());
    }
  }

  public PSRuntimeJobResponse demandPublish(String editionId, PSDemandPublishRequest request) {
    requireNonBlank(editionId, "editionId");
    if (request == null || request.getContentIds() == null || request.getContentIds().isEmpty()) {
      throw badRequest("contentIds are required");
    }
    requirePubWs();
    try {
      IPSGuid edGuid = guidManager.makeGuid(editionId, PSTypeEnum.EDITION);
      PSDemandWork work = new PSDemandWork();
      List<String> contentIds = request.getContentIds();
      List<String> folderIds = request.getFolderIds();
      for (int i = 0; i < contentIds.size(); i++) {
        String cid = contentIds.get(i);
        if (cid == null || cid.isBlank()) {
          continue;
        }
        IPSGuid contentGuid = toItemGuid(cid.trim());
        IPSGuid folderGuid = null;
        if (folderIds != null
            && i < folderIds.size()
            && folderIds.get(i) != null
            && !folderIds.get(i).isBlank()) {
          folderGuid = toItemGuid(folderIds.get(i).trim());
        } else if (contentWs != null) {
          List<PSItemSummary> parents = contentWs.findFolderParents(contentGuid, false);
          if (parents != null && !parents.isEmpty()) {
            folderGuid = parents.get(0).getGUID();
          }
        }
        if (folderGuid == null) {
          throw badRequest(
              "folderId required for contentId " + cid + " (or enable content parent lookup)");
        }
        work.addItem(folderGuid, contentGuid);
      }
      if (work.getContent().isEmpty()) {
        throw badRequest("No valid content items to demand-publish");
      }
      long requestId = publishingWs.queueDemandWork(edGuid.getUUID(), work);
      PSRuntimeJobResponse r = new PSRuntimeJobResponse();
      r.setEditionId(editionId);
      r.setRequestId(requestId);
      r.setStatus("queued");
      Long jobId = publishingWs.getDemandRequestJob(requestId);
      if (jobId != null) {
        r.setJobId(jobId);
      }
      return r;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internal(e.getMessage());
    }
  }

  public void purgeJobLog(String jobId) {
    requireNonBlank(jobId, "jobId");
    requirePubWs();
    try {
      publishingWs.purgeJobLog(Long.parseLong(jobId.trim()));
    } catch (NumberFormatException e) {
      throw badRequest("jobId must be numeric");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internal(e.getMessage());
    }
  }

  public void clearSiteItems(String siteId) {
    requireNonBlank(siteId, "siteId");
    requirePubWs();
    try {
      publishingWs.deleteSiteItems(guidManager.makeGuid(siteId, PSTypeEnum.SITE));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internal(e.getMessage());
    }
  }

  private PSRuntimeJobResponse jobResponse(long jobId, String editionId, IPSPublisherJobStatus st) {
    if (st == null && rxPublisherService != null && jobId > 0) {
      try {
        st = rxPublisherService.getPublishingJobStatus(jobId);
      } catch (Exception ignored) {
        // status optional right after start
      }
    }
    return fromStatus(st, editionId, jobId);
  }

  private PSRuntimeJobResponse fromStatus(IPSPublisherJobStatus st, String editionId) {
    return fromStatus(st, editionId, st != null ? st.getJobId() : 0);
  }

  private PSRuntimeJobResponse fromStatus(IPSPublisherJobStatus st, String editionId, long jobId) {
    PSRuntimeJobResponse r = new PSRuntimeJobResponse();
    r.setJobId(jobId);
    r.setEditionId(editionId);
    if (st != null) {
      r.setJobId(st.getJobId());
      if (st.getState() != null) {
        r.setStatus(st.getState().getDisplayName());
      }
      r.setDelivered(st.countItemsDelivered());
      r.setFailed(st.countFailedItems());
    } else {
      r.setStatus("started");
    }
    return r;
  }

  private IPSGuid toItemGuid(String id) {
    // Prefer legacy content id when numeric
    try {
      long n = Long.parseLong(id);
      return guidManager.makeGuid(n, PSTypeEnum.LEGACY_CONTENT);
    } catch (NumberFormatException e) {
      return guidManager.makeGuid(id, PSTypeEnum.LEGACY_CONTENT);
    }
  }

  private void requireRx() {
    if (rxPublisherService == null) {
      throw new WebApplicationException(
          "Rx publisher service unavailable", Response.Status.SERVICE_UNAVAILABLE);
    }
  }

  private void requirePubWs() {
    if (publishingWs == null) {
      throw new WebApplicationException(
          "Publishing web service unavailable", Response.Status.SERVICE_UNAVAILABLE);
    }
  }

  private static void requireNonBlank(String v, String field) {
    if (v == null || v.isBlank()) {
      throw badRequest(field + " is required");
    }
  }

  private static WebApplicationException badRequest(String msg) {
    return new WebApplicationException(msg, Response.Status.BAD_REQUEST);
  }

  private static WebApplicationException notFound(String msg) {
    return new WebApplicationException(msg, Response.Status.NOT_FOUND);
  }

  private static WebApplicationException conflict(String msg) {
    return new WebApplicationException(msg, Response.Status.CONFLICT);
  }

  private static WebApplicationException internal(String msg) {
    return new WebApplicationException(msg, Response.Status.INTERNAL_SERVER_ERROR);
  }
}
