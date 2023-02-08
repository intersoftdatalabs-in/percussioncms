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
package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSDeployComponentUtils;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSServer;
import com.percussion.services.notification.PSNotificationHelper;
import com.percussion.util.IOTools;
import com.percussion.utils.collections.PSIteratorUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for handlers that package and install files directly to and from 
 * the file system.
 */
public abstract class PSFileDependencyHandler extends PSDependencyHandler
{
   /**
    * Construct a dependency handler.
    * 
    * @param def The def for the type supported by this handler.  May not be
    * <code>null</code> and must be of the type supported by this class.  See
    * {@link #getType()} for more info.
    * @param dependencyMap The full dependency map.  May not be 
    * <code>null</code>.
    * 
    * @throws IllegalArgumentException if any param is invalid.
    */
   public PSFileDependencyHandler(PSDependencyDef def, 
      PSDependencyMap dependencyMap)
   {
      super(def, dependencyMap);
   }
   
   
   /**
    * This class returns an empty list.  Derrived class should override this
    * method if they support child types.  See 
    * {@link PSDependencyHandler#getChildDependencies(PSSecurityToken, 
    * PSDependency) Base Class} for more info.
    */
   public Iterator getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
         
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");
      
      if (!dep.getObjectType().equals(getType()))
         throw new IllegalArgumentException("dep wrong type");
         
      return PSIteratorUtils.emptyIterator();
   }
   
   // see base class
   public Iterator getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
         
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");
         
      if (!dep.getObjectType().equals(getType()))
         throw new IllegalArgumentException("dep wrong type");
      
      List files = new ArrayList();
      File depFile = new File(
            PSServer.getRxDir().getAbsolutePath(), 
            PSDeployComponentUtils.getNormalizedPath(dep.getDependencyId()));
      if (!depFile.exists())
      {
         Object[] args = {dep.getObjectTypeName(), dep.getDependencyId(), 
               dep.getDisplayName()};
         throw new PSDeployException(IPSDeploymentErrors.DEP_OBJECT_NOT_FOUND, 
            args);
      }
      
      files.add(new PSDependencyFile(PSDependencyFile.TYPE_SUPPORT_FILE, 
         depFile));
      
      return files.iterator();
   }

   // see base class
   public void installDependencyFiles(PSSecurityToken tok, 
      PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx) 
         throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
         
      if (archive == null)
         throw new IllegalArgumentException("archive may not be null");
         
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");
         
      if (!dep.getObjectType().equals(getType()))
         throw new IllegalArgumentException("dep wrong type");
      
      if (ctx == null)
         throw new IllegalArgumentException("ctx may not be null");
      
      // get file data
      Iterator files = archive.getFiles(dep);
      PSDependencyFile depFile = null;
      if (files.hasNext())
      {
         depFile = (PSDependencyFile)files.next();
      }
      
      if (depFile == null)
      {
         Object[] args = 
         {
            PSDependencyFile.TYPE_ENUM[PSDependencyFile.TYPE_SUPPORT_FILE], 
            dep.getObjectType(), dep.getDependencyId(), dep.getDisplayName()
         };
         throw new PSDeployException(
            IPSDeploymentErrors.MISSING_DEPENDENCY_FILE, args);
      }            
      
      int transAction = PSTransactionSummary.ACTION_CREATED;
      File tgtFile = new File(PSServer.getRxDir().getAbsolutePath(),
            PSDeployComponentUtils.getNormalizedPath(dep.getDependencyId()));
      if (tgtFile.exists())
         transAction = PSTransactionSummary.ACTION_MODIFIED;
      
      // create directories if they do not exist
      File parentDir = tgtFile.getParentFile();
      if (parentDir != null)
         parentDir.mkdirs();

         // ensure the timestamp is updated
         tgtFile.setLastModified(System.currentTimeMillis());

          try (FileOutputStream out = new FileOutputStream(tgtFile)) {
             try (InputStream in = archive.getFileData(depFile)) {
                IOTools.copyStream(in, out);
             }
          } catch (FileNotFoundException e) {
             throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR,
                     e.getLocalizedMessage());
          } catch (IOException e) {
             throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR,
                     e.getLocalizedMessage());
          }

         addTransactionLogEntry(dep, ctx, tgtFile.getPath(),
            PSTransactionSummary.TYPE_FILE, transAction);

         // notify whoever interested after a the file is successful installed
         PSNotificationHelper.notifyFile(tgtFile);                  
      }

   
   // see base class
   public Iterator getDependencies(PSSecurityToken tok) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
         
      return PSIteratorUtils.emptyIterator();
   }
   
   
   // see base class
   public PSDependency getDependency(PSSecurityToken tok, String id) 
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
         
      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");
       
      PSDependency dep = null;
      
      id = PSDeployComponentUtils.getNormalizedPath(id);
      File depFile = new File(id);
      if (depFile.exists())
         dep = createDependency(m_def, id, depFile.getName());
      
      return dep;
   }

   /**
    * Provides the list of child dependency types this class can discover.
    * Base class returns an empty list.  Derrived class should override this
    * method if they support child types.
    * 
    * @return An empty iterator, never <code>null</code>.
    */
   public Iterator getChildTypes()
   {
      return PSIteratorUtils.emptyIterator();
   }

   // see base class
   public boolean doesDependencyExist(PSSecurityToken tok, String id) 
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
         
      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");
      
      return (getDependency(tok, id) != null);
   }
}

