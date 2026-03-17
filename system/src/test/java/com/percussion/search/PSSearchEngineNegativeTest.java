/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.search;

import org.junit.jupiter.api.Test;

/**
 * This unit tests runs w/o the Rx server and attempts to do things that are not allowed and
 * verifies the proper exceptions are thrown.
 *
 * @author paulhoward
 */
public class PSSearchEngineNegativeTest {
  /**
   * Tries to get the search engine instance w/o properties.
   *
   * @throws PSSearchException
   */
  @Test
  public void testInitializeEngine() throws PSSearchException {
    String oldTimeout = System.getProperty("com.percussion.search.init.timeout");
    System.setProperty("com.percussion.search.init.timeout", "1");
    try {
      PSSearchEngine.getInstance();
      org.junit.jupiter.api.Assertions.fail("Returned engine instance w/o properties.");
    } catch (IllegalStateException ise) {
      // expected
    } finally {
      if (oldTimeout != null) System.setProperty("com.percussion.search.init.timeout", oldTimeout);
      else System.clearProperty("com.percussion.search.init.timeout");
    }
  }
}
