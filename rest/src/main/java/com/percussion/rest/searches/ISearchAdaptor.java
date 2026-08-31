/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

import java.util.List;

/**
 * Adaptor for CX search design catalog (UI-06 read/write) and design-search execute façade.
 *
 * <p>Write methods persist through {@code IPSUiDesignWs} create/save/delete searches. Execute is
 * unchanged and is not invoked from write.
 */
public interface ISearchAdaptor {

  List<SearchDef> listSearches();

  /**
   * List CX search definitions, optionally merging CX views (Explorer saved-search picker).
   *
   * <p>Default implementation ignores {@code includeViews} and delegates to {@link
   * #listSearches()} so existing test stubs stay source-compatible.
   *
   * @param includeViews when true, include view definitions (for example {@code View_All})
   */
  default List<SearchDef> listSearches(boolean includeViews) {
    return listSearches();
  }

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

  /**
   * Admin. Create and persist a CX search ({@code createSearches} then {@code saveSearches}).
   *
   * @param body required; {@code name} is the unique catalog key
   * @return the persisted search
   */
  SearchDef createSearch(SearchDef body);

  /**
   * Admin. Update and persist a CX search by name or GUID ({@code loadSearches} lock, {@code
   * saveSearches} release). Does not steal another user's lock.
   *
   * @param idOrName catalog key (same rules as {@link #findSearchByKey})
   * @param body required writable fields (label, description, type, displayFormat)
   * @return the persisted search, or {@code null} when missing/unsafe
   */
  SearchDef saveSearch(String idOrName, SearchDef body);

  /**
   * Admin. Delete a CX search by name or GUID ({@code deleteSearches}, {@code
   * ignoreDependencies=false}). Does not steal another user's lock.
   *
   * @param idOrName catalog key (same rules as {@link #findSearchByKey})
   * @return {@code true} when deleted, {@code false} when missing/unsafe
   */
  boolean deleteSearch(String idOrName);
}
