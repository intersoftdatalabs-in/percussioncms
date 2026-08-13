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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * GH-3342: Spring fails creating {@code sitesAdaptor} when a declared method mentions {@code
 * PSVirtualSitePublishCopyResult} and that type is missing from the Rhythmyx/H2 classloader.
 *
 * <p>sitesAdaptor must load using only rest wire types / JDK types in its method descriptors.
 * perc-system must still ship the record for publisher internals.
 */
@Tag("UnitTest")
class SitesAdaptorSpringClasspathContractTest {

  private static final String COPY_RESULT_FQN =
      "com.percussion.services.virtualsite.PSVirtualSitePublishCopyResult";

  private static final String COPY_RESULT_SIMPLE = "PSVirtualSitePublishCopyResult";

  @Test
  @DisplayName("sitesAdaptor method descriptors do not mention the perc-system copy record")
  void sitesAdaptorMethodsDoNotReferenceCopyResult() {
    for (Method method : SitesAdaptor.class.getDeclaredMethods()) {
      assertFalse(
          COPY_RESULT_FQN.equals(method.getReturnType().getName()),
          () -> "return type of " + method + " must not be " + COPY_RESULT_SIMPLE);
      for (Class<?> param : method.getParameterTypes()) {
        assertFalse(
            COPY_RESULT_FQN.equals(param.getName()),
            () -> "parameter of " + method + " must not be " + COPY_RESULT_SIMPLE);
      }
    }
  }

  @Test
  @DisplayName("SitesAdaptor class file constant pool does not name the copy record")
  void sitesAdaptorClassFileDoesNotNameCopyResult() throws Exception {
    try (InputStream in = SitesAdaptor.class.getResourceAsStream("SitesAdaptor.class")) {
      assertNotNull(in, "SitesAdaptor.class resource");
      String latin1 = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
      assertFalse(
          latin1.contains(COPY_RESULT_SIMPLE),
          "SitesAdaptor bytecode must not mention "
              + COPY_RESULT_SIMPLE
              + " (Spring lookup-method resolution / class load)");
    }
  }

  @Test
  @DisplayName("perc-system record is loadable on the sitemanage test classpath")
  void percSystemCopyResultIsOnSitemanageClasspath() throws Exception {
    Class<?> type = Class.forName(COPY_RESULT_FQN);
    assertTrue(type.isRecord(), COPY_RESULT_FQN + " must be the perc-system record");
    assertNotNull(type.getProtectionDomain());
  }
}
