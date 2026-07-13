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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

/**
 * Utility class providing helpers for the embedded JavaFX web view, including a singleton accessor
 * and a method to fetch the textual contents of a URL.
 */
public class PSWebViewUtils {

  private static PSWebViewUtils INSTANCE;

  /**
   * Gets the shared singleton instance.
   *
   * @return the shared instance, never <code>null</code>.
   */
  public static synchronized PSWebViewUtils getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PSWebViewUtils();
    }
    return INSTANCE;
  }

  private PSWebViewUtils() {}

  /**
   * Utility method to retrieve the contents of a URL.
   *
   * @param protocol the URL protocol (e.g. "http" or "https"), may not be <code>null</code>.
   * @param host the host portion of the URL, may not be <code>null</code>.
   * @param port the port portion of the URL as a string, may not be <code>null</code>.
   * @param uri the path portion of the URL, may not be <code>null</code>.
   * @return the textual contents of the URL, never <code>null</code>.
   */
  public String getText(String protocol, String host, String port, String uri) {
    try {
      if (!uri.startsWith("http")) {
        if (!uri.startsWith("/")) uri = "/" + uri;
        uri = protocol + "//" + host + ":" + port + uri;
      }
      URL url = new URL(uri);
      return IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
