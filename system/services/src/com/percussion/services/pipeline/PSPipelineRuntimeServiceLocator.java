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

import com.percussion.services.pipeline.http.PSPipelineHttpAdapter;
import com.percussion.services.pipeline.sql.IPSPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe locator for {@link IPSPipelineRuntimeService}.
 *
 * <p>Default production wiring is HTTP-capable (loopback/local fixture) with an unconfigured SQL
 * adapter. Tests may {@link #setPipelineRuntimeService(IPSPipelineRuntimeService)} to inject H2.
 */
public final class PSPipelineRuntimeServiceLocator {

  private static final AtomicReference<IPSPipelineRuntimeService> SERVICE = new AtomicReference<>();

  private PSPipelineRuntimeServiceLocator() {}

  /**
   * @return configured runtime service (lazy default includes HTTP adapter)
   */
  public static IPSPipelineRuntimeService getPipelineRuntimeService() {
    IPSPipelineRuntimeService svc = SERVICE.get();
    if (svc == null) {
      synchronized (PSPipelineRuntimeServiceLocator.class) {
        svc = SERVICE.get();
        if (svc == null) {
          svc =
              new PSPipelineRuntimeService(
                  PSPipelineIrServiceLocator.getPipelineIrService(),
                  new UnconfiguredSqlAdapter(),
                  new PSPipelineHttpAdapter(),
                  List.of(),
                  List.of());
          SERVICE.set(svc);
        }
      }
    }
    return svc;
  }

  /**
   * Override for tests / bootstrap. Pass {@code null} to clear.
   *
   * @param service replacement or {@code null}
   */
  public static void setPipelineRuntimeService(IPSPipelineRuntimeService service) {
    SERVICE.set(service);
  }

  /**
   * SQL path placeholder when no CMS datasource is wired. HTTP execute does not use this adapter.
   */
  static final class UnconfiguredSqlAdapter implements IPSPipelineSqlAdapter {
    @Override
    public List<Map<String, Object>> query(PSPipelineSqlPlan plan) throws PSPipelineIrException {
      throw new PSPipelineIrException("SQL pipeline adapter is not configured");
    }

    @Override
    public int update(PSPipelineSqlPlan plan) throws PSPipelineIrException {
      throw new PSPipelineIrException("SQL pipeline adapter is not configured");
    }

    @Override
    public int updateAll(List<PSPipelineSqlPlan> plans, String transactionMode)
        throws PSPipelineIrException {
      throw new PSPipelineIrException("SQL pipeline adapter is not configured");
    }
  }
}
