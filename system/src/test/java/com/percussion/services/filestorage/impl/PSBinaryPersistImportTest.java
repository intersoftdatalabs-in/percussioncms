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
package com.percussion.services.filestorage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.filestorage.IPSHashedFileDAO;
import com.percussion.services.filestorage.data.PSBinary;
import com.percussion.services.filestorage.data.PSBinaryData;
import com.percussion.services.filestorage.data.PSBinaryMetaKey;
import com.percussion.services.filestorage.data.PSMeta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToOne;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.util.Arrays;
import java.util.EnumSet;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * FastForward file-store import persists a new {@link PSBinary} that references
 * unsaved {@link PSBinaryData} (shared PK / foreign-id generator). Hibernate 6
 * throws TransientPropertyValueException unless both sides are linked and
 * persist is cascaded / ordered parent-then-child.
 */
@Tag("UnitTest")
class PSBinaryPersistImportTest {

  private PSHashedFileDAO dao;
  private Session session;
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    dao = new PSHashedFileDAO();
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(dao, "entityManager", entityManager);
  }

  @Test
  @DisplayName("PSBinary.data OneToOne cascades persist so import does not leave PSBinaryData transient")
  void binaryDataMappingCascadesPersist() throws Exception {
    Field dataField = PSBinary.class.getDeclaredField("data");
    OneToOne oneToOne = dataField.getAnnotation(OneToOne.class);
    assertNotNull(oneToOne, "PSBinary.data must be a OneToOne");
    assertEquals("binary", oneToOne.mappedBy());
    EnumSet<CascadeType> cascades = EnumSet.noneOf(CascadeType.class);
    cascades.addAll(Arrays.asList(oneToOne.cascade()));
    assertTrue(
        cascades.contains(CascadeType.PERSIST),
        "PSBinary.data must cascade persist so FastForward import can save PSBinaryData");
    assertTrue(
        cascades.contains(CascadeType.MERGE),
        "PSBinary.data must cascade merge for existing hashed files");
    assertTrue(
        !cascades.contains(CascadeType.ALL) && !cascades.contains(CascadeType.REMOVE),
        "Inverse OneToOne must not cascade REMOVE/ALL; delete stays on DAO/owning side");
  }

  @Test
  @DisplayName("setData wires the bidirectional shared-PK link")
  void setDataLinksBothSides() {
    PSBinary binary = new PSBinary();
    PSBinaryData data = new PSBinaryData(mock(Blob.class));
    binary.setData(data);
    assertSame(data, binary.getData());
    assertSame(binary, data.getBinary());
  }

  @Test
  @DisplayName("save persists new PSBinary then PSBinaryData (not merge)")
  void savePersistsNewBinaryThenData() {
    PSBinary binary = new PSBinary();
    PSBinaryData data = new PSBinaryData(mock(Blob.class));
    binary.setData(data);
    when(session.contains(data)).thenReturn(false);

    dao.save(binary);

    InOrder order = inOrder(session);
    order.verify(session).persist(binary);
    order.verify(session).persist(data);
    verify(session, never()).merge(any());
    assertSame(binary, data.getBinary());
  }

  @Test
  @DisplayName("save merges an existing PSBinary and its data")
  void saveMergesExistingBinaryAndData() {
    PSBinary binary = new PSBinary();
    binary.setId(42);
    PSBinaryData data = new PSBinaryData(mock(Blob.class));
    binary.setData(data);

    dao.save(binary);

    verify(session).merge(binary);
    verify(session).merge(data);
    verify(session, never()).persist(any());
  }

  @Test
  @DisplayName("create links blob data before DAO save")
  void createLinksDataBeforeSave() throws Exception {
    PSDbStorageService storage = new PSDbStorageService();
    IPSHashedFileDAO hashDao = mock(IPSHashedFileDAO.class);
    Blob blob = mock(Blob.class);
    when(hashDao.createBlob(any(), anyLong())).thenReturn(blob);
    when(hashDao.findOrCreateMetaKey(any(), anyBoolean()))
        .thenAnswer(invocation -> new PSBinaryMetaKey(invocation.getArgument(0), true));
    ReflectionTestUtils.setField(storage, "hashDao", hashDao);

    PSMeta meta = new PSMeta();
    meta.setHash("abc123def456");
    meta.setLength(4);

    PSBinary[] saved = new PSBinary[1];
    doAnswer(
            invocation -> {
              saved[0] = invocation.getArgument(0);
              return null;
            })
        .when(hashDao)
        .save(any(PSBinary.class));

    PSBinary created = storage.create(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}), meta);

    assertSame(saved[0], created);
    assertNotNull(created.getData());
    assertSame(created, created.getData().getBinary());
    assertEquals("abc123def456", created.getHash());
  }

  @Test
  @DisplayName("save(null) throws rather than silently returning")
  void saveNullThrows() {
    assertThrows(IllegalArgumentException.class, () -> dao.save(null));
    verify(session, never()).persist(any());
    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("getBinary does not save when the hash is missing")
  void getBinaryMissingHashDoesNotSave() {
    PSDbStorageService storage = new PSDbStorageService();
    IPSHashedFileDAO hashDao = mock(IPSHashedFileDAO.class);
    when(hashDao.getBinary("missing-hash")).thenReturn(null);
    ReflectionTestUtils.setField(storage, "hashDao", hashDao);

    assertNull(storage.getBinary("missing-hash"));
    verify(hashDao, never()).save(any());
    verify(hashDao, never()).save(null);
  }
}
