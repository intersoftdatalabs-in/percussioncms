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
package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.percussion.log.PSLogApplicationStatistics;
import com.percussion.log.PSLogMultipleHandlers;
import com.percussion.log.PSLogSubMessage;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Behavioral tests for typed dataHandlers / request-root maps (#3212 residual of #3186).
 */
class PSApplicationHandlerDataHandlersTypedTest {

  private ConcurrentHashMap<String, IPSRequestHandler> savedRequestHandlers;
  private ConcurrentHashMap<String, IPSRequestHandler> savedRootedHandlers;
  private Map<String, Map<IPSHandlerStateListener, Integer>> savedListenerMap;

  @BeforeEach
  void snapshotServerMaps() throws Exception {
    savedRequestHandlers = PSServer.ms_RequestHandlers;
    savedRootedHandlers = getRootedHandlers();
    savedListenerMap = getHandlerStateListenerMap();
    PSServer.ms_RequestHandlers = new ConcurrentHashMap<>();
    setRootedHandlers(new ConcurrentHashMap<>());
    setHandlerStateListenerMap(new HashMap<>());
  }

  @AfterEach
  void restoreServerMaps() throws Exception {
    PSServer.ms_RequestHandlers = savedRequestHandlers;
    setRootedHandlers(savedRootedHandlers);
    setHandlerStateListenerMap(savedListenerMap);
  }

  @Test
  @DisplayName("PSRequestHandlerDef stores typed request roots and HTTP methods")
  void requestHandlerDefTypedRootsAndMethods() {
    PSRequestHandlerDef def =
        new PSRequestHandlerDef(
            "compare",
            "com.percussion.server.compare.PSCompareRequestHandler",
            new File("compare.xml"),
            List.of("compare", "diff").iterator());

    List<String> roots = iteratorToList(def.getRequestRoots());
    assertEquals(2, roots.size());
    assertTrue(roots.contains("compare"));
    assertTrue(roots.contains("diff"));

    def.addRequestMethods("compare", List.of("GET", "POST").iterator());
    List<String> methods = iteratorToList(def.getRequestMethods("compare"));
    assertEquals(List.of("GET", "POST"), methods);
    assertNull(def.getRequestMethods("diff"));
  }

  @Test
  @DisplayName("PSRequestHandlerDef rejects missing or unknown request roots")
  void requestHandlerDefRejectsInvalidRoots() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSRequestHandlerDef(
                "h", "c", null, Collections.emptyIterator()));

    PSRequestHandlerDef def =
        new PSRequestHandlerDef("h", "c", null, List.of("root").iterator());
    assertThrows(
        IllegalArgumentException.class,
        () -> def.addRequestMethods("missing", List.of("GET").iterator()));
    assertThrows(IllegalArgumentException.class, () -> def.getRequestMethods("missing"));
    assertThrows(IllegalArgumentException.class, () -> def.getRequestMethods(null));
  }

  @Test
  @DisplayName("addHandlerStateListener merges ORed event flags per listener")
  void handlerStateListenerEventsMerge() throws Exception {
    AtomicInteger started = new AtomicInteger();
    IPSHandlerStateListener listener = e -> started.incrementAndGet();

    PSServer.addHandlerStateListener(
        listener, "sys_cx", PSHandlerStateEvent.HANDLER_EVENT_STARTED);
    PSServer.addHandlerStateListener(
        listener, "sys_cx", PSHandlerStateEvent.HANDLER_EVENT_STOPPED);

    Map<IPSHandlerStateListener, Integer> events = getHandlerStateListenerMap().get("sys_cx");
    assertNotNull(events);
    assertEquals(
        PSHandlerStateEvent.HANDLER_EVENT_STARTED | PSHandlerStateEvent.HANDLER_EVENT_STOPPED,
        events.get(listener).intValue());
  }

  @Test
  @DisplayName("addHandlerStateListener rejects null listener or empty handler name")
  void handlerStateListenerRejectsInvalidArgs() {
    IPSHandlerStateListener listener = e -> {};
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSServer.addHandlerStateListener(
                null, "sys_cx", PSHandlerStateEvent.HANDLER_EVENT_STARTED));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSServer.addHandlerStateListener(
                listener, "", PSHandlerStateEvent.HANDLER_EVENT_STARTED));
  }

  @Test
  @DisplayName("getRequestHandlersStatus lists typed handler names and classes")
  void requestHandlersStatusUsesTypedMaps() {
    IPSRequestHandler dummy = new NoopRequestHandler();
    PSServer.ms_RequestHandlers.put("extdata-compare", dummy);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element status = PSServer.getRequestHandlersStatus(doc, false);
    assertEquals("RequestHandlers", status.getNodeName());
    NodeList handlers = status.getElementsByTagName("Handler");
    assertEquals(1, handlers.getLength());
    Element handler = (Element) handlers.item(0);
    assertEquals("extdata-compare", handler.getAttribute("name"));
    assertEquals(NoopRequestHandler.class.getName(), handler.getAttribute("class"));
  }

  @Test
  @DisplayName("getApplicationHandler returns null for missing apps and CCE for wrong types")
  void getApplicationHandlerFailFastOnWrongType() {
    IPSRequestHandler dummy = new NoopRequestHandler();
    PSServer.ms_RequestHandlers.put("data-foo", dummy);
    assertNull(PSServer.getApplicationHandler("missing"));
    assertThrows(ClassCastException.class, () -> PSServer.getApplicationHandler("foo"));
    assertFalse(PSServer.isApplicationActive("missing"));
    assertTrue(PSServer.isApplicationActive("foo"));
  }

  @Test
  @DisplayName("getInternalRequestHandler returns null when missing and CCE for wrong types")
  void getInternalRequestHandlerFailFastOnWrongType() throws Exception {
    PSApplicationHandler ah = mock(PSApplicationHandler.class, CALLS_REAL_METHODS);
    ConcurrentHashMap<String, IPSRequestHandler> handlers = new ConcurrentHashMap<>();
    Field dataHandlers = PSApplicationHandler.class.getDeclaredField("m_dataHandlers");
    dataHandlers.setAccessible(true);
    dataHandlers.set(ah, handlers);

    assertNull(ah.getInternalRequestHandler("missing"));
    handlers.put("ds", new NoopRequestHandler());
    assertThrows(ClassCastException.class, () -> ah.getInternalRequestHandler("ds"));
    assertThrows(IllegalArgumentException.class, () -> ah.getInternalRequestHandler(""));
    assertThrows(IllegalArgumentException.class, () -> ah.getInternalRequestHandler((String) null));
  }

  @Test
  @DisplayName("PSLogApplicationStatistics reads typed string map keys")
  void logApplicationStatisticsTypedMap() {
    Map<String, String> stats = new HashMap<>();
    stats.put("elapsedTime", "10");
    stats.put("eventsProcessed", "2");
    stats.put("eventsPending", "0");
    stats.put("eventsFailed", "1");
    stats.put("cacheHits", "3");
    stats.put("cacheMisses", "4");
    stats.put("minProcTime", "5");
    stats.put("maxProcTime", "6");
    stats.put("avgProcTime", "7");

    PSLogApplicationStatistics msg = new PSLogApplicationStatistics(42, stats);
    PSLogSubMessage[] subs = msg.getSubMessages();
    assertEquals(9, subs.length);
    assertEquals("10", subs[0].getText());
    assertEquals("2", subs[1].getText());
    assertEquals("7", subs[8].getText());
  }

  @Test
  @DisplayName("PSLogMultipleHandlers reads typed session and dataset keys")
  void logMultipleHandlersTypedMap() {
    ConcurrentHashMap<String, String> info = new ConcurrentHashMap<>();
    info.put(PSLogMultipleHandlers.PROP_SESS_ID, "sess-1");
    info.put(PSLogMultipleHandlers.PROP_DATASET_NAMES, "dsA, dsB");

    PSLogMultipleHandlers msg = new PSLogMultipleHandlers(7, info);
    PSLogSubMessage[] subs = msg.getSubMessages();
    assertEquals("sess-1", subs[0].getText());
    assertEquals("dsA, dsB", subs[1].getText());
  }

  @Test
  @DisplayName("dataHandlers and request-root field types are generic")
  void fieldGenericSignatures() throws Exception {
    Field dataHandlers = PSApplicationHandler.class.getDeclaredField("m_dataHandlers");
    assertTrue(dataHandlers.getGenericType().getTypeName().contains("IPSRequestHandler"));

    Field dataHandlerMap = PSApplicationHandler.class.getDeclaredField("m_dataHandlerMap");
    assertTrue(dataHandlerMap.getGenericType().getTypeName().contains("PSRequestPageMap"));

    Field requestRoots = PSRequestHandlerDef.class.getDeclaredField("m_requestRoots");
    assertTrue(requestRoots.getGenericType().getTypeName().contains("ArrayList"));

    Method getRoots = PSApplicationHandler.class.getMethod("getRequestRoots");
    assertTrue(getRoots.getGenericReturnType().getTypeName().contains("String"));
  }

  @Test
  @DisplayName("getHandlerDefs returns typed iterator")
  void handlerDefsIteratorType() throws Exception {
    Method m = PSRequestHandlerConfiguration.class.getMethod("getHandlerDefs");
    assertTrue(m.getGenericReturnType().getTypeName().contains("PSRequestHandlerDef"));
  }

  @Test
  @DisplayName("rooted handler collection is typed")
  void rootedHandlersCollectionType() throws Exception {
    Method m = PSServer.class.getDeclaredMethod("getRootedAppHandlers");
    assertTrue(m.getGenericReturnType().getTypeName().contains("IPSRequestHandler"));
  }

  private static List<String> iteratorToList(Iterator<String> it) {
    List<String> out = new ArrayList<>();
    if (it == null) {
      return out;
    }
    while (it.hasNext()) {
      out.add(it.next());
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private static ConcurrentHashMap<String, IPSRequestHandler> getRootedHandlers()
      throws Exception {
    Field f = PSServer.class.getDeclaredField("ms_rootedRequestHandlers");
    f.setAccessible(true);
    return (ConcurrentHashMap<String, IPSRequestHandler>) f.get(null);
  }

  private static void setRootedHandlers(ConcurrentHashMap<String, IPSRequestHandler> map)
      throws Exception {
    Field f = PSServer.class.getDeclaredField("ms_rootedRequestHandlers");
    f.setAccessible(true);
    f.set(null, map);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Map<IPSHandlerStateListener, Integer>> getHandlerStateListenerMap()
      throws Exception {
    Field f = PSServer.class.getDeclaredField("ms_handlerStateListenerMap");
    f.setAccessible(true);
    return (Map<String, Map<IPSHandlerStateListener, Integer>>) f.get(null);
  }

  private static void setHandlerStateListenerMap(
      Map<String, Map<IPSHandlerStateListener, Integer>> map) throws Exception {
    Field f = PSServer.class.getDeclaredField("ms_handlerStateListenerMap");
    f.setAccessible(true);
    f.set(null, map);
  }

  private static final class NoopRequestHandler implements IPSRequestHandler {
    @Override
    public void processRequest(PSRequest request) {
      // no-op
    }

    @Override
    public void shutdown() {
      // no-op
    }
  }
}
