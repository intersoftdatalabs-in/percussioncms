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

package com.percussion.deployer.error;

import static org.junit.jupiter.api.Assertions.*;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.conn.PSServerException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSDeployException;
import com.percussion.error.PSDeployNonUniqueException;
import com.percussion.error.PSLockedException;
import com.percussion.error.PSException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit test for the PSDeployException class. */
public class PSDeployExceptionTest {

  /**
   * Tests the Xml functions.
   *
   * @throws Exception if there are any errors.
   */
  @Test
  public void testXml() throws Exception {
    Object[] args1 = {"a", "b", "c"};
    PSDeployException ex1 = new PSDeployException(555, args1);
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el1 = ex1.toXml(doc);
    PSDeployException ex2 = new PSDeployException(el1);

    assertEquals(ex1.getErrorCode(), ex2.getErrorCode());
    Object[] args2 = ex2.getErrorArguments();
    assertNotNull(args2);
    assertEquals(args1.length, args2.length);
    for (int i = 0; i < args1.length; i++) {
      assertEquals(args1[i], args2[i]);
    }
    assertNull(ex2.getOriginalExceptionClass());

    Object[] args3 = {"a", "", "c"};
    PSServerException sEx1 = new PSServerException(123, args3);
    ex1 = new PSDeployException(sEx1);
    el1 = ex1.toXml(doc);
    ex2 = new PSDeployException(el1);
    assertEquals(ex1.getErrorCode(), ex2.getErrorCode());
    Object[] args4 = ex2.getErrorArguments();
    assertNotNull(args4);
    assertEquals(args3.length, args4.length);
    for (int i = 0; i < args3.length; i++) {
      assertEquals(args3[i], args4[i]);
    }
    assertEquals(ex2.getOriginalExceptionClass(), sEx1.getClass().getName());
  }

  /**
   * Test the <code>PSDeployNonUniqueException</code> class
   *
   * @throws Exception if there are any errors.
   */
  @Test
  public void testNonUnique() throws Exception {
    Object[] args1 = {"a", "b", "c"};
    PSDeployNonUniqueException ex1 = new PSDeployNonUniqueException(555, args1);
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el1 = ex1.toXml(doc);
    PSDeployNonUniqueException ex2 = new PSDeployNonUniqueException(el1);
    assertEquals(ex1.getErrorCode(), ex2.getErrorCode());
    assertEquals(ex1.getClass().getName(), ex2.getOriginalExceptionClass());

    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          Object[] args3 = {"a", "", "c"};
          PSServerException sEx1 = new PSServerException(123, args3);
          new PSDeployNonUniqueException(sEx1);
        });
  }

  @Test
  public void typedCatalogCodesRetainCodeAndSkipAudit() {
    PSDeployException noArg = new PSDeployException(DeploymentErrorCodes.NULL_INPUT_DOC);
    assertEquals(DeploymentErrorCodes.NULL_INPUT_DOC.numericCode(), noArg.getErrorCode());
    assertSame(DeploymentErrorCodes.NULL_INPUT_DOC, noArg.getTypedErrorCode());
    assertFalse(noArg.isAuditable());

    PSDeployException single =
        new PSDeployException(DeploymentErrorCodes.INVALID_REQUEST_TYPE, "PSXBad");
    assertEquals(DeploymentErrorCodes.INVALID_REQUEST_TYPE.numericCode(), single.getErrorCode());
    assertSame(DeploymentErrorCodes.INVALID_REQUEST_TYPE, single.getTypedErrorCode());
    assertFalse(single.isAuditable());

    Object[] args = {"widget", "99", "src"};
    PSDeployException array = new PSDeployException(DeploymentErrorCodes.MISSING_ID_MAPPING, args);
    assertEquals(DeploymentErrorCodes.MISSING_ID_MAPPING.numericCode(), array.getErrorCode());
    assertSame(DeploymentErrorCodes.MISSING_ID_MAPPING, array.getTypedErrorCode());
    assertArrayEquals(args, array.getErrorArguments());
    assertFalse(array.isAuditable());

    IllegalStateException cause = new IllegalStateException("boom");
    PSDeployException withCause =
        new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, cause, "detail");
    assertEquals(DeploymentErrorCodes.UNEXPECTED_ERROR.numericCode(), withCause.getErrorCode());
    assertSame(DeploymentErrorCodes.UNEXPECTED_ERROR, withCause.getTypedErrorCode());
    assertSame(cause, withCause.getCause());
    assertArrayEquals(new Object[] {"detail"}, withCause.getErrorArguments());
    assertFalse(withCause.isAuditable());
  }

  @Test
  public void typedLockCodeIsAuditableAndCopiedFromPsException() {
    PSDeployException lock = new PSDeployException(DeploymentErrorCodes.LOCK_ALREADY_HELD);
    assertTrue(lock.isAuditable());
    assertSame(DeploymentErrorCodes.LOCK_ALREADY_HELD, lock.getTypedErrorCode());

    PSException pe = new PSException(DeploymentErrorCodes.LOCK_ALREADY_HELD);
    PSDeployException wrapped = new PSDeployException(pe);
    assertSame(DeploymentErrorCodes.LOCK_ALREADY_HELD, wrapped.getTypedErrorCode());
    assertTrue(wrapped.isAuditable());
    assertEquals(pe.getClass().getName(), wrapped.getOriginalExceptionClass());
  }

  @Test
  public void typedConstructorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSDeployException((IPSErrorCode) null));
  }

  @Test
  public void typedLockedAndNonUniqueConstructorsRetainCodes() {
    Object[] lockArgs = {"alice", "2"};
    PSLockedException locked =
        new PSLockedException(DeploymentErrorCodes.LOCK_ALREADY_HELD, lockArgs);
    assertEquals(DeploymentErrorCodes.LOCK_ALREADY_HELD.numericCode(), locked.getErrorCode());
    assertSame(DeploymentErrorCodes.LOCK_ALREADY_HELD, locked.getTypedErrorCode());
    assertTrue(locked.isAuditable());

    PSDeployNonUniqueException nonUnique =
        new PSDeployNonUniqueException(DeploymentErrorCodes.ARCHIVE_REF_FOUND, "archive-1");
    assertEquals(DeploymentErrorCodes.ARCHIVE_REF_FOUND.numericCode(), nonUnique.getErrorCode());
    assertSame(DeploymentErrorCodes.ARCHIVE_REF_FOUND, nonUnique.getTypedErrorCode());
    assertFalse(nonUnique.isAuditable());
  }
}
