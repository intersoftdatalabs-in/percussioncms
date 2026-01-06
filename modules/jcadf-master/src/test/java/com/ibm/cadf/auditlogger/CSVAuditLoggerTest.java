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
<<<<<<< HEAD
import com.opencsv.CSVReader;
=======
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
>>>>>>> development-8.1.x
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
<<<<<<< HEAD
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CSVAuditLoggerTest {

  @BeforeEach
=======
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CSVAuditLoggerTest {

  @Before
>>>>>>> development-8.1.x
  public void setUp() {
    System.setProperty(Constants.API_AUDIT_MAP, "/com/ibm/cadf/cfg/api_audit_map.conf");
  }

  @Test
<<<<<<< HEAD
  public void testCSVAuditing()
      throws CADFException, IOException, com.opencsv.exceptions.CsvException {
=======
  public void testCSVAuditing() throws CADFException, IOException, CsvException {
>>>>>>> development-8.1.x

    File file = new File(Constants.CSV_AUDIT_FILES_NAME);
    if (file.exists()) {
      file.delete();
    }

    AuditLogger auditLogger = AuditLoggerFactory.getAuditLogger(Constants.AUDIT_FORMAT_TYPE_CSV);

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

<<<<<<< HEAD
    Assertions.assertTrue(true);
=======
    Assert.assertTrue(true);
>>>>>>> development-8.1.x

    file = new File(Constants.CSV_AUDIT_FILES_NAME);

    if (file.exists()) {

      // create CSVReader object
<<<<<<< HEAD
      CSVReader reader = new CSVReader(new FileReader(Constants.CSV_AUDIT_FILES_NAME));
=======
      CSVReader reader =
          new CSVReaderBuilder(new FileReader(Constants.CSV_AUDIT_FILES_NAME))
              .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
              .build();
>>>>>>> development-8.1.x

      // read all lines at once
      List<String[]> records = reader.readAll();

      Iterator<String[]> iterator = records.iterator();
      // header row
      String[] headerRecord = iterator.next();

<<<<<<< HEAD
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
=======
      Assert.assertEquals("Id", headerRecord[0]);
      Assert.assertEquals("Timestamp", headerRecord[1]);
      Assert.assertEquals("Action", headerRecord[2]);
      Assert.assertEquals("Observer", headerRecord[3]);
      Assert.assertEquals("Initiator", headerRecord[4]);
      Assert.assertEquals("Target", headerRecord[5]);
      Assert.assertEquals("Outcome", headerRecord[6]);
      Assert.assertEquals("<Measurements>", headerRecord[7]);

      // audit row
      String[] auditRecord = iterator.next();
      Assert.assertEquals("Send File", auditRecord[2]);
      Assert.assertEquals("Management Component", auditRecord[3]);
      Assert.assertEquals("AuditLoggerTest", auditRecord[4]);
      Assert.assertEquals("Configuration Component", auditRecord[5]);
      Assert.assertEquals("successful", auditRecord[6]);
      Assert.assertEquals(
>>>>>>> development-8.1.x
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
<<<<<<< HEAD
      Assertions.fail();
    }
  }
=======
      Assert.fail();
    }
  }

  @AfterClass
  public static void clean() {
    File auditFile = new File(Constants.CSV_AUDIT_FILES_NAME);
    // auditFile.delete();
  }
>>>>>>> development-8.1.x
}
