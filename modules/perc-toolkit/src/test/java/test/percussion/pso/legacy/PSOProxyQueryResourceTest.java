/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.design.objectstore.IPSReplacementValue;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.pso.legacy.PSOProxyQueryResource;
import com.percussion.server.IPSRequestContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.w3c.dom.Document;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PSOProxyQueryResourceTest {
  // Mockito will manage mocks
  PSOProxyQueryResource proxy = new PSOProxyQueryResource();

  @BeforeEach
  public void setUp() throws Exception {
    proxy.init(makeExtensionDef("url"), null);
  }

  @Disabled("TODO: Fix Me")
  @Test
  public void shouldFailIfUrlIsNotProvided() throws Exception {
    IPSRequestContext request = makeRequest(makeRequestParams(null, null, null));
    assertThrows(IllegalArgumentException.class,
        () -> proxy.processResultDocument(new Object[] {}, request, null));
  }

  @Test
  public void shouldReturnAnXmlDocumentFromTheUrlProvidedAsAnExtensionParameter() throws Exception {
    IPSRequestContext request = makeRequest(makeRequestParams(null, null, null));
    Object[] eParams = makeParams("http://news.google.com/?output=atom");
    Document doc = proxy.processResultDocument(eParams, request, null);
    assertNotNull(doc);
    assertEquals("feed", doc.getDocumentElement().getTagName());
  }

  @Test
  public void shouldReturnAnXmlDocumentFromTheUrlProvidedAsARequestParameter() throws Exception {
    IPSRequestContext request =
        makeRequest(makeRequestParams("http://news.google.com/?output=atom", null, null));
    Object[] eParams = new Object[] {};
    Document doc = proxy.processResultDocument(eParams, request, null);
    assertNotNull(doc);
    assertEquals("feed", doc.getDocumentElement().getTagName());
  }

  @Test
  public void shouldReturnAnXmlDocumentFromTheGivenUrlWithTheRequestParametersAppendedToTheUrl()
      throws Exception {
    Map<String, String> params = makeRequestParams(null, null, null);
    params.put("output", "atom");
    IPSRequestContext request = makeRequest(params);
    Object[] eParams = makeParams("http://news.google.com/");
    Document doc = proxy.processResultDocument(eParams, request, null);
    assertNotNull(doc);
    assertEquals("feed", doc.getDocumentElement().getTagName());
  }

  public IPSExtensionDef makeExtensionDef(final String... names) {
    IPSExtensionDef extensionDef = mock(IPSExtensionDef.class);
    when(extensionDef.getRuntimeParameterNames()).thenReturn(java.util.Arrays.asList(names).iterator());
    return extensionDef;
  }

  public IPSRequestContext makeRequest(final Map<String, String> parameters) {
    IPSRequestContext request = mock(IPSRequestContext.class);
    for (Entry<String, String> entry : parameters.entrySet()) {
      when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
    }
    when(request.getParametersIterator()).thenReturn((java.util.Iterator) parameters.entrySet().iterator());
    return request;
  }

  public Map<String, String> makeRequestParams(String url, String user, String password) {
    Map<String, String> ps = new HashMap<String, String>();
    ps.put("url", url);
    ps.put("user", user);
    ps.put("password", password);
    return ps;
  }

  public Object[] makeParams(final String... params) throws Exception {
    Object[] rvalue = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      IPSReplacementValue irv = mock(IPSReplacementValue.class);
      when(irv.getValueText()).thenReturn(params[i]);
      rvalue[i] = irv;
    }
    return rvalue;
  }
}
