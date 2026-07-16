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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.legacy.security.deprecated;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression test for feature 004 (zero-code-scanning-alerts) Phase 5 T047/T048: confirms PSAesCBC
 * carries the {@link Deprecated} annotation with {@code forRemoval=true} so that the CodeQL
 * java/weak-cryptographic-algorithm and java/static-initialization-vector alerts are documented as
 * accepted-risk (deferred to release 9.0) rather than outstanding. The test fails on the pre-fix
 * code (annotation absent) and passes on the post-fix code.
 */
class PSAesCBCDeprecationTest {

  @Test
  void classIsAnnotatedAsDeprecatedForRemoval() {
    Deprecated annotation = PSAesCBC.class.getAnnotation(Deprecated.class);
    assertNotNull(
        annotation,
        "PSAesCBC must be annotated @Deprecated for the 9.0 removal (CodeQL accepted-risk)");
    assertTrue(
        annotation.forRemoval(),
        "PSAesCBC @Deprecated(forRemoval=true) is required by accepted-risks.md");
  }

  @Test
  void staticInitializationVectorFieldIsDocumentedAsAcceptedRisk() throws Exception {
    java.lang.reflect.Field f = PSAesCBC.class.getDeclaredField("INITIAL_VECTOR");
    assertNotNull(f, "INITIAL_VECTOR field must exist (CodeQL static-IV accepted-risk)");
    // The class-level @Deprecated + class-level javadoc on INITIAL_VECTOR is the
    // accepted-risk marker that downstream consumers see; the test asserts the field
    // is still present so decryption of historical ciphertext continues to work.
    Deprecated classDep = PSAesCBC.class.getAnnotation(Deprecated.class);
    assertNotNull(
        classDep,
        "PSAesCBC must remain @Deprecated to flag the static-IV accepted-risk to callers");
  }
}
