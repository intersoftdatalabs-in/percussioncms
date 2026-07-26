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

import com.percussion.cx.PSContentExplorerApplet;
import com.percussion.cx.PSJavaBridge;
import com.percussion.cx.PSSelection;
import com.percussion.cx.objectstore.PSMenuAction;
import com.percussion.guitools.PSDialog;
import com.vladsch.boxed.json.BoxedJsObject;
import com.vladsch.boxed.json.BoxedJson;
import com.vladsch.javafx.webview.debugger.JfxScriptStateProvider;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for desktop explorer windows that host a JavaFX WebView inside a Swing JFrame and
 * coordinate state, selection, and the parent/child window relationship with the {@link
 * PSWindowManager}. Concrete subclasses provide the browser instance via {@link #instanceOpen()}
 * and may pre-validate the open request with {@link #validateOpen(String, String, String,
 * PSSelection, PSMenuAction)}.
 */
public abstract class PSDesktopExplorerWindow extends JFrame {
  /**
   * State provider for the embedded JavaFX web view debugger, exposing a JSON-serializable state
   * blob to the page so it can be restored on reload.
   */
  public class PSDesktopExplorerStateProvider implements JfxScriptStateProvider {
    /** Default constructor. Initializes the state to an empty boxed JSON object. */
    public PSDesktopExplorerStateProvider() {
      // no-op
    }

    private BoxedJsObject ourJsState = BoxedJson.of(); // start with empty state

    @Override
    public void setState(@NotNull BoxedJsObject state) {
      ourJsState = state;
    }

    @Override
    public @NotNull BoxedJsObject getState() {
      return ourJsState;
    }
  }

  /** State provider for the embedded web view, one per window instance. */
  protected PSDesktopExplorerStateProvider myStateProvider = new PSDesktopExplorerStateProvider();

  /** Logger for this class. */
  static Logger log = LogManager.getLogger(PSDesktopExplorerWindow.class);

  /** The Java bridge exposed to the page's JavaScript as {@code java}. */
  protected PSJavaBridge bridge = new PSJavaBridge(this);

  /** This window's target identifier used by the window manager. */
  protected String target;

  /** The parent window's target identifier, if any. */
  protected String parentTarget;

  /** The owning applet, set when the window is created from the content explorer applet. */
  protected PSContentExplorerApplet applet = null;

  /** The URL the web view should load; updated when the user navigates. */
  protected volatile String mi_actionurl;

  /** Default window style string used when no override is supplied. */
  protected String mi_style =
      "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=0,resizable=1,width=780,height=400";

  /** The current content selection passed through to the opened view. */
  protected PSSelection selection;

  /** The menu action that triggered this window's open. */
  protected PSMenuAction action;

  /** Parsed browser properties derived from {@link #mi_style}. */
  protected BrowserProps browserProps;

  /** The JavaFX web view, set when the window is opened. */
  protected volatile WebView webView;

  /** The JavaFX web engine backing the page. */
  protected WebEngine engine;

  /** {@code true} once the page has finished its initial load. */
  protected boolean windowLoaded = false;

  /** The JavaScript window object for the loaded page. */
  protected JSObject window = null;

  /** The window that opened this one, if any. */
  protected PSDesktopExplorerWindow opener = null;

  /** Whether the window has been closed. */
  private volatile boolean isClosed;

  /** When {@code true}, the Firebug Lite bookmarklet is loaded into the page on open. */
  protected static AtomicBoolean firebug = new AtomicBoolean(false);

  /** The shared window manager that tracks all open desktop explorer windows. */
  private static final PSWindowManager wgmr = PSWindowManager.getInstance();

  /** Default no-arg constructor; delegates to {@link JFrame#JFrame()}. */
  public PSDesktopExplorerWindow() {
    super();
  }

  /**
   * Constructs the window with the given title.
   *
   * @param string the title to display in the frame's title bar
   */
  public PSDesktopExplorerWindow(String string) {
    super(string);
  }

  /**
   * Opens this window for the supplied menu action context, sizing and positioning it relative to
   * its parent window (or centered on screen when no parent is registered), and registers a resize
   * listener with the window manager.
   *
   * @param parent the parent window's target identifier
   * @param mi_actionurl the URL the web view should load
   * @param mi_target this window's target identifier
   * @param mi_style optional window style string; if non-empty, replaces the default
   * @param selection the current selection passed through to the opened view
   * @param action the menu action that triggered the open
   * @return the resulting JFrame for chaining
   */
  public JFrame open(
      String parent,
      String mi_actionurl,
      String mi_target,
      String mi_style,
      PSSelection selection,
      PSMenuAction action) {
    this.parentTarget = parent;
    this.target = mi_target;
    this.mi_actionurl = mi_actionurl;
    if (StringUtils.isNotEmpty(mi_style)) this.mi_style = mi_style;
    this.selection = selection;
    this.action = action;
    this.browserProps = new BrowserProps(this.mi_style);

    Dimension windowSize =
        new Dimension(this.browserProps.getWidth(), this.browserProps.getHeight() + 20);
    setPreferredSize(windowSize);
    setSize(windowSize);

    PSDesktopExplorerWindow parentWin = getParentDceWindow();
    if (parentWin != null) {
      this.opener = parentWin;
      Point parentLocation = parentWin.getLocation();
      int wdwLeft = (int) parentLocation.getX() + 50;
      int wdwTop = (int) parentLocation.getY() + 50;
      setLocation(wdwLeft, wdwTop);

    } else {
      Rectangle bounds = PSDialog.getScreenBoundsAt(this.getLocation());
      Dimension size = getSize();
      setLocation(
          bounds.x + ((bounds.width - size.width) / 2),
          bounds.y + ((bounds.height - size.height) / 2));
    }

    setAutoRequestFocus(true);

    JFrame result = instanceOpen();
    windowLoaded = true;

    this.setFocusable(true);
    this.requestFocus();
    this.setAutoRequestFocus(true);
    this.addComponentListener(
        new ComponentAdapter() {
          public void componentResized(ComponentEvent evt) {
            Component c = (Component) evt.getSource();
            Rectangle r = PSDesktopExplorerWindow.this.getBounds();
            log.debug("Window " + target + " Resized to " + r.height + ", " + r.width);
            PSWindowManager.getInstance().windowResized(target, r.height, r.width);
          }
        });
    return result;
  }

  /**
   * Validates whether the window may be opened with the given context. Subclasses may refuse, for
   * example, when required fields are missing.
   *
   * @param mi_actionurl the URL the web view should load
   * @param mi_target the window target identifier
   * @param mi_style optional window style string
   * @param selection the current selection
   * @param action the menu action that triggered the open
   * @return {@code true} when the window is allowed to open
   */
  public abstract boolean validateOpen(
      String mi_actionurl,
      String mi_target,
      String mi_style,
      PSSelection selection,
      PSMenuAction action);

  /**
   * Performs the concrete browser-instance open for this window. Implemented by subclasses that
   * know whether to create a JavaFX WebView window or a Swing-based fallback.
   *
   * @return the constructed JFrame
   */
  public abstract JFrame instanceOpen();

  /** Closes this window through the {@link PSWindowManager} rather than directly. */
  public void managerClose() {
    wgmr.close(this.target);
  }

  /**
   * Returns this window's target identifier.
   *
   * @return this window's target identifier
   */
  public String getTarget() {
    return this.target;
  }

  /**
   * Sets this window's target identifier.
   *
   * @param target the target identifier to associate with this window
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Returns the URL the web view should load.
   *
   * @return the URL the web view should load
   */
  public String getUrl() {
    return this.mi_actionurl;
  }

  /**
   * Resolves the parent desktop explorer window from the window manager.
   *
   * @return the parent desktop explorer window, or {@code null} if none is registered
   */
  public PSDesktopExplorerWindow getParentDceWindow() {
    return wgmr.getWindow(this.parentTarget);
  }

  /**
   * Returns the JavaScript window object for the loaded page by executing {@code window} in the web
   * engine.
   *
   * @return the JavaScript window object for the loaded page
   */
  public JSObject getJSWindow() {
    return (JSObject) getEngine().executeScript("window");
  }

  /**
   * Returns the parent window's target identifier.
   *
   * @return the parent window's target identifier
   */
  public String getParentTarget() {
    return parentTarget;
  }

  /**
   * Returns the applet that owns this window.
   *
   * @return the applet that owns this window, or {@code null} if not yet associated
   */
  public PSContentExplorerApplet getApplet() {
    return applet;
  }

  /** Reloads the web view with the current parameters. No-op in the base implementation. */
  public void reload() {
    return;
  }

  /**
   * Reloads the web view with the supplied parameters. No-op in the base implementation.
   *
   * @param params the parameters to forward to the reload
   */
  public void reload(Map<String, String> params) {
    return;
  }

  /**
   * Asks the parent window to reload itself with the supplied parameters on the AWT event dispatch
   * thread.
   *
   * @param newParams the parameters to forward to the parent window's reload
   */
  public void reloadParent(HashMap<String, String> newParams) {
    PSDesktopExplorerWindow parentWindow =
        PSWindowManager.getInstance().getWindow(getParentTarget());
    if (parentWindow != null) {
      SwingUtilities.invokeLater(() -> parentWindow.reload(newParams));
    }
  }

  /** Closes this window through the window manager. */
  public void closeDceWindow() {
    wgmr.close(target);
  }

  /**
   * Opens a child window of this window via the {@link PSWindowManager}.
   *
   * @param mi_actionurl2 the URL the child window should load
   * @param mi_target the child window's target identifier
   * @param mi_style2 optional window style string
   * @param mi_selection the current selection
   * @param action2 the menu action that triggered the open
   * @return the opened child window
   */
  public PSDesktopExplorerWindow openChildWindow(
      String mi_actionurl2,
      String mi_target,
      String mi_style2,
      PSSelection mi_selection,
      PSMenuAction action2) {
    return wgmr.openWithParent(target, mi_actionurl2, mi_target, mi_style2, mi_selection, action2);
  }

  /**
   * Returns the JavaFX web engine backing this window.
   *
   * @return the JavaFX web engine backing this window
   */
  public WebEngine getEngine() {
    return engine;
  }

  /**
   * Sets the JavaFX web engine backing this window.
   *
   * @param engine the JavaFX web engine backing this window
   */
  public void setEngine(WebEngine engine) {
    this.engine = engine;
  }

  /** Installs the {@link PSJavaBridge} and the helper utility object on the page's window. */
  protected void setJavaBridge() {

    JSObject window = getJSWindow();

    if (window != null && !window.toString().equals("undefined")) {

      PSDesktopExplorerWindow parentDce = getParentDceWindow();
      if (parentDce != null) {
        JSObject opener = parentDce.getJSWindow();
        JSObject currentOpener = (JSObject) window.getMember("opener");
        if (currentOpener == null || opener != currentOpener) {
          window.setMember("opener", opener);
        }

      } else {
        log.debug("No parent for window " + this.target + "with url " + mi_actionurl);
      }

      Object currJava = (Object) window.getMember("java");
      if (currJava == null || currJava.toString().equals("undefined")) {
        window.setMember("java", this.bridge);
        if (applet != null) window.setMember("contentexplorer", applet);

        getEngine()
            .executeScript(
                "if(typeof perfInserted == 'undefined') {onerror = function(msg,url,line) {"
                    + " java.log(msg +', url: '+url+', line:'+line); };console.log ="
                    + " function(message){ java.log(message); };window.close = function() { return"
                    + " java.closeWindow();};window.open = function(url, name, specs, replace) {"
                    + " win = java.openWindow(url, name, specs , replace); java.log('window"
                    + " open='+win); return win;};percInserted=true;console.log('inserted perc js"
                    + " overrides')}");
      }
      if (firebug.get()) showFirebug();
    }

    Object currUtils = (Object) window.getMember("percUtils");
    if (currUtils == null || currUtils.toString().equals("undefined")) {
      window.setMember("percUtils", PSWebViewUtils.getInstance());
    }
  }

  /** Loads and injects the Firebug Lite bookmarklet into the page when Firebug mode is enabled. */
  public void showFirebug() {
    getEngine()
        .executeScript(
            "if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] &&"
                + " document.documentElement.namespaceURI;E = E ? document['createElement' +"
                + " 'NS'](E, 'script') :"
                + " document['createElement']('script');E['setAttribute']('id',"
                + " 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' +"
                + " 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite',"
                + " '4');(document['getElementsByTagName']('head')[0] ||"
                + " document['getElementsByTagName']('body')[0]).appendChild(E);E = new"
                + " Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
  }

  /**
   * Returns whether the window has been closed.
   *
   * @return {@code true} when the window has been closed
   */
  public boolean isClosed() {
    return isClosed;
  }

  /**
   * Sets whether the window is considered closed.
   *
   * @param isClosed sets whether the window is considered closed
   */
  public void setClosed(boolean isClosed) {
    this.isClosed = isClosed;
  }
}
