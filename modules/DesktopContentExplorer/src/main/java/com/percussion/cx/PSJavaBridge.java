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

import com.percussion.cx.javafx.PSDesktopExplorerWindow;
import com.percussion.cx.javafx.PSFileSaver;
import com.percussion.cx.javafx.PSWindowManager;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.concurrent.CountDownLatch;
import netscape.javascript.JSObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridge object exposed to the embedded web view's JavaScript as {@code java}. Provides methods for
 * logging, opening and closing windows, saving files and accessing the system clipboard.
 */
public class PSJavaBridge implements ClipboardOwner {

  static Logger log = LogManager.getLogger(PSJavaBridge.class);

  /** Latch used to coordinate initialization of the bridge with the JavaScript side. */
  CountDownLatch initialized = new CountDownLatch(1);

  private PSDesktopExplorerWindow frame;

  /**
   * Constructs the bridge for the given desktop explorer window.
   *
   * @param frame the owning window, may not be <code>null</code>.
   */
  public PSJavaBridge(PSDesktopExplorerWindow frame) {
    this.frame = frame;
  }

  /**
   * Logs a message from JavaScript at debug level.
   *
   * @param text the message text from JavaScript, may be <code>null</code>.
   */
  public void log(String text) {
    log.debug("javascript.log: " + text);
  }

  /**
   * Logs an error message from JavaScript at debug level.
   *
   * @param text the error text from JavaScript, may be <code>null</code>.
   */
  public void error(String text) {
    log.debug("javascript.error: " + text);
  }

  /** Closes the owning window. */
  public void closeWindow() {
    frame.setClosed(true);
    frame.closeDceWindow();
  }

  /**
   * Closes the named window.
   *
   * @param windowName the target identifier of the window to close, may not be <code>null</code>.
   */
  public void closeWindow(String windowName) {
    PSDesktopExplorerWindow window = PSWindowManager.getInstance().getWindow(windowName);
    window.setClosed(true);
    window.closeDceWindow();
  }

  /**
   * Saves the file at the supplied URL to the local file system via the file saver dialog.
   *
   * @param binaryURL the URL of the file to save, may not be <code>null</code>.
   * @param fileName the suggested file name, may not be <code>null</code>.
   */
  public void saveFile(String binaryURL, String fileName) {
    PSFileSaver fileSaver = new PSFileSaver(binaryURL, fileName);
    fileSaver.startFileSaver();
  }

  /**
   * Opens a child window with the supplied URL and properties.
   *
   * @param url the URL to load in the new window, may not be <code>null</code>.
   * @param name the target name of the new window, may not be <code>null</code>.
   * @param specs the window specifications, may not be <code>null</code>.
   * @param replace if <code>true</code> replaces any existing window with the same name.
   * @return the JavaScript window object for the new window, may be <code>null</code>.
   */
  public JSObject openWindow(String url, String name, String specs, boolean replace) {

    PSDesktopExplorerWindow window = frame.openChildWindow(url, name, specs, null, null);
    return window.getJSWindow();
  }

  /**
   * Gets the JavaScript window object for the named window, if any.
   *
   * @param name the target name of the window to look up, may not be <code>null</code>.
   * @return the JavaScript window object, or <code>null</code> if no such window exists.
   */
  public JSObject getWindowByName(String name) {
    PSDesktopExplorerWindow window = PSWindowManager.getInstance().getWindow(name);
    return window == null ? null : window.getJSWindow();
  }

  /**
   * Gets a new clipboard data bridge instance for use from JavaScript.
   *
   * @return a new clipboard data bridge, never <code>null</code>.
   */
  public JSClipDataBridge getClipboardData() {
    return new JSClipDataBridge();
  }

  /**
   * Gets a new clipboard event bridge instance for use from JavaScript.
   *
   * @return a new clipboard event bridge, never <code>null</code>.
   */
  public JSClipEventBridge getClipboardDataEvent() {
    return new JSClipEventBridge();
  }

  @Override
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    log.debug("Lost clipboard ownership");
  }
}
