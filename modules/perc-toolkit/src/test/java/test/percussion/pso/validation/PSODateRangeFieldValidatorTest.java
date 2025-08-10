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
package test.percussion.pso.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
// ...existing code...

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.pso.validation.PSODateRangeFieldValidator;
import com.percussion.server.IPSRequestContext;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// REFACTORED: CP-JAVA11
@ExtendWith(MockitoExtension.class)
public class PSODateRangeFieldValidatorTest {
  private static final Logger log = LogManager.getLogger(PSODateRangeFieldValidatorTest.class);

  @Mock IPSRequestContext request;

  @Mock IPSExtensionDef extDef;

  PSODateRangeFieldValidator cut;

  @BeforeEach
  public void setUp() throws Exception {
    cut = new PSODateRangeFieldValidator();

    final String[] pnames = {
      PSODateRangeFieldValidator.CURRENT_FIELD,
      PSODateRangeFieldValidator.SOURCE_FIELD,
      PSODateRangeFieldValidator.MIN_DAYS,
      PSODateRangeFieldValidator.MAX_DAYS
    };

    Iterator<String> paramIterator = Arrays.asList(pnames).iterator();
    when(extDef.getRuntimeParameterNames()).thenReturn(paramIterator);
    when(request.getParameter(PSODateRangeFieldValidator.CURRENT_FIELD)).thenReturn(null);
    when(request.getParameter(PSODateRangeFieldValidator.SOURCE_FIELD)).thenReturn(null);
    when(request.getParameter(PSODateRangeFieldValidator.MIN_DAYS)).thenReturn(null);
    when(request.getParameter(PSODateRangeFieldValidator.MAX_DAYS)).thenReturn(null);

    cut.init(extDef, new File("foo"));
  }

  @Test
  public final void testProcessUdf() {
    log.debug("Testing current date in interval... expecting result=true");
    final Date sourceDate = new Date();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    final String sourceStr = format.format(sourceDate);

    Calendar cal = Calendar.getInstance();
    cal.setTime(sourceDate);
    cal.add(Calendar.DAY_OF_MONTH, 30);
    final Date testDate = cal.getTime();
    final String testStr = format.format(testDate);

    final String[] params = {testStr, "field1", "10", "60"};

    try {
      when(request.getParameter("field1")).thenReturn(sourceStr);

      Boolean result = (Boolean) cut.processUdf(params, request);

      assertNotNull(result);
      log.debug("Result is " + result);
      assertTrue(result.booleanValue());

      verify(request).getParameter("field1");

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception Caught");
    }
  }

  @Test
  public final void testProcessUdfAfter() {
    log.debug("testing current date after interval... expect result=false");
    final Date sourceDate = new Date();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    final String sourceStr = format.format(sourceDate);

    Calendar cal = Calendar.getInstance();
    cal.setTime(sourceDate);
    cal.add(Calendar.DAY_OF_MONTH, 90);
    final Date testDate = cal.getTime();
    final String testStr = format.format(testDate);

    final String[] params = {testStr, "field1", "0", "60"};

    try {
      when(request.getParameter("field1")).thenReturn(sourceStr);

      Boolean result = (Boolean) cut.processUdf(params, request);

      assertNotNull(result);
      log.debug("Result is " + result);
      assertFalse(result.booleanValue());

      verify(request).getParameter("field1");

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception Caught");
    }
  }

  @Test
  public final void testProcessUdfBefore() {
    log.debug("testing current date before interval... expect result=false");
    final Date sourceDate = new Date();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    final String sourceStr = format.format(sourceDate);

    Calendar cal = Calendar.getInstance();
    cal.setTime(sourceDate);
    cal.add(Calendar.DAY_OF_MONTH, 30);
    final Date testDate = cal.getTime();
    final String testStr = format.format(testDate);

    final String[] params = {testStr, "field1", "60", "90"};

    try {
      when(request.getParameter("field1")).thenReturn(sourceStr);

      Boolean result = (Boolean) cut.processUdf(params, request);

      assertNotNull(result);
      log.debug("Result is " + result);
      assertFalse(result.booleanValue());

      verify(request).getParameter("field1");

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception Caught");
    }
  }
}
