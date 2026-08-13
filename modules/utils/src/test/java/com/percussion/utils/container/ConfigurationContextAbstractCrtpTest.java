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
package com.percussion.utils.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.container.config.model.impl.BaseContainerUtils;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral coverage for CRTP {@code self()} and typed {@code cloneConfig} (#3172 residual after
 * batch 8).
 */
@Tag("UnitTest")
public class ConfigurationContextAbstractCrtpTest {

  @TempDir Path tempDir;

  @Test
  public void selfReturnsThisForLoadAndSave() {
    DefaultConfigurationContextImpl ctx = new DefaultConfigurationContextImpl(tempDir, "encKey");
    // load/save pass self() into adapters; empty adapter list must still type-check at runtime.
    ctx.load();
    ctx.save();
    assertSame(ctx, ctx.self());
  }

  @Test
  public void copyFromClonesConfigAndDoesNotAliasSource() {
    DefaultConfigurationContextImpl from = new DefaultConfigurationContextImpl(tempDir, "encKey");
    DefaultConfigurationContextImpl to = new DefaultConfigurationContextImpl(tempDir, "encKey");

    BaseContainerUtils source = from.getConfig();
    source.setEnabled(true);
    source.setLoaded(true);

    to.copyFrom(from);

    assertNotSame(source, to.getConfig());
    assertTrue(to.getConfig().isEnabled());
    assertTrue(to.getConfig().isLoaded());

    to.getConfig().setEnabled(false);
    assertTrue(source.isEnabled());
    assertFalse(to.getConfig().isEnabled());
    assertEquals(true, source.isLoaded());
  }
}
