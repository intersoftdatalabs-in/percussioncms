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

package com.percussion.utils.container;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Describes a connector (HTTP/HTTPS/AJP) configuration used by the container.
 */
public interface IPSConnector {

  /** HTTPS scheme constant. */
  String SCHEME_HTTPS = "https";

  /** HTTP scheme constant. */
  String SCHEME_HTTP = "http";

  /** HTTP/1.1 protocol constant. */
  String PROTOCOL_HTTP = "HTTP/1.1";

  /** AJP/1.3 protocol constant. */
  String PROTOCOL_AJP = "AJP/1.3";

  /**
   * Get the scheme.
   *
   * @return the scheme, may be <code>null</code>.
   */
  String getScheme();

  /**
   * Set the scheme.
   *
   * @param scheme the scheme to set, may be <code>null</code>.
   */
  void setScheme(String scheme);

  /**
   * Get the host address.
   *
   * @return the host address, may be <code>null</code>.
   */
  String getHostAddress();

  /**
   * Set the host address.
   *
   * @param getHostAddress the host address to set, may be <code>null</code>.
   */
  void setHostAddress(String getHostAddress);

  /**
   * Get the port.
   *
   * @return the port.
   */
  int getPort();

  /**
   * Set the port.
   *
   * @param port the port to set.
   */
  void setPort(int port);

  /**
   * Get the keystore file.
   *
   * @return the keystore file, may be <code>null</code>.
   */
  Path getKeystoreFile();

  /**
   * Set the keystore file.
   *
   * @param keystoreFile the keystore file to set, may be <code>null</code>.
   */
  void setKeystoreFile(Path keystoreFile);

  /**
   * Get the keystore password.
   *
   * @return the keystore password, may be <code>null</code>.
   */
  String getKeystorePass();

  /**
   * Set the keystore password.
   *
   * @param keystorePass the keystore password to set, may be <code>null</code>.
   */
  void setKeystorePass(String keystorePass);

  /**
   * Get the truststore file.
   *
   * @return the truststore file, may be <code>null</code>.
   */
  Path getTruststoreFile();

  /**
   * Set the truststore file.
   *
   * @param truststoreFile the truststore file to set, may be <code>null</code>.
   */
  void setTruststoreFile(Path truststoreFile);

  /**
   * Get the truststore password.
   *
   * @return the truststore password, may be <code>null</code>.
   */
  String getTruststorePass();

  /**
   * Set the truststore password.
   *
   * @param truststorePass the truststore password to set, may be <code>null</code>.
   */
  void setTruststorePass(String truststorePass);

  /**
   * Get the ciphers.
   *
   * @return the ciphers, never <code>null</code>.
   */
  Set<String> getCiphers();

  /**
   * Set the ciphers.
   *
   * @param ciphers the ciphers to set, may be <code>null</code>.
   */
  void setCiphers(Set<String> ciphers);

  /**
   * Get the SSL protocols.
   *
   * @return the SSL protocols, never <code>null</code>.
   */
  Set<String> getSslProtocols();

  /**
   * Set the SSL protocols.
   *
   * @param protocols the protocols to set, may be <code>null</code>.
   */
  void setSslProtocols(Set<String> protocols);

  /**
   * Get the protocol.
   *
   * @return the protocol, may be <code>null</code>.
   */
  String getProtocol();

  /**
   * Set the protocol.
   *
   * @param protocol the protocol to set, may be <code>null</code>.
   */
  void setProtocol(String protocol);

  /**
   * Copy configuration from another connector.
   *
   * @param c the connector to copy from, may not be <code>null</code>.
   */
  void copyFrom(IPSConnector c);

  /**
   * Whether this connector uses HTTPS.
   *
   * @return <code>true</code> if HTTPS, <code>false</code> otherwise.
   */
  default boolean isHttps() {
    return this.getScheme() != null && this.getScheme().equals(SCHEME_HTTPS);
  }

  /**
   * Whether this connector uses HTTP.
   *
   * @return <code>true</code> if HTTP, <code>false</code> otherwise.
   */
  default boolean isHttp() {
    return (this.getProtocol() != null && this.getProtocol().equals(PROTOCOL_HTTP))
        && Optional.ofNullable(this.getScheme()).orElse(SCHEME_HTTP).equals(SCHEME_HTTP);
  }

  /**
   * Whether this connector uses AJP.
   *
   * @return <code>true</code> if AJP, <code>false</code> otherwise.
   */
  default boolean isAJP() {
    return (this.getProtocol() != null && this.getProtocol().equals(PROTOCOL_AJP));
  }

  /**
   * Get the callback host.
   *
   * @return the callback host, never <code>null</code>.
   */
  default String getCallbackHost() {
    return (this.getHostAddress() == null || this.getHostAddress().equals("0.0.0.0"))
        ? "127.0.0.1"
        : this.getHostAddress();
  }

  /**
   * Get the properties.
   *
   * @return the properties, never <code>null</code>.
   */
  Map<String, String> getProperties();
}