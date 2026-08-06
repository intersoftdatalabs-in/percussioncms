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

package com.percussion.cx.javafx;

import com.percussion.cx.PSSelection;
import com.percussion.cx.objectstore.PSMenuAction;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages desktop explorer windows for the content explorer application. Provides window creation,
 * tracking, and lifecycle management for multiple browser windows with various targets.
 *
 * @since 8.0.0
 */
public class PSWindowManager {

  /** Default constructor. */
  public PSWindowManager() {
    // no-op
  }

  private static PSWindowManager instance = new PSWindowManager();

  private Map<String, PSDesktopExplorerWindow> windows =
      new HashMap<String, PSDesktopExplorerWindow>();
  private Map<String, String> parents = new HashMap<String, String>();

  /** Default browser style string used when no override is supplied. */
  protected String mi_style =
      "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=0,resizable=1";

  private int defaultHeight = 400;
  private int defaultWidth = 780;

  private static final String ROOT_TARGET = "_root";

  static Logger log = LogManager.getLogger(PSWindowManager.class);

  int window_count = 0;

  private String last_opened;

  /**
   * Gets the singleton instance of the window manager.
   *
   * @return the PSWindowManager instance
   */
  public static PSWindowManager getInstance() {
    return instance;
  }

  /**
   * Adds a root window to the manager.
   *
   * @param baseFrame the desktop explorer window to add as root
   */
  public void addRoot(PSDesktopExplorerWindow baseFrame) {
    instance.windows.put(ROOT_TARGET, baseFrame);
    baseFrame.setTarget(ROOT_TARGET);
  }

  private static final Class<?>[] WINDOW_CLASSES = {
    PSPopupAppletFrame.class, PSSimpleSwingBrowser.class
  };

  /**
   * Opens a new window or returns an existing window with the specified parameters.
   *
   * @param parent the parent window identifier
   * @param url the URL to load in the window
   * @param target the target name (_blank, _parent, _self, _top, or custom name)
   * @param specs the window specifications string
   * @param mi_selection the current selection context
   * @param action the menu action to perform
   * @return the opened or existing window, or null if opening failed
   */
  public PSDesktopExplorerWindow open(
      String parent,
      String url,
      String target,
      String specs,
      PSSelection mi_selection,
      PSMenuAction action) {
    // _blank, _parent _self, _top or name
    //  force creation of unique name when blank specified so we will store the entry and it can be
    // removed on close
    if (target.equals("_blank")) target = this.getClass().getName() + "_" + window_count++;

    PSDesktopExplorerWindow window = windows.get(target);

    if (window == null) {

      for (Class<?> windowClass : WINDOW_CLASSES) {
        try {
          // Don't like too much that we have to create a new instance to check here
          window = (PSDesktopExplorerWindow) windowClass.newInstance();
          if (window != null && window.validateOpen(url, target, specs, mi_selection, action))
            break;
        } catch (InstantiationException | IllegalAccessException e) {
          log.error("Cannot instantiate window class " + windowClass.getName(), e);
        }
      }
      if (window != null)
        ;
      windows.put(target, window);
      last_opened = target;
      log.debug("Loading " + url + " to new window with target " + target);

    } else {
      if (window.getUrl() != null && target.equals(window.target) && (window.getUrl().equals(url))
          || url.isEmpty()) return window;
    }

    if (window != null) {
      StringBuffer specbuffer = null;
      if (StringUtils.isEmpty(specs) || specs.equals("undefined")) {
        specs = mi_style;
      }
      specbuffer = new StringBuffer(specs);

      if (!specs.contains("height=")) {
        specbuffer.append(",height=");
        specbuffer.append(defaultHeight);
      }
      if (!specs.contains("width=")) {
        specbuffer.append(",width=");
        specbuffer.append(defaultWidth);
      }

      window.open(parent, url, target, specbuffer.toString(), mi_selection, action);
    } else log.error("Cannot open window for url " + url + " to target " + target);

    return window;
  }

  /**
   * Closes and disposes the window with the specified name.
   *
   * @param name the target name of the window to close
   */
  public void close(String name) {
    PSDesktopExplorerWindow window = windows.get(name);
    if (window != null) {
      window.setClosed(true);
    }

    SwingUtilities.invokeLater(
        () -> {
          if (window != null) {
            window.dispose();
            windows.remove(name);
            parents.remove(name);
          }
        });
  }

  /**
   * Opens a window with an explicit parent reference.
   *
   * @param parent the parent window identifier
   * @param mi_actionurl the action URL to open
   * @param mi_target the target frame name
   * @param mi_style the window specifications
   * @param selection the current selection context
   * @param action the menu action to perform
   * @return the opened window, or null if failed
   */
  public PSDesktopExplorerWindow openWithParent(
      String parent,
      String mi_actionurl,
      String mi_target,
      String mi_style,
      PSSelection selection,
      PSMenuAction action) {
    try {
      if (parent != null) {

        URL baseUrl;

        PSDesktopExplorerWindow parentWindow = windows.get(parent);

        if (mi_actionurl.isEmpty() && mi_target.equals("_self")) {

          /*
           * This condition is true only when the code in javascript wants to close the window.
           * As javascript can't close the window which it has not opened, the workaround code is used.
           * window.open('','_self').close(); - When application is running in DCE, the handle is lost after
           * opening the window. So this piece of code would simply close the window as intended.
           */

          parentWindow.setClosed(true);
          parentWindow.closeDceWindow();

          return null;
        }

        baseUrl = new URL(parentWindow.getUrl());
        mi_actionurl = new URL(baseUrl, mi_actionurl).toString();
      }
      boolean addParent = true;
      if (mi_target.equals("_parent")) {
        if (parents.containsKey(parent)) {
          mi_target = parents.get(parent);
          addParent = false;
        }
      } else if (StringUtils.isEmpty(parent) && StringUtils.isEmpty(mi_target)
          || mi_target.equals("_self")) {
        mi_target = parent;
        addParent = false;
      }

      PSDesktopExplorerWindow window =
          open(parent, mi_actionurl, mi_target, mi_style, selection, action);

      if (window != null && addParent) parents.put(window.getTarget(), parent);

      return window;

    } catch (MalformedURLException e) {
      log.error("invalid url", e);
      return null;
    }
  }

  /**
   * Gets the window with the specified target name.
   *
   * @param target the target name of the window
   * @return the window, or null if not found
   */
  public PSDesktopExplorerWindow getWindow(String target) {
    return windows.get(target);
  }

  /**
   * Updates the default window size based on the last opened window.
   *
   * @param target the target name of the resized window
   * @param height the new height
   * @param width the new width
   */
  public void windowResized(String target, int height, int width) {
    if (last_opened.equals(target)) {
      defaultHeight = height;
      defaultWidth = width;
    }
  }
}
