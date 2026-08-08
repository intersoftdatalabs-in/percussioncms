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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Aggregates multiple {@link X509TrustManager} instances and delegates certificate checks to each in
 * order. Used by the TLS testing utilities to evaluate trust against several trust stores.
 */
public class WrappedTrustManager implements X509TrustManager {

  /** Logger used by this class. */
  private static final Logger log = LogManager.getLogger(WrappedTrustManager.class);

  /** Wrapped trust managers keyed by their identifying alias. */
  private LinkedHashMap<String, X509TrustManager> wrappedManagers = new LinkedHashMap<>();

  WrappedTrustManager() {
    addKeyStore("Default Java", null);
  }

  /**
   * Registers a named keystore whose trust manager should be wrapped.
   *
   * @param name the alias used to identify this trust store
   * @param keyStore the keystore to wrap, or {@code null} to use the JVM default trust store
   */
  public void addKeyStore(String name, KeyStore keyStore) {
    try {
      wrappedManagers.put(name, getTrustManager(keyStore));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("No such algorithm", e);
    } catch (KeyStoreException e) {
      throw new RuntimeException("Not adding keystore due to error", e);
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {

    // If you're planning to use client-cert auth,
    // merge results from "defaultTm" and "myTm".
    Set<X509Certificate> accepted = new HashSet<>();
    for (Map.Entry<String, X509TrustManager> thistm : wrappedManagers.entrySet()) {
      accepted.addAll(Arrays.asList(thistm.getValue().getAcceptedIssuers()));
    }
    return accepted.toArray(new X509Certificate[accepted.size()]);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {

    CertificateException exception = null;
    for (Map.Entry<String, X509TrustManager> thistm : wrappedManagers.entrySet()) {
      String successTm = thistm.getKey();
      try {
        thistm.getValue().checkServerTrusted(chain, authType);
        log.debug("Server certificate validation succeeded with trust manager: {}", successTm);
        return;
      } catch (CertificateException e) {
        log.debug("Server certificate validation failed for trust manager: {}", thistm.getKey());
        exception = e;
      }
    }
    log.warn("Failed to validate server certificate with any trust manager");

    throw exception;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    // If you're planning to use client-cert auth,
    // do the same as checking the server.
    CertificateException exception = null;
    for (Map.Entry<String, X509TrustManager> thistm : wrappedManagers.entrySet()) {
      try {
        thistm.getValue().checkClientTrusted(chain, authType);
        return;
      } catch (CertificateException e) {
        exception = e;
      }
    }

    if (exception != null) {
      log.warn("Failed to validate client certificate with any trust manager");
      throw exception;
    } else {
      log.debug("Client certificate validation failed with all trust managers");
    }
  }

  private static X509TrustManager getTrustManager(KeyStore keystore)
      throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    // Using null here initialises the TMF with the default trust store.
    tmf.init(keystore);

    TrustManager[] trustManagers = tmf.getTrustManagers();
    if (trustManagers.length == 0) {
      throw new KeyStoreException("No trust managers found");
    }
    return (X509TrustManager) trustManagers[0];
  }
}
