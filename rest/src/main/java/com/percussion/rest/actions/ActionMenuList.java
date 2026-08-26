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

package com.percussion.rest.actions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * List of ActionMenu objects.
 *
 * <p>{@link JsonFormat.Shape#ARRAY} keeps Jackson from treating this {@code ArrayList}
 * subclass as a bean ({@code {"empty":false}}) when it is nested as {@code
 * ActionMenu.children}. A bean-shaped children field would drop the cascade and the
 * Explorer toolbar would dump MENUITEMs as flat buttons (#3379 / #2730).
 *
 * <p>{@link JsonRootName} matches {@link com.percussion.rest.sites.SiteList} so
 * {@code JacksonContextResolver} WRAP_ROOT_VALUE emits {@code {"ActionMenuList":
 * [...]}} for top-level {@code /actions/find/types} and {@code
 * /actions/find/templates/{id}}. Nested {@code children} stay a JSON array
 * because WRAP_ROOT_VALUE only wraps the document root.
 *
 * <p>{@link XmlSeeAlso} registers {@link ActionMenu} in the JAXB context so
 * those list endpoints can marshal. Without it, CXF JAXB JSON fails with
 * {@code ActionMenu nor any of its super class is known to this context} (HTTP
 * 500, #3855). Peer: {@link com.percussion.rest.sites.SiteList}.
 */
@XmlRootElement(name = "ActionMenuList")
@JsonRootName("ActionMenuList")
@XmlSeeAlso(ActionMenu.class)
@ArraySchema(schema = @Schema(implementation = ActionMenu.class))
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class ActionMenuList extends ArrayList<ActionMenu> {
  /** Safe to serialize. */
  private static final long serialVersionUID = 1L;

  /** No-op constructor. */
  public ActionMenuList() {
    super();
  }

  /**
   * Creates a new list populated with the supplied collection.
   *
   * @param c the source collection
   */
  public ActionMenuList(Collection<? extends ActionMenu> c) {
    super(c != null ? c : Collections.emptyList());
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ActionMenuList && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    return "ActionMenuList" + super.toString();
  }
}
