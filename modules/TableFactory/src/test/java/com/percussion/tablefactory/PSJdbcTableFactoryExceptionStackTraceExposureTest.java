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

package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for PSJdbcTableFactoryException that verify wrapped (cause) exception
 * stack-trace information is NOT leaked through {@code printStackTrace(...)} sinks that could
 * be HTTP response writers (CodeQL {@code java/stack-trace-exposure}).
 *
 * <p>Follows the Constitution III fail-then-pass contract: on pre-fix code the {@code
 * printStackTrace(PrintWriter s)} and {@code printStackTrace(PrintStream s)} overrides
 * delegated to {@code m_th.printStackTrace(s)} which would emit the wrapped exception's
 * stack trace (potentially containing internal paths / connection-string fragments) into the
 * caller's writer. On post-fix code the overrides delegate to {@code super.printStackTrace(s)},
 * which emits only {@code this} exception's stack trace, and callers that need the wrapped
 * trace for server-side logging must use {@link
 * PSJdbcTableFactoryException#getStackTraceAsString(Throwable)}.
 */
@DisplayName("PSJdbcTableFactoryException Stack Trace Exposure Prevention Tests")
class PSJdbcTableFactoryExceptionStackTraceExposureTest {

  /** Sensitive substring that must NEVER appear in a client-facing printStackTrace output. */
  private static final String SENSITIVE_TOKEN = "internal-secret-conn=db1.example.com";

  /** A wrapped exception whose message contains the sensitive token. */
  private static final Throwable LEAKY_WRAPPER =
      new IllegalStateException("connection failed at " + SENSITIVE_TOKEN);

  @Test
  @DisplayName("printStackTrace(PrintWriter) must not leak wrapped exception's stack trace")
  void testPrintStackTracePrintWriterDoesNotLeakWrappedTrace() {
    var exception = new PSJdbcTableFactoryException(1, new Object[] {"ctx"}, LEAKY_WRAPPER);

    var sink = new StringWriter();
    try (var pw = new PrintWriter(sink)) {
      exception.printStackTrace(pw);
      pw.flush();
    }

    String output = sink.toString();
    assertFalse(
        output.contains(SENSITIVE_TOKEN),
        "printStackTrace(PrintWriter) must not leak the wrapped exception's message; got: "
            + output);
  }

  @Test
  @DisplayName("printStackTrace(PrintStream) must not leak wrapped exception's stack trace")
  void testPrintStackTracePrintStreamDoesNotLeakWrappedTrace() {
    var exception = new PSJdbcTableFactoryException(1, new Object[] {"ctx"}, LEAKY_WRAPPER);

    var buffer = new java.io.ByteArrayOutputStream();
    try (var ps = new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8)) {
      exception.printStackTrace(ps);
      ps.flush();
    }

    String output = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
    assertFalse(
        output.contains(SENSITIVE_TOKEN),
        "printStackTrace(PrintStream) must not leak the wrapped exception's message; got: "
            + output);
  }

  @Test
  @DisplayName(
      "printStackTrace() (no-arg, System.err) must not leak wrapped exception's stack trace")
  void testPrintStackTraceNoArgDoesNotLeakWrappedTrace() {
    var exception = new PSJdbcTableFactoryException(1, new Object[] {"ctx"}, LEAKY_WRAPPER);

    // Capture System.err for the duration of this test.
    var originalErr = System.err;
    var buffer = new java.io.ByteArrayOutputStream();
    System.setOut(new java.io.PrintStream(new java.io.ByteArrayOutputStream(), true));
    System.setErr(new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8));
    try {
      exception.printStackTrace();
    } finally {
      System.setErr(originalErr);
    }

    String output = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
    assertFalse(
        output.contains(SENSITIVE_TOKEN),
        "printStackTrace() must not leak the wrapped exception's message; got: " + output);
  }

  @Test
  @DisplayName(
      "getStackTraceAsString(Throwable) IS the supported path for server-side logging of the "
          + "wrapped exception's stack trace")
  void testGetStackTraceAsStringStillSurfacesWrappedTrace() {
    var stackTrace = PSJdbcTableFactoryException.getStackTraceAsString(LEAKY_WRAPPER);
    assertNotNull(stackTrace, "getStackTraceAsString must return non-null");
    assertTrue(
        stackTrace.contains(SENSITIVE_TOKEN),
        "getStackTraceAsString must still surface the wrapped exception's message for server-side"
            + " logging; got: "
            + stackTrace);
  }

  @Test
  @DisplayName("getMessage() returns only this exception's localized message, never the cause")
  void testGetMessageDoesNotLeakWrappedMessage() {
    var exception = new PSJdbcTableFactoryException(1, new Object[] {"ctx"}, LEAKY_WRAPPER);
    var msg = exception.getMessage();
    assertNotNull(msg);
    assertFalse(
        msg.contains(SENSITIVE_TOKEN),
        "getMessage() must return only this exception's localized message; got: " + msg);
  }

  @Test
  @DisplayName(
      "getCause() returns null: the wrapped exception is stored in the protected m_th field, not"
          + " via the standard Throwable cause channel, so the standard Exception.getCause() API"
          + " cannot accidentally leak it to client code that calls getCause()")
  void testGetCauseReturnsNull() {
    var exception = new PSJdbcTableFactoryException(1, new Object[] {"ctx"}, LEAKY_WRAPPER);
    assertEquals(
        null,
        exception.getCause(),
        "Throwable.getCause() should return null because PSJdbcTableFactoryException does not"
            + " pass the cause to super(message, cause). The wrapped exception lives only in the"
            + " protected m_th field, accessible to subclasses but not via the standard"
            + " Throwable.getCause() API.");
  }
}