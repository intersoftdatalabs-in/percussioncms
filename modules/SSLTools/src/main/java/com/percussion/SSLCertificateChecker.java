/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion;

import com.percussion.security.error.PSExceptionUtils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * This Java class can run on command line in main method and take an httpsurl as an argument and a
 * warningDays argument where warningDays is the number of days before an SSL certificate expires.
 * This class also accepts a urlFile argument that can be a list of urls to check. If there are
 * certificates expiring this class post to a configurable slack account and channel.
 *
 * <p>Slack account configurations can be added to slack.properties in the resources directory.
 */
public class SSLCertificateChecker {

  private static final String SLACK_PROPERTY_FILE = "/slack.properties";
  private URL slackUrl;
  private String slackUrlStr;
  private String slackChannel;
  private String slackUserName;
  private boolean messagePostedFlag = false;
  private StringBuilder messageBuffer = null;

  private static final Logger log = LogManager.getLogger(SSLCertificateChecker.class.getName());

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public SSLCertificateChecker() {
    // utility state is initialized lazily on first use
  }

  /**
   * Entry point used when the class is run on the command line.
   *
   * <p>Arguments expected are:
   *
   * <ol>
   *   <li>url-or-file: a single HTTPS URL or the path of a file containing one URL per line.
   *   <li>warningDays: number of days before an SSL certificate expires that should trigger a
   *       warning notification.
   * </ol>
   *
   * @param args the command-line arguments as documented above.
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      log.info("Usage: PSSSLCertificateChecker [url or file containing urls] [warningDays] ");
      return;
    }
    SSLCertificateChecker sslChecker = new SSLCertificateChecker();
    sslChecker.initSlackProperties();
    String urlStr = args[0];
    String warnDays = args[1];
    int warningDays = Integer.parseInt(warnDays);
    // b'coz maximum limit of slack post message is 4000 chars.

    URL url = null;
    try {
      URI uri = URI.create(urlStr);
      url = uri.toURL();
    } catch (Exception e) {
      // Eating this Exception as may be list of urls sent in file rather than 1 url.
    }

    if (url != null) {
      sslChecker.checkCertificate(urlStr, warningDays);
    } else {

      try (BufferedReader reader = new BufferedReader(new FileReader(urlStr))) {
        String line = reader.readLine();
        while (line != null) {
          sslChecker.checkCertificate(line, warningDays);
          line = reader.readLine();
        }
      } catch (IOException e) {
        log.error("Invalid URL or File passed : {}", urlStr);
      }
    }
    if (!sslChecker.messagePostedFlag) {
      if (sslChecker.messageBuffer == null
          || sslChecker.messageBuffer.toString().trim().length() == 0) {
        return;
      }
      sslChecker.postSlackMessage();
    }
  }

  // TODO: Remove me @SuppressFBWarnings("URLCONNECTION_SSRF_FD")
  private void checkCertificate(String urlStr, int warningDays) {
    URL url = null;

    try {
      URI uri = URI.create(urlStr);
      url = uri.toURL();
    } catch (MalformedURLException e) {
      log.error("Not a Valid URL : {}", urlStr);
      return;
    }
    if (url == null) {
      log.error("Not a valid URL : {}", urlStr);
      return;
    }
    try {
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      conn.connect();
      SecureHeaderCheckResponse headerCheck = SecureHeaderChecker.check(conn);
      if (headerCheck.isFailedCheck()) {
        StringBuilder msg = new StringBuilder();
        msg.append(url).append(" is missing the following secure headers:\r\n");
        for (Map.Entry<String, Boolean> e : headerCheck.getChecks().entrySet()) {
          if (Boolean.FALSE.equals(e.getValue())) msg.append(e.getKey()).append("\r\n");
        }
        sendSlackMessage(msg.toString());
      }

      try {
        Certificate[] certs = conn.getServerCertificates();
        for (Certificate c : certs) {
          if (c instanceof X509Certificate) {
            X509Certificate xc = (X509Certificate) c;
            Date expiresOn = xc.getNotAfter();
            Date now = new Date();
            long daysLeft = (expiresOn.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
            if (daysLeft < warningDays) {
              String msg =
                  url
                      + " : "
                      + " Certificate Will expire on : "
                      + expiresOn
                      + " So, only "
                      + daysLeft
                      + " days to go";
              sendSlackMessage(msg);
            }
          }
        }
      } catch (IllegalStateException e) {
        log.error("Error: {} connecting to host: {}", url, PSExceptionUtils.getMessageForLog(e));
      }

    } catch (IOException io) {
      log.error("Failed to Load Certificates for given URL : {}", urlStr);
    }
  }

  /**
   * This API loads slack post properties required from slack.properties file in the current
   * directory resources folder
   */
  private void initSlackProperties() {

    try {
      Properties prop = new Properties();
      try (InputStream input = this.getClass().getResourceAsStream(SLACK_PROPERTY_FILE)) {
        // load a properties file
        prop.load(input);
      }
      slackUrlStr = prop.getProperty("url");
      URI uri = URI.create(slackUrlStr);
      slackUrl = uri.toURL();
      slackChannel = prop.getProperty("channel");
      slackUserName = prop.getProperty("username");

    } catch (FileNotFoundException fnf) {
      log.error("Slack Properties file not found");
    } catch (IOException io) {
      log.error("Failed to load Slack Properties File");
    }
  }

  /**
   * This API keeps collecting the messages in messageBuffer and puts the message on console as
   * well.
   */
  private void sendSlackMessage(String message) {

    log.info("{}", message);
    if (messageBuffer == null) {
      messageBuffer = new StringBuilder(4000);
      messageBuffer.append(message);
    } else if (messageBuffer.length() < 2000) {
      messageBuffer.append(System.getProperty("line.separator"));
      messageBuffer.append(message);

    } else {
      messageBuffer.append(System.getProperty("line.separator"));
      messageBuffer.append(message);
      postSlackMessage();
    }
  }

  /**
   * This API actually posts an http Request to Slack Url In case Slack properties are not set, then
   * just system out will happen on console.
   */
  private void postSlackMessage() {

    if (slackUrl != null) {
      JSONObject json = new JSONObject();
      try {
        json.put("channel", slackChannel);
        json.put("text", messageBuffer.toString());
        json.put("username", slackUserName);

        HttpRequest request =
            HttpRequest.newBuilder(URI.create(slackUrlStr))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());

      } catch (Exception ex) {
        log.error(ex.getMessage());
        log.debug(ex.getMessage(), ex);
        if (ex instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      } finally {
        messagePostedFlag = true;
        messageBuffer = null;
      }
    }
  }
}
