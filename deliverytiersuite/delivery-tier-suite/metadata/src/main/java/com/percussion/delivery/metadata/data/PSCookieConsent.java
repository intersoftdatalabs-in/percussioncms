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

package com.percussion.delivery.metadata.data;

import com.percussion.delivery.metadata.IPSCookieConsent;
import java.time.Instant;

/**
 * Provides model for cookie consent entries. Statistics include:
 *
 * <ul>
 *   <li>siteName
 *   <li>IP
 *   <li>serviceName
 *   <li>optIn
 *   <li>consentDate
 * </ul>
 *
 * @author chriswright
 */
public class PSCookieConsent implements IPSCookieConsent {

  private String siteName;
  private String ip;
  private String serviceName;
  private boolean optIn;
  private Instant consentDate;

  /** No-arg constructor required by the JSON binding layer. */
  public PSCookieConsent() {}

  /**
   * Constructs a fully populated cookie-consent entry.
   *
   * @param siteName the site the consent was captured for; may not be {@code null}.
   * @param serviceName the service / cookie name the consent applies to; may not be {@code null}.
   * @param consentDate the instant the consent was captured; may not be {@code null}.
   * @param ip the originating client IP; may not be {@code null}.
   * @param optIn {@code true} if the client opted in, {@code false} otherwise.
   */
  public PSCookieConsent(
      String siteName, String serviceName, Instant consentDate, String ip, boolean optIn) {

    if (siteName == null) throw new IllegalArgumentException("siteName may not be null");
    if (serviceName == null) throw new IllegalArgumentException("serviceName may not be null");
    if (consentDate == null) throw new IllegalArgumentException("consentDate may not be null");
    if (ip == null) throw new IllegalArgumentException("ip may not be null");

    // Direct field assignment avoids this-escape from overridable setters (subclassed by
    // PSCookieConsentQuery). Instant is immutable so no defensive copy is required.
    this.siteName = siteName;
    this.serviceName = serviceName;
    this.consentDate = consentDate;
    this.ip = ip;
    this.optIn = optIn;
  }

  @Override
  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  @Override
  public String getSiteName() {
    return siteName;
  }

  @Override
  public void setIP(String ip) {
    this.ip = ip;
  }

  @Override
  public String getIP() {
    return ip;
  }

  @Override
  public void setConsentDate(Instant consentDate) {
    this.consentDate = consentDate;
  }

  @Override
  public Instant getConsentDate() {
    return consentDate;
  }

  @Override
  public void setService(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public String getService() {
    return serviceName;
  }

  @Override
  public void setOptIn(boolean optIn) {
    this.optIn = optIn;
  }

  @Override
  public boolean getOptIn() {
    return optIn;
  }
}
