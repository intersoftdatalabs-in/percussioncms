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

public class AbstractImageMetaData {
  private String imageKey;
  private ImageMetaData metaData;

  public AbstractImageMetaData() {
    super();
  }

<<<<<<< HEAD
  /**
   * @return the imageKey
   */
=======
  /** @return the imageKey */
>>>>>>> development-8.1.x
  public String getImageKey() {
    return imageKey;
  }

<<<<<<< HEAD
  /**
   * @param imageKey the imageKey to set
   */
=======
  /** @param imageKey the imageKey to set */
>>>>>>> development-8.1.x
  public void setImageKey(String imageKey) {
    this.imageKey = imageKey;
  }

<<<<<<< HEAD
  /**
   * @return the metaData
   */
=======
  /** @return the metaData */
>>>>>>> development-8.1.x
  public ImageMetaData getMetaData() {
    return metaData;
  }

<<<<<<< HEAD
  /**
   * @param metaData the metaData to set
   */
=======
  /** @param metaData the metaData to set */
>>>>>>> development-8.1.x
  public void setMetaData(ImageMetaData metaData) {
    this.metaData = metaData;
  }
}
