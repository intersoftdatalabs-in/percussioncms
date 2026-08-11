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

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of roles.
 *
 * <p>Some tools have problems serializing a list of strings, hence this wrapping object.
 *
 * @author adamgent
 * @author DavidBenua
 */
@XmlRootElement(name = "RoleList")
@JsonRootName("RoleList")
public class PSRoleList extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;
  private ArrayList<String> roles;

  public PSRoleList() {
    roles = new ArrayList<>();
  }

  /** Gets the roles. */
  public List<String> getRoles() {
    return roles;
  }

  /** Sets the roles. */
  @SuppressWarnings("unchecked")
  public void setRoles(List<String> roles) {
    if (roles == null) {
      this.roles = null;
    } else if (roles instanceof ArrayList) {
      this.roles = (ArrayList<String>) roles;
    } else {
      this.roles = new ArrayList<>(roles);
    }
  }
}
