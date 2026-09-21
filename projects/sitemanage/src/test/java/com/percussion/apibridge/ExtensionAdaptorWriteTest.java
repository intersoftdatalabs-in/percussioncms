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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.error.PSNonUniqueException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionHandler;
import com.percussion.extension.PSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.extensions.IPSExtensionService;
import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionMethod;
import com.percussion.rest.extensions.ExtensionParameter;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SY-01 Admin POST register / PUT update / DELETE user extensions via {@code
 * IPSExtensionService}. Admin only; system/handler-owned are 409.
 */
@Tag("UnitTest")
class ExtensionAdaptorWriteTest {

  private static final URI BASE = URI.create("http://localhost/");

  private IPSExtensionService extensionService;
  private ExtensionAdaptor adaptor;
  private final Map<String, IPSExtensionDef> installed = new HashMap<>();

  @BeforeEach
  void setUp() throws Exception {
    extensionService = mock(IPSExtensionService.class);
    adaptor = new ExtensionAdaptor(extensionService, () -> true);

    when(extensionService.getExtensionNames(any(), any(), any(), any()))
        .thenAnswer(inv -> installed.values().stream().map(IPSExtensionDef::getRef).iterator());
    when(extensionService.exists(any()))
        .thenAnswer(
            inv -> {
              PSExtensionRef ref = inv.getArgument(0);
              return ref != null && installed.containsKey(ref.getFQN());
            });
    when(extensionService.getExtensionDef(any()))
        .thenAnswer(
            inv -> {
              PSExtensionRef ref = inv.getArgument(0);
              if (ref == null) {
                throw new PSNotFoundException("null");
              }
              IPSExtensionDef def = installed.get(ref.getFQN());
              if (def == null) {
                throw new PSNotFoundException(ref.getFQN());
              }
              return def;
            });
    doAnswer(
            inv -> {
              IPSExtensionDef def = inv.getArgument(0);
              if (def != null) {
                installed.put(def.getRef().getFQN(), def);
              }
              return null;
            })
        .when(extensionService)
        .installExtension(any(), any());
    doAnswer(
            inv -> {
              IPSExtensionDef def = inv.getArgument(0);
              if (def != null) {
                installed.put(def.getRef().getFQN(), def);
              }
              return null;
            })
        .when(extensionService)
        .updateExtension(any(), any());
    doAnswer(
            inv -> {
              PSExtensionRef ref = inv.getArgument(0);
              if (ref != null) {
                installed.remove(ref.getFQN());
              }
              return null;
            })
        .when(extensionService)
        .removeExtension(any());
  }

  @Test
  void register_installsUnderUserContext() throws Exception {
    Extension body = userBody("my_user_ext");

    Extension out = adaptor.registerExtension(BASE, body);

    assertEquals("my_user_ext", out.getExtensionName());
    assertEquals("user/", out.getContext());
    assertEquals("Java", out.getHandlerName());
    assertTrue(out.getFqn().contains("/user/"));
    assertTrue(installed.containsKey(out.getFqn()));

    ArgumentCaptor<IPSExtensionDef> cap = ArgumentCaptor.forClass(IPSExtensionDef.class);
    verify(extensionService).installExtension(cap.capture(), any());
    assertEquals(ExtensionAdaptor.USER_CONTEXT, cap.getValue().getRef().getContext());
  }

  @Test
  void register_thenFindByKeyRoundTrips() {
    Extension body = userBody("my_user_ext");
    Extension created = adaptor.registerExtension(BASE, body);
    Extension fetched = adaptor.findExtensionByKey(BASE, created.getFqn());
    assertNotNull(fetched);
    assertEquals("my_user_ext", fetched.getExtensionName());
    assertEquals(
        "com.example.MyExt",
        fetched.getInitParameters().get(IPSExtensionDef.INIT_PARAM_CLASSNAME));
  }

  @Test
  void register_duplicateIs409() throws Exception {
    adaptor.registerExtension(BASE, userBody("my_user_ext"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.registerExtension(BASE, userBody("my_user_ext")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void register_systemContextIs409() throws Exception {
    Extension body = userBody("my_user_ext");
    body.setContext("global/percussion/exit");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.registerExtension(BASE, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(extensionService, never()).installExtension(any(), any());
  }

  @Test
  void register_blankNameThrows400() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.registerExtension(BASE, null));
    Extension blank = new Extension();
    blank.setExtensionName("  ");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.registerExtension(BASE, blank));
    assertTrue(ex.getMessage().contains("extensionName is required"));
  }

  @Test
  void register_missingInterfacesThrows() {
    Extension body = userBody("my_user_ext");
    body.setSupportedInterfaces(List.of());
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.registerExtension(BASE, body));
    assertTrue(ex.getMessage().contains("supportedInterfaces"));
  }

  @Test
  void register_missingClassNameForJavaThrows() {
    Extension body = userBody("my_user_ext");
    body.setInitParameters(Map.of());
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.registerExtension(BASE, body));
    assertTrue(ex.getMessage().contains("className"));
  }

  @Test
  void register_nonAdminIs403() {
    ExtensionAdaptor denied = new ExtensionAdaptor(extensionService, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.registerExtension(BASE, userBody("my_user_ext")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void update_mutatesInitParams() {
    adaptor.registerExtension(BASE, userBody("my_user_ext"));
    Extension body = userBody("my_user_ext");
    body.setInitParameters(
        Map.of(
            IPSExtensionDef.INIT_PARAM_CLASSNAME,
            "com.example.MyExt",
            IPSExtensionDef.INIT_PARAM_DESCRIPTION,
            "updated via REST"));
    body.setDeprecated(true);

    Extension out = adaptor.updateExtension(BASE, "my_user_ext", body);

    assertTrue(out.isDeprecated());
    assertEquals("updated via REST", out.getInitParameters().get(IPSExtensionDef.INIT_PARAM_DESCRIPTION));
  }

  @Test
  void update_nullInitParamValueDeletesKey() {
    adaptor.registerExtension(BASE, userBody("my_user_ext"));
    Extension add = userBody("my_user_ext");
    add.setInitParameters(
        Map.of(
            IPSExtensionDef.INIT_PARAM_CLASSNAME,
            "com.example.MyExt",
            "extra",
            "keep"));
    adaptor.updateExtension(BASE, "my_user_ext", add);

    Extension clear = userBody("my_user_ext");
    Map<String, String> params = new HashMap<>();
    params.put(IPSExtensionDef.INIT_PARAM_CLASSNAME, "com.example.MyExt");
    // Omit extra (Jackson drops nulls); replace merge must delete it.
    clear.setInitParameters(params);
    Extension out = adaptor.updateExtension(BASE, "my_user_ext", clear);

    assertFalse(out.getInitParameters().containsKey("extra"));
    assertEquals(
        "com.example.MyExt",
        out.getInitParameters().get(IPSExtensionDef.INIT_PARAM_CLASSNAME));
  }

  @Test
  void update_systemExtensionIs409() throws Exception {
    seedSystemExtension();
    Extension body = userBody("sys_add");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateExtension(BASE, "sys_add", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(extensionService, never()).updateExtension(any(IPSExtensionDef.class), any());
  }

  @Test
  void update_handlerOwnedIs409() throws Exception {
    seedHandlerExtension();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateExtension(BASE, "Java", userBody("Java")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void update_unknownReturnsNull() {
    assertEquals(null, adaptor.updateExtension(BASE, "missing", userBody("missing")));
  }

  @Test
  void register_persistsMethodMapRoundTrip() {
    Extension body = userBody("my_user_ext");
    body.setMethods(List.of(method("productVersion", "java.lang.String", "ver")));

    Extension created = adaptor.registerExtension(BASE, body);
    Extension fetched = adaptor.findExtensionByKey(BASE, created.getFqn());

    assertNotNull(fetched.getMethods());
    assertEquals(1, fetched.getMethods().size());
    ExtensionMethod meth = fetched.getMethods().get(0);
    assertNotNull(meth);
    assertEquals("productVersion", meth.getName());
    assertEquals("java.lang.String", meth.getReturnType());
    assertEquals("ver", meth.getDescription());
  }

  @Test
  void update_replacesAndClearsMethodMap() {
    Extension body = userBody("my_user_ext");
    body.setMethods(List.of(method("oldMethod", "java.lang.Object", "old")));
    adaptor.registerExtension(BASE, body);

    Extension replace = userBody("my_user_ext");
    ExtensionMethod next = method("newMethod", "java.lang.Integer", "n");
    ExtensionParameter p = new ExtensionParameter();
    p.setName("n");
    p.setDataType("int");
    next.setParameters(List.of(p));
    replace.setMethods(List.of(next));
    Extension updated = adaptor.updateExtension(BASE, "my_user_ext", replace);
    assertEquals(1, updated.getMethods().size());
    assertEquals("newMethod", updated.getMethods().get(0).getName());
    assertEquals("java.lang.Integer", updated.getMethods().get(0).getReturnType());
    assertEquals(1, updated.getMethods().get(0).getParameters().size());
    assertEquals("n", updated.getMethods().get(0).getParameters().get(0).getName());

    Extension omit = userBody("my_user_ext");
    omit.setMethods(null);
    Extension kept = adaptor.updateExtension(BASE, "my_user_ext", omit);
    assertEquals("newMethod", kept.getMethods().get(0).getName());

    Extension clear = userBody("my_user_ext");
    clear.setMethods(List.of());
    Extension cleared = adaptor.updateExtension(BASE, "my_user_ext", clear);
    assertTrue(cleared.getMethods() == null || cleared.getMethods().isEmpty());
  }

  @Test
  void update_blankMethodNameClears() {
    adaptor.registerExtension(BASE, userBody("my_user_ext"));
    Extension body = userBody("my_user_ext");
    body.setMethods(List.of(method(" ", "java.lang.Object", "")));
    Extension clearedBlanks = adaptor.updateExtension(BASE, "my_user_ext", body);
    assertTrue(clearedBlanks.getMethods() == null || clearedBlanks.getMethods().isEmpty());
  }

  @Test
  void update_duplicateMethodNameIs400() {
    List<ExtensionMethod> dup =
        List.of(
            method("productVersion", "java.lang.String", ""),
            method("productVersion", "java.lang.String", ""));
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> ExtensionAdaptor.requireMethods(dup));
    assertTrue(ex.getMessage().contains("duplicate method name"));
  }

  @Test
  void update_systemMethodMapIs409() throws Exception {
    seedSystemExtension();
    Extension body = userBody("sys_add");
    body.setMethods(List.of(method("x", "java.lang.Object", "")));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateExtension(BASE, "sys_add", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void requireMethods_defaultsReturnType() {
    Map<String, ExtensionMethod> out =
        ExtensionAdaptor.requireMethods(List.of(method("m", null, "d")));
    assertEquals(ExtensionAdaptor.DEFAULT_METHOD_RETURN_TYPE, out.get("m").getReturnType());
  }

  @Test
  void register_persistsRuntimeParametersRoundTrip() {
    Extension body = userBody("my_user_ext");
    body.setRuntimeParameters(List.of(runtimeParam("htmlParams", "java.util.Map", "params")));

    Extension created = adaptor.registerExtension(BASE, body);
    Extension fetched = adaptor.findExtensionByKey(BASE, created.getFqn());

    assertNotNull(fetched.getRuntimeParameters());
    assertEquals(1, fetched.getRuntimeParameters().size());
    assertEquals("htmlParams", fetched.getRuntimeParameters().get(0).getName());
    assertEquals("java.util.Map", fetched.getRuntimeParameters().get(0).getDataType());
    assertEquals("params", fetched.getRuntimeParameters().get(0).getDescription());
  }

  @Test
  void update_replacesAndClearsRuntimeParameters() {
    Extension body = userBody("my_user_ext");
    body.setRuntimeParameters(List.of(runtimeParam("oldParam", "java.lang.String", "old")));
    adaptor.registerExtension(BASE, body);

    Extension replace = userBody("my_user_ext");
    replace.setRuntimeParameters(List.of(runtimeParam("htmlParams", "java.util.Map", "params")));
    Extension updated = adaptor.updateExtension(BASE, "my_user_ext", replace);
    assertEquals(1, updated.getRuntimeParameters().size());
    assertEquals("htmlParams", updated.getRuntimeParameters().get(0).getName());

    Extension omit = userBody("my_user_ext");
    omit.setRuntimeParameters(null);
    Extension kept = adaptor.updateExtension(BASE, "my_user_ext", omit);
    assertEquals(1, kept.getRuntimeParameters().size());
    assertEquals("htmlParams", kept.getRuntimeParameters().get(0).getName());

    Extension clear = userBody("my_user_ext");
    clear.setRuntimeParameters(List.of());
    Extension cleared = adaptor.updateExtension(BASE, "my_user_ext", clear);
    assertTrue(
        cleared.getRuntimeParameters() == null || cleared.getRuntimeParameters().isEmpty());
  }

  @Test
  void update_systemRuntimeParametersIs409() throws Exception {
    seedSystemExtension();
    Extension body = userBody("sys_add");
    body.setRuntimeParameters(List.of(runtimeParam("htmlParams", "java.util.Map", "")));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateExtension(BASE, "sys_add", body));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void update_blankRuntimeParamNameClears() {
    // Wire clear path: JAXB drops empty arrays, so the SPA sends [{name:""}].
    Extension body = userBody("my_user_ext");
    body.setRuntimeParameters(List.of(runtimeParam("htmlParams", "java.util.Map", "params")));
    adaptor.registerExtension(BASE, body);

    Extension clear = userBody("my_user_ext");
    clear.setRuntimeParameters(List.of(runtimeParam(" ", null, null)));
    Extension cleared = adaptor.updateExtension(BASE, "my_user_ext", clear);
    assertTrue(
        cleared.getRuntimeParameters() == null || cleared.getRuntimeParameters().isEmpty());
  }

  @Test
  void delete_removesUserExtension() {
    adaptor.registerExtension(BASE, userBody("my_user_ext"));
    assertTrue(adaptor.deleteExtension(BASE, "my_user_ext"));
    assertEquals(null, adaptor.findExtensionByKey(BASE, "my_user_ext"));
  }

  @Test
  void delete_systemIs409() throws Exception {
    seedSystemExtension();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.deleteExtension(BASE, "sys_add"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(extensionService, never()).removeExtension(any(PSExtensionRef.class));
  }

  @Test
  void delete_unknownReturnsFalse() {
    assertFalse(adaptor.deleteExtension(BASE, "missing"));
  }

  @Test
  void isImmutableHelpers() {
    Extension system = new Extension();
    system.setHandlerName("Java");
    system.setContext("global/percussion/exit/");
    system.setExtensionName("sys_add");
    assertTrue(ExtensionAdaptor.isImmutableExtension(system));
    assertTrue(ExtensionAdaptor.isImmutableContext("global/percussion/generic"));

    Extension user = new Extension();
    user.setHandlerName("Java");
    user.setContext("user/");
    user.setExtensionName("my_user_ext");
    assertFalse(ExtensionAdaptor.isImmutableExtension(user));

    Extension handler = new Extension();
    handler.setHandlerName(IPSExtensionHandler.HANDLER_HANDLER);
    handler.setContext(IPSExtensionHandler.HANDLER_CONTEXT);
    handler.setExtensionName("Java");
    assertTrue(ExtensionAdaptor.isImmutableExtension(handler));

    Extension blankHandler = new Extension();
    blankHandler.setHandlerName("");
    blankHandler.setContext("user/");
    blankHandler.setExtensionName("orphan");
    assertTrue(ExtensionAdaptor.isImmutableExtension(blankHandler));
  }

  @Test
  void register_raceExistsFalseInstallNonUniqueIs409() throws Exception {
    // Race: exists() returned false, but installExtension still throws PSNonUniqueException.
    // Production maps that directly to 409 (not wrapped PSExtensionException → 500).
    IPSExtensionService svc = mock(IPSExtensionService.class);
    when(svc.exists(any())).thenReturn(false);
    doThrow(new PSNonUniqueException(1, "dup")).when(svc).installExtension(any(), any());
    ExtensionAdaptor local = new ExtensionAdaptor(svc, () -> true);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> local.registerExtension(BASE, userBody("my_user_ext")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void register_existsThrowsPsExtensionExceptionIs500() throws Exception {
    IPSExtensionService svc = mock(IPSExtensionService.class);
    when(svc.exists(any())).thenThrow(new PSExtensionException(1, "exists failed"));
    ExtensionAdaptor local = new ExtensionAdaptor(svc, () -> true);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> local.registerExtension(BASE, userBody("my_user_ext")));
    assertTrue(ex.getMessage().contains("existence"));
  }

  private void seedSystemExtension() throws Exception {
    PSExtensionRef ref =
        new PSExtensionRef("Java", "global/percussion/generic/", "sys_add");
    Properties init = new Properties();
    init.setProperty(IPSExtensionDef.INIT_PARAM_CLASSNAME, "com.percussion.generic.PSAdd");
    PSExtensionDef def =
        new PSExtensionDef(
            ref,
            List.of("com.percussion.extension.IPSUdfProcessor").iterator(),
            Collections.emptyIterator(),
            init,
            Collections.emptyIterator());
    installed.put(ref.getFQN(), def);
  }

  private void seedHandlerExtension() throws Exception {
    PSExtensionRef ref =
        new PSExtensionRef(
            IPSExtensionHandler.HANDLER_HANDLER,
            IPSExtensionHandler.HANDLER_CONTEXT + "/",
            "Java");
    Properties init = new Properties();
    init.setProperty(IPSExtensionDef.INIT_PARAM_CLASSNAME, "com.percussion.extension.PSJavaExtensionHandler");
    PSExtensionDef def =
        new PSExtensionDef(
            ref,
            List.of("com.percussion.extension.IPSExtensionHandler").iterator(),
            Collections.emptyIterator(),
            init,
            Collections.emptyIterator());
    installed.put(ref.getFQN(), def);
  }

  private static Extension userBody(String name) {
    Extension e = new Extension();
    e.setExtensionName(name);
    e.setHandlerName("Java");
    e.setSupportedInterfaces(List.of("com.percussion.extension.IPSUdfProcessor"));
    e.setInitParameters(Map.of(IPSExtensionDef.INIT_PARAM_CLASSNAME, "com.example.MyExt"));
    return e;
  }

  private static ExtensionMethod method(String name, String returnType, String description) {
    ExtensionMethod m = new ExtensionMethod();
    m.setName(name);
    m.setReturnType(returnType);
    m.setDescription(description);
    return m;
  }

  private static ExtensionParameter runtimeParam(String name, String dataType, String description) {
    ExtensionParameter p = new ExtensionParameter();
    p.setName(name);
    p.setDataType(dataType);
    p.setDescription(description);
    return p;
  }
}
