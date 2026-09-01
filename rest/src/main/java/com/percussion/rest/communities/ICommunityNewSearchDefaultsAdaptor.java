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

package com.percussion.rest.communities;

/**
 * Admin adaptor for community Content Explorer new-search defaults (UI-09).
 *
 * <p>Persistence is the Workbench {@code cxNewSearch} property on design searches ({@code
 * IPSUiDesignWs} load/save). An empty set is a valid GET/PUT result, not 404.
 */
public interface ICommunityNewSearchDefaultsAdaptor {

  /**
   * Admin. Return searches marked as CX new-search defaults for the community.
   *
   * @param communityIdOrName numeric id, GUID string, or exact name
   * @return current set (possibly empty); {@code null} when the community is missing
   */
  CommunityNewSearchDefaults getDefaults(String communityIdOrName);

  /**
   * Admin. Replace the CX new-search default set for the community. Idempotent when the stored
   * set already matches.
   *
   * @param communityIdOrName numeric id, GUID string, or exact name
   * @param body required; {@code searches} may be empty to clear
   * @return the stored set after replace; {@code null} when the community is missing
   * @throws CommunityNewSearchDefaultsDesignLockException when a design lock is required or held by
   *     another user
   */
  CommunityNewSearchDefaults replaceDefaults(String communityIdOrName, CommunityNewSearchDefaults body);
}
