/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.cecontrols;

import java.util.List;

/** Adaptor for content editor control catalog (UI-01 read). */
public interface IControlAdaptor {

  /** List system and user CE controls. Never null. */
  List<ControlDef> listControls();

  /** Resolve by control name (case-insensitive). Null if missing or unsafe key. */
  ControlDef findControlByName(String name);
}
