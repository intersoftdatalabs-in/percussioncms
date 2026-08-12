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
import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSObjectFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #3176: {@link PSXmlObjectStoreLockManager} must use typed {@link ObjectStoreErrorCodes}
 * (not bare {@code IPSObjectStoreErrors} ints) for lock corrupt / I/O paths, and must not
 * implement {@code IPSObjectStoreErrors}.
 *
 * <p>Extends {@link PSObjectFactory} so protected factory helpers match peer lock-manager tests.
 */
public class PSXmlObjectStoreLockManagerTypedErrorCodeTest extends PSObjectFactory {

  @TempDir Path temporaryFolder;

  @Test
  public void lockAcquisitionExceptionTypedCtorRetainsObjectStoreCode() {
    PSLockAcquisitionException ex =
        new PSLockAcquisitionException(
            ObjectStoreErrorCodes.LOCK_CORRUPT_LOCKFILE, "corrupt.lock");
    assertEquals(
        ObjectStoreErrorCodes.LOCK_CORRUPT_LOCKFILE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.LOCK_CORRUPT_LOCKFILE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());

    PSLockAcquisitionException ioEx =
        new PSLockAcquisitionException(
            ObjectStoreErrorCodes.LOCK_IO_EXCEPTION, new Object[] {"f", "ioe"});
    assertEquals(ObjectStoreErrorCodes.LOCK_IO_EXCEPTION.numericCode(), ioEx.getErrorCode());
    assertSame(ObjectStoreErrorCodes.LOCK_IO_EXCEPTION, ioEx.getTypedErrorCode());
  }

  @Test
  public void acquireLockCorruptLockfileUsesTypedCorruptCode() throws Exception {
    Path lockDirPath = temporaryFolder.resolve("locks");
    Files.createDirectories(lockDirPath);
    File lockDir = lockDirPath.toFile();

    PSXmlObjectStoreLockManager locker = new PSXmlObjectStoreLockManager(lockDir);

    PSApplication app = createApplication();
    app.setName("typedLockApp");
    Object lockKey = locker.getLockKey(app, IPSObjectStoreLockManager.LOCKTYPE_EXCLUSIVE);
    assertTrue(lockKey.toString().contains("typedLockApp"));

    // Pre-create a corrupt lock file at the key path (missing created/expires props).
    File lockFile = new File(lockKey.toString());
    Files.createDirectories(lockFile.getParentFile().toPath());
    Files.writeString(
        lockFile.toPath(),
        "locker=other\nsessionId=sess1\n",
        StandardCharsets.UTF_8);

    PSXmlObjectStoreLockerId id =
        new PSXmlObjectStoreLockerId("tester", false, "session-typed-lock");

    PSLockAcquisitionException ex =
        assertThrows(
            PSLockAcquisitionException.class,
            () -> locker.acquireLock(id, lockKey, 60_000L, 0L, null));
    assertEquals(
        ObjectStoreErrorCodes.LOCK_CORRUPT_LOCKFILE.numericCode(), ex.getErrorCode());
    assertSame(ObjectStoreErrorCodes.LOCK_CORRUPT_LOCKFILE, ex.getTypedErrorCode());
  }

  @Test
  public void classDoesNotImplementIpsObjectStoreErrors() {
    assertFalse(
        com.percussion.design.objectstore.IPSObjectStoreErrors.class.isAssignableFrom(
            PSXmlObjectStoreLockManager.class));
  }
}
