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
package com.percussion.taxonomy.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link UrlValidator} scheme allow-list and validity. */
public class UrlValidatorTest {

  @Test
  public void defaultSchemes_acceptsHttp() {
    UrlValidator validator = new UrlValidator();
    assertTrue(validator.isValid("http://example.com/path"));
  }

  @Test
  public void nullValue_isInvalid() {
    UrlValidator validator = new UrlValidator();
    assertFalse(validator.isValid(null));
  }

  @Test
  public void customSchemes_rejectUnknownScheme() {
    UrlValidator validator = new UrlValidator(new String[] {"https"});
    assertFalse(validator.isValid("http://example.com/"));
    assertTrue(validator.isValid("https://example.com/"));
  }
}
