// REFACTORED: CP-JAVA11
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
package com.percussion.webui;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.share.test.PSMatchers;
import com.percussion.share.test.PSRestTestCase;
import org.apache.commons.httpclient.HttpException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test methods in this class query the main pages from a running server and
 * validate certain properties such as XML well-formedness and no tabs.
 * <p>
 * This is in the sitemanage project because the webui project is not set up to
 * support Java code. For that reason, base classes from the sitemanage project are not used.
 * <p>
 * There is one test for each primary page in the application.
 *
 * TODO Use {@link PSMatchers#validXhtml()}
 */
@Disabled
public class PSPageValidatorTest extends PSRestTestCase<PSPageValidatorRestClient> {

    private static PSPageValidatorRestClient restClient;

    /**
     * Creates a connection and logs in using an indirect technique.
     */
    @BeforeEach All
    public static void setUp() throws Exception {
        restClient = new PSPageValidatorRestClient(baseUrl);
        setupClient(restClient);
    }

    @Override
    protected PSPageValidatorRestClient getRestClient(String baseUrl) {
        return restClient;
    }

    @AfterAll
    public static void tearDown() {
        // No-op, placeholder for future cleanup if needed.
    }

    @Test
    public void testEditorPage() throws Exception {
        validatePage("editor");
    }

    @Test
    public void testDesignPage() throws Exception {
        validatePage("design");
    }

    @Test
    public void testSiteArchPage() throws Exception {
        validatePage("arch");
    }

    @Disabled
    public void testPublishPage() throws Exception {
        validatePage("publish");
    }

    @Test
    public void testUserMgtPage() throws Exception {
        validatePage("users");
    }

    /**
     * Requests the page using the supplied method, gets the response body and
     * attempts to parse it into a w3c DOM document to check for
     * well-formedness. It removes the leading DOCTYPE and replaces it with an
     * internal DOCTYPE that includes the nbsp entity.
     *
     * @param viewName the value for the view parameter supplied in the page request URL.
     */
    private void validatePage(String viewName)
            throws HttpException, IOException, ParserConfigurationException, SAXException {
        var src = restClient.getPage(viewName);

        src = fixupDoctype(src);

        var factory = PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
                new PSXmlSecurityOptions(
                        true,
                        true,
                        true,
                        false,
                        true,
                        false
                )
        );
        factory.setValidating(false);

        var parser = factory.newDocumentBuilder();
        var is = new InputSource(new StringReader(src));
        parser.parse(is);

        assertTrue(src.indexOf('\t') < 0, "File contains one or more tabs.");
    }

    /**
     * Modifies the DOCTYPE so it points to a local copy of the transitional DTD
     * rather than on the web.
     *
     * @param src the original HTML source string.
     * @return The supplied string with everything up to the opening html tag
     * replaced with the internal DOCTYPE.
     */
    private String fixupDoctype(String src) {
        return src.replace("http://www.w3.org/TR/xhtml1/DTD", "src/test/java/com/percussion/webui");
    }
}
