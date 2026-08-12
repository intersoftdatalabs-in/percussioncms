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
package com.percussion.design.objectstore.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.design.objectstore.IPSObjectStoreErrors;
import com.percussion.design.objectstore.PSUnknownDocTypeException;
import org.junit.jupiter.api.Test;

/**
 * Issue #3177 / parent #2616: {@link PSXmlObjectStoreHandler} no longer implements bare {@link
 * IPSObjectStoreErrors}; XML doc validation throws use typed {@link ObjectStoreErrorCodes}.
 *
 * <p>Full handler request paths need server wiring (covered by existing suite). This test locks the
 * class contract and the high-frequency typed {@link PSUnknownDocTypeException} ctor path used at
 * the bulk of the former unqualified sites.
 */
public class PSXmlObjectStoreHandlerTypedErrorCodeTest {

  @Test
  public void handlerDoesNotImplementBareIpsObjectStoreErrors() {
    assertFalse(IPSObjectStoreErrors.class.isAssignableFrom(PSXmlObjectStoreHandler.class));
    assertTrue(IPSObjectStoreHandler.class.isAssignableFrom(PSXmlObjectStoreHandler.class));
  }

  @Test
  public void unknownDocTypeNullUsesTypedObjectStoreErrorCode() {
    PSUnknownDocTypeException ex =
        assertThrows(
            PSUnknownDocTypeException.class,
            () -> {
              throw new PSUnknownDocTypeException(
                  ObjectStoreErrorCodes.XML_ELEMENT_NULL, "PSXApplication");
            });
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_NULL, ex.getTypedErrorCode());
  }

  @Test
  public void unknownDocTypeWrongTypeUsesTypedObjectStoreErrorCode() {
    Object[] args = {"expectedRoot", "actualRoot"};
    PSUnknownDocTypeException ex =
        assertThrows(
            PSUnknownDocTypeException.class,
            () -> {
              throw new PSUnknownDocTypeException(
                  ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, args);
            });
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }
}
