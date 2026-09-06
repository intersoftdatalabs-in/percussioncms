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

package com.percussion.rest.problems;

import java.util.List;

/**
 * Adaptor for Developer Problems (Workbench §12.4). Read-only validation/design
 * problems for the open editor/session. Distinct from pipeline application
 * validation ({@code GET /pipelines/{id}/validation}).
 */
public interface IProblemsAdaptor {

  /**
   * List session design problems. Optional {@code fixture} selects a known
   * invalid open-editor fixture ({@code invalid-session}).
   *
   * @param fixture catalog token or {@code null}/{@code blank} for the default session list
   * @return never {@code null}; empty when the session has no problems
   */
  List<DesignProblem> listProblems(String fixture);
}
