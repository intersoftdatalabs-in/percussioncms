/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.views;

import java.util.List;

/** Adaptor for CX view design catalog (UI-07 read). */
public interface IViewAdaptor {

  List<ViewDef> listViews();

  /** Resolve by name or GUID string. Returns null if missing/unsafe. */
  ViewDef findViewByKey(String idOrName);
}
