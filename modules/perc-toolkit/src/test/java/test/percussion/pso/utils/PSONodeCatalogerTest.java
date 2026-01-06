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
/*
 * test.percussion.pso.utils PSONodeCatalogerTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.percussion.pso.utils.PSONodeCataloger;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import java.util.Arrays;
import java.util.List;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PSONodeCatalogerTest {

  private static final Logger log = LogManager.getLogger(PSONodeCatalogerTest.class);

  @Mock private IPSContentMgr cmgr;
  @Mock private IPSNodeDefinition t1;
  @Mock private IPSNodeDefinition t2;
  @Mock private IPSNodeDefinition nd1;
  @Mock private IPSNodeDefinition nd2;
  @Mock private NodeType t1NodeType;
  @Mock private NodeType t2NodeType;
  @Mock private PropertyDefinition p1;
  @Mock private PropertyDefinition p2;
  @Mock private PropertyDefinition p3;
  @Mock private NodeTypeIterator nodes;
  @Mock private IPSNodeDefinition nodeDef;
  @Mock private NodeType t1Type;

  private PSONodeCataloger cut;

  @BeforeEach
  public void setUp() {
    cut = new PSONodeCataloger();
    cut.setCmgr(cmgr);
  }

  @Test
  void testGetContentTypeNames() {
    log.info("Getting content type names");

    final List<IPSNodeDefinition> nodeDefs = Arrays.asList(t1, t2);
    try {
      when(cmgr.findAllItemNodeDefinitions()).thenReturn(nodeDefs);
      when(t1.getName()).thenReturn("type1");
      when(t2.getName()).thenReturn("type2");

      List<String> names = cut.getContentTypeNames();
      assertNotNull(names);
      assertEquals(2, names.size());
      assertEquals("type1", names.get(0));
      assertEquals("type2", names.get(1));
    } catch (Exception e) {
      log.error("Unexpected Exception" + e, e);
      fail("Exception");
    }
    log.info("test complete");
  }

  @Test
  void testGetContentTypeNamesWithField() {
    log.info("Getting content type names with field");
    final List<IPSNodeDefinition> nodes = Arrays.asList(nd1, nd2);
    final PropertyDefinition[] t1p = new PropertyDefinition[] {p1, p2, p3};
    final PropertyDefinition[] t2p = new PropertyDefinition[] {p1, p3};
    try {
      when(cmgr.findAllItemNodeDefinitions()).thenReturn(nodes);
      when(nd1.getDeclaringNodeType()).thenReturn(t1NodeType);
      when(nd1.getName()).thenReturn("rx:node1");
      when(nd2.getName()).thenReturn("rx:node2");
      when(nd2.getDeclaringNodeType()).thenReturn(t2NodeType);
      when(t1NodeType.getName()).thenReturn("rx:type1");
      when(t2NodeType.getName()).thenReturn("rx:type2");
      when(t1NodeType.getDeclaredPropertyDefinitions()).thenReturn(t1p);
      when(t2NodeType.getDeclaredPropertyDefinitions()).thenReturn(t2p);
      when(p1.getName()).thenReturn("rx:prop1");
      when(p2.getName()).thenReturn("rx:prop2");
      when(p3.getName()).thenReturn("rx:prop3");

      List<String> names = cut.getContentTypeNamesWithField("prop2");
      assertNotNull(names);
      assertEquals(1, names.size());
      assertEquals("rx:type1", names.get(0));
    } catch (Exception e) {
      log.error("Unexpected Exception" + e, e);
      fail("Exception");
    }
    log.info("test complete");
  }

  @Test
  void testGetFieldNamesForContentType() {
    log.info("Getting content type names with field");

    final PropertyDefinition[] t1p = new PropertyDefinition[] {p1, p2, p3};
    try {
      when(cmgr.findNodeDefinitionByName("rx:type1")).thenReturn(nodeDef);
      when(nodeDef.getDeclaringNodeType()).thenReturn(t1Type);
      when(t1Type.getName()).thenReturn("rx:type1");
      when(t1Type.getDeclaredPropertyDefinitions()).thenReturn(t1p);
      when(p1.getName()).thenReturn("rx:prop1");
      when(p2.getName()).thenReturn("rx:prop2");
      when(p3.getName()).thenReturn("rx:prop3");

      List<String> names = cut.getFieldNamesForContentType("type1");
      assertNotNull(names);
      assertEquals(3, names.size());
      assertEquals("rx:prop1", names.get(0));
    } catch (Exception e) {
      log.error("Unexpected Exception" + e, e);
      fail("Exception");
    }
    log.info("test complete");
  }
}
