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

/**
 * Bean class to hold basic page/likes summary info.
 *
 * @author davidpardini
 */
public class PSLikesSummary {

  /** Total number of likes. */
  private int total;

  /** The like identifier. */
  private String likeId;

  /** Default no-arg constructor required by JAXB. */
  public PSLikesSummary() {
    // Default constructor
  }

  /**
   * Creates a new likes summary with the supplied total and like id.
   *
   * @param total the total number of likes.
   * @param likeId the like identifier.
   */
  public PSLikesSummary(int total, String likeId) {
    this.total = total;
    this.likeId = likeId;
  }

  /**
   * Gets the total number of likes.
   *
   * @return the total count.
   */
  public int getTotal() {
    return total;
  }

  /**
   * Sets the total number of likes.
   *
   * @param total the total count.
   */
  public void setTotal(int total) {
    this.total = total;
  }

  /**
   * Gets the like identifier.
   *
   * @return the like ID.
   */
  public String getLikeId() {
    return likeId;
  }

  /**
   * Sets the like identifier.
   *
   * @param likeId the like ID.
   */
  public void setLikeId(String likeId) {
    this.likeId = likeId;
  }
}
