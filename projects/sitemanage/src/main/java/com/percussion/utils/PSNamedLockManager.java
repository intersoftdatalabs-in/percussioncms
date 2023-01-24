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
package com.percussion.utils;

import org.apache.commons.lang.Validate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a map of named locks, so that locks can be acquired based on a name, also simplifies some of the semantics.
 * Locks are reentrant and waiting threads are managed fairly favoring the longest waiting thread. 
 * 
 * @author JaySeletz
 *
 */
public class PSNamedLockManager
{
    private ConcurrentMap<String, ReentrantLock> lockMap;
    private long waitMillis;
    
    /**
     * Create the lock manager, specifying the timeout for acquiring a lock.  
     * 
     * @param waitMillis The timeout, in milliseconds, specifies the wait time when acquiring locks, <=0 for no wait.
     */
    public PSNamedLockManager(long waitMillis)
    {
        this.waitMillis = waitMillis;
        lockMap = new ConcurrentHashMap<>();
    }
    
    /**
     * Attempt to acquire the lock.  This may block for the timeout specified during construction.
     * 
     * @param name The name for which the lock is to be acquired, not <code>null<code/> or empty.
     * 
     * @return <code>true</code> if the lock is acquired, <code>false</code> if not.
     *  
     */
    public boolean getLock(String name)
    {
        Validate.notEmpty(name);
        
        ReentrantLock lock = new ReentrantLock(true);
        ReentrantLock current = lockMap.get(name);
        // putIfAbsent requires internal lock even if item exists
        // use double checked locking to use non blocking reads when
        // lock already exists.
        //FB: JLM_JSR166_UTILCONCURRENT_MONITORENTER NC 1-16-16
        if (current==null)
        {
                current = lockMap.putIfAbsent(name, lock);
        }
        if (current != null) {
            lock = current;
        }
        
        boolean didLock = false;
        
        try
        {
            didLock = lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        
        return didLock;
    }
    
    /**
     * Release the lock held by the current tread.
     * 
     * @param name The name of the lock to release, not <code>null<code/> or empty.
     * 
     */
    public boolean releaseLock(String name)
    {
        Validate.notEmpty(name);
        
        boolean unlocked = false;
        
        ReentrantLock lock = lockMap.get(name);
        if (lock != null)
        {
            try
            {
                lock.unlock();
                unlocked = true;
            }
            catch (Exception e)
            {
                // didn't unlock, fall through
            }
        }
        
        return unlocked;
    }
    
    /**
     * Determine if the current thread holds the named lock.
     * 
     * @param name the name of the lock to check, not <code>null<code/> or empty.
     * 
     * @return <code>true</code> if the current thread has the lock, <code>false</code> if not.
     */
    public boolean haveLock(String name)
    {
        Validate.notEmpty(name);
        
        boolean haveLock = false;
        ReentrantLock lock = lockMap.get(name);
        if (lock != null)
        {
            haveLock = lock.isHeldByCurrentThread();
        }
        
        return haveLock;
    }
    
    /**
     * Determine if any thread holds the named lock.
     * 
     * @param name the name of the lock to check, not <code>null<code/> or empty.
     * 
     * @return <code>true</code> if any thread has the lock, <code>false</code> if not.
     */
    public boolean isLocked(String name)
    {
        boolean isLocked = false;
        ReentrantLock lock = lockMap.get(name);
        if (lock != null)
        {
            isLocked = lock.isLocked();
        }
        
        return isLocked;
    }
}
