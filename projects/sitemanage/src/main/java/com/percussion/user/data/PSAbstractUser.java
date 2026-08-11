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

import com.percussion.share.data.PSAbstractDataObject;
import java.text.Collator;

/**
 * Abstract representation of a user.
 *
 * <p>Implements Comparable for sorting by user name.
 *
 * @author adamgent
 */
public abstract class PSAbstractUser extends PSAbstractDataObject
    implements Comparable<PSAbstractUser> {

  private static final long serialVersionUID = 1L;
  protected String name;

  /**
   * Gets the user name that uniquely identifies the user.
   *
   * @return should not be {@code null} or empty unless the object is not finished being processed.
   */
  public String getName() {
    return name;
  }

  public final void setName(String name) {
    this.name = name;
  }

  @Override
  public int compareTo(PSAbstractUser o) {
    // Java 11: Use Collator for locale-sensitive comparison
    return Collator.getInstance().compare(this.getName(), o.getName());
  }
}
