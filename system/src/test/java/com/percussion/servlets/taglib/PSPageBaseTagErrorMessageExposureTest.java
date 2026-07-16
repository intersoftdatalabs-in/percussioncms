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
package com.percussion.servlets.taglib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.data.PSInternalRequestCallException;
import com.percussion.servlets.utils.PSComponentUrls;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for CodeQL {@code java/error-message-exposure} (alert #1728, CWE-209) on {@link
 * PSPageBaseTag#getUrlContent(String, java.util.Map)}.
 *
 * <p><strong>Background.</strong> The pre-fix implementation returned {@code
 * e.getLocalizedMessage()} from the {@code catch (Exception e)} block. The caller {@link
 * PSPageSidenavTag#doStartTag()} writes the returned string to the JSP output via {@code
 * out.print(...)}. If an exception occurred while loading the component URL, the JSP page would
 * render the internal exception message to the end user, potentially leaking class names, paths,
 * host information, or other details useful to an attacker (CWE-209: Information Exposure Through
 * an Error Message).
 *
 * <p>The fix logs the exception detail server-side and returns an empty string, keeping the
 * user-visible surface free of internal exception text. These tests exercise the catch path
 * directly: when {@code PSComponentUrls#getComponentUrl} throws, the returned string must not
 * contain any portion of the exception message.
 */
@DisplayName("PSPageBaseTag.getUrlContent - error-message-exposure (CWE-209) regression tests")
class PSPageBaseTagErrorMessageExposureTest {

  /**
   * Concrete subclass used to instantiate the abstract {@link PSPageBaseTag}. Only {@link
   * PSPageBaseTag#getUrlContent} is exercised; {@code doStartTag} / {@code doEndTag} are not
   * called.
   */
  private static final class TestPageBaseTag extends PSPageBaseTag {
    @Override
    public int doStartTag() {
      return 0;
    }

    @Override
    public int doEndTag() {
      return 0;
    }
  }

  /**
   * When the component-URL fetch throws, getUrlContent must not return the exception message. The
   * pre-fix code returned {@code e.getLocalizedMessage()}, which CodeQL flagged as an
   * error-message-exposure sink flowing into the JSP response body.
   */
  @Test
  @DisplayName("getUrlContent returns empty string (not the exception message) on internal failure")
  void testGetUrlContentDoesNotLeakExceptionMessage() throws PSInternalRequestCallException {
    PageContext pageContext = mock(PageContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    PSComponentUrls componentUrls = mock(PSComponentUrls.class);

    when(pageContext.getRequest()).thenReturn(request);
    when(request.getParameterMap()).thenReturn(new HashMap<>());
    when(componentUrls.getComponentUrl("flaky-component"))
        .thenThrow(
            new RuntimeException(
                "INTERNAL-DETAIL: /secret/path/to/resource.xml (do-not-leak-this-token-91827)"));

    TestPageBaseTag tag = new TestPageBaseTag();
    // Inject mocked state directly; the production setPageContext creates a
    // real PSComponentUrls (which does an internal HTTP request to load
    // sys_ComponentSupport/componentsupport.xml) and that is not feasible in a
    // unit test. m_context and m_urls are protected, so this works in the same
    // package as PSPageBaseTag.
    tag.m_context = pageContext;
    tag.m_urls = componentUrls;

    String result = tag.getUrlContent("flaky-component", null);

    assertNotNull(result, "getUrlContent must return a value, never null");
    assertEquals(
        "",
        result,
        "getUrlContent must return an empty string on internal failure rather than the"
            + " exception message; the JSP layer renders this value verbatim to the"
            + " browser, so any non-empty content is an error-message-exposure (CWE-209)"
            + " leak. Got: "
            + result);
    // Belt-and-braces: explicitly check the leak tokens are not in the response.
    assertFalse(
        result.contains("INTERNAL-DETAIL"),
        "internal exception prefix must not appear in JSP response body, got: " + result);
    assertFalse(
        result.contains("do-not-leak-this-token"),
        "internal exception body must not appear in JSP response body, got: " + result);
  }

  /**
   * Sanity check: the contract is "empty string on failure", not null. The JSP {@code
   * out.print(...)} would render the literal text "null" if we returned null, which is still better
   * than the exception message but worse than the empty string. Pin the contract.
   */
  @Test
  @DisplayName("getUrlContent contract: empty string on failure, never null")
  void testGetUrlContentContractIsEmptyStringNotNull() throws PSInternalRequestCallException {
    PageContext pageContext = mock(PageContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    PSComponentUrls componentUrls = mock(PSComponentUrls.class);

    when(pageContext.getRequest()).thenReturn(request);
    when(request.getParameterMap()).thenReturn(new HashMap<>());
    when(componentUrls.getComponentUrl(any()))
        .thenThrow(new IllegalStateException("upstream is down"));

    TestPageBaseTag tag = new TestPageBaseTag();
    tag.m_context = pageContext;
    tag.m_urls = componentUrls;

    String result = tag.getUrlContent("any-component", null);

    assertNotNull(result, "must be non-null");
    assertTrue(result.isEmpty(), "must be empty, got: [" + result + "]");
  }

  // Avoid an extra import: org.mockito.ArgumentMatchers.any
  private static <T> T any() {
    return org.mockito.ArgumentMatchers.any();
  }
}
