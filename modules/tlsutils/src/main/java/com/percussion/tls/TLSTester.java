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

package com.percussion.tls;

import com.percussion.security.error.PSExceptionUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility for verifying TLS connections to a target host. */
public class TLSTester {

  /** No-op constructor. */
  public TLSTester() {
    // no-op
  }

  /** Logger used by this class. */
  private static final Logger log = LogManager.getLogger(TLSTester.class);

  /** Cached value of the {@code os.name} system property. */
  private static String OS = null;

  /**
   * Returns the cached value of the {@code os.name} system property, computing it on first access.
   *
   * @return the operating system name
   */
  public static String getOsName() {
    if (OS == null) {
      OS = System.getProperty("os.name");
    }
    return OS;
  }

  /**
   * Tests whether the current operating system is Windows.
   *
   * @return {@code true} if {@code os.name} starts with "windows"
   */
  public static boolean isWindows() {
    // perform case-insensitive check, cache handles case already
    return getOsName().toLowerCase().startsWith("windows");
  }

  /** Default password used when reading the bundled keystore. */
  public static final String KEYSTORE_PASS = "changeit";

  /**
   * Command-line entry point that exercises the TLS utilities against the hosts/ports supplied on
   * the command line.
   *
   * @param args command-line arguments; expected to be {@code host port} pairs
   * @throws IOException if a network or keystore I/O error occurs
   * @throws NoSuchAlgorithmException if a required algorithm is not available
   * @throws CertificateException if a certificate cannot be parsed or validated
   * @throws KeyManagementException if the key management subsystem fails
   * @throws KeyStoreException if the keystore cannot be loaded
   */
  public static void main(String[] args)
      throws IOException,
          NoSuchAlgorithmException,
          CertificateException,
          KeyManagementException,
          KeyStoreException {

    String yahoocert =
        "-----BEGIN CERTIFICATE-----\n"
            + "MIIJHzCCCAegAwIBAgIQCJCo+qXyE8vjILXtpTJnkjANBgkqhkiG9w0BAQsFADBw\n"
            + "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n"
            + "d3cuZGlnaWNlcnQuY29tMS8wLQYDVQQDEyZEaWdpQ2VydCBTSEEyIEhpZ2ggQXNz\n"
            + "dXJhbmNlIFNlcnZlciBDQTAeFw0xOTA1MDEwMDAwMDBaFw0xOTEwMjgxMjAwMDBa\n"
            + "MGMxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRIwEAYDVQQHEwlT\n"
            + "dW5ueXZhbGUxETAPBgNVBAoTCE9hdGggSW5jMRgwFgYDVQQDDA8qLnd3dy55YWhv\n"
            + "by5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCkM1GHoSo/9oKj\n"
            + "PqENo9GMbP5yvtXZQoi8doHlLkHOGToMV90U+zKxfobAGYlJYV4kJjCxHXg/8FU0\n"
            + "AYvHVcs+VhicEGaSIUZ6p1T87YqqKC5x7QSUMk+ffmHA0Y75bMqOogmy4o6p+7fq\n"
            + "t4qaW1XHm0a3vXCZNFAz2nrJg3RI8bcCFbdP4mFccHuDH31s9gNJDFKD/qgaB29p\n"
            + "gR+uL0X/T8REDHVDtIfaDHE9WpPxeqdwltDQieGlg4Bm40jJDcHA7u0gpVnlTX77\n"
            + "0JajcqGguTXIpAqQJH14iHDE8oqSvFJF0Hy3xZTl4cn/LqvHBzvdYYMu6RCaKNvG\n"
            + "fqhKJSzvAgMBAAGjggXAMIIFvDAfBgNVHSMEGDAWgBRRaP+QrwIHdTzM2WVkYqIS\n"
            + "uFlyOzAdBgNVHQ4EFgQUrpm7KbnCAMUN1HOkiQNiNVmUAZswggLpBgNVHREEggLg\n"
            + "MIIC3IIPKi53d3cueWFob28uY29tghBhZGQubXkueWFob28uY29tgg4qLmFtcC55\n"
            + "aW1nLmNvbYIMYXUueWFob28uY29tggxiZS55YWhvby5jb22CDGJyLnlhaG9vLmNv\n"
            + "bYIPY2EubXkueWFob28uY29tghNjYS5yb2dlcnMueWFob28uY29tggxjYS55YWhv\n"
            + "by5jb22CEGRkbC5mcC55YWhvby5jb22CDGRlLnlhaG9vLmNvbYIUZW4tbWFrdG9v\n"
            + "Yi55YWhvby5jb22CEWVzcGFub2wueWFob28uY29tggxlcy55YWhvby5jb22CD2Zy\n"
            + "LWJlLnlhaG9vLmNvbYIWZnItY2Eucm9nZXJzLnlhaG9vLmNvbYISZnJvbnRpZXIu\n"
            + "eWFob28uY29tggxmci55YWhvby5jb22CDGdyLnlhaG9vLmNvbYIMaGsueWFob28u\n"
            + "Y29tgg5oc3JkLnlhaG9vLmNvbYIXaWRlYW5ldHNldHRlci55YWhvby5jb22CDGlk\n"
            + "LnlhaG9vLmNvbYIMaWUueWFob28uY29tggxpbi55YWhvby5jb22CDGl0LnlhaG9v\n"
            + "LmNvbYIRbWFrdG9vYi55YWhvby5jb22CEm1hbGF5c2lhLnlhaG9vLmNvbYIMbWJw\n"
            + "LnlpbWcuY29tggxteS55YWhvby5jb22CDG56LnlhaG9vLmNvbYIMcGgueWFob28u\n"
            + "Y29tggxxYy55YWhvby5jb22CDHJvLnlhaG9vLmNvbYIMc2UueWFob28uY29tggxz\n"
            + "Zy55YWhvby5jb22CDHR3LnlhaG9vLmNvbYIMdWsueWFob28uY29tggx1cy55YWhv\n"
            + "by5jb22CEXZlcml6b24ueWFob28uY29tggx2bi55YWhvby5jb22CDXd3dy55YWhv\n"
            + "by5jb22CCXlhaG9vLmNvbYIMemEueWFob28uY29tgg9oay5yZC55YWhvby5jb22C\n"
            + "D3R3LnJkLnlhaG9vLmNvbTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYB\n"
            + "BQUHAwEGCCsGAQUFBwMCMHUGA1UdHwRuMGwwNKAyoDCGLmh0dHA6Ly9jcmwzLmRp\n"
            + "Z2ljZXJ0LmNvbS9zaGEyLWhhLXNlcnZlci1nNi5jcmwwNKAyoDCGLmh0dHA6Ly9j\n"
            + "cmw0LmRpZ2ljZXJ0LmNvbS9zaGEyLWhhLXNlcnZlci1nNi5jcmwwTAYDVR0gBEUw\n"
            + "QzA3BglghkgBhv1sAQEwKjAoBggrBgEFBQcCARYcaHR0cHM6Ly93d3cuZGlnaWNl\n"
            + "cnQuY29tL0NQUzAIBgZngQwBAgIwgYMGCCsGAQUFBwEBBHcwdTAkBggrBgEFBQcw\n"
            + "AYYYaHR0cDovL29jc3AuZGlnaWNlcnQuY29tME0GCCsGAQUFBzAChkFodHRwOi8v\n"
            + "Y2FjZXJ0cy5kaWdpY2VydC5jb20vRGlnaUNlcnRTSEEySGlnaEFzc3VyYW5jZVNl\n"
            + "cnZlckNBLmNydDAMBgNVHRMBAf8EAjAAMIIBAwYKKwYBBAHWeQIEAgSB9ASB8QDv\n"
            + "AHYA7ku9t3XOYLrhQmkfq+GeZqMPfl+wctiDAMR7iXqo/csAAAFqdMSsygAABAMA\n"
            + "RzBFAiEA+x2otregfacyFT3PRD33cgNQWIi4yrR0kBAtnCZsn2kCIDrHp/xP6zwD\n"
            + "dsGqEjSINAE9jmKrgo/elELKjftwT83lAHUAh3W/51l8+IxDmV+9827/Vo1HVjb/\n"
            + "SrVgwbTq/16ggw8AAAFqdMSt5gAABAMARjBEAiAB4YV22p0U22BJh5roMLNgx+Ms\n"
            + "h2VIEz0Jz56BSmtv6gIgP2dSVn2gw61bntjp9yGGR14Lyj5Q+LwTlVXmvNrlW1sw\n"
            + "DQYJKoZIhvcNAQELBQADggEBALAFKLcI0WP4KM5SSnQniOi0Y3lVaVCRsEX40aIp\n"
            + "2vA1oPnrN+Y1ZvheFnZXfT2wlfbvEW4RBIT2NBm7z+adVldZ+lQE56qgng+Tab/j\n"
            + "bccWlpHioITDQHkILEZEi4jpD6L3A55OfJtOtanYF4ZriagYW7XUmaHGsKEgAJ7N\n"
            + "OsqsXud1I8L/DYkokttQnbiPvl+3jNnwlq4vbHvYJMBHTr9vwUJHRpLyGkpD7cwn\n"
            + "FRqHMK/+/gxjRr+GgNgA5UwjptyEwzfiXlHpOgYhawSS/pJphxjpNpnwbfozwo4j\n"
            + "ThR/tNqj9qhqwdtKQKNYEhyQNipodImwdKGcDIOC77cgj/A=\n"
            + "-----END CERTIFICATE-----\n";

    final File store = new File("truststore.jks");
    KeyStore jksKeystore = getJKSKeystore(store, KEYSTORE_PASS, true);

    boolean isWindows = isWindows();
    WrappedTrustManager customTm = new WrappedTrustManager();
    customTm.addKeyStore("Local Keystore", jksKeystore);

    if (isWindows) {
      KeyStore windowsMyKeystore = KeyStore.getInstance("Windows-MY");
      KeyStore winRootKeystore = KeyStore.getInstance("Windows-ROOT");
      customTm.addKeyStore("Windows-MY", windowsMyKeystore);
      customTm.addKeyStore("Windows-ROOT", winRootKeystore);
    }

    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(null, new TrustManager[] {customTm}, null);

    SSLSocketFactory sf = sslContext.getSocketFactory();
    String[] supportedCiphers = null;
    String[] protocols = null;
    try (SSLSocket socket = (SSLSocket) sf.createSocket("www.google.com", 443)) {

      supportedCiphers = socket.getSupportedCipherSuites();
      protocols = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};
    }
    Set<String> workingProtocols = new HashSet<>();
    Map<String, Set<String>> serverSupported = new HashMap<>();

    boolean connected = true;

    Set<String> testProt = new HashSet<>(Arrays.asList(protocols));
    Set<String> testCipher = null;
    String lastProt = null;
    testCipher = new HashSet<>(Arrays.asList(supportedCiphers));
    log.debug("Testing cipher suites: {}", testCipher);
    try {
      while (connected = true) {
        try {

          try (SSLSocket socket = (SSLSocket) sf.createSocket("www.percussion.com", 443)) {
            socket.setSoTimeout(500);
            socket.setEnabledProtocols(testProt.toArray(new String[testProt.size()]));
            socket.setEnabledCipherSuites(testCipher.toArray(new String[testCipher.size()]));
            socket.startHandshake();
            SSLSession session = socket.getSession();
            socket.close();
            String cipher = session.getCipherSuite();
            String protocol = session.getProtocol();
            socket.close();

            lastProt = protocol;
            log.info("Connected with protocol {} using cipher {}", protocol, cipher);
            testCipher.remove(cipher);
            workingProtocols.add(protocol + ":" + cipher);
          }
        } catch (IOException e) {

          if (testProt.size() > 0 && testCipher.size() != supportedCiphers.length) {
            testProt.remove(lastProt);
            testCipher = new HashSet<>(Arrays.asList(supportedCiphers));
          } else {

            throw e;
          }
        }
      }
    } catch (IOException e2) {
      log.info("No more SSL connections available for testing");
    }

    //  SSLSocket csf = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
    String https_url = "https://www.google.com/";
    URL url;
    try {

      url = new URL(https_url);
      HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

      // dumpl all cert info
      print_https_cert(con);

      // dump all the content
      // print_content(con);

    } catch (MalformedURLException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  private static void importCertificate(
      X509Certificate caCert,
      String pem,
      CertificateFactory cf,
      KeyStore myTrustStore,
      File store) {

    try (FileInputStream myKeys = new FileInputStream(store)) {

      // Do the same with your trust store this time
      // Adapt how you load the keystore to your needs

      myTrustStore.load(myKeys, KEYSTORE_PASS.toCharArray());
      Principal DN = caCert.getSubjectDN();
      try (ByteArrayInputStream bis = new ByteArrayInputStream(pem.getBytes())) {
        Certificate cert = cf.generateCertificate(bis);
        String alias = caCert.getSubjectDN().getName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        log.debug("Adding certificate alias: {}", alias);
        myTrustStore.setCertificateEntry(alias, cert);
      }
      try (FileOutputStream fo = new FileOutputStream(store)) {
        myTrustStore.store(fo, KEYSTORE_PASS.toCharArray());
      }
    } catch (NoSuchAlgorithmException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (KeyStoreException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (CertificateException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return;
  }

  private static void print_content(HttpsURLConnection con) {
    if (con != null) {

      log.info("****** Content of the URL ********");
      try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {

        String input;

        while ((input = br.readLine()) != null) {
          log.info(input);
        }

      } catch (IOException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }
  }

  private static void print_https_cert(HttpsURLConnection con) {

    if (con != null) {

      try {

        log.info("Response Code : {}", con.getResponseCode());
        log.info("Cipher Suite : {}", con.getCipherSuite());

        Certificate[] certs = con.getServerCertificates();
        for (Certificate cert : certs) {
          log.info("Cert Type : {}", cert.getType());
          log.info("Cert Hash Code : {}", cert.hashCode());
          log.info("Cert Public Key Algorithm : {}", cert.getPublicKey().getAlgorithm());
          log.info("Cert Public Key Format : {}", cert.getPublicKey().getFormat());
        }

      } catch (SSLPeerUnverifiedException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      } catch (IOException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }
  }

  private static void listCiphers() throws IOException {
    SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    String scheme = "https";

    // SecureProtocolSocketFactory ssf = new TLSV12ProtocolSocketFactory(baseFactory);

    SSLSocket csf = (SSLSocket) SSLSocketFactory.getDefault().createSocket();

    Set<String> enabledCiphers = new HashSet<>(Arrays.asList(csf.getEnabledCipherSuites()));
    Set<String> defaultCiphers = new HashSet<>(Arrays.asList(ssf.getDefaultCipherSuites()));
    Set<String> availableCiphers = new HashSet<>(Arrays.asList(ssf.getSupportedCipherSuites()));

    log.info("Default\tCipher");
    for (Iterator i = availableCiphers.iterator(); i.hasNext(); ) {
      String cipher = (String) i.next();
      StringBuilder cipherInfo = new StringBuilder();
      if (defaultCiphers.contains(cipher)) cipherInfo.append('*');
      else cipherInfo.append(' ');
      if (enabledCiphers.contains(cipher)) cipherInfo.append('*');
      else cipherInfo.append(' ');

      cipherInfo.append('\t');
      cipherInfo.append(cipher);
      log.info(cipherInfo.toString());
    }
  }

  /**
 * Tests whether the JCE jurisdiction policy allows unlimited-strength cryptography.
 *
 * @return {@code true} if {@link Cipher#getMaxAllowedKeyLength(String)} returns
 *     {@link Integer#MAX_VALUE} for AES, {@code false} otherwise
 */
static boolean isUnlimitedCryptoLength() {

    try {
      int length = Cipher.getMaxAllowedKeyLength("AES");
      // 128 is the limited cryto length, and Int.max_value is is unlimited.
      boolean unlimited = (length == Integer.MAX_VALUE);
      return unlimited;
    } catch (NoSuchAlgorithmException e) {
    }
    // catch (NoSuchProviderException e) {
    // }
    return false;
  }

  /**
 * Logs the cipher suites enabled for each installed security provider. Used for diagnostics.
 */
public static void getEnabledCiphers() {
    for (Provider provider : Security.getProviders()) {
      log.info("Security Provider: {}", provider.getName());
      for (String key : provider.stringPropertyNames())
        log.debug("\t{}\t{}", key, provider.getProperty(key));
    }
  }

  /**
   * Converts an X.509 certificate to its PEM-encoded string representation.
   *
   * @param cert the certificate to encode, never {@code null}
   * @return the PEM-encoded certificate, including begin/end markers
   * @throws CertificateEncodingException if the certificate cannot be DER-encoded
   */
  protected static String convertToPem(X509Certificate cert) throws CertificateEncodingException {

    String cert_begin = "-----BEGIN CERTIFICATE-----\n";
    String end_cert = "-----END CERTIFICATE-----";

    byte[] derCert = cert.getEncoded();
    String pemCertPre = new String(Base64.encodeBase64(derCert, true));
    String pemCert = cert_begin + pemCertPre + end_cert;
    return pemCert;
  }

  private static KeyStore getJKSKeystore(File store, String password, boolean create)
      throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
    KeyStore myTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());

    if (!store.exists()) {

      myTrustStore.load(null, null);
      try (FileOutputStream fo = new FileOutputStream(store)) {
        myTrustStore.store(fo, password.toCharArray());
      }
    }

    try (FileInputStream myKeys = new FileInputStream(store)) {
      myTrustStore.load(myKeys, KEYSTORE_PASS.toCharArray());
    }
    return myTrustStore;
  }
}
