// REFACTORED: CP-JAVA11
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
package com.percussion.share.async.impl;

import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.share.async.IPSAsyncJob;
import com.percussion.share.async.IPSAsyncJobFactory;
import com.percussion.share.async.IPSAsyncJobListener;
import com.percussion.share.async.IPSAsyncJobService;
import com.percussion.share.async.PSAsyncJobStatus;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service implementation to start and manage jobs that need to run asynchronously.
 *
 * @author JaySeletz
 */
public class PSAsyncJobService implements IPSAsyncJobService, IPSAsyncJobListener {

  private IPSAsyncJobFactory m_jobFactory;
  private final ConcurrentMap<Long, IPSAsyncJob> m_jobMap = new ConcurrentHashMap<>();
  private final AtomicLong m_jobIdCounter = new AtomicLong();

  // Actual implementation provided by the Spring container
  public void setAsyncJobFactory(IPSAsyncJobFactory jobFactory) {
    m_jobFactory = jobFactory;
  }

  @Override
  public long startJob(String jobType, Object config)
      throws IPSFolderService.PSWorkflowNotFoundException {
    var job = m_jobFactory.getJob(jobType);
    var jobId = m_jobIdCounter.incrementAndGet();
    job.setId(jobId);
    job.init(config);

    // Add self as listener
    job.addJobListener(this);

    // Start job
    var thread = new Thread(job);
    thread.setDaemon(true);
    thread.start();

    m_jobMap.put(jobId, job);

    return jobId;
  }

  @Override
  public PSAsyncJobStatus getJobStatus(long jobId) {
    var job = m_jobMap.get(jobId);
    if (job != null) {
      return new PSAsyncJobStatus(jobId, job.getStatus(), job.getStatusMessage());
    } else {
      return new PSAsyncJobStatus(jobId, IPSAsyncJob.COMPLETE_STATUS, "");
    }
  }

  @Override
  public void cancelJob(long jobId) {
    var job = m_jobMap.get(jobId);
    if (job == null) {
      return;
    }

    // Cancel the job
    job.cancelJob();

    // Wait until it's completed
    while (!job.isCompleted()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if (job.isCompleted()) {
          break;
        }
      }
    }

    // Remove self as listener
    job.removeJobListener(this);
  }

  @Override
  public void jobCompleted(long jobId) {
    var job = m_jobMap.get(jobId);
    if (job == null) {
      return;
    }
    // Remove self as listener
    job.removeJobListener(this);

    // Handle grooming old jobs from list
    groomJobList();
  }

  @Override
  public Object getJobResult(long jobId) {
    var job = m_jobMap.get(jobId);
    return job != null ? job.getResult() : null;
  }

  /** Removes any expired jobs from the list. */
  private void groomJobList() {
    for (Entry<Long, IPSAsyncJob> entry : m_jobMap.entrySet()) {
      if (entry.getValue().isDiscarded()) {
        m_jobMap.remove(entry.getKey());
      }
    }
  }
}
