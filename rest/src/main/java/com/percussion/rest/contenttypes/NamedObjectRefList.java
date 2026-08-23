/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.contenttypes;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/** Wire list of {@link NamedObjectRef} (content-type allowed templates/workflows). */
@XmlRootElement(name = "NamedObjectRef")
@ArraySchema(schema = @Schema(implementation = NamedObjectRef.class))
public class NamedObjectRefList extends ArrayList<NamedObjectRef> {

  private static final long serialVersionUID = 1L;

  public NamedObjectRefList() {}

  public NamedObjectRefList(Collection<? extends NamedObjectRef> c) {
    super(c);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof NamedObjectRefList && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    return "NamedObjectRefList" + super.toString();
  }
}
