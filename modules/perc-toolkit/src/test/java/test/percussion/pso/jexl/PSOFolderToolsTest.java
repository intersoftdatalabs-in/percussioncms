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
    tools = new PSOFolderTools();
    tools.setContentWs(contentWs);
    tools.setGuidManager(guidManager);
  }

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
    assertEquals("testPath", tools.getParentFolderPath(assemblyItem));
  }
}
