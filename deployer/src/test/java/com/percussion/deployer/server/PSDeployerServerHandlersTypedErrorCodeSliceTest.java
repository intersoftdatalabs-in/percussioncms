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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.JobErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.conn.PSServerException;
import com.percussion.deployer.server.dependencies.PSFilterInstallUtils;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSDeployException;
import com.percussion.error.PSDeployNonUniqueException;
import com.percussion.error.PSLockedException;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.server.job.PSJobException;
import com.percussion.util.PSResourceUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.FileInputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Issue #3740 (parent #2616 slice 2): leftover deployer server / dependency-handler production
 * sites throw typed {@code *ErrorCodes}. Lock codes remain auditable; leftover operational codes
 * skip dual-write. Catalog/client remain on #3739; DCE/TableFactory on later slices.
 */
@Tag("UnitTest")
public class PSDeployerServerHandlersTypedErrorCodeSliceTest {

  @Test
  public void packageConfigInvalidOrderUsesTypedNonAuditableCode() throws Exception {
    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> loadConfig("sys_PkgConfig_MissDeployEl.xml"));
    assertEquals(DeploymentErrorCodes.INCOMPLATE_ORDER_DEF.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.INCOMPLATE_ORDER_DEF, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void packageConfigMissingChildUsesTypedNonAuditableCode() throws Exception {
    PSDeployException ex =
        assertThrows(
            PSDeployException.class, () -> loadConfig("sys_PkgConfig_MissNonDeployEl.xml"));
    assertEquals(DeploymentErrorCodes.INVALID_NUM_CHILD_DEFS.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.INVALID_NUM_CHILD_DEFS, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void packageConfigInvalidParentUsesTypedNonAuditableCode() throws Exception {
    PSDeployException ex =
        assertThrows(
            PSDeployException.class,
            () -> loadConfig("sys_PkgConfig_InvalidNonDeployElParent.xml"));
    assertEquals(DeploymentErrorCodes.CANNOT_FIND_PARENT_DEP_DEF.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.CANNOT_FIND_PARENT_DEP_DEF, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void lockedExceptionTypedLockCodesAreAuditable() {
    Object[] args = {"alice", "3"};
    PSLockedException held = new PSLockedException(DeploymentErrorCodes.LOCK_ALREADY_HELD, args);
    assertEquals(DeploymentErrorCodes.LOCK_ALREADY_HELD.numericCode(), held.getErrorCode());
    assertSame(DeploymentErrorCodes.LOCK_ALREADY_HELD, held.getTypedErrorCode());
    assertTrue(held.isAuditable());

    PSLockedException taken =
        new PSLockedException(DeploymentErrorCodes.LOCK_NOT_EXTENSIBLE_TAKEN);
    assertSame(DeploymentErrorCodes.LOCK_NOT_EXTENSIBLE_TAKEN, taken.getTypedErrorCode());
    assertTrue(taken.isAuditable());

    PSLockedException released =
        new PSLockedException(
            DeploymentErrorCodes.LOCK_NOT_EXTENSIBLE_TAKEN_RELEASED, "bob");
    assertSame(
        DeploymentErrorCodes.LOCK_NOT_EXTENSIBLE_TAKEN_RELEASED, released.getTypedErrorCode());
    assertTrue(released.isAuditable());
  }

  @Test
  public void jobExceptionTypedCodesSkipAudit() {
    PSJobException invalid =
        new PSJobException(JobErrorCodes.INVALID_JOB_DESCRIPTOR, "bad-desc");
    assertEquals(JobErrorCodes.INVALID_JOB_DESCRIPTOR.numericCode(), invalid.getErrorCode());
    assertSame(JobErrorCodes.INVALID_JOB_DESCRIPTOR, invalid.getTypedErrorCode());
    assertFalse(invalid.isAuditable());

    PSJobException unexpected = new PSJobException(JobErrorCodes.UNEXPECTED_ERROR, "boom");
    assertSame(JobErrorCodes.UNEXPECTED_ERROR, unexpected.getTypedErrorCode());
    assertFalse(unexpected.isAuditable());

    PSJobException missingCfg =
        new PSJobException(JobErrorCodes.CONFIG_FILE_NOT_FOUND, "rxconfig.xml");
    assertSame(JobErrorCodes.CONFIG_FILE_NOT_FOUND, missingCfg.getTypedErrorCode());
    assertFalse(missingCfg.isAuditable());

    assertThrows(IllegalArgumentException.class, () -> new PSJobException((IPSErrorCode) null));
  }

  @Test
  public void nonUniqueArchiveRefUsesTypedNonAuditableCode() {
    PSDeployNonUniqueException ex =
        new PSDeployNonUniqueException(DeploymentErrorCodes.ARCHIVE_REF_FOUND, "pkg-1");
    assertEquals(DeploymentErrorCodes.ARCHIVE_REF_FOUND.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.ARCHIVE_REF_FOUND, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void serverAndAuthTypedConstructorsRetainCodes() {
    Object[] args = {"DeploymentHandler", "init failed"};
    PSServerException server =
        new PSServerException(ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION, args);
    assertEquals(
        ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION.numericCode(),
        server.getErrorCode());
    assertSame(
        ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION, server.getTypedErrorCode());
    assertFalse(server.isAuditable());

    PSAuthenticationFailedException auth =
        new PSAuthenticationFailedException(
            SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED, null);
    assertEquals(
        SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.numericCode(), auth.getErrorCode());
    assertSame(SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED, auth.getTypedErrorCode());
    assertTrue(auth.isAuditable());
  }

  @Test
  public void filterMissingUsesTypedNumericCode() {
    assertTrue(
        PSFilterInstallUtils.isFilterMissingErrorCode(
            FilterServiceErrorCodes.FILTER_MISSING.numericCode()));
    assertFalse(
        PSFilterInstallUtils.isFilterMissingErrorCode(
            FilterServiceErrorCodes.DATABASE.numericCode()));
    assertFalse(FilterServiceErrorCodes.FILTER_MISSING.isAuditable());
  }

  private static PSPackageConfiguration loadConfig(String fileName) throws Exception {
    String resource = "/com/percussion/config/" + fileName;
    File f = PSResourceUtils.getFile(PSPackageConfigurationTest.class, resource, null);
    try (FileInputStream in = new FileInputStream(f)) {
      Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
      return new PSPackageConfiguration(doc.getDocumentElement(), false);
    }
  }
}
