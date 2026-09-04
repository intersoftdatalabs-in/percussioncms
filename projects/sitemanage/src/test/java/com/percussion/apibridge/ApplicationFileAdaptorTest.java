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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.rest.applicationfiles.ApplicationFileSummary;
import com.percussion.security.PSSecurityToken;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * SY-05 path-safe application CMS/resource list/get/put. Catalog allow-list + relative-path
 * barriers never reach object-store write for unsafe input.
 */
@Tag("UnitTest")
class ApplicationFileAdaptorTest {

  private ApplicationFileAdaptor.ApplicationFileStore fileStore;
  private PSSecurityToken token;
  private ApplicationFileAdaptor adaptor;
  private final AtomicReference<String> savedContent = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    fileStore = mock(ApplicationFileAdaptor.ApplicationFileStore.class);
    token = mock(PSSecurityToken.class);
    savedContent.set(null);

    PSApplicationSummary sum = mock(PSApplicationSummary.class);
    when(sum.getId()).thenReturn(42);
    when(sum.getName()).thenReturn("sys_resources");

    adaptor =
        new ApplicationFileAdaptor(
            tok -> new PSApplicationSummary[] {sum}, fileStore, () -> true, () -> token);

    when(fileStore.listFiles(eq("sys_resources")))
        .thenReturn(
            List.of(
                    new File("ApplicationFiles" + File.separator + "a.css"),
                    new File("ApplicationFiles" + File.separator + "b.js"))
                .iterator());

    when(fileStore.read(eq("sys_resources"), any(File.class), eq(token)))
        .thenAnswer(
            inv -> {
              String text = savedContent.get() != null ? savedContent.get() : "original";
              return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            });

    org.mockito.Mockito.doAnswer(
            inv -> {
              InputStream in = inv.getArgument(2);
              savedContent.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
              return null;
            })
        .when(fileStore)
        .write(eq("sys_resources"), any(File.class), any(InputStream.class), anyBoolean(), eq(token));
  }

  @Test
  void list_mapsRelativePathsWithForwardSlash() {
    List<ApplicationFileSummary> out = adaptor.listFiles("sys_resources");
    assertNotNull(out);
    assertEquals(2, out.size());
    assertEquals("ApplicationFiles/a.css", out.get(0).getPath());
    assertEquals("ApplicationFiles/b.js", out.get(1).getPath());
    assertEquals("sys_resources", out.get(0).getApplicationName());
    assertNull(out.get(0).getDesignGaps());
  }

  @Test
  void list_unknownAppIsNull() {
    assertNull(adaptor.listFiles("no_such_app"));
    assertNull(adaptor.listFiles("../escape"));
  }

  @Test
  void get_roundTripsUtf8Content() {
    ApplicationFileSummary out = adaptor.getFile("sys_resources", "ApplicationFiles/a.css");
    assertNotNull(out);
    assertEquals("original", out.getContent());
    assertEquals("ApplicationFiles/a.css", out.getPath());
    assertEquals("text/css", out.getMimeType());
    assertNotNull(out.getDesignGaps());
    assertTrue(out.getDesignGaps().stream().anyMatch(g -> g.contains("serverconfigs")));
  }

  @Test
  void get_rejectsUnsafePathsWithoutRead() throws Exception {
    assertNull(adaptor.getFile("sys_resources", "../escape.txt"));
    assertNull(adaptor.getFile("sys_resources", "/etc/passwd"));
    assertNull(adaptor.getFile("sys_resources", "C:\\Windows\\win.ini"));
    assertNull(adaptor.getFile("sys_resources", "a/../../b.txt"));
    assertNull(adaptor.getFile("sys_resources", ""));
    verify(fileStore, never()).read(any(), any(), any());
  }

  @Test
  void put_savesAllowListedPathAndRoundTrips() throws Exception {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("body{color:red}");
    // Body path must not drive persistence
    body.setPath("../../evil.css");

    ApplicationFileSummary out =
        adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body);

    assertNotNull(out);
    assertEquals("body{color:red}", out.getContent());
    assertEquals("body{color:red}", savedContent.get());
    assertEquals("ApplicationFiles/a.css", out.getPath());
    verify(fileStore)
        .write(
            eq("sys_resources"),
            any(File.class),
            any(InputStream.class),
            eq(true),
            eq(token));
  }

  @Test
  void put_pathTraversalNeverWrites() throws Exception {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    assertNull(adaptor.putFile("sys_resources", "../escape.txt", body));
    assertNull(adaptor.putFile("sys_resources", "a\\..\\b.txt", body));
    assertNull(adaptor.putFile("nope", "ApplicationFiles/a.css", body));
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any());
  }

  @Test
  void put_nullContentIs400() throws Exception {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.putFile(
                    "sys_resources", "ApplicationFiles/a.css", new ApplicationFileSummary()));
    assertTrue(ex.getMessage().contains("content is required"));
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any());
  }

  @Test
  void put_nonAdminIs403AndDoesNotWrite() throws Exception {
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            () -> false,
            () -> token);
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(ApplicationFileAdaptor.ADMIN_REQUIRED, ex.getMessage());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any());
  }

  @Test
  void put_nullUserServiceFailsClosedAs403() throws Exception {
    // adminChecker null → isCurrentUserAdmin; userService field stays null → false.
    adaptor =
        new ApplicationFileAdaptor(
            tok -> {
              PSApplicationSummary sum = mock(PSApplicationSummary.class);
              when(sum.getName()).thenReturn("sys_resources");
              when(sum.getId()).thenReturn(42);
              return new PSApplicationSummary[] {sum};
            },
            fileStore,
            null,
            () -> token);
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.putFile("sys_resources", "ApplicationFiles/a.css", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(fileStore, never()).write(any(), any(), any(), anyBoolean(), any());
  }

  @Test
  void toListSummary_setsDirectoryFlag() {
    ApplicationFileSummary file =
        ApplicationFileAdaptor.toListSummary("sys_resources", "ApplicationFiles/a.css", false);
    assertFalse(Boolean.TRUE.equals(file.getDirectory()));
    ApplicationFileSummary dir =
        ApplicationFileAdaptor.toListSummary("sys_resources", "ApplicationFiles", true);
    assertTrue(Boolean.TRUE.equals(dir.getDirectory()));
    assertEquals("ApplicationFiles", dir.getName());
  }

  @Test
  void normalizeSafeRelativePath_acceptsNestedRelative() {
    assertEquals(
        "ApplicationFiles/css/site.css",
        ApplicationFileAdaptor.normalizeSafeRelativePath("ApplicationFiles/css/site.css"));
    assertEquals(
        "ApplicationFiles/css/site.css",
        ApplicationFileAdaptor.normalizeSafeRelativePath("ApplicationFiles\\css\\site.css"));
  }

  @Test
  void normalizeSafeRelativePath_rejectsTraversalAndAbsolute() {
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("../x"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("a/../b"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("/abs"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("C:/Windows/x"));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath(null));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath(""));
    assertNull(ApplicationFileAdaptor.normalizeSafeRelativePath("a/\0/b"));
  }

  @Test
  void isSafeApplicationName_rejectsSeparators() {
    assertTrue(ApplicationFileAdaptor.isSafeApplicationName("sys_resources"));
    assertFalse(ApplicationFileAdaptor.isSafeApplicationName("../x"));
    assertFalse(ApplicationFileAdaptor.isSafeApplicationName("a/b"));
    assertFalse(ApplicationFileAdaptor.isSafeApplicationName("a\\b"));
  }

  @Test
  void resolveApplicationName_usesCatalogNotRawInput() {
    PSApplicationSummary sum = mock(PSApplicationSummary.class);
    when(sum.getId()).thenReturn(7);
    when(sum.getName()).thenReturn("sys_resources");
    PSApplicationSummary[] sums = {sum};

    assertEquals(
        "sys_resources", ApplicationFileAdaptor.resolveApplicationName("SYS_RESOURCES", sums));
    assertEquals("sys_resources", ApplicationFileAdaptor.resolveApplicationName("7", sums));
    assertNull(ApplicationFileAdaptor.resolveApplicationName("other", sums));
    assertNull(ApplicationFileAdaptor.resolveApplicationName("../x", sums));
  }

  @Test
  void list_emptyIteratorIsEmptyListNotNull() throws Exception {
    when(fileStore.listFiles(eq("sys_resources")))
        .thenReturn(
            new Iterator<>() {
              @Override
              public boolean hasNext() {
                return false;
              }

              @Override
              public File next() {
                return null;
              }
            });
    List<ApplicationFileSummary> out = adaptor.listFiles("sys_resources");
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }
}
