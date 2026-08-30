/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.rest.test.apibridge;

import com.percussion.rest.Guid;
import com.percussion.rest.itemfilter.IItemFilterAdaptor;
import com.percussion.rest.itemfilter.ItemFilter;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IItemFilterAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code ItemFilterResource}.
 */
@Component
@Lazy
public class TestItemFilterAdaptor implements IItemFilterAdaptor {

  @Override
  public List<ItemFilter> getItemFilters() {
    return null;
  }

  @Override
  public ItemFilter updateOrCreateItemFilter(ItemFilter filter) {
    return null;
  }

  @Override
  public void deleteItemFilter(Guid itemFilterId) {
    // No-op for test adaptor
  }

  @Override
  public ItemFilter getItemFilter(Guid itemFilterId) {
    return null;
  }

  @Override
  public ItemFilter findItemFilter(String idOrName) {
    return null;
  }
}
