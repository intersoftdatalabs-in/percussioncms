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

package com.percussion.services.pipeline;

import com.percussion.server.PSServer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe locator for {@link IPSPipelineIrService}.
 *
 * <p>Default store root: {@code <rxRoot>/ObjectStore/pipeline-ir} when the server root is known;
 * otherwise {@code ./ObjectStore/pipeline-ir} relative to the process working directory (tests
 * should construct {@link PSPipelineIrService} with an explicit temp path instead).
 */
public final class PSPipelineIrServiceLocator {

  private static final AtomicReference<IPSPipelineIrService> SERVICE = new AtomicReference<>();

  private PSPipelineIrServiceLocator() {}

  /** Lazy default instance. */
  public static IPSPipelineIrService getPipelineIrService() {
    IPSPipelineIrService svc = SERVICE.get();
    if (svc == null) {
      synchronized (PSPipelineIrServiceLocator.class) {
        svc = SERVICE.get();
        if (svc == null) {
          svc = new PSPipelineIrService(defaultStoreDir());
          SERVICE.set(svc);
        }
      }
    }
    return svc;
  }

  /**
   * Override for tests. Pass {@code null} to clear.
   *
   * @param service replacement or {@code null}
   */
  public static void setPipelineIrService(IPSPipelineIrService service) {
    SERVICE.set(service);
  }

  static Path defaultStoreDir() {
    String rxRoot = null;
    try {
      rxRoot = PSServer.getRxDir() != null ? PSServer.getRxDir().getAbsolutePath() : null;
    } catch (Throwable t) {
      // Server not initialized — fall through to relative default
      rxRoot = null;
    }
    if (rxRoot == null || rxRoot.isBlank()) {
      return Paths.get("ObjectStore", "pipeline-ir").toAbsolutePath().normalize();
    }
    return Paths.get(rxRoot, "ObjectStore", "pipeline-ir").toAbsolutePath().normalize();
  }
}
