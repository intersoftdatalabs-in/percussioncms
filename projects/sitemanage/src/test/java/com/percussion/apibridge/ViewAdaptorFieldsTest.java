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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.rest.views.ViewDef;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.rest.views.ViewFieldSummary;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** UI-08 PUT field criteria persist on standard views; unknown field is 400. */
@Tag("UnitTest")
class ViewAdaptorFieldsTest {

  private IPSUiDesignWs designWs;
  private ViewAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    adaptor =
        new ViewAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> true);
    guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("0-18-42");
    when(guid.toStringUntyped()).thenReturn("42");
    when(guid.getHostId()).thenReturn(0L);
    when(guid.longValue()).thenReturn(42L);
    when(guid.getType()).thenReturn((short) 18);
    when(guid.getUUID()).thenReturn(42);
    when(designWs.findViews(any(), isNull())).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void applyFields_replacesAndOrdersCriteria() throws Exception {
    PSSearch view = new PSSearch("MyView");
    ViewFieldSummary second = criterion("sys_title", "like", "News%", 1);
    ViewFieldSummary first = criterion("sys_contentid", "equal", "12", 0);
    ViewAdaptor.applyFields(view, List.of(first, second));

    List<PSSearchField> persisted = listFields(view);
    assertEquals(2, persisted.size());
    assertEquals("sys_contentid", persisted.get(0).getFieldName());
    assertEquals(PSSearchField.OP_EQUALS, persisted.get(0).getOperator());
    assertEquals("12", persisted.get(0).getFieldValue());
    assertEquals("sys_title", persisted.get(1).getFieldName());
    assertEquals(PSSearchField.OP_LIKE, persisted.get(1).getOperator());
    assertEquals("News%", persisted.get(1).getFieldValue());
  }

  @Test
  void applyFields_emptyListClearsCriteria() throws Exception {
    PSSearch view = new PSSearch("MyView");
    ViewAdaptor.applyFields(view, List.of(criterion("sys_title", "equal", "x", 0)));
    assertEquals(1, listFields(view).size());
    ViewAdaptor.applyFields(view, List.of());
    assertTrue(listFields(view).isEmpty());
  }

  @Test
  void applyWritableFields_nullFieldsLeavesExisting() throws Exception {
    PSSearch view = new PSSearch("MyView");
    ViewAdaptor.applyFields(view, List.of(criterion("sys_title", "equal", "keep", 0)));
    ViewDef body = new ViewDef();
    body.setLabel("Updated");
    body.setFields(null);
    ViewAdaptor.applyWritableFields(view, body);
    assertEquals("Updated", view.getLabel());
    assertEquals(1, listFields(view).size());
    assertEquals("keep", listFields(view).get(0).getFieldValue());
  }

  @Test
  void requireValidFieldName_unknownIs400Shape() {
    IllegalArgumentException blank =
        assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.requireValidFieldName("  "));
    assertTrue(blank.getMessage().toLowerCase().contains("unknown field"));
    IllegalArgumentException unknown =
        assertThrows(
            IllegalArgumentException.class, () -> ViewAdaptor.requireValidFieldName("not_a_cx_field"));
    assertTrue(unknown.getMessage().contains("unknown field: not_a_cx_field"));
    assertEquals("sys_contentid", ViewAdaptor.requireValidFieldName("sys_contentid"));
  }

  @Test
  void applyFields_duplicateNameRejected() throws Exception {
    PSSearch view = new PSSearch("MyView");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ViewAdaptor.applyFields(
                    view,
                    List.of(
                        criterion("sys_title", "equal", "a", 0),
                        criterion("sys_title", "like", "b", 1))));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
  }

  @Test
  void applyFields_invalidOperatorRejected() throws Exception {
    PSSearch view = new PSSearch("MyView");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ViewAdaptor.applyFields(view, List.of(criterion("sys_title", "bogusOp", "x", 0))));
    assertTrue(ex.getMessage().toLowerCase().contains("operator"));
  }

  @Test
  void update_usesBodyNameWhenPathGuidMissesCatalog() throws Exception {
    when(designWs.findAllViews()).thenReturn(List.of());
    when(designWs.findViews(eq("0-18-42"), isNull())).thenReturn(List.of());
    PSSearch found = mockCatalogView("MyView");
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn("MyView");
    when(sum.getGUID()).thenReturn(guid);
    when(designWs.findViews(eq("MyView"), isNull())).thenReturn(List.of(sum));
    when(designWs.loadViews(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of())
        .thenReturn(List.of(found));
    PSSearch locked = new PSSearch("MyView");
    locked.setDisplayName("My View");
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ViewDef body = new ViewDef();
    body.setName("MyView");
    body.setFields(List.of(criterion("sys_contentid", "equal", "9", 0)));
    ViewDef out = adaptor.saveView("0-18-42", body);

    assertEquals(1, out.getFields().size());
    assertEquals("sys_contentid", out.getFields().get(0).getFieldName());
    verify(designWs).saveViews(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_byGuid_whenCatalogMissesName() throws Exception {
    when(designWs.findAllViews()).thenReturn(List.of());
    PSSearch found = mockCatalogView("MyView");
    PSSearch locked = new PSSearch("MyView");
    locked.setDisplayName("My View");
    when(designWs.loadViews(anyList(), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(found));
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ViewDef body = new ViewDef();
    body.setFields(List.of(criterion("sys_contentid", "equal", "9", 0)));
    ViewDef out = adaptor.saveView("0-18-42", body);

    assertEquals(1, out.getFields().size());
    assertEquals("sys_contentid", out.getFields().get(0).getFieldName());
    verify(designWs).saveViews(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_returnsAppliedFieldsWhenCatalogXmlLags() throws Exception {
    PSSearch catalog = mockCatalogView("MyView");
    PSSearch locked = new PSSearch("MyView");
    locked.setDisplayName("My View");
    when(designWs.findAllViews()).thenReturn(List.of(catalog));
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ViewDef body = new ViewDef();
    body.setLabel("My View");
    body.setFields(List.of(criterion("sys_title", "like", "News%", 0)));

    ViewDef out = adaptor.saveView("MyView", body);

    assertEquals(1, out.getFields().size());
    assertEquals("sys_title", out.getFields().get(0).getFieldName());
    assertEquals("News%", out.getFields().get(0).getFieldValue());
    verify(designWs).saveViews(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_appliesFieldsOnLockedView() throws Exception {
    PSSearch catalog = mockCatalogView("MyView");
    PSSearch locked = new PSSearch("MyView");
    locked.setDisplayName("My View");
    when(designWs.findAllViews()).thenReturn(List.of(catalog), List.of(locked));
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ViewDef body = new ViewDef();
    body.setLabel("My View");
    body.setFields(List.of(criterion("sys_contentid", "=", "7", 0)));

    ViewDef out = adaptor.saveView("MyView", body);

    assertEquals("MyView", out.getName());
    assertEquals(1, out.getFields().size());
    assertEquals("sys_contentid", out.getFields().get(0).getFieldName());
    assertEquals("7", out.getFields().get(0).getFieldValue());
    verify(designWs).saveViews(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_unknownField_is400BeforeSave() throws Exception {
    PSSearch catalog = mockCatalogView("MyView");
    when(designWs.findAllViews()).thenReturn(List.of(catalog));
    PSSearch locked = new PSSearch("MyView");
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ViewDef body = new ViewDef();
    body.setFields(List.of(criterion("not_a_cx_field", "equal", "x", 0)));
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveView("MyView", body));
    assertTrue(ex.getMessage().contains("unknown field"));
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_inbox_is409AndDoesNotApplyFields() throws Exception {
    PSSearch inbox = mockCatalogView("Inbox");
    when(inbox.isCustomView()).thenReturn(true);
    when(inbox.getUrl()).thenReturn("../sys_cxViews/inbox.xml");
    when(designWs.findAllViews()).thenReturn(List.of(inbox));

    ViewDef body = new ViewDef();
    body.setFields(List.of(criterion("sys_title", "equal", "x", 0)));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.saveView("Inbox", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadViews(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void designGaps_omitFieldCriterionClaim() {
    assertFalse(
        ViewAdaptor.DESIGN_GAPS.stream()
            .anyMatch(g -> g.toLowerCase().contains("field criterion")));
  }

  private static ViewFieldSummary criterion(String name, String op, String value, int position) {
    ViewFieldSummary row = new ViewFieldSummary();
    row.setFieldName(name);
    row.setOperator(op);
    row.setFieldValue(value);
    row.setPosition(position);
    return row;
  }

  private static List<PSSearchField> listFields(PSSearch view) {
    List<PSSearchField> out = new ArrayList<>();
    Iterator<PSSearchField> it = view.getFields();
    while (it.hasNext()) {
      out.add(it.next());
    }
    return out;
  }

  private PSSearch mockCatalogView(String name) {
    PSSearch s = mock(PSSearch.class);
    when(s.getName()).thenReturn(name);
    when(s.getLabel()).thenReturn(name);
    when(s.getType()).thenReturn(PSSearch.TYPE_VIEW);
    when(s.getDisplayFormatId()).thenReturn("1");
    when(s.isView()).thenReturn(true);
    when(s.isStandardView()).thenReturn(true);
    when(s.isCustomView()).thenReturn(false);
    when(s.getGUID()).thenReturn(guid);
    when(s.getId()).thenReturn(42);
    return s;
  }
}
