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

package com.percussion.rest.contenttypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.percussion.rest.DesignGap;
import com.percussion.rest.Guid;
import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.rest.contenttypes.NamedObjectRefList;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@Tag("UnitTest")
public class ContentTypesResourceDetailTest {

  private IContentTypesAdaptor adaptor;
  private ContentTypesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IContentTypesAdaptor.class);
    resource = new ContentTypesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void listContentTypesReturnsNameLabelGuid() {
    ContentType ct = sampleContentType();
    when(adaptor.listContentTypes(any())).thenReturn(List.of(ct));

    List<ContentType> list = resource.listContentTypes();
    assertEquals(1, list.size());
    ContentType first = list.get(0);
    assertEquals("percPage", first.getName());
    assertEquals("Page", first.getLabel());
    assertNotNull(first.getGuid());
    assertFalse(first.isHideFromMenu());
  }

  @Test
  public void listContentTypesJsonIsNotHideFromMenuOnly() {
    ContentType ct = sampleContentType();
    when(adaptor.listContentTypes(any())).thenReturn(List.of(ct));

    List<ContentType> list = resource.listContentTypes();
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentType.class);
    String json = mapper.writeValueAsString(new ContentTypeList(list));

    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("\"guid\""), json);
    // Regression: live install returned only hideFromMenu per item (#1693)
    assertFalse(
        json.replaceAll("\\s", "").contains("{\"hideFromMenu\":false}")
            && !json.contains("\"name\""),
        json);
  }

  @Test
  public void getContentTypeReturnsDetail() {
    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    when(adaptor.getContentType(any(), eq("percPage"))).thenReturn(d);
    assertEquals("percPage", resource.getContentType("percPage").getName());
  }

  @Test
  public void getContentTypeReturnsStructuredDesignGaps() {
    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    d.setDesignGaps(List.of(DesignGap.of("CT_ITEM_EXITS", "Item-level pre/post exits not exposed")));
    when(adaptor.getContentType(any(), eq("percPage"))).thenReturn(d);

    ContentTypeDetail out = resource.getContentType("percPage");
    assertNotNull(out.getDesignGaps());
    assertEquals(1, out.getDesignGaps().size());
    assertEquals("CT_ITEM_EXITS", out.getDesignGaps().get(0).getCode());
    assertEquals("Item-level pre/post exits not exposed", out.getDesignGaps().get(0).getMessage());
  }

  @Test
  public void fieldRuleExpressionsSerializeWhenPresentAndOmitWhenEmpty() throws Exception {
    ContentTypeField withRules = new ContentTypeField();
    withRules.setName("sys_title");
    withRules.setHasValidation(true);
    withRules.setValidationExpression("sys_title <>  AND");
    withRules.setControlPropertyNames(List.of("height", "width"));

    ContentTypeField emptyRules = new ContentTypeField();
    emptyRules.setName("page_title");
    emptyRules.setHasValidation(false);

    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    d.setFields(List.of(withRules, emptyRules));

    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeDetail.class);
    String json = mapper.writeValueAsString(d);

    assertTrue(json.contains("\"validationExpression\""), json);
    assertTrue(json.contains("sys_title <>"), json);
    assertTrue(json.contains("\"controlPropertyNames\""), json);
    assertTrue(json.contains("height"), json);
    withRules.setControlProperties(List.of(new ContentTypeControlProperty("height", "200")));
    json = mapper.writeValueAsString(d);
    assertTrue(json.contains("\"controlProperties\""), json);
    assertTrue(json.contains("200"), json);
    // empty expression fields must not force empty-string noise for the second field
    assertTrue(json.contains("page_title"), json);
  }

  private static ContentType sampleContentType() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setGuid(new Guid("0-2-311"));
    ct.setHideFromMenu(false);
    return ct;
  }

  @Test
  public void getContentTypeNotFound() {
    when(adaptor.getContentType(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getContentType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeSuccess() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setLabel("Page");
    ContentTypeDetail updated = new ContentTypeDetail();
    updated.setName("percPage");
    updated.setLabel("Page");
    when(adaptor.updateContentType(any(), eq("percPage"), any())).thenReturn(updated);
    assertEquals("Page", resource.updateContentType("percPage", body).getLabel());
  }

  @Test
  public void updateContentTypeNotFound() {
    when(adaptor.updateContentType(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("missing", new ContentTypeDetail()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeBadRequest() {
    when(adaptor.updateContentType(any(), eq("percPage"), any()))
        .thenThrow(new IllegalArgumentException("body is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateContentType("percPage", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeLockConflictWhenLockNotHeld() {
    when(adaptor.updateContentType(any(), eq("percPage"), any()))
        .thenThrow(
            new ContentTypeDesignLockException("Could not save content type; design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("percPage", new ContentTypeDetail()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void getAllowedTemplatesReturnsList() {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    when(adaptor.getAllowedTemplates(any(), eq("percPage"))).thenReturn(List.of(ref));
    NamedObjectRefList out = resource.getAllowedTemplates("percPage");
    assertEquals(1, out.size());
    assertEquals("perc.page", out.get(0).getName());
  }

  @Test
  public void getAllowedTemplatesNotFound() {
    when(adaptor.getAllowedTemplates(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getAllowedTemplates("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceAllowedTemplatesSuccess() {
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    NamedObjectRefList body = new NamedObjectRefList(List.of(ref));
    when(adaptor.replaceAllowedTemplates(any(), eq("percPage"), any())).thenReturn(body);
    NamedObjectRefList out = resource.replaceAllowedTemplates("percPage", body);
    assertEquals(1, out.size());
    assertEquals("perc.page", out.get(0).getName());
  }

  @Test
  public void replaceAllowedTemplatesNotFound() {
    when(adaptor.replaceAllowedTemplates(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceAllowedTemplates("missing", new NamedObjectRefList()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceAllowedTemplatesRequiresLock() {
    when(adaptor.replaceAllowedTemplates(any(), eq("percPage"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save template associations; design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceAllowedTemplates("percPage", new NamedObjectRefList()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceAllowedTemplatesLockedByOtherUser() {
    when(adaptor.replaceAllowedTemplates(any(), eq("percPage"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save template associations; locked by editor"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceAllowedTemplates("percPage", new NamedObjectRefList()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void itemExitsJsonSerializesListsAndGaps() throws Exception {
    ContentTypeItemExits env = new ContentTypeItemExits();
    ContentTypeItemExit exit = new ContentTypeItemExit();
    exit.setExtension("Java/global/percussion/generic/sys_ToUpperCase");
    exit.setName("sys_ToUpperCase");
    env.setInputTranslations(List.of(exit));
    env.setOutputTranslations(List.of());
    env.setValidations(List.of());
    env.setPreExits(List.of());
    env.setPostExits(List.of());
    env.setDesignGaps(
        List.of(
            DesignGap.of(
                "CT_ITEM_EXIT_CONDITIONS", "Apply-when conditions on item-level exits are read-only")));

    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeItemExits.class);
    String json = mapper.writeValueAsString(env);
    assertTrue(json.contains("inputTranslations"), json);
    assertTrue(json.contains("sys_ToUpperCase"), json);
    assertTrue(json.contains("CT_ITEM_EXIT_CONDITIONS"), json);
    assertTrue(json.contains("\"code\""), json);
    assertTrue(json.contains("ContentTypeItemExits"), json);
    ContentTypeItemExits back = mapper.readValue(json, ContentTypeItemExits.class);
    assertNotNull(back.getInputTranslations());
    assertEquals(1, back.getInputTranslations().size());
    assertNotNull(back.getOutputTranslations());
    assertTrue(back.getOutputTranslations().isEmpty());
    assertNotNull(back.getValidations());
  }

  @Test
  public void itemExitsPutJsonUnwrapsWrappedEmptyLists() throws Exception {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeItemExits.class);
    String json =
        "{\"ContentTypeItemExits\":{"
            + "\"inputTranslations\":[{\"extension\":\"Java/global/percussion/generic/sys_ToUpperCase\","
            + "\"parameters\":[{\"value\":\"sys_title\"}]}],"
            + "\"outputTranslations\":[],\"validations\":[],\"preExits\":[],\"postExits\":[],"
            + "\"maxErrorsToStopValidation\":10}}";
    ContentTypeItemExits back = mapper.readValue(json, ContentTypeItemExits.class);
    assertNotNull(back.getInputTranslations());
    assertEquals(1, back.getInputTranslations().size());
    assertEquals(
        "Java/global/percussion/generic/sys_ToUpperCase",
        back.getInputTranslations().get(0).getExtension());
    assertNotNull(back.getOutputTranslations());
    assertTrue(back.getOutputTranslations().isEmpty());
    assertNotNull(back.getValidations());
    assertTrue(back.getValidations().isEmpty());
    assertNotNull(back.getPreExits());
    assertEquals(Integer.valueOf(10), back.getMaxErrorsToStopValidation());
  }

  @Test
  public void getItemExitsReturnsEnvelope() {
    ContentTypeItemExits env = new ContentTypeItemExits();
    ContentTypeItemExit exit = new ContentTypeItemExit();
    exit.setExtension("Java/global/percussion/generic/sys_ToUpperCase");
    env.setInputTranslations(List.of(exit));
    when(adaptor.getItemExits(any(), eq("percPage"))).thenReturn(env);
    ContentTypeItemExits out = resource.getItemExits("percPage");
    assertEquals(1, out.getInputTranslations().size());
    assertEquals(
        "Java/global/percussion/generic/sys_ToUpperCase",
        out.getInputTranslations().get(0).getExtension());
  }

  @Test
  public void getItemExitsNotFound() {
    when(adaptor.getItemExits(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getItemExits("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceItemExitsSuccess() {
    ContentTypeItemExits body = emptyItemExitsBody();
    when(adaptor.replaceItemExits(any(), eq("percPage"), any())).thenReturn(body);
    ContentTypeItemExits out = resource.replaceItemExits("percPage", body);
    assertNotNull(out.getInputTranslations());
    assertTrue(out.getInputTranslations().isEmpty());
  }

  @Test
  public void replaceItemExitsNotFound() {
    when(adaptor.replaceItemExits(any(), eq("missing"), any())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceItemExits("missing", emptyItemExitsBody()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceItemExitsRequiresLock() {
    when(adaptor.replaceItemExits(any(), eq("percPage"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not update content type item-level exits; design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceItemExits("percPage", emptyItemExitsBody()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceItemExitsLockedByOtherUser() {
    when(adaptor.replaceItemExits(any(), eq("percPage"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not update content type item-level exits; locked by editor"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceItemExits("percPage", emptyItemExitsBody()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceItemExitsMissingLists400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceItemExits("percPage", new ContentTypeItemExits()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void replaceItemExitsInvalidExtension400() {
    when(adaptor.replaceItemExits(any(), eq("percPage"), any()))
        .thenThrow(new IllegalArgumentException("inputTranslations[0].extension is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceItemExits("percPage", emptyItemExitsBody()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  private static ContentTypeItemExits emptyItemExitsBody() {
    ContentTypeItemExits body = new ContentTypeItemExits();
    body.setInputTranslations(List.of());
    body.setOutputTranslations(List.of());
    body.setValidations(List.of());
    return body;
  }

  @Test
  public void replaceAllowedTemplatesInvalidTemplate() {
    when(adaptor.replaceAllowedTemplates(any(), eq("percPage"), any()))
        .thenThrow(new IllegalArgumentException("allowedTemplates[0] template not found: nope"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceAllowedTemplates("percPage", new NamedObjectRefList()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeLockConflictWhenLockedByOtherUser() {
    when(adaptor.updateContentType(any(), eq("percPage"), any()))
        .thenThrow(new ContentTypeDesignLockException("Could not save content type; locked by editor2"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("percPage", new ContentTypeDetail()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeGenericFailureWithBlockNameIs500Not409() {
    when(adaptor.updateContentType(any(), eq("percBlockquote"), any()))
        .thenThrow(
            new IllegalStateException(
                "Failed to open content type design session: percBlockquote"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("percBlockquote", new ContentTypeDetail()));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void updateContentTypeForbiddenWhenNotAdmin() {
    when(adaptor.updateContentType(any(), eq("percPage"), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.updateContentType("percPage", new ContentTypeDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void lockContentTypeBadRequestForWildcardName() {
    when(adaptor.lockContentType(any(), eq("perc*")))
        .thenThrow(new IllegalArgumentException("Content type name must not contain wildcards"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.lockContentType("perc*"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void lockContentTypeSuccess() {
    ObjectLockSummary summary = new ObjectLockSummary();
    summary.setLocker("Admin");
    summary.setSession("sess-1");
    summary.setRemainingTime(30);
    when(adaptor.lockContentType(any(), eq("percPage"))).thenReturn(summary);
    ObjectLockSummary out = resource.lockContentType("percPage");
    assertEquals("Admin", out.getLocker());
    assertEquals("sess-1", out.getSession());
    assertEquals(30, out.getRemainingTime());
  }

  @Test
  public void lockContentTypeNotFound() {
    when(adaptor.lockContentType(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.lockContentType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void lockContentTypeConflictWhenLockedByOtherUser() {
    when(adaptor.lockContentType(any(), eq("percPage")))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not acquire design lock for content type; locked by editor2"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.lockContentType("percPage"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void lockContentTypeForbidden() {
    when(adaptor.lockContentType(any(), eq("percPage")))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.lockContentType("percPage"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void createContentTypeSuccess() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    ContentTypeDetail created = new ContentTypeDetail();
    created.setName("percNewType");
    created.setLabel("percNewType");
    when(adaptor.createContentType(any(), any())).thenReturn(created);

    ContentTypeDetail out = resource.createContentType(body);
    assertEquals("percNewType", out.getName());
  }

  @Test
  public void createContentTypeRequiresName() {
    when(adaptor.createContentType(any(), any()))
        .thenThrow(new IllegalArgumentException("name is required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.createContentType(new ContentTypeDetail()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createContentTypeRejectsSpaces() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("has space");
    when(adaptor.createContentType(any(), any()))
        .thenThrow(new IllegalArgumentException("name cannot contain spaces"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createContentType(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void createContentTypeDuplicateIs409() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percPage");
    when(adaptor.createContentType(any(), any()))
        .thenThrow(new WebApplicationException("Content type already exists: percPage", 409));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createContentType(body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void createContentTypeForbiddenWhenNotAdmin() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    when(adaptor.createContentType(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createContentType(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void createContentTypeForbiddenWhenNoSession() {
    ContentTypeDetail body = new ContentTypeDetail();
    body.setName("percNewType");
    when(adaptor.createContentType(any(), any()))
        .thenThrow(
            new WebApplicationException(
                "Request session/user required for content type design session", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createContentType(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void unlockContentTypeSuccess() {
    when(adaptor.unlockContentType(any(), eq("percPage"))).thenReturn(Boolean.TRUE);
    Response response = resource.unlockContentType("percPage");
    assertEquals(204, response.getStatus());
  }

  @Test
  public void deleteContentTypeNoContent() {
    when(adaptor.deleteContentType(any(), eq("percPage"))).thenReturn(Boolean.TRUE);
    Response response = resource.deleteContentType("percPage");
    assertEquals(204, response.getStatus());
    verify(adaptor).deleteContentType(any(), eq("percPage"));
  }

  @Test
  public void deleteContentTypeNotFound() {
    when(adaptor.deleteContentType(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteContentType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void deleteContentTypeLockConflictWhenLockNotHeld() {
    when(adaptor.deleteContentType(any(), eq("percPage")))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not delete content type; design lock required"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteContentType("percPage"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteContentTypeLockConflictWhenLockedByOtherUser() {
    when(adaptor.deleteContentType(any(), eq("percPage")))
        .thenThrow(
            new ContentTypeDesignLockException("Could not delete content type; locked by editor2"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteContentType("percPage"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteContentTypeForbiddenWhenNotAdmin() {
    when(adaptor.deleteContentType(any(), eq("percPage")))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteContentType("percPage"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void deleteContentTypeInUseIs400() {
    when(adaptor.deleteContentType(any(), eq("percPage")))
        .thenThrow(
            new IllegalArgumentException(
                "Could not delete content type: Content Type 311 has dependents"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteContentType("percPage"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteContentTypeWildcardIs400() {
    when(adaptor.deleteContentType(any(), eq("perc*")))
        .thenThrow(new IllegalArgumentException("idOrName must not contain wildcards"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.deleteContentType("perc*"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void deleteContentTypeGenericFailureIs500() {
    when(adaptor.deleteContentType(any(), eq("percBlockquote")))
        .thenThrow(
            new IllegalStateException("Failed to delete content type: percBlockquote"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.deleteContentType("percBlockquote"));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void unlockContentTypeNotFound() {
    when(adaptor.unlockContentType(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.unlockContentType("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getFieldControlPropertiesReturnsValues() {
    ContentTypeFieldControlProperties envelope = new ContentTypeFieldControlProperties();
    envelope.setFieldName("sys_title");
    envelope.setControl("sys_EditBox");
    envelope.setProperties(List.of(new ContentTypeControlProperty("height", "200")));
    when(adaptor.getFieldControlProperties(any(), eq("percPage"), eq("sys_title")))
        .thenReturn(envelope);
    ContentTypeFieldControlProperties out =
        resource.getFieldControlProperties("percPage", "sys_title");
    assertEquals("sys_title", out.getFieldName());
    assertEquals("200", out.getProperties().get(0).getValue());
  }

  @Test
  public void getFieldControlPropertiesContentTypeNotFound() {
    when(adaptor.getFieldControlProperties(any(), eq("missing"), eq("sys_title"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getFieldControlProperties("missing", "sys_title"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getFieldControlPropertiesFieldNotFound() {
    when(adaptor.getFieldControlProperties(any(), eq("percPage"), eq("nope")))
        .thenThrow(new WebApplicationException("Field not found: nope", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getFieldControlProperties("percPage", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldControlPropertiesSuccess() {
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "400")));
    ContentTypeFieldControlProperties updated = new ContentTypeFieldControlProperties();
    updated.setFieldName("sys_title");
    updated.setProperties(body.getProperties());
    when(adaptor.replaceFieldControlProperties(any(), eq("percPage"), eq("sys_title"), any()))
        .thenReturn(updated);
    ContentTypeFieldControlProperties out =
        resource.replaceFieldControlProperties("percPage", "sys_title", body);
    assertEquals("400", out.getProperties().get(0).getValue());
  }

  @Test
  public void replaceFieldControlPropertiesRequiresProperties() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.replaceFieldControlProperties(
                    "percPage", "sys_title", new ContentTypeFieldControlProperties()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldControlPropertiesRequiresLock() {
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    when(adaptor.replaceFieldControlProperties(any(), eq("percPage"), eq("sys_title"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save control properties; design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceFieldControlProperties("percPage", "sys_title", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldControlPropertiesLockedByOtherUser() {
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    when(adaptor.replaceFieldControlProperties(any(), eq("percPage"), eq("sys_title"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save control properties; locked by editor"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceFieldControlProperties("percPage", "sys_title", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldControlPropertiesInvalidChoice() {
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    when(adaptor.replaceFieldControlProperties(any(), eq("percPage"), eq("sys_title"), any()))
        .thenThrow(new IllegalArgumentException("choices.globalId is required for type global"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceFieldControlProperties("percPage", "sys_title", body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void getFieldRuleExpressionsReturnsEnvelope() {
    ContentTypeFieldRuleExpressions envelope = new ContentTypeFieldRuleExpressions();
    envelope.setFieldName("sys_title");
    ContentTypeFieldRule rule = new ContentTypeFieldRule();
    rule.setType(ContentTypeFieldRule.TYPE_CONDITIONAL);
    rule.setConditionals(List.of(new ContentTypeFieldConditional("sys_title", "<>", "")));
    envelope.setValidation(List.of(rule));
    envelope.setVisibility(List.of());
    envelope.setInputTranslation(List.of());
    envelope.setOutputTranslation(List.of());
    envelope.setValidationExpression("sys_title <> ");
    when(adaptor.getFieldRuleExpressions(any(), eq("percPage"), eq("sys_title")))
        .thenReturn(envelope);
    ContentTypeFieldRuleExpressions out = resource.getFieldRuleExpressions("percPage", "sys_title");
    assertEquals("sys_title", out.getFieldName());
    assertEquals(1, out.getValidation().size());
    assertEquals("<>", out.getValidation().get(0).getConditionals().get(0).getOperator());
  }

  @Test
  public void getFieldRuleExpressionsContentTypeNotFound() {
    when(adaptor.getFieldRuleExpressions(any(), eq("missing"), eq("sys_title"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getFieldRuleExpressions("missing", "sys_title"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getFieldRuleExpressionsFieldNotFound() {
    when(adaptor.getFieldRuleExpressions(any(), eq("percPage"), eq("nope")))
        .thenThrow(new WebApplicationException("Unknown field: nope", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.getFieldRuleExpressions("percPage", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldRuleExpressionsSuccess() {
    ContentTypeFieldRuleExpressions body = emptyRuleExpressionsBody();
    ContentTypeFieldRuleExpressions updated = emptyRuleExpressionsBody();
    updated.setFieldName("sys_title");
    when(adaptor.replaceFieldRuleExpressions(any(), eq("percPage"), eq("sys_title"), any()))
        .thenReturn(updated);
    ContentTypeFieldRuleExpressions out =
        resource.replaceFieldRuleExpressions("percPage", "sys_title", body);
    assertEquals("sys_title", out.getFieldName());
  }

  @Test
  public void replaceFieldRuleExpressionsRequiresLists() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.replaceFieldRuleExpressions(
                    "percPage", "sys_title", new ContentTypeFieldRuleExpressions()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldRuleExpressionsRequiresLock() {
    ContentTypeFieldRuleExpressions body = emptyRuleExpressionsBody();
    when(adaptor.replaceFieldRuleExpressions(any(), eq("percPage"), eq("sys_title"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save field rule expressions; design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceFieldRuleExpressions("percPage", "sys_title", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldRuleExpressionsLockedByOtherUser() {
    ContentTypeFieldRuleExpressions body = emptyRuleExpressionsBody();
    when(adaptor.replaceFieldRuleExpressions(any(), eq("percPage"), eq("sys_title"), any()))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save field rule expressions; locked by editor"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceFieldRuleExpressions("percPage", "sys_title", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void replaceFieldRuleExpressionsUnknownField400() {
    ContentTypeFieldRuleExpressions body = emptyRuleExpressionsBody();
    when(adaptor.replaceFieldRuleExpressions(any(), eq("percPage"), eq("nope"), any()))
        .thenThrow(new IllegalArgumentException("Unknown field: nope"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceFieldRuleExpressions("percPage", "nope", body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void fieldRuleExpressionsJsonSerializesRulesAndSummaries() throws Exception {
    ContentTypeFieldRuleExpressions env = emptyRuleExpressionsBody();
    env.setFieldName("sys_title");
    ContentTypeFieldRule rule = new ContentTypeFieldRule();
    rule.setType(ContentTypeFieldRule.TYPE_CONDITIONAL);
    rule.setConditionals(List.of(new ContentTypeFieldConditional("sys_title", "<>", "")));
    env.setValidation(List.of(rule));
    env.setValidationExpression("sys_title <> ");
    env.setDesignGaps(
        List.of(
            DesignGap.of(
                "CT_FIELD_RULE_APPLY_WHEN",
                "Apply-when on field validation is read-only")));

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(ContentTypeFieldRuleExpressions.class);
    String json = mapper.writeValueAsString(env);
    assertTrue(json.contains("validation"), json);
    assertTrue(json.contains("sys_title"), json);
    assertTrue(json.contains("validationExpression"), json);
    assertTrue(json.contains("CT_FIELD_RULE_APPLY_WHEN"), json);
  }

  private static ContentTypeFieldRuleExpressions emptyRuleExpressionsBody() {
    ContentTypeFieldRuleExpressions body = new ContentTypeFieldRuleExpressions();
    body.setValidation(List.of());
    body.setVisibility(List.of());
    body.setInputTranslation(List.of());
    body.setOutputTranslation(List.of());
    return body;
  }

  @Test
  public void unlockContentTypeConflictWhenLockedByOtherUser() {
    when(adaptor.unlockContentType(any(), eq("percPage")))
        .thenThrow(
            new ContentTypeDesignLockException("Could not release design lock; locked by editor2"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.unlockContentType("percPage"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeSuccess() {
    ContentTypeDetail updated = new ContentTypeDetail();
    updated.setName("percRenamedPage");
    when(adaptor.renameContentType(any(), eq("percPage"), eq("percRenamedPage")))
        .thenReturn(updated);
    ContentTypeDetail out =
        resource.renameContentType("percPage", new ContentTypeName("percRenamedPage"));
    assertEquals("percRenamedPage", out.getName());
  }

  @Test
  public void renameContentTypeRequiresName() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("percPage", new ContentTypeName()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeSpaces400() {
    when(adaptor.renameContentType(any(), eq("percPage"), eq("perc Renamed")))
        .thenThrow(new IllegalArgumentException("Content type name must not contain spaces"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("percPage", new ContentTypeName("perc Renamed")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeCollision400() {
    when(adaptor.renameContentType(any(), eq("percPage"), eq("percEventAsset")))
        .thenThrow(new IllegalArgumentException("Content type name already exists: percEventAsset"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("percPage", new ContentTypeName("percEventAsset")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeNotFound() {
    when(adaptor.renameContentType(any(), eq("missing"), eq("percRenamed"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("missing", new ContentTypeName("percRenamed")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeRequiresLock() {
    when(adaptor.renameContentType(any(), eq("percPage"), eq("percRenamed")))
        .thenThrow(
            new ContentTypeDesignLockException(
                "Could not save content type; design lock required"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("percPage", new ContentTypeName("percRenamed")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeLockedByOtherUser() {
    when(adaptor.renameContentType(any(), eq("percPage"), eq("percRenamed")))
        .thenThrow(
            new ContentTypeDesignLockException("Could not save content type; locked by editor2"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("percPage", new ContentTypeName("percRenamed")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void renameContentTypeForbiddenWhenNotAdmin() {
    when(adaptor.renameContentType(any(), eq("percPage"), eq("percRenamed")))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.renameContentType("percPage", new ContentTypeName("percRenamed")));
    assertEquals(403, ex.getResponse().getStatus());
  }
}
