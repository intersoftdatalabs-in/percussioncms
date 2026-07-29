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

/**
 * CADF {@code Host} reference attached to a {@link Resource}. Captures the host id, network
 * address, user-agent, and platform string. {@link #isValid()} currently accepts any state; a
 * future schema-driven validation is the noted TODO.
 */
public class Host extends CADFType {
  private static final long serialVersionUID = 1L;

  /** Host id, may be {@code null}. */
  private String id;

  /** Host network address (typically an IP), may be {@code null}. */
  private String address;

  /** User-agent string reported by the host, may be {@code null}. */
  private String agent;

  /** Platform identifier reported by the host, may be {@code null}. */
  private String platform;

  /** Default no-argument constructor for {@link Host}. */
  public Host() throws CADFException {
    super();
  }

  /**
   * Returns the host id.
   *
   * @return the id, may be {@code null}.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the host id.
   *
   * @param id the id, may be {@code null}.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the network address of the host (typically an IP address).
   *
   * @return the address, may be {@code null}.
   */
  public String getAddress() {
    return address;
  }

  /**
   * Sets the network address of the host.
   *
   * @param address the address, may be {@code null}.
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * Returns the user-agent string the host reported.
   *
   * @return the agent, may be {@code null}.
   */
  public String getAgent() {
    return agent;
  }

  /**
   * Sets the user-agent string the host reported.
   *
   * @param agent the agent, may be {@code null}.
   */
  public void setAgent(String agent) {
    this.agent = agent;
  }

  /**
   * Returns the platform identifier reported by the host.
   *
   * @return the platform, may be {@code null}.
   */
  public String getPlatform() {
    return platform;
  }

  /**
   * Sets the platform identifier reported by the host.
   *
   * @param platform the platform, may be {@code null}.
   */
  public void setPlatform(String platform) {
    this.platform = platform;
  }

  // TODO: validate this cadf:Host type against schema
  /**
   * Validates the host state. Always returns {@code true}; see the noted TODO for schema-driven
   * validation.
   *
   * @return always {@code true}.
   */
  @Override
  public boolean isValid() {
    return true;
  }
}
