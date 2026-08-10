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

package com.percussion.packages.pagexml;

/**
 * How package build stages modern page {@code component-package.json} sources into deployer {@code
 * TemplateDef} archive entries (issue #2806 / parent #2630).
 *
 * <ul>
 *   <li>{@link #DUAL_SHIP} — materialize root {@code *.templateDef} before reorganize (legacy dual-ship
 *       bridge from #2786)
 *   <li>{@link #NATIVE} — convert modern pages directly into archive {@code TemplateDef-N/} folders
 *       without dual-ship root files (preferred for converted packages)
 * </ul>
 */
public enum PSPageXmlInstallMode {
  /** Dual-ship: write root {@code *.templateDef} so existing reorganize mapping picks them up. */
  DUAL_SHIP,
  /**
   * Native: stage {@code TemplateDef-N/&lt;stem&gt;.templateDef} from modern {@code pages/} after
   * reorganize; no root dual-ship materialization.
   */
  NATIVE
}
