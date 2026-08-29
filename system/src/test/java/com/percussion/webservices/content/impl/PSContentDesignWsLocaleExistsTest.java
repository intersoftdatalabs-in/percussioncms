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
package com.percussion.webservices.content.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.i18n.PSLocale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * CD-18: locale create must not treat {@link Optional#empty()} as "already exists"
 * (#4005). {@code Optional != null} is always true.
 */
class PSContentDesignWsLocaleExistsTest {

  @Test
  void emptyOptionalIsNotAlreadyExists() {
    assertFalse(PSContentDesignWs.localeLanguageAlreadyExists(Optional.empty()));
  }

  @Test
  void nullOptionalIsNotAlreadyExists() {
    assertFalse(PSContentDesignWs.localeLanguageAlreadyExists(null));
  }

  @Test
  void presentOptionalIsAlreadyExists() {
    PSLocale locale = new PSLocale();
    locale.setLanguageString("ko-kr");
    assertTrue(PSContentDesignWs.localeLanguageAlreadyExists(Optional.of(locale)));
  }
}
