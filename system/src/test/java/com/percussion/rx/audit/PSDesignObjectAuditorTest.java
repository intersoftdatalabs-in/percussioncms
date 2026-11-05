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
package com.percussion.rx.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.rx.audit.PSDesignObjectAuditor.PSAuditData;
import com.percussion.services.audit.IPSDesignObjectAuditConfig;
import com.percussion.services.audit.IPSDesignObjectAuditService;
import com.percussion.services.audit.PSDesignObjectAuditServiceLocator;
import com.percussion.services.audit.data.PSAuditLogEntry;
import com.percussion.services.audit.data.PSAuditLogEntry.AuditTypes;
import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSDesignObjectAuditor}: extract audit data and dual-write through {@link
 * DesignErrorCodes} / system audit log when design auditing is enabled.
 */
class PSDesignObjectAuditorTest {

  private IPSDesignObjectAuditService auditService;
  private IPSDesignObjectAuditConfig auditConfig;
  private List<Collection<PSAuditLogEntry>> savedBatches;

  @BeforeEach
  void setUp() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }

    auditService = mock(IPSDesignObjectAuditService.class);
    auditConfig = mock(IPSDesignObjectAuditConfig.class);
    when(auditService.getConfig()).thenReturn(auditConfig);
    when(auditConfig.isEnabled()).thenReturn(true);
    when(auditService.createAuditLogEntry()).thenAnswer(inv -> new PSAuditLogEntry());
    savedBatches = new ArrayList<>();
    org.mockito.Mockito.doAnswer(
            inv -> {
              @SuppressWarnings("unchecked")
              Collection<PSAuditLogEntry> batch = inv.getArgument(0);
              savedBatches.add(new ArrayList<>(batch));
              return null;
            })
        .when(auditService)
        .saveAuditLogEntries(any());

    PSDesignObjectAuditServiceLocator.setAuditService(auditService);
  }

  @AfterEach
  void tearDown() {
    PSDesignObjectAuditServiceLocator.clearCache();
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }
  }

  @Test
  void createAuditDataExtractsSaveAndDeleteGuids() {
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();

    assertTrue(auditor.createAuditData(null, null).isEmpty());
    assertTrue(auditor.createAuditData("", null).isEmpty());
    assertTrue(auditor.createAuditData("findSomething", null).isEmpty());
    assertTrue(auditor.createAuditData("deleteSomething", null).isEmpty());
    assertTrue(auditor.createAuditData("deleteSomething", "Test").isEmpty());
    assertTrue(auditor.createAuditData("saveSomething", "Test").isEmpty());

    IPSGuid guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 301);
    assertTrue(auditor.createAuditData("findSomething", guid).isEmpty());

    Collection<PSAuditData> auditData = auditor.createAuditData("deleteSomething", guid);
    assertFalse(auditData.isEmpty());
    PSAuditData data = auditData.iterator().next();
    assertNotNull(data);
    assertEquals(guid, data.getGuid());
    assertEquals(AuditTypes.DELETE, data.getAction());

    PSMockCatalogIdentifier id = new PSMockCatalogIdentifier();
    id.mi_guid = guid;
    id.mi_version = null;
    auditData = auditor.createAuditData("deleteSomething", id);
    assertFalse(auditData.isEmpty());
    data = auditData.iterator().next();
    assertEquals(guid, data.getGuid());
    assertEquals(AuditTypes.DELETE, data.getAction());

    id.mi_version = Integer.valueOf(2);
    auditData = auditor.createAuditData("saveSomething", id);
    assertFalse(auditData.isEmpty());
    data = auditData.iterator().next();
    assertEquals(AuditTypes.SAVE, data.getAction());
  }

  @Test
  void createAuditDataHandlesCollections() {
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    List<Object> coll = new ArrayList<>();

    PSMockCatalogIdentifier id1 = new PSMockCatalogIdentifier();
    PSMockCatalogIdentifier id2 = new PSMockCatalogIdentifier();
    PSMockCatalogIdentifier id3 = new PSMockCatalogIdentifier();

    id1.mi_guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 301);
    id1.mi_version = 0;
    id2.mi_guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 302);
    id2.mi_version = 1;
    id3.mi_guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 303);
    id3.mi_version = 0;

    coll.add(id1);
    coll.add(id2);
    coll.add(id3);

    Collection<PSAuditData> auditData = auditor.createAuditData("saveSomething", coll);
    assertEquals(coll.size(), auditData.size());
    validateAuditedCollection(coll, auditData);

    coll.add("An object that is not audited");
    auditData = auditor.createAuditData("saveSomething", coll);
    assertEquals(coll.size() - 1, auditData.size());
    validateAuditedCollection(coll, auditData);

    coll.clear();
    coll.add(id1.mi_guid);
    coll.add(id2.mi_guid);
    id3.mi_version = null;
    coll.add(id3);
    auditData = auditor.createAuditData("deleteSomething", coll);
    assertEquals(coll.size(), auditData.size());
    validateAuditedCollection(coll, auditData);
  }

  @Test
  void enabledSaveDualWritesUpdateAndLegacyEntry() throws Throwable {
    Map<String, Object> initial = new HashMap<>();
    initial.put(PSRequestInfo.KEY_USER, "designer");
    PSRequestInfo.initRequestInfo(initial);

    PSMockCatalogIdentifier id = new PSMockCatalogIdentifier();
    id.mi_guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 401);
    id.mi_version = 1;

    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    auditor.audit(joinPoint("saveContentList", id));

    assertEquals(1, savedBatches.size());
    assertEquals(1, savedBatches.get(0).size());
    PSAuditLogEntry legacy = savedBatches.get(0).iterator().next();
    assertEquals(AuditTypes.SAVE, legacy.getAction());
    assertEquals("designer", legacy.getUserName());

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(DesignErrorCodes.UPDATE, rec.code());
    assertEquals("DESN", rec.code().module().code());
    assertEquals(2902, rec.code().numericCode());
    assertEquals("designer", rec.actor().orElse(""));
    assertTrue(rec.formattedLine().startsWith("[DESN-2902]-"));
  }

  @Test
  void enabledDeleteDualWritesDeleteCode() throws Throwable {
    Map<String, Object> initial = new HashMap<>();
    initial.put(PSRequestInfo.KEY_USER, "admin");
    PSRequestInfo.initRequestInfo(initial);

    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 55);
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    auditor.audit(joinPoint("deleteTemplate", guid));

    assertEquals(1, savedBatches.size());
    assertEquals(AuditTypes.DELETE, savedBatches.get(0).iterator().next().getAction());

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(DesignErrorCodes.DELETE, rec.code());
    assertEquals(2903, rec.code().numericCode());
    assertEquals("admin", rec.actor().orElse(""));
  }

  @Test
  void multiObjectCollectionDualWritesEach() throws Throwable {
    Map<String, Object> initial = new HashMap<>();
    initial.put(PSRequestInfo.KEY_USER, "bulkuser");
    PSRequestInfo.initRequestInfo(initial);

    List<Object> coll = new ArrayList<>();
    coll.add(new PSGuid(PSTypeEnum.CONTENT_LIST, 501));
    coll.add(new PSGuid(PSTypeEnum.CONTENT_LIST, 502));
    coll.add(new PSGuid(PSTypeEnum.CONTENT_LIST, 503));

    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    auditor.audit(joinPoint("deleteMany", coll));

    assertEquals(1, savedBatches.size());
    assertEquals(3, savedBatches.get(0).size());
    assertEquals(3, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    assertTrue(
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().stream()
            .allMatch(r -> r.code() == DesignErrorCodes.DELETE));
  }

  @Test
  void disabledAuditingWritesNothing() throws Throwable {
    when(auditConfig.isEnabled()).thenReturn(false);

    Map<String, Object> initial = new HashMap<>();
    initial.put(PSRequestInfo.KEY_USER, "designer");
    PSRequestInfo.initRequestInfo(initial);

    PSMockCatalogIdentifier id = new PSMockCatalogIdentifier();
    id.mi_guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 601);
    id.mi_version = 1;

    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    auditor.audit(joinPoint("saveContentList", id));

    verify(auditService, never()).saveAuditLogEntries(any());
    verify(auditService, never()).createAuditLogEntry();
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    assertTrue(savedBatches.isEmpty());
  }

  @Test
  void blankUserMapsToUnknownOnBothPaths() throws Throwable {
    // No KEY_USER and no KEY_PSREQUEST → unknown
    Map<String, Object> initial = new HashMap<>();
    PSRequestInfo.initRequestInfo(initial);

    IPSGuid guid = new PSGuid(PSTypeEnum.SITE, 9);
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    auditor.audit(joinPoint("saveSite", guid));

    assertEquals(1, savedBatches.size());
    assertEquals("unknown", savedBatches.get(0).iterator().next().getUserName());

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals("unknown", rec.actor().orElse(""));
    assertEquals(DesignErrorCodes.UPDATE, rec.code());
  }

  @Test
  void resolveUserNameBlankWithoutRequestInfoIsUnknown() {
    // PSRequestInfo not inited — getRequestInfo returns null
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    assertEquals("unknown", auditor.resolveUserName());
  }

  @Test
  void dualWriteSystemAuditUsesUpdateForSaveAction() {
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    Collection<PSAuditData> data =
        auditor.createAuditData("saveX", new PSGuid(PSTypeEnum.CONTENT_LIST, 1));
    auditor.dualWriteSystemAudit("u1", data.iterator().next());

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    assertEquals(
        DesignErrorCodes.UPDATE, ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
  }

  @Test
  void dualWriteSystemAuditUsesDeleteForDeleteAction() {
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    Collection<PSAuditData> data =
        auditor.createAuditData("deleteX", new PSGuid(PSTypeEnum.CONTENT_LIST, 2));
    auditor.dualWriteSystemAudit("u2", data.iterator().next());

    assertEquals(
        DesignErrorCodes.DELETE, ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
  }

  @Test
  void nonAuditedMethodDoesNotWrite() throws Throwable {
    Map<String, Object> initial = new HashMap<>();
    initial.put(PSRequestInfo.KEY_USER, "designer");
    PSRequestInfo.initRequestInfo(initial);

    IPSGuid guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 701);
    PSDesignObjectAuditor auditor = new PSDesignObjectAuditor();
    auditor.audit(joinPoint("findContentList", guid));

    verify(auditService, never()).saveAuditLogEntries(any());
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void resolveTypeNameUsesTypeEnum() {
    IPSGuid guid = new PSGuid(PSTypeEnum.CONTENT_LIST, 1);
    assertEquals("CONTENT_LIST", PSDesignObjectAuditor.resolveTypeName(guid));
    assertEquals("", PSDesignObjectAuditor.resolveTypeName(null));
  }

  private static JoinPoint joinPoint(String methodName, Object firstArg) {
    JoinPoint jp = mock(JoinPoint.class);
    Signature sig = mock(Signature.class);
    when(jp.getSignature()).thenReturn(sig);
    when(sig.getName()).thenReturn(methodName);
    when(jp.getArgs()).thenReturn(new Object[] {firstArg});
    return jp;
  }

  private void validateAuditedCollection(List<Object> coll, Collection<PSAuditData> auditData) {
    Map<IPSGuid, PSAuditData> resultMap = new HashMap<>();
    for (PSAuditData result : auditData) {
      resultMap.put(result.getGuid(), result);
    }

    for (Object object : coll) {
      if (object instanceof PSMockCatalogIdentifier) {
        PSMockCatalogIdentifier id = (PSMockCatalogIdentifier) object;
        PSAuditData result = resultMap.get(id.getGUID());
        assertNotNull(result);
        Integer version = id.getVersion();
        if (version == null) {
          assertEquals(AuditTypes.DELETE, result.getAction());
        } else {
          assertEquals(AuditTypes.SAVE, result.getAction());
        }
      } else if (object instanceof IPSGuid) {
        PSAuditData result = resultMap.get(object);
        assertNotNull(result);
        assertEquals(AuditTypes.DELETE, result.getAction());
      }
    }
  }

  /** Mock implementation of {@link IPSCatalogIdentifier} with optional Hibernate version. */
  static class PSMockCatalogIdentifier implements IPSCatalogIdentifier {
    private IPSGuid mi_guid;
    private Integer mi_version;

    @Override
    public IPSGuid getGUID() {
      return mi_guid;
    }

    public Integer getVersion() {
      return mi_version;
    }
  }
}
