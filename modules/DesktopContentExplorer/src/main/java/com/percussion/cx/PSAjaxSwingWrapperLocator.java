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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Locator class for obtaining the appropriate IPSAjaxSwingWrapper implementation. Determines at
 * runtime whether the application is running in AjaxSwing or browser context and returns the
 * corresponding wrapper implementation.
 */
public class PSAjaxSwingWrapperLocator {
  /**
   * Creates a new locator instance. Exposed explicitly only so that its Javadoc can be provided;
   * this constructor performs no initialization.
   */
  public PSAjaxSwingWrapperLocator() {}

  private static volatile IPSAjaxSwingWrapper wrapperClass;

  private static final Object lock = new Object();

  static Logger log = LogManager.getLogger(PSAjaxSwingWrapperLocator.class);

  /**
   * Gets the singleton instance of IPSAjaxSwingWrapper for the current runtime context. Detects
   * whether the application is running in AjaxSwing or browser context and returns the appropriate
   * wrapper implementation.
   *
   * @return the IPSAjaxSwingWrapper instance for the current context, never null
   */
  public static IPSAjaxSwingWrapper getInstance() {
    if (wrapperClass == null) {
      synchronized (lock) {
        if (wrapperClass == null && isAjaxSwingApplet()) {
          try {
            Class<?> c = Class.forName("com.percussion.ajaxswing.PSAjaxSwingWrapper");
            wrapperClass = (IPSAjaxSwingWrapper) c.newInstance();
            log.info("Running Applet in AjaxSwing context");
          } catch (ClassNotFoundException e) {
            log.error(
                "Running with AjaxSwing but com.percussion.ajaxswing.PSAjaxSwingWrapper not"
                    + " compiled with rxcx.",
                e);

          } catch (InstantiationException e) {
            log.error(
                "Running with AjaxSwing but Cannot instantiate"
                    + " com.percussion.ajaxswing.PSAjaxSwingWrapper",
                e);

          } catch (IllegalAccessException e) {
            log.error(
                "Running with AjaxSwing but IllegalAccess creating instance"
                    + " com.percussion.ajaxswing.PSAjaxSwingWrapper",
                e);
          }
          if (wrapperClass == null) {
            wrapperClass = new PSDefaultAjaxSwingWrapper();
            log.info("Running Applet in Browser context");
          }
        } else {
          if (wrapperClass == null) {
            wrapperClass = new PSDefaultAjaxSwingWrapper();
            log.info("Running Applet in Browser context");
          }
        }
      }
    }
    return wrapperClass;
  }

  /**
   * Checks whether the application is running in an AjaxSwing applet context. Looks for the
   * presence of AjaxSwing classes to determine the runtime environment.
   *
   * @return true if running in AjaxSwing context, false if in browser context
   */
  private static boolean isAjaxSwingApplet() {
    boolean exist = true;

    try {
      // if the class has a PSContentExplorerFrame then it is launched as an application
      Class<?> c = Class.forName("com.percussion.cx.PSContentExplorerFrame");
      if (c != null) {
        exist = false;
      }

      Class.forName("com.creamtec.ajaxswing.AjaxSwingManager");
    } catch (ClassNotFoundException e) {
      exist = false;
    }
    return exist;
  }
}
