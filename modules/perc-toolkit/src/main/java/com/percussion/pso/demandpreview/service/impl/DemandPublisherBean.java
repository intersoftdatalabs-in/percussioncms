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
package com.percussion.pso.demandpreview.service.impl;

import com.percussion.pso.demandpreview.service.DemandPublisherService;
import com.percussion.rx.publisher.IPSPublisherJobStatus.State;
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.rx.publisher.PSRxPublisherServiceLocator;
import com.percussion.rx.publisher.data.PSDemandWork;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.utils.guid.IPSGuid;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a Spring Bean implementation of the Demand Publisher Service. The XML file that defines
 * this bean will specify the Preview Site, Template and Context to be used on each site.
 *
 * @author davidbenua
 */
public class DemandPublisherBean implements DemandPublisherService {

  /**
   * Creates a new DemandPublisherBean.
   */
  public DemandPublisherBean() {
    // default
  }
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(DemandPublisherBean.class);

  /** Service for RxPublisherService */
  private IPSRxPublisherService rxPubSvc = null;

  /** timeout in seconds */
  private long timeout = 100L; // seconds

  /** Sleep interval for polling. */
  private long sleeptime = 500L; // milliseconds

  /**
   * Initialize this bean. Intended to be called from the Spring <code>init-method</code> attribute.
   */
  public void init() {
    if (rxPubSvc == null) {
      rxPubSvc = PSRxPublisherServiceLocator.getRxPublisherService();
    }
  }

  /**
   * Queues a demand work request for publishing a single content item.
   *
   * <p>This method adds the specified content item to the publication queue for the given edition
   * and returns immediately without waiting for the publication to complete. Use this method when
   * you want to initiate a publish operation asynchronously.
   *
   * <p>Note: The edition must have at least one content list that uses the "Selected Items" content
   * list generator, or the publisher will timeout.
   *
   * @param edition the edition to publish to. Must not be null and must be a valid edition with at
   *     least one "Selected Items" content list.
   * @param content the content item GUID to publish. Must not be null.
   * @param folder the folder GUID where the content item resides. Must not be null.
   * @return the unique request ID that can be used to track or wait for the publication to complete
   * @throws TimeoutException if the publisher fails to queue the work within the configured timeout
   *     period
   */
  public long queueDemandWork(IPSEdition edition, IPSGuid content, IPSGuid folder)
      throws TimeoutException {
    log.trace("Queueing demand work...");
    PSDemandWork work = new PSDemandWork();
    work.addItem(folder, content);
    Long jobId = null;
    int editionId = edition.getGUID().getUUID();
    log.debug("demand edition is {}", editionId);
    log.debug("work contains {} items", work.getContent().size());
    long requestId = -1;
    try {
      requestId = rxPubSvc.queueDemandWork(editionId, work);
    } catch (PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    log.debug("Started demand job. Request id = {}", requestId);
    long timeLimit = System.currentTimeMillis() + timeout * 1000;

    while (jobId == null) {
      jobId = rxPubSvc.getDemandRequestJob(requestId);
      if (jobId == null) {
        log.debug("Job Not queued for request id {}", requestId);
        long now = System.currentTimeMillis();
        log.trace("time now {}", now);
        if (now > timeLimit) {
          throw new TimeoutException("Publishing did not complete before timeout. ");
        }
        try {
          log.trace("sleeping...");
          Thread.sleep(sleeptime);
        } catch (InterruptedException ex) {
          log.error("Interrupted: {} ", PSExceptionUtils.getMessageForLog(ex));
          Thread.currentThread().interrupt();
        }
      }
    }
    log.debug("JobID {} for request id {}", jobId, requestId);
    return requestId;
  }

  /**
   * Waits for a previously queued demand work request to complete.
   *
   * <p>This method blocks until the publication job reaches a terminal state (completed, failed, or
   * canceled) or until the configured timeout is exceeded.
   *
   * @param jobId the job ID to wait for. This is the request ID returned from a previous call to
   *     {@link #queueDemandWork(IPSEdition, IPSGuid, IPSGuid)}
   * @return the final state of the publishing job, which will be a terminal state (COMPLETED,
   *     FAILED, or CANCELED)
   * @throws TimeoutException if the job does not complete within the configured timeout period
   */
  public State waitDemandWorkComplete(long jobId) throws TimeoutException {
    long timeLimit = System.currentTimeMillis() + timeout * 1000;
    log.trace("time out {}", timeLimit);
    State state = State.INITIAL;
    while (true) {
      state = rxPubSvc.getDemandWorkStatus(jobId);
      if (log.isDebugEnabled()) {
        if (state == null) {
          log.debug("Status for job {} is null", jobId);
        }
      }
      if (state != null && state.isTerminal()) {
        return state;
      }
      long now = System.currentTimeMillis();
      log.trace("time now {}", now);
      if (now > timeLimit) {
        throw new TimeoutException("Publishing did not complete before timeout. ");
      }
      try {
        log.trace("sleeping...");
        Thread.sleep(sleeptime);
      } catch (InterruptedException ex) {
        log.error("Interrupted: {}", PSExceptionUtils.getMessageForLog(ex));
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Publishes a single content item and waits for the publication to complete.
   *
   * <p>This is a convenience method that combines {@link #queueDemandWork(IPSEdition, IPSGuid,
   * IPSGuid)} and {@link #waitDemandWorkComplete(long)} into a single synchronous operation. The
   * method blocks until the publication completes or times out.
   *
   * <p>Note: The edition must have at least one content list that uses the "Selected Items" content
   * list generator, or the publisher will timeout.
   *
   * @param edition the edition to publish to. Must not be null.
   * @param content the content item GUID to publish. Must not be null.
   * @param folder the folder GUID where the content item resides. Must not be null.
   * @throws TimeoutException if the publisher fails to complete within the configured timeout
   *     period
   * @throws PSAssemblyException if the publication fails (e.g., if the final state is not
   *     COMPLETED)
   */
  public void publishAndWait(IPSEdition edition, IPSGuid content, IPSGuid folder)
      throws TimeoutException, PSAssemblyException {
    long jobId = queueDemandWork(edition, content, folder);
    State state = waitDemandWorkComplete(jobId);
    if (state != State.COMPLETED) {
      String emsg = "Publishing failed " + state;
      log.error(emsg);
      throw new PSAssemblyException(0, emsg);
    }
  }

  /**
   * Gets the timeout (in seconds)
   *
   * @return the timeout
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Sets the timeout (in seconds).
   *
   * @param timeout the timeout to set
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Sets the publisher service. Used for unit testing only.
   *
   * @param rxPubSvc the rxPubSvc to set
   */
  protected void setRxPubSvc(IPSRxPublisherService rxPubSvc) {
    this.rxPubSvc = rxPubSvc;
  }
}
