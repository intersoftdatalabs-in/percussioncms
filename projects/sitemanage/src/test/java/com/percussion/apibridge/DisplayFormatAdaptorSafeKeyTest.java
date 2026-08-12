/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSDbComponent;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.rest.displayformat.DisplayFormat;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.lang.reflect.Method;
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
    assertTrue(
        out.getGuid().getStringValue().isPresent(),
        "guid.stringValue must be present for SPA binding");
    String sv = out.getGuid().getStringValue().get();
    assertFalse(sv.isBlank(), "guid.stringValue must not be blank");
    // PSGuid#toString form host-type-uuid; uuid part is display id 301
    assertTrue(sv.endsWith("-301"), "unexpected guid form: " + sv);
    assertEquals(301, out.getGuid().getUuid());
    assertEquals(sv, out.getGuidString(), "guidString must match guid.stringValue for SPA bind");
  }

  @Test
  void findDisplayFormatByKey_returnsNullWhenDesignWsMisses() throws Exception {
    IPSUiDesignWs designWs = mock(IPSUiDesignWs.class);
    when(designWs.findDisplayFormat(eq("Missing"))).thenReturn(null);
    DisplayFormatAdaptor adaptor = new DisplayFormatAdaptor(designWs);
    assertNull(adaptor.findDisplayFormatByKey("Missing"));
  }
}
