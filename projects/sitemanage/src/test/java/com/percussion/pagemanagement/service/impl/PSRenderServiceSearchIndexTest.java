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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Unit tests for {@link PSRenderService#renderPageForSearchIndex(String)} TX handling used by FTS
 * HTML extract.
 */
@DisplayName("PSRenderService#renderPageForSearchIndex")
class PSRenderServiceSearchIndexTest {

  private IPSRenderAssemblyBridge bridge;
  private PlatformTransactionManager tm;
  private TransactionStatus status;
  private PSRenderService service;

  @BeforeEach
  void setUp() {
    bridge = mock(IPSRenderAssemblyBridge.class);
    tm = mock(PlatformTransactionManager.class);
    status = mock(TransactionStatus.class);
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
    service =
        new PSRenderService(mock(IPSPageService.class), bridge, mock(IPSTemplateService.class), tm);
  }

  @Test
  void commitsAndReturnsHtmlOnSuccess() throws Exception {
    when(status.isRollbackOnly()).thenReturn(false);
    when(bridge.renderPage(eq("page-1"), eq(false), eq(false))).thenReturn("<html/>");
    assertEquals("<html/>", service.renderPageForSearchIndex("page-1"));
    verify(tm).commit(status);
    verify(tm, never()).rollback(status);
  }

  @Test
  void nullHtmlBecomesEmptyString() throws Exception {
    when(status.isRollbackOnly()).thenReturn(false);
    when(bridge.renderPage(anyString(), anyBoolean(), anyBoolean())).thenReturn(null);
    assertEquals("", service.renderPageForSearchIndex("page-1"));
    verify(tm).commit(status);
  }

  @Test
  void rollbackOnlyThrowsClearRuntimeException() throws Exception {
    when(status.isRollbackOnly()).thenReturn(true);
    when(bridge.renderPage(anyString(), anyBoolean(), anyBoolean())).thenReturn("<fail/>");
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> service.renderPageForSearchIndex("page-1"));
    assertTrue(ex.getMessage().contains("rolled back"));
    verify(tm).rollback(status);
    verify(tm, never()).commit(status);
  }

  @Test
  void assemblyExceptionRollsBackAndRethrows() throws Exception {
    when(bridge.renderPage(anyString(), anyBoolean(), anyBoolean()))
        .thenThrow(new RuntimeException("assembly boom"));
    when(status.isCompleted()).thenReturn(false);
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> service.renderPageForSearchIndex("page-1"));
    assertEquals("assembly boom", ex.getMessage());
    verify(tm).rollback(status);
    verify(tm, never()).commit(status);
  }
}
