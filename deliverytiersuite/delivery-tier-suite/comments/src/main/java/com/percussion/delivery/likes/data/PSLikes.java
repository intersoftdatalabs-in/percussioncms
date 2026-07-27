// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.likes.data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for a list of likes, used for serialization.
 *
 * @author davidpardini
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"likes"})
@XmlRootElement(name = "likes")
public class PSLikes {

  private List<IPSLikes> likes;

  /** Default no-arg constructor required by JAXB. */
  public PSLikes() {
    // Default constructor
  }

  /**
   * Creates a new likes container with the supplied list.
   *
   * @param likes the initial list of likes, may be {@code null}.
   */
  public PSLikes(List<IPSLikes> likes) {
    this.likes = likes;
  }

  /**
   * Gets the wrapped list of likes. Lazily initializes an empty list if needed.
   *
   * @return the list of likes, never {@code null}.
   */
  public List<IPSLikes> getLikes() {
    if (likes == null) {
      likes = new ArrayList<>();
    }
    return likes;
  }
}
