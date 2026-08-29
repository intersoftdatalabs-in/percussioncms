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

package com.percussion.rest.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.DesignGap;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class TemplatesResourceDetailTest {

  private ITemplatesAdaptor adaptor;
  private TemplatesResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ITemplatesAdaptor.class);
    resource = new TemplatesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    Field f = TemplatesResource.class.getDeclaredField("uriInfo");
    f.setAccessible(true);
    f.set(resource, uriInfo);
  }

  @Test
  public void getTemplateReturnsDetail() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    when(adaptor.getTemplate(any(), eq("perc.page"))).thenReturn(d);

    TemplateDetail out = resource.getTemplate("perc.page");
    assertEquals("perc.page", out.getName());
  }

  @Test
  public void getTemplateReturnsTemplateSource() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    d.setTemplateSource("#header()\n$body\n");
    when(adaptor.getTemplate(any(), eq("perc.page"))).thenReturn(d);

    TemplateDetail out = resource.getTemplate("perc.page");
    assertEquals("#header()\n$body\n", out.getTemplateSource());
  }

  @Test
  public void getTemplateReturnsStructuredDesignGaps() {
    TemplateDetail d = new TemplateDetail();
    d.setName("perc.page");
    d.setDesignGaps(
        List.of(DesignGap.of("TPL_CREATE_DELETE_LOCK", "Create / delete / lock not supported via this API")));
    when(adaptor.getTemplate(any(), eq("perc.page"))).thenReturn(d);

    TemplateDetail out = resource.getTemplate("perc.page");
    assertNotNull(out.getDesignGaps());
    assertEquals(1, out.getDesignGaps().size());
    assertEquals("TPL_CREATE_DELETE_LOCK", out.getDesignGaps().get(0).getCode());
    assertEquals(
        "Create / delete / lock not supported via this API", out.getDesignGaps().get(0).getMessage());
  }

  @Test
  public void getTemplateNotFound() {
    when(adaptor.getTemplate(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getTemplate("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getTemplateWrapsFailures() {
    when(adaptor.getTemplate(any(), eq("boom"))).thenThrow(new IllegalStateException("fail"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getTemplate("boom"));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void updateTemplateSuccess() {
    TemplateDetail body = new TemplateDetail();
    body.setLabel("New Label");
    TemplateDetail updated = new TemplateDetail();
    updated.setName("perc.page");
    updated.setLabel("New Label");
    when(adaptor.updateTemplate(any(), eq("perc.page"), any())).thenReturn(updated);
    assertEquals("New Label", resource.updateTemplate("perc.page", body).getLabel());
  }

  @Test
  public void updateTemplateNotFound() {
    when(adaptor.updateTemplate(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateTemplate("missing", new TemplateDetail()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateTemplateBadRequest() {
    when(adaptor.updateTemplate(any(), eq("perc.page"), any()))
        .thenThrow(new IllegalArgumentException("body is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateTemplate("perc.page", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateTemplateWithBindings() {
    TemplateDetail body = new TemplateDetail();
    TemplateBindingSummary b = new TemplateBindingSummary();
    b.setExecutionOrder(1);
    b.setVariable("$sys.item");
    b.setExpression("$sys.item");
    body.setBindings(java.util.List.of(b));
    TemplateDetail updated = new TemplateDetail();
    updated.setName("perc.page");
    updated.setBindings(java.util.List.of(b));
    when(adaptor.updateTemplate(any(), eq("perc.page"), any())).thenReturn(updated);
    assertEquals(1, resource.updateTemplate("perc.page", body).getBindings().size());
  }

  @Test
  public void updateTemplateWithAssembler() {
    TemplateDetail body = new TemplateDetail();
    body.setAssembler("Java/global/percussion/assembly/htmlAssembler");
    TemplateDetail updated = new TemplateDetail();
    updated.setName("perc.page");
    updated.setAssembler("Java/global/percussion/assembly/htmlAssembler");
    when(adaptor.updateTemplate(any(), eq("perc.page"), any())).thenReturn(updated);
    assertEquals(
        "Java/global/percussion/assembly/htmlAssembler",
        resource.updateTemplate("perc.page", body).getAssembler());
  }

  @Test
  public void createTemplateSuccess() {
    TemplateDetail body = new TemplateDetail();
    body.setName("site.html.snippet");
    TemplateDetail created = new TemplateDetail();
    created.setName("site.html.snippet");
    created.setAssembler("Java/global/percussion/assembly/htmlAssembler");
    when(adaptor.createTemplate(any(), any())).thenReturn(created);
    TemplateDetail out = resource.createTemplate(body);
    assertEquals("site.html.snippet", out.getName());
    assertEquals("Java/global/percussion/assembly/htmlAssembler", out.getAssembler());
  }

  @Test
  public void createTemplateBadRequest() {
    when(adaptor.createTemplate(any(), any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createTemplate(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createTemplateWrapsFailures() {
    when(adaptor.createTemplate(any(), any())).thenThrow(new IllegalStateException("fail"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createTemplate(new TemplateDetail()));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void deleteTemplateSuccess() {
    when(adaptor.deleteTemplate(any(), eq("site.html.snippet"))).thenReturn(true);
    Response out = resource.deleteTemplate("site.html.snippet");
    assertEquals(204, out.getStatus());
    verify(adaptor).deleteTemplate(any(), eq("site.html.snippet"));
  }

  @Test
  public void deleteTemplateNotFound() {
    when(adaptor.deleteTemplate(any(), eq("missing"))).thenReturn(false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteTemplate("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteTemplateBadRequest() {
    when(adaptor.deleteTemplate(any(), eq("  ")))
        .thenThrow(new IllegalArgumentException("idOrName is required"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteTemplate("  "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteTemplateWrapsFailures() {
    when(adaptor.deleteTemplate(any(), eq("boom"))).thenThrow(new IllegalStateException("fail"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteTemplate("boom"));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void exportTemplateReturnsXmlAndFilenameFromName() {
    TemplateExport exported =
        new TemplateExport(
            "perc.page", "<assembly-template><name>perc.page</name></assembly-template>");
    when(adaptor.exportTemplate(any(), eq("perc.page"))).thenReturn(exported);

    Response out = resource.exportTemplate("perc.page");
    assertEquals(200, out.getStatus());
    assertEquals(exported.getXml(), out.getEntity());
    String disposition = String.valueOf(out.getHeaderString("Content-Disposition"));
    assertTrue(disposition.contains("perc.page.xml"));
    verify(adaptor).exportTemplate(any(), eq("perc.page"));
  }

  @Test
  public void exportTemplateNotFound() {
    when(adaptor.exportTemplate(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.exportTemplate("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void exportTemplateForbidden() {
    when(adaptor.exportTemplate(any(), eq("perc.page")))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.exportTemplate("perc.page"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void exportTemplateWrapsFailures() {
    when(adaptor.exportTemplate(any(), eq("boom"))).thenThrow(new IllegalStateException("fail"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.exportTemplate("boom"));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void exportFilenameSanitizesPathCharacters() {
    assertEquals("perc.page.xml", TemplatesResource.exportFilename("perc.page"));
    assertEquals("a_b.xml", TemplatesResource.exportFilename("a/b"));
    assertEquals("template.xml", TemplatesResource.exportFilename("  "));
  }

  @Test
  public void exportFilenameShortNamesAppendXmlWithoutThrowing() {
    assertEquals("a.xml", TemplatesResource.exportFilename("a"));
    assertEquals("ab.xml", TemplatesResource.exportFilename("ab"));
    assertEquals("abc.xml", TemplatesResource.exportFilename("abc"));
    assertEquals("a.xml", TemplatesResource.exportFilename("a.xml"));
    assertEquals("Foo.XML", TemplatesResource.exportFilename("Foo.XML"));
  }
}
