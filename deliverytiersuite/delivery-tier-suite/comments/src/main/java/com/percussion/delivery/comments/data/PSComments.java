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
package com.percussion.delivery.comments.data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple container. Its use is just to add a root element name for Jersey to spit out when
 * serializing to JSON.
 *
 * @author erikserating
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"comments"})
@XmlRootElement(name = "comments")
public class PSComments {
  private List<IPSComment> comments;

  /** Default no-arg constructor required by JAXB. Initializes the comments list to empty. */
  public PSComments() {
    comments = new ArrayList<>();
  }

  /**
   * Constructor for the {@link PSComments} object.
   *
   * @param comments. Never <code>null</code>.
   */
  public PSComments(List<IPSComment> comments) {
    if (comments == null) {
      this.comments = new ArrayList<>();
    } else {
      this.comments = comments;
    }
  }

  /**
   * Gets the list of comments held by this container.
   *
   * @return the list of comments. Never <code>null</code>.
   */
  public List<IPSComment> getComments() {
    return comments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PSComments that = (PSComments) o;
    return Objects.equals(comments, that.comments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(comments);
  }
}
