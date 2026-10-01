/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.deployer.server.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.extension.PSExtensionRef;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers the typed required-application id mapping used by {@link PSExitDefDependencyHandler}.
 * {@code IPSExtensionDef.getRequiredApplications()} returns {@link PSExtensionRef}, not application
 * name strings.
 */
public class PSExitDefDependencyHandlerTypedTest {

  @Test
  public void requiredApplicationIdsUseExtensionNameNotFqn() {
    PSExtensionRef simple =
        new PSExtensionRef("app", "rx_resources/", "rx_resources");
    PSExtensionRef named = new PSExtensionRef("Java", "user/", "MyExit");

    List<String> ids =
        PSExitDefDependencyHandler.requiredApplicationIds(
            Arrays.asList(simple, null, named).iterator());

    assertEquals(List.of("rx_resources", "MyExit"), ids);
    assertTrue(simple.getFQN().contains("/"));
    assertEquals("rx_resources", ids.get(0));
  }

  @Test
  public void requiredApplicationIdsTreatsNullIteratorAsNone() {
    Iterator<PSExtensionRef> apps = null;
    assertEquals(List.of(), PSExitDefDependencyHandler.requiredApplicationIds(apps));
  }

  @Test
  public void extensionFilesTreatsNullAsEmptyAndKeepsRealIterator() throws Exception {
    assertFalse(PSExitDefDependencyHandler.extensionFiles(null).hasNext());
    Iterator<URL> urls = Collections.singleton(new URL("file:///ext.jar")).iterator();
    assertSame(urls, PSExitDefDependencyHandler.extensionFiles(urls));
  }
}
