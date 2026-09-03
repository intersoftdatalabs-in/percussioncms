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

package com.percussion.extensions;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.extension.PSExtensionManager;
import com.percussion.server.PSServer;
import java.util.Collections;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * {@link PSExtensionService} must not permanently capture a null manager when Spring
 * constructs the bean before {@code PSServer.init} (#4241 H2 catalog NPE).
 */
@Tag("UnitTest")
class PSExtensionServiceLazyManagerTest {

  @Test
  void resolveManagerCachesAfterServerReadyWhenConstructedTooEarly() throws Exception {
    PSExtensionManager live = mock(PSExtensionManager.class);
    when(live.getExtensionHandlerNames()).thenReturn(Collections.emptyIterator());

    try (MockedStatic<PSServer> server = mockStatic(PSServer.class)) {
      // ctor + failed resolve see null; third call is post-PSServer.init.
      server
          .when(() -> PSServer.getExtensionManager(null))
          .thenReturn(null)
          .thenReturn(null)
          .thenReturn(live);

      PSExtensionService svc = new PSExtensionService();
      assertThrows(IllegalStateException.class, svc::resolveManager);

      assertSame(live, svc.resolveManager());
      assertSame(live, svc.resolveManager());
      // Cached after first success — no further PSServer lookups.
      server.verify(() -> PSServer.getExtensionManager(null), times(3));
      svc.getExtensionHandlerNames();
      verify(live, times(1)).getExtensionHandlerNames();
    }
  }

  @Test
  void constructorCachesManagerWhenServerAlreadyReady() {
    PSExtensionManager live = mock(PSExtensionManager.class);
    try (MockedStatic<PSServer> server = mockStatic(PSServer.class)) {
      server.when(() -> PSServer.getExtensionManager(null)).thenReturn(live);
      PSExtensionService svc = new PSExtensionService();
      assertSame(live, svc.resolveManager());
      server.verify(() -> PSServer.getExtensionManager(null), times(1));
    }
  }
}
