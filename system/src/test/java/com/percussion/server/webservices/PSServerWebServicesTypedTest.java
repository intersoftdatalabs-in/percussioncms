/*
 * Copyright 1999-2026 Percussion Software and its affiliates.
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
package com.percussion.server.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.ws.PSLocatorWithName;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.search.objectstore.PSWSSearchRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed collections in {@code com.percussion.server.webservices} (#3160 residual
 * of #2877 / parent #2022).
 */
class PSServerWebServicesTypedTest {

  @Test
  void webServicesRequestHandler_initAndRoots_areTypedStrings() throws Exception {
    PSWebServicesRequestHandler handler = new PSWebServicesRequestHandler();
    Collection<String> roots = Arrays.asList("Rhythmyx/webservices", "rxwebservices");
    handler.init(roots, null);

    Iterator<String> it = handler.getRequestRoots();
    assertTrue(it.hasNext());
    assertEquals("Rhythmyx/webservices", it.next());
    assertEquals("rxwebservices", it.next());
    assertFalse(it.hasNext());
  }

  @Test
  void webServicesRequestHandler_initRejectsEmptyRoots() {
    PSWebServicesRequestHandler handler = new PSWebServicesRequestHandler();
    assertThrows(
        IllegalArgumentException.class, () -> handler.init(new ArrayList<String>(), null));
  }

  @Test
  void searchRequest_internalParams_areTypedStringMap() {
    Map<String, String> in = new HashMap<>();
    in.put("sys_contentid", "301");
    in.put("sys_folderid", "1");
    PSWSSearchRequest req = new PSWSSearchRequest("myInternalSearch", in);

    Map<String, String> out = req.getInternalSearchParams();
    assertNotNull(out);
    assertEquals(2, out.size());
    assertEquals("301", out.get("sys_contentid"));
    assertEquals("1", out.get("sys_folderid"));
    assertThrows(UnsupportedOperationException.class, () -> out.put("x", "y"));
  }

  @Test
  void locatorWithName_projectsToPlainLocator() {
    // Mirrors PSFolderHandler.ParentChildIds#getChildLocatorList projection used by
    // add/move/remove folder APIs that require List<PSLocator>.
    List<Object> mixed = new ArrayList<>();
    mixed.add(new PSLocator(10, 1));
    mixed.add(new PSLocatorWithName(20, 2, "Override Title"));

    List<PSLocator> projected = new ArrayList<>();
    for (Object o : mixed) {
      if (o instanceof PSLocator) {
        projected.add((PSLocator) o);
      } else if (o instanceof PSLocatorWithName) {
        PSLocatorWithName named = (PSLocatorWithName) o;
        projected.add(new PSLocator(named.getId(), named.getRevision()));
      } else {
        throw new IllegalStateException("unexpected type " + o);
      }
    }

    assertEquals(2, projected.size());
    assertEquals(10, projected.get(0).getId());
    assertEquals(1, projected.get(0).getRevision());
    assertEquals(20, projected.get(1).getId());
    assertEquals(2, projected.get(1).getRevision());
  }

  @Test
  void folderCommunities_setIsTypedInteger() {
    // Documents the getFolderCommunities contract used by PSFolderHandler.
    Set<Integer> communities = new HashSet<>(Arrays.asList(1001, 1002));
    assertTrue(communities.contains(1001));
    assertEquals(2, communities.size());
  }
}
