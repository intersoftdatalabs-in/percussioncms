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
package test.percussion.pso.relationshipbuilder;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.relationshipbuilder.PSAaDependentRelationshipBuilder;
import com.percussion.pso.relationshipbuilder.PSAaOwnerRelationshipBuilder;
import org.junit.jupiter.api.Test;

/** Owner/dependent orientation set via super(boolean) constructors (this-escape free). */
public class PSAaRelationshipBuilderOrientationTest {

  @Test
  void ownerBuilderIsParent() {
    assertTrue(new PSAaOwnerRelationshipBuilder().isParent());
  }

  @Test
  void dependentBuilderIsNotParent() {
    assertFalse(new PSAaDependentRelationshipBuilder().isParent());
  }
}
