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

// REFACTORED: CP-JAVA11
package com.percussion.integrations.siteimprove.data;

import java.util.Objects;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Siteimprove credentials to access their API. */
@XmlRootElement(name = "SiteimproveCredentials")
public class PSSiteImproveCredentials {

  private String siteName;
  private String token;
  private String siteProtocol;
  private String defaultDocument;
  private String canonicalDist;

  /** Empty constructor for JAX-RS to use. */
  public PSSiteImproveCredentials() {
    // Default constructor
  }

  /**
   * @param token the token generated from the Siteimprove GET token endpoint for the site
   * @param siteName the name of the site to associate credentials with
   */
  public PSSiteImproveCredentials(String token, String siteName) {
    this.siteName = siteName;
    this.token = token;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public Optional<String> getSiteProtocol() {
    return Optional.ofNullable(siteProtocol);
  }

  public void setSiteProtocol(String protocol) {
    this.siteProtocol = protocol;
  }

  public Optional<String> getDefaultDocument() {
    return Optional.ofNullable(defaultDocument);
  }

  public void setDefaultDocument(String defaultDocument) {
    this.defaultDocument = defaultDocument;
  }

  public Optional<String> getCanonicalDist() {
    return Optional.ofNullable(canonicalDist);
  }

  public void setCanonicalDist(String canonicalDist) {
    this.canonicalDist = canonicalDist;
  }

  @Override
  public String toString() {
    return "PSSiteImproveCredentials{"
        + "siteName='"
        + siteName
        + '\''
        + ", token='"
        + (token != null ? "***" : null)
        + '\''
        + ", siteProtocol='"
        + siteProtocol
        + '\''
        + ", defaultDocument='"
        + defaultDocument
        + '\''
        + ", canonicalDist='"
        + canonicalDist
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSSiteImproveCredentials)) return false;
    var that = (PSSiteImproveCredentials) o;
    return Objects.equals(siteName, that.siteName)
        && Objects.equals(token, that.token)
        && Objects.equals(siteProtocol, that.siteProtocol)
        && Objects.equals(defaultDocument, that.defaultDocument)
        && Objects.equals(canonicalDist, that.canonicalDist);
  }

  @Override
  public int hashCode() {
    return Objects.hash(siteName, token, siteProtocol, defaultDocument, canonicalDist);
  }
}
