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
package com.percussion.user.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractNamedObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import net.sf.oval.configuration.annotation.IsInvariant;
import net.sf.oval.constraint.Length;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.ValidateWithMethod;

/**
 * Represents a user in the system and their associated roles.
 *
 * @author DavidBenua
 * @author adamgent
 */
@XmlRootElement(name = "User")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("User")
public class PSUser extends PSAbstractNamedObject {

  private static final long serialVersionUID = 1L;
  private static final String INVALID_CHAR_ERROR_MSG =
      "The username should be 4-20 characters long. OR "
          + "The user name contains an invalid character. "
          + "The valid characters of a user name are 'a' to 'z', 'A' to 'Z' and '0' to '9'.";
  private static final String NAME_LENGTH_ERROR_MSG =
      "The maximum length of a user name is 50 characters.";

  /** Package-private for same-package {@link PSCurrentUser} constructor seeding. */
  String password;

  String email = "";
  PSUserProviderType providerType = PSUserProviderType.INTERNAL;
  private boolean isCreateUser;

  /** A user has to be in at least one role. */
  @NotEmpty @NotNull ArrayList<String> roles;

  public PSUser() {
    roles = new ArrayList<>();
  }

  public boolean isCreateUser() {
    return isCreateUser;
  }

  public void setCreateUser(boolean createUser) {
    isCreateUser = createUser;
  }

  @Override
  @IsInvariant
  @NotNull
  @NotBlank
  @Length(max = 50, message = NAME_LENGTH_ERROR_MSG)
  @ValidateWithMethod(
      methodName = "isValidName",
      parameterType = String.class,
      message = INVALID_CHAR_ERROR_MSG)
  public String getName() {
    return super.getName();
  }

  @Override
  protected boolean isValidName(String name) {
    if (getProviderType() != PSUserProviderType.INTERNAL) return true;
    if (isCreateUser()) {
      return name.matches("^[a-zA-Z0-9]([._-](?![._-])|[a-zA-Z0-9]){2,18}[a-zA-Z0-9]$")
          && super.isValidName(name);
    } else {
      return super.isValidName(name);
    }
  }

  /** Gets the password. */
  public String getPassword() {
    return password;
  }

  /** Sets the password. */
  public final void setPassword(String password) {
    this.password = password;
  }

  /** Gets the email. Never {@code null}, might be empty. */
  @NotNull
  public String getEmail() {
    return email;
  }

  /** Sets the email. */
  public final void setEmail(String email) {
    this.email = email;
  }

  /** Gets the roles. */
  public List<String> getRoles() {
    return roles;
  }

  /** Sets the roles. */
  @SuppressWarnings("unchecked")
  public final void setRoles(List<String> roles) {
    if (roles == null) {
      this.roles = null;
    } else if (roles instanceof ArrayList) {
      this.roles = (ArrayList<String>) roles;
    } else {
      this.roles = new ArrayList<>(roles);
    }
  }

  /**
   * Where the authentication is done for this user. If not set, the default {@link
   * PSUserProviderType#INTERNAL} will be returned.
   *
   * @return never {@code null}.
   */
  public PSUserProviderType getProviderType() {
    return providerType;
  }

  public final void setProviderType(PSUserProviderType providerType) {
    this.providerType = providerType;
  }

  @Override
  public PSUser clone() throws CloneNotSupportedException {
    var user = (PSUser) super.clone();
    if (this.getRoles() != null) {
      user.setRoles(new ArrayList<>(this.getRoles()));
    }
    return user;
  }
}
