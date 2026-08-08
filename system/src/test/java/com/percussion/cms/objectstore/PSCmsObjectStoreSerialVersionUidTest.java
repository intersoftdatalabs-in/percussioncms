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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.objectstore.client.PSRemoteException;
import com.percussion.cms.objectstore.server.PSCorruptDatabaseException;
import com.percussion.design.objectstore.PSLocator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Java serialization checks for {@code com.percussion.cms.objectstore} types that implement {@link
 * Serializable} (directly or via {@code IPSCatalogSummary}/{@code PSComponent}/exceptions).
 *
 * <p>Inventory after #2297 foundation UID batch (#2022 slice 3e / #2403): all 20 true Serializable
 * types already declare {@code serialVersionUID}. This suite locks that invariant and exercises
 * round-trips for types constructible without a live server.
 *
 * <p>Non-Serializable {@code PSDbComponent} collection subclasses intentionally have no UID (same
 * policy as design.objectstore dead-UID cleanup).
 */
public class PSCmsObjectStoreSerialVersionUidTest {

  /** All concrete/abstract Serializable types under cms.objectstore (+ client/server exceptions). */
  private static final Class<?>[] SERIALIZABLE_TYPES = {
    PSAaRelationship.class,
    PSAction.class,
    PSCloneSiteFolderRequest.class,
    PSCloningOptions.class,
    PSComponentSummary.class,
    PSDisplayFormat.class,
    PSFolder.class,
    PSFolderPermissions.class,
    PSInvalidChildTypeException.class,
    PSInvalidContentTypeException.class,
    PSItemChildLocator.class,
    PSItemDefSummary.class,
    PSItemDefinition.class,
    PSKey.class,
    PSObjectPermissions.class,
    PSSearch.class,
    PSSimpleKey.class,
    PSSite.class,
    PSRemoteException.class,
    PSCorruptDatabaseException.class,
  };

  @Test
  public void testAllSerializableTypesDeclareSerialVersionUid() throws Exception {
    for (Class<?> type : SERIALIZABLE_TYPES) {
      assertTrue(
          Serializable.class.isAssignableFrom(type),
          type.getName() + " must be assignable to Serializable");
      assertFalse(type.isInterface(), type.getName());
      long uid = readSerialVersionUid(type);
      // Prefer 1L for modern batches; PSAaRelationship / PSFolder retain legacy constants.
      assertTrue(uid != 0L, type.getName() + " serialVersionUID must be non-zero");
      Field f = type.getDeclaredField("serialVersionUID");
      assertTrue(
          Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()),
          type.getName() + " serialVersionUID must be static final");
    }
  }

  @Test
  public void testKeyFamilyAndPermissionsSerialization() throws Exception {
    PSKey key = new PSKey(new String[] {"CONTENTID", "REVISIONID"}, new int[] {42, 3}, true);
    PSSimpleKey simple = new PSSimpleKey("id", "99", true);
    PSItemChildLocator childLoc = new PSItemChildLocator("sys_body", "7");
    PSFolderPermissions perms = new PSFolderPermissions(PSObjectPermissions.ACCESS_READ);

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(key);
      oos.writeObject(simple);
      oos.writeObject(childLoc);
      oos.writeObject(perms);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSKey serKey = (PSKey) ois.readObject();
      PSSimpleKey serSimple = (PSSimpleKey) ois.readObject();
      PSItemChildLocator serChild = (PSItemChildLocator) ois.readObject();
      PSFolderPermissions serPerms = (PSFolderPermissions) ois.readObject();

      assertEquals(key, serKey);
      assertEquals(42, serKey.getPartAsInt("CONTENTID"));
      assertEquals(simple, serSimple);
      assertEquals(99, serSimple.getKeyValueAsInt());
      assertEquals("sys_body", serChild.getChildContentType());
      assertEquals("7", serChild.getChildRowId());
      assertEquals(perms, serPerms);
      assertTrue(serPerms.hasAccess(PSObjectPermissions.ACCESS_READ));
    }

    assertEquals(1L, readSerialVersionUid(PSKey.class));
    assertEquals(1L, readSerialVersionUid(PSSimpleKey.class));
    assertEquals(1L, readSerialVersionUid(PSItemChildLocator.class));
    assertEquals(1L, readSerialVersionUid(PSFolderPermissions.class));
    assertEquals(1L, readSerialVersionUid(PSObjectPermissions.class));
  }

  @Test
  public void testCloningRequestAndExceptionSerialization() throws Exception {
    Map<Integer, Integer> communities = new HashMap<>();
    communities.put(100, 200);
    PSCloningOptions options =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE_SUBFOLDER,
            "folder-copy",
            PSCloningOptions.COPY_NO_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            communities);
    PSCloneSiteFolderRequest request =
        new PSCloneSiteFolderRequest(new PSLocator(10, 1), new PSLocator(20, 1), options);

    PSInvalidContentTypeException invalidType = new PSInvalidContentTypeException("ghost-type");
    PSInvalidChildTypeException invalidChild =
        new PSInvalidChildTypeException("bad-child", "Article");
    PSRemoteException remote = new PSRemoteException(1, "remote-fail");
    PSCorruptDatabaseException corrupt =
        new PSCorruptDatabaseException("CONTENTSTATUS", "42", "duplicate key");

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(options);
      oos.writeObject(request);
      oos.writeObject(invalidType);
      oos.writeObject(invalidChild);
      oos.writeObject(remote);
      oos.writeObject(corrupt);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSCloningOptions serOpts = (PSCloningOptions) ois.readObject();
      PSCloneSiteFolderRequest serReq = (PSCloneSiteFolderRequest) ois.readObject();
      PSInvalidContentTypeException serType = (PSInvalidContentTypeException) ois.readObject();
      PSInvalidChildTypeException serChild = (PSInvalidChildTypeException) ois.readObject();
      PSRemoteException serRemote = (PSRemoteException) ois.readObject();
      PSCorruptDatabaseException serCorrupt = (PSCorruptDatabaseException) ois.readObject();

      assertEquals(options, serOpts);
      assertTrue(serOpts.isCloneSiteSubfolder());
      assertEquals("folder-copy", serOpts.getFolderName());
      assertEquals(request.getSource(), serReq.getSource());
      assertEquals(request.getTarget(), serReq.getTarget());
      assertEquals(options, serReq.getOptions());
      assertNotNull(serType.getMessage());
      assertNotNull(serChild.getMessage());
      assertNotNull(serRemote.getMessage());
      assertNotNull(serCorrupt.getMessage());
    }

    assertEquals(1L, readSerialVersionUid(PSCloningOptions.class));
    assertEquals(1L, readSerialVersionUid(PSCloneSiteFolderRequest.class));
    assertEquals(1L, readSerialVersionUid(PSInvalidContentTypeException.class));
    assertEquals(1L, readSerialVersionUid(PSInvalidChildTypeException.class));
    assertEquals(1L, readSerialVersionUid(PSRemoteException.class));
    assertEquals(1L, readSerialVersionUid(PSCorruptDatabaseException.class));
  }

  @Test
  public void testCatalogSummaryTypesDeclareUidOnly() throws Exception {
    // Heavy DB-component types that are Serializable via IPSCatalogSummary — construct free checks.
    assertEquals(1L, readSerialVersionUid(PSAction.class));
    assertEquals(1L, readSerialVersionUid(PSDisplayFormat.class));
    assertEquals(1L, readSerialVersionUid(PSSearch.class));
    assertEquals(1L, readSerialVersionUid(PSItemDefSummary.class));
    assertEquals(1L, readSerialVersionUid(PSItemDefinition.class));
    assertEquals(1L, readSerialVersionUid(PSComponentSummary.class));
    assertEquals(1L, readSerialVersionUid(PSSite.class));
    // Legacy non-1L constants kept for wire compatibility
    assertNotEquals(0L, readSerialVersionUid(PSFolder.class));
    assertNotEquals(0L, readSerialVersionUid(PSAaRelationship.class));
  }

  private static long readSerialVersionUid(Class<?> type) throws Exception {
    Field f = type.getDeclaredField("serialVersionUID");
    f.setAccessible(true);
    return f.getLong(null);
  }
}
