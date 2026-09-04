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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.serverconfigs.ServerConfigSummary;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.data.PSConfigurationTypes;
import com.percussion.services.system.data.PSMimeContentAdapter;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SY-02 Admin PUT updates allow-listed {@link PSConfigurationTypes} file bodies via {@link
 * IPSSystemService#saveConfiguration}. Non-allow-listed names never reach save.
 */
@Tag("UnitTest")
class ServerConfigAdaptorWriteTest {

  private IPSSystemService systemService;
  private ServerConfigAdaptor adaptor;
  private final AtomicReference<String> savedContent = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    systemService = mock(IPSSystemService.class);
    adaptor = new ServerConfigAdaptor(systemService, () -> true);
    savedContent.set(null);

    when(systemService.loadConfiguration(any(PSConfigurationTypes.class)))
        .thenAnswer(
            inv -> {
              PSConfigurationTypes type = inv.getArgument(0);
              PSMimeContentAdapter content = new PSMimeContentAdapter();
              content.setName(type.name());
              String text = savedContent.get() != null ? savedContent.get() : "original";
              byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
              content.setContent(new ByteArrayInputStream(bytes));
              content.setContentLength(bytes.length);
              content.setMimeType("text/plain");
              content.setCharacterEncoding("UTF-8");
              return content;
            });

    doAnswer(
            inv -> {
              PSMimeContentAdapter config = inv.getArgument(0);
              // valueOf throws for unknown names — same gate production save uses
              PSConfigurationTypes.valueOf(config.getName());
              savedContent.set(
                  new String(config.getContent().readAllBytes(), StandardCharsets.UTF_8));
              return null;
            })
        .when(systemService)
        .saveConfiguration(any(PSMimeContentAdapter.class));
  }

  @Test
  void update_savesAllowListedContentAndRoundTrips() throws Exception {
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("rootLogger=DEBUG");

    ServerConfigSummary out = adaptor.updateConfig("LOG_CONFIG", body);

    assertNotNull(out);
    assertEquals("LOG_CONFIG", out.getName());
    assertEquals("rootLogger=DEBUG", out.getContent());
    assertEquals("rootLogger=DEBUG", savedContent.get());

    ArgumentCaptor<PSMimeContentAdapter> cap = ArgumentCaptor.forClass(PSMimeContentAdapter.class);
    verify(systemService).saveConfiguration(cap.capture());
    assertEquals("LOG_CONFIG", cap.getValue().getName());
  }

  @Test
  void update_emptyContentAllowed() {
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("");

    ServerConfigSummary out = adaptor.updateConfig("TIDY_CONFIG", body);

    assertNotNull(out);
    assertEquals("", savedContent.get());
  }

  @Test
  void update_unknownEnumNameIsNullAndDoesNotSave() throws Exception {
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("x");

    assertNull(adaptor.updateConfig("NOT_A_REAL_CONFIG", body));
    verify(systemService, never()).saveConfiguration(any());
  }

  @Test
  void update_pathTraversalIsNullAndDoesNotSave() throws Exception {
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("x");

    assertNull(adaptor.updateConfig("../etc/passwd", body));
    assertNull(adaptor.updateConfig("a/b", body));
    assertNull(adaptor.updateConfig("LOG_CONFIG\\x", body));
    verify(systemService, never()).saveConfiguration(any());
  }

  @Test
  void update_nullContentIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.updateConfig("LOG_CONFIG", new ServerConfigSummary()));
    assertTrue(ex.getMessage().contains("content is required"));
    verify(systemService, never()).saveConfiguration(any());
  }

  @Test
  void update_nullBodyIs400() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateConfig("LOG_CONFIG", null));
    verify(systemService, never()).saveConfiguration(any());
  }

  @Test
  void update_nonAdminIs403AndDoesNotSave() throws Exception {
    adaptor = new ServerConfigAdaptor(systemService, () -> false);
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateConfig("LOG_CONFIG", body));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(ServerConfigAdaptor.ADMIN_REQUIRED, ex.getMessage());
    verify(systemService, never()).saveConfiguration(any());
  }

  @Test
  void update_ioFailureIs500() throws Exception {
    doThrow(new IOException("disk full"))
        .when(systemService)
        .saveConfiguration(any(PSMimeContentAdapter.class));
    ServerConfigSummary body = new ServerConfigSummary();
    body.setContent("x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateConfig("LOG_CONFIG", body));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  void find_stillRejectsUnsafeKeys() {
    assertNull(adaptor.findConfigByName("../x"));
    assertNull(adaptor.findConfigByName("NOT_REAL"));
  }
}
