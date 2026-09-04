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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import tools.jackson.databind.annotation.JsonDeserialize;

/** List of CommunityRole associations. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "CommunityRoleList")
@JsonRootName("CommunityRoleList")
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@XmlSeeAlso({CommunityRole.class})
@ArraySchema(
    schema =
        @Schema(
            implementation = CommunityRole.class,
            description = "A List of CommunityRole associations"))
@JsonDeserialize(using = CommunityRoleListDeserializer.class)
public class CommunityRoleList extends ArrayList<CommunityRole> {

  private static final long serialVersionUID = 1L;

  public CommunityRoleList(Collection<? extends CommunityRole> c) {
    super(c);
  }

  public CommunityRoleList() {}

  @Override
  public boolean equals(Object o) {
    return o instanceof CommunityRoleList && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    return "CommunityRoleList" + super.toString();
  }
}
