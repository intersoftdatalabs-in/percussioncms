/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.views;

import java.util.List;

/** Adaptor for CX view design catalog (UI-07 read) and view execute façade. */
public interface IViewAdaptor {

  List<ViewDef> listViews();

  /** Resolve by name or GUID string. Returns null if missing/unsafe. */
  ViewDef findViewByKey(String idOrName);

  /**
   * Execute a design view by name, GUID string, or numeric id with optional scope/paging
   * overrides. Loads the view via the views design catalog ({@code IPSUiDesignWs} find/load
   * views) — not the search catalog.
   *
   * <p>Standard (field-criteria) views run through the design search engine. Custom-URL views
   * in the Inbox family ({@code sys_cxViews/inbox}, outbox, recent, session, checkedoutbyme,
   * duplicatefolderpaths) invoke the classic app resource and map rows to Explorer items.
   *
   * @param idOrName view key (same rules as {@link #findViewByKey})
   * @param request optional overrides; {@code null} treated as empty defaults by implementations
   * @return paged results, or {@code null} when the view is missing/unsafe
   * @throws IllegalArgumentException when the body is invalid or the custom URL is unsupported
   */
  ViewExecuteResult executeView(String idOrName, ViewExecuteRequest request);
}
