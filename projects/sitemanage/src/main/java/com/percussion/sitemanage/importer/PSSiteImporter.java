// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.sitemanage.importer;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.server.IPSHttpErrors;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.dao.IPSImportLogDao;
import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Date;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("siteImporter")
@Lazy
public class PSSiteImporter {

  public static final String REDIRECTED_FROM_URL =
      "Redirect the original URL from  '{originalUrl}' to '{newUrl}'";

  private static final Logger log = LogManager.getLogger(PSSiteImporter.class);
  private static final String SITE_IMPORTER = "Site Importer";
  private static final String HTML = "html";
  private static final String HEAD = "head";
  private static final String BODY = "body";

  private static IPSSystemProperties systemProperties = null;

  /**
   * Gets the page in the given URL, and parses its content into a PSPageContent object.
   *
   * @param siteImportCtx must not be <code>null</code> and the site url must not be <code>null
   *     </code> either.
   * @return PSPageContent object with all fields filled in from the page found in the provided URL.
   */
  public static PSPageContent getPageContentFromSite(PSSiteImportCtx siteImportCtx)
      throws IOException {
    notNull(siteImportCtx);
    notNull(siteImportCtx.getSiteUrl().orElse(null));
    notNull(siteImportCtx.getUserAgent().orElse(null));

    URLConnectionProperties properties = null;

    try {
      properties = overrideConnectionProperties();

      var con =
          buildJsoupConnection(
              siteImportCtx.getSiteUrl().orElse(""),
              true,
              true,
              siteImportCtx.getUserAgent().orElse(""));
      var doc = con.get();

      var pageContent = createPageContent(doc, siteImportCtx.getLogger());
      pageContent.setPath(siteImportCtx.getSiteUrl().orElse(""));
      return pageContent;
    } catch (IOException e) {
      throw e;
    } finally {
      restoreConnectionProperties(properties);
    }
  }

  /** Given a JSoup document, extracts the content and creates a PSPageContent object. */
  public static PSPageContent createPageContent(Document doc, IPSSiteImportLogger logger) {
    var pageContent = new PSPageContent();
    var docHead = doc.head();
    var titleElems = docHead.select("title");
    var title = "";
    if (!titleElems.isEmpty()) {
      title = titleElems.get(0).text();
    }

    // Extract all style, script, and link elements
    var addHeadElems = new Elements();
    addHeadElems.addAll(docHead.select("style"));
    addHeadElems.addAll(docHead.select("link"));
    addHeadElems.addAll(docHead.select("script"));

    var additionalHeadContent = new StringBuilder();
    for (var element : addHeadElems) {
      additionalHeadContent.append(element.outerHtml());
    }

    var bodyContent = getBodyContent(doc, logger);

    pageContent.setTitle(title);
    pageContent.setHeadContent(additionalHeadContent.toString());
    pageContent.setBodyContent(bodyContent);
    pageContent.setSourceDocument(doc);
    return pageContent;
  }

  /**
   * Retrieves the body content of the document. If the document has no body, tries to build one
   * using content outside header but inside html element.
   */
  private static String getBodyContent(Document doc, IPSSiteImportLogger logger) {
    var body = doc.body();

    if (body != null) {
      return body.html();
    }

    logger.appendLogMessage(
        PSLogEntryType.ERROR,
        SITE_IMPORTER,
        "Cannot find <body> element, the imported template and page will not look the same as the"
            + " original page.");

    return buildBodyFromDocument(doc).html();
  }

  /**
   * Builds the body element and puts inside it the tags that are inside the html element, and
   * outside the header.
   */
  private static Element buildBodyFromDocument(Document doc) {
    addBodyToDocument(doc);

    var body = doc.body();
    var html = doc.getElementsByTag(HTML).get(0);

    var htmlChildren = html.children();
    for (var element : htmlChildren) {
      if (equalsIgnoreCase(element.nodeName(), HEAD)
          || equalsIgnoreCase(element.nodeName(), BODY)) {
        continue;
      }
      body.appendChild(element);
    }
    return body;
  }

  /** Adds the body element to the document, as a child of the html element. */
  private static void addBodyToDocument(Document doc) {
    // jsoup Document has no normalize/normalise method; skip this step

    // check just in case the document could not be normalised
    if (doc.body() == null) {
      var html = doc.getElementsByTag(HTML).get(0);
      html.appendElement("body");
    }
  }

  /**
   * Gets the redirected url for the given site url. Follows redirections and returns the final url.
   */
  public static String getRedirectedUrl(
      String siteUrl, IPSSiteImportLogger logger, String userAgent) throws IOException {
    notNull(siteUrl);
    notNull(logger);
    notNull(userAgent);

    URLConnectionProperties properties = null;

    try {
      properties = overrideConnectionProperties();

      var conn = buildJsoupConnection(siteUrl, true, false, userAgent);
      conn.get();
      var response = conn.response();

      if (response.statusCode() != IPSHttpErrors.HTTP_MOVED_TEMPORARILY
          && response.statusCode() != IPSHttpErrors.HTTP_MOVED_PERMANENTLY) {
        return siteUrl;
      }

      var redirectedConn = buildJsoupConnection(siteUrl, true, true, userAgent);
      redirectedConn.get();
      var newUrl = redirectedConn.response().url();

      logger.appendLogMessage(
          PSLogEntryType.STATUS,
          SITE_IMPORTER,
          REDIRECTED_FROM_URL
              .replace("{originalUrl}", siteUrl)
              .replace("{newUrl}", newUrl.toString()));

      return newUrl.toString();
    } catch (IOException e) {
      throw e;
    } finally {
      restoreConnectionProperties(properties);
    }
  }

  /** Generates a JSoup Connection using the given parameters. */
  public static Connection buildJsoupConnection(
      String url, boolean ignoreContentType, boolean followRedirects, String userAgent) {
    var conn = Jsoup.connect(url);
    conn.ignoreContentType(ignoreContentType);
    conn.followRedirects(followRedirects);
    conn.userAgent(userAgent);
    int timeOut = getImportTimeout();
    if (timeOut > 0) conn.timeout(timeOut);

    return conn;
  }

  /** Get the timeout to use for importing pages, files, and assets. */
  public static int getImportTimeout() {
    int timeOut = 30;
    if (systemProperties != null) {
      timeOut =
          NumberUtils.toInt(
              systemProperties.getProperty(IPSSystemProperties.IMPORT_TIME_OUT), timeOut);
    }
    return (timeOut * 1000);
  }

  @Autowired
  public synchronized void setSystemProperties(IPSSystemProperties systemProps) {
    systemProperties = systemProps;
  }

  /** Saves the log and any error log entries. */
  public static void saveImportLog(
      String objectId,
      IPSSiteImportLogger logger,
      IPSImportLogDao logDao,
      String siteId,
      String desc)
      throws IPSGenericDao.SaveException {
    Validate.notEmpty(objectId);
    Validate.notNull(logger);
    Validate.notNull(logDao);
    Validate.notNull(desc);

    var entry =
        new PSImportLogEntry(objectId, logger.getType().name(), new Date(), logger.getLog());
    logDao.save(entry);

    if (!StringUtils.isBlank(siteId)) {
      var errors = logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc);
      if (errors != null) {
        for (var errorLogEntry : errors) {
          logDao.save(errorLogEntry);
        }
      }
    }
  }

  /** Holds the current URL connection properties for restoration. */
  public static class URLConnectionProperties {
    private SSLSocketFactory defaultSSLSocketFactory = null;
    private HostnameVerifier defaultHostnameVerifier = null;

    public SSLSocketFactory getDefaultSSLSocketFactory() {
      return defaultSSLSocketFactory;
    }

    public void setDefaultSSLSocketFactory(SSLSocketFactory defaultSSLSocketFactory) {
      this.defaultSSLSocketFactory = defaultSSLSocketFactory;
    }

    public HostnameVerifier getDefaultHostnameVerifier() {
      return defaultHostnameVerifier;
    }

    public void setDefaultHostnameVerifier(HostnameVerifier defaultHostnameVerifier) {
      this.defaultHostnameVerifier = defaultHostnameVerifier;
    }
  }

  /** Override connection properties and install the JVM's default certificate manager. */
  public static URLConnectionProperties overrideConnectionProperties() {
    // CodeQL java/insecure-trustmanager (alert #791): previously installed an all-trusting
    // X509TrustManager that accepted any chain. Replace with the JVM's default trust managers,
    // which validate TLS server certificates against the system trust store (cacerts).
    // Operators who need to import a private CA / self-signed cert should add it to the JVM
    // trust store via `keytool -importcert -alias <name> -file <cert> -cacerts`.
    TrustManager[] defaultTrustManagers;
    try {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init((KeyStore) null);
      defaultTrustManagers = tmf.getTrustManagers();
      if (defaultTrustManagers == null || defaultTrustManagers.length == 0) {
        throw new GeneralSecurityException(
            "Default TrustManagerFactory returned no trust managers (algorithm="
                + TrustManagerFactory.getDefaultAlgorithm()
                + ")");
      }
    } catch (GeneralSecurityException e) {
      log.error("Error initializing default trust managers", e);
      return null;
    }

    try {
      var sc = SSLContext.getInstance("TLS");
      sc.init(null, defaultTrustManagers, new java.security.SecureRandom());

      var connectionData = new URLConnectionProperties();
      connectionData.setDefaultSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
      connectionData.setDefaultHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());

      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(
          (String urlHostName, SSLSession session) -> true);

      return connectionData;
    } catch (Exception e) {
      log.error("Error setting override certificates", e);
      return null;
    }
  }

  /** Restore connection properties to their values from before the override. */
  public static void restoreConnectionProperties(URLConnectionProperties properties) {
    if (properties != null) {
      HttpsURLConnection.setDefaultSSLSocketFactory(properties.getDefaultSSLSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(properties.getDefaultHostnameVerifier());
    }
  }
}
