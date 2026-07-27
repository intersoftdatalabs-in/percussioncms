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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class that provides shared utilities for the Content Explorer, including resource loading,
 * default parameter initialization, and image icon resolution.
 */
public class PSContentExplorerHelper {

  /** Logger for this class. */
  private static final Logger log = LogManager.getLogger(PSContentExplorerHelper.class);

  /** List of recognized HTML file extensions. */
  public static List<String> htmExt = Arrays.asList("html", "htm");

  /** List of recognized Excel file extensions. */
  public static List<String> xlsExt = Arrays.asList("xls");

  private static ResourceBundle sm_res = null;

  /** Constructs a new helper instance. */
  public PSContentExplorerHelper() {}

  /**
   * Gets the code base URL for the Content Explorer applet.
   *
   * @return the code base URL, or <code>null</code> if it could not be constructed.
   */
  public URL getCodeBase() {
    URL url = null;
    try {
      String host = "localhost";
      // String proto = documentBase.getProtocol();
      int port = 9992; // Integer.parseInt(
      // getResources().getString("port"));
      url = new URL("http", host, port, "/Rhythmyx/sys_resources/AppletJars/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return url;
  }

  /**
   * Gets the shared resource bundle for the Content Explorer UI strings.
   *
   * @return the resource bundle, may be <code>null</code> if the bundle is not found.
   */
  public static ResourceBundle getResources() {
    try {
      if (sm_res == null)
        sm_res =
            ResourceBundle.getBundle(
                "com.percussion.cx.PSContentExplorerResources", Locale.getDefault());
    } catch (MissingResourceException e) {
      log.error(e);
    }

    return sm_res;
  }

  /**
   * Gets an image icon for the given class using the main icon path from the resource bundle.
   *
   * @param clazz the class whose resource path is used to locate the icon, assumed not <code>null
   *     </code>.
   * @return the loaded image icon, or <code>null</code> if it cannot be loaded.
   */
  public static ImageIcon getImageIcon(Class clazz) {

    ImageIcon icon;
    try {
      icon = new ImageIcon(clazz.getResource(getResources().getString("gif_main")));
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      icon = null;
    }
    return icon;
  }

  /**
   * Initializes the default applet parameters for the Content Explorer view.
   *
   * @return a map of default parameter names to values, never <code>null</code>.
   */
  public static Map<String, String> initializeDefaultParameters() {
    Map<String, String> parameters = new HashMap<String, String>();

    // this var is important because we will check to see if we are invoking
    // this as a swing app
    parameters.put("SWING", "true");
    parameters.put("IMPACT", "false");

    // set defaults
    parameters.put("CODE", "com.percussion.cx.PSContentExplorerApplet.class");
    parameters.put("VIEW", "CX");
    parameters.put("RESTRICTSEARCHFIELDSTOUSERCOMMUNITY", "");
    parameters.put("CacheSearchableFieldsInApplet", "");
    parameters.put("isManagedNavUsed", "yes");
    parameters.put("CODEBASE", "../dce");
    parameters.put("OPTIONS_URL", "../sys_cxSupport/options.xml");
    parameters.put("MENU_URL", "../sys_cx/ContentExplorerMenu.html");
    parameters.put("NAV_URL", "../sys_cx/ContentExplorer.html");
    parameters.put("CACHE_ARCHIVE", "ContentExplorer*.jar");
    parameters.put("CACHE_OPTION", "Plugin");
    parameters.put("ARCHIVE", "ContentExplorer*.jar");
    parameters.put("helpset_file", "../Docs/Business_Users/Content_Explorer_Help.hs");
    parameters.put("sys_cxinternalpath", "");
    parameters.put("sys_cxdisplaypath", "");
    parameters.put("TYPE", "application/x-java-applet;version=1.8.0_12");
    parameters.put("MAYSCRIPT", "true");
    parameters.put("NAME", "ContentExplorerApplet");
    parameters.put("ID", "ContentExplorerApplet");
    parameters.put("WIDTH", "960");
    parameters.put("HEIGHT", "700");
    parameters.put("LABEL", "Desktop Content Explorer");

    parameters.put("pssessionid", "");

    parameters.put("securitySOAPEndpoint", "/Rhythmyx/webservices/securitySOAP");

    return parameters;
  }

  /**
   * Initializes the parameters specific to the Dependency Tree view.
   *
   * @param parameters the existing parameter map to update, assumed not <code>null</code>.
   * @return the updated parameter map, never <code>null</code>.
   */
  public static Map<String, String> initializeDTParameters(Map<String, String> parameters) {

    parameters.put("VIEW", "DT");
    parameters.put("CODE", "com.percussion.cx.PSContentExplorerApplet.class");
    parameters.put("OPTIONS_URL", "../sys_cxSupport/options.xml");
    parameters.put("MENU_URL", "../sys_cxDependencyTree/DependencyTreeMenu.html");
    parameters.put("NAV_URL", "../sys_cx/ContentExplorer.html");
    parameters.put("TITLE", "Impact Analysis");
    parameters.put("sys_cxinternalpath", "");
    parameters.put("sys_cxdisplaypath", "");
    parameters.put("NAME", "ContentExplorerApplet");
    parameters.put("ID", "ContentExplorerApplet");
    parameters.put("POPUP_TITLE", "Rhythmyx- Impact Analysis");
    return parameters;
  }

  /**
   * Initializes the parameters specific to the Item Assembly view, derived from the supplied base
   * parameter map and the action URL.
   *
   * @param parameters the existing parameter map to update, assumed not <code>null</code>.
   * @param actionUrl the action URL whose query string, if any, is appended to the navigation URL,
   *     may be <code>null</code> or empty.
   * @return the updated parameter map, never <code>null</code>.
   */
  public static Map<String, String> initializeIAParameters(
      Map<String, String> parameters, String actionUrl) {
    String queryParams = "";
    String nav_url = "../sys_cxItemAssembly/itemassemblydata.html";
    int i = actionUrl.indexOf("?");
    if (i >= 0) {
      queryParams = actionUrl.substring(i + 1);
      nav_url = nav_url + "?" + queryParams;
    }
    parameters.put("VIEW", "IA");
    parameters.put("CODE", "com.percussion.cx.PSContentExplorerApplet.class");
    parameters.put("OPTIONS_URL", "../sys_cxSupport/options.xml");
    parameters.put("MENU_URL", "../sys_cxItemAssembly/ItemAssemblyMenu.html");
    parameters.put("NAV_URL", nav_url);

    parameters.put("sys_cxinternalpath", "");
    parameters.put("sys_cxdisplaypath", "");
    parameters.put("NAME", "ContentExplorerApplet");
    parameters.put("ID", "ContentExplorerApplet");
    parameters.put("HEIGHT", "320");
    parameters.put("WIDTH", "782");
    parameters.put("POPUP_TITLE", "Active Assembly for Documents");

    return parameters;
  }
}
