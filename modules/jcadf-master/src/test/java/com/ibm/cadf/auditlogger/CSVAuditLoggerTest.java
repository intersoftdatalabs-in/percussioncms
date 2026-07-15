/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.ibm.cadf.auditlogger;

import com.ibm.cadf.EventFactory;
import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.model.CADFType;
import com.ibm.cadf.model.Event;
import com.ibm.cadf.model.Identifier;
import com.ibm.cadf.model.Measurement;
import com.ibm.cadf.model.Metric;
import com.ibm.cadf.model.Resource;
import com.ibm.cadf.util.Constants;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CSVAuditLoggerTest {

  @BeforeEach
  public void setUp() {
    System.setProperty(Constants.API_AUDIT_MAP, "/com/ibm/cadf/cfg/api_audit_map.conf");
  }

  @Test
  public void testCSVAuditing()
      throws CADFException, IOException, com.opencsv.exceptions.CsvException {

    File file = new File("target/" + Constants.CSV_AUDIT_FILES_NAME);
    if (file.exists()) {
      file.delete();
    }

    AuditLogger auditLogger = AuditLoggerFactory.getAuditLogger(Constants.AUDIT_FORMAT_TYPE_CSV);
    auditLogger.setOutputFilePath("target/" + Constants.CSV_AUDIT_FILES_NAME);

    String initiatorId = Identifier.generateUniqueId();
    Resource initiator = new Resource(initiatorId);
    initiator.setTypeURI("/testcase");
    initiator.setName("AuditLoggerTest");

    String targetId = Identifier.generateUniqueId();
    Resource target = new Resource(targetId);
    target.setTypeURI("/configurator");
    target.setName("Configuration Component");

    String observerId = Identifier.generateUniqueId();
    Resource observer = new Resource(observerId);
    observer.setTypeURI("/management");
    observer.setName("Management Component");

    // Reason reason = new Reason("File transfer", "10101", null, null);

    Event event =
        EventFactory.getEventInstance(
            CADFType.EVENTTYPE.EVENTTYPE_ACTIVITY.name(),
            Identifier.generateUniqueId(),
            "Send File",
            "successful",
            initiator,
            null,
            target,
            null,
            observer,
            null);

    String metricId = Identifier.generateUniqueId();
    Metric metric1 = new Metric(metricId, "size", "MB");
    Measurement measurement1 = new Measurement("FileData", metric1, null);
    Measurement measurement2 = new Measurement("FileData", metric1, null);

    event.addMeasurement(measurement1);
    event.addMeasurement(measurement2);
    auditLogger.audit(event);

    Assertions.assertTrue(true);

    file = new File("target/" + Constants.CSV_AUDIT_FILES_NAME);

    if (file.exists()) {

      // create CSVReader object
      CSVReader reader = new CSVReader(new FileReader("target/" + Constants.CSV_AUDIT_FILES_NAME));

      // read all lines at once
      List<String[]> records = reader.readAll();

      Iterator<String[]> iterator = records.iterator();
      // header row
      String[] headerRecord = iterator.next();

      Assertions.assertEquals("Id", headerRecord[0]);
      Assertions.assertEquals("Timestamp", headerRecord[1]);
      Assertions.assertEquals("Action", headerRecord[2]);
      Assertions.assertEquals("Observer", headerRecord[3]);
      Assertions.assertEquals("Initiator", headerRecord[4]);
      Assertions.assertEquals("Target", headerRecord[5]);
      Assertions.assertEquals("Outcome", headerRecord[6]);
      Assertions.assertEquals("<Measurements>", headerRecord[7]);

      // audit row
      String[] auditRecord = iterator.next();
      Assertions.assertEquals("Send File", auditRecord[2]);
      Assertions.assertEquals("Management Component", auditRecord[3]);
      Assertions.assertEquals("AuditLoggerTest", auditRecord[4]);
      Assertions.assertEquals("Configuration Component", auditRecord[5]);
      Assertions.assertEquals("successful", auditRecord[6]);
      Assertions.assertEquals(
          "<"
              + metric1.getMetricId()
              + " - "
              + metric1.getName()
              + " "
              + measurement1.getResult()
              + " : "
              + metric1.getMetricId()
              + " - "
              + metric1.getName()
              + " "
              + measurement2.getResult()
              + " : >",
          auditRecord[7]);
      reader.close();
    } else {
      Assertions.fail();
    }
  }
}
