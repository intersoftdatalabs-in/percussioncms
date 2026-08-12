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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.server.compare.PSCompareRequestHandler;
import com.percussion.server.job.PSJobHandlerConfiguration;
import com.percussion.server.job.PSJobRunner;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral unit tests for typed residual collections under {@code com.percussion.server}
 * (non-cache, non-webservices) for issue #3170.
 */
class PSServerResidualTypedTest {

  @Test
  @DisplayName("getPersistedPropertyMeta rejects both category and name empty")
  void getPersistedPropertyMetaRejectsEmptyKeys() {
    PSPersistentPropertyManager mgr = PSPersistentPropertyManager.getInstance();
    assertThrows(
        IllegalArgumentException.class,
        () -> mgr.getPersistedPropertyMeta("", null, ""));
  }

  @Test
  @DisplayName("getPersistedProperty rejects both category and name empty")
  void getPersistedPropertyRejectsEmptyKeys() {
    PSPersistentPropertyManager mgr = PSPersistentPropertyManager.getInstance();
    assertThrows(
        IllegalArgumentException.class, () -> mgr.getPersistedProperty("", null, ""));
  }

  @Test
  @DisplayName("save rejects null collection")
  void saveRejectsNullCollection() {
    PSPersistentPropertyManager mgr = PSPersistentPropertyManager.getInstance();
    assertThrows(IllegalArgumentException.class, () -> mgr.save(null, null));
  }

  @Test
  @DisplayName("getUserName rejects null session")
  void getUserNameRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> PSPersistentPropertyManager.getUserName(null));
  }

  @Test
  @DisplayName("compare handler init accepts typed request roots")
  void compareHandlerInitTypedRoots() {
    PSCompareRequestHandler handler = new PSCompareRequestHandler();
    List<String> roots = new ArrayList<>();
    roots.add("compare");
    handler.init(roots, null);
    Iterator<String> it = handler.getRequestRoots();
    assertTrue(it.hasNext());
    assertEquals("compare", it.next());
    assertFalse(it.hasNext());
  }

  @Test
  @DisplayName("compare handler init rejects empty roots")
  void compareHandlerInitRejectsEmpty() {
    PSCompareRequestHandler handler = new PSCompareRequestHandler();
    assertThrows(
        IllegalArgumentException.class, () -> handler.init(new ArrayList<>(), null));
  }

  @Test
  @DisplayName("PSJobHandlerConfiguration loads typed job class and init params")
  void jobHandlerConfigTypedMaps() throws Exception {
    String xml =
        """
        <PSXJobHandlerConfiguration>
          <InitParams>
            <InitParam name="h1" value="v1"/>
          </InitParams>
          <Categories>
            <Category name="cat1">
              <InitParams>
                <InitParam name="c1" value="cv1"/>
              </InitParams>
              <Jobs>
                <Job jobType="jt1" className="com.example.JobRunner">
                  <InitParams>
                    <InitParam name="j1" value="jv1"/>
                  </InitParams>
                </Job>
              </Jobs>
            </Category>
          </Categories>
        </PSXJobHandlerConfiguration>
        """;
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
    PSJobHandlerConfiguration cfg = new PSJobHandlerConfiguration(doc);
    assertEquals("com.example.JobRunner", cfg.getJobClassName("cat1", "jt1"));
    Properties props = cfg.getJobInitParams("cat1", "jt1");
    assertNotNull(props);
    assertEquals("jv1", props.getProperty("j1"));
    assertEquals("cv1", props.getProperty("c1"));
    assertEquals("v1", props.getProperty("h1"));
  }

  @Test
  @DisplayName("PSJobRunner typed listeners fire on setCompleted")
  void jobRunnerTypedListeners() throws Exception {
    TestJobRunner runner = new TestJobRunner();
    runner.init(42, null, null, null);
    final long[] completedId = {-1L};
    runner.addJobListener(id -> completedId[0] = id);
    Method setCompleted = PSJobRunner.class.getDeclaredMethod("setCompleted");
    setCompleted.setAccessible(true);
    setCompleted.invoke(runner);
    assertEquals(42L, completedId[0]);
    assertTrue(runner.isCompleted());
  }

  @Test
  @DisplayName("singleton PSPersistentPropertyManager is stable")
  void managerSingleton() {
    assertSame(
        PSPersistentPropertyManager.getInstance(), PSPersistentPropertyManager.getInstance());
  }

  @Test
  @DisplayName("public collection return types are generic (compile-time contract)")
  void publicApiCollectionTypes() throws Exception {
    Method meta =
        PSPersistentPropertyManager.class.getMethod(
            "getPersistedPropertyMeta", String.class, PSUserSession.class, String.class);
    assertEquals(Collection.class, meta.getReturnType());
    // generic signature retains type arguments
    assertTrue(meta.getGenericReturnType().getTypeName().contains("PSPersistentPropertyMeta"));

    Method props =
        PSPersistentPropertyManager.class.getMethod(
            "getPersistedProperty", String.class, PSUserSession.class, String.class);
    assertTrue(props.getGenericReturnType().getTypeName().contains("PSPersistentProperty"));

    Method sessionProps = PSUserSession.class.getMethod("getUserProperties");
    assertTrue(sessionProps.getGenericReturnType().getTypeName().contains("PSPersistentProperty"));
  }

  /** Minimal concrete job runner for listener tests. */
  private static final class TestJobRunner extends PSJobRunner {
    @Override
    public void init(
        int id, Document descriptor, PSRequest req, Properties initParams) {
      m_id = id;
    }

    @Override
    public void doRun() {
      // no-op
    }
  }
}
