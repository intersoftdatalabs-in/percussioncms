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
package com.percussion.test.util;

import static org.junit.Assert.assertEquals;

import com.percussion.server.PSServer;
import com.percussion.testing.PSMockRequestContext;
import com.percussion.util.PSUrlUtils;

import java.net.URL;
import java.util.LinkedHashMap;

import org.junit.Ignore;
import org.junit.Test;

public class PSUrlUtilsTest
{

    @Test
    //TODO: Fix me!
    @Ignore()
    public void testCreateUrls() throws Exception
    {
        String base = "test/base.htm?par1=val1&par2=val2#anchor";
        String anchor = "a";
        LinkedHashMap map = new LinkedHashMap(16,1,false);

        map.put("p=1", "v:+1");
        map.put("p2", "v2");
        map.put("p3", null);
        map.put(null, "v1");
        String createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);

        assertEquals("test/base.htm?par1=val1&par2=val2&p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        anchor = "";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("test/base.htm?par1=val1&par2=val2&p3=&p2=v2&p%3D1=v%3A%2B1", createdURL);

        anchor = null;
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("test/base.htm?par1=val1&par2=val2&p3=&p2=v2&p%3D1=v%3A%2B1#anchor", createdURL);

        base = "test/base.htm?par1=val1&par2=val2";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("test/base.htm?par1=val1&par2=val2&p3=&p2=v2&p%3D1=v%3A%2B1", createdURL);

        base = "test/base.htm";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1", createdURL);

        anchor = "a";
        base = "test/base.htm?par1=val1&par2=val2";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("test/base.htm?par1=val1&par2=val2&p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "test/base.htm";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "http://server:9992/te&t/base.htm";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("http://server:9992/te%26t/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "http://server/te=t/base.htm";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("http://server/te%3Dt/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "file:test/base.htm";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("file:test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "file:/test/base.htm";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("file:/test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "file:/test/base.htm?";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("file:/test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "file:/test/base.htm#";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("file:/test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

        base = "file:/test/base.htm#anchorText";
        createdURL = PSUrlUtils.createUrl(base, map.entrySet().iterator(), anchor);
        assertEquals("file:/test/base.htm?p3=&p2=v2&p%3D1=v%3A%2B1#a", createdURL);

    }
    
    @Test
    public void testSSLProtocol() throws Exception
    {
        String anchor = "a";
        LinkedHashMap map = new LinkedHashMap(0,1,false);
        map.put("p2", "v2");

        PSServer.ms_sslListenerPort = 9991;
        PSServer.ms_listenerPort = 9992;
        
        PSMockRequestContext reqContextMock = new PSMockRequestContext();
        URL createdURL = PSUrlUtils.createUrl("server", new Integer("9990"), "test/base.htm", map.entrySet().iterator(), anchor, reqContextMock, true);
        assertEquals("http://server:9990/test/base.htm?p2=v2#a", createdURL.toString());
        
        createdURL = PSUrlUtils.createUrl("server", new Integer("9989"), "test/base.htm", map.entrySet().iterator(), anchor, reqContextMock, true);
        assertEquals("http://server:9989/test/base.htm?p2=v2#a", createdURL.toString());
            
        reqContextMock = new PSMockRequestContext();
        reqContextMock.setOriginalPort(9992);
        reqContextMock.setOriginalProtocol("http");
        
        createdURL = PSUrlUtils.createUrl(null, null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMock, true);
        assertEquals("http://originalhost:9992/test/base.htm?p2=v2#a", createdURL.toString());
        
        createdURL = PSUrlUtils.createUrl(null, null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMock, false);
        assertEquals("http://originalhost:9992/test/base.htm?p2=v2#a", createdURL.toString());
        
        createdURL = PSUrlUtils.createUrl("server", null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMock, true);
        
        assertEquals("http://server:9992/test/base.htm?p2=v2#a", createdURL.toString());
        
        PSMockRequestContext reqContextMockSSL = new PSMockRequestContext();
        reqContextMockSSL.setOriginalPort(9991);
        reqContextMockSSL.setOriginalProtocol("https");

        createdURL = PSUrlUtils.createUrl(null, null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMockSSL, true);
        assertEquals("https://originalhost:9991/test/base.htm?p2=v2#a", createdURL.toString());
        
        createdURL = PSUrlUtils.createUrl(null, null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMockSSL, false);
        assertEquals("http://originalhost:9992/test/base.htm?p2=v2#a", createdURL.toString());
        
        createdURL = PSUrlUtils.createUrl("server", null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMockSSL, true);
        assertEquals("https://server:9991/test/base.htm?p2=v2#a", createdURL.toString());

        createdURL = PSUrlUtils.createUrl("server", null, "test/base.htm", map.entrySet().iterator(), anchor, reqContextMockSSL, false);
        assertEquals("http://server:9992/test/base.htm?p2=v2#a", createdURL.toString());
    }
}
