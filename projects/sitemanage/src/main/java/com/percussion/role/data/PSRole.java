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
package com.percussion.role.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractNamedObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import net.sf.oval.configuration.annotation.IsInvariant;
import net.sf.oval.constraint.Length;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.ValidateWithMethod;

/** A role in the system and the users which are members of it. */
@XmlRootElement(name = "Role")
@JsonRootName("Role")
public class PSRole extends PSAbstractNamedObject {

  private static final long serialVersionUID = 1L;

  private static final String INVALID_CHAR_ERROR_MSG = "invalid_character";
  private static final String DESCR_LENGTH_ERROR_MSG =
      "The maximum length of a role description is 255 characters.";
  private static final String NAME_LENGTH_ERROR_MSG =
      "The maximum length of a role name is 50 characters.";

  private String oldName;
  private String description;
  private String homepage;

  @NotNull private ArrayList<String> users = new ArrayList<>();

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

  @IsInvariant
  @Length(max = 255, message = DESCR_LENGTH_ERROR_MSG)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public List<String> getUsers() {
    // Defensive copy for immutability
    return users == null ? List.of() : Collections.unmodifiableList(users);
  }

  @SuppressWarnings("unchecked")
  public void setUsers(List<String> users) {
    if (users == null) {
      this.users = new ArrayList<>();
    } else if (users instanceof ArrayList) {
      this.users = (ArrayList<String>) users;
    } else {
      this.users = new ArrayList<>(users);
    }
  }

  @Override
  protected boolean isValidName(String name) {
    var regex = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!\\s]");
    return !regex.matcher(name).find() && super.isValidName(name);
  }

  /**
   * Validates a role description.
   *
   * @param description the description to validate
   * @return true if the description is null or no longer than 255 characters, false otherwise.
   */
  protected boolean isValidDescription(String description) {
    return description == null || description.length() <= 255;
  }

  public String getOldName() {
    return oldName;
  }

  public void setOldName(String oldName) {
    this.oldName = oldName;
  }

  @Override
  public PSRole clone() throws CloneNotSupportedException {
    var role = (PSRole) super.clone();
    role.setDescription(this.getDescription());
    role.setHomepage(this.getHomepage());
    role.setOldName(this.getOldName());
    role.setUsers(this.getUsers());
    return role;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSRole)) return false;
    if (!super.equals(o)) return false;
    var psRole = (PSRole) o;
    return Objects.equals(oldName, psRole.oldName)
        && Objects.equals(description, psRole.description)
        && Objects.equals(homepage, psRole.homepage)
        && Objects.equals(users, psRole.users);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), oldName, description, homepage, users);
  }
}
