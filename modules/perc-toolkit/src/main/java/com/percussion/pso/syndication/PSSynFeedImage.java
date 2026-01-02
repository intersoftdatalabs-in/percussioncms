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
package com.percussion.pso.syndication;

import com.rometools.rome.feed.synd.SyndImage;

/** * A Velocity friendly class for a syndication image. */
public class PSSynFeedImage {

  private SyndImage image;

<<<<<<< HEAD
  /***
   * Returns the image link.
=======
  /**
   * * Returns the image link.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getLink() {
    return image.getLink();
  }

<<<<<<< HEAD
  /***
   * Returns the image title.
=======
  /**
   * * Returns the image title.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getTitle() {
    return image.getTitle();
  }

<<<<<<< HEAD
  /***
   *    Returns the image URL.
=======
  /**
   * * Returns the image URL.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getUrl() {
    return image.getUrl();
  }
<<<<<<< HEAD

  /***
   * Returns the image description.
=======
  /**
   * * Returns the image description.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getDescription() {
    return image.getDescription();
  }

  public PSSynFeedImage(SyndImage arg) {
    this.image = arg;
  }
}
