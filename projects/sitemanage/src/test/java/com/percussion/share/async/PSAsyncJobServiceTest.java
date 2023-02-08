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
package com.percussion.share.async;

import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.share.async.impl.PSAsyncJobService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author JaySeletz
 *
 */
public class PSAsyncJobServiceTest
{
    IPSAsyncJobService svc;
    private PSTestAsyncJob m_testJob;
    
    @Before
    public void setUp()
    {  
        svc = new PSAsyncJobService();
        PSAsyncJobService impl = (PSAsyncJobService)svc;
        impl.setAsyncJobFactory(new IPSAsyncJobFactory()
        {
            
            @Override
            public IPSAsyncJob getJob(String jobType)
            {
                m_testJob = new PSTestAsyncJob();
                return m_testJob;
            }
        });
    }
    
    @Test
    public void testJob() throws Exception
    {
        runJob(25);
    }
    
    @Test
    public void testMutlipleJobs() throws Exception
    {
        int increment1 = 25;
        int increment2 = 50;
        long jobId1 = svc.startJob("asyncJobTest", increment1);
        assertTrue(jobId1 > 0);
        
        long jobId2 = svc.startJob("asyncJobTest", increment2);
        assertTrue(jobId2 > 0);
        assertTrue("Multiple jobs with same jobid", jobId1 != jobId2);
        
        boolean allDone = false;
        boolean done1 = false;
        boolean done2 = false;
        int status1 = 1;
        int status2 = 1;
        while (!allDone)
        {
            if (!done1)
            {
                if (checkStatus(jobId1, status1) == 100)
                    done1 = true;
                else
                    status1 += increment1;
            }
            
            if (!done2)
            {
                if (checkStatus(jobId2, status2) == 100)
                    done2 = true;
                else
                    status2 += increment2;
            }
            


            
            if (status1 > 100)
                status1 = 100;
            if (status2 > 100)
                status2 = 100;
            
            if (done1 && done2)
            {
                allDone = true;
            }
        }

    }
    
    @Test
    public void testGetResult() throws Exception
    {
        int increment = 25;
        long jobId = runJob(increment);
        
        Object result = svc.getJobResult(jobId);
        assertNotNull(result);
        assertEquals(new Integer(25), result);
    }
    
    @Test
    public void testGroomingJobs() throws Exception
    {
        // run a job
        long jobId = runJob(50);

        // check status, expect to succeed
        assertNotNull("Job groomed early", svc.getJobStatus(jobId));
        assertNotNull("No result available", svc.getJobResult(jobId));
        
        // run another
        long newJobId = runJob(50);
        assertTrue("Job result", 50 == (Integer)svc.getJobResult(newJobId));

        assertTrue("Job not groomed", svc.getJobStatus(jobId).getStatus() == 100);
    }

    @Test
    public void testCancelJob() throws Exception
    {
        long jobId = svc.startJob("asyncJobTest", 5);
        assertTrue(jobId > 0);
        svc.cancelJob(jobId);
        assertTrue(m_testJob.isCancelled());
        assertEquals(m_testJob.CANCEL_MESSAGE, svc.getJobStatus(jobId).getMessage());        
    }
    
    @Test
    public void testCancelJobWithInterrupt() throws Exception
    {
        m_testJob = null;
        long jobId = svc.startJob("asyncJobTestInterrupt", 5);
        if(m_testJob != null){
        m_testJob.setUseInterrupt(true);
        assertTrue(jobId > 0);
        }else{
        	throw(new Exception("Job not initialized by Start Job!"));
        }
        // wait for job to start
        while(!m_testJob.isStarted())
        {
            Thread.sleep(5);
        }
        
        svc.cancelJob(jobId);
        assertTrue(m_testJob.isCancelled());
        assertEquals(m_testJob.CANCEL_MESSAGE, svc.getJobStatus(jobId).getMessage());        
    }
    
    @Test
    public void testGetStatusBadJobId() throws Exception
    {
        assertEquals("Old or bad ids should be treated as complete status 100", Integer.valueOf(100),svc.getJobStatus(9999).getStatus());
    }
    
    private long runJob(int increment) throws IPSFolderService.PSWorkflowNotFoundException {
        long jobId = svc.startJob("asyncJobTest", increment);
        assertTrue(jobId > 0);
        
        for (int status = 1; status < 100; status += increment)
        {
            checkStatus(jobId, status);
        }

        if (increment < 100)
            checkStatus(jobId, 100);
        
        // check status again
        checkStatus(jobId, 100);
        return jobId;
    }
    
    private int checkStatus(long jobId, int expectedStatus)
    {
        PSAsyncJobStatus jobStatus = svc.getJobStatus(jobId);
        assertNotNull(jobStatus);
        assertEquals(jobId, jobStatus.getJobId().longValue());
        assertEquals(expectedStatus, jobStatus.getStatus().intValue());
        
        String expectedMessage;
        if (expectedStatus == 100)
            expectedMessage = PSTestAsyncJob.DONE_MESSAGE;
        else
            expectedMessage = PSTestAsyncJob.STATUS_MESSAGE + expectedStatus;
            
        assertEquals(expectedMessage, jobStatus.getMessage());
        
        return jobStatus.getStatus();
    }
}
