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

package com.percussion.rest.test.apibridge;

import com.percussion.rest.communities.CommunityNewSearchDefaults;
import com.percussion.rest.communities.ICommunityNewSearchDefaultsAdaptor;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link ICommunityNewSearchDefaultsAdaptor}. Required for ApplicationContext
 * load.
 */
@Component
@Lazy
public class TestCommunityNewSearchDefaultsAdaptor implements ICommunityNewSearchDefaultsAdaptor {

  @Override
  public CommunityNewSearchDefaults getDefaults(String communityIdOrName) {
    CommunityNewSearchDefaults empty = new CommunityNewSearchDefaults();
    empty.setCommunityName(communityIdOrName);
    empty.setSearches(List.of());
    return empty;
  }

  @Override
  public CommunityNewSearchDefaults replaceDefaults(
      String communityIdOrName, CommunityNewSearchDefaults body) {
    if (body == null) {
      return getDefaults(communityIdOrName);
    }
    body.setCommunityName(communityIdOrName);
    return body;
  }
}
