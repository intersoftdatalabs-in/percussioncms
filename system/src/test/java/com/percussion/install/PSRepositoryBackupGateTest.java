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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Backup gate matrix (T043 / QC-007 / FR-018 / SC-010): product backup, external confirm, neither.
 */
@Tag("UnitTest")
public class PSRepositoryBackupGateTest {

  @Test
  void productBackupOpensGate() {
    Properties props = new Properties();
    PSBackupGateKind gate = PSRepositoryBackupGate.evaluate(true, props);
    assertEquals(PSBackupGateKind.PRODUCT_BACKUP, gate);
    assertTrue(PSRepositoryBackupGate.isSatisfied(gate));
  }

  @Test
  void externalConfirmOpensGateWithoutProductBackup() {
    Properties props = new Properties();
    props.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "true");
    PSBackupGateKind gate = PSRepositoryBackupGate.evaluate(false, props);
    assertEquals(PSBackupGateKind.EXTERNAL_CONFIRM, gate);
    assertTrue(PSRepositoryBackupGate.isSatisfied(gate));
  }

  @Test
  void externalConfirmIsCaseInsensitive() {
    Properties props = new Properties();
    props.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "TRUE");
    assertTrue(PSRepositoryBackupGate.isExternalBackupConfirmed(props));
  }

  @Test
  void neitherBlocksGate() {
    Properties props = new Properties();
    PSBackupGateKind gate = PSRepositoryBackupGate.evaluate(false, props);
    assertEquals(PSBackupGateKind.NOT_SATISFIED, gate);
    assertFalse(PSRepositoryBackupGate.isSatisfied(gate));
  }

  @Test
  void falseExternalConfirmDoesNotOpenGate() {
    Properties props = new Properties();
    props.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "false");
    PSBackupGateKind gate = PSRepositoryBackupGate.evaluate(false, props);
    assertEquals(PSBackupGateKind.NOT_SATISFIED, gate);
  }

  @Test
  void blankExternalConfirmDoesNotOpenGate() {
    Properties props = new Properties();
    props.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, " ");
    assertFalse(PSRepositoryBackupGate.isExternalBackupConfirmed(props));
  }

  @Test
  void productBackupTakesPrecedenceOverExternalConfirm() {
    Properties props = new Properties();
    props.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "true");
    assertEquals(PSBackupGateKind.PRODUCT_BACKUP, PSRepositoryBackupGate.evaluate(true, props));
  }
}
