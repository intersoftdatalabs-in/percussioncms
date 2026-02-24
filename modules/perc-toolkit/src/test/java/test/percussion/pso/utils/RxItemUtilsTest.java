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
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.IPSItemAccessor;
import com.percussion.cms.objectstore.PSBinaryValue;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSItemFieldMeta;
import com.percussion.pso.utils.RxItemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RxItemUtilsTest {
  private static final Logger log = LogManager.getLogger(RxItemUtilsTest.class);


  @Test
  public final void testIsBinaryFieldTrue() {
    IPSItemAccessor item = mock(IPSItemAccessor.class);
    PSItemField fld = mock(PSItemField.class);
    PSItemFieldMeta meta = mock(PSItemFieldMeta.class);

    when(item.getFieldByName("a")).thenReturn(fld);
    when(fld.getItemFieldMeta()).thenReturn(meta);
    when(meta.getBackendDataType()).thenReturn(PSItemFieldMeta.DATATYPE_BINARY);

    boolean result = RxItemUtils.isBinaryField(item, "a");
    assertTrue(result);
    verify(item).getFieldByName("a");
    verify(fld).getItemFieldMeta();
    verify(meta).getBackendDataType();
  }

  @Test
  public final void testIsBinaryFieldFalse() {
    IPSItemAccessor item = mock(IPSItemAccessor.class);
    PSItemField fld = mock(PSItemField.class);
    PSItemFieldMeta meta = mock(PSItemFieldMeta.class);

    when(item.getFieldByName("a")).thenReturn(fld);
    when(fld.getItemFieldMeta()).thenReturn(meta);
    when(meta.getBackendDataType()).thenReturn(PSItemFieldMeta.DATATYPE_TEXT);

    boolean result = RxItemUtils.isBinaryField(item, "a");
    assertFalse(result);
    verify(item).getFieldByName("a");
    verify(fld).getItemFieldMeta();
    verify(meta).getBackendDataType();
  }

  @Test
  public final void testGetFieldBinary() {
    final IPSItemAccessor item = mock(IPSItemAccessor.class);
    final PSItemField fld = mock(PSItemField.class);
    final PSBinaryValue value = mock(PSBinaryValue.class);
    final byte[] myArray = new byte[100];
    try {
      when(item.getFieldByName("a")).thenReturn(fld);
      when(fld.getValue()).thenReturn(value);
      when(value.getValue()).thenReturn(myArray);

      Object o = RxItemUtils.getFieldBinary(item, "a");
      assertNotNull(o);
    } catch (PSCmsException ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception");
    }
  }
}
