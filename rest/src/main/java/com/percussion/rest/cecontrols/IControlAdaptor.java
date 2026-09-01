/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.cecontrols;

import java.util.List;

/**
 * Adaptor for content editor control catalog (UI-01 read/write).
 *
 * <p>Write methods persist <strong>user</strong> controls through {@code
 * PSCustomControlManager} (file under {@code rx_resources/stylesheets/controls} plus import
 * list). System controls are read-only packaged defaults.
 */
public interface IControlAdaptor {

  /** List system and user CE controls. Never null. */
  List<ControlDef> listControls();

  /** Resolve by control name (case-insensitive). Null if missing or unsafe key. */
  ControlDef findControlByName(String name);

  /**
   * Admin. Create and persist a user CE control (XSL file + import list).
   *
   * @param body required; {@code name} is the unique catalog key
   * @return the persisted control
   */
  ControlDef createControl(ControlDef body);

  /**
   * Admin. Update and persist a user CE control by name. Does not mutate packaged system
   * controls.
   *
   * @param name catalog key (same rules as {@link #findControlByName})
   * @param body required writable fields (displayName, description, dimension, choiceSet,
   *     optional xslSource)
   * @return the persisted control, or {@code null} when missing/unsafe
   */
  ControlDef saveControl(String name, ControlDef body);

  /**
   * Admin. Delete a user CE control by name (removes the user XSL file and refreshes imports).
   * Does not mutate packaged system controls.
   *
   * @param name catalog key (same rules as {@link #findControlByName})
   * @return {@code true} when deleted, {@code false} when missing/unsafe
   */
  boolean deleteControl(String name);
}
