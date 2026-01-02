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

import com.rometools.rome.feed.synd.SyndEnclosure;

<<<<<<< HEAD
/***
 * Provides a Velocity friendly class for handling enclosures.
 *
 * @author natechadwick
=======
/**
 * * Provides a Velocity friendly class for handling enclosures.
>>>>>>> development-8.1.x
 *
 * @author natechadwick
 */
public class PSSynFeedEnclosure {

  private SyndEnclosure enc;

<<<<<<< HEAD
  /***
   * Returns the enclosure length.
=======
  /**
   * * Returns the enclosure length.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public long getLength() {
    return enc.getLength();
  }

<<<<<<< HEAD
  /***
   * Returns the enclosure type.
=======
  /**
   * * Returns the enclosure type.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getType() {
    return enc.getType();
  }

<<<<<<< HEAD
  /***
   * Returns the enclosure URL.
=======
  /**
   * * Returns the enclosure URL.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getUrl() {
    return enc.getUrl();
  }

  public PSSynFeedEnclosure(SyndEnclosure arg) {
    this.enc = arg;
  }
}
