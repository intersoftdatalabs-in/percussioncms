/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.test.multitenant;

import com.percussion.delivery.multitenant.IPSTenantInfo;
import com.percussion.delivery.multitenant.PSSimpleTenantCache;
import com.percussion.delivery.multitenant.PSTenantInfo;
import jakarta.servlet.ServletRequest;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit tests for the SimpleTenant Cache.
 *
 * @author natechadwick
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/test-beans.xml"})
public class PSSimpleTenantCacheTest {

  PSSimpleTenantCache cache;

  @BeforeEach
  public void setup() {
    cache = new PSSimpleTenantCache();
  }

  @AfterEach
  public void teardown() {
    cache = null;
  }

  /***
   * Tests basic cache operations.
   */
  @Test
  public void testBasicOps() {
    PSTenantInfo t = new PSTenantInfo();

    t.clearAPIUsage();
    t.setAPIUsageStart(new Date());
    t.setLastAuthorizationCheckDate(new Date());
    t.setTenantId("007");

    cache.put(t);

    MockHttpServletRequest req = new MockHttpServletRequest();

    IPSTenantInfo u = cache.get(t.getTenantId(), (ServletRequest) req);

    Assertions.assertEquals(1, u.getAPIUsage());
    Assertions.assertEquals(t.getAPIUsageStart(), u.getAPIUsageStart());
    Assertions.assertEquals(t.getLastAuthorizationCheckDate(), u.getLastAuthorizationCheckDate());
    Assertions.assertEquals(t.getTenantId(), u.getTenantId());
    Assertions.assertEquals(t, u);

    cache.clear();

    Assertions.assertEquals(null, cache.get("007", null));
  }
}
