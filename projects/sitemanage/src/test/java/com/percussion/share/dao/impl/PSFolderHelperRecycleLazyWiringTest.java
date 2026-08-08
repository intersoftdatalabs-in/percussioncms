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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.recycle.service.IPSRecycleService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Regression test for Spring constructor-injection cycles rooted at {@code folderHelper}'s
 * {@code recycleService} dependency (#2423 / #2437).
 *
 * <pre>
 * folderHelper -&gt; recycleService -&gt; widgetAssetRelationshipService
 *   -&gt; pageIndexService -&gt; pageDaoHelper -&gt; folderHelper
 * folderHelper -&gt; recycleService -&gt; … -&gt; contentItemDao -&gt; folderHelper
 * </pre>
 *
 * <p>Class-level {@code @Lazy} on {@link PSFolderHelper} does not break constructor edges. The
 * {@code IPSRecycleService} constructor parameter must be {@link Lazy @Lazy} so Spring injects a
 * proxy and finishes {@code folderHelper} construction before resolving the recycle subgraph.
 */
@Tag("UnitTest")
public class PSFolderHelperRecycleLazyWiringTest {

  @Test
  public void recycleServiceConstructorParameterIsLazy() throws NoSuchMethodException {
    Constructor<PSFolderHelper> ctor =
        PSFolderHelper.class.getDeclaredConstructor(
            com.percussion.webservices.content.IPSContentWs.class,
            com.percussion.share.service.IPSDataItemSummaryService.class,
            com.percussion.webservices.content.IPSContentDesignWs.class,
            com.percussion.share.service.IPSIdMapper.class,
            com.percussion.services.publisher.IPSPublisherService.class,
            com.percussion.services.system.IPSSystemService.class,
            com.percussion.services.notification.IPSNotificationService.class,
            com.percussion.services.sitemgr.IPSSiteManager.class,
            com.percussion.services.workflow.IPSWorkflowService.class,
            IPSRecycleService.class);

    assertNotNull(ctor, "PSFolderHelper constructor signature must match");

    Parameter recycleParam = null;
    for (Parameter p : ctor.getParameters()) {
      if (IPSRecycleService.class.isAssignableFrom(p.getType())) {
        recycleParam = p;
        break;
      }
    }

    assertNotNull(recycleParam, "Expected an IPSRecycleService constructor parameter");
    assertTrue(
        recycleParam.isAnnotationPresent(Lazy.class),
        "IPSRecycleService constructor parameter must be @Lazy to break "
            + "folderHelper→recycleService constructor cycles (see #2423 / #2437)");
  }
}
