/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

import java.util.List;

/** Adaptor for CX search design catalog (UI-06 read) and design-search execute façade. */
public interface ISearchAdaptor {

  List<SearchDef> listSearches();

  /** Resolve by name or GUID string. Returns null if missing/unsafe. */
  SearchDef findSearchByKey(String idOrName);

  /**
   * Execute a design search by name, GUID string, or numeric id with optional scope/paging
   * overrides. Preserves design field operators (unlike equality-only searchmanagement criteria).
   *
   * @param idOrName search key (same rules as {@link #findSearchByKey})
   * @param request optional overrides; {@code null} treated as empty defaults by implementations
   * @return paged results, or {@code null} when the search is missing/unsafe
   * @throws IllegalArgumentException when the body is invalid or the search type cannot be executed
   *     (e.g. custom URL searches)
   */
  SearchExecuteResult executeSearch(String idOrName, SearchExecuteRequest request);
}
