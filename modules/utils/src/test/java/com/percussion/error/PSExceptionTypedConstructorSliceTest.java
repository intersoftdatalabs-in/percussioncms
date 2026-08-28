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
package com.percussion.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.util.UtilErrorCode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3939 (parent #2616): additive {@link IPSErrorCode} constructors on utils exception types
 * used by leftover {@code com.percussion.data} production sites retain typed codes and skip
 * dual-write when the catalog is non-auditable.
 */
@Tag("UnitTest")
class PSExceptionTypedConstructorSliceTest {

  @Test
  void evaluationIllegalArgumentAndSqlExceptionsRetainTypedNonAuditableCodes() {
    PSEvaluationException eval = new PSEvaluationException(UtilErrorCode.BASE64_DECODING_EXCEPTION);
    assertSame(UtilErrorCode.BASE64_DECODING_EXCEPTION, eval.getTypedErrorCode());
    assertEquals(UtilErrorCode.BASE64_DECODING_EXCEPTION.numericCode(), eval.getErrorCode());
    assertFalse(eval.isAuditable());

    PSEvaluationException evalArg =
        new PSEvaluationException(UtilErrorCode.BASE64_DECODING_EXCEPTION, "blob");
    assertSame(UtilErrorCode.BASE64_DECODING_EXCEPTION, evalArg.getTypedErrorCode());

    PSIllegalArgumentException illegal =
        new PSIllegalArgumentException(UtilErrorCode.COLLECTION_CLASS_NOT_FOUND, "missing");
    assertSame(UtilErrorCode.COLLECTION_CLASS_NOT_FOUND, illegal.getTypedErrorCode());
    assertFalse(illegal.isAuditable());

    PSSqlException sql =
        new PSSqlException(UtilErrorCode.RECEIVE_DATA_ERROR, "host", "08001");
    assertSame(UtilErrorCode.RECEIVE_DATA_ERROR, sql.getTypedErrorCode());
    assertEquals(UtilErrorCode.RECEIVE_DATA_ERROR.numericCode(), sql.getErrorCode());
    assertFalse(sql.isAuditable());

    PSSqlException noArg = new PSSqlException(UtilErrorCode.POST_DATA_ERROR, "08000");
    assertSame(UtilErrorCode.POST_DATA_ERROR, noArg.getTypedErrorCode());
  }

  @Test
  void typedConstructorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSEvaluationException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSIllegalArgumentException((IPSErrorCode) null));
    assertThrows(IllegalArgumentException.class, () -> new PSSqlException((IPSErrorCode) null, "25000"));
  }
}
