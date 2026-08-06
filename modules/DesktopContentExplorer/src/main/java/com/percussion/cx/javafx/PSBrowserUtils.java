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

import java.awt.Desktop;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class for opening URLs in the system's default web browser. Provides methods to launch
 * browser pages from both URI and URL objects.
 *
 * @since 8.0.0
 */
public class PSBrowserUtils {

  /** Default constructor. This class is not intended to be instantiated; all methods are static. */
  public PSBrowserUtils() {}

  /**
   * Opens the specified URI in the system's default web browser.
   *
   * @param uri the URI to open; may be {@code null}
   */
  public static void openWebpage(URI uri) {
    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
      try {
        desktop.browse(uri);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Opens the specified URL in the system's default web browser.
   *
   * @param url the URL to open; may be {@code null}
   */
  public static void openWebpage(URL url) {
    try {
      openWebpage(url.toURI());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * Test method to verify browser opening functionality.
   *
   * @param args command line arguments (not used)
   * @throws MalformedURLException if the test URL is invalid
   */
  public static void main(String[] args) throws MalformedURLException {
    URL url = new URL("http://www.google.com");
    String pdf =
        "http://localhost:9992/Rhythmyx/assembler/render?sys_revision=2&sys_authtype=0&sys_variantid=533&sys_context=0&sys_folderid=526&sys_siteid=303&sys_contentid=698&sys_command=edit";
    url = new URL(pdf);
    openWebpage(url);
  }

  /**
   * Converts a string representation to a URL string.
   *
   * @param str the string to convert
   * @return the external form of the URL, or {@code null} if the string is malformed
   */
  public static String toStringURL(String str) {
    try {
      return new URL(str).toExternalForm();
    } catch (MalformedURLException exception) {
      return null;
    }
  }

  /**
   * Converts a string representation to a URL object.
   *
   * @param str the string to convert
   * @return the URL object, or {@code null} if the string is malformed
   */
  public static URL toURL(String str) {
    try {
      return new URL(str);
    } catch (MalformedURLException exception) {
      return null;
    }
  }
}
