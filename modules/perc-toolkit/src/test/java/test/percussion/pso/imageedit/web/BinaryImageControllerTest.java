/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package test.percussion.pso.imageedit.web;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.imageedit.data.ImageData;
import com.percussion.pso.imageedit.services.cache.ImageCacheManager;
import com.percussion.pso.imageedit.web.BinaryImageController;
import com.percussion.pso.imageedit.web.ImageUrlBuilder;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class BinaryImageControllerTest {
  private static final Logger log = LogManager.getLogger(BinaryImageControllerTest.class);

  BinaryImageController cut;
  @Mock
  ImageUrlBuilder urlBldr;
  @Mock
  ImageCacheManager cacheMgr;

  @BeforeEach
  public void setUp() {
    cut = new BinaryImageController();
    cut.setUrlBuilder(urlBldr);
    cut.setCacheMgr(cacheMgr);
  }

  @Test
  public final void testHandleRequestNormal() {
    try {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setMethod("GET");
      request.setRequestURI("/xyz/img1234.jpg");
      String body = "The quick brown fox jumped over the lazy dog";

      final ImageData data = new ImageData();
      data.setSize(body.length());
      data.setMimeType("text/plain");
      data.setBinary(body.getBytes());
      MockHttpServletResponse response = new MockHttpServletResponse();

      when(urlBldr.extractKey("/xyz/img1234.jpg")).thenReturn("1234");
      when(cacheMgr.hasImage("1234")).thenReturn(true);
      when(cacheMgr.getImage("1234")).thenReturn(data);

      cut.handleRequest(request, response);

      assertTrue(response.getContentLength() > 0);
      assertEquals(HttpServletResponse.SC_OK, response.getStatus());
      assertEquals("text/plain", response.getContentType());

      verify(urlBldr).extractKey("/xyz/img1234.jpg");
      verify(cacheMgr).hasImage("1234");
      verify(cacheMgr).getImage("1234");
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception caught");
    }
  }

  @Test
  public final void testHandleRequestNotFound() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI("/xyz/img1234.jpg");

    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      when(urlBldr.extractKey("/xyz/img1234.jpg")).thenReturn("1234");
      when(cacheMgr.hasImage("1234")).thenReturn(false);

      cut.handleRequest(request, response);

      verify(urlBldr).extractKey("/xyz/img1234.jpg");
      verify(cacheMgr).hasImage("1234");
      assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception caught");
    }
  }

  @Test
  public final void testHandleRequestNoContent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI("/xyz/img1234.jpg");

    final ImageData data = new ImageData();
    data.setSize(0);
    data.setMimeType("text/plain");

    MockHttpServletResponse response = new MockHttpServletResponse();

    when(urlBldr.extractKey("/xyz/img1234.jpg")).thenReturn("1234");
    when(cacheMgr.hasImage("1234")).thenReturn(true);
    when(cacheMgr.getImage("1234")).thenReturn(data);

    cut.handleRequest(request, response);

    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
    verify(cacheMgr).getImage("1234");
  }

  @Test
  public final void testHandleRequestNullRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");

    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      when(urlBldr.extractKey(anyString())).thenReturn(null);

      cut.handleRequest(request, response);

      verify(urlBldr).extractKey(anyString());
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception caught");
    }
  }
}
