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

  /** Member. */
  public Map<String, String> getProperties() {
    return properties;
  }

  /** Member. */
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

  /** Member. */
  public PSAbstractConnector(Path connectorFileContext) {
    this.connectorFileContext = connectorFileContext;
  }

  /** Member. */
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

  @Override
  /** Member. */
  public String getScheme() {
    return scheme;
  }

  @Override
  /** Member. */
  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  @Override
  /** Member. */
  public String getHostAddress() {
    return getHostAddress;
  }

  @Override
  /** Member. */
  public void setHostAddress(String getHostAddress) {
    this.getHostAddress = getHostAddress;
  }

  @Override
  /** Member. */
  public Path getKeystoreFile() {
    return keystoreFile;
  }

  @Override
  /** Member. */
  public void setKeystoreFile(Path keystoreFile) {
    this.keystoreFile = keystoreFile;
  }

  @Override
  /** Member. */
  public String getKeystorePass() {
    return keystorePass;
  }

  @Override
  /** Member. */
  public void setKeystorePass(String keystorePass) {
    this.keystorePass = keystorePass;
  }

  @Override
  /** Member. */
  public Path getTruststoreFile() {
    return truststoreFile;
  }

  @Override
  /** Member. */
  public void setTruststoreFile(Path truststoreFile) {
    this.truststoreFile = truststoreFile;
  }

  @Override
  /** Member. */
  public String getTruststorePass() {
    return truststorePass;
  }

  @Override
  /** Member. */
  public void setTruststorePass(String truststorePass) {
    this.truststorePass = truststorePass;
  }

  @Override
  /** Member. */
  public int getPort() {
    return port;
  }

  @Override
  /** Member. */
  public void setPort(int port) {
    this.port = port;
  }

  @Override
  /** Member. */
  public Set<String> getCiphers() {
    return ciphers;
  }

  @Override
  /** Member. */
  public void setCiphers(Set<String> ciphers) {
    this.ciphers = ciphers;
  }

  @Override
  /** Member. */
  public Set<String> getSslProtocols() {
    return sslProtocols;
  }

  @Override
  /** Member. */
  public void setSslProtocols(Set<String> protocols) {
    this.sslProtocols = protocols;
  }

  @Override
  /** Member. */
  public String getProtocol() {
    return protocol;
  }

  @Override
  /** Member. */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /** Member. */
  public String toCommaString(Collection<String> string) {
    return string.stream().collect(Collectors.joining(","));
  }

  @Override
  /** Member. */
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

  /** Member. */
  public Path getConnectorFileContext() {
    return connectorFileContext;
  }

  /** Field. */
  protected Function<String, Path> getAbsolutePath() {
    return p -> getConnectorFileContext().resolve(p).normalize().toAbsolutePath();
  }

  /** Field. */
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

  /** Member. */
    public Builder(Builder connectorBuilder) {
      this.connectorFileContext = connectorBuilder.connectorFileContext;
      this.source = connectorBuilder.source;
      this.copyFrom = connectorBuilder.copyFrom;
      this.props = connectorBuilder.props;
      this.port = connectorBuilder.port;
    }

  /** Member. */
    public Builder setConnectorFileContext(Path connectorFileContext) {
      this.connectorFileContext = connectorFileContext;
      return this;
    }

  /** Member. */
    public Builder setSource(Element source) {
      this.source = source;
      return this;
    }

  /** Member. */
    public Builder setCopyFrom(IPSConnector copyFrom) {
      this.copyFrom = copyFrom;

      return copyFrom.getScheme().equals(SCHEME_HTTPS) ? setHttps() : this;
    }

  /** Member. */
    public Builder setProps(Map<String, String> props) {
      this.props = props;
      return this;
    }

  /** Member. */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

  /** Member. */
    public HttpsBuilder setHttps() {
      scheme = SCHEME_HTTPS;
      return (this instanceof HttpsBuilder hb) ? hb : new HttpsBuilder(this);
    }

  /** Member. */
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

  /** Member. */
    public HttpsBuilder() {
      super();
    }

  /** Member. */
    public HttpsBuilder(Builder connectorBuilder) {
      super(connectorBuilder);
      // Direct field assign — avoid overridable setHttps() during construction (this-escape)
      this.scheme = PSAbstractConnector.SCHEME_HTTPS;
      this.connectorFileContext = connectorBuilder.connectorFileContext;
    }

  /** Member. */
    public HttpsBuilder setKeystoreFile(Path keystoreFile) {
      this.keystoreFile = keystoreFile;
      return this;
    }

  /** Member. */
    public HttpsBuilder setKeystorePass(String keystorePass) {
      this.keystorePass = keystorePass;
      return this;
    }

  /** Member. */
    public HttpsBuilder setTruststoreFile(Path truststoreFile) {
      this.truststoreFile = truststoreFile;
      return this;
    }

  /** Member. */
    public HttpsBuilder setTruststorePass(String truststorePass) {
      this.truststorePass = truststorePass;
      return this;
    }

  /** Member. */
    public HttpsBuilder setCiphers(Set<String> ciphers) {
      this.ciphers = ciphers;
      return this;
    }

  /** Member. */
    public HttpsBuilder setSslProtocols(Set<String> sslProtocols) {
      this.sslProtocols = sslProtocols;
      return this;
    }

    @Override
  /** Member. */
    public HttpsBuilder setHttps() {
      this.scheme = PSAbstractConnector.SCHEME_HTTPS;
      return this;
    }

  /** Member. */
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

  /** Member. */
  public static PSAbstractConnector.Builder getBuilder() {
    return new PSAbstractConnector.Builder();
  }
}
