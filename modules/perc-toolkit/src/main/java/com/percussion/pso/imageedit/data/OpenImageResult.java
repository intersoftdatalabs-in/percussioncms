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
package com.percussion.pso.imageedit.data;

import com.percussion.services.content.data.PSItemStatus;

/**
 * Result of the OpenImage method.
 *
 * @author DavidBenua
 */
public class OpenImageResult {
  private MasterImageMetaData masterImage;
  private PSItemStatus itemStatus;

  public OpenImageResult() {}

  public OpenImageResult(MasterImageMetaData masterImage, PSItemStatus itemStatus) {
    this();
    this.masterImage = masterImage;
    this.itemStatus = itemStatus;
  }
<<<<<<< HEAD

  /**
   * @return the masterImage
   */
=======
  /** @return the masterImage */
>>>>>>> development-8.1.x
  public MasterImageMetaData getMasterImage() {
    return masterImage;
  }

<<<<<<< HEAD
  /**
   * @return the itemStatus
   */
=======
  /** @return the itemStatus */
>>>>>>> development-8.1.x
  public PSItemStatus getItemStatus() {
    return itemStatus;
  }

  /**
   * Sets the masterImage
   *
   * @param masterImage the master image
   */
  public void setMasterImage(MasterImageMetaData masterImage) {
    this.masterImage = masterImage;
  }

  /**
   * Sets the item status
   *
   * @param itemStatus the item status to set
   */
  public void setItemStatus(PSItemStatus itemStatus) {
    this.itemStatus = itemStatus;
  }
}
