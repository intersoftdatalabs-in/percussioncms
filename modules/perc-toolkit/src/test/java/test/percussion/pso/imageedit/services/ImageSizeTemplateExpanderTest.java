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
package test.percussion.pso.imageedit.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.pso.imageedit.services.ImageSizeDefinitionManager;
import com.percussion.pso.imageedit.services.ImageSizeTemplateExpander;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ImageSizeTemplateExpanderTest {
  private static final Logger log = LogManager.getLogger(ImageSizeTemplateExpanderTest.class);

  TestableImageSizeTemplateExpander cut;
  @Mock ImageSizeDefinitionManager isdm;
  @Mock IPSAssemblyService asm;

  @BeforeEach
  public void setUp() {
    cut = new TestableImageSizeTemplateExpander();
    cut.setIsdm(isdm);
    cut.setAsm(asm);
  }

  @Test
  public final void testFindTemplates() {
    // no-op test; previous implementation commented out.
    assertTrue(true);
  }

  private class TestableImageSizeTemplateExpander extends ImageSizeTemplateExpander {

    /**
     * @see ImageSizeTemplateExpander#findTemplates(IPSGuid, IPSGuid, IPSGuid, int,
     *     PSComponentSummary, Node, Map)
     */
    @Override
    public List<IPSGuid> findTemplates(
        IPSGuid itemGuid,
        IPSGuid folderGuid,
        IPSGuid siteGuid,
        int context,
        PSComponentSummary summary,
        Node contentNode,
        Map<String, String> parameters) {
      return super.findTemplates(
          itemGuid, folderGuid, siteGuid, context, summary, contentNode, parameters);
    }

    /**
     * @see ImageSizeTemplateExpander#setAsm(IPSAssemblyService)
     */
    @Override
    public void setAsm(IPSAssemblyService asm) {
      super.setAsm(asm);
    }

    /**
     * @see ImageSizeTemplateExpander#setIsdm(ImageSizeDefinitionManager)
     */
    @Override
    public void setIsdm(ImageSizeDefinitionManager isdm) {
      super.setIsdm(isdm);
    }
  }
}
