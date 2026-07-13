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
import com.percussion.cx.PSContentExplorerAppletStub;
import com.percussion.cx.PSContentExplorerApplication;
import com.percussion.cx.PSContentExplorerConstants;
import com.percussion.cx.PSContentExplorerHelper;
import com.percussion.cx.PSContentExplorerUtils;
import com.percussion.cx.PSSelection;
import com.percussion.cx.objectstore.PSMenuAction;
import com.percussion.cx.objectstore.PSNode;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Popup frame for launching content explorer applets in separate windows. Supports dependency tree
 * and item assembly views.
 *
 * @since 8.0.0
 */
public class PSPopupAppletFrame extends PSDesktopExplorerWindow {
  static Logger log = LogManager.getLogger(PSPopupAppletFrame.class);

  /** The applet stub used to provide parameter values to the embedded applet. */
  PSContentExplorerAppletStub stub = new PSContentExplorerAppletStub();

  /** Whether the embedded web view has completed loading. */
  private boolean windowLoaded;

  /** The active view ("DT" or "IA"), may be <code>null</code>. */
  private String view = null;

  /** Creates a new popup applet frame with default settings. */
  public PSPopupAppletFrame() {
    super();

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            setVisible(false);
            if (PSPopupAppletFrame.this.applet != null) {
              PSPopupAppletFrame.this.applet.stop();
              PSPopupAppletFrame.this.applet.destroy();

              PSWindowManager.getInstance().close(PSPopupAppletFrame.this.target);
            }
          }
        });
  }

  /**
   * Determines the view type based on the action URL.
   *
   * @param mi_actionurl the action URL to inspect
   * @return "DT" for dependency tree, "IA" for item assembly, or null if unknown
   */
  private static String getView(String mi_actionurl) {
    String view;
    if (mi_actionurl.contains("/Rhythmyx/sys_cxDependencyTree/dependencytree.html")) view = "DT";
    else if (mi_actionurl.contains("/sys_cxItemAssembly/itemassembly.html")) view = "IA";
    else return null;
    return view;
  }

  /**
   * Validates whether this frame can open the given URL.
   *
   * @param mi_actionurl the action URL
   * @param mi_target the target name
   * @param mi_style the window style specifications
   * @param selection the current selection
   * @param actiom the menu action
   * @return true if the URL matches a supported view type
   */
  @Override
  public boolean validateOpen(
      String mi_actionurl,
      String mi_target,
      String mi_style,
      PSSelection selection,
      PSMenuAction actiom) {
    return getView(mi_actionurl) != null;
  }

  /**
   * Creates and returns the popup frame instance.
   *
   * @return the configured popup frame
   */
  @Override
  public JFrame instanceOpen() {

    this.view = getView(this.mi_actionurl);

    if (this.applet != null) {

      this.applet.stop();
      this.applet.destroy();
      this.remove(this.applet);
    }

    this.applet = new PSContentExplorerApplet(true);

    Platform.runLater(
        () -> {
          this.webView = new WebView();
          this.engine = this.webView.getEngine();
          this.engine.loadContent("<html></html>");
          setJavaBridge();
        });

    SwingUtilities.invokeLater(
        () -> {
          Map<String, String> params = buildSessionParameterMapForInnerApplet();

          setTitle(params.get(PSContentExplorerConstants.POPUP_TITLE));

          this.stub = new PSContentExplorerAppletStub();
          this.stub.setParameters(params);
          this.applet.setStub(this.stub);
          this.applet.setIsApplication(true);

          //  Need to create a webView to get javascript object for opener.

          this.browserProps = new BrowserProps(this.mi_style);

          this.add(this.applet, BorderLayout.NORTH);

          PSContentExplorerApplet baseapplet = PSContentExplorerApplication.getApplet();
          this.applet.init();
          this.applet.setupApplet(baseapplet.getUserInfo());
          this.applet.start();

          setVisible(true);
        });
    return this;
  }

  /**
   * Builds the session parameter map for the inner applet.
   *
   * @return map of parameter names to values
   */
  private Map<String, String> buildSessionParameterMapForInnerApplet() {

    Map<String, String> params = PSContentExplorerHelper.initializeDefaultParameters();
    if (this.view.equals("DT"))
      params.putAll(PSContentExplorerHelper.initializeDTParameters(params));
    else if (this.view.equals("IA"))
      params.putAll(PSContentExplorerHelper.initializeIAParameters(params, this.mi_actionurl));

    PSContentExplorerApplet baseapplet = PSContentExplorerApplication.getApplet();

    String sessionId = baseapplet.getParameter("pssessionid");
    String host = baseapplet.getParameter("serverName");
    String proto = baseapplet.getParameter("protocol");
    String port = baseapplet.getParameter("port");

    params.put("pssessionid", sessionId);
    params.put("serverName", host);
    params.put("protocol", proto);
    params.put("port", port);

    Map<String, String> queryParams = PSContentExplorerUtils.getQueryMap(this.mi_actionurl);
    // add all query params to the map
    queryParams.forEach(params::put);

    params.put(PSContentExplorerConstants.PARAM_CONTENTID, queryParams.get("sys_contentid"));
    params.put(PSContentExplorerConstants.PARAM_REVISIONID, queryParams.get("sys_revision"));
    params.put("LABEL", this.action.getLabel());

    if (this.selection.getNodeList() != null && this.selection.getNodeList().hasNext()) {
      PSNode node = (PSNode) this.selection.getNodeList().next();
      params.put(PSContentExplorerConstants.PARAM_ITEM_TITLE, node.getName());
    }

    return params;
  }

  /**
   * Reloads the popup with optional new parameters.
   *
   * @param parameters optional new parameters to merge
   */
  @Override
  public void reload(Map<String, String> parameters) {
    Map<String, String> params = buildSessionParameterMapForInnerApplet();
    // Don't use specific item unless when resetting unless specified
    params.remove(PSContentExplorerConstants.PARAM_CONTENTID);
    params.remove(PSContentExplorerConstants.PARAM_REVISIONID);

    if (parameters != null) params.putAll(parameters);
    reload();
  }
}
