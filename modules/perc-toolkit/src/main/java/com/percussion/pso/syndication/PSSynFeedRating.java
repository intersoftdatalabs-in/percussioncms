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

import com.rometools.modules.mediarss.types.Rating;

/***
 * &lt;media:rating&gt;
 *
 * This allows the permissible audience to be declared. If this element is not included, it assumes that no restrictions are necessary. It has one optional attribute.
 *
 * &lt;media:rating scheme="urn:simple"&gt;adult&lt;/media:rating&gt;
 * &lt;media:rating scheme="urn:icra"&gt;r (cz 1 lz 1 nz 1 oz 1 vz 1)&lt;/media:rating&gt;
 * &lt;media:rating scheme="urn:mpaa"&gt;pg&lt;/media:rating&gt;
 *
 * &lt;media:rating scheme="urn:v-chip"&gt;tv-y7-fv&lt;/media:rating&gt;
 * scheme is the URI that identifies the rating scheme. It is an optional attribute. If this attribute is not included, the default scheme is urn:simple (adult | nonadult).
 *
 * For compatibility, a medai:adult tag will appear in the ratings as a urn:simple equiv.
 *
 * @author natechadwick
 *
 * <p>This allows the permissible audience to be declared. If this element is not included, it
 * assumes that no restrictions are necessary. It has one optional attribute.
 *
 * <p>&lt;media:rating scheme="urn:simple"&gt;adult&lt;/media:rating&gt; &lt;media:rating scheme="urn:icra"&gt;r (cz 1
 * lz 1 nz 1 oz 1 vz 1)&lt;/media:rating&gt; &lt;media:rating scheme="urn:mpaa"&gt;pg&lt;/media:rating&gt;
 *
 * <p>&lt;media:rating scheme="urn:v-chip"&gt;tv-y7-fv&lt;/media:rating&gt; scheme is the URI that identifies
 * the rating scheme. It is an optional attribute. If this attribute is not included, the default
 * scheme is urn:simple (adult | nonadult).
 *
 * <p>For compatibility, a medai:adult tag will appear in the ratings as a urn:simple equiv.
 *
 * @author natechadwick
 */
public class PSSynFeedRating {

  private Rating rating;

  /**
   * Returns the scheme.
   *
   * @return the result
   */
  public String getScheme() {
    return rating.getScheme();
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public String getValue() {
    return rating.getValue();
  }

  /**
   * Creates a new PSSynFeedRating.
   *
   * @param arg the arg
   */
  public PSSynFeedRating(Rating arg) {
    rating = arg;
  }

  /**
   * toString operation.
   *
   * @return the result
   */
  @Override
  public String toString() {
    return rating.getValue();
  }
}
