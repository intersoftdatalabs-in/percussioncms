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

package com.percussion.services.pipeline.sql;

import com.percussion.services.pipeline.PSPipelineIrException;
import java.util.List;
import java.util.Map;

/**
 * SQL adapter for pipeline runtime. Implementations execute only parameterized plans (no string
 * concatenation of request values into SQL).
 */
public interface IPSPipelineSqlAdapter {

  /**
   * Execute a SELECT plan and return rows as ordered maps (column label → value).
   *
   * @param plan must be {@link PSPipelineSqlPlan.Kind#QUERY}
   */
  List<Map<String, Object>> query(PSPipelineSqlPlan plan) throws PSPipelineIrException;

  /**
   * Execute an INSERT/UPDATE/DELETE plan.
   *
   * @param plan must be {@link PSPipelineSqlPlan.Kind#UPDATE}
   * @return total affected row count
   */
  int update(PSPipelineSqlPlan plan) throws PSPipelineIrException;
}
