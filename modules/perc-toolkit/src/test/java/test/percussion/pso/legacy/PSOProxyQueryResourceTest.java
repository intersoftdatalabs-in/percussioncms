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
package test.percussion.pso.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// ...existing code...

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.w3c.dom.Document;

// ...existing code...
import com.percussion.extension.IPSExtensionDef;
import com.percussion.pso.legacy.PSOProxyQueryResource;
import com.percussion.server.IPSRequestContext;

@ExtendWith(MockitoExtension.class)
public class PSOProxyQueryResourceTest {
    @InjectMocks
    PSOProxyQueryResource proxy = new PSOProxyQueryResource();

    @Mock
    IPSExtensionDef extensionDef;

    @Mock
    IPSRequestContext request;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(extensionDef.getRuntimeParameterNames())
            .thenReturn(java.util.Collections.singletonList("url").iterator());
        proxy.init(extensionDef, null);
    }
    
    @Test
    @Disabled("Test is failing") //TODO: Fix me
    public void shouldFailIfUrlIsNotProvided() throws Exception {
        Mockito.when(request.getParameter("url")).thenReturn(null);
        proxy.processResultDocument(new Object[]{}, request, null);
    }
    
    @Test
    public void shouldReturnAnXmlDocumentFromTheUrlProvidedAsAnExtensionParameter() throws Exception {
        Mockito.when(request.getParameter("url")).thenReturn(null);
        Object[] eParams = new Object[]{"http://news.google.com/?output=atom"};
        Document doc = proxy.processResultDocument(eParams, request, null);
        assertNotNull(doc);
        assertEquals("feed",doc.getDocumentElement().getTagName());
    }
    
    @Test
    public void shouldReturnAnXmlDocumentFromTheUrlProvidedAsARequestParameter() throws Exception {
        Mockito.when(request.getParameter("url")).thenReturn("http://news.google.com/?output=atom");
        Object[] eParams = new Object[] {};
        Document doc = proxy.processResultDocument(eParams, request, null);
        assertNotNull(doc);
        assertEquals("feed",doc.getDocumentElement().getTagName());
    }
    
    @Test
    public void shouldReturnAnXmlDocumentFromTheGivenUrlWithTheRequestParametersAppendedToTheUrl() throws Exception {
        Mockito.when(request.getParameter("url")).thenReturn(null);
        Mockito.when(request.getParameter("output")).thenReturn("atom");
        Object[] eParams = new Object[]{"http://news.google.com/"};
        Document doc = proxy.processResultDocument(eParams, request, null);
        assertNotNull(doc);
        assertEquals("feed",doc.getDocumentElement().getTagName());
    }
    
    // Helper methods are no longer needed with Mockito
}
