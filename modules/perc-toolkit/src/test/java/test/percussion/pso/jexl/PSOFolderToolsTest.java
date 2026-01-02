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
package test.percussion.pso.jexl;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.jexl.PSOFolderTools;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PSOFolderToolsTest {

  @Mock IPSContentWs contentWs;
  @Mock IPSGuidManager guidManager;
  @Mock IPSGuid guid;
  @Mock IPSAssemblyItem assemblyItem;
  @InjectMocks PSOFolderTools tools;

  @BeforeEach
  public void setUp() {
=======
import static org.junit.Assert.*;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.jexl.PSOFolderTools;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Arrays;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class PSOFolderToolsTest {

  Mockery context = new JUnit4Mockery();
  IPSContentWs contentWs;
  IPSGuidManager guidManager;
  IPSGuid guid;
  IPSAssemblyItem assemblyItem;
  PSOFolderTools tools;

  @Before
  public void setUp() throws Exception {
    contentWs = context.mock(IPSContentWs.class);
    guidManager = context.mock(IPSGuidManager.class);
    guid = context.mock(IPSGuid.class);
    assemblyItem = context.mock(IPSAssemblyItem.class);
>>>>>>> development-8.1.x
    tools = new PSOFolderTools();
    tools.setContentWs(contentWs);
    tools.setGuidManager(guidManager);
  }
<<<<<<< HEAD

  @Test
  @Disabled("JMock test, needs logic update")
  public void shouldGetFirstParentFolderPathFromGuid() throws Exception {
    Mockito.when(contentWs.findFolderPaths(guid)).thenReturn(new String[] {"a", "b", "c"});
    assertEquals("a", tools.getParentFolderPath(guid));
  }

  @Test
  @Disabled("JMock test, needs logic update")
  public void shouldGetParentFolderPathFromAssemblyItem() throws Exception {
    final PSFolder folder = new PSFolder("test", 1, 1, 1, "description");
    folder.setFolderPath("testPath");
    Mockito.when(assemblyItem.getFolderId()).thenReturn(1);
    Mockito.when(guidManager.makeGuid(Mockito.any(PSLocator.class))).thenReturn(guid);
    Mockito.when(contentWs.loadFolders(Arrays.asList(guid))).thenReturn(Arrays.asList(folder));
=======

  @Test
  @Ignore
  public void shouldGetFirstParentFolderPathFromGuid() throws Exception {
    context.checking(
        new Expectations() {
          {
            one(contentWs).findFolderPaths(guid);
            will(returnValue(new String[] {"a", "b", "c"}));
          }
        });
    assertEquals("a", tools.getParentFolderPath(guid));
  }

  @Test
  @Ignore
  public void shouldGetParentFolderPathFromAssemblyItem() throws Exception {
    final PSFolder folder = new PSFolder("test", 1, 1, 1, "description");
    folder.setFolderPath("testPath");
    context.checking(
        new Expectations() {
          {
            one(assemblyItem).getFolderId();
            will(returnValue(1));

            one(guidManager).makeGuid(new PSLocator(1, -1));
            will(returnValue(guid));

            one(contentWs).loadFolders(Arrays.asList(guid));
            will(returnValue(Arrays.asList(folder)));
          }
        });

>>>>>>> development-8.1.x
    assertEquals("testPath", tools.getParentFolderPath(assemblyItem));
  }
}
