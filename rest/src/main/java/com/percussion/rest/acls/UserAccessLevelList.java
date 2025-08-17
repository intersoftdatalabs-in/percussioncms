// REFACTORED: CP-JAVA11
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

package com.percussion.rest.acls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Permissions;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/** List of UserAccessLevel objects. */
@XmlRootElement(name = "AclEntryList")
@XmlSeeAlso({UserAccessLevel.class, Permissions.class})
@ArraySchema(schema = @Schema(implementation = UserAccessLevel.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAccessLevelList extends ArrayList<UserAccessLevel> {

  public UserAccessLevelList() {
    super();
  }

  public UserAccessLevelList(Collection<? extends UserAccessLevel> c) {
    super(c);
  }

  @Override
  public String toString() {
    return "UserAccessLevelList" + super.toString();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof UserAccessLevelList && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }
}
