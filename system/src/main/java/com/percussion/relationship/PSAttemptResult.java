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

package com.percussion.relationship;

/**
 * This class may define additional methods to pass sufficient information as attempt result to
 * relationship engine after executing the effect's attempt method. This information includes
 * whether to test for dependents' processing and error details if the test fails for some reason.
 */
public final class PSAttemptResult extends PSEffectResult {
  /** Default constructor. Does not do much. */
  public PSAttemptResult() {
    // Default constructor
  }

  /**
   * Implementation for an abstract method, that does NOT allow to set a recursion flag by always
   * throwing UnsupportedOperationException. This is to indicate to the effect implementer that it
   * is an illegal operation for the attempt method.
   *
   * @param recurseDependents flag indicating whether to recurse dependents
   * @throws UnsupportedOperationException always, as this operation is not allowed for attempt
   */
  @Override
  public void setRecurseDependents(boolean recurseDependents) {
    throw new UnsupportedOperationException("Recursion not allowed for attempt operations");
  }
}
