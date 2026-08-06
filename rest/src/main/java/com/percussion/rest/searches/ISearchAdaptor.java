/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

import java.util.List;

/** Adaptor for CX search design catalog (UI-06 read). */
public interface ISearchAdaptor {

  List<SearchDef> listSearches();

  /** Resolve by name or GUID string. Returns null if missing/unsafe. */
  SearchDef findSearchByKey(String idOrName);
}
