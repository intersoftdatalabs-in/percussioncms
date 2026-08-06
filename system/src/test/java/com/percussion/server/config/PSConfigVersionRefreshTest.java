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

package com.percussion.server.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSConfig;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Regression: repeated saves of the cached relationships {@link PSConfig} during package install
 * must re-sync Hibernate {@code @Version} from the DB, otherwise merge fails with "Row was already
 * updated or deleted by another transaction".
 */
public class PSConfigVersionRefreshTest {

  @Test
  public void testRefreshOptimisticLockVersionUpdatesCachedEntity() throws Exception {
    PSConfig cached = mock(PSConfig.class);
    when(cached.getName()).thenReturn("relationships");
    when(cached.getVersion()).thenReturn(3);

    PSConfig db = mock(PSConfig.class);
    when(db.getVersion()).thenReturn(7);

    IPSCmsObjectMgr mgr = mock(IPSCmsObjectMgr.class);
    when(mgr.findConfig("relationships")).thenReturn(Optional.of(db));

    PSConfigManager.refreshOptimisticLockVersion(cached, mgr);

    org.mockito.Mockito.verify(cached).setVersion(7);
  }

  @Test
  public void testRefreshOptimisticLockVersionNoopsWhenMissing() throws Exception {
    PSConfig cached = mock(PSConfig.class);
    when(cached.getName()).thenReturn("relationships");

    IPSCmsObjectMgr mgr = mock(IPSCmsObjectMgr.class);
    when(mgr.findConfig("relationships")).thenReturn(Optional.empty());

    PSConfigManager.refreshOptimisticLockVersion(cached, mgr);

    org.mockito.Mockito.verify(cached, org.mockito.Mockito.never())
        .setVersion(org.mockito.ArgumentMatchers.any());
  }
}
