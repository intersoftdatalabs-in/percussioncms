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
package com.percussion.rest.acls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/** List of {@link Acl} objects. */
@XmlRootElement(name = "AclList")
@JsonRootName("AclList")
@XmlSeeAlso(Acl.class)
@ArraySchema(schema = @Schema(implementation = Acl.class))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AclList extends ArrayList<Acl> {
  /** Safe to serialize. */
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new list populated with the supplied collection.
   *
   * @param c the source collection
   */
  public AclList(Collection<? extends Acl> c) {
    super(c);
  }

  /** No-op constructor. */
  public AclList() {
    super();
  }

  @Override
  public String toString() {
    return "AclList" + super.toString();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AclList && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }
}
