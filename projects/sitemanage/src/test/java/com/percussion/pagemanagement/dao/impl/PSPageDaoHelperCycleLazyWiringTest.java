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
package com.percussion.pagemanagement.dao.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.share.dao.IPSFolderHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

/**
 * Regression test for the Spring constructor-injection cycle observed on Docker CMS after
 * contentItemDao {@code @Lazy} (#2435):
 *
 * <pre>
 * folderHelper -&gt; recycleService -&gt; widgetAssetRelationshipService
 *   -&gt; pageIndexService -&gt; pageDaoHelper -&gt; folderHelper
 * </pre>
 *
 * <p>Class-level {@code @Lazy} on {@link PSPageDaoHelper} only defers first request; constructor
 * dependencies still resolve eagerly. The {@code IPSFolderHelper} constructor parameter must be
 * {@link Lazy @Lazy} so Spring injects a proxy and does not re-enter {@code folderHelper} while it
 * is still being created (#2423 / #2437).
 */
@Tag("UnitTest")
public class PSPageDaoHelperCycleLazyWiringTest {

  @Test
  public void folderHelperConstructorParameterIsLazy() throws NoSuchMethodException {
    Constructor<PSPageDaoHelper> ctor =
        PSPageDaoHelper.class.getDeclaredConstructor(
            com.percussion.webservices.content.IPSContentWs.class,
            IPSFolderHelper.class,
            com.percussion.share.service.IPSIdMapper.class);

    assertNotNull(ctor, "PSPageDaoHelper constructor signature must match");

    Parameter folderHelperParam = null;
    for (Parameter p : ctor.getParameters()) {
      if (IPSFolderHelper.class.isAssignableFrom(p.getType())) {
        folderHelperParam = p;
        break;
      }
    }

    assertNotNull(folderHelperParam, "Expected an IPSFolderHelper constructor parameter");
    assertTrue(
        folderHelperParam.isAnnotationPresent(Lazy.class),
        "IPSFolderHelper constructor parameter must be @Lazy to break the "
            + "folderHelper↔pageDaoHelper Spring cycle (see #2423 / #2437)");
  }
}
