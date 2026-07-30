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
 * CADF {@code Credential} reference attached to a {@link Resource}. Holds a free-form credential
 * {@code type} identifier and a non-empty {@code token}; {@link #isValid()} enforces the non-empty
 * token constraint.
 */
public class Credential extends CADFType {

  private static final long serialVersionUID = 1L;

  /** The credential type identifier, may be {@code null}. */
  private String type;

  /** The credential token, may be {@code null}. */
  private String token;

  /**
   * Constructs a credential wrapping the supplied token.
   *
   * @param token the credential token (e.g., a hashed password or API key), never {@code null} or
   *     empty.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Credential(String token) throws CADFException {
    super();
    this.token = token;
  }

  /**
   * Returns the credential type identifier.
   *
   * @return the type, may be {@code null}.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the credential type identifier.
   *
   * @param type the type, may be {@code null}.
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the credential token.
   *
   * @return the token, may be {@code null} when not yet set.
   */
  public String getToken() {
    return token;
  }

  /**
   * Sets the credential token.
   *
   * @param token the token, never {@code null} or empty.
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Validates that the {@code token} field is populated.
   *
   * @return always {@code true} when validation passes.
   * @throws CADFException when {@link #getToken()} is blank.
   */
  @Override
  public boolean isValid() throws CADFException {
    // Validation to ensure Credential required attributes are set.
    if (StringUtils.isNotEmpty(this.token)) return true;
    else throw new CADFException(MessageFormat.format(Messages.MISSING_MANDATORY_FIELDS, "token"));
  }
}
