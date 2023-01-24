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
package com.percussion.utils.service.impl;

import com.percussion.design.objectstore.PSSubject;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.data.PSBackEndRole;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 
 * Simplifies the Back-end role manager while also making it thread safe.
 * <p>
 * The back end role manager users the Server XML Object Store locker to lock
 * server configuration. This locker is not thread-safe so we make access to
 * back-end role manager single threaded.
 * 
 * @author adamgent
 * 
 */
public class PSBackEndRoleManagerFacade
{
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private IPSBackEndRoleMgr backEndRoleMgr;

    public PSBackEndRoleManagerFacade(IPSBackEndRoleMgr backEndRoleMgr)
    {
        this.backEndRoleMgr = backEndRoleMgr;
    }
    
    /**
     * See {@link IPSBackEndRoleMgr#getRhythmyxRoles()}.  The returned list will be sorted case-insensitive.
     */
    public List<String> getRoles()
    {
        try
        {
            lock.readLock().lock();
            List<String> roles = backEndRoleMgr.getRhythmyxRoles();
            Collections.sort(roles, ms_caseInsensitiveComparator);
            return roles;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * See {@link IPSBackEndRoleMgr#getRhythmyxRoles(String, int)}.  The returned list will be sorted case-insensitive.
     */
    public List<String> getRoles(String subjectName)
    {
        try
        {
            lock.readLock().lock();
            List<String> roles = backEndRoleMgr.getRhythmyxRoles(subjectName, PSSubject.SUBJECT_TYPE_USER);
            Collections.sort(roles, ms_caseInsensitiveComparator);
            return roles;
        }
        finally
        {
            lock.readLock().unlock();
        }

    }

    /**
     * See {@link IPSBackEndRoleMgr#setRhythmyxRoles(String, int, Collection)}.
     */
    public void setRoles(String subjectName, Collection<String> roles)
    {
        try
        {
            lock.writeLock().lock();
            backEndRoleMgr.setRhythmyxRoles(subjectName, PSSubject.SUBJECT_TYPE_USER, roles);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * See {@link IPSBackEndRoleMgr#setRhythmyxRoles(Collection<String>, int, Collection)}.
     */
    public void setRoles(Collection<String> subjectNames, Collection<String> roles)
    {
        try
        {
            lock.writeLock().lock();
            backEndRoleMgr.setRhythmyxRoles(subjectNames, PSSubject.SUBJECT_TYPE_USER, roles);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }    

    /**
     * See {@link IPSBackEndRoleMgr#setSubjectEmail(String, String)}.
     */
    public void setSubjectEmail(String subjectName, String subjectEmail)
    {
        try
        {
            lock.writeLock().lock();
            backEndRoleMgr.setSubjectEmail(subjectName, subjectEmail);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Finds the role with the specified name.
     * 
     * @param name of the role, never <code>null</code> or empty.
     * 
     * @return role object or <code>null</code> if the role does not exist.
     */
    public PSBackEndRole getRole(String name)
    {
        PSBackEndRole role = null;

        try
        {
            lock.readLock().lock();
            List<PSBackEndRole> roles = backEndRoleMgr.findRolesByName(name);
            if (!roles.isEmpty())
            {
                if (name.contains("%"))
                {
                    // '%' is a wildcard, so find the exact match
                    for (PSBackEndRole r : roles)
                    {
                        if (r.getName().equals(name))
                        {
                            role = r;
                            break;
                        }
                    }
                }
                else
                {
                    role = roles.get(0);
                }
            }
        }
        finally
        {
            lock.readLock().unlock();
        }

        return role;
    }


    /**
     * See {@link IPSBackEndRoleMgr#createRole(String, String)}.
     */
    public PSBackEndRole createRole(String name, String description)
    {
        try
        {
            lock.writeLock().lock();
            return backEndRoleMgr.createRole(name, description);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * See {@link IPSBackEndRoleMgr#deleteRole(String)}.
     */
    public void deleteRole(String name)
    {
        try
        {
            lock.writeLock().lock();
            backEndRoleMgr.deleteRole(name);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
    
    
    /**
     * See {@link IPSBackEndRoleMgr#update(String,String)}.
     */
    public PSBackEndRole update(String name, String description)
    {
        try
        {
            lock.writeLock().lock();
            return backEndRoleMgr.update(name, description);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
    
    
    private static Comparator<String> ms_caseInsensitiveComparator = new Comparator<String>()
    {
        public int compare(String o1, String o2)
        {
            return o1.compareToIgnoreCase(o2);
        }        
    };
    
}
