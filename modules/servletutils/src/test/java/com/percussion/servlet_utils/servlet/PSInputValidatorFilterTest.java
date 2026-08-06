/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.servlet_utils.servlet;

import static com.percussion.servlet_utils.servlet.PSInputValidatorFilter.RESPONSE_ERROR_STATUS;
import static com.percussion.servlet_utils.servlet.PSInputValidatorFilter.VALIDATOR_CONFIG_RESOURCE_PROP_NAME;
import static com.percussion.servlet_utils.servlet.PSInputValidatorFilter.VALIDATOR_ENABLE_PROP_NAME;
import static com.percussion.util.PSResourceUtils.getResourcePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author erikserating
 * @author adamgent
 */
@ExtendWith(MockitoExtension.class)
public class PSInputValidatorFilterTest {

  private PSInputValidatorFilter filter;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @Mock private FilterConfig filterConfig;

  private StringWriter responseWriter;
  private int responseStatus;
  private List<String> responseHeaders;

  /*
   * (non-Javadoc)
   *
   * @see junit.framework.TestCase#setUp()
   */
  @BeforeEach
  protected void setUp() throws Exception {
    String filePath =
        getResourcePath(
            PSInputValidatorFilterTest.class,
            "/com/percussion/servlet_utils/servlet/" + getClass().getSimpleName() + ".properties");

    URL url = new File(filePath).toURI().toURL();
    setupFilter("true", url.toExternalForm());
  }

  private void setupFilter(String enable, String customConfigPath) throws ServletException {
    filter = new PSInputValidatorFilter();

    // Setup mock response
    responseWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(responseWriter);
    responseStatus = 200;
    responseHeaders = new ArrayList<>();

    try {
      when(response.getWriter()).thenReturn(printWriter);
    } catch (java.io.IOException e) {
      // This shouldn't happen during mock setup
      throw new RuntimeException(e);
    }
    when(response.getStatus()).thenAnswer(invocation -> responseStatus);

    // Capture setStatus calls
    doNothing().when(response).setStatus(anyInt());

    // Setup mock FilterConfig with ServletContext
    ServletContext mockServletContext = mock(ServletContext.class);
    when(filterConfig.getServletContext()).thenReturn(mockServletContext);

    if (enable != null) {
      when(mockServletContext.getInitParameter(VALIDATOR_ENABLE_PROP_NAME)).thenReturn(enable);
    }
    if (customConfigPath != null) {
      when(mockServletContext.getInitParameter(VALIDATOR_CONFIG_RESOURCE_PROP_NAME))
          .thenReturn(customConfigPath);
    }

    filter.init(filterConfig);
  }

  // Parameter name constants from test properties
  private static final String PARAM_TEST_NUMERIC = "testNumeric";
  private static final String PARAM_TEST_BOOLEAN = "testBool";
  private static final String PARAM_TEST_GUID = "testGuid";
  private static final String PARAM_TEST_MULTI_NO = "testNoCcNoLtGtNoQ";
  private static final String PARAM_TEST_NOLTGT = "testNoLtGt";
  private static final String PARAM_TEST_NOCC = "testNoCc";
  private static final String PARAM_TEST_NOQUOTES = "testNoQ";
  private static final String PARAM_TEST_NORESTRICT = "testNoRestrict";
  private static final String PARAM_TEST_SINGLE_REGEX = "testSingleRegex"; // [^Z]*
  private static final String PARAM_TEST_MULTI_REGEX = "testMultiRegex"; // [^X]* and [^Y]*
  private static final String PARAM_TEST_REGEX_NOLTGT = "testRegexNoLtGt"; // [^T]*

  /*
   * (non-Javadoc)
   *
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterEach
  protected void tearDown() throws Exception {}

  private void assertErrorMessage(String badParam, String goodParam) {
    String actualBody = responseWriter.toString();
    assertTrue(actualBody.contains(badParam), "Response should contain: " + badParam);
    if (goodParam != null)
      assertFalse(actualBody.contains(goodParam), "Response should not contain: " + goodParam);
  }

  private void assertErrorStatus() {
    int actualStatus = responseStatus;
    assertEquals(RESPONSE_ERROR_STATUS, actualStatus, "status should be 422");
  }

  private void assertOkStatus() {
    int actualStatus = responseStatus;
    assertEquals(200, actualStatus, "status should be 200");
  }

  @Test
  @Disabled
  public void testRestrictToGuid() throws Exception {
    HttpServletRequest req =
        createMockRequest("sys_contentid", "NonGuidValue", "sys_folderid", "334");

    filter.doFilter(req, response, filterChain);
    assertErrorMessage("sys_contentid", "sys_folderid");
    assertErrorStatus();
  }

  @Test
  @Disabled
  public void testNoRestrict() throws Exception {
    HttpServletRequest req = createMockRequest(PARAM_TEST_NORESTRICT, "NonGuidValue <");
    filter.doFilter(req, response, filterChain);
    assertOkStatus();
  }

  @Test
  @Disabled
  public void testInvalidParamNameRemoval() throws Exception {
    HttpServletRequest req =
        createMockRequest("<script>alert('')</script>", "NonGuidValue", "sys_folderid", "334");
    filter.doFilter(req, response, filterChain);
    assertErrorStatus();
  }

  //
  @Test
  @Disabled
  public void testNoControlChars() throws Exception {
    // If this test fails then the custom property files
    // is probably not loading.

    HttpServletRequest req = createMockRequest(PARAM_TEST_NOCC, "\u0000NonGuidValue");
    filter.doFilter(req, response, filterChain);
    assertErrorStatus();
    assertErrorMessage(PARAM_TEST_NOCC, null);
  }

  private HttpServletRequest createMockRequest(String... params) {
    Map<String, String> paramMap = new HashMap<String, String>();
    for (int i = 0; i < (params.length - 1); i += 2) {
      paramMap.put(params[i], params[i + 1]);
    }

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameterNames()).thenReturn(new Vector<>(paramMap.keySet()).elements());

    // For each parameter, setup the mock to return its value
    for (Map.Entry<String, String> entry : paramMap.entrySet()) {
      when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
      when(request.getParameterValues(entry.getKey())).thenReturn(new String[] {entry.getValue()});
    }

    return request;
  }
}
