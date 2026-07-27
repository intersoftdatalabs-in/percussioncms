/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Outcome → plugin response mapping for upgrade wiring (T063). */
@Tag("UnitTest")
public class PSUpgradePluginEmbeddedRepositoryMigrationTest {

  @Test
  void successOutcomesMapToSuccess() {
    assertEquals(
        PSPluginResponse.SUCCESS,
        PSUpgradePluginEmbeddedRepositoryMigration.mapOutcome(PSMigrationOutcome.SUCCESS)
            .getType());
    assertEquals(
        PSPluginResponse.SUCCESS,
        PSUpgradePluginEmbeddedRepositoryMigration.mapOutcome(PSMigrationOutcome.ALREADY_MIGRATED)
            .getType());
    assertEquals(
        PSPluginResponse.SUCCESS,
        PSUpgradePluginEmbeddedRepositoryMigration.mapOutcome(PSMigrationOutcome.SKIPPED_NON_DERBY)
            .getType());
  }

  @Test
  void failureOutcomesMapToException() {
    PSPluginResponse blocked =
        PSUpgradePluginEmbeddedRepositoryMigration.mapOutcome(
            PSMigrationOutcome.BLOCKED_BACKUP_GATE);
    assertEquals(PSPluginResponse.EXCEPTION, blocked.getType());
    assertTrue(
        blocked.getMessage().contains(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY));
    assertTrue(
        blocked.getMessage().toLowerCase().contains("automatically"),
        "blocked message should mention automatic product offline backup: " + blocked.getMessage());

    PSPluginResponse failed =
        PSUpgradePluginEmbeddedRepositoryMigration.mapOutcome(PSMigrationOutcome.FAILED);
    assertEquals(PSPluginResponse.EXCEPTION, failed.getType());
    assertTrue(failed.getMessage().toLowerCase().contains("failed"));
  }
}
