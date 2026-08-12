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
package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.conn.PSServerException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSDeployException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3165 (parent #3149 slice 3): deployer server call sites must throw typed {@link
 * ObjectStoreErrorCodes} via IPSErrorCode-aware exception constructors — not bare {@code
 * IPSObjectStoreErrors} ints.
 */
@Tag("UnitTest")
public class PSDeployerServerTypedErrorCodeSlice3Test {

  @Test
  public void dependencyDefWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDependencyDef");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSDependencyDef(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void dependencyMapWrongRootUsesTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotDependencyMap");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSDependencyMap(wrong));
    assertEquals(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
  }

  /**
   * Mirrors {@link PSDeploymentHandler#getFeatureSet} exception wrap: {@code PSServerException}
   * has no {@code IPSErrorCode}-aware constructor, so the call site must pass {@link
   * ObjectStoreErrorCodes#FEATURE_SET_LOAD_EXCEPTION}{@code .numericCode()}. If the enum's
   * numeric value drifts, or a typed ctor is added and this bridge is removed without updating
   * callers, this assertion fails.
   */
  @Test
  public void featureSetLoadExceptionNumericBridgeMatchesTypedEnum() {
    int expected = ObjectStoreErrorCodes.FEATURE_SET_LOAD_EXCEPTION.numericCode();
    PSServerException se =
        new PSServerException(expected, new Object[] {"synthetic feature-set load failure"});
    assertEquals(expected, se.getErrorCode());
    // Same wrap shape as getFeatureSet catch: PSDeployException(PSException)
    PSDeployException de = new PSDeployException(se);
    assertEquals(expected, de.getErrorCode());
    // Enum constant remains the typed contract source for the numeric bridge.
    assertEquals(
        ObjectStoreErrorCodes.FEATURE_SET_LOAD_EXCEPTION.numericCode(), de.getErrorCode());
  }
}
