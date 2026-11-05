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
package com.percussion.webservices.transformation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.TransformationErrorCodes;
import com.percussion.webservices.transformation.converter.PSConverter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3585 (parent #2616): leftover transformation call sites use typed {@link
 * TransformationErrorCodes} (not bare {@code IPSTransformationErrors} ints). Package-local int 1 is
 * not flat-registered (Workflow collision).
 */
@Tag("UnitTest")
public class PSTransformationTypedErrorCodeSliceTest {

  @Test
  public void noConverterFoundRetainsTypedNonAuditableCode() {
    PSTransformationException ex =
        new PSTransformationException(
            TransformationErrorCodes.NO_CONVERTER_FOUND, "com.example.Missing");
    assertEquals(1, ex.getErrorCode());
    assertSame(TransformationErrorCodes.NO_CONVERTER_FOUND, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void typedConstructorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSTransformationException(null));
  }

  @Test
  public void missingConverterMessageUsesTypedCatalogCode() {
    BeanUtilsBean beans = new BeanUtilsBean();
    beans.getConvertUtils().deregister();
    ProbeConverter probe = new ProbeConverter(beans);
    ConversionException ex =
        assertThrows(ConversionException.class, () -> probe.lookup(UnregisteredType.class));
    assertTrue(ex.getMessage().contains("UnregisteredType") || ex.getMessage().contains("1:"));
  }

  /** Exposes protected {@link PSConverter#getConverter(Class)} for the leftover throw site. */
  private static final class ProbeConverter extends PSConverter {
    ProbeConverter(BeanUtilsBean beanUtils) {
      super(beanUtils);
    }

    Converter lookup(Class<?> type) {
      return getConverter(type);
    }
  }

  private static final class UnregisteredType {}
}
