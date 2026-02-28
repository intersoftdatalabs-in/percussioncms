/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package test.percussion.soln.p13n.tracking.web;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.percussion.soln.p13n.tracking.IVisitorTrackingService;
import com.percussion.soln.p13n.tracking.VisitorTrackingActionRequest;
import com.percussion.soln.p13n.tracking.VisitorTrackingResponse;
import com.percussion.soln.p13n.tracking.web.TrackRestClient;
import com.percussion.soln.p13n.tracking.web.VisitorTrackingWebUtils;




public class TrackRestClientTest {

    protected static final int PORT = 9990;
    protected TrackRestClient client = new TrackRestClient();
    protected VisitorTrackingResponse response;
    protected VisitorTrackingActionRequest tr;
    protected MockHttpServletRequest servletRequest;
    protected MockHttpServletResponse servletResponse;
    protected TestWebServer tw = new TestWebServer(PORT);

    @BeforeEach
    public void setUp() throws Exception {
        client.setTrackingURI("http://localhost:" + PORT + "/soln-p13n/track/track");
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        response = new VisitorTrackingResponse();
        response.setStatus(IVisitorTrackingService.ResponseStatus.OK.name());
        setReponse(response);
        tw.start();
        client.setTimeOut(30000);
        tr = new VisitorTrackingActionRequest();
        tr.setActionName("login");
        tr.setUserId("test");
        Map<String, Integer> w = new HashMap<String, Integer>();
        w.put("a", 100);
        tr.setSegmentWeights(w);
        tr.setVisitorProfileId(100);
    }

    protected void setReponse(VisitorTrackingResponse response) {
        tw.setResponse(VisitorTrackingWebUtils.responseToJson(response));
    }


    @Test
    public void testRestClientWithCookie() throws Exception {
        assertEquals(4, new char[4].length);

        Cookie c = new Cookie(VisitorTrackingWebUtils.VISITOR_PROFILE_ID_COOKIE_NAME, ""+100);
        servletRequest.setCookies(new Cookie[] {c});

        client.clientRequest(servletRequest, servletResponse, tr);
        String body = tw.getRequestBody();
        assertRequestBody(
                "GET /soln-p13n/track/track" +
                "?address=127.0.0.1&hostname=localhost&locale=en&userId=test&visitorProfileId=100&actionName=login&segmentWeights%5Ba%5D=100" +
                " HTTP/1.1\n" +
        "User-Agent: Percussion-TrackRestClient/1.0\n" +
        		"Host: localhost:9990\n" +
        "Cookie: visitorProfileId=100\n",
        		body);
    }

    public void assertRequestBody(String expected, String actual) {
        assertEquals(expected.replaceAll("\n", ""), actual.replaceAll("\r\n", ""));
    }

    public void assertRequestBody(String expected) {
        assertRequestBody(expected, tw.getRequestBody());
    }

    @AfterEach
    public void tearDown() throws Exception {
        tw.stop();
    }

}
