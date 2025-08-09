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
package com.percussion.deployer.server.dependencies;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.server.PSDependencyManager;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.security.PSRoleMgrLocator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test case for the {@link PSRoleDefDependencyHandler}.
 */
@Tag("IntegrationTest")
public class PSRoleDefDependencyHandlerTest {

  /**
   * Test the handler.
   *
   * @throws Exception if the test fails
   */
  @Test
  public void testHandler() throws Exception {
    var roleMgr = PSRoleMgrLocator.getBackEndRoleManager();
    var roles = roleMgr.getRhythmyxRoles();
    assertTrue(roles.size() > 0);

    // test does dependency exist
    var role = roles.get(0);

    var hdlr =
        PSDependencyManager.getInstance()
            .getDependencyHandler(PSRoleDefDependencyHandler.DEPENDENCY_TYPE);

    var tok = new PSSecurityToken("test");
    assertTrue(hdlr.doesDependencyExist(tok, role));
    assertFalse(hdlr.doesDependencyExist(tok, "This dependency does not exist"));

    // test get dependency, dependencies
    Set<PSDependency> roleDeps = new HashSet<>();
    for (var r : roles) {
      var dep = hdlr.getDependency(tok, r);
      assertNotNull(dep);
      roleDeps.add(dep);
    }

    Iterator<?> depIter = hdlr.getDependencies(tok);
    int i = 0;
    while (depIter.hasNext()) {
      assertTrue(roleDeps.contains((PSDependency) depIter.next()));
      i++;
    }

    assertEquals(roleDeps.size(), i);
  }
}
