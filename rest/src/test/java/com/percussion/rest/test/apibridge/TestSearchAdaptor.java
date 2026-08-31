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

import com.percussion.rest.searches.ISearchAdaptor;
import com.percussion.rest.searches.SearchDef;
import com.percussion.rest.searches.SearchExecuteRequest;
import com.percussion.rest.searches.SearchExecuteResult;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Spring test stub for {@link ISearchAdaptor}. Required for ApplicationContext load. */
@Component
@Lazy
public class TestSearchAdaptor implements ISearchAdaptor {

  @Override
  public List<SearchDef> listSearches() {
    return listSearches(false);
  }

  @Override
  public List<SearchDef> listSearches(boolean includeViews) {
    if (!includeViews) {
      return List.of();
    }
    SearchDef viewAll = new SearchDef();
    viewAll.setName("View_All");
    viewAll.setLabel("All");
    viewAll.setType("View");
    return List.of(viewAll);
  }

  @Override
  public SearchDef findSearchByKey(String idOrName) {
    return null;
  }

  @Override
  public SearchExecuteResult executeSearch(String idOrName, SearchExecuteRequest request) {
    SearchExecuteResult empty = new SearchExecuteResult();
    empty.setChildren(List.of());
    empty.setTotalCount(0);
    empty.setStartIndex(1);
    return empty;
  }

  @Override
  public SearchDef createSearch(SearchDef body) {
    return body;
  }

  @Override
  public SearchDef saveSearch(String idOrName, SearchDef body) {
    return body;
  }

  @Override
  public boolean deleteSearch(String idOrName) {
    return false;
  }
}
