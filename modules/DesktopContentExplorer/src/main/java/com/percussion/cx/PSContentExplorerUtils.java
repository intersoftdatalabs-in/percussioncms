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

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSUserInfo;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for the desktop content explorer providing helpers for URL parsing, downloads and
 * i18n value logging.
 */
public class PSContentExplorerUtils {

  /** Default constructor. */
  public PSContentExplorerUtils() {
    // no-op
  }

  static Logger log = LogManager.getLogger(PSContentExplorerUtils.class);

  /**
   * Parses the supplied URL and returns a map of its query string parameters.
   *
   * @param url the URL to parse, assumed not <code>null</code>.
   * @return a map of query parameter name to value, never <code>null</code>, may be empty.
   */
  public static Map<String, String> getQueryMap(String url) {
    Map<String, String> map = new HashMap<>();

    int idx = url.indexOf("?");
    String query = "";
    if (idx >= 0) query = url.substring(idx + 1);
    else return map;

    String[] params = query.split("&");
    for (String param : params) {
      String name = param.split("=")[0];
      String value = param.split("=")[1];
      map.put(name, value);
    }
    return map;
  }

  /**
   * Downloads the file at the supplied URL into the supplied target directory, replacing any
   * existing file with the same name.
   *
   * @param sourceUrl the URL of the file to download, may not be <code>null</code>.
   * @param targetDirectory the local directory to receive the file, may not be <code>null</code>.
   * @return the path of the downloaded file on the local file system, never <code>null</code>.
   * @throws MalformedURLException if the supplied URL is malformed.
   * @throws IOException if an I/O error occurs during the download.
   */
  public static Path download(String sourceUrl, String targetDirectory)
      throws MalformedURLException, IOException {
    URL url = new URL(sourceUrl);

    String fileName = url.getFile();

    Path targetPath = new File(targetDirectory + fileName).toPath();

    Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

    return targetPath;
  }

  /**
   * Splts the supplied URL string into the url string without parameters and a map of parameters.
   * This will be useful when the url string is too large and hence want to post the data to the
   * server.
   *
   * @param actionUrl the url to split. This will have a syntax:
   *     <p>http://&lt;server&gt;:port/Rhythmyx/appName/resource.html?param1=
   *     value1&amp;param2=value2....
   *     <p>The result would be such that the returned string is
   *     <p>http://&lt;server&gt;:port/Rhythmyx/appName/resource.html and the params object which is
   *     assumed to empty but not <code>null</code> will containg the paramN-valueN pairs.
   * @param params An empty Map object to contain the param-value pairs after splitting. Must not be
   *     <code>null</code>. The existing map values will not be deleted, if it is not empty.
   * @return the url part as explained above. Never <code>null</code> or empty.
   */
  public static String splitUrl(String actionUrl, Map<? super String, ? super String> params) {
    if (StringUtils.isEmpty(actionUrl)) {
      throw new IllegalArgumentException("actionUrl must not be null or empty");
    }

    if (params == null) {
      throw new IllegalArgumentException("params must not be null");
    }

    try {
      // Post should the url decode form. Note this was encoded earlier.
      actionUrl = URLDecoder.decode(actionUrl, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // This should never happen
      throw new RuntimeException(e);
    }
    int index = actionUrl.indexOf('?');
    if (index == -1) return actionUrl;
    String url = actionUrl.substring(0, index);
    if (actionUrl.length() <= index + 1) return url;

    String paramString = actionUrl.substring(index + 1);
    StringTokenizer tokenizer = new StringTokenizer(paramString, "&");
    while (tokenizer.hasMoreElements()) {
      String temp = tokenizer.nextToken();
      index = temp.indexOf('=');
      if (index == -1) {
        params.put(temp, "");
        continue;
      }
      String param = temp.substring(0, index);
      String value = "";
      if (temp.length() > index + 1) value = temp.substring(index + 1);
      params.put(param, value);
    }
    return url;
  }

  /**
   * Logs user information (session id, user name and locale) for the supplied applet at debug
   * level. Errors are caught and logged at error level.
   *
   * @param applet the applet whose user info should be logged, may not be <code>null</code>.
   */
  public static void outputUserInfo(PSContentExplorerApplet applet) {
    try {
      log.debug("checking userinfo");
      PSUserInfo ms_userInfo =
          new PSUserInfo(applet.getHttpConnection(), applet.getRhythmyxCodeBase());
      log.debug("UserInfo sessionId = " + ms_userInfo.getSessionId());
      log.debug("UserInfo user = " + ms_userInfo.getUserName());
      log.debug("UserInfo locale = " + ms_userInfo.getLocale());

    } catch (PSCmsException e) {
      e.printStackTrace();

      log.error("Error getting userinfo", e);
    }
  }

  static void OutputJaxpImplementationInfo() {
    if (PSContentExplorerApplet.log.isDebugEnabled()) {
      try {
        var secureOpts = PSXmlSecurityOptions.secure();
        PSContentExplorerApplet.log.debug(
            PSContentExplorerApplet.getJaxpImplementationInfo(
                "DocumentBuilderFactory",
                PSSecureXMLUtils.getSecuredDocumentBuilderFactory(secureOpts).getClass()));
        PSContentExplorerApplet.log.debug(
            PSContentExplorerApplet.getJaxpImplementationInfo(
                "XPathFactory", XPathFactory.newInstance().getClass()));
        PSContentExplorerApplet.log.debug(
            PSContentExplorerApplet.getJaxpImplementationInfo(
                "TransformerFactory", PSSecureXMLUtils.getSecuredTransformerFactory().getClass()));
        PSContentExplorerApplet.log.debug(
            PSContentExplorerApplet.getJaxpImplementationInfo(
                "SAXParserFactory",
                PSSecureXMLUtils.getSecuredSaxParserFactory(secureOpts).getClass()));
      } catch (Exception e) {
        PSContentExplorerApplet.log.error("Couldn't print JAXP property", e);
      }
    }
  }
}
