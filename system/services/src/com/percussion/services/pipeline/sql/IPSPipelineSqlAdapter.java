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
   * Transaction modes for multi-plan mutation batches (mirrors IR {@code transactionMode}).
   *
   * <ul>
   *   <li>{@link #TX_NONE} — each plan auto-commits independently (default when unset)
   *   <li>{@link #TX_ROW} — each plan runs in its own explicit transaction (commit per plan)
   *   <li>{@link #TX_ALL} — all plans share one connection/transaction; any failure rolls back all
   * </ul>
   */
  String TX_NONE = "none";

  String TX_ROW = "row";
  String TX_ALL = "all";

  /**
   * Execute a SELECT plan and return rows as ordered maps (column label → value).
   *
   * @param plan must be {@link PSPipelineSqlPlan.Kind#QUERY}
   */
  List<Map<String, Object>> query(PSPipelineSqlPlan plan) throws PSPipelineIrException;

  /**
   * Execute a single INSERT/UPDATE/DELETE plan (auto-commit / connection-per-call).
   *
   * @param plan must be {@link PSPipelineSqlPlan.Kind#UPDATE}
   * @return total affected row count
   */
  int update(PSPipelineSqlPlan plan) throws PSPipelineIrException;

  /**
   * Execute multiple mutation plans under the given transaction mode.
   *
   * @param plans non-null; each must be {@link PSPipelineSqlPlan.Kind#UPDATE}
   * @param transactionMode {@link #TX_NONE}, {@link #TX_ROW}, or {@link #TX_ALL} (null/blank → none)
   * @return sum of affected row counts
   */
  int updateAll(List<PSPipelineSqlPlan> plans, String transactionMode) throws PSPipelineIrException;
}
