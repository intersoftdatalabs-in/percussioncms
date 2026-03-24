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
package com.percussion.rx.publisher.impl;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.sitemgr.IPSSite;

import java.io.File;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Post edition task which invokes the cache manager to flush the publication cache.
 * Caching not currently being used.  This task should be moved if needed so dependency on com.percussion.delivery and percCachingAPI.jar does not exist in core
 */
@Deprecated
public class PSFlushPublicationCacheTask implements IPSEditionTask
{

   public TaskType getType()
   {
      return TaskType.POSTEDITION;
   }

   @SuppressWarnings("unused")
   public void perform(IPSEdition edition, IPSSite site, Date startTime,
         Date endTime, long jobId, long duration, boolean success,
         Map<String, String> params, IPSEditionTaskStatusCallback status)
      throws Exception
   {
      Validate.notNull(edition, "edition may not be null");

      Validate.notNull(site, "site may not be null");

      int maxUrls = MAX_URLS;
      String maxUrlsParam = params.get("maxUrls");
      if (StringUtils.isNumeric(maxUrlsParam))
      {
          int parsed = Integer.parseInt(maxUrlsParam.trim());
          if (parsed >= 0)
          {
              maxUrls = parsed;
          }
      }

      if (ms_log.isDebugEnabled())
      {
          ms_log.debug("Maximum urls: " + maxUrls);
      }

      //flushPageCache(jobId, site.getName(), maxUrls);
   }

   @SuppressWarnings("unused")
   public void init(IPSExtensionDef def, File codeRoot)
      throws PSExtensionException
   {
      // No init
   }
   
   /**
    * Logger.
    */
   private static final Logger ms_log = LogManager.getLogger(PSFlushPublicationCacheTask.class);
   
   /**
    * Default maximum number of urls to submit for page cache flushing.
    */
   private static final int MAX_URLS = 5000;
   
   /**
    * Base url for the caching service.
    */
   private static final String CACHE_SVC_URL = "/perc-caching/manager"; 
    
   /**
    * Action url for the flush cache request.
    */
   private static final String FLUSH_CACHE_URL = CACHE_SVC_URL + "/invalidate";
}
