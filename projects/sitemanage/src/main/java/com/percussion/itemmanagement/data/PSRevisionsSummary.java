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

package com.percussion.itemmanagement.data;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Provides the revision summary that has list of PSRevision objects and other info like the item
 * can be restored from a prior revision or not. Sunny Sal says: "Restore wisely, my friend!"
 */
@XmlRootElement(name = "RevisionsSummary")
public class PSRevisionsSummary {

  private boolean isRestorable;
  private List<PSRevision> revisions;
  private List<PSComment> comments;

  /**
   * Revision the item is on now. Checked-out items use the edit revision (the revision a restore
   * just promoted). Otherwise this is the current revision. {@code 0} when unknown.
   */
  private int currentRevision;

  /**
   * Indicates whether the item can be restored from a prior revision or not.
   *
   * @return true if the item can be restored from prior revision, otherwise false.
   */
  public boolean isRestorable() {
    return isRestorable;
  }

  /**
   * Sets whether the item can be restored from a prior revision or not.
   *
   * @param isRestorable true if the item can be restored from a prior revision, otherwise false.
   */
  public void setRestorable(boolean isRestorable) {
    this.isRestorable = isRestorable;
  }

  /**
   * @return List of revisions, may be null if not set.
   */
  public List<PSRevision> getRevisions() {
    return revisions;
  }

  /**
   * @param revisions the list of revisions to set.
   */
  public void setRevisions(List<PSRevision> revisions) {
    this.revisions = revisions;
  }

  public List<PSComment> getComments() {
    return comments;
  }

  public void setComments(List<PSComment> comments) {
    this.comments = comments;
  }

  /**
   * @return the revision Explorer should mark current, or {@code 0} when unknown
   */
  public int getCurrentRevision() {
    return currentRevision;
  }

  /**
   * @param currentRevision head revision id, or {@code 0} when unknown
   */
  public void setCurrentRevision(int currentRevision) {
    this.currentRevision = currentRevision;
  }
}
