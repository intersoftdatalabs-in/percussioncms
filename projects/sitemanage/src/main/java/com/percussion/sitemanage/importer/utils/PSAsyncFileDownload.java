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

// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.utils;

import com.percussion.queue.impl.PSPageImportQueue;
import com.percussion.server.PSRequest;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Handles asynchronous file downloads with a configurable thread pool. */
public class PSAsyncFileDownload {

  private boolean complete = false;
  private static final Logger log = LogManager.getLogger(PSPageImportQueue.class);
  private final List<PSPair<Boolean, String>> results = new ArrayList<>();
  private final List<PSFileDownloadJob> jobs = new ArrayList<>();
  private static final int MAX_THREADS = 6;
  private final Map<String, Object> requestMap;

  public boolean hasCompleted() {
    return complete;
  }

  public PSAsyncFileDownload(Map<String, Object> requestMap) {
    this.requestMap = requestMap;
  }

  public void addDownload(String filePath, String url, boolean createAsset) {
    jobs.add(new PSFileDownloadJob(filePath, url, createAsset));
  }

  public void download() {
    setRequestInfo(this.requestMap);
    var i = jobs.iterator();
    var runningJobs = new ArrayList<PSFileDownLoadJobRunner>();
    var threads = new ArrayList<Thread>();
    while (i.hasNext()) {
      if (runningJobs.size() < MAX_THREADS) {
        var job = i.next();
        final var requestInfoMap = PSRequestInfo.copyRequestInfoMap();
        var request = (PSRequest) requestInfoMap.get(PSRequestInfo.KEY_PSREQUEST);
        requestInfoMap.put(PSRequestInfo.KEY_PSREQUEST, request.cloneRequest());
        var download = new PSFileDownLoadJobRunner(job, requestInfoMap);
        var t = new Thread(download);
        t.setDaemon(true);
        t.start();
        runningJobs.add(download);
        threads.add(t);

        if (runningJobs.size() == MAX_THREADS || !i.hasNext()) {
          for (var thread : threads) {
            try {
              thread.join();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          threads.clear();
          for (var runningJob : runningJobs) {
            results.addAll(runningJob.getResults());
          }
          runningJobs.clear();
        }
      } else {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    complete = true;
  }

  public void setRequestInfo(Map<String, Object> requestInfoMap) {
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }
    PSRequestInfo.initRequestInfo(requestInfoMap);
  }

  public List<PSPair<Boolean, String>> getResults() {
    return results;
  }
}
