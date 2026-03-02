/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.server;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PSPageCacheTest {

  /** Original timeout restored after each test to avoid polluting static state. */
  private long originalTimeout;

  @BeforeEach
  void setUp() {
    originalTimeout = PSPageCache.getCacheTimeout();
  }

  @AfterEach
  void tearDown() {
    PSPageCache.setCacheTimeout(originalTimeout);
    PSPageCache.cleanCache();
  }

  @Test
  void testPSPageCache() throws Exception {
    DocumentBuilderFactory factory =
        PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
            new PSXmlSecurityOptions(true, true, true, false, true, false));

    DocumentBuilder builder = factory.newDocumentBuilder();

    // Use a short timeout (milliseconds) so the sleep needed to trigger
    // expiry is minimal. The 300 ms sleep gives a 3x margin over the 100 ms
    // cache timeout, keeping the test reliable without sacrificing speed.
    PSPageCache.setCacheTimeout(100);

    PSPageCache.addPage(builder.newDocument());
    assertEquals(1L, PSPageCache.getInstance().getCacheSize());

    Thread.sleep(300);
    PSPageCache.cleanCache();
    assertEquals(0L, PSPageCache.getInstance().getCacheSize());

    // Test FIFO ceiling: only 1000 entries are retained.
    Document prototypeDoc = builder.newDocument();
    for (int i = 1; i < 2000; i++) {
      PSPageCache.addPage(prototypeDoc);
    }
    assertEquals(1000L, PSPageCache.getInstance().getCacheSize());

    // Wait for cache entries to expire, then verify clean-up.
    Thread.sleep(300);
    PSPageCache.cleanCache();
    assertEquals(0L, PSPageCache.getInstance().getCacheSize());
  }
}
