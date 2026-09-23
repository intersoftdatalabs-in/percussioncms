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
package com.percussion.sitemanage.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.percussion.share.service.PSSiteCopyUtils;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/** Maps site-copy failures to 400 / 403 / 409 without echoing the cause text. */
class PSSiteCopyHttpStatusTest {

  @Test
  void missingSourceIsBadRequest() {
    PSSiteCopyStatusException ex =
        PSSiteCopyHttpStatus.toException(
            new RuntimeException("Unable to copy site, failed to find site with name: Missing"));
    assertEquals(400, ex.status());
    assertEquals("Site copy request was rejected.", ex.getMessage());
    assertFalse(ex.getMessage().contains("Missing"));
  }

  @Test
  void notAuthorizedIsForbidden() throws Exception {
    Exception validation =
        catchValidation(
            () ->
                validateParameters("copy")
                    .reject("site.saveNotAuthorized", "You are not authorized to create a site")
                    .throwIfInvalid());
    assertEquals(403, PSSiteCopyHttpStatus.toException(validation).status());
  }

  @Test
  void duplicateNameIsConflict() throws Exception {
    Exception validation =
        catchValidation(
            () ->
                validateParameters("save")
                    .reject("site.exists", "Cannot create a site because a site named X already exists.")
                    .throwIfInvalid());
    assertEquals(Response.Status.CONFLICT, PSSiteCopyHttpStatus.statusFor(validation));
  }

  @Test
  void copyInProgressIsConflict() throws Exception {
    Exception validation =
        catchValidation(
            () ->
                validateParameters("copy")
                    .reject(PSSiteCopyUtils.SITE_COPY_KEY, PSSiteCopyUtils.CAN_NOT_COPY_SITE)
                    .throwIfInvalid());
    assertEquals(409, PSSiteCopyHttpStatus.toException(validation).status());
  }

  @Test
  void rollbackWrappingDuplicateNameIsConflict() throws Exception {
    Exception validation =
        catchValidation(
            () ->
                validateParameters("save")
                    .reject("site.exists", "a site named X already exists")
                    .throwIfInvalid());
    var wrapped =
        new org.springframework.transaction.UnexpectedRollbackException(
            "Transaction rolled back because it has been marked as rollback-only", validation);
    assertEquals(409, PSSiteCopyHttpStatus.toException(wrapped).status());
  }

  @Test
  void unexpectedFailureStays500() {
    assertEquals(
        500,
        PSSiteCopyHttpStatus.toException(new IllegalStateException("disk full")).status());
  }

  private static Exception catchValidation(RunnableThrowing action) throws Exception {
    try {
      action.run();
    } catch (Exception e) {
      return e;
    }
    throw new AssertionError("expected validation failure");
  }

  @FunctionalInterface
  private interface RunnableThrowing {
    void run() throws Exception;
  }
}
