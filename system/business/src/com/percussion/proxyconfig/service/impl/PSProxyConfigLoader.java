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
package com.percussion.proxyconfig.service.impl;

import static org.apache.commons.lang.Validate.notNull;
import static com.percussion.share.dao.PSSerializerUtils.unmarshalWithValidation;
import static com.percussion.share.dao.PSSerializerUtils.marshal;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.percussion.error.PSExceptionUtils;
import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.impl.ProxyConfig.Password;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.io.PathUtils;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author LucasPiccoli
 *
 */
public class PSProxyConfigLoader
{
   /**
    * Logger for this class
    */
    public static final Logger log = LogManager.getLogger(PSProxyConfigLoader.class);
   
   /**
    * List of all proxy configurations loaded from the file
    */
   private List<PSProxyConfig> proxyConfigurations;
   
   public PSProxyConfigLoader(File configFile)
   {
       notNull(configFile);
       
       proxyConfigurations = new ArrayList<>();
       
       if (configFile.exists()) {
           try {
               readAndEncryptConfigFile(configFile);
           } catch (CloneNotSupportedException e) {
               throw new RuntimeException(e);
           }
       }
   }

   /**
    * @return the proxyConfigurations
    */
   public List<PSProxyConfig> getProxyConfigurations()
   {
      return proxyConfigurations;
   }

   /**
    * If the configuration file exists, read every present Proxy server
    * configuration. Loads every password.
    * 
    * @param configFile the proxy configuration file.
    */
   private void readAndEncryptConfigFile(File configFile) throws CloneNotSupportedException {
      ProxyConfigurations config = getProxyConfig(configFile);
       PSProxyConfig proxyConfig;
       String encrypterKey = PSLegacyEncrypter.getInstance(PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)).getPartOneKey();
       boolean configChanged = false;
       
       for (ProxyConfig s : config.getConfigs())
       {
           log.debug("Proxy Configuration: {}" , s.getHost());
           
           proxyConfig = new PSProxyConfig(s);
           proxyConfigurations.add(proxyConfig);
           
           configChanged = processPassword(s.getPassword(), configFile,proxyConfig, encrypterKey);
       }

       if (configChanged)
       {
          updateConfigFile(configFile, config);
       }
   }

   /**
    * processPassword
    * @param pwd
    * @param proxyConfig
    * @param encrypterKey 
    * @return true if the password was encrypted by the process. false if it was already encrypted.
    */
   private boolean processPassword(Password pwd,File configFile, PSProxyConfig proxyConfig, String encrypterKey)
   {
      if (pwd == null)
         return false;
      String pwdVal = pwd.getValue();
      String decryptedPassword;
       if (pwd.isEncrypted())
      {
          try {
            decryptedPassword = PSEncryptor.decryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),configFile.getAbsolutePath(),null,pwdVal);
          }catch (PSEncryptionException e) {
              try {
                  decryptedPassword = PSEncryptor.decryptWithOldKey(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), pwdVal);

              } catch (PSEncryptionException pe) {
                  decryptedPassword = PSLegacyEncrypter.getInstance(PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)
                  ).decrypt(pwdVal, encrypterKey, null);

              }
              String enc = null;
              try {
                  enc = PSEncryptor.encryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), configFile.getAbsolutePath(), null, pwdVal);
              } catch (PSEncryptionException e2) {
                  log.error("Error encrypting password:{} " + e2.getMessage(), e2);
                  enc = "";
              }
              pwd.setValue(enc);
              pwd.setEncrypted(Boolean.TRUE);
              proxyConfig.setPassword(decryptedPassword);
              return true;
          }
          proxyConfig.setPassword(decryptedPassword);
          return false;
      }
       return false;
   }

   /**
    * @param configFile configuration file
    * @param config valid configuration
    */
   private void updateConfigFile(File configFile, ProxyConfigurations config)
   {
      // Update config file with encrypted passwords
      FileWriter fileWriter = null;
      BufferedWriter bfWriter = null;
      try
      {
          fileWriter = new FileWriter(configFile);
          bfWriter = new BufferedWriter(fileWriter);
          bfWriter.write(marshal(config));
      }
      catch (IOException e)
      {
          log.error("Error writing the proxy configuration file: " +
                  PSExceptionUtils.getMessageForLog(e));
      }
      finally
      {
          IOUtils.closeQuietly(bfWriter);
          IOUtils.closeQuietly(fileWriter);
      }
   }

   /**
    * Reads the configuration file and returns a ProxyConfigurations object.
    * 
    * @param configFile the configuration file, assumed not <code>null</code>.
    * 
    * @return main configuration object. Never <code>null</code>.
    */
   private ProxyConfigurations getProxyConfig(File configFile)
   {
       try(InputStream in = new FileInputStream(configFile))
       {
           return unmarshalWithValidation(in, ProxyConfigurations.class);
       }
       catch (Exception e)
       {
          String msg = "Unknown Exception";
          Throwable cause = e.getCause();
          if(cause != null && isNotBlank(cause.getLocalizedMessage()))
          {
             msg = cause.getLocalizedMessage();
          }
          else if(isNotBlank(e.getLocalizedMessage()))
          {
             msg = e.getLocalizedMessage();
          }
          log.error("Error getting proxy server configurations from file: " +  msg);
          return new ProxyConfigurations();
       }
   }

}
