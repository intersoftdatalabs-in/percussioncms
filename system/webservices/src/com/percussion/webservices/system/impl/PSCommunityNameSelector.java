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

package com.percussion.webservices.system.impl;

import com.percussion.services.security.data.PSCommunity;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Picks one community from a {@code findCommunitiesByName} result.
 *
 * <p>{@code findCommunitiesByName} uses SQL {@code LIKE %name%} where {@code _} is a
 * single-character wildcard, so {@code Corporate_Investments} also matches
 * {@code Corporate_Investments_Admin}. Session switch (#3506) must prefer an
 * exact name match.
 */
public final class PSCommunityNameSelector {
  private PSCommunityNameSelector() {}

  /**
   * @return the unique exact (case-insensitive) match; otherwise the sole LIKE
   *     hit; otherwise {@code null}
   */
  public static PSCommunity select(List<PSCommunity> candidates, String name) {
    if (candidates == null || candidates.isEmpty() || StringUtils.isBlank(name)) {
      return null;
    }
    String wanted = name.trim();
    List<PSCommunity> exact = new ArrayList<>();
    for (PSCommunity community : candidates) {
      if (community != null && wanted.equalsIgnoreCase(community.getName())) {
        exact.add(community);
      }
    }
    if (exact.size() == 1) {
      return exact.get(0);
    }
    if (exact.isEmpty() && candidates.size() == 1) {
      return candidates.get(0);
    }
    return null;
  }
}
