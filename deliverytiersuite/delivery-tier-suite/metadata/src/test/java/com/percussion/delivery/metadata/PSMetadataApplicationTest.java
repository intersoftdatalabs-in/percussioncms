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
package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Structural coverage for {@link PSMetadataApplication}: the class is {@code final} so Jersey
 * {@code ResourceConfig#register} calls in the constructor cannot observe a subclass before full
 * initialization (javac {@code this-escape}).
 */
public class PSMetadataApplicationTest {

  @Test
  public void applicationClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSMetadataApplication.class.getModifiers()),
        "PSMetadataApplication must be final to avoid this-escape in the ctor");
  }
}
