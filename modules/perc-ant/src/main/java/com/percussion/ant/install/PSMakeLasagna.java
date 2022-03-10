/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.ant.install;

import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.server.PSServer;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.util.PSProperties;
import com.percussion.utils.io.PathUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * This task is used to encrypt the password found in the repository properties
 * file.
 * <br>
 * Example Usage:
 * <br>
 * <pre>
 * 
 * First set the taskdef:
 * 
 *  <code>  
 *  &lt;taskdef name="makeLasagna"
 *              class="com.percussion.ant.install.PSMakeLasagna"
 *              classpath="${INSTALL.CLASSPATH}"/&gt;
 *  </code>
 * 
 * Now use the task to encrypt the repository password:
 * 
 *  <code>  
 *  &lt;makeLasagna root="${install.dir}"/&gt;
 *  </code>
 * 
 * </pre>
 */
public class PSMakeLasagna extends Task
{
   
   /**
    * Sets the root directory.
    * 
    * @param root the installation directory, cannot be <code>null</code> or
    * empty.
    */
   public void setRoot(String root)
   {
      if (StringUtils.isBlank(root))
      {
         throw new IllegalArgumentException("root may not be null or empty");
      }
      
      m_root = root;
   }
   
   /**
    * Gets the root directory
    * 
    * @return the installation directory.
    */
   public String getRoot()
   {
      return m_root;
   }
   
   // see base class
   @SuppressFBWarnings({"HARD_CODE_PASSWORD", "HARD_CODE_PASSWORD"})
   @Override
   public void execute() throws BuildException
   {
      FileOutputStream out = null;
      try
      {
         String propFilePath = m_root + File.separator +
                 "rxconfig/Installer/rxrepository.properties";
         PSProperties props = new PSProperties(propFilePath);
         String encryptedPWDProp = props.getProperty(PSJdbcDbmsDef.PWD_ENCRYPTED_PROPERTY);
         String pwd = props.getProperty(PSJdbcDbmsDef.PWD_PROPERTY);
         if(StringUtils.isEmpty(pwd)){
            return;
         }
         String decryptPwd = "";
         //Considering that password is encrypted in case PWD_ENCRYPTED flag is not set
         //If user gives a plain password then PWD_ENCRYPTED needs to be set to "N"
         if (encryptedPWDProp == null || encryptedPWDProp.equalsIgnoreCase("Y"))
         {
             try {
                decryptPwd = PSEncryptor.decryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),propFilePath, PSJdbcDbmsDef.PWD_PROPERTY, pwd);
             }catch (PSEncryptionException pe) {
                try {
                   decryptPwd = PSEncryptor.decryptWithOldKey(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),pwd);
                } catch (PSEncryptionException | java.lang.IllegalArgumentException e) {
                   decryptPwd = PSLegacyEncrypter.getInstance(
                           PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                   ).decrypt(pwd, PSServer.getPartOneKey(), null);
                }
             }
         }
         if(!StringUtils.isEmpty(decryptPwd)){
            pwd = decryptPwd;
         }
         //Encrypt the password with new key and save.
         pwd = PSEncryptor.encryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),propFilePath,PSJdbcDbmsDef.PWD_PROPERTY,pwd );
         props.setProperty(PSJdbcDbmsDef.PWD_PROPERTY, pwd);
         props.setProperty(PSJdbcDbmsDef.PWD_ENCRYPTED_PROPERTY, "Y");
         out = new FileOutputStream(m_root + File.separator +
         "rxconfig/Installer/rxrepository.properties");
         props.store(out, null);
      }
      catch (Exception e)
      {
         throw new BuildException(e.getMessage());
      }
      finally
      {
         if (out != null)
         {
            try
            {
               out.close();
            }
            catch (IOException e)
            {
               throw new BuildException(e.getMessage());
            }
         }
      }
   }
   
   /**
    * The root installation directory, should not be <code>null</code> or empty.
    */
   private String m_root;
   
}
