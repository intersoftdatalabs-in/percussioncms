/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSDbComponent;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.rest.displayformat.DisplayFormat;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class DisplayFormatAdaptorSafeKeyTest {

  @Test
  void isSafeDisplayFormatKey_rejectsPathTraversal() {
    assertTrue(DisplayFormatAdaptor.isSafeDisplayFormatKey("Default"));
    assertTrue(DisplayFormatAdaptor.isSafeDisplayFormatKey("0-11-301"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("../x"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("a/b"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("a\\b"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey(""));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("   "));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("a\u0000b"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey(null));
  }

  /**
   * Detail mapping must always set REST {@code guid.stringValue} so Developer Object ACL can load
   * (issues #2689 / #2951).
   */
  @Test
  void findDisplayFormatByKey_mapsGuidStringValue() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    PSDisplayFormat nativeDf = new PSDisplayFormat();
    // Unpersisted formats have displayId -1; assign a real DISPLAYID so getGUID() is valid.
    PSKey key = PSDisplayFormat.createKey(new String[] {"301"});
    Method setKey = PSDbComponent.class.getDeclaredMethod("setKey", PSKey.class);
    setKey.setAccessible(true);
    setKey.invoke(nativeDf, key);
    assertEquals(301, nativeDf.getDisplayId());

    when(designWs.findDisplayFormat(eq("By_Author"))).thenReturn(nativeDf);

    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    DisplayFormat out = adaptor.findDisplayFormatByKey("By_Author");

    assertNotNull(out, "adaptor should return a REST DisplayFormat");
    assertNotNull(out.getGuid(), "guid must be mapped for Object ACL");
    assertNotNull(out.getGuid().getStringValue(), "guid.stringValue must be present for SPA binding");
    String sv = out.getGuid().getStringValue();
    assertFalse(sv.isBlank(), "guid.stringValue must not be blank");
    // PSGuid#toString form host-type-uuid; uuid part is display id 301
    assertTrue(sv.endsWith("-301"), "unexpected guid form: " + sv);
    assertEquals(301, out.getGuid().getUuid());
    assertEquals(sv, out.getGuidString(), "guidString must match guid.stringValue for SPA bind");
  }

  @Test
  void findAllDisplayFormats_keepsUniqueNamesWhenLoadReplaysByAuthor() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    IPSCatalogSummary byAuthor = catalogSummary("By_Author", "By Author", 5);
    IPSCatalogSummary byAuthorDup = catalogSummary("By_Author", "By Author", 5);
    IPSCatalogSummary def = catalogSummary("Default", "Default View", 2);
    when(designWs.findDisplayFormats(null, null))
        .thenReturn(List.of(byAuthor, byAuthorDup, def));

    PSDisplayFormat nativeByAuthor = nativeDisplayFormat(5, "By_Author");
    when(designWs.loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(nativeByAuthor, nativeByAuthor));
    when(designWs.findDisplayFormat(eq("By_Author"))).thenReturn(nativeByAuthor);
    // Production defect: name lookup loads By_Author for every key.
    when(designWs.findDisplayFormat(eq("Default"))).thenReturn(nativeByAuthor);

    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    List<DisplayFormat> out = adaptor.findAllDisplayFormats();
    assertEquals(2, out.size());
    assertEquals("By_Author", out.get(0).getName());
    assertEquals("Default", out.get(1).getName());
    assertEquals("Default View", out.get(1).getLabel());
  }

  @Test
  void findAllDisplayFormats_usesBulkLoadWhenNamesAreUnique() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    IPSCatalogSummary byAuthor = catalogSummary("By_Author", "By Author", 5);
    IPSCatalogSummary def = catalogSummary("Default", "Default View", 2);
    when(designWs.findDisplayFormats(null, null)).thenReturn(List.of(byAuthor, def));
    PSDisplayFormat nativeByAuthor = nativeDisplayFormat(5, "By_Author");
    PSDisplayFormat nativeDefault = nativeDisplayFormat(2, "Default");
    when(designWs.loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(nativeByAuthor, nativeDefault));

    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    List<DisplayFormat> out = adaptor.findAllDisplayFormats();
    assertEquals(2, out.size());
    assertEquals("By_Author", out.get(0).getName());
    assertEquals("Default", out.get(1).getName());
    verify(designWs, never()).findDisplayFormat(any(String.class));
  }

  @Test
  void findAllDisplayFormats_nullLoadedNameFallsBackToSummary() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    IPSCatalogSummary def = catalogSummary("Default", "Default View", 2);
    when(designWs.findDisplayFormats(null, null)).thenReturn(List.of(def));
    PSDisplayFormat nameless = mock(PSDisplayFormat.class);
    when(nameless.getName()).thenReturn(null);
    when(designWs.loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(nameless));
    when(designWs.findDisplayFormat(eq("Default"))).thenReturn(nameless);

    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    List<DisplayFormat> out = adaptor.findAllDisplayFormats();
    assertEquals(1, out.size());
    assertEquals("Default", out.get(0).getName());
    assertEquals("Default View", out.get(0).getLabel());
  }

  @Test
  void findAllDisplayFormats_guidLoadRestoresFullRowWhenNameReplays() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    IPSCatalogSummary def = catalogSummary("Default", "Default View", 2);
    when(designWs.findDisplayFormats(null, null)).thenReturn(List.of(def));
    PSDisplayFormat replayed = nativeDisplayFormat(5, "By_Author");
    PSDisplayFormat realDefault = nativeDisplayFormat(2, "Default");
    when(designWs.loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(replayed));
    when(designWs.findDisplayFormat(eq("Default"))).thenReturn(replayed);
    when(designWs.findDisplayFormat(eq(def.getGUID()))).thenReturn(realDefault);

    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    List<DisplayFormat> out = adaptor.findAllDisplayFormats();
    assertEquals(1, out.size());
    assertEquals("Default", out.get(0).getName());
    assertEquals(2, out.get(0).getDisplayId());
  }

  private static IPSCatalogSummary catalogSummary(String name, String label, int uuid) {
    IPSCatalogSummary s = mock(IPSCatalogSummary.class);
    when(s.getName()).thenReturn(name);
    when(s.getLabel()).thenReturn(label);
    when(s.getDescription()).thenReturn(label);
    when(s.getGUID()).thenReturn(new PSGuid(PSTypeEnum.DISPLAY_FORMAT, uuid));
    return s;
  }

  private static PSDisplayFormat nativeDisplayFormat(int displayId, String name) throws Exception {
    PSDisplayFormat nativeDf = new PSDisplayFormat();
    PSKey key = PSDisplayFormat.createKey(new String[] {String.valueOf(displayId)});
    Method setKey = PSDbComponent.class.getDeclaredMethod("setKey", PSKey.class);
    setKey.setAccessible(true);
    setKey.invoke(nativeDf, key);
    nativeDf.setName(name);
    nativeDf.setInternalName(name);
    return nativeDf;
  }

  @Test
  void findDisplayFormatByKey_returnsNullWhenDesignWsMisses() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    when(designWs.findDisplayFormat(eq("Missing"))).thenReturn(null);
    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    assertNull(adaptor.findDisplayFormatByKey("Missing"));
  }
}
