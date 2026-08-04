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
package com.percussion.delivery.metadata.impl;

import com.percussion.security.error.PSExceptionUtils;
import java.util.Comparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Comparator that orders tag JSON objects alphabetically by their {@code TAG_NAME} property. Used
 * by the metadata indexer when rendering tag lists that should not be re-ordered by visit count.
 *
 * @author davidpardini
 */
public class AlphaOrderTagComparator implements Comparator<JSONObject> {
  private static final Logger log = LogManager.getLogger(AlphaOrderTagComparator.class);

  /** No-arg constructor. Spring / JAXB-friendly; the comparator is stateless. */
  public AlphaOrderTagComparator() {}

  /**
   * Compares two tag JSON objects alphabetically by their {@code TAG_NAME} value.
   *
   * @param o1 the first tag object to compare; may be <code>null</code>.
   * @param o2 the second tag object to compare; may be <code>null</code>.
   * @return a negative integer, zero or a positive integer following the {@link Comparator}
   *     contract. Returns {@code 0} when the {@code TAG_NAME} cannot be read from either object.
   */
  public int compare(JSONObject o1, JSONObject o2) {
    JSONObject ob1 = o1;
    JSONObject ob2 = o2;
    int returnCompare = 0;
    try {
      returnCompare =
          ((String) ob1.get(PSMetadataTagsHelper.TAG_NAME))
              .compareTo((String) ob2.get(PSMetadataTagsHelper.TAG_NAME));
    } catch (JSONException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return returnCompare;
  }
}
