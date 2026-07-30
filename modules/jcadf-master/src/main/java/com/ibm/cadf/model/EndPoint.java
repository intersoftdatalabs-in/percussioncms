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

import com.ibm.cadf.Messages;
import com.ibm.cadf.exception.CADFException;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code EndPoint} reference that locates a target resource by URL, name, and (optionally)
 * port. {@link #isValid()} enforces the non-empty URL constraint.
 */
public class EndPoint extends CADFType {

  private static final long serialVersionUID = 1L;

  /** The endpoint URL, may be {@code null}. */
  private String url;

  /** The human-readable endpoint name, may be {@code null}. */
  private String name;

  /** The port component of the endpoint URL, may be {@code null}. */
  private String port;

  /**
   * Constructs an endpoint with the supplied URL.
   *
   * @param url the endpoint URL, never {@code null} or empty.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public EndPoint(String url) throws CADFException {
    super();
    this.url = url;
  }

  /**
   * Returns the endpoint URL.
   *
   * @return the URL, may be {@code null} when not yet set.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the endpoint URL.
   *
   * @param url the URL, never {@code null} or empty.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Returns the human-readable endpoint name.
   *
   * @return the name, may be {@code null}.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the human-readable endpoint name.
   *
   * @param name the name, may be {@code null}.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the port component of the endpoint URL.
   *
   * @return the port, may be {@code null}.
   */
  public String getPort() {
    return port;
  }

  /**
   * Sets the port component of the endpoint URL.
   *
   * @param port the port, may be {@code null}.
   */
  public void setPort(String port) {
    this.port = port;
  }

  /**
   * Validates that the {@code url} field is populated.
   *
   * @return always {@code true} when validation passes.
   * @throws CADFException when {@link #getUrl()} is blank.
   */
  @Override
  public boolean isValid() throws CADFException {
    // Validation to ensure Endpoint required attributes are set.
    if (StringUtils.isNotEmpty(this.url)) return true;
    else throw new CADFException(MessageFormat.format(Messages.MISSING_MANDATORY_FIELDS, "url"));
  }
}
