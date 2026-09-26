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
package com.percussion.services.contentmgr.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import org.junit.jupiter.api.Test;

/**
 * A missing node definition must stay a {@link NoSuchNodeTypeException}. Wrapping it made
 * content-type create abort with "Specified defs not found" before the row was inserted (#4905).
 */
class PSContentMgrLoadDefinitionsFailureTest {

  @Test
  void missingNodeTypeIsNotWrapped() throws Exception {
    NoSuchNodeTypeException missing = new NoSuchNodeTypeException("Specified defs not found");
    RepositoryException out = PSContentMgr.loadDefinitionsFailure(missing);
    assertSame(missing, out);
    assertInstanceOf(NoSuchNodeTypeException.class, out);
  }

  @Test
  void repositoryExceptionIsNotWrapped() {
    RepositoryException already = new RepositoryException("Problem loading definitions");
    assertSame(already, PSContentMgr.loadDefinitionsFailure(already));
  }

  @Test
  void unexpectedFailureIsWrapped() {
    RepositoryException out = PSContentMgr.loadDefinitionsFailure(new IllegalStateException("db"));
    assertInstanceOf(RepositoryException.class, out);
    assertFalse(out instanceof NoSuchNodeTypeException);
    assertInstanceOf(IllegalStateException.class, out.getCause());
  }
}
