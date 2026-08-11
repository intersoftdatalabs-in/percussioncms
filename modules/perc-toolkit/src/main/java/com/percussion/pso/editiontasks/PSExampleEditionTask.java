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

package com.percussion.pso.editiontasks;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPubItemStatus;
import com.percussion.services.publisher.IPSSiteItem;
import com.percussion.services.sitemgr.IPSSite;
import java.io.File;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PSExampleEditionTask class.
 */
public class PSExampleEditionTask implements IPSEditionTask {
  /**
   * Creates a new PSExampleEditionTask.
   */
  public PSExampleEditionTask() {
    // default
  }


  private static final Logger logger = LogManager.getLogger(PSExampleEditionTask.class);

  /**
   * init operation.
   *
   * @param def the def
   * @param codeRoot the code root
   */
  public void init(IPSExtensionDef def, File codeRoot) {
    // No initialization required
  }

  /**
   * Returns the type.
   *
   * @return the result
   */
  public TaskType getType() {
    return TaskType.POSTEDITION;
  }

  /**
   * perform operation.
   *
   * @param edition the edition
   * @param site the site
   * @param starTime the star time
   * @param endTime the end time
   * @param jobId the job id
   * @param duration the duration
   * @param success the success
   * @param params the params
   * @param status the status
   * @throws Exception if an error occurs
   */
  public void perform(
      IPSEdition edition,
      IPSSite site,
      Date starTime,
      Date endTime,
      long jobId,
      long duration,
      boolean success,
      Map<String, String> params,
      IPSEditionTaskStatusCallback status)
      throws Exception {

    logger.info("*********PSExampleEditionTaskStarting*********");

    Iterable<IPSPubItemStatus> jobPages = status.getIterableJobStatus();
    String siteBaseURL = StringUtils.removeEnd(site.getBaseUrl(), "/");
    String url = "";

    for (IPSPubItemStatus page : jobPages) {

      url = page.getLocation();

      if (page.getStatus().equals(IPSSiteItem.Status.SUCCESS)) {
        logger.info(siteBaseURL + url);
        // here the URL can be sent to any service/end point using
        // commons HTTP client, etc.
      }
    } // end for loop
  } // end method perform
}
