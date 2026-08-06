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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.webdav.test.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ensures {@code PSServletRequesterTest.writeStackTrace} never exposes exception type, message, or
 * stack frames on the response (CodeQL {@code java/stack-trace-exposure} / {@code
 * java/error-message-exposure} #789 residual #1768 / T055).
 */
@DisplayName("PSServletRequesterTest.writeStackTrace — generic error only (#789/#1768)")
class PSServletRequesterTestStackTraceExposureTest {

  private static final String SECRET_MESSAGE = "boom <secret-path>/internal";

  @Test
  void writeStackTraceEmitsGenericErrorWithoutMessageOrFrames() throws Exception {
    PSServletRequesterTest harness = new PSServletRequesterTest();
    StringWriter sw = new StringWriter();
    java.lang.reflect.Field writerField = PSServletRequesterTest.class.getDeclaredField("m_writer");
    writerField.setAccessible(true);
    writerField.set(harness, new PrintWriter(sw, true));

    Method m = PSServletRequesterTest.class.getDeclaredMethod("writeStackTrace", Exception.class);
    m.setAccessible(true);
    m.invoke(harness, new IllegalStateException(SECRET_MESSAGE));

    String out = sw.toString();
    assertTrue(out.contains("error"), "generic error body expected: " + out);
    assertFalse(out.contains("IllegalStateException"), "must not leak exception type: " + out);
    assertFalse(out.contains("boom"), "must not leak exception message: " + out);
    assertFalse(out.contains("secret-path"), "must not leak exception message: " + out);
    assertFalse(out.contains("at "), "must not dump stack frames: " + out);
    assertFalse(out.contains("PSServletRequesterTestStackTraceExposureTest"), out);
  }
}
