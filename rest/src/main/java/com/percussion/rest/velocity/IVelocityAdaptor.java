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

package com.percussion.rest.velocity;

import java.util.List;

/**
 * Adaptor for Velocity template authoring helpers (AS-09 snippet catalog).
 *
 * <p>Production implementation lives in sitemanage {@code VelocityAdaptor}. Editing of System/User
 * Velocity config files is out of scope (SY-02).
 */
public interface IVelocityAdaptor {

  /**
   * List the built-in Velocity snippet catalog (Appendix C field/slot/misc macros and samples).
   * Never null.
   */
  List<VelocitySnippet> listSnippets();

  /**
   * Resolve one snippet by stable catalog id (case-insensitive). Null when missing or blank.
   *
   * @param id catalog id
   */
  VelocitySnippet findSnippetById(String id);
}
