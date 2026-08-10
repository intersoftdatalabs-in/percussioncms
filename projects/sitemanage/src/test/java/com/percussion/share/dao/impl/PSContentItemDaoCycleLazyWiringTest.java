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

import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Regression test for the Spring constructor-injection cycle
 *
 * <pre>
 * folderHelper -> recycleService -> widgetAssetRelationshipService
 *             -> assetDao        -> contentItemDao       -> folderHelper
 * </pre>
 *
 * <p>The cycle blocks Rhythmyx startup (#2423 / slice A #2435). The fix marks {@code
 * IPSFolderHelper} in {@link PSContentItemDao}'s constructor as {@link Lazy @Lazy} so Spring
 * injects a proxy and does not try to fully resolve {@code folderHelper} while {@code folderHelper}
 * is still being created.
 *
 * <p>This test fails if the {@code @Lazy} annotation is removed or applied to the wrong parameter,
 * which would re-introduce the {@code BeanCurrentlyInCreationException} on Jetty/Rhythmyx startup.
 */
@Tag("UnitTest")
public class PSContentItemDaoCycleLazyWiringTest {

  @Test
  public void folderHelperConstructorParameterIsLazy() throws NoSuchMethodException {
    Constructor<PSContentItemDao> ctor =
        PSContentItemDao.class.getDeclaredConstructor(
            com.percussion.webservices.content.IPSContentDesignWs.class,
            com.percussion.webservices.content.IPSContentWs.class,
            com.percussion.share.service.IPSIdMapper.class,
            com.percussion.share.service.IPSDataItemSummaryService.class,
            IPSFolderHelper.class,
            com.percussion.services.legacy.IPSCmsObjectMgr.class,
            com.percussion.share.dao.IPSRelationshipCataloger.class,
            com.percussion.webservices.system.IPSSystemWs.class);

    assertNotNull(ctor, "PSContentItemDao constructor signature must match");

    Parameter[] params = ctor.getParameters();
    Parameter folderHelperParam = null;
    for (Parameter p : params) {
      if (IPSFolderHelper.class.isAssignableFrom(p.getType())) {
        folderHelperParam = p;
        break;
      }
    }

    assertNotNull(folderHelperParam, "Expected an IPSFolderHelper constructor parameter");
    assertTrue(
        folderHelperParam.isAnnotationPresent(Lazy.class),
        "IPSFolderHelper constructor parameter must be @Lazy to break the "
            + "folderHelper<->contentItemDao Spring cycle (see #2423 / #2435)");
  }
}
