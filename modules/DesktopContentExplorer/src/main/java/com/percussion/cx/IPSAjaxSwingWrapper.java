/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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

package com.percussion.cx;

import com.percussion.util.PSHttpConnection;
import java.net.MalformedURLException;

/**
 * Interface for AjaxSwing wrapper functionality in the Content Explorer desktop application.
 * Provides methods to manage AjaxSwing handlers, window creation, and refresh operations.
 */
public interface IPSAjaxSwingWrapper {

  /**
   * Creates AjaxSwing handlers for the given content explorer applet.
   *
   * @param applet the content explorer applet to create handlers for
   */
  public void createAjaxSwingHandlers(PSContentExplorerApplet applet);

  /**
   * Checks whether AjaxSwing functionality is enabled.
   *
   * @return true if AjaxSwing is enabled, false otherwise
   */
  public boolean isAjaxSwingEnabled();

  /**
   * Opens a new window with the specified connection and URL parameters.
   *
   * @param conn the HTTP connection to use
   * @param url the URL to open in the new window
   * @param target the target window or frame name
   * @param style the style attributes for the window
   * @throws MalformedURLException if the URL is malformed
   */
  public void openWindow(PSHttpConnection conn, String url, String target, String style)
      throws MalformedURLException;

  /**
   * Refreshes the current AjaxSwing window using the provided connection.
   *
   * @param httpConnection the HTTP connection to use for the refresh
   * @throws MalformedURLException if the stored URL is malformed
   */
  public void refreshWindow(PSHttpConnection httpConnection) throws MalformedURLException;
}
