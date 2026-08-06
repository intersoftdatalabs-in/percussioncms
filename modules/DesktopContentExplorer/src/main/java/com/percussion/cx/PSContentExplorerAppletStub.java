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

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A minimal implementation of both {@link AppletStub} and {@link AppletContext} used to host the
 * {@link PSContentExplorerApplet} when it is not loaded by a browser, for example when running as a
 * desktop application. It supplies parameter storage and stub behavior required by the applet's
 * lifecycle.
 */
public class PSContentExplorerAppletStub implements AppletStub, AppletContext {

  /**
   * Constructs an empty stub with no parameters. Parameters are supplied later through {@link
   * #setParemeter(String, String)} or {@link #setParameters(Map)}.
   */
  public PSContentExplorerAppletStub() {
    super();
  }

  PSContentExplorerHelper helper = new PSContentExplorerHelper();

  /** hashmap containing all parameters needed for the applet */
  Map<String, String> parameters = new HashMap<String, String>();

  /** Minimal implementation for AppletStub. */
  public boolean isActive() {
    return false;
  }

  public URL getDocumentBase() {
    return null;
  }

  public URL getCodeBase() {
    return helper.getCodeBase();
  }

  public String getParameter(String key) {
    return parameters.get(key);
  }

  /**
   * Stores the supplied parameter value, replacing any existing value for the same key.
   *
   * @param parameter the parameter name, must not be <code>null</code>.
   * @param value the parameter value, may be <code>null</code>.
   */
  public void setParemeter(String parameter, String value) {
    this.parameters.put(parameter, value);
  }

  /**
   * Gets all parameters currently stored in this stub.
   *
   * @return the parameter map, never <code>null</code>.
   */
  public Map<String, String> getParameters() {
    return parameters;
  }

  /**
   * Replaces the entire set of parameters held by this stub.
   *
   * @param map the new parameter map, must not be <code>null</code>.
   */
  public void setParameters(Map<String, String> map) {
    this.parameters = map;
  }

  public AppletContext getAppletContext() {
    return this;
  }

  /** Minimal implementation for AppletContext. */
  public AudioClip getAudioClip(URL url) {
    return null;
  }

  public Image getImage(URL url) {
    return null;
  }

  public Applet getApplet(String name) {
    return null;
  }

  public Enumeration getApplets() {
    return null;
  }

  public void showDocument(URL url) {}

  public void showDocument(URL url, String taget) {}

  public void showStatus(String status) {}

  /*
   * (non-Javadoc)
   *
   * @see java.applet.AppletContext#getStream(java.lang.String)
   */
  public InputStream getStream(String key) {
    // TODO - Implement for JDK 1.4
    throw new UnsupportedOperationException("This method is not yet implemented");
  }

  @Override
  public void setStream(String key, InputStream stream) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public Iterator<String> getStreamKeys() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void appletResize(int width, int height) {
    // TODO Auto-generated method stub

  }
}
