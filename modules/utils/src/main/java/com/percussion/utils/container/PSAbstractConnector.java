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

import com.percussion.utils.string.PSStringUtils;
import com.percussion.utils.tomcat.PSTomcatConnector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.w3c.dom.Element;

/**
 * Abstract connector.
 */
public abstract class PSAbstractConnector implements IPSConnector, XMLEnabled {

  /** Scheme constant for the "http" protocol. */
  public static final String SCHEME_HTTP = "http";

  /** Scheme constant for the "https" protocol. */
  public static final String SCHEME_HTTPS = "https";

  /** Member. */
  private static Pattern EXTRACT_PROPNAME = Pattern.compile("\\$\\{([^}]*)}");

  /** Member. */
  private Path connectorFileContext = Paths.get(".");

  /** {@inheritDoc} */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Sets the connector properties.
   *
   * @param properties the properties to set, never <code>null</code>
   */
  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  /** Field. */
  protected Map<String, String> properties = new HashMap<>();

  /** Field. */
  protected String scheme = SCHEME_HTTP;
  /** Field. */
  protected String getHostAddress;
  /** Field. */
  protected Path keystoreFile;
  /** Field. */
  protected String keystorePass;
  /** Field. */
  protected Path truststoreFile;
  /** Field. */
  protected String truststorePass;
  /** Field. */
  protected int port;
  /** Field. */
  protected Set<String> ciphers;
  /** Field. */
  protected Set<String> sslProtocols;

  /** Field. */
  protected String protocol = PROTOCOL_HTTP;

  /**
   * Creates a connector bound to the given file context.
   *
   * @param connectorFileContext base directory for relative connector files, never <code>null
   *     </code>
   */
  public PSAbstractConnector(Path connectorFileContext) {
    this.connectorFileContext = connectorFileContext;
  }

  /**
   * Resolves {@code ${name}} property references in the supplied value.
   *
   * @param value the raw value, may be <code>null</code>
   * @return the value with known references resolved, never <code>null</code>
   */
  public Optional<String> processPropertyReference(String value) {

    if (value == null) return Optional.empty();

    Matcher matcher = EXTRACT_PROPNAME.matcher(value);

    StringBuffer buffer = new StringBuffer();
    // if (!matcher.matches())
    //    return Optional.of(value);

    while (matcher.find()) {
      // if cannot find the property name within ${ } then just use the original text
      String replacement = properties.getOrDefault(matcher.group(1), matcher.group(0));

      if (replacement != null) {
        // matcher.appendReplacement(buffer, replacement);
        // see comment
        matcher.appendReplacement(buffer, "");
        buffer.append(replacement);
      }
    }
    matcher.appendTail(buffer);
    return Optional.of(buffer.toString());
  }

  /** {@inheritDoc} */
  @Override
  public String getScheme() {
    return scheme;
  }

  /** {@inheritDoc} */
  @Override
  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  /** {@inheritDoc} */
  @Override
  public String getHostAddress() {
    return getHostAddress;
  }

  /** {@inheritDoc} */
  @Override
  public void setHostAddress(String getHostAddress) {
    this.getHostAddress = getHostAddress;
  }

  /** {@inheritDoc} */
  @Override
  public Path getKeystoreFile() {
    return keystoreFile;
  }

  /** {@inheritDoc} */
  @Override
  public void setKeystoreFile(Path keystoreFile) {
    this.keystoreFile = keystoreFile;
  }

  /** {@inheritDoc} */
  @Override
  public String getKeystorePass() {
    return keystorePass;
  }

  /** {@inheritDoc} */
  @Override
  public void setKeystorePass(String keystorePass) {
    this.keystorePass = keystorePass;
  }

  /** {@inheritDoc} */
  @Override
  public Path getTruststoreFile() {
    return truststoreFile;
  }

  /** {@inheritDoc} */
  @Override
  public void setTruststoreFile(Path truststoreFile) {
    this.truststoreFile = truststoreFile;
  }

  /** {@inheritDoc} */
  @Override
  public String getTruststorePass() {
    return truststorePass;
  }

  /** {@inheritDoc} */
  @Override
  public void setTruststorePass(String truststorePass) {
    this.truststorePass = truststorePass;
  }

  /** {@inheritDoc} */
  @Override
  public int getPort() {
    return port;
  }

  /** {@inheritDoc} */
  @Override
  public void setPort(int port) {
    this.port = port;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getCiphers() {
    return ciphers;
  }

  /** {@inheritDoc} */
  @Override
  public void setCiphers(Set<String> ciphers) {
    this.ciphers = ciphers;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getSslProtocols() {
    return sslProtocols;
  }

  /** {@inheritDoc} */
  @Override
  public void setSslProtocols(Set<String> protocols) {
    this.sslProtocols = protocols;
  }

  /** {@inheritDoc} */
  @Override
  public String getProtocol() {
    return protocol;
  }

  /** {@inheritDoc} */
  @Override
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /**
   * Joins the supplied strings with commas.
   *
   * @param string the strings to join, never <code>null</code>
   * @return the comma-separated string, never <code>null</code>
   */
  public String toCommaString(Collection<String> string) {
    return string.stream().collect(Collectors.joining(","));
  }

  /** {@inheritDoc} */
  @Override
  public void copyFrom(IPSConnector c) {
    Map<String, String> fromProperties = c.getProperties();
    this.getProperties().putAll(fromProperties);
    updateFields();
  }

  /** Field. */
  protected abstract void updateFields();

  /** Field. */
  protected abstract void updateProperties();

  /**
   * To string.
   *
   * @return the string.
   */
  @Override
  public String toString() {
    return "PSAbstractConnector{"
        + "protocol="
        + protocol
        + ", scheme='"
        + scheme
        + '\''
        + ", getHostAddress='"
        + getHostAddress
        + '\''
        + ", keystoreFile="
        + keystoreFile
        + ", keystorePass='"
        + PSStringUtils.hidePass(keystorePass)
        + '\''
        + ", truststoreFile="
        + truststoreFile
        + ", truststorePass='"
        + PSStringUtils.hidePass(truststorePass)
        + '\''
        + ", port="
        + port
        + ", ciphers="
        + ciphers
        + ", sslProtocols="
        + sslProtocols
        + '}';
  }

  /**
   * Returns the file context this connector resolves relative files against.
   *
   * @return the connector file context, never <code>null</code>
   */
  public Path getConnectorFileContext() {
    return connectorFileContext;
  }

  /**
   * Returns a function resolving a relative path against the connector file context.
   *
   * @return the resolving function, never <code>null</code>
   */
  protected Function<String, Path> getAbsolutePath() {
    return p -> getConnectorFileContext().resolve(p).normalize().toAbsolutePath();
  }

  /**
   * Returns a function relativizing a path against the connector file context.
   *
   * @return the relativizing function, never <code>null</code>
   */
  protected Function<Path, String> getRelativePathString() {
    return p ->
        getConnectorFileContext()
            .normalize()
            .toAbsolutePath()
            .relativize(getConnectorFileContext().resolve(p).normalize().toAbsolutePath())
            .toString();
  }

  /** Builder. */
  public static class Builder {
  /** Field. */
    protected String scheme = SCHEME_HTTP;
  /** Field. */
    protected Path connectorFileContext;
  /** Field. */
    protected Element source;
  /** Field. */
    protected IPSConnector copyFrom;
  /** Field. */
    protected Map<String, String> props;
  /** Field. */
    protected int port;

    /** Default constructor. */
    public Builder() {}

    /**
     * Copy constructor.
     *
     * @param connectorBuilder the builder to copy, never <code>null</code>
     */
    public Builder(Builder connectorBuilder) {
      this.connectorFileContext = connectorBuilder.connectorFileContext;
      this.source = connectorBuilder.source;
      this.copyFrom = connectorBuilder.copyFrom;
      this.props = connectorBuilder.props;
      this.port = connectorBuilder.port;
    }

    /**
     * Sets the connector file context.
     *
     * @param connectorFileContext base directory for relative connector files, never
     *     <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public Builder setConnectorFileContext(Path connectorFileContext) {
      this.connectorFileContext = connectorFileContext;
      return this;
    }

    /**
     * Sets the connector definition source.
     *
     * @param source the source element, never <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public Builder setSource(Element source) {
      this.source = source;
      return this;
    }

    /**
     * Copies configuration from another connector.
     *
     * @param copyFrom the connector to copy from, never <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public Builder setCopyFrom(IPSConnector copyFrom) {
      this.copyFrom = copyFrom;

      return copyFrom.getScheme().equals(SCHEME_HTTPS) ? setHttps() : this;
    }

    /**
     * Sets the connector properties.
     *
     * @param props the properties to set, never <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public Builder setProps(Map<String, String> props) {
      this.props = props;
      return this;
    }

    /**
     * Sets the connector port.
     *
     * @param port the port to set
     * @return this builder, never <code>null</code>
     */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    /**
     * Switches this builder to HTTPS.
     *
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setHttps() {
      scheme = SCHEME_HTTPS;
      return (this instanceof HttpsBuilder hb) ? hb : new HttpsBuilder(this);
    }

    /**
     * Builds the connector from this builder's state.
     *
     * @return the built connector, never <code>null</code>
     */
    public PSAbstractConnector build() {

      PSAbstractConnector connector = null;

      if (connectorFileContext == null) {
        connectorFileContext = Paths.get(".");
      }

      if (this.source != null) {
        connector = new PSTomcatConnector(connectorFileContext, this.source, this.props);
      } else if (copyFrom != null)
        connector = new PSTomcatConnector(connectorFileContext, copyFrom, props);
      else connector = new PSTomcatConnector(connectorFileContext, scheme, port, props);

      return connector;
    }
  }

  /** Https builder. */
  public static class HttpsBuilder extends Builder {

  /** Field. */
    protected Path truststoreFile;
  /** Field. */
    protected String truststorePass;
  /** Field. */
    protected Set<String> ciphers;
  /** Field. */
    protected Set<String> sslProtocols;

  /** Field. */
    protected Path keystoreFile;
  /** Field. */
    protected String keystorePass;

    /**
     * Creates an empty HTTPS builder.
     */
    public HttpsBuilder() {
      super();
    }

    /**
     * Copy constructor.
     *
     * @param connectorBuilder the builder to copy, never <code>null</code>
     */
    public HttpsBuilder(Builder connectorBuilder) {
      super(connectorBuilder);
      // Direct field assign — avoid overridable setHttps() during construction (this-escape)
      this.scheme = PSAbstractConnector.SCHEME_HTTPS;
      this.connectorFileContext = connectorBuilder.connectorFileContext;
    }

    /**
     * Sets the keystore file.
     *
     * @param keystoreFile the keystore file to set, may be <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setKeystoreFile(Path keystoreFile) {
      this.keystoreFile = keystoreFile;
      return this;
    }

    /**
     * Sets the keystore password.
     *
     * @param keystorePass the keystore password to set, may be <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setKeystorePass(String keystorePass) {
      this.keystorePass = keystorePass;
      return this;
    }

    /**
     * Sets the truststore file.
     *
     * @param truststoreFile the truststore file to set, may be <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setTruststoreFile(Path truststoreFile) {
      this.truststoreFile = truststoreFile;
      return this;
    }

    /**
     * Sets the truststore password.
     *
     * @param truststorePass the truststore password to set, may be <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setTruststorePass(String truststorePass) {
      this.truststorePass = truststorePass;
      return this;
    }

    /**
     * Sets the cipher suites.
     *
     * @param ciphers the cipher suites to set, may be <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setCiphers(Set<String> ciphers) {
      this.ciphers = ciphers;
      return this;
    }

    /**
     * Sets the SSL protocols.
     *
     * @param sslProtocols the SSL protocols to set, may be <code>null</code>
     * @return this builder, never <code>null</code>
     */
    public HttpsBuilder setSslProtocols(Set<String> sslProtocols) {
      this.sslProtocols = sslProtocols;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public HttpsBuilder setHttps() {
      this.scheme = PSAbstractConnector.SCHEME_HTTPS;
      return this;
    }

    /**
     * Builds the connector from this builder's state.
     *
     * @return the built connector, never <code>null</code>
     */
    public PSAbstractConnector build() {

      PSAbstractConnector connector = super.build();
      if (truststoreFile != null) connector.setTruststoreFile(truststoreFile);
      if (truststorePass != null) connector.setTruststorePass(truststorePass);
      if (keystoreFile != null) connector.setKeystoreFile(keystoreFile);
      if (keystorePass != null) connector.setKeystorePass(keystorePass);
      if (ciphers != null) connector.setCiphers(ciphers);
      if (sslProtocols != null) connector.setSslProtocols(sslProtocols);

      connector.setScheme(PSAbstractConnector.SCHEME_HTTPS);

      return connector;
    }
  }

  /**
   * Creates a new connector builder.
   *
   * @return a new builder, never <code>null</code>
   */
  public static PSAbstractConnector.Builder getBuilder() {
    return new PSAbstractConnector.Builder();
  }
}
