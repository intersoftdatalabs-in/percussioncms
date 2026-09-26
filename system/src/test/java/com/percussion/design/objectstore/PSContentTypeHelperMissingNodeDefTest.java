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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import org.junit.jupiter.api.Test;

/** findNodeDef must treat a wrapped missing node def as absent (#4905). */
class PSContentTypeHelperMissingNodeDefTest {

  @Test
  void wrappedMissingNodeTypeIsStillRecognized() {
    NoSuchNodeTypeException missing = new NoSuchNodeTypeException("Specified defs not found");
    RepositoryException wrapped = new RepositoryException("Problem loading definitions", missing);
    assertTrue(PSContentTypeHelper.isMissingNodeDefinition(wrapped));
    assertTrue(PSContentTypeHelper.isMissingNodeDefinition(missing));
    assertFalse(PSContentTypeHelper.isMissingNodeDefinition(new RepositoryException("other")));
    assertFalse(PSContentTypeHelper.isMissingNodeDefinition(null));
  }
}
