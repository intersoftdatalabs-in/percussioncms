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
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code FederatedCredential} specialization that captures the identity provider, the
 * federated user name, and the group memberships. {@link #isValid()} requires all three pieces to
 * be non-empty.
 */
public class FederatedCredential extends Credential {

  private static final long serialVersionUID = 1L;

  /** The identity provider name, may be {@code null}. */
  private String identity_provider;

  /** The federated user name, may be {@code null}. */
  private String user;

  /** The group memberships, may be {@code null}. */
  private List<String> groups;

  /**
   * Constructs a federated credential with the supplied identity provider, user, and groups.
   *
   * @param type the credential type (e.g., {@code "token"}), may be {@code null}.
   * @param token the credential token forwarded to the supertype, never {@code null} or empty.
   * @param identity_provider the identity provider name, never {@code null} or empty.
   * @param user the federated user name, never {@code null} or empty.
   * @param groups the group memberships, never {@code null} or empty.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public FederatedCredential(
      String type, String token, String identity_provider, String user, List<String> groups)
      throws CADFException {
    super(token);
    this.identity_provider = identity_provider;
    this.user = user;
    this.groups = groups;
  }

  /**
   * Returns the identity provider name.
   *
   * @return the identity provider, may be {@code null}.
   */
  public String getIdentity_provider() {
    return identity_provider;
  }

  /**
   * Sets the identity provider name.
   *
   * @param identity_provider the identity provider, may be {@code null}.
   */
  public void setIdentity_provider(String identity_provider) {
    this.identity_provider = identity_provider;
  }

  /**
   * Returns the federated user name.
   *
   * @return the user, may be {@code null}.
   */
  public String getUser() {
    return user;
  }

  /**
   * Sets the federated user name.
   *
   * @param user the user, may be {@code null}.
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Returns the group memberships.
   *
   * @return the groups, may be {@code null}.
   */
  public List<String> getGroups() {
    return groups;
  }

  /**
   * Sets the group memberships.
   *
   * @param groups the groups, may be {@code null}.
   */
  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  /**
   * Validates that {@code identity_provider}, {@code user}, and {@code groups} are all non-empty.
   *
   * @return always {@code true} when validation passes.
   * @throws CADFException listing any missing mandatory fields.
   */
  @Override
  public boolean isValid() throws CADFException {
    boolean missingMandatoryField = false;
    StringBuilder missingMadatoryFields = new StringBuilder();
    if (StringUtils.isEmpty(identity_provider)) {
      missingMandatoryField = true;
      missingMadatoryFields.append("identity_provider");
    }

    if (StringUtils.isEmpty(user)) {
      if (missingMandatoryField) {
        missingMadatoryFields.append(",");
      } else {
        missingMandatoryField = true;
      }
      missingMadatoryFields.append("user");
    }

    if (CollectionUtils.isEmpty(groups)) {
      if (missingMandatoryField) {
        missingMadatoryFields.append(",");
      } else {
        missingMandatoryField = true;
      }
      missingMadatoryFields.append("groups");
    }

    // Validation to ensure FederatedCredential required attributes are set.
    if (!missingMandatoryField) return true;
    else
      throw new CADFException(
          MessageFormat.format(
              Messages.MISSING_MANDATORY_FIELDS, missingMadatoryFields.toString()));
  }
}
