/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.server.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.JobErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.error.IPSErrorCode;
import com.percussion.server.PSRequest;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #4143 (parent #2616 leftover): {@code com.percussion.server.job} production throw sites use
 * typed {@link JobErrorCodes} via IPSErrorCode-aware {@link PSJobException} constructors — not
 * bare {@code IPSJobErrors} ints. Job catalog codes are non-auditable and skip dual-write.
 */
@Tag("UnitTest")
public class PSJobHandlerTypedErrorCodeSliceTest {

  @BeforeEach
  void setUp() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @AfterEach
  void tearDown() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @Test
  public void leftoverJobCodesMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSJobErrors.JOB_DEFINITION_NOT_FOUND,
        JobErrorCodes.JOB_DEFINITION_NOT_FOUND.numericCode());
    assertEquals(IPSJobErrors.FACTORY_GET_RUNNER, JobErrorCodes.FACTORY_GET_RUNNER.numericCode());
    assertEquals(
        IPSJobErrors.INVALID_REQUEST_TYPE, JobErrorCodes.INVALID_REQUEST_TYPE.numericCode());
    assertEquals(IPSJobErrors.UNEXPECTED_ERROR, JobErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertEquals(IPSJobErrors.NULL_INPUT_DOC, JobErrorCodes.NULL_INPUT_DOC.numericCode());
    assertEquals(
        IPSJobErrors.SERVER_REQUEST_PARAM_INVALID,
        JobErrorCodes.SERVER_REQUEST_PARAM_INVALID.numericCode());
    assertEquals(
        IPSJobErrors.JOB_ALREADY_RUNNING, JobErrorCodes.JOB_ALREADY_RUNNING.numericCode());
    assertEquals(
        IPSJobErrors.SERVER_REQUEST_MALFORMED,
        JobErrorCodes.SERVER_REQUEST_MALFORMED.numericCode());
    assertEquals(IPSJobErrors.INVALID_JOB_ID, JobErrorCodes.INVALID_JOB_ID.numericCode());
    assertEquals(
        IPSJobErrors.INVALID_JOB_DESCRIPTOR,
        JobErrorCodes.INVALID_JOB_DESCRIPTOR.numericCode());
    assertEquals(
        IPSJobErrors.CONFIG_FILE_NOT_FOUND, JobErrorCodes.CONFIG_FILE_NOT_FOUND.numericCode());

    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.JOB_DEFINITION_NOT_FOUND, new Object[] {"cat", "type"}),
        JobErrorCodes.JOB_DEFINITION_NOT_FOUND);
    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.FACTORY_GET_RUNNER, new Object[] {"cls", "err"}),
        JobErrorCodes.FACTORY_GET_RUNNER);
    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.INVALID_REQUEST_TYPE, "job-foo"),
        JobErrorCodes.INVALID_REQUEST_TYPE);
    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.UNEXPECTED_ERROR, "boom"),
        JobErrorCodes.UNEXPECTED_ERROR);
    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.NULL_INPUT_DOC), JobErrorCodes.NULL_INPUT_DOC);
    leftoverNonAuditable(
        new PSJobException(
            JobErrorCodes.SERVER_REQUEST_PARAM_INVALID, new Object[] {"sys_jobType", "null"}),
        JobErrorCodes.SERVER_REQUEST_PARAM_INVALID);
    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.JOB_ALREADY_RUNNING), JobErrorCodes.JOB_ALREADY_RUNNING);
    leftoverNonAuditable(
        new PSJobException(
            JobErrorCodes.SERVER_REQUEST_MALFORMED, new Object[] {"PSXJobGetStatus", "bad"}),
        JobErrorCodes.SERVER_REQUEST_MALFORMED);
    leftoverNonAuditable(
        new PSJobException(JobErrorCodes.INVALID_JOB_ID, "99"), JobErrorCodes.INVALID_JOB_ID);

    for (JobErrorCodes code : JobErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      AuditLogId id = DefaultAuditLogService.Holder.get().log(code, AuditContext.empty());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.name());
    }
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  public void missingJobDefinitionThrowsTypedNonAuditableCode() throws Exception {
    PSJobHandlerConfiguration cfg = configWithJob("cat1", "jt1", "com.example.JobRunner");
    PSJobException missingClass =
        assertThrows(PSJobException.class, () -> cfg.getJobClassName("cat1", "missing"));
    leftoverNonAuditable(missingClass, JobErrorCodes.JOB_DEFINITION_NOT_FOUND);

    PSJobException missingParams =
        assertThrows(PSJobException.class, () -> cfg.getJobInitParams("nope", "jt1"));
    leftoverNonAuditable(missingParams, JobErrorCodes.JOB_DEFINITION_NOT_FOUND);
  }

  @Test
  public void factoryMissingRunnerClassThrowsTypedNonAuditableCode() throws Exception {
    PSJobHandlerConfiguration cfg =
        configWithJob("cat1", "jt1", "com.percussion.server.job.DoesNotExistJobRunner");
    PSJobRunnerFactory factory = new PSJobRunnerFactory(cfg);
    PSJobException ex = assertThrows(PSJobException.class, () -> factory.getJobRunner("cat1", "jt1"));
    leftoverNonAuditable(ex, JobErrorCodes.FACTORY_GET_RUNNER);
  }

  @Test
  public void runJobMissingParamsThrowsTypedNonAuditableCode() throws Exception {
    PSJobHandler handler = new PSJobHandler();
    Document inDoc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(inDoc, "PSXJobRun");
    PSRequest req = new PSRequest(null, null, null, null);

    PSJobException missingCategory =
        assertThrows(PSJobException.class, () -> handler.runJob(inDoc, req));
    leftoverNonAuditable(missingCategory, JobErrorCodes.SERVER_REQUEST_PARAM_INVALID);

    req.setParameter("sys_jobCategory", "deployer");
    PSJobException missingType =
        assertThrows(PSJobException.class, () -> handler.runJob(inDoc, req));
    leftoverNonAuditable(missingType, JobErrorCodes.SERVER_REQUEST_PARAM_INVALID);
  }

  @Test
  public void getStatusAndCancelThrowTypedCodesForMalformedAndUnknownJobId() throws Exception {
    PSJobHandler handler = new PSJobHandler();
    PSRequest req = new PSRequest(null, null, null, null);

    PSJobException malformedStatus =
        assertThrows(
            PSJobException.class, () -> handler.getJobStatus(jobIdDoc("PSXJobGetStatus", "x"), req));
    leftoverNonAuditable(malformedStatus, JobErrorCodes.SERVER_REQUEST_MALFORMED);

    PSJobException unknownStatus =
        assertThrows(
            PSJobException.class, () -> handler.getJobStatus(jobIdDoc("PSXJobGetStatus", "7"), req));
    leftoverNonAuditable(unknownStatus, JobErrorCodes.INVALID_JOB_ID);

    PSJobException malformedCancel =
        assertThrows(
            PSJobException.class, () -> handler.cancelJob(jobIdDoc("PSXJobCancel", "bad"), req));
    leftoverNonAuditable(malformedCancel, JobErrorCodes.SERVER_REQUEST_MALFORMED);

    PSJobException unknownCancel =
        assertThrows(
            PSJobException.class, () -> handler.cancelJob(jobIdDoc("PSXJobCancel", "8"), req));
    leftoverNonAuditable(unknownCancel, JobErrorCodes.INVALID_JOB_ID);
  }

  @Test
  public void lockJobHandlerThrowsTypedAlreadyRunningWhenJobAlive() throws Exception {
    CountDownLatch hold = new CountDownLatch(1);
    ParkingJobRunner first = new ParkingJobRunner(hold);
    first.start();
    assertTrue(first.awaitStarted(2, TimeUnit.SECONDS));
    try {
      PSJobHandler handler = new PSJobHandler();
      Method lock = PSJobHandler.class.getDeclaredMethod("lockJobHandler", PSJobRunner.class);
      lock.setAccessible(true);
      lock.invoke(handler, first);

      ParkingJobRunner second = new ParkingJobRunner(new CountDownLatch(1));
      InvocationTargetException wrapped =
          assertThrows(InvocationTargetException.class, () -> lock.invoke(handler, second));
      assertTrue(wrapped.getCause() instanceof PSJobException);
      leftoverNonAuditable((PSJobException) wrapped.getCause(), JobErrorCodes.JOB_ALREADY_RUNNING);
    } finally {
      hold.countDown();
      first.interrupt();
      first.join(2000);
    }
  }

  @Test
  public void typedConstructorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSJobException((IPSErrorCode) null));
  }

  private static void leftoverNonAuditable(PSJobException ex, JobErrorCodes expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }

  private static Document jobIdDoc(String rootName, String id) {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, rootName);
    root.setAttribute("id", id);
    return doc;
  }

  private static PSJobHandlerConfiguration configWithJob(
      String category, String jobType, String className) throws Exception {
    String xml =
        """
        <PSXJobHandlerConfiguration>
          <InitParams>
            <InitParam name="h1" value="v1"/>
          </InitParams>
          <Categories>
            <Category name="%s">
              <InitParams/>
              <Jobs>
                <Job jobType="%s" className="%s">
                  <InitParams/>
                </Job>
              </Jobs>
            </Category>
          </Categories>
        </PSXJobHandlerConfiguration>
        """
            .formatted(category, jobType, className);
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
    return new PSJobHandlerConfiguration(doc);
  }

  /** Job runner that stays alive until the hold latch is released. */
  private static final class ParkingJobRunner extends PSJobRunner {
    private final CountDownLatch hold;
    private final CountDownLatch started = new CountDownLatch(1);

    ParkingJobRunner(CountDownLatch hold) {
      this.hold = hold;
    }

    @Override
    public void init(int id, Document descriptor, PSRequest req, Properties initParams) {
      m_id = id;
    }

    @Override
    public void doRun() {
      // unused — run() is overridden to avoid PSRequestInfo init
    }

    @Override
    public void run() {
      started.countDown();
      try {
        hold.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
      return started.await(timeout, unit);
    }
  }
}
