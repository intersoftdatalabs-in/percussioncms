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
package com.percussion.cms.objectstore.server;

import com.percussion.cms.objectstore.IPSFieldValue;
import com.percussion.cms.objectstore.PSDateValue;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSTextValue;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.filestorage.IPSFileStorageService;
import com.percussion.services.filestorage.PSFileStorageServiceLocator;
import com.percussion.utils.request.PSRequestInfo;

import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test use of the {@link PSBinaryFileValue} class.
 */
@Tag("IntegrationTest")
public class PSBinaryFileValueTest {

    /**
     * Test saving a file item to ensure info fields are set by the extractor.
     *
     * @throws Exception If the test fails or there are any errors.
     */
    @Test
    public void testFileUpload() throws Exception {
        var currentRelativePath = Paths.get("");
        var s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current relative path is: " + s);

        // login to set community etc
        var secWs = PSSecurityWsLocator.getSecurityWebservice();
        secWs.login(request, response, "admin1", "demo", null, "Enterprise_Investments", null);
        var psReq = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
        var tok = psReq.getSecurityToken();

        // create the item
        var itemDef = PSItemDefManager.getInstance().getItemDef("rffFile", tok);
        var item = new PSServerItem(itemDef, null, tok);
        var fields = item.getAllFields();
        var fileFieldName = "item_file_attachment";
        while (fields.hasNext()) {
            var field = fields.next();
            var name = field.getName();

            if (name.equals("sys_title") || name.equals("displaytitle")) {
                field.addValue(new PSTextValue("testFile"));
            } else if (name.equals("sys_contentstartdate")) {
                field.addValue(new PSDateValue(new Date()));
            } else if (name.equals(fileFieldName + "_hash")) {
                // This is no longer to a real field "item_file_attachment"
                // the content item will store item_file_attachement_hash value
                // and store the file in the binary store

                var storage = PSFileStorageServiceLocator.getFileStorageService();
                var hash = storage.store(new File(PSServer.getRxDir(), "rx_resources/images/boxcheck.gif"));
                field.addValue(new PSTextValue(hash));
            } else if (name.equals(fileFieldName + "_filename")) {
                field.addValue(new PSTextValue("rx_resources/images/boxcheck.gif"));
            }
        }

        item.save(tok);

        // load and ensure fields are set
        item = PSServerItem.loadItem(new PSLocator(item.getContentId(), item.getRevision()), tok);

        fields = item.getAllFields();
        while (fields.hasNext()) {
            var field = fields.next();
            var name = field.getName();
            if (name.startsWith(fileFieldName + "_")) {
                var val = field.getValue();
                assertNotNull(val);
                if (val instanceof PSTextValue) {
                    assertFalse(StringUtils.isBlank((String) ((PSTextValue) val).getValue()));
                }
            }
        }
    }
}
