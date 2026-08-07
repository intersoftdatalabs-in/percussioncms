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

package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSCookieConsent;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Hibernate-managed entity backing a single cookie-consent entry as recorded by the DTS metadata
 * micro-service.
 *
 * <p>{@code consentDate} is an {@link Instant} so Hibernate 7 maps the TIMESTAMP column without
 * deprecated {@code @Temporal}.
 *
 * @author chriswright
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSCookieConsent")
@Table(name = "PERC_COOKIE_CONSENT")
public final class PSDbCookieConsent implements IPSCookieConsent {

  /** Surrogate primary key for this consent entry. */
  @Id
  @GeneratedValue
  @Column(name = "CONSENT_ID")
  private long consentId;

  /** Originating client IP that submitted the consent. */
  @Basic
  @Column(length = 100, name = "IP_ADDRESS")
  private String ip;

  /** Service / cookie name the consent applies to. */
  @Basic
  @Column(length = 2000, name = "SERVICE_NAME")
  private String serviceName;

  /** Site the consent was captured for. */
  @Basic
  @Column(length = 255, name = "SITE_NAME")
  private String siteName;

  /** {@code true} when the client opted in to the cookie. */
  @Basic
  @Column(name = "OPT_IN")
  private boolean optIn;

  /** Instant the consent was captured. */
  @Basic
  @Column(name = "CONSENT_DATE")
  private Instant consentDate;

  /** No-arg constructor required by Hibernate. */
  public PSDbCookieConsent() {}

  /**
   * Constructs a fully-populated cookie-consent entity.
   *
   * @param siteName the site the consent was captured for; may not be {@code null}.
   * @param serviceName the service / cookie name the consent applies to; may not be {@code null}.
   * @param consentDate the instant the consent was captured; may not be {@code null}.
   * @param ip the originating client IP; may not be {@code null}.
   * @param optIn {@code true} if the client opted in, {@code false} otherwise.
   */
  public PSDbCookieConsent(
      String siteName, String serviceName, Instant consentDate, String ip, boolean optIn) {

    if (siteName == null) throw new IllegalArgumentException("siteName may not be null");
    if (serviceName == null) throw new IllegalArgumentException("serviceName may not be null");
    if (consentDate == null) throw new IllegalArgumentException("consentDate may not be null");
    if (ip == null) throw new IllegalArgumentException("ip may not be null");

    // Direct field assignment; class is final (no this-escape via overridable setters).
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (consentId ^ (consentId >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PSDbCookieConsent other = (PSDbCookieConsent) obj;
    if (consentId != other.consentId) return false;
    return true;
  }
}
