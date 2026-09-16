/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.JPanel;

/**
 * Swing panel that preserves the {@code javax.swing.JApplet} surface Content Explorer still uses
 * (parameters, codebase, content pane, lifecycle) without depending on the JDK applet API removed
 * in JDK 24+.
 *
 * <p>Unlike {@code java.applet.Applet}, construction does not throw {@code HeadlessException}, so
 * unit tests can instantiate subclasses on display-less runners.
 */
public class PSJApplet extends JPanel {

  private static final long serialVersionUID = 1L;

  /** Host stub supplying parameters and codebase; {@code null} until {@link #setStub} is called. */
  private transient PSAppletStub stub;

  /** Creates an empty applet panel with a border layout, matching historical {@code JApplet}. */
  @SuppressWarnings("this-escape")
  public PSJApplet() {
    setLayout(new BorderLayout());
  }

  /**
   * Installs the host stub used for parameters and codebase.
   *
   * @param stub the host stub, may be {@code null} to clear
   */
  public void setStub(PSAppletStub stub) {
    this.stub = stub;
  }

  /**
   * Returns the host stub, or {@code null} if none has been set.
   *
   * @return the stub, may be {@code null}
   */
  public PSAppletStub getStub() {
    return stub;
  }

  /**
   * Named parameter from the host stub.
   *
   * @param name the parameter name
   * @return the value, or {@code null} if no stub is set or the name is unknown
   */
  public String getParameter(String name) {
    return stub == null ? null : stub.getParameter(name);
  }

  /**
   * Code base from the host stub.
   *
   * @return the code base, or {@code null} if no stub is set
   */
  public URL getCodeBase() {
    return stub == null ? null : stub.getCodeBase();
  }

  /**
   * Document base from the host stub.
   *
   * @return the document base, or {@code null} if no stub is set
   */
  public URL getDocumentBase() {
    return stub == null ? null : stub.getDocumentBase();
  }

  /**
   * Applet context from the host stub.
   *
   * @return the context, or {@code null} if no stub is set
   */
  public PSAppletContext getAppletContext() {
    return stub == null ? null : stub.getAppletContext();
  }

  /**
   * Whether the host reports this applet as active.
   *
   * @return {@code true} if a stub is set and reports active
   */
  public boolean isActive() {
    return stub != null && stub.isActive();
  }

  /**
   * Content pane for child components. This panel <em>is</em> the content pane.
   *
   * @return this panel, never {@code null}
   */
  public Container getContentPane() {
    return this;
  }

  /**
   * Loads an image via AWT toolkit. Safe to call without a stub.
   *
   * @param url the image location, may be {@code null}
   * @return the image, or {@code null} if {@code url} is {@code null}
   */
  public Image getImage(URL url) {
    if (url == null) {
      return null;
    }
    PSAppletContext context = getAppletContext();
    if (context != null) {
      Image fromContext = context.getImage(url);
      if (fromContext != null) {
        return fromContext;
      }
    }
    return Toolkit.getDefaultToolkit().getImage(url);
  }

  /** Called by the host after construction. Default is a no-op; subclasses override. */
  public void init() {
    // default no-op
  }

  /** Called by the host to start the applet. Default is a no-op; subclasses override. */
  public void start() {
    // default no-op
  }

  /** Called by the host to stop the applet. Default is a no-op; subclasses override. */
  public void stop() {
    // default no-op
  }

  /** Called by the host before disposal. Default is a no-op; subclasses override. */
  public void destroy() {
    // default no-op
  }
}
