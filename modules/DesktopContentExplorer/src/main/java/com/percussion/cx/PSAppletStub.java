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

import java.net.URL;

/**
 * Desktop-hosted replacement for the JDK {@code java.applet.AppletStub} type removed in JDK 24+.
 * Supplies parameters and codebase to {@link PSJApplet} when Content Explorer is not loaded by a
 * browser.
 */
public interface PSAppletStub {

  /**
   * Whether the hosted applet is considered active.
   *
   * @return {@code true} if active
   */
  boolean isActive();

  /**
   * Document base URL, typically the hosting page.
   *
   * @return the document base, or {@code null} if unknown
   */
  URL getDocumentBase();

  /**
   * Code base URL, typically the directory containing the application resources.
   *
   * @return the code base, or {@code null} if unknown
   */
  URL getCodeBase();

  /**
   * Returns a named parameter supplied by the host.
   *
   * @param name the parameter name, may be {@code null}
   * @return the value, or {@code null} if not set
   */
  String getParameter(String name);

  /**
   * Context used for document/status/image operations.
   *
   * @return the context, never {@code null} for a fully initialized host
   */
  PSAppletContext getAppletContext();

  /**
   * Called when the applet requests a resize. Desktop hosts may no-op.
   *
   * @param width requested width
   * @param height requested height
   */
  void appletResize(int width, int height);
}
