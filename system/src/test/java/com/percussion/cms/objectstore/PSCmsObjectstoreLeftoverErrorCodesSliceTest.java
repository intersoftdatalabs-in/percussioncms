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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.RemoteErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.client.PSRemoteAgent;
import com.percussion.cms.objectstore.client.PSRemoteCataloger;
import com.percussion.cms.objectstore.client.PSRemoteException;
import com.percussion.cms.objectstore.client.PSRemoteProcessor;
import com.percussion.cms.objectstore.client.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSNotFoundException;
import com.percussion.server.IPSServerErrors;
import com.percussion.util.IPSRemoteRequester;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Issue #3884 (parent #2616): leftover {@code cms.objectstore} (non-server) + {@code
 * objectstore.client} production sites throw typed {@code *ErrorCodes} (not bare {@code
 * IPS*Errors} ints). Dual-write is skipped where the catalog is non-auditable.
 */
@Tag("UnitTest")
class PSCmsObjectstoreLeftoverErrorCodesSliceTest {

  @Test
  void cmsAndServerAndRemotePeersMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSCmsErrors.INVALID_CONTENT_TYPE_ID, CmsErrorCodes.INVALID_CONTENT_TYPE_ID.numericCode());
    assertEquals(IPSCmsErrors.KEY_NOT_ASSIGNED, CmsErrorCodes.KEY_NOT_ASSIGNED.numericCode());
    assertEquals(IPSCmsErrors.KEY_MISMATCH, CmsErrorCodes.KEY_MISMATCH.numericCode());
    assertEquals(
        IPSCmsErrors.TOO_MANY_FOREIGN_KEY_PARTS,
        CmsErrorCodes.TOO_MANY_FOREIGN_KEY_PARTS.numericCode());
    assertEquals(
        IPSCmsErrors.TOO_FEW_FOREIGN_KEY_PARTS,
        CmsErrorCodes.TOO_FEW_FOREIGN_KEY_PARTS.numericCode());
    assertEquals(IPSCmsErrors.MISSING_LOOKUP_KEY, CmsErrorCodes.MISSING_LOOKUP_KEY.numericCode());
    assertEquals(
        IPSCmsErrors.COMPONENT_INSTANTIATION_ERROR,
        CmsErrorCodes.COMPONENT_INSTANTIATION_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_ENTRY_CLASSNAME, CmsErrorCodes.INVALID_ENTRY_CLASSNAME.numericCode());
    assertEquals(IPSCmsErrors.KEY_PARTS_NOT_MATCH, CmsErrorCodes.KEY_PARTS_NOT_MATCH.numericCode());
    assertEquals(
        IPSCmsErrors.MISMATCH_BETWEEN_KEY_AND_DATA,
        CmsErrorCodes.MISMATCH_BETWEEN_KEY_AND_DATA.numericCode());
    assertEquals(IPSCmsErrors.INVALID_CHILD_TYPE, CmsErrorCodes.INVALID_CHILD_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.DATA_EXTRACTION_ERROR_NULL_DATAPIPE,
        CmsErrorCodes.DATA_EXTRACTION_ERROR_NULL_DATAPIPE.numericCode());
    assertEquals(
        IPSCmsErrors.UNSUPPORTED_COMPONENT_TYPE,
        CmsErrorCodes.UNSUPPORTED_COMPONENT_TYPE.numericCode());
    assertEquals(IPSCmsErrors.MISSING_PROPERTY, CmsErrorCodes.MISSING_PROPERTY.numericCode());
    assertEquals(
        IPSCmsErrors.SERIALIZED_COMPONENTS_WRONG_XML_DOC,
        CmsErrorCodes.SERIALIZED_COMPONENTS_WRONG_XML_DOC.numericCode());
    assertEquals(
        IPSCmsErrors.PROCESSOR_CONFIG_MISSING,
        CmsErrorCodes.PROCESSOR_CONFIG_MISSING.numericCode());
    assertEquals(IPSCmsErrors.XML_PARSING_ERROR, CmsErrorCodes.XML_PARSING_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.PROCESSOR_CONFIG_IO_ERROR,
        CmsErrorCodes.PROCESSOR_CONFIG_IO_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.DUPLICATE_PROCESSOR_PROPERTY,
        CmsErrorCodes.DUPLICATE_PROCESSOR_PROPERTY.numericCode());
    assertEquals(
        IPSCmsErrors.DUPLICATE_PROCESSOR_ENTRY,
        CmsErrorCodes.DUPLICATE_PROCESSOR_ENTRY.numericCode());
    assertEquals(IPSCmsErrors.NO_PROCESSOR_ENTRY, CmsErrorCodes.NO_PROCESSOR_ENTRY.numericCode());
    assertEquals(
        IPSCmsErrors.PROCESSOR_NO_SUCH_METHOD,
        CmsErrorCodes.PROCESSOR_NO_SUCH_METHOD.numericCode());
    assertEquals(
        IPSCmsErrors.PROCESSOR_INSTANTIATION_ERROR,
        CmsErrorCodes.PROCESSOR_INSTANTIATION_ERROR.numericCode());
    assertEquals(IPSCmsErrors.COMM_ERROR_WITH_SERVER, CmsErrorCodes.COMM_ERROR_WITH_SERVER.numericCode());
    assertEquals(
        IPSCmsErrors.SAX_PROCESSING_EXCEPTION,
        CmsErrorCodes.SAX_PROCESSING_EXCEPTION.numericCode());
    assertEquals(IPSCmsErrors.UNEXPECTED_ERROR, CmsErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.CONTENT_TYPE_CANNOT_BE_OPENED,
        CmsErrorCodes.CONTENT_TYPE_CANNOT_BE_OPENED.numericCode());
    assertEquals(
        IPSCmsErrors.UNEXPECTED_CATALOG_ERROR,
        CmsErrorCodes.UNEXPECTED_CATALOG_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_AA_RELATIONSHIP, CmsErrorCodes.INVALID_AA_RELATIONSHIP.numericCode());
    assertEquals(
        IPSCmsErrors.VARIANT_LOOKUP_FAILED, CmsErrorCodes.VARIANT_LOOKUP_FAILED.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_AA_RELATIONSHIP_TYPE,
        CmsErrorCodes.INVALID_AA_RELATIONSHIP_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_AA_RELATIONSHIP_SLOT_VARIANT,
        CmsErrorCodes.INVALID_AA_RELATIONSHIP_SLOT_VARIANT.numericCode());
    assertEquals(IPSCmsErrors.INVALID_AUTHTYPE, CmsErrorCodes.INVALID_AUTHTYPE.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_CONTEXT_FOR_AA_PROXY,
        CmsErrorCodes.INVALID_CONTEXT_FOR_AA_PROXY.numericCode());
    assertEquals(
        IPSServerErrors.MISSING_INTERNAL_REQUEST_RESOURCE,
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode());
    assertEquals(
        com.percussion.cms.objectstore.client.IPSRemoteErrors.REMOTE_UNEXPECTED_ERROR,
        RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.numericCode());
    assertEquals(
        com.percussion.cms.objectstore.client.IPSRemoteErrors.REMOTE_WRONG_SOAP_RESP,
        RemoteErrorCodes.REMOTE_WRONG_SOAP_RESP.numericCode());

    assertFalse(CmsErrorCodes.KEY_NOT_ASSIGNED.isAuditable());
    assertFalse(CmsErrorCodes.UNEXPECTED_ERROR.isAuditable());
    assertFalse(CmsErrorCodes.COMM_ERROR_WITH_SERVER.isAuditable());
    assertFalse(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.isAuditable());
    assertFalse(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.isAuditable());
    assertFalse(RemoteErrorCodes.REMOTE_WRONG_SOAP_RESP.isAuditable());
  }

  @Test
  void invalidChildAndContentTypeExceptionsUseTypedCmsCodes() {
    PSInvalidChildTypeException child =
        new PSInvalidChildTypeException("sys_title", "percPage");
    assertSame(CmsErrorCodes.INVALID_CHILD_TYPE, child.getTypedErrorCode());
    assertEquals(CmsErrorCodes.INVALID_CHILD_TYPE.numericCode(), child.getErrorCode());
    assertFalse(child.isAuditable());

    PSInvalidContentTypeException ctype = new PSInvalidContentTypeException("percPage");
    assertSame(CmsErrorCodes.INVALID_CONTENT_TYPE_ID, ctype.getTypedErrorCode());
    assertEquals(CmsErrorCodes.INVALID_CONTENT_TYPE_ID.numericCode(), ctype.getErrorCode());
    assertFalse(ctype.isAuditable());
  }

  @Test
  void dbComponentSetPersistedWithoutAssignedKeyThrowsTypedCmsException() {
    SliceDbComponent component = new SliceDbComponent();
    PSCmsException ex = assertThrows(PSCmsException.class, component::setPersisted);
    assertSame(CmsErrorCodes.KEY_NOT_ASSIGNED, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.KEY_NOT_ASSIGNED.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void dbComponentFromXmlWrongRootThrowsTypedUnknownNodeType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(doc, "NotThisComponent");
    SliceDbComponent component = new SliceDbComponent();
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> component.fromXml(wrong));
    assertSame(CmsErrorCodes.INVALID_CONTENT_TYPE_ID, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.INVALID_CONTENT_TYPE_ID.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void dbComponentAssignKeyTooManyForeignPartsThrowsTypedCmsException() throws Exception {
    SliceDbComponent component = new SliceDbComponent();
    IPSKeyGenerator gen = mock(IPSKeyGenerator.class);
    when(gen.allocateId(anyString())).thenReturn(7);
    PSKey parent = new PSKey(new String[] {"id"}, new String[] {"99"}, true);
    PSCmsException ex =
        assertThrows(PSCmsException.class, () -> component.assignKey(gen, parent));
    assertSame(CmsErrorCodes.TOO_MANY_FOREIGN_KEY_PARTS, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.TOO_MANY_FOREIGN_KEY_PARTS.numericCode(), ex.getErrorCode());
  }

  @Test
  void dbComponentMissingLookupKeyThrowsTypedCmsException() {
    SliceDbComponent component = new SliceDbComponent() {
      @Override
      protected String getLookupName() {
        return "";
      }
    };
    IPSKeyGenerator gen = mock(IPSKeyGenerator.class);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> component.assignKey(gen, null));
    assertSame(CmsErrorCodes.MISSING_LOOKUP_KEY, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.MISSING_LOOKUP_KEY.numericCode(), ex.getErrorCode());
  }

  @Test
  void dbComponentKeyMismatchThrowsTypedCmsException() {
    SliceDbComponent component = new SliceDbComponent() {
      @Override
      protected String[] getKeyPartValues(IPSKeyGenerator gen) {
        return new String[] {"a", "b"};
      }
    };
    IPSKeyGenerator gen = mock(IPSKeyGenerator.class);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> component.assignKey(gen, null));
    assertSame(CmsErrorCodes.KEY_MISMATCH, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.KEY_MISMATCH.numericCode(), ex.getErrorCode());
  }

  @Test
  void dbComponentTooFewForeignKeyPartsThrowsTypedCmsException() {
    SliceDbComponent component =
        new SliceDbComponent(new PSKey(new String[] {"id", "parent"})) {
          @Override
          protected String[] getKeyPartValues(IPSKeyGenerator gen) {
            return new String[] {"only-one"};
          }
        };
    IPSKeyGenerator gen = mock(IPSKeyGenerator.class);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> component.assignKey(gen, null));
    assertSame(CmsErrorCodes.TOO_FEW_FOREIGN_KEY_PARTS, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.TOO_FEW_FOREIGN_KEY_PARTS.numericCode(), ex.getErrorCode());
  }

  @Test
  void keyFromXmlMismatchedPartsThrowsTypedUnknownNodeType() throws Exception {
    PSKey key = new PSKey(new String[] {"id"}, new String[] {"1"}, true);
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element src = PSXmlDocumentBuilder.createRoot(doc, "PSXKey");
    PSXmlDocumentBuilder.addElement(doc, src, "other", "1");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> key.fromXml(src));
    assertSame(CmsErrorCodes.KEY_PARTS_NOT_MATCH, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.KEY_PARTS_NOT_MATCH.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void menuChildKeyDataMismatchThrowsTypedUnknownNodeType() throws Exception {
    PSMenuChild child = new PSMenuChild(1L, "child", 2L);
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = child.toXml(doc);
    xml.setAttribute(PSMenuChild.XML_ATTR_CHILDID, "99");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> child.fromXml(xml));
    assertSame(CmsErrorCodes.MISMATCH_BETWEEN_KEY_AND_DATA, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.MISMATCH_BETWEEN_KEY_AND_DATA.numericCode(), ex.getErrorCode());
  }

  @Test
  void menuModeContextMappingMismatchThrowsTypedUnknownNodeType() throws Exception {
    PSMenuModeContextMapping mapping = new PSMenuModeContextMapping("10", "20", "30");
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = mapping.toXml(doc);
    xml.setAttribute(PSMenuModeContextMapping.XML_ATTR_MODEID, "99");
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> mapping.fromXml(xml));
    assertSame(CmsErrorCodes.MISMATCH_BETWEEN_KEY_AND_DATA, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.MISMATCH_BETWEEN_KEY_AND_DATA.numericCode(), ex.getErrorCode());
  }

  @Test
  void dbComponentListInvalidEntryClassnameThrowsTypedUnknownNodeType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXDbComponentList");
    root.appendChild(doc.createElement("NotPSX"));
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSDbComponentList(root));
    assertSame(CmsErrorCodes.INVALID_ENTRY_CLASSNAME, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.INVALID_ENTRY_CLASSNAME.numericCode(), ex.getErrorCode());
  }

  @Test
  void dbComponentSetInvalidEntryClassnameThrowsTypedUnknownNodeType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXDbComponentSet");
    root.appendChild(doc.createElement("NotPSX"));
    PSUnknownNodeTypeException ex =
        assertThrows(
            PSUnknownNodeTypeException.class, () -> new PSDbComponentSet<IPSDbComponent>(root));
    assertSame(CmsErrorCodes.INVALID_ENTRY_CLASSNAME, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.INVALID_ENTRY_CLASSNAME.numericCode(), ex.getErrorCode());
  }

  @Test
  void dbComponentListUnknownClassThrowsTypedInstantiationError() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXDbComponentList");
    root.setAttribute(PSDbComponentList.XML_ATTR_CLASS, "com.percussion.cms.objectstore.NoSuchComp");
    root.appendChild(doc.createElement("PSXMenuChild"));
    PSUnknownNodeTypeException ex =
        assertThrows(PSUnknownNodeTypeException.class, () -> new PSDbComponentList(root));
    assertSame(CmsErrorCodes.COMPONENT_INSTANTIATION_ERROR, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.COMPONENT_INSTANTIATION_ERROR.numericCode(), ex.getErrorCode());
  }

  @Test
  void processorLoadUnknownTypeThrowsTypedUnsupportedComponentType() {
    Map<String, Map<String, Object>> props = new HashMap<>();
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    PSRemoteProcessor processor = new PSRemoteProcessor(requester, props);
    PSCmsException ex =
        assertThrows(PSCmsException.class, () -> processor.load("UnknownType", null));
    assertSame(CmsErrorCodes.UNSUPPORTED_COMPONENT_TYPE, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.UNSUPPORTED_COMPONENT_TYPE.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void processorLoadMissingPropertyThrowsTypedMissingProperty() {
    Map<String, Map<String, Object>> props = new HashMap<>();
    Map<String, Object> typeProps = new HashMap<>();
    typeProps.put("other", "value");
    props.put("psfolder", typeProps);
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    PSRemoteProcessor processor = new PSRemoteProcessor(requester, props);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> processor.load("PSFolder", null));
    assertSame(CmsErrorCodes.MISSING_PROPERTY, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.MISSING_PROPERTY.numericCode(), ex.getErrorCode());
  }

  @Test
  void remoteProcessorIoFailureThrowsTypedCommError() throws Exception {
    Map<String, Map<String, Object>> props = folderLoadProps();
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenThrow(new IOException("down"));
    PSRemoteProcessor processor = new PSRemoteProcessor(requester, props);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> processor.load("PSFolder", null));
    assertSame(CmsErrorCodes.COMM_ERROR_WITH_SERVER, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.COMM_ERROR_WITH_SERVER.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void remoteProcessorSaxFailureThrowsTypedSaxProcessing() throws Exception {
    Map<String, Map<String, Object>> props = folderLoadProps();
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenThrow(new SAXException("bad xml"));
    PSRemoteProcessor processor = new PSRemoteProcessor(requester, props);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> processor.load("PSFolder", null));
    assertSame(CmsErrorCodes.SAX_PROCESSING_EXCEPTION, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.SAX_PROCESSING_EXCEPTION.numericCode(), ex.getErrorCode());
  }

  @Test
  void remoteProcessorWrongXmlRootThrowsTypedSerializedDocError() throws Exception {
    Map<String, Map<String, Object>> props = folderLoadProps();
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(doc, "WrongRoot");
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenReturn(doc);
    PSRemoteProcessor processor = new PSRemoteProcessor(requester, props);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> processor.load("PSFolder", null));
    assertSame(CmsErrorCodes.SERIALIZED_COMPONENTS_WRONG_XML_DOC, ex.getTypedErrorCode());
    assertEquals(
        CmsErrorCodes.SERIALIZED_COMPONENTS_WRONG_XML_DOC.numericCode(), ex.getErrorCode());
  }

  @Test
  void remoteCatalogerTransportFailureThrowsTypedCatalogError() throws Exception {
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenThrow(new IOException("down"));
    PSRemoteCataloger cataloger = new PSRemoteCataloger(requester);
    PSCmsException ex = assertThrows(PSCmsException.class, () -> cataloger.getCEFieldXml(0));
    assertSame(CmsErrorCodes.CONTENT_TYPE_CANNOT_BE_OPENED, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.CONTENT_TYPE_CANNOT_BE_OPENED.numericCode(), ex.getErrorCode());
  }

  @Test
  void remoteCatalogerUnexpectedDocumentThrowsTypedCatalogError() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(doc, "NotRelationshipInfoSet");
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenReturn(doc);
    PSRemoteCataloger cataloger = new PSRemoteCataloger(requester);
    PSCmsException ex =
        assertThrows(PSCmsException.class, cataloger::getRelationshipInfoSet);
    assertSame(CmsErrorCodes.UNEXPECTED_CATALOG_ERROR, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.UNEXPECTED_CATALOG_ERROR.numericCode(), ex.getErrorCode());
  }

  @Test
  void clientRelationshipProcessorTransportFailureThrowsTypedUnexpectedError() throws Exception {
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenThrow(new IOException("down"));
    PSRelationshipProcessor processor = new PSRelationshipProcessor(requester, Map.of());
    PSCmsException ex =
        assertThrows(
            PSCmsException.class,
            () -> processor.getChildren("Relationship", "folder", new PSLocator(1, 1)));
    assertSame(CmsErrorCodes.UNEXPECTED_ERROR, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.UNEXPECTED_ERROR.numericCode(), ex.getErrorCode());
  }

  @Test
  void remoteAgentCommunitiesFailureThrowsTypedRemoteException() throws Exception {
    IPSRemoteRequester requester = mock(IPSRemoteRequester.class);
    when(requester.getDocument(anyString(), any())).thenThrow(new IOException("down"));
    PSRemoteAgent agent = new PSRemoteAgent(requester);
    PSRemoteException ex = assertThrows(PSRemoteException.class, agent::getCommunities);
    assertSame(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR, ex.getTypedErrorCode());
    assertEquals(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void itemDefExtractorNullPipeThrowsTypedCmsException() {
    PSItemDefinition definition = mock(PSItemDefinition.class);
    PSContentEditor editor = mock(PSContentEditor.class);
    when(definition.getContentEditor()).thenReturn(editor);
    when(editor.getPipe()).thenReturn(null);
    PSCmsException ex =
        assertThrows(
            PSCmsException.class, () -> PSItemDefExtractor.extractDefinition(definition));
    assertSame(CmsErrorCodes.DATA_EXTRACTION_ERROR_NULL_DATAPIPE, ex.getTypedErrorCode());
    assertEquals(
        CmsErrorCodes.DATA_EXTRACTION_ERROR_NULL_DATAPIPE.numericCode(), ex.getErrorCode());
  }

  @Test
  void leftoverProcessorAndAaSitesUseTypedProductionExceptionTypes() {
    PSCmsException configMissing =
        new PSCmsException(CmsErrorCodes.PROCESSOR_CONFIG_MISSING, new String[] {"CmsProcessorConfig.xml"});
    assertSame(CmsErrorCodes.PROCESSOR_CONFIG_MISSING, configMissing.getTypedErrorCode());
    assertFalse(configMissing.isAuditable());

    PSCmsException xmlParse =
        new PSCmsException(CmsErrorCodes.XML_PARSING_ERROR, new String[] {"CmsProcessorConfig.xml", "bad", "none"});
    assertSame(CmsErrorCodes.XML_PARSING_ERROR, xmlParse.getTypedErrorCode());

    PSCmsException io =
        new PSCmsException(CmsErrorCodes.PROCESSOR_CONFIG_IO_ERROR, "disk");
    assertSame(CmsErrorCodes.PROCESSOR_CONFIG_IO_ERROR, io.getTypedErrorCode());

    PSUnknownNodeTypeException dupProp =
        new PSUnknownNodeTypeException(
            CmsErrorCodes.DUPLICATE_PROCESSOR_PROPERTY, new String[] {"local", "PSFolder", "name"});
    assertSame(CmsErrorCodes.DUPLICATE_PROCESSOR_PROPERTY, dupProp.getTypedErrorCode());

    PSUnknownNodeTypeException dupEntry =
        new PSUnknownNodeTypeException(
            CmsErrorCodes.DUPLICATE_PROCESSOR_ENTRY, new String[] {"local", "PSFolder"});
    assertSame(CmsErrorCodes.DUPLICATE_PROCESSOR_ENTRY, dupEntry.getTypedErrorCode());

    PSCmsException noEntry =
        new PSCmsException(CmsErrorCodes.NO_PROCESSOR_ENTRY, new String[] {"PSFolder", "local"});
    assertSame(CmsErrorCodes.NO_PROCESSOR_ENTRY, noEntry.getTypedErrorCode());

    PSCmsException noMethod =
        new PSCmsException(
            CmsErrorCodes.PROCESSOR_NO_SUCH_METHOD,
            new String[] {"MissingProc", "1", "java.util.Map", ""});
    assertSame(CmsErrorCodes.PROCESSOR_NO_SUCH_METHOD, noMethod.getTypedErrorCode());

    PSCmsException instantiation =
        new PSCmsException(
            CmsErrorCodes.PROCESSOR_INSTANTIATION_ERROR, new String[] {"MissingProc", "PSFolder", "boom"});
    assertSame(CmsErrorCodes.PROCESSOR_INSTANTIATION_ERROR, instantiation.getTypedErrorCode());

    PSCmsException aaRel =
        new PSCmsException(CmsErrorCodes.INVALID_AA_RELATIONSHIP, new String[] {"1", "x"});
    assertSame(CmsErrorCodes.INVALID_AA_RELATIONSHIP, aaRel.getTypedErrorCode());

    PSCmsException variant =
        new PSCmsException(CmsErrorCodes.VARIANT_LOOKUP_FAILED, new String[] {"7"});
    assertSame(CmsErrorCodes.VARIANT_LOOKUP_FAILED, variant.getTypedErrorCode());

    PSCmsException aaType =
        new PSCmsException(
            CmsErrorCodes.INVALID_AA_RELATIONSHIP_TYPE, new String[] {"1", "other", "rs_activeassembly"});
    assertSame(CmsErrorCodes.INVALID_AA_RELATIONSHIP_TYPE, aaType.getTypedErrorCode());

    PSCmsException slotVariant =
        new PSCmsException(
            CmsErrorCodes.INVALID_AA_RELATIONSHIP_SLOT_VARIANT, new String[] {"1", "2", "3"});
    assertSame(CmsErrorCodes.INVALID_AA_RELATIONSHIP_SLOT_VARIANT, slotVariant.getTypedErrorCode());

    PSCmsException authtype =
        new PSCmsException(CmsErrorCodes.INVALID_AUTHTYPE, new String[] {"9", "auth.xml"});
    assertSame(CmsErrorCodes.INVALID_AUTHTYPE, authtype.getTypedErrorCode());

    PSCmsException ctx =
        new PSCmsException(CmsErrorCodes.INVALID_CONTEXT_FOR_AA_PROXY, new String[] {});
    assertSame(CmsErrorCodes.INVALID_CONTEXT_FOR_AA_PROXY, ctx.getTypedErrorCode());

    PSNotFoundException missing =
        new PSNotFoundException(
            ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE,
            new Object[] {"sys_casSupport/casSupport_1", "No request handler found."});
    assertSame(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, missing.getTypedErrorCode());
    assertEquals(
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode(), missing.getErrorCode());
    assertFalse(missing.isAuditable());

    PSCmsException fieldCatalog =
        new PSCmsException(CmsErrorCodes.UNEXPECTED_ERROR, "choices xml");
    assertSame(CmsErrorCodes.UNEXPECTED_ERROR, fieldCatalog.getTypedErrorCode());

    PSRemoteException soap =
        new PSRemoteException(
            RemoteErrorCodes.REMOTE_WRONG_SOAP_RESP, new Object[] {"UpdateItemResponse", "<Fault/>"});
    assertSame(RemoteErrorCodes.REMOTE_WRONG_SOAP_RESP, soap.getTypedErrorCode());
    assertFalse(soap.isAuditable());
  }

  @Test
  void typedCmsExceptionCtorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSCmsException((CmsErrorCodes) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSRemoteException((RemoteErrorCodes) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSNotFoundException((ServerErrorCodes) null));
  }

  private static Map<String, Map<String, Object>> folderLoadProps() {
    Map<String, Map<String, Object>> props = new HashMap<>();
    Map<String, Object> typeProps = new HashMap<>();
    typeProps.put("loadresource", "sys_psxCms/folder.xml");
    typeProps.put("queryrootelementname", "ExpectedRoot");
    props.put("psfolder", typeProps);
    return props;
  }

  /** Minimal concrete {@link PSDbComponent} for leftover KEY_* production paths. */
  private static class SliceDbComponent extends PSDbComponent {
    SliceDbComponent() {
      super(new PSKey(new String[] {"id"}));
    }

    SliceDbComponent(PSKey key) {
      super(key);
    }
  }
}
