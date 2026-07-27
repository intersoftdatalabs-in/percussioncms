/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.security.impl.PSAclService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards against regression of the interface-default / Spring proxy pitfall: {@link IPSAclService}
 * default methods that call {@code *Impl} never re-enter the Spring proxy, so
 * {@code @Transactional} on the impl alone is not applied and package install fails with {@code No
 * transactional EntityManager available}.
 */
class PSAclServiceTransactionalEntryPointsTest {

  @Test
  @DisplayName("createAcl/saveAcls/deleteAcls are @Transactional on PSAclService")
  void publicEntryPointsAreTransactionalOnImpl() throws Exception {
    assertTransactional(
        "createAcl",
        com.percussion.utils.guid.IPSGuid.class,
        com.percussion.security.IPSTypedPrincipal.class);
    assertTransactional("saveAcls", List.class);
    assertTransactional("deleteAcls", List.class);
    assertTransactional("deleteAcl", com.percussion.utils.guid.IPSGuid.class);
  }

  private static void assertTransactional(String name, Class<?>... params) throws Exception {
    Method m = PSAclService.class.getMethod(name, params);
    assertNotNull(m, "PSAclService must declare " + name);
    assertTrue(
        m.isAnnotationPresent(Transactional.class),
        name + " must be @Transactional on PSAclService so Spring proxy binds an EntityManager");
  }
}
