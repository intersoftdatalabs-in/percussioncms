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

package com.percussion.rx.publisher.impl;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.rx.publisher.PSRxPublisherServiceLocator;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSSite;

import java.io.File;
import java.util.Date;
import java.util.Map;


import org.apache.commons.lang3.StringUtils;

/**
 * Edition task to asynchronously invoke another edition. It is typically used
 * in a post edition task to chain another edition.
 *
 * @author Bill Langlais
 */
public class PSEditionRunner implements IPSEditionTask {

   @Override
   public TaskType getType() {
      return TaskType.PREANDPOSTEDITION;
   }

   @Override
   public void perform(IPSEdition edition, IPSSite site, Date startTime,
         Date endTime, long jobId, long duration, boolean success,
         Map<String, String> params, IPSEditionTaskStatusCallback status) throws Exception {
      if (edition == null) {
         throw new IllegalArgumentException("edition may not be null");
      }
      if (site == null) {
         throw new IllegalArgumentException("site may not be null");
      }
      String nextEditionName = params.get("Edition");
      if (StringUtils.isBlank(nextEditionName)) {
         throw new IllegalArgumentException("You must specify an Edition");
      }
      publish(nextEditionName);
   }

   /**
    * Publish the edition.
    * 
    * @param editionName - Name of the edition to be published assumed not
    * <code>null</code> or empty.
    */
   /**
    * Java 11 refactor: Uses reflection to call findEditionByName due to legacy API gap.
    * Throws clear error if method is missing or result is not IPSEdition.
    */
   private void publish(String editionName) {
      var ps = PSPublisherServiceLocator.getPublisherService();
      IPSEdition edition = null;
      try {
         var method = ps.getClass().getMethod("findEditionByName", String.class);
         var result = method.invoke(ps, editionName);
         if (result instanceof IPSEdition) {
            edition = (IPSEdition) result;
         } else {
            throw new RuntimeException("findEditionByName did not return IPSEdition");
         }
      } catch (NoSuchMethodException e) {
         throw new RuntimeException("PublisherService does not support findEditionByName(String). Legacy API gap.", e);
      } catch (Exception e) {
         throw new RuntimeException("Error invoking findEditionByName via reflection", e);
      }
      // edition null check is redundant: reflection throws if not found
      var rxPub = PSRxPublisherServiceLocator.getRxPublisherService();
      rxPub.startPublishingJob(edition.getGUID(), null);
   }

   @Override
   public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
      // No initialization required
   }
}
