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

package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSContentEditor;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link PSItemDefinition#getInternalRequestResource()}.
 *
 * <p>The resource name joins the application name and the content editor name with a forward
 * slash. That slash is a logical request separator parsed by
 * {@code PSServer.getInternalRequestHandler(String)} via {@code indexOf("/")}; it is not an OS
 * filesystem path, so it must stay {@code "/"} on every platform (see issue #7018).
 */
public class PSItemDefinitionInternalRequestResourceTest {

  private static PSItemDefinition createDefinition(String appName, String editorName) {
    PSContentType typeDef =
        new PSContentType(1, "testType", "Test Type", "desc", "../app/resource", false, 0);
    PSContentEditor editorDef = new PSContentEditor(editorName, 1L, 1);
    return new PSItemDefinition(appName, typeDef, editorDef);
  }

  @Test
  public void testInternalRequestResourceUsesForwardSlash() {
    PSItemDefinition def = createDefinition("testApp", "testEditor");

    assertEquals("testApp/testEditor", def.getInternalRequestResource());
  }

  @Test
  public void testInternalRequestResourceMatchesServerParseContract() {
    PSItemDefinition def = createDefinition("testApp", "testEditor");
    String resource = def.getInternalRequestResource();

    // Mirrors PSServer.getInternalRequestHandler(String): exactly one "/",
    // neither first nor last, splitting app root from page name.
    int sep = resource.indexOf("/");
    assertTrue(sep > 0, "separator must not be first");
    assertTrue(sep == resource.lastIndexOf("/"), "exactly one separator expected");
    assertTrue(sep < resource.length() - 1, "separator must not be last");
    assertEquals("testApp", resource.substring(0, sep));
    assertEquals("testEditor", resource.substring(sep + 1));
  }
}
