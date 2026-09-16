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

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

/**
 * Desktop-hosted replacement for the JDK {@code java.applet.AppletContext} type removed in JDK 24+.
 * Used by {@link PSJApplet} when Content Explorer runs as an application rather than a browser
 * applet.
 */
public interface PSAppletContext {

  /**
   * Returns an image from the given URL.
   *
   * @param url the image location, may be {@code null}
   * @return the image, or {@code null} if this context does not resolve images
   */
  Image getImage(URL url);

  /**
   * Requests that a document be shown. Desktop hosts may no-op.
   *
   * @param url the document to show, may be {@code null}
   */
  void showDocument(URL url);

  /**
   * Requests that a document be shown in the named target. Desktop hosts may no-op.
   *
   * @param url the document to show, may be {@code null}
   * @param target the target frame name, may be {@code null}
   */
  void showDocument(URL url, String target);

  /**
   * Shows a status message. Desktop hosts may no-op.
   *
   * @param status the status text, may be {@code null}
   */
  void showStatus(String status);

  /**
   * Returns a stream previously stored under {@code key}.
   *
   * @param key the stream key, may be {@code null}
   * @return the stream, or {@code null} if none is stored
   */
  InputStream getStream(String key);

  /**
   * Stores a stream under {@code key}.
   *
   * @param key the stream key, may be {@code null}
   * @param stream the stream to store, may be {@code null} to remove
   * @throws IOException if the host cannot store the stream
   */
  void setStream(String key, InputStream stream) throws IOException;

  /**
   * Keys of streams stored in this context.
   *
   * @return the keys, or {@code null} if the host does not expose them
   */
  Iterator<String> getStreamKeys();
}
