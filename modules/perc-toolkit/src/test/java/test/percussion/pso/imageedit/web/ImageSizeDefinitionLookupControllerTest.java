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
package test.percussion.pso.imageedit.web;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.imageedit.data.ImageSizeDefinition;
import com.percussion.pso.imageedit.services.ImageSizeDefinitionManager;
import com.percussion.pso.imageedit.web.ImageSizeDefinitionLookupController;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;

@ExtendWith(MockitoExtension.class)
public class ImageSizeDefinitionLookupControllerTest {
  private static final Logger log =
      LogManager.getLogger(ImageSizeDefinitionLookupControllerTest.class);

  private ImageSizeDefinitionLookupController cut;

  @Mock
  private ImageSizeDefinitionManager defmgr;

  @BeforeEach
  public void setUp() {
    cut = new ImageSizeDefinitionLookupController();
    cut.setDefmgr(defmgr);
    cut.setResultKey("result");
  }

  @Test
  public final void testHandleRequestInternalHttpServletRequestHttpServletResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();

    final ImageSizeDefinition sizea =
        new ImageSizeDefinition() {
          {
            setCode("a");
            setLabel("Size A");
          }
        };
    final ImageSizeDefinition sizeb =
        new ImageSizeDefinition() {
          {
            setCode("b");
            setLabel("Size B");
          }
        };

    when(defmgr.getAllImageSizes()).thenReturn(Arrays.asList(new ImageSizeDefinition[] {sizea, sizeb}));

      ModelAndView mav = cut.handleRequest(request, response);

      Document result = (Document) mav.getModel().get("result");
      assertNotNull(result);
      log.info("Document is " + PSXmlDocumentBuilder.toString(result));

      PSXmlTreeWalker walker = new PSXmlTreeWalker(result.getDocumentElement());
      walker.getNextElement("PSXEntry", PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
      log.info("current element " + walker.getCurrentNodeName());
      walker.getNextElement("PSXDisplayText", PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
      String display = walker.getElementData();
      assertEquals("Size A", display);
      walker.getNextElement("Value", PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS);
      String value = walker.getElementData();
      assertEquals("a", value);

    // no exception expected
  }
}
