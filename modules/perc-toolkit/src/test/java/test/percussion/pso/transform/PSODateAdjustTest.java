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
package test.percussion.pso.transform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.pso.transform.PSODateAdjust;
import com.percussion.server.IPSRequestContext;
import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
public class PSODateAdjustTest {
  private static final Logger log = LogManager.getLogger(PSODateAdjustTest.class);

  PSODateAdjust cut;

  @Mock IPSRequestContext request;

  @Mock IPSExtensionDef def;

  @BeforeEach
  public void setUp() throws Exception {
    cut = new PSODateAdjust();

    final String[] pnames = {
      "sourceFieldName", "years", "months", "days", "hours", "minutes", "seconds"
    };
    Iterator<String> paramIterator = Arrays.asList(pnames).iterator();

    when(def.getRuntimeParameterNames()).thenReturn(paramIterator);
    when(request.getParameter("sourceFieldName")).thenReturn(null);
    when(request.getParameter("years")).thenReturn(null);
    when(request.getParameter("months")).thenReturn(null);
    when(request.getParameter("days")).thenReturn(null);
    when(request.getParameter("hours")).thenReturn(null);
    when(request.getParameter("minutes")).thenReturn(null);
    when(request.getParameter("seconds")).thenReturn(null);

    cut.init(def, new File("foo"));
  }

  @Test
  void testProcessUdf() {
    final Object[] params = new Object[] {"field1", "0", "0", "0", "0", "0", "0"};
    final Date dateNow = new Date();
    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    final String dateString = formatter.format(dateNow);

    when(request.getParameter("field1")).thenReturn(dateString);

    try {
      Timestamp result = (Timestamp) cut.processUdf(params, request);
      assertNotNull(result);
      long rl = result.getTime();
      long dl = dateNow.getTime();
      long diff = dl - rl;
      log.debug("Date diff is " + diff);
      assertTrue(Math.abs(diff) < 1000);

      verify(request).getParameter("field1");

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }

  @Test
  void testProcessUdfNullDate() {
    final Object[] params = new Object[] {"field1", "0", "0", "0", "0", "0", "0"};
    final Date dateNow = new Date();

    when(request.getParameter("field1")).thenReturn(null);

    try {
      Timestamp result = (Timestamp) cut.processUdf(params, request);
      assertNotNull(result);
      long rl = result.getTime();
      long dl = dateNow.getTime();
      long diff = dl - rl;
      log.debug("Date diff is " + diff);
      assertTrue(Math.abs(diff) < 1000);

      verify(request).getParameter("field1");

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }

  @Test
  void testProcessUdfBlankDate() {
    final Object[] params = new Object[] {"", "0", "0", "0", "0", "0", "0"};
    final Date dateNow = new Date();
    try {
      Timestamp result = (Timestamp) cut.processUdf(params, request);
      assertNotNull(result);
      long rl = result.getTime();
      long dl = dateNow.getTime();
      long diff = dl - rl;
      log.debug("Date diff is " + diff);
      assertTrue(Math.abs(diff) < 1000);

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }

  @Test
  void testProcessUdf1Year() {
    final Object[] params = new Object[] {"", "1", "0", "0", "0", "0", "0"};
    final Date dateNow = new Date();
    try {
      Timestamp result = (Timestamp) cut.processUdf(params, request);
      assertNotNull(result);
      long rl = result.getTime();
      long dl = dateNow.getTime();
      long diff = dl - rl;
      log.debug("Date diff is " + diff);
      assertTrue(Math.abs(diff) > 10000);

    } catch (PSConversionException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }
}
