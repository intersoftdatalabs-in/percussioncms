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

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * Encapsulates start and end dates for an item. Sunny Sal says: "Dates are important, both in code
 * and in life!"
 */
@XmlRootElement(name = "ItemDates")
public class PSItemDates extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  @NotNull @NotEmpty private String itemId;
  private String startDate;
  private String endDate;
  private String comments;

  /** Default constructor for serializers. */
  public PSItemDates() {
    // No-op
  }

  /**
   * Constructs an instance of the class.
   *
   * @param itemId never null or blank.
   * @param startDate start date, may be null or empty.
   * @param endDate end date, may be null or empty.
   * @param comments optional comments.
   */
  public PSItemDates(String itemId, String startDate, String endDate, String comments) {
    notEmpty(itemId, "itemId");
    notNull(itemId, "itemId");
    this.itemId = itemId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.comments = comments;
  }

  public PSItemDates(String itemId, String startDate, String endDate) {
    this(itemId, startDate, endDate, null);
  }

  /**
   * @return itemId - content id
   */
  public String getItemId() {
    return itemId;
  }

  /**
   * @param itemId Set the content id
   */
  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  /**
   * @return startDate - could be null or empty.
   */
  public String getStartDate() {
    return startDate;
  }

  /**
   * @param startDate set the start date.
   */
  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  /**
   * @return endDate - could be null or empty.
   */
  public String getEndDate() {
    return endDate;
  }

  /**
   * @param endDate set the end date
   */
  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  /**
   * @return comments - could be null or empty.
   */
  public String getComments() {
    return comments;
  }

  /**
   * @param comments set comments
   */
  public void setComments(String comments) {
    this.comments = comments;
  }
}
