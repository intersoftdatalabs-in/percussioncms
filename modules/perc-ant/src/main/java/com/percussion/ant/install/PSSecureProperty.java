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

package com.percussion.ant.install;

import com.percussion.error.PSExceptionUtils;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.security.ToDoVulnerability;
import com.percussion.utils.io.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.text.BasicTextEncryptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encrypts properties in a properties file that are specified to be encrypted.
 * @author erikserating
 *
 */
public class PSSecureProperty
{

    private static final Logger log = LogManager.getLogger(PSSecureProperty.class);

    private PSSecureProperty(){}

    /**
     * Encrypts all specified properties if they have not already been encrypted. The file is re-written
     * with the encrypted values. To determine if a property is encrypted we check for the following format
     * in the value ENC(thehashgoeshere).
     * @param filepath the path to the properties file to be modified. Cannot be <code>null</code>.
     * @param propnames list of properties names of properties whose values should be encrypted. Cannot be
     * <code>null</code>, may be empty.
     * @param k an optional key to use to encrypt the value, the default key will be used if this is
     * <code>null</code>.
     * @param  encryptionType Optional.  They type of encryption to use.
     */
    @ToDoVulnerability
    public static void secureProperties(File filepath, Collection<String> propnames, String k, String encryptionType)
    {
        if(propnames == null)
            throw new IllegalArgumentException(ERROR_PROPS);
        if(propnames.isEmpty())
        {
            log.info("No property names specified, nothing to do.");
            return;
        }
        if (validateFilePath(filepath)) {

            Properties props = new Properties();
            boolean modified = false;
            String ky = k == null ? PSLegacyEncrypter.SECURE_PROPERTY_DEFAULT_KEY : k;


            log.debug("loading properties from file: {}" , filepath.getAbsolutePath());
            try (FileInputStream is = new FileInputStream(filepath)) {
                props.load(is);
            } catch (IOException e) {
                log.error("{}" ,PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            for (String regex : propnames) {
                String[] expandedKeys = expandMatchingKeys(regex, props);
                for (String key : expandedKeys) {
                    boolean enc = isValueClouded((String) props.get(key));
                    if (!enc) {
                        String pwd = (String) props.get(key);
                        String encVal = StringUtils.EMPTY;
                        if(pwd != null){
                            try {
                                encVal = PSEncryptor.encryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),filepath.getAbsolutePath(),key,pwd);
                                encVal = getClouded(encVal);
                            } catch (PSEncryptionException e) {
                                log.error(PSExceptionUtils.getMessageForLog(e));
                                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                            }
                        }
                        props.put(key,encVal);
                        modified = true;
                    }
                }
            }
            if (modified) {
                try (FileOutputStream os = new FileOutputStream(filepath)) {
                    props.store(os, "");
                } catch (IOException e) {
                    log.error("ERROR: {}" ,PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        }
    }

    private static boolean validateFilePath(File filepath){

        if (filepath == null)
            throw new IllegalArgumentException(ERROR_FILEPATH);

        if (!filepath.exists() || !filepath.isFile()) {
            log.warn("File does not exist: {}" , filepath.getAbsolutePath());
            return false;
        }
        return true;

    }

    public static void unsecureProperties(File filepath)
    {

        if(validateFilePath(filepath)) {

            Properties props = new Properties();
            boolean modified = false;
            log.debug("loading properties from file: {}" , filepath.getAbsolutePath());
            try(FileInputStream is = new FileInputStream(filepath)) {
                props.load(is);
            } catch (IOException e) {
                log.error("ERROR : {}",PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }

            for (Map.Entry entry : props.entrySet()) {
                String value = (String)entry.getValue();
                if(isValueClouded(value)){
                    props.put(entry.getKey(), getValue(value,null));
                    modified=true;

                }
            }

            if (modified) {
                try(FileOutputStream os = new FileOutputStream(filepath)) {
                    props.store(os, "");
                } catch (IOException e) {
                    log.error("ERROR: {}" ,PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        }
    }
    /**
     * Determine if a value is encrypted.
     * @param s may be <code>null</code> or empty.
     * @return <code>true</code> if encrypted.
     */
    public static boolean isValueClouded(String s)
    {
        if(s == null || s.length() == 0)
            return false;
        return ((s.startsWith(ENC_PREFIX) || s.startsWith(ENC_AES_PREFIX)) && s.endsWith(ENC_POSTFIX));
    }

    /**
     * Retrieves the decrypted value of the passed in string.
     *
     * @param s encrypted string value. Cannot be <code>null</code>.
     * @param k an optional key to use to encrypt the value, the default key will be used if this is
     *          <code>null</code>.
     * @return the decrypted string, never <code>null</code>, may be empty.
     */
    public static String getValue(String s, String k) {
        if (s == null)
            throw new IllegalArgumentException();


        String encryptedValue = "";
        String decryptedValue = StringUtils.EMPTY;

        if (s.startsWith(ENC_PREFIX) && s.endsWith(ENC_POSTFIX)) {
            encryptedValue = s.substring(ENC_PREFIX.length(), s.length() - 1);
        } else if (s.startsWith(ENC_AES_PREFIX) && s.endsWith(ENC_POSTFIX)) {
            encryptedValue = s.substring(ENC_AES_PREFIX.length(), s.length() - 1);
        }

        try {
            //Attempt using the updated encryptor
            decryptedValue = PSEncryptor.decryptString(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR),encryptedValue);
        } catch (PSEncryptionException e) {
            log.debug("Decrypting using legacy algorithm");
            String ky = k == null ? PSLegacyEncrypter.SECURE_PROPERTY_DEFAULT_KEY : k;

            try {
                BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
                textEncryptor.setPassword(ky);
                decryptedValue = textEncryptor.decrypt(encryptedValue);
            } catch (EncryptionOperationNotPossibleException | EncryptionInitializationException e1) {
                log.debug("Decrypting using legacy AES algorithm");
                try {
                    StandardPBEStringEncryptor pbe = getStrongEncryptor(ky);
                    decryptedValue = pbe.decrypt(encryptedValue);
                } catch (EncryptionOperationNotPossibleException | EncryptionInitializationException e2) {
                    log.error("Unable to decrypt property:{}", e2.getMessage());
                }
            }
        }
        return decryptedValue;
    }

    /**
     * Retrieves the encrypted value of the passed in string.
     *
     * @param s A clear text string to encrypted.
     * @return A clouded & encrypted string
     */
    public static String getClouded(String s) {
        if (s == null)
            throw new IllegalArgumentException();
          return ENC_PREFIX +  s + ENC_POSTFIX;
    }

    /**
     * Helper to expand all matching keys for a regular expression.
     *
     * @param regex assumed not <code>null</code> or empty.
     * @param props assumed not <code>null</code>.
     * @return An array of matching keys
     */
    private static String[] expandMatchingKeys(String regex, Properties props) {
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        for(Object key : props.keySet())
        {
            String current = (String)key;
            matcher = pattern.matcher(current);
            if(matcher.matches())
            {
                results.add(current);
            }

        }

        return results.toArray(new String[]{});
    }

    /**
     * Helper to create a standard PBE encryptor.
     *
     * @param password the password to set, assumed not <code>null</code> or empty.
     *
     * @return A StandardPBEStringEncryptor, never <code>null</code> or empty.
     */
    private static StandardPBEStringEncryptor getStrongEncryptor(String password)
    {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setProvider(new BouncyCastleProvider());
        encryptor.setAlgorithm(ENC_AES_ALGORITHM);
        encryptor.setPassword(password);

        return encryptor;
    }



    private static final String ERROR_FILEPATH =
            "The filepath cannot be null!!!!!";

    private static final String ERROR_PROPS =
            "Propnames cannot be null!!!!!";

    /**
     * Constant for the encryption prefix string.
     */
    private static final String ENC_PREFIX = "ENC(";

    /**
     * Constant for the strong encryption prefix string.
     */
    private static final String ENC_AES_PREFIX = "ENC2(";

    /**
     * Constant for the encryption postfix string.
     */
    private static final String ENC_POSTFIX = ")";

    /**
     * Constant for the default encryption algorithm.
     */
    private static final String DEFAULT_ENCRYPTION = "ENC";

    /**
     * Constant for the strong encryption using AES.
     */
    private static final String AES_ENCRYPTION = "ENC2";

    /**
     * Constant for the encryption algorithm.
     */
    private static final String ENC_AES_ALGORITHM = "PBEWITHSHA256AND128BITAES-CBC-BC";
}
