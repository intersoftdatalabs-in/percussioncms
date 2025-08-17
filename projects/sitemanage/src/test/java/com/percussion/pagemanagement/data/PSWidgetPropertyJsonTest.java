// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.data;

import static com.percussion.share.dao.PSSerializerUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class PSWidgetPropertyJsonTest {

  private final String number = "234";
  private final String string = "'hello'";
  private final String list = "['a','b','c']";
  private final String empty = "";

  @Test
  public void testJson() throws Exception {
    log.debug(getObjectFromJson(list));
    log.debug(getObjectFromJson(number));
    log.debug(getObjectFromJson(string));
    log.debug(getJsonFromObject(42));
    log.debug(getJsonFromObject("42"));

    var trueJson = getJsonFromObject(Boolean.TRUE);
    var trueObject = getObjectFromJson(trueJson);
    assertTrue(trueObject instanceof Boolean);
    assertTrue((Boolean) trueObject);
  }

  @Test
  public void testEmptyJsonString() throws Exception {
    var o = getObjectFromJson(empty);
    assertThat(o, nullValue());
  }

  /** The log instance to use for this class, never null. */
  private static final Logger log = LogManager.getLogger(PSWidgetPropertyJsonTest.class);
}
