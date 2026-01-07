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

package com.percussion.comments.data;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Comment moderation object sent by client, with a list of comment IDs to moderate in the delivery
 * side.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"deletes", "approves", "rejects"})
@XmlRootElement(name = "moderation")
public class PSCommentModeration {

  private Collection<PSSiteComments> deletes;
  private Collection<PSSiteComments> approves;
  private Collection<PSSiteComments> rejects;

  public PSCommentModeration() {
    deletes = new ArrayList<>();
    approves = new ArrayList<>();
    rejects = new ArrayList<>();
  }

  /**
   * @return the collection of deletes
   */
  public Collection<PSSiteComments> getDeletes() {
    return deletes;
  }

  public void setDeletes(Collection<PSSiteComments> deletes) {
    this.deletes = deletes;
  }

  /**
   * @return the collection of approves
   */
  public Collection<PSSiteComments> getApproves() {
    return approves;
  }

  public void setApproves(Collection<PSSiteComments> approves) {
    this.approves = approves;
  }

  /**
   * @return the collection of rejects
   */
  public Collection<PSSiteComments> getRejects() {
    return rejects;
  }

  public void setRejects(Collection<PSSiteComments> rejects) {
    this.rejects = rejects;
  }
}
