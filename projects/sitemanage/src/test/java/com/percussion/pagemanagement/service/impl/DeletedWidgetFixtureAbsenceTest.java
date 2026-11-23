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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Issue #7109 (parent #2626, follow-up to #7094): the dead legacy widget-definition XML
 * fixtures {@code widgets/RawHtmlWidget.xml}, {@code widgets/RawHtmlWidget2.xml}, and
 * {@code widgets/TestBadWidget.xml} under {@code src/test/resources} had zero consumers
 * (production + test sweep 2026-11-23) and were deleted. They must never silently return.
 * Fails if any of the files is ever re-added under the old path.
 *
 * <p>Out of scope (staying): compiler/parser upgrade-input fixtures
 * ({@code modules/perc-packages/src/test/resources/widgetxml|gadgetxml}) and the inventory
 * gates that pin the modern ship path.
 */
class DeletedWidgetFixtureAbsenceTest {

  private static final String[] DELETED_FIXTURES = {
    "widgets/RawHtmlWidget.xml", "widgets/RawHtmlWidget2.xml", "widgets/TestBadWidget.xml"
  };

  @Test
  void deletedLegacyWidgetFixturesStayOffClasspath() {
    ClassLoader loader = DeletedWidgetFixtureAbsenceTest.class.getClassLoader();
    for (String fixture : DELETED_FIXTURES) {
      assertNull(
          loader.getResource(fixture), "deleted legacy fixture " + fixture + " must stay absent");
    }
  }
}
