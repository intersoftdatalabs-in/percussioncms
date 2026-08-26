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
package com.percussion.sitemanage.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import java.net.HttpURLConnection;
import java.net.URI;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

/**
 * {@link PSSiteImporter#getRedirectedUrl} treats 301/302 as protocol statuses via JDK constants
 * (issue #3846).
 */
@Tag("UnitTest")
class PSSiteImporterHttpErrorCodesTest {

  private static final String ORIGINAL = "http://example.test/page";
  private static final String FINAL_URL = "http://example.test/final";
  private static final String USER_AGENT = "PercussionCMS-Test";

  @Test
  void isMovedHttpRedirectMatchesJdk301And302Only() {
    assertTrue(PSSiteImporter.isMovedHttpRedirect(HttpURLConnection.HTTP_MOVED_PERM));
    assertTrue(PSSiteImporter.isMovedHttpRedirect(HttpURLConnection.HTTP_MOVED_TEMP));
    assertFalse(PSSiteImporter.isMovedHttpRedirect(HttpURLConnection.HTTP_OK));
    assertFalse(PSSiteImporter.isMovedHttpRedirect(HttpURLConnection.HTTP_NOT_FOUND));
    assertEquals(301, HttpURLConnection.HTTP_MOVED_PERM);
    assertEquals(302, HttpURLConnection.HTTP_MOVED_TEMP);
  }

  @Test
  void getRedirectedUrlReturnsOriginalWhenNotMoved() throws Exception {
    Connection conn = mock(Connection.class, Answers.RETURNS_SELF);
    Connection.Response response = mock(Connection.Response.class);
    when(conn.get()).thenReturn(mock(Document.class));
    when(conn.response()).thenReturn(response);
    when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);

    IPSSiteImportLogger logger = mock(IPSSiteImportLogger.class);

    try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
      jsoup.when(() -> Jsoup.connect(ORIGINAL)).thenReturn(conn);
      assertEquals(ORIGINAL, PSSiteImporter.getRedirectedUrl(ORIGINAL, logger, USER_AGENT));
      verify(logger, never()).appendLogMessage(any(), anyString(), anyString());
    }
  }

  @Test
  void getRedirectedUrlFollowsMovedPermanently() throws Exception {
    assertFollowsRedirect(HttpURLConnection.HTTP_MOVED_PERM);
  }

  @Test
  void getRedirectedUrlFollowsMovedTemporarily() throws Exception {
    assertFollowsRedirect(HttpURLConnection.HTTP_MOVED_TEMP);
  }

  private static void assertFollowsRedirect(int status) throws Exception {
    Connection probe = mock(Connection.class, Answers.RETURNS_SELF);
    Connection follow = mock(Connection.class, Answers.RETURNS_SELF);
    Connection.Response probeResp = mock(Connection.Response.class);
    Connection.Response followResp = mock(Connection.Response.class);
    when(probe.get()).thenReturn(mock(Document.class));
    when(probe.response()).thenReturn(probeResp);
    when(probeResp.statusCode()).thenReturn(status);
    when(follow.get()).thenReturn(mock(Document.class));
    when(follow.response()).thenReturn(followResp);
    when(followResp.url()).thenReturn(URI.create(FINAL_URL).toURL());

    IPSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);

    try (MockedStatic<Jsoup> jsoup = mockStatic(Jsoup.class)) {
      jsoup.when(() -> Jsoup.connect(ORIGINAL)).thenReturn(probe, follow);
      assertEquals(FINAL_URL, PSSiteImporter.getRedirectedUrl(ORIGINAL, logger, USER_AGENT));
    }
    assertTrue(logger.getLog().contains(ORIGINAL));
    assertTrue(logger.getLog().contains(FINAL_URL));
  }
}
