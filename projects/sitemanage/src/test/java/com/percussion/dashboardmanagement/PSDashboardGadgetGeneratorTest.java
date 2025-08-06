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

package com.percussion.dashboardmanagement;

import com.percussion.dashboardmanagement.data.DashboardContent;
import com.percussion.dashboardmanagement.data.DashboardContent.GadgetDef;
import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.metadata.web.service.PSMetadataServiceRestClient;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("If you want to run these unit tests, adjust the SERVER_URL constant and start your CMS server.")
public class PSDashboardGadgetGeneratorTest {

    private static final String SERVER_URL = "http://localhost:9982";
    private static final String USERNAME = "Admin";
    private static final String PASSWORD = "demo";

    private static final String VALID_XML_FILE = "data/validGadgetsXml.xml";
    private static final String VALID_XML_FILE2 = "data/validGadgetsXml2.xml";
    private static final String INVALID_XML_FILE = "data/invalidGadgetsXml.xml";
    private static final String VALID_XML_FILE_WITH_ALL_GADGETS = "data/validGadgetsXmlWithAllGadgets.xml";

    private static final String GADGET_REPOSITORY_PATH = "/cm/gadgets/repository/";

    private PSMetadataServiceRestClient restClient;

    private String getGadgetUrl(String gadgetType) {
        if (!gadgetType.contains("/"))
            return GADGET_REPOSITORY_PATH + gadgetType + "/" + gadgetType + ".xml";
        else
            return GADGET_REPOSITORY_PATH + gadgetType;
    }

    private InputStream getDataFile(String filePath) throws FileNotFoundException {
        return getClass().getResourceAsStream(filePath);
    }

    @BeforeEach
    public void setUp() {
        restClient = new PSMetadataServiceRestClient(SERVER_URL);
        restClient.login(USERNAME, PASSWORD);
        restClient.removeAllGadgets();
    }

    @Test
    public void testGadgetGenerator_LoadsXmlFile_XmlIsValid() throws Exception {
        var xmlFile = getDataFile(VALID_XML_FILE);
        var contentGenerator = new PSDashboardGadgetGenerator(SERVER_URL, xmlFile, USERNAME, PASSWORD);
        contentGenerator.cleanup();

        assertTrue(contentGenerator.dataSuccessfullyLoaded(), "data is valid");

        var dashboardContent = contentGenerator.getRootData();
        assertNotNull(dashboardContent, "dashboard content not null");
        assertEquals(3, dashboardContent.getGadgetDef().size(), "gadget count");

        int count = 0;

        for (var gad : dashboardContent.getGadgetDef()) {
            if (gad.getGadgetType().equals("perc_comments_gadget")) {
                count += 7;
                assertEquals(1, gad.getColumn(), "comments gadget - column");
                assertFalse(gad.isExpanded(), "comments gadget - extended");

                assertEquals(2, gad.getUserPref().size(), "comments gadget - settings count");
                assertEquals("site", gad.getUserPref().get(0).getName(), "comments gadget - setting 1 - name");
                assertEquals("Site1", gad.getUserPref().get(0).getValue(), "comments gadget - setting 1 - value");

                assertEquals("zrows", gad.getUserPref().get(1).getName(), "comments gadget - setting 2 - name");
                assertEquals("5", gad.getUserPref().get(1).getValue(), "comments gadget - setting 2 - value");
            } else if (gad.getGadgetType().equals("cm1_welcome_gadget/perc_welcome_gadget.xml")) {
                count += 11;
                assertEquals(0, gad.getColumn(), "welcome gadget - column");
                assertTrue(gad.isExpanded(), "welcome gadget - extended");

                assertEquals(0, gad.getUserPref().size(), "welcome gadget - settings count");
            } else if (gad.getGadgetType().equals("perc_workflow_status_gadget")) {
                count += 13;
                assertEquals(1, gad.getColumn(), "workflow gadget - column");
                assertFalse(gad.isExpanded(), "workflow gadget - extended");

                assertEquals(3, gad.getUserPref().size(), "workflow gadget - settings count");
                assertEquals("site", gad.getUserPref().get(0).getName(), "workflow gadget - setting 1 - name");
                assertEquals("@all", gad.getUserPref().get(0).getValue(), "workflow gadget - setting 1 - value");

                assertEquals("status", gad.getUserPref().get(1).getName(), "workflow gadget - setting 2 - name");
                assertEquals("Pending", gad.getUserPref().get(1).getValue(), "workflow gadget - setting 2 - value");

                assertEquals("zrows", gad.getUserPref().get(2).getName(), "workflow gadget - setting 3 - name");
                assertEquals("5", gad.getUserPref().get(2).getValue(), "workflow gadget - setting 3 - value");
            } else {
                fail("gadget not expected");
            }
        }

        assertEquals(31, count, "all gadgets loaded");
    }

    @Test
    public void testGadgetGenerator_LoadsXmlFile_XmlIsInvalid() throws Exception {
        var xmlFile = getDataFile(INVALID_XML_FILE);
        var contentGenerator = new PSDashboardGadgetGenerator(SERVER_URL, xmlFile, USERNAME, PASSWORD);
        contentGenerator.cleanup();

        assertFalse(contentGenerator.dataSuccessfullyLoaded(), "data is valid");
    }

    @Test
    public void testGadgetGenerator_GenerateGadgets() throws Exception {
        var xmlFile = getDataFile(VALID_XML_FILE);
        var contentGenerator = new PSDashboardGadgetGenerator(SERVER_URL, xmlFile, USERNAME, PASSWORD);

        contentGenerator.generateContent();

        assertTrue(contentGenerator.dataSuccessfullyLoaded(), "data is valid");

        List<PSGadget> gadgets = restClient.getCurrentGadgets();
        assertEquals(3, gadgets.size(), "gadget count");

        int count = 0;

        for (var aGadget : gadgets) {
            if (aGadget.getUrl().equals(getGadgetUrl("perc_comments_gadget"))) {
                count += 7;

                assertNotNull(aGadget.getInstanceId(), "1 - instanceId not null");
                assertEquals(1, aGadget.getInstanceId(), "1 - instanceId");
                assertEquals(1, (int) aGadget.getCol(), "1 - column");
                assertEquals(0, (int) aGadget.getRow(), "1 - row");
                assertFalse(aGadget.isExpanded(), "1 - expanded");

                assertNotNull(aGadget.getSettings(), "1 - settings not null");
                assertEquals(2, aGadget.getSettings().size(), "1 - settings count");
                assertEquals("Site1", aGadget.getSettings().get("site"), "1 - site");
                assertEquals("5", aGadget.getSettings().get("zrows"), "1 - zrows");
            } else if (aGadget.getUrl().equals(getGadgetUrl("cm1_welcome_gadget/perc_welcome_gadget.xml"))) {
                count += 11;

                assertNotNull(aGadget.getInstanceId(), "2 - instanceId not null");
                assertEquals(2, aGadget.getInstanceId(), "2 - instanceId");
                assertEquals(0, (int) aGadget.getCol(), "2 - column");
                assertEquals(0, (int) aGadget.getRow(), "2 - row");
                assertTrue(aGadget.isExpanded(), "2 - expanded");

                assertNotNull(aGadget.getSettings(), "2 - settings not null");
                assertEquals(0, aGadget.getSettings().size(), "2 - settings count");
            } else if (aGadget.getUrl().equals(getGadgetUrl("perc_workflow_status_gadget"))) {
                count += 13;

                assertNotNull(aGadget.getInstanceId(), "3 - instanceId not null");
                assertEquals(3, aGadget.getInstanceId(), "3 - instanceId");
                assertEquals(1, (int) aGadget.getCol(), "3 - column");
                assertEquals(1, (int) aGadget.getRow(), "3 - row");
                assertFalse(aGadget.isExpanded(), "3 - expanded");

                assertNotNull(aGadget.getSettings(), "3 - settings not null");
                assertEquals(3, aGadget.getSettings().size(), "3 - settings count");
                assertEquals("@all", aGadget.getSettings().get("site"), "3 - site");
                assertEquals("5", aGadget.getSettings().get("zrows"), "3 - zrows");
                assertEquals("Pending", aGadget.getSettings().get("status"), "3 - status");
            } else {
                fail("Invalid gadget url");
            }
        }

        assertEquals(31, count, "count");
    }

    @Test
    public void testGadgetGenerator_GenerateAllGadgets() throws Exception {
        var xmlFile = getDataFile(VALID_XML_FILE_WITH_ALL_GADGETS);
        var contentGenerator = new PSDashboardGadgetGenerator(SERVER_URL, xmlFile, USERNAME, PASSWORD);

        contentGenerator.generateContent();

        assertTrue(contentGenerator.dataSuccessfullyLoaded(), "data is valid");

        List<PSGadget> gadgets = restClient.getCurrentGadgets();
        assertEquals(10, gadgets.size(), "gadget count");
    }

    @Test
    public void testGadgetGenerator_GenerateAllGadgets_UsingMainMethod() throws Exception {
        URL dataFile = getClass().getResource(VALID_XML_FILE_WITH_ALL_GADGETS);

        String[] args = new String[4];
        args[0] = SERVER_URL;
        args[1] = USERNAME;
        args[2] = PASSWORD;
        args[3] = dataFile.getFile();

        PSDashboardGadgetGenerator.main(args);

        List<PSGadget> gadgets = restClient.getCurrentGadgets();
        assertEquals(10, gadgets.size(), "gadget count");
    }

    @Test
    // TODO: Remove me @SuppressFBWarnings({"NP_NONNULL_PARAM_VIOLATION", "NP_NULL_PARAM_DEREF_NONVIRTUAL"})
    public void testGadgetGenerator_MainMethod_ArgumentsNull() {
        assertThrows(IllegalArgumentException.class, () -> PSDashboardGadgetGenerator.main(null));
    }

    @Test
    public void testGadgetGenerator_MainMethod_SomeArgumentsNotSpecified() {
        assertThrows(IllegalArgumentException.class, () -> PSDashboardGadgetGenerator.main(new String[2]));
    }

    @Test
    public void testGadgetGenerator_MainMethod_ServerURLIsEmptyOrNull() {
        assertThrows(IllegalArgumentException.class, () ->
                PSDashboardGadgetGenerator.main(new String[]{null, "username", "password", "filepath"}));
        assertThrows(IllegalArgumentException.class, () ->
                PSDashboardGadgetGenerator.main(new String[]{StringUtils.EMPTY, "username", "password", "filepath"}));
    }

    @Test
    public void testGadgetGenerator_MainMethod_XMLFileIsEmptyOrNull() {
        assertThrows(IllegalArgumentException.class, () ->
                PSDashboardGadgetGenerator.main(new String[]{"serverurl", "username", "password", null}));
        assertThrows(IllegalArgumentException.class, () ->
                PSDashboardGadgetGenerator.main(new String[]{"serverurl", "username", "password", StringUtils.EMPTY}));
    }

    @Test
    public void testGadgetGenerator_Cleanup() throws Exception {
        // Add some gadgets
        var xmlFile = getDataFile(VALID_XML_FILE);
        var contentGenerator = new PSDashboardGadgetGenerator(SERVER_URL, xmlFile, USERNAME, PASSWORD);

        contentGenerator.generateContent();

        List<PSGadget> gadgets = restClient.getCurrentGadgets();
        assertEquals(3, gadgets.size(), "gadget count");

        // Cleanup. Use a different file with different gadgets. Make sure
        // that the cleanup method is removing only the gadgets present
        // in the XML file.
        var xmlFile2 = getDataFile(VALID_XML_FILE2);
        var contentGenerator2 = new PSDashboardGadgetGenerator(SERVER_URL, xmlFile2, USERNAME, PASSWORD);
        contentGenerator2.cleanup();

        gadgets = restClient.getCurrentGadgets();
        assertEquals(1, gadgets.size(), "gadget count");

        assertEquals(getGadgetUrl("perc_comments_gadget"), gadgets.get(0).getUrl(), "gadget url");
    }
}
