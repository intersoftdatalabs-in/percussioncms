/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.views;

import java.util.List;

/** Adaptor for CX view design catalog (UI-07 read) and standard-view execute façade. */
public interface IViewAdaptor {

  List<ViewDef> listViews();

  /** Resolve by name or GUID string. Returns null if missing/unsafe. */
  ViewDef findViewByKey(String idOrName);

  /**
   * Execute a standard (field-criteria) design view by name, GUID string, or numeric id with
   * optional scope/paging overrides. Loads the view via the views design catalog ({@code
   * IPSUiDesignWs} find/load views) — not the search catalog.
   *
   * @param idOrName view key (same rules as {@link #findViewByKey})
   * @param request optional overrides; {@code null} treated as empty defaults by implementations
   * @return paged results, or {@code null} when the view is missing/unsafe
   * @throws IllegalArgumentException when the body is invalid or the view type cannot be executed
   *     (e.g. custom URL / Inbox-family views)
   */
  ViewExecuteResult executeView(String idOrName, ViewExecuteRequest request);
}
