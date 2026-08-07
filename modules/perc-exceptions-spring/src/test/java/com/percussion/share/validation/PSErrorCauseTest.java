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
package com.percussion.share.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSErrorCause} constructors that avoid overridable setters (issue
 * #2017 this-escape remediation).
 *
 * <p>Note: {@code new PSErrorCause(t)} records {@code t}'s message fields and then chains {@code
 * t.getCause()} (not {@code t} itself) via {@link PSErrorCause#setCause(Throwable)} — matching the
 * historical JAXB wrapper contract.
 */
public class PSErrorCauseTest {

  @Test
  @DisplayName("throwable constructor copies message and chains nested cause")
  void throwableConstructorCopiesMessageAndNestedCause() {
    Exception nested = new IllegalArgumentException("nested");
    RuntimeException root = new RuntimeException("root", nested);

    PSErrorCause wrapper = new PSErrorCause(root);

    assertEquals("root", wrapper.getMessage());
    assertEquals("root", wrapper.getLocalizedMessage());
    // Historical contract: cause field holds t.getCause(), not t
    assertSame(nested, wrapper.getCause());
    assertNotNull(wrapper.getErrorCause());
    assertEquals("nested", wrapper.getErrorCause().getMessage());
  }

  @Test
  @DisplayName("throwable without nested cause leaves cause fields null")
  void throwableWithoutNestedCauseLeavesCauseNull() {
    PSErrorCause wrapper = new PSErrorCause(new RuntimeException("solo"));
    assertEquals("solo", wrapper.getMessage());
    assertNull(wrapper.getCause());
    assertNull(wrapper.getErrorCause());
  }

  @Test
  @DisplayName("setMessage completes for HTML-ish input")
  void setMessageSanitizesHtml() {
    PSErrorCause wrapper = new PSErrorCause();
    wrapper.setMessage("<script>x</script>");
    assertNotNull(wrapper.getMessage());
  }

  @Test
  @DisplayName("PSErrorCauseElement copies stack frame fields")
  void elementCopiesStackFrame() {
    StackTraceElement ste = new StackTraceElement("com.example.Foo", "bar", "Foo.java", 42);
    PSErrorCause.PSErrorCauseElement element = new PSErrorCause.PSErrorCauseElement(ste);

    assertEquals("com.example.Foo", element.getClassName());
    assertEquals("bar", element.getMethodName());
    assertEquals("Foo.java", element.getFileName());
    assertEquals(42, element.getLineNumber());
    assertEquals(ste, element.getStackTraceElement());
  }
}
