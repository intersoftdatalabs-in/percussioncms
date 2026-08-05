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

package com.percussion.rest.displayformat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/** Represents a list of allowed communities for a display format. */
public class AllowedCommunityList extends ArrayList<String> {

  /** Safe to serialize. */
  private static final long serialVersionUID = 1L;

  /** No-op constructor. */
  public AllowedCommunityList() {
    super();
  }

  /**
   * Creates a list populated with the supplied collection.
   *
   * @param c the source collection
   */
  public AllowedCommunityList(Collection<? extends String> c) {
    super(c);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AllowedCommunityList && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    return "AllowedCommunityList" + super.toString();
  }
}
