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

package com.percussion.cx.javafx;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses and stores browser window properties from a comma-separated string.
 * Supports toolbar, location, directories, status, menubar, scrollbars, resizable, 
 * left, width, and height properties.
 * 
 * @since 8.0.0
 */
public class BrowserProps {
  static Logger log = LogManager.getLogger(BrowserProps.class);

  private boolean toolbar = false;
  private boolean location = false;
  private boolean directories = false;
  private boolean status = false;
  private boolean menubar = false;
  private boolean scrollbars = false;
  private boolean resizable = false;
  private int left = 200;
  private int height = 200;
  private int width = 200;

  /**
   * Creates a new BrowserProps instance by parsing the given properties string.
   * 
   * @param props comma-separated window properties (e.g., "toolbar=0,location=0,status=1")
   */
  public BrowserProps(String props) {
    String[] propList = StringUtils.split(props, ",");

    for (String prop : propList) {
      String[] propEntryList = StringUtils.split(prop.trim(), "=");
      if (propEntryList.length == 2) {
        String key = propEntryList[0].toLowerCase();
        String value = propEntryList[1];
        try {
          switch (key) {
            case "toolbar":
              this.toolbar = booleanValue(value);
              break;
            case "location":
              this.location = booleanValue(value);
              break;
            case "directories":
              this.directories = booleanValue(value);
              break;
            case "status":
              this.status = booleanValue(value);
              break;
            case "menubar":
              this.menubar = booleanValue(value);
              break;
            case "scrollbars":
              this.scrollbars = booleanValue(value);
              break;
            case "resizable":
              this.resizable = booleanValue(value);
              break;
            case "left":
              this.left = intValue(value);
              break;
            case "width":
              this.width = intValue(value);
              break;
            case "height":
              this.height = intValue(value);
              break;
          }
        } catch (Exception e) {
          log.debug(
              "cannot parse window property " + key + " value=" + value + " from string " + props);
        }
      }
    }
  }

  private int intValue(String value) {
    return Integer.parseInt(value);
  }

  private boolean booleanValue(String value) {
    value = value.toLowerCase();
    return value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1");
  }

  /**
   * @return true if toolbar is enabled
   */
  public boolean isToolbar() {
    return this.toolbar;
  }

  /**
   * @return true if location bar is enabled
   */
  public boolean isLocation() {
    return this.location;
  }

  /**
   * @return true if directories are enabled
   */
  public boolean isDirectories() {
    return this.directories;
  }

  /**
   * @return true if status bar is enabled
   */
  public boolean isStatus() {
    return this.status;
  }

  /**
   * @return true if menu bar is enabled
   */
  public boolean isMenubar() {
    return this.menubar;
  }

  /**
   * @return true if scrollbars are enabled
   */
  public boolean isScrollbars() {
    return this.scrollbars;
  }

  /**
   * @return true if window is resizable
   */
  public boolean isResizable() {
    return this.resizable;
  }

  /**
   * @return the left position of the window
   */
  public int getLeft() {
    return this.left;
  }

  /**
   * @return the height of the window
   */
  public int getHeight() {
    return this.height;
  }

  /**
   * @return the width of the window
   */
  public int getWidth() {
    return this.width;
  }
}
