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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.notification.PSNotificationHelper;
import com.percussion.services.system.data.PSConfigurationTypes;
import com.percussion.services.system.data.PSMimeContentAdapter;
import com.percussion.services.system.impl.PSSystemService;
import com.percussion.utils.guid.IPSGuid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Behavioral tests for {@link PSSystemService#saveConfiguration} parent-directory creation (#4283 /
 * residual of #1690). Fresh H2/QA cells may lack {@code rxconfig/XSpLit}; allow-listed config saves
 * must still succeed via portable NIO {@code Files.createDirectories}.
 */
public class PSSystemServiceSaveConfigurationTest {

  @TempDir Path tempDir;

  private PSSystemService service;
  private MockedStatic<PSNotificationHelper> notifications;

  @BeforeEach
  void setUp() {
    service =
        new PSSystemService(
            mock(com.percussion.utils.jdbc.IPSDatasourceManager.class),
            mock(IPSGuidManager.class),
            mock(com.percussion.services.workflow.IPSWorkflowService.class),
            mock(com.percussion.services.legacy.IPSCmsObjectMgr.class));
    notifications = mockStatic(PSNotificationHelper.class);
    notifications
        .when(() -> PSNotificationHelper.notifyFile(any(File.class)))
        .thenAnswer(invocation -> null);
  }

  @AfterEach
  void tearDown() {
    if (notifications != null) {
      notifications.close();
    }
  }

  @Test
  void saveConfiguration_createsMissingXSpLitParentForTidyConfig() throws Exception {
    Path xsplittDir = tempDir.resolve("rxconfig").resolve("XSpLit");
    assertFalse(Files.exists(xsplittDir), "precondition: XSpLit parent must be absent");

    injectDescriptor(PSConfigurationTypes.TIDY_CONFIG, xsplittDir.toFile());

    byte[] payload = "indent-spaces: 2\n".getBytes(StandardCharsets.UTF_8);
    PSMimeContentAdapter config = new PSMimeContentAdapter();
    config.setName(PSConfigurationTypes.TIDY_CONFIG.name());
    config.setContent(new ByteArrayInputStream(payload));
    config.setContentLength(payload.length);

    service.saveConfiguration(config);

    Path saved = xsplittDir.resolve(PSConfigurationTypes.TIDY_CONFIG.getFileName());
    assertTrue(Files.isDirectory(xsplittDir));
    assertTrue(Files.isRegularFile(saved));
    assertEquals("indent-spaces: 2\n", Files.readString(saved));
    // Cross-platform: Path resolve/readString — no hardcoded separators in assertions.
    assertEquals(xsplittDir.resolve("tidy.properties").normalize(), saved.normalize());
  }

  @Test
  void saveConfiguration_createsMissingXSpLitParentForServerPageTags() throws Exception {
    Path xsplittDir = tempDir.resolve("rxconfig").resolve("XSpLit");
    assertFalse(Files.exists(xsplittDir), "precondition: XSpLit parent must be absent");

    injectDescriptor(PSConfigurationTypes.SERVER_PAGE_TAGS, xsplittDir.toFile());

    byte[] payload = "<serverPageTags/>\n".getBytes(StandardCharsets.UTF_8);
    PSMimeContentAdapter config = new PSMimeContentAdapter();
    config.setName(PSConfigurationTypes.SERVER_PAGE_TAGS.name());
    config.setContent(new ByteArrayInputStream(payload));
    config.setContentLength(payload.length);

    service.saveConfiguration(config);

    Path saved = xsplittDir.resolve(PSConfigurationTypes.SERVER_PAGE_TAGS.getFileName());
    assertTrue(Files.isDirectory(xsplittDir));
    assertTrue(Files.isRegularFile(saved));
    assertEquals("<serverPageTags/>\n", Files.readString(saved));
  }

  @Test
  void saveConfiguration_succeedsWhenParentAlreadyExists() throws Exception {
    Path xsplittDir = tempDir.resolve("rxconfig").resolve("XSpLit");
    Files.createDirectories(xsplittDir);

    injectDescriptor(PSConfigurationTypes.TIDY_CONFIG, xsplittDir.toFile());

    byte[] payload = "wrap: 72\n".getBytes(StandardCharsets.UTF_8);
    PSMimeContentAdapter config = new PSMimeContentAdapter();
    config.setName(PSConfigurationTypes.TIDY_CONFIG.name());
    config.setContent(new ByteArrayInputStream(payload));
    config.setContentLength(payload.length);

    service.saveConfiguration(config);

    Path saved = xsplittDir.resolve(PSConfigurationTypes.TIDY_CONFIG.getFileName());
    assertTrue(Files.isRegularFile(saved));
    assertEquals("wrap: 72\n", Files.readString(saved));
  }

  @Test
  void saveConfiguration_propagatesWhenParentPathIsRegularFile() throws Exception {
    Path rxconfig = tempDir.resolve("rxconfig");
    Files.createDirectories(rxconfig);
    Path blockedParent = rxconfig.resolve("XSpLit");
    Files.writeString(blockedParent, "not-a-directory");
    assertTrue(Files.isRegularFile(blockedParent), "precondition: parent path is a file");

    injectDescriptor(PSConfigurationTypes.TIDY_CONFIG, blockedParent.toFile());

    byte[] payload = "indent-spaces: 2\n".getBytes(StandardCharsets.UTF_8);
    PSMimeContentAdapter config = new PSMimeContentAdapter();
    config.setName(PSConfigurationTypes.TIDY_CONFIG.name());
    config.setContent(new ByteArrayInputStream(payload));
    config.setContentLength(payload.length);

    IOException thrown = assertThrows(IOException.class, () -> service.saveConfiguration(config));
    assertTrue(
        thrown instanceof FileAlreadyExistsException
            || thrown.getCause() instanceof FileAlreadyExistsException
            || thrown.getMessage() != null,
        "createDirectories must fail when parent path is an existing file");
  }

  /**
   * Injects a {@code PSMimeContentDescriptor} for {@code type} under {@code configDir} without
   * calling {@code initContentDescriptors} (which needs a live {@code PSServer} config root).
   * Descriptor type is package-private inner; map values are stored as {@link Object}.
   */
  private void injectDescriptor(PSConfigurationTypes type, File configDir) throws Exception {
    Class<?> descClass =
        Class.forName(
            "com.percussion.services.system.impl.PSSystemService$PSMimeContentDescriptor");
    Constructor<?> ctor =
        descClass.getDeclaredConstructor(
            PSSystemService.class, IPSGuid.class, String.class, File.class);
    ctor.setAccessible(true);
    Object descriptor =
        ctor.newInstance(
            service,
            new PSGuid(PSTypeEnum.CONFIGURATION, type.getId()),
            type.getFileName(),
            configDir);

    Map<PSConfigurationTypes, Object> map = new EnumMap<>(PSConfigurationTypes.class);
    map.put(type, descriptor);
    ReflectionTestUtils.setField(service, "m_mimeContentMap", map);
  }
}
