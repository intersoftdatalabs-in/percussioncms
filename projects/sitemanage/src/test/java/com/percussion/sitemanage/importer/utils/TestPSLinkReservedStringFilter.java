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

// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.utils;

import static com.percussion.test.TestAssertions.*;

import com.percussion.services.assembly.impl.PSReplacementFilter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSReplacementFilter}. */
@Tag("UnitTest")
class TestPSLinkReservedStringFilter {

  private static final String FOO = "foo";
  private static final String BAR = "bar";

  @Test
  void testFilter() {
    assertEquals(
        "/this-has-spaces", PSReplacementFilter.filter("/this has spaces"), "Test Space Failed");
    assertEquals(
        "/this-has-brackets",
        PSReplacementFilter.filter("/this [has] brackets"),
        "Test Brackets Failed");
    assertEquals(
        "/this-has-brac-kets",
        PSReplacementFilter.filter("/this       [has]     brac--kets"),
        "Test Multiple Dashes Failed");
    assertEquals(
        "/test/this-has-spaces",
        PSReplacementFilter.filter("/test/this%20has%20spaces"),
        "Test Encoded Space Failed");
    assertEquals(
        "/test/this-has-spaces",
        PSReplacementFilter.filter("\\test\\this has spaces"),
        "Test Backslash Failed");
    assertEquals(
        "/this-has-spaces/and-malformed#anchor",
        PSReplacementFilter.filter("\\this has spaces\\and#anchor?malformed"),
        "Test Anchor Failed");
    assertEquals(
        "/test-a-colon/this-has-spaces",
        PSReplacementFilter.filter("\\test:a:colon\\this has spaces"),
        "Test Colon Failed");
    assertEquals(
        "/test-a-percent/this-has-spaces",
        PSReplacementFilter.filter("\\test%a%percent\\this has spaces"),
        "Test Percent Failed");
    assertEquals(
        "/test-a-semicolon/this-has-spaces",
        PSReplacementFilter.filter("\\test;a;semicolon\\this has spaces"),
        "Test Semicolon Failed");
    assertEquals(
        "/test-a-asterisk/this-has-spaces",
        PSReplacementFilter.filter("\\test*a*asterisk\\this has spaces"),
        "Test Asterisk Failed");
    assertEquals(
        "/test-a-question-mark/this-has-spaces",
        PSReplacementFilter.filter("\\test?a?question?mark\\this has spaces"),
        "Test Question Mark Failed");
    assertEquals(
        "/test-a-less-than/this-has-spaces",
        PSReplacementFilter.filter("\\test<a<less<than\\this has spaces"),
        "Test Less Than Failed");
    assertEquals(
        "/test-a-greater-than/this-has-spaces",
        PSReplacementFilter.filter("\\test>a>greater>than\\this has spaces"),
        "Test Greater Than Failed");
    assertEquals(
        "/test-a-pipe/this-has-spaces",
        PSReplacementFilter.filter("\\test|a|pipe\\this has spaces"),
        "Test Pipe Failed");
  }
}
