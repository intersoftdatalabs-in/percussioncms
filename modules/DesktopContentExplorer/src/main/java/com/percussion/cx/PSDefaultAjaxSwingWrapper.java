/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
 * Default implementation of IPSAjaxSwingWrapper for browser context. Provides no-op implementations
 * for all AjaxSwing-specific operations since these are not needed when running in a standard web
 * browser.
 */
public class PSDefaultAjaxSwingWrapper implements IPSAjaxSwingWrapper {

  /** Default constructor. */
  public PSDefaultAjaxSwingWrapper() {
    // no-op
  }

  /**
   * No-op implementation - browser context does not need AjaxSwing handlers.
   *
   * @param applet the content explorer applet (ignored)
   */
  public void createAjaxSwingHandlers(PSContentExplorerApplet applet) {
    // DO Nothing

  }

  /**
   * No-op implementation - browser context uses standard window management.
   *
   * @param conn the HTTP connection (ignored)
   * @param url the URL (ignored)
   * @param target the target window (ignored)
   * @param style the style attributes (ignored)
   * @throws MalformedURLException never thrown
   */
  public void openWindow(PSHttpConnection conn, String url, String target, String style)
      throws MalformedURLException {
    // DO Nothing

  }

  /**
   * No-op implementation - browser refresh handled by browser.
   *
   * @param conn the HTTP connection (ignored)
   * @throws MalformedURLException never thrown
   */
  public void refreshWindow(PSHttpConnection conn) throws MalformedURLException {
    // DO Nothing

  }

  /**
   * Always returns false - AjaxSwing is not enabled in browser context.
   *
   * @return false indicating AjaxSwing is not enabled
   */
  public boolean isAjaxSwingEnabled() {
    return false;
  }
}
