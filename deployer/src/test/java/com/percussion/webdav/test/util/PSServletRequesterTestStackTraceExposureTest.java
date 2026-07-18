/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Ensures {@code PSServletRequesterTest.writeStackTrace} no longer dumps frames (CodeQL {@code
 * java/stack-trace-exposure} #789 / T055 residual).
 */
@DisplayName("PSServletRequesterTest.writeStackTrace — no stack dump (#789)")
class PSServletRequesterTestStackTraceExposureTest {

  @Test
  void writeStackTraceEmitsTypeAndMessageWithoutFrames() throws Exception {
    PSServletRequesterTest harness = new PSServletRequesterTest();
    StringWriter sw = new StringWriter();
    // m_writer is package-private? set via reflection if needed
    java.lang.reflect.Field writerField = PSServletRequesterTest.class.getDeclaredField("m_writer");
    writerField.setAccessible(true);
    writerField.set(harness, new PrintWriter(sw, true));

    Method m =
        PSServletRequesterTest.class.getDeclaredMethod("writeStackTrace", Exception.class);
    m.setAccessible(true);
    m.invoke(harness, new IllegalStateException("boom <x>"));

    String out = sw.toString();
    assertTrue(out.contains("IllegalStateException"), out);
    assertTrue(out.contains("boom &lt;x&gt;"), "HTML-escaped message expected: " + out);
    assertFalse(out.contains("at "), "must not dump stack frames: " + out);
    assertFalse(out.contains("PSServletRequesterTestStackTraceExposureTest"), out);
  }
}
