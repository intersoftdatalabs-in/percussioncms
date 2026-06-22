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

package com.percussion.rest.pages;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.percussion.rest.errors.BackendException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PagesTest {

  @Mock IPageAdaptor adaptor;

  @Mock UriInfo uriInfo;

  @InjectMocks PagesResource resource;

  @BeforeEach
  void setUp() {
    lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/api").build());
  }

  @Test
  void shouldReturnPageById() throws Exception {
    Page page = new Page();
    page.setId("1234");
    when(adaptor.getPage(any(), eq("1234"))).thenReturn(page);

    Page result = resource.getPageById("1234");

    assertSame(page, result);
    verify(adaptor).getPage(uriInfo.getBaseUri(), "1234");
  }

  @Test
  void shouldThrowWebAppExceptionWhenPageByIdFails() throws Exception {
    when(adaptor.getPage(any(), eq("bad")))
        .thenThrow(new BackendException("fail", new Exception("cause")));
    assertThrows(WebApplicationException.class, () -> resource.getPageById("bad"));
  }

  @Test
  void shouldDecodeAndDelegatePathLookup() throws Exception {
    Page page = new Page();
    when(adaptor.getPage(any(), anyString(), anyString(), anyString())).thenReturn(page);

    Page result = resource.getPage("site/mypage");
    assertSame(page, result);
    verify(adaptor).getPage(uriInfo.getBaseUri(), "site", "", "mypage");
  }

  @Test
  void updatePage_shouldEnforcePathConstraints() {
    Page p = new Page();
    p.setName("foo");
    p.setFolderPath("bar");
    p.setSiteName("baz");

    assertThrows(
        WebApplicationException.class, () -> resource.updatePage(p, "site/other/page.html"));
  }

  @Test
  void updatePage_shouldCallAdaptorWhenValid() throws Exception {
    Page p = new Page();
    p.setName("page.html");
    p.setFolderPath("path");
    p.setSiteName("site");
    when(adaptor.updatePage(any(), any())).thenReturn(p);

    Page updated = resource.updatePage(p, "site/path/page.html");

    assertSame(p, updated);
    verify(adaptor).updatePage(uriInfo.getBaseUri(), p);
  }

  @Test
  void updatePage_backendExceptionWrapped() throws Exception {
    Page p = new Page();
    p.setName("page.html");
    p.setFolderPath("path");
    p.setSiteName("site");
    when(adaptor.updatePage(any(), any()))
        .thenThrow(new BackendException("oops", new Exception("cause")));

    assertThrows(
        WebApplicationException.class, () -> resource.updatePage(p, "site/path/page.html"));
  }

  @Test
  public void testPageWithLeadingSitesPath() {
    String responseMsg =
        target("pages/by-path/Sites/sitea/path1/pathsub%20/pathsub2/pathsub3/page1.html")
            .request(MediaType.APPLICATION_JSON)
            .get(String.class);
    assertTrue("Name should match", responseMsg.contains("page1.html"));

    String responseMsgLeadingSlash =
        target("pages/by-path//Sites/sitea/path1/pathsub%20/pathsub2/pathsub3/page1.html")
            .request(MediaType.APPLICATION_JSON)
            .get(String.class);
    assertTrue("Name should match", responseMsgLeadingSlash.contains("page1.html"));

    Response deleteResponse =
        target("pages/by-path/Sites/sitea/path1/pathsub%20/pathsub2/pathsub3/page1.html")
            .request()
            .delete();
    assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
  }
}
