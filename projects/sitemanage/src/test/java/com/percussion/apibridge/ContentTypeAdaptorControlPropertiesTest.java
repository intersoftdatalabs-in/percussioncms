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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSChoiceFilter;
import com.percussion.design.objectstore.PSChoiceTableInfo;
import com.percussion.design.objectstore.PSChoices;
import com.percussion.design.objectstore.PSDefaultSelected;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSNullEntry;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.design.objectstore.PSUrlRequest;
import com.percussion.rest.contenttypes.ContentTypeChoiceCatalog;
import com.percussion.rest.contenttypes.ContentTypeChoiceDefaultSelected;
import com.percussion.rest.contenttypes.ContentTypeChoiceEntry;
import com.percussion.rest.contenttypes.ContentTypeChoiceFilter;
import com.percussion.rest.contenttypes.ContentTypeChoiceFilterField;
import com.percussion.rest.contenttypes.ContentTypeChoiceNullEntry;
import com.percussion.rest.contenttypes.ContentTypeChoiceTable;
import com.percussion.rest.contenttypes.ContentTypeControlProperty;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeFieldControlProperties;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.PSCollection;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CD-07 control property values and choice catalogs: GET returns name+value plus choice filter,
 * null-entry, and default-selected; PUT requires a held design-session lock and persists via
 * {@code saveContentTypes}.
 */
@Tag("UnitTest")
class ContentTypeAdaptorControlPropertiesTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;
  private PSUISet uiSet;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void controlProperties_emptyAndPresent() {
    assertTrue(ContentTypeAdaptor.controlProperties(null).isEmpty());

    PSControlRef bare = new PSControlRef("sys_EditBox");
    assertTrue(ContentTypeAdaptor.controlProperties(bare).isEmpty());

    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("height", new PSTextLiteral("200")));
    params.add(new PSParam("width", new PSTextLiteral("400")));
    PSControlRef withParams = new PSControlRef("sys_TextArea");
    withParams.setParameters(params);
    List<ContentTypeControlProperty> props = ContentTypeAdaptor.controlProperties(withParams);
    assertEquals(2, props.size());
    assertEquals("height", props.get(0).getName());
    assertEquals("200", props.get(0).getValue());
    assertEquals("width", props.get(1).getName());
    assertEquals("400", props.get(1).getValue());
    assertEquals(List.of("height", "width"), ContentTypeAdaptor.controlPropertyNames(withParams));
  }

  @Test
  void toChoiceCatalog_localAndGlobal() {
    PSCollection local = new PSCollection(PSEntry.class);
    local.add(new PSEntry("open", new PSDisplayText("Open")));
    ContentTypeChoiceCatalog localCat = ContentTypeAdaptor.toChoiceCatalog(new PSChoices(local));
    assertEquals("local", localCat.getType());
    assertEquals(1, localCat.getEntries().size());
    assertEquals("open", localCat.getEntries().get(0).getValue());
    assertEquals("Open", localCat.getEntries().get(0).getLabel());

    ContentTypeChoiceCatalog globalCat = ContentTypeAdaptor.toChoiceCatalog(new PSChoices(42));
    assertEquals("global", globalCat.getType());
    assertEquals(42, globalCat.getGlobalId());
  }

  @Test
  void controlPropertyDesignGaps_emptyWhenChoiceExtrasWritable() {
    assertTrue(ContentTypeAdaptor.controlPropertyDesignGaps().isEmpty());
  }

  @Test
  void fromChoiceCatalog_roundTripLocalAndNone() {
    ContentTypeChoiceCatalog body = new ContentTypeChoiceCatalog();
    body.setType("local");
    body.setEntries(List.of(new ContentTypeChoiceEntry("closed", "Closed")));
    PSChoices choices = ContentTypeAdaptor.fromChoiceCatalog(body);
    assertEquals(PSChoices.TYPE_LOCAL, choices.getType());
    Iterator<?> it = choices.getLocal();
    PSEntry first = (PSEntry) it.next();
    assertEquals("closed", first.getValue());

    ContentTypeChoiceCatalog none = new ContentTypeChoiceCatalog();
    none.setType("none");
    assertNull(ContentTypeAdaptor.fromChoiceCatalog(none));
  }

  @Test
  void fromChoiceCatalog_tableAndLookup() {
    ContentTypeChoiceCatalog table = new ContentTypeChoiceCatalog();
    table.setType("tableinfo");
    ContentTypeChoiceTable t = new ContentTypeChoiceTable();
    t.setDataSource("rxdefault");
    t.setTableName("RXLOOKUP");
    t.setLabelColumn("LABEL");
    t.setValueColumn("VALUE");
    table.setTable(t);
    PSChoices tableChoices = ContentTypeAdaptor.fromChoiceCatalog(table);
    assertEquals(PSChoices.TYPE_TABLE_INFO, tableChoices.getType());
    PSChoiceTableInfo info = tableChoices.getTableInfo();
    assertEquals("RXLOOKUP", info.getTableName());
    assertEquals("LABEL", info.getLableColumn());

    ContentTypeChoiceCatalog lookup = new ContentTypeChoiceCatalog();
    lookup.setType("lookup");
    lookup.setLookupHref("../sys_lookup/foo.xml");
    PSChoices lookupChoices = ContentTypeAdaptor.fromChoiceCatalog(lookup);
    assertEquals(PSChoices.TYPE_LOOKUP, lookupChoices.getType());
    assertEquals("../sys_lookup/foo.xml", lookupChoices.getLookup().getHref());
  }

  @Test
  void fromChoiceCatalog_rejectsInvalid() {
    ContentTypeChoiceCatalog global = new ContentTypeChoiceCatalog();
    global.setType("global");
    IllegalArgumentException missingId =
        assertThrows(
            IllegalArgumentException.class, () -> ContentTypeAdaptor.fromChoiceCatalog(global));
    assertTrue(missingId.getMessage().contains("globalId"), missingId.getMessage());

    ContentTypeChoiceCatalog bogus = new ContentTypeChoiceCatalog();
    bogus.setType("keyword");
    assertThrows(
        IllegalArgumentException.class, () -> ContentTypeAdaptor.fromChoiceCatalog(bogus));
  }

  @Test
  void fromChoiceCatalog_roundTripFilterNullEntryDefaultSelected() {
    ContentTypeChoiceCatalog body = sampleCatalogWithExtras("closed", "Closed");
    PSChoices choices = ContentTypeAdaptor.fromChoiceCatalog(body);
    ContentTypeChoiceCatalog round = ContentTypeAdaptor.toChoiceCatalog(choices);
    assertEquals("local", round.getType());
    assertEquals("", round.getNullEntry().getValue());
    assertEquals("None", round.getNullEntry().getLabel());
    assertEquals("always", round.getNullEntry().getIncludeWhen());
    assertEquals("first", round.getNullEntry().getSortOrder());
    assertEquals(2, round.getDefaultSelected().size());
    assertEquals("nullEntry", round.getDefaultSelected().get(0).getType());
    assertEquals("text", round.getDefaultSelected().get(1).getType());
    assertEquals("closed", round.getDefaultSelected().get(1).getText());
    assertEquals("sys_communityid", round.getFilter().getDependentFields().get(0).getFieldRef());
    assertEquals("optional", round.getFilter().getDependentFields().get(0).getDependencyType());
    assertEquals("../sys_lookup/filter.xml", round.getFilter().getLookupHref());
    assertEquals("choiceFilter", round.getFilter().getLookupName());
  }

  @Test
  void fromChoiceCatalog_rejectsInvalidFilterAndDefaultSelected() {
    ContentTypeChoiceCatalog missingHref = sampleCatalogWithExtras("open", "Open");
    missingHref.getFilter().setLookupHref(null);
    IllegalArgumentException href =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.fromChoiceCatalog(missingHref));
    assertTrue(href.getMessage().contains("lookupHref"), href.getMessage());

    ContentTypeChoiceCatalog missingDeps = sampleCatalogWithExtras("open", "Open");
    missingDeps.getFilter().setDependentFields(List.of());
    IllegalArgumentException deps =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.fromChoiceCatalog(missingDeps));
    assertTrue(deps.getMessage().contains("dependentFields"), deps.getMessage());

    ContentTypeChoiceCatalog badDepType = sampleCatalogWithExtras("open", "Open");
    badDepType.getFilter().getDependentFields().get(0).setDependencyType("maybe");
    IllegalArgumentException depType =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.fromChoiceCatalog(badDepType));
    assertTrue(depType.getMessage().contains("dependencyType"), depType.getMessage());

    ContentTypeChoiceCatalog badDefault = sampleCatalogWithExtras("open", "Open");
    ContentTypeChoiceDefaultSelected seq = new ContentTypeChoiceDefaultSelected("sequence");
    badDefault.setDefaultSelected(List.of(seq));
    IllegalArgumentException sequence =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.fromChoiceCatalog(badDefault));
    assertTrue(sequence.getMessage().contains("sequence"), sequence.getMessage());

    ContentTypeChoiceCatalog badInclude = sampleCatalogWithExtras("open", "Open");
    badInclude.getNullEntry().setIncludeWhen("sometimes");
    IllegalArgumentException includeWhen =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.fromChoiceCatalog(badInclude));
    assertTrue(includeWhen.getMessage().contains("includeWhen"), includeWhen.getMessage());
  }

  @Test
  void get_returnsPropertyValuesAndChoices() throws Exception {
    stubDefinition();
    ContentTypeFieldControlProperties out =
        adaptor.getFieldControlProperties(null, "311", "sys_title");
    assertEquals("sys_title", out.getFieldName());
    assertEquals("sys_TextArea", out.getControl());
    assertEquals(1, out.getProperties().size());
    assertEquals("height", out.getProperties().get(0).getName());
    assertEquals("200", out.getProperties().get(0).getValue());
    assertEquals("local", out.getChoices().getType());
    assertEquals("open", out.getChoices().getEntries().get(0).getValue());
    assertEquals("None", out.getChoices().getNullEntry().getLabel());
    assertEquals("always", out.getChoices().getNullEntry().getIncludeWhen());
    assertEquals("nullEntry", out.getChoices().getDefaultSelected().get(0).getType());
    assertEquals(
        "sys_communityid", out.getChoices().getFilter().getDependentFields().get(0).getFieldRef());
    assertTrue(out.getDesignGaps().isEmpty());
  }

  @Test
  void get_unknownField_is404() throws Exception {
    stubDefinition();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.getFieldControlProperties(null, "311", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void get_unknownType_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.getFieldControlProperties(null, "missing", "sys_title"));
  }

  @Test
  void get_cacheMiss_doesNotUseObjectStoreFallback() throws Exception {
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("311"));
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(mock(PSItemDefinition.class)).when(spy).loadItemDefFromObjectStore("311");
    assertNull(spy.getFieldControlProperties(null, "311", "sys_title"));
    verify(spy, never()).loadItemDefFromObjectStore("311");
  }

  @Test
  void put_persistsValuesWhenLockHeld() throws Exception {
    stubHeldLock();
    stubDefinition();

    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));
    ContentTypeChoiceCatalog choices = new ContentTypeChoiceCatalog();
    choices.setType("local");
    choices.setEntries(List.of(new ContentTypeChoiceEntry("closed", "Closed")));
    body.setChoices(choices);

    ContentTypeFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "311", "sys_title", body);

    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("640", out.getProperties().get(0).getValue());
    assertEquals("closed", out.getChoices().getEntries().get(0).getValue());
    PSControlRef control = uiSet.getControl();
    PSParam first = (PSParam) control.getParameters().next();
    assertEquals("width", first.getName());
    assertEquals("640", first.getValue().getValueText());
  }

  @Test
  void put_cacheMissAfterSave_fallsBackToLockedDef() throws Exception {
    stubHeldLock();
    stubDefinition();
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("not cached"));

    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));

    ContentTypeFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "311", "sys_title", body);

    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("640", out.getProperties().get(0).getValue());
    PSControlRef control = uiSet.getControl();
    PSParam first = (PSParam) control.getParameters().next();
    assertEquals("width", first.getName());
    assertEquals("640", first.getValue().getValueText());
  }

  @Test
  void put_emptyPropertiesClears() throws Exception {
    stubHeldLock();
    stubDefinition();
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    ContentTypeFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "311", "sys_title", body);
    assertTrue(out.getProperties().isEmpty());
    assertTrue(!uiSet.getControl().getParameters().hasNext(), "expected empty control parameters");
  }

  @Test
  void put_omittedChoicesLeaveCatalog() throws Exception {
    stubHeldLock();
    stubDefinition();
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("height", "12")));
    ContentTypeFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "311", "sys_title", body);
    assertEquals("local", out.getChoices().getType());
    assertEquals("open", out.getChoices().getEntries().get(0).getValue());
    assertEquals("None", out.getChoices().getNullEntry().getLabel());
    assertEquals("sys_communityid", out.getChoices().getFilter().getDependentFields().get(0).getFieldRef());
  }

  @Test
  void put_persistsChoiceFilterNullEntryDefaultSelected() throws Exception {
    stubHeldLock();
    stubDefinition();

    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of(new ContentTypeControlProperty("width", "640")));
    body.setChoices(sampleCatalogWithExtras("closed", "Closed"));

    ContentTypeFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "311", "sys_title", body);

    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("closed", out.getChoices().getEntries().get(0).getValue());
    assertEquals("None", out.getChoices().getNullEntry().getLabel());
    assertEquals("always", out.getChoices().getNullEntry().getIncludeWhen());
    assertEquals("text", out.getChoices().getDefaultSelected().get(1).getType());
    assertEquals("closed", out.getChoices().getDefaultSelected().get(1).getText());
    assertEquals("../sys_lookup/filter.xml", out.getChoices().getFilter().getLookupHref());
    PSChoices persisted = uiSet.getChoices();
    assertEquals("", persisted.getNullEntry().getValue());
    assertEquals("None", persisted.getNullEntry().getLabel().getText());
    assertEquals(PSNullEntry.INCLUDE_WHEN_ALWAYS, persisted.getNullEntry().getIncludeWhen());
    PSDefaultSelected firstDefault = (PSDefaultSelected) persisted.getDefaultSelected().next();
    assertEquals(PSDefaultSelected.TYPE_NULL_ENTRY, firstDefault.getType());
    PSChoiceFilter.DependentField dep =
        (PSChoiceFilter.DependentField) persisted.getChoiceFilter().getDependentFields().get(0);
    assertEquals("sys_communityid", dep.getFieldRef());
  }

  @Test
  void put_typeNoneClearsCatalogIncludingExtras() throws Exception {
    stubHeldLock();
    stubDefinition();
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    ContentTypeChoiceCatalog none = new ContentTypeChoiceCatalog();
    none.setType("none");
    body.setChoices(none);

    ContentTypeFieldControlProperties out =
        adaptor.replaceFieldControlProperties(null, "311", "sys_title", body);

    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertNull(out.getChoices());
    assertNull(uiSet.getChoices());
  }

  @Test
  void put_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of());
    stubDefinition();
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceFieldControlProperties(null, "311", "sys_title", body));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubDefinition();
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceFieldControlProperties(null, "311", "sys_title", body));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_missingProperties_isBadRequest() {
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldControlProperties(null, "311", "sys_title", body));
    assertTrue(ex.getMessage().contains("properties"), ex.getMessage());
  }

  @Test
  void put_unknownType_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    assertNull(adaptor.replaceFieldControlProperties(null, "missing", "sys_title", body));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    ContentTypeFieldControlProperties body = new ContentTypeFieldControlProperties();
    body.setProperties(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.replaceFieldControlProperties(null, "311", "sys_title", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private PSItemDefinition stubDefinition() throws Exception {
    uiSet = new PSUISet();
    PSControlRef control = new PSControlRef("sys_TextArea");
    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("height", new PSTextLiteral("200")));
    control.setParameters(params);
    uiSet.setControl(control);
    PSCollection local = new PSCollection(PSEntry.class);
    local.add(new PSEntry("open", new PSDisplayText("Open")));
    uiSet.setChoices(new PSChoices(local));
    applySampleChoiceExtras(uiSet.getChoices());

    PSDisplayMapper dmapper = new PSDisplayMapper("fs");
    dmapper.add(new PSDisplayMapping("sys_title", uiSet));
    PSUIDefinition uiDef = new PSUIDefinition(dmapper);
    PSContentEditorMapper ceMapper = mock(PSContentEditorMapper.class);
    when(ceMapper.getUIDefinition()).thenReturn(uiDef);
    PSContentEditorPipe pipe = mock(PSContentEditorPipe.class);
    when(pipe.getMapper()).thenReturn(ceMapper);
    PSContentEditor editor = mock(PSContentEditor.class);
    when(editor.getPipe()).thenReturn(pipe);

    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getTypeId()).thenReturn(311);
    when(def.getContentEditor()).thenReturn(editor);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(def));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def;
  }

  private static ContentTypeChoiceCatalog sampleCatalogWithExtras(String value, String label) {
    ContentTypeChoiceCatalog catalog = new ContentTypeChoiceCatalog();
    catalog.setType("local");
    catalog.setEntries(List.of(new ContentTypeChoiceEntry(value, label)));
    ContentTypeChoiceNullEntry nullEntry = new ContentTypeChoiceNullEntry();
    nullEntry.setValue("");
    nullEntry.setLabel("None");
    nullEntry.setIncludeWhen("always");
    nullEntry.setSortOrder("first");
    catalog.setNullEntry(nullEntry);
    ContentTypeChoiceDefaultSelected nullDefault = new ContentTypeChoiceDefaultSelected("nullEntry");
    ContentTypeChoiceDefaultSelected textDefault = new ContentTypeChoiceDefaultSelected("text");
    textDefault.setText(value);
    catalog.setDefaultSelected(List.of(nullDefault, textDefault));
    ContentTypeChoiceFilter filter = new ContentTypeChoiceFilter();
    filter.setDependentFields(List.of(new ContentTypeChoiceFilterField("sys_communityid", "optional")));
    filter.setLookupHref("../sys_lookup/filter.xml");
    filter.setLookupName("choiceFilter");
    catalog.setFilter(filter);
    return catalog;
  }

  private static void applySampleChoiceExtras(PSChoices choices) {
    PSNullEntry nullEntry = new PSNullEntry("", new PSDisplayText("None"));
    nullEntry.setIncludeWhen(PSNullEntry.INCLUDE_WHEN_ALWAYS);
    choices.setNullEntry(nullEntry);
    PSCollection<PSDefaultSelected> defaults = new PSCollection<>(PSDefaultSelected.class);
    defaults.add(new PSDefaultSelected());
    choices.setDefaultSelected(defaults);
    PSCollection<PSChoiceFilter.DependentField> deps =
        new PSCollection<>(PSChoiceFilter.DependentField.class);
    deps.add(new PSChoiceFilter.DependentField("sys_communityid", "optional"));
    choices.setChoiceFilter(
        new PSChoiceFilter(
            deps,
            new PSUrlRequest(
                "choiceFilter",
                "../sys_lookup/filter.xml",
                new PSCollection<>(PSParam.class))));
  }
}
