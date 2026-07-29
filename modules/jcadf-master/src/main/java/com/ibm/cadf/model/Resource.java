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

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.util.Constants;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code Resource} — the central CADF resource reference. Carries the resource id, type URI,
 * display name and domain, plus optional credential, host, geolocation, endpoint addresses, and
 * attachments. {@link #isValid()} requires a non-empty id and either a non-empty typeURI or one of
 * the canonical role ids {@link Constants#TARGET} / {@link Constants#INITIATOR}.
 */
public class Resource extends CADFType {

  private static final long serialVersionUID = 1L;

  /** The resource id, may be {@code null}. */
  private String id;

  /** The CADF type URI, may be {@code null}. */
  private String typeURI;

  /** The human-readable resource name, may be {@code null}. */
  private String name;

  /** The domain, may be {@code null}. */
  private String domain;

  /** The credential, may be {@code null}. */
  private Credential credential;

  /** The host, may be {@code null}. */
  private Host host;

  /** The reference id, may be {@code null}. */
  private String ref;

  /** The geolocation, may be {@code null}. */
  private Geolocation geolocation;

  /** The alternate geolocation id, may be {@code null}. */
  private String geolocationId;

  /** The {@link EndPoint} addresses, may be {@code null}. */
  private List<EndPoint> addresses;

  /** The {@link Attachment}s, may be {@code null}. */
  private List<Attachment> attachments;

  /** Default no-argument constructor for {@link Resource}. */
  public Resource() {}

  /**
   * Constructs a resource with the supplied id.
   *
   * @param id the resource id, never {@code null} or empty.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Resource(String id) throws CADFException {
    super();
    this.id = id;
  }

  /**
   * Returns the resource id.
   *
   * @return the id, may be {@code null} when not yet set.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the resource id.
   *
   * @param id the id, may be {@code null}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the CADF type URI of the resource.
   *
   * @return the type URI, may be {@code null}.
   */
  public String getTypeURI() {
    return typeURI;
  }

  /**
   * Sets the CADF type URI of the resource.
   *
   * @param typeURI the type URI, may be {@code null}.
   */
  public void setTypeURI(String typeURI) {
    this.typeURI = typeURI;
  }

  /**
   * Returns the human-readable resource name.
   *
   * @return the name, may be {@code null}.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the human-readable resource name.
   *
   * @param name the name, may be {@code null}.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the domain associated with this resource.
   *
   * @return the domain, may be {@code null}.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Sets the domain associated with this resource.
   *
   * @param domain the domain, may be {@code null}.
   */
  public void setDomain(String domain) {
    this.domain = domain;
  }

  /**
   * Returns the credential attached to this resource.
   *
   * @return the credential, may be {@code null}.
   */
  public Credential getCredential() {
    return credential;
  }

  /**
   * Sets the credential attached to this resource.
   *
   * @param credential the credential, may be {@code null}.
   */
  public void setCredential(Credential credential) {
    this.credential = credential;
  }

  /**
   * Returns the host attached to this resource.
   *
   * @return the host, may be {@code null}.
   */
  public Host getHost() {
    return host;
  }

  /**
   * Sets the host attached to this resource.
   *
   * @param host the host, may be {@code null}.
   */
  public void setHost(Host host) {
    this.host = host;
  }

  /**
   * Returns the reference identifier for this resource (when it is a forward reference to another
   * resource).
   *
   * @return the ref, may be {@code null}.
   */
  public String getRef() {
    return ref;
  }

  /**
   * Sets the reference identifier for this resource.
   *
   * @param ref the ref, may be {@code null}.
   */
  public void setRef(String ref) {
    this.ref = ref;
  }

  /**
   * Returns the geolocation attached to this resource.
   *
   * @return the geolocation, may be {@code null}.
   */
  public Geolocation getGeolocation() {
    return geolocation;
  }

  /**
   * Sets the geolocation attached to this resource.
   *
   * @param geolocation the geolocation, may be {@code null}.
   */
  public void setGeolocation(Geolocation geolocation) {
    this.geolocation = geolocation;
  }

  /**
   * Returns the alternate geolocation id.
   *
   * @return the geolocation id, may be {@code null}.
   */
  public String getGeolocationId() {
    return geolocationId;
  }

  /**
   * Sets the alternate geolocation id.
   *
   * @param geolocationId the id, may be {@code null}.
   */
  public void setGeolocationId(String geolocationId) {
    this.geolocationId = geolocationId;
  }

  /**
   * Appends an {@link EndPoint} address to the resource, creating the address list on first use.
   *
   * @param endpoint the endpoint to add, never {@code null}.
   */
  public void addAddress(EndPoint endpoint) {
    if (addresses == null) {
      addresses = new ArrayList<>();
    }
    addresses.add(endpoint);
  }

  /**
   * Appends an {@link Attachment} to the resource, creating the attachments list on first use.
   *
   * @param attachment the attachment to add, never {@code null}.
   */
  public void addAttachment(Attachment attachment) {
    if (attachments == null) {
      attachments = new ArrayList<>();
    }
    attachments.add(attachment);
  }

  /**
   * Returns the {@link EndPoint} addresses attached to this resource.
   *
   * @return the addresses, may be {@code null} or empty when none have been added.
   */
  public List<EndPoint> getAddresses() {
    return addresses;
  }

  /**
   * Returns the {@link Attachment}s attached to this resource.
   *
   * @return the attachments, may be {@code null} or empty when none have been added.
   */
  public List<Attachment> getAttachments() {
    return attachments;
  }

  /**
   * Validates that the resource id is set and either the typeURI is populated or the id matches one
   * of the canonical role ids ({@link Constants#TARGET} / {@link Constants#INITIATOR}).
   *
   * @return {@code true} when either condition holds.
   */
  @Override
  public boolean isValid() {
    return (StringUtils.isNotEmpty(id)
        && (StringUtils.isNotEmpty(typeURI)
            || (id.equals(Constants.TARGET) || id.equals(Constants.INITIATOR))));
  }
}
