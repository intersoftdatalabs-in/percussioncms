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

package com.percussion.extensions.general;

import com.percussion.extension.PSSimpleJavaUdfExtension;
import com.percussion.server.IPSRequestContext;

/**
 * Base test class for simple Java UDF extensions.
 *
 * @author DougRand
 *     <p>To change the template for this generated type comment go to
 *     Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class PSSimpleJavaUdfBaseTest {
  /** Creates a new PSSimpleJavaUdfBaseTest. */
  public PSSimpleJavaUdfBaseTest() {}

  /**
   * Calls the UDF with no parameters.
   *
   * @param ext the UDF extension to invoke.
   * @param request the request context.
   * @return the UDF result.
   * @throws Exception if processing fails.
   */
  public Object callUDF(PSSimpleJavaUdfExtension ext, IPSRequestContext request) throws Exception {
    Object params[] = new Object[0];
    return ext.processUdf(params, request);
  }

  /**
   * Calls the UDF with one parameter.
   *
   * @param ext the UDF extension to invoke.
   * @param request the request context.
   * @param p the single parameter value.
   * @return the UDF result.
   * @throws Exception if processing fails.
   */
  public Object callUDF(PSSimpleJavaUdfExtension ext, IPSRequestContext request, Object p)
      throws Exception {
    Object params[] = new Object[] {p};
    return ext.processUdf(params, request);
  }

  /**
   * Calls the UDF with two parameters.
   *
   * @param ext the UDF extension to invoke.
   * @param request the request context.
   * @param p1 the first parameter value.
   * @param p2 the second parameter value.
   * @return the UDF result.
   * @throws Exception if processing fails.
   */
  public Object callUDF(
      PSSimpleJavaUdfExtension ext, IPSRequestContext request, Object p1, Object p2)
      throws Exception {
    Object params[] = new Object[] {p1, p2};
    return ext.processUdf(params, request);
  }

  /**
   * Calls the UDF with three parameters.
   *
   * @param ext the UDF extension to invoke.
   * @param request the request context.
   * @param p1 the first parameter value.
   * @param p2 the second parameter value.
   * @param p3 the third parameter value.
   * @return the UDF result.
   * @throws Exception if processing fails.
   */
  public Object callUDF(
      PSSimpleJavaUdfExtension ext, IPSRequestContext request, Object p1, Object p2, Object p3)
      throws Exception {
    Object params[] = new Object[] {p1, p2, p3};
    return ext.processUdf(params, request);
  }

  /**
   * Calls the UDF with four parameters.
   *
   * @param ext the UDF extension to invoke.
   * @param request the request context.
   * @param p1 the first parameter value.
   * @param p2 the second parameter value.
   * @param p3 the third parameter value.
   * @param p4 the fourth parameter value.
   * @return the UDF result.
   * @throws Exception if processing fails.
   */
  public Object callUDF(
      PSSimpleJavaUdfExtension ext,
      IPSRequestContext request,
      Object p1,
      Object p2,
      Object p3,
      Object p4)
      throws Exception {
    Object params[] = new Object[] {p1, p2, p3, p4};
    return ext.processUdf(params, request);
  }

  /**
   * Calls the UDF with five parameters.
   *
   * @param ext the UDF extension to invoke.
   * @param request the request context.
   * @param p1 the first parameter value.
   * @param p2 the second parameter value.
   * @param p3 the third parameter value.
   * @param p4 the fourth parameter value.
   * @param p5 the fifth parameter value.
   * @return the UDF result.
   * @throws Exception if processing fails.
   */
  public Object callUDF(
      PSSimpleJavaUdfExtension ext,
      IPSRequestContext request,
      Object p1,
      Object p2,
      Object p3,
      Object p4,
      Object p5)
      throws Exception {
    Object params[] = new Object[] {p1, p2, p3, p4, p5};
    return ext.processUdf(params, request);
  }
}
