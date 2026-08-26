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
package com.percussion.rx.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.DeliveryErrorCodes;
import com.percussion.error.IPSErrorCode;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3860 (parent #2616 leftover): system/business delivery production sites throw typed {@link
 * DeliveryErrorCodes} via IPSErrorCode-aware {@link PSDeliveryException} constructors — not bare
 * {@code IPSDeliveryErrors} ints. Package-local ints are not flat-registered (Workflow/Assembly
 * collision); dual-write skip is {@link PSDeliveryException#isAuditable()} on the typed exception.
 */
@Tag("UnitTest")
public class PSDeliveryTypedErrorCodeSliceTest {

  @Test
  public void leftoverOperationalCodesSkipDualWrite() {
    IOException cause = new IOException("io");
    leftoverNonAuditable(
        new PSDeliveryException(DeliveryErrorCodes.ABORT_FAILURE, cause),
        DeliveryErrorCodes.ABORT_FAILURE);
    leftoverNonAuditable(
        new PSDeliveryException(DeliveryErrorCodes.DIR_CANT_CREATE, "missing-dir"),
        DeliveryErrorCodes.DIR_CANT_CREATE);
    leftoverNonAuditable(
        new PSDeliveryException(
            DeliveryErrorCodes.CREATE_DIR_W_EXCEPTION, cause, "missing-dir", "io"),
        DeliveryErrorCodes.CREATE_DIR_W_EXCEPTION);
    leftoverNonAuditable(
        new PSDeliveryException(
            DeliveryErrorCodes.COPY_FILE_FAILED, cause, "src", "dest", "io"),
        DeliveryErrorCodes.COPY_FILE_FAILED);
    leftoverNonAuditable(
        new PSDeliveryException(DeliveryErrorCodes.UNEXPECTED_ERROR, "boom"),
        DeliveryErrorCodes.UNEXPECTED_ERROR);
    leftoverNonAuditable(
        new PSDeliveryException(DeliveryErrorCodes.COULD_NOT_WRITE_TEMP, cause, "io"),
        DeliveryErrorCodes.COULD_NOT_WRITE_TEMP);
    leftoverNonAuditable(
        new PSDeliveryException(
            DeliveryErrorCodes.COULD_NOT_COPY_TO_AMAMZON, cause, "file-key", "bucket", "io"),
        DeliveryErrorCodes.COULD_NOT_COPY_TO_AMAMZON);
    leftoverNonAuditable(
        new PSDeliveryException(
            DeliveryErrorCodes.COULD_NOT_DELETE_FROM_AMAZON, cause, "file-key", "bucket", "io"),
        DeliveryErrorCodes.COULD_NOT_DELETE_FROM_AMAZON);
    leftoverNonAuditable(
        new PSDeliveryException(
            DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION,
            (Throwable) null,
            "Max Solr Errors Reached"),
        DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION);
    leftoverNonAuditable(
        new PSDeliveryException(DeliveryErrorCodes.CANNOT_DELIVER_NO_DELIVERYTYPE, "ftp"),
        DeliveryErrorCodes.CANNOT_DELIVER_NO_DELIVERYTYPE);
  }

  @Test
  public void decryptCredentialsRetainsTypedAuditableCode() {
    PSDeliveryException ex =
        new PSDeliveryException(
            DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS,
            new IllegalStateException("bad key"),
            "bad key");
    assertEquals(
        DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.numericCode(), ex.getErrorCode());
    assertSame(DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());
    assertEquals(IPSDeliveryErrors.COULD_NOT_DECRYPT_CREDENTIALS, ex.getErrorCode());
  }

  @Test
  public void connectivityCheckComparesTypedNumericCode() {
    PSDeliveryException unexpected =
        new PSDeliveryException(DeliveryErrorCodes.UNEXPECTED_ERROR, "login failed");
    assertEquals(IPSDeliveryErrors.UNEXPECTED_ERROR, unexpected.getErrorCode());
    assertEquals(
        DeliveryErrorCodes.UNEXPECTED_ERROR.numericCode(), unexpected.getErrorCode());
    assertSame(DeliveryErrorCodes.UNEXPECTED_ERROR, unexpected.getTypedErrorCode());
    assertFalse(unexpected.isAuditable());

    PSDeliveryException other =
        new PSDeliveryException(DeliveryErrorCodes.CANNOT_DELIVER_NO_DELIVERYTYPE, "ftp");
    assertTrue(other.getErrorCode() != DeliveryErrorCodes.UNEXPECTED_ERROR.numericCode());
  }

  @Test
  public void legacyIntConstructionHasNoTypedCode() {
    PSDeliveryException ex = new PSDeliveryException(IPSDeliveryErrors.UNEXPECTED_ERROR, "boom");
    assertEquals(IPSDeliveryErrors.UNEXPECTED_ERROR, ex.getErrorCode());
    assertNull(ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void typedConstructorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSDeliveryException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSDeliveryException((IPSErrorCode) null, "arg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSDeliveryException((IPSErrorCode) null, new IOException("x"), "arg"));
  }

  @Test
  public void leftoverNumericCodesMatchLegacyIpsDeliveryErrors() {
    assertEquals(IPSDeliveryErrors.ABORT_FAILURE, DeliveryErrorCodes.ABORT_FAILURE.numericCode());
    assertEquals(
        IPSDeliveryErrors.DIR_CANT_CREATE, DeliveryErrorCodes.DIR_CANT_CREATE.numericCode());
    assertEquals(
        IPSDeliveryErrors.CREATE_DIR_W_EXCEPTION,
        DeliveryErrorCodes.CREATE_DIR_W_EXCEPTION.numericCode());
    assertEquals(
        IPSDeliveryErrors.COPY_FILE_FAILED, DeliveryErrorCodes.COPY_FILE_FAILED.numericCode());
    assertEquals(
        IPSDeliveryErrors.UNEXPECTED_ERROR, DeliveryErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertEquals(
        IPSDeliveryErrors.COULD_NOT_WRITE_TEMP,
        DeliveryErrorCodes.COULD_NOT_WRITE_TEMP.numericCode());
    assertEquals(
        IPSDeliveryErrors.COULD_NOT_DECRYPT_CREDENTIALS,
        DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.numericCode());
    assertEquals(
        IPSDeliveryErrors.COULD_NOT_COPY_TO_AMAMZON,
        DeliveryErrorCodes.COULD_NOT_COPY_TO_AMAMZON.numericCode());
    assertEquals(
        IPSDeliveryErrors.COULD_NOT_DELETE_FROM_AMAZON,
        DeliveryErrorCodes.COULD_NOT_DELETE_FROM_AMAZON.numericCode());
    assertEquals(
        IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION,
        DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION.numericCode());
    assertEquals(
        IPSDeliveryErrors.CANNOT_DELIVER_NO_DELIVERYTYPE,
        DeliveryErrorCodes.CANNOT_DELIVER_NO_DELIVERYTYPE.numericCode());
  }

  private static void leftoverNonAuditable(PSDeliveryException ex, DeliveryErrorCodes expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.name());
    assertSame(expected, ex.getTypedErrorCode(), expected.name());
    assertFalse(ex.isAuditable(), expected.name());
    assertFalse(expected.isAuditable(), expected.name());
  }
}
