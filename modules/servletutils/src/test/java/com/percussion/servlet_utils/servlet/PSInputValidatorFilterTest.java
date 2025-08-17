/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 * @author erikserating
 * @author adamgent
 */
public class PSInputValidatorFilterTest {

  private PSInputValidatorFilter filter;
  private MockHttpServletResponse response = new MockHttpServletResponse();
  private MockFilterChain filterChain = new MockFilterChain();
  private MockFilterConfig filterConfig;

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
            "/com/percussion/utils/servlet/" + getClass().getSimpleName() + ".properties");

    URL url = new File(filePath).toURI().toURL();
    setupFilter("true", url.toExternalForm());
  }

  private void setupFilter(String enable, String customConfigPath) throws ServletException {
    filter = new PSInputValidatorFilter();
    filterConfig = new MockFilterConfig();
    MockServletContext context = (MockServletContext) filterConfig.getServletContext();
    if (enable != null) context.addInitParameter(VALIDATOR_ENABLE_PROP_NAME, enable);
    if (customConfigPath != null)
      context.addInitParameter(VALIDATOR_CONFIG_RESOURCE_PROP_NAME, customConfigPath);
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
    String actualBody = response.getErrorMessage();
    assertTrue(actualBody.contains(badParam));
    if (goodParam != null) assertFalse(actualBody.contains(goodParam));
  }

  private void assertErrorStatus() {
    int actualStatus = response.getStatus();
    assertEquals(RESPONSE_ERROR_STATUS, actualStatus, "status should be");
  }

  private void assertOkStatus() {
    int actualStatus = response.getStatus();
    assertEquals(200, actualStatus, "status should be");
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
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (params != null) request.addParameters(paramMap);
    return request;
  }
}
