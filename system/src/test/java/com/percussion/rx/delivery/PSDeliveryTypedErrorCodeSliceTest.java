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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.DeliveryErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.rx.delivery.IPSDeliveryResult.Outcome;
import com.percussion.rx.delivery.impl.PSBaseDeliveryHandler;
import com.percussion.utils.guid.IPSGuid;
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

  @Test
  public void typedGetExceptionResultFailsWithTypedMessageAndItemIds() {
    var handler = new TestDeliveryHandler();
    var id = mock(IPSGuid.class);
    var item = mockItem(id, 11L, 22L, 3);
    var cause = new IOException("disk full");

    IPSDeliveryResult failed =
        handler.exposeGetExceptionResult(item, DeliveryErrorCodes.COULD_NOT_WRITE_TEMP, cause);

    assertEquals(Outcome.FAILED, failed.getOutcome());
    assertSame(id, failed.getId());
    assertEquals(11L, failed.getJobId());
    assertEquals(22L, failed.getReferenceId());
    assertEquals(3, failed.getDeliveryContext());
    assertNull(failed.getUnpublishData());

    var expected =
        new PSDeliveryException(DeliveryErrorCodes.COULD_NOT_WRITE_TEMP, cause, "disk full");
    assertEquals(expected.getLocalizedMessage(), failed.getFailureMessage());
    assertSame(DeliveryErrorCodes.COULD_NOT_WRITE_TEMP, expected.getTypedErrorCode());

    IPSDeliveryResult fromInt =
        handler.exposeGetExceptionResult(item, IPSDeliveryErrors.COULD_NOT_WRITE_TEMP, cause);
    assertEquals(fromInt.getFailureMessage(), failed.getFailureMessage());
  }

  @Test
  public void typedGetExceptionResultRejectsNullsAndBlankCauseUsesClassName() {
    var handler = new TestDeliveryHandler();
    var item = mockItem(mock(IPSGuid.class), 1L, 2L, 0);
    var cause = new IOException("x");

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.exposeGetExceptionResult(item, null, cause));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            handler.exposeGetExceptionResult(null, DeliveryErrorCodes.UNEXPECTED_ERROR, cause));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            handler.exposeGetExceptionResult(
                item, DeliveryErrorCodes.UNEXPECTED_ERROR, null));

    var blankCause = new RuntimeException("   ");
    IPSDeliveryResult failed =
        handler.exposeGetExceptionResult(item, DeliveryErrorCodes.UNEXPECTED_ERROR, blankCause);
    var expected =
        new PSDeliveryException(
            DeliveryErrorCodes.UNEXPECTED_ERROR, blankCause, RuntimeException.class.getName());
    assertEquals(Outcome.FAILED, failed.getOutcome());
    assertEquals(expected.getLocalizedMessage(), failed.getFailureMessage());
  }

  private static IPSDeliveryItem mockItem(
      IPSGuid id, long jobId, long referenceId, int deliveryContext) {
    var item = mock(IPSDeliveryItem.class);
    when(item.getId()).thenReturn(id);
    when(item.getJobId()).thenReturn(jobId);
    when(item.getReferenceId()).thenReturn(referenceId);
    when(item.getDeliveryContext()).thenReturn(deliveryContext);
    return item;
  }

  private static void leftoverNonAuditable(PSDeliveryException ex, DeliveryErrorCodes expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.name());
    assertSame(expected, ex.getTypedErrorCode(), expected.name());
    assertFalse(ex.isAuditable(), expected.name());
    assertFalse(expected.isAuditable(), expected.name());
  }

  /**
   * Exposes {@link PSBaseDeliveryHandler} {@code getExceptionResult} overloads for #3860 slice
   * coverage. Delivery/removal are unused.
   */
  private static final class TestDeliveryHandler extends PSBaseDeliveryHandler {
    IPSDeliveryResult exposeGetExceptionResult(
        IPSDeliveryItem result, IPSErrorCode errorCode, Throwable th) {
      return getExceptionResult(result, errorCode, th);
    }

    IPSDeliveryResult exposeGetExceptionResult(
        IPSDeliveryItem result, int errorCode, Throwable th) {
      return getExceptionResult(result, errorCode, th);
    }

    @Override
    protected IPSDeliveryResult doDelivery(Item item, long jobId, String location) {
      throw new UnsupportedOperationException("not used");
    }

    @Override
    protected IPSDeliveryResult doRemoval(Item item, long jobId, String location) {
      throw new UnsupportedOperationException("not used");
    }
  }
}
