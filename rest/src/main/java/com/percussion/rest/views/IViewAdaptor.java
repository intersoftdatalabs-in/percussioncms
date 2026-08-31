/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.views;

import java.util.List;

/**
 * Adaptor for CX view design catalog (UI-07 read/write) and view execute façade.
 *
 * <p>Write methods persist through {@code IPSUiDesignWs} create/save/delete views. Execute is
 * unchanged and is not invoked from write.
 */
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

  /**
   * Admin. Create and persist a CX standard (field-criteria) view ({@code createViews} then
   * {@code saveViews}).
   *
   * @param body required; {@code name} is the unique catalog key
   * @return the persisted view
   */
  ViewDef createView(ViewDef body);

  /**
   * Admin. Update and persist a CX view by name or GUID ({@code loadViews} lock, {@code
   * saveViews} release). Does not steal another user's lock. Inbox-family and custom URL views
   * are not mutated.
   *
   * @param idOrName catalog key (same rules as {@link #findViewByKey})
   * @param body required writable fields (label, description, type, displayFormat)
   * @return the persisted view, or {@code null} when missing/unsafe
   */
  ViewDef saveView(String idOrName, ViewDef body);

  /**
   * Admin. Delete a CX view by name or GUID ({@code deleteViews}, {@code
   * ignoreDependencies=false}). Does not steal another user's lock. Inbox-family and custom URL
   * views return conflict (not deleted).
   *
   * @param idOrName catalog key (same rules as {@link #findViewByKey})
   * @return {@code true} when deleted, {@code false} when missing/unsafe
   */
  boolean deleteView(String idOrName);
}
