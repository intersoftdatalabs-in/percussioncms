/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.guitools;

import java.awt.*;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.*;

/**
 * This is a class that aids in retrieving different kinds of resources from a resource bundle. It
 * has static methods to get mnemonics, accelerator keys, icons, etc.
 *
 * <p>To use this class, resources must use the following key naming convention:
 *
 * <ul>
 *   <li>mn_&lt;base_resource_key&gt; for mnemonics
 *   <li>ks_&lt;base_resource_key&gt; for accel keys
 *   <li>gif_&lt;base_resource_key&gt; for icon file names
 *   <li>tt_&lt;base_resource_key&gt; for tooltip text
 *   <li>pt_&lt;base_resource_key&gt; for Points (can be used for cursor hotspot)
 * </ul>
 *
 * This allows all resources associated with the same UI object to be accessed with the 'same' key
 * from the caller's point of view.
 */
public class ResourceHelper {

  /** No-op default constructor. */
  public ResourceHelper() {}

  /**
   * Returns the character that is the mnemonic for the for the supplied action, or 0 if the action
   * does not have a mnemonic.
   *
   * @param rb the resource bundle to search; must not be {@code null}.
   * @param strBaseKeyName the base key name (the {@code mn_} prefix is added internally).
   * @return the mnemonic character, or {@code 0} if the action does not have a mnemonic.
   */
  public static char getMnemonic(PSResources rb, String strBaseKeyName) {
    try {
      return (rb.getCharacter("mn_" + strBaseKeyName));
    } catch (MissingResourceException e) {
      return (0);
    }
  }

  /**
   * Checks the supplied resource bundle for an accelerator key by the supplied name. If one is
   * found it is returned, otherwise null is returned.
   *
   * @param rb the resource bundle to search; must not be {@code null}.
   * @param strBaseKeyName the base key name (the {@code ks_} prefix is added internally).
   * @return the accelerator {@link KeyStroke}, or {@code null} if not found.
   */
  public static KeyStroke getAccelKey(PSResources rb, String strBaseKeyName) {
    try {
      return (rb.getKeyStroke("ks_" + strBaseKeyName));
    } catch (MissingResourceException e) {
      return (null);
    }
  }

  /**
   * Checks the supplied resource bundle for a tool tip string by the supplied name. If a non-empty
   * one is found it is returned, otherwise null is returned.
   *
   * @param rb the resource bundle to search; must not be {@code null}.
   * @param strBaseKeyName the base key name (the {@code tt_} prefix is added internally).
   * @return the tool tip string, or {@code null} if not found or empty.
   */
  public static String getToolTipText(PSResources rb, String strBaseKeyName) {
    try {
      String strTip = rb.getString("tt_" + strBaseKeyName);
      return (strTip.length() > 0 ? strTip : null);
    } catch (MissingResourceException e) {
      return (null);
    }
  }

  /**
   * Checks the supplied resource bundle for an icon filename whose key is gif_strBaseKeyName. If a
   * non-empty one is found, the icon is loaded and it is returned, otherwise null is returned. Uses
   * the Class instance of rb to load the image file.
   *
   * @param rb the resource bundle to search for the filename, using strBaseKeyName as the key
   * @param strBaseKeyName must be a valid string
   * @throws MissingResourceException If the icon filename is present in the resource bundle, but
   *     the file cannot be found or loaded.
   */
  public static ImageIcon getIcon(PSResources rb, String strBaseKeyName) {
    String strFilename = null;
    try {
      strFilename = rb.getString("gif_" + strBaseKeyName);
    } catch (MissingResourceException e) {
      return (null);
    }

    if (strFilename.length() > 0) {
      return BitmapManager.getBitmapManager(rb.getClass()).getImage(strFilename);
    }
    return (null);
  }

  /**
   * Returns the point found in the supplied resource bundle under the supplied key name.
   *
   * @param rb the resource bundle to search; must not be {@code null}.
   * @param strBaseKeyName the base key name (the {@code pt_} prefix is added internally).
   * @return the {@link Point} found in the resource bundle; a fresh {@code Point(0,0)} is returned
   *     if one is not found or is of the wrong type.
   */
  public static Point getPoint(PSResources rb, String strBaseKeyName) {
    Point pt = null;
    try {
      pt = (Point) rb.getObject("pt_" + strBaseKeyName);
    } catch (MissingResourceException e) {
      final String[] astrParams = {strBaseKeyName};
      pt = new Point();
    } catch (ClassCastException e) {
      final String[] astrParams = {strBaseKeyName};
      pt = new Point();
    }
    return (pt);
  }

  /**
   * Gets the resource bundle used by all the classes in this package.
   *
   * @return the resource bundle, never <code>null</code>
   * @throws MissingResourceException if the resources properties is not found.
   */
  public static ResourceBundle getResources() {
    if (sm_res == null) {
      sm_res =
          ResourceBundle.getBundle(
              "com.percussion.guitools.GuitoolsResources", Locale.getDefault());
    }

    return sm_res;
  }

  static ResourceBundle sm_res = null;
}
