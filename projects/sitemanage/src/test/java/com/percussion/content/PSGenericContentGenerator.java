// REFACTORED: CP-JAVA11
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

package com.percussion.content;

import com.percussion.error.PSExceptionUtils;
import com.percussion.share.dao.PSSerializerUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.UnmarshalException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Abstract root class for all generators. It provides methods to validate input
 * arguments (server URL and XML file), to load the XML file with the content to
 * be generated, and wrapper methods to generate and cleanup content. These last
 * two ones automatically load XML content and call appropriate methods in the
 * implementing class. They also log all errors. This class also provides
 * functionality to run a main method.
 * <p>
 * Note that this class has a different responsibility than {@link PSGenerator
 * <T>}.
 *
 * @author miltonpividori
 * @param <T> A JAXB class which represents the content to load from the XML
 *            file.
 */
public abstract class PSGenericContentGenerator<T> {
    protected static Logger log = LogManager.getLogger(PSGenericContentGenerator.class);

    protected String serverUrl;
    protected String username;
    protected String password;
    protected InputStream xmlData;
    protected T content;
    protected String licenseId;

    public PSGenericContentGenerator(String serverUrl, InputStream xmlData, String username, String password) {
        org.apache.commons.lang.Validate.notEmpty(serverUrl);
        org.apache.commons.lang.Validate.notNull(xmlData);

        this.serverUrl = serverUrl;
        this.xmlData = xmlData;
        this.username = username;
        this.password = password;
    }

    public PSGenericContentGenerator(String serverUrl, InputStream xmlData, String username, String password, String licenseId) {
        org.apache.commons.lang.Validate.notEmpty(serverUrl);
        org.apache.commons.lang.Validate.notNull(xmlData);

        this.serverUrl = serverUrl;
        this.xmlData = xmlData;
        this.username = username;
        this.password = password;
        this.licenseId = licenseId;
    }

    public T getRootData() {
        return content;
    }

    public boolean dataSuccessfullyLoaded() {
        return content != null;
    }

    protected boolean loadXmlData() {
        if (content != null) {
            return true;
        }
        log.info("Loading the XML content");
        try {
            content = com.percussion.share.dao.PSSerializerUtils.unmarshalWithValidation(xmlData, getRootDataType());
            return true;
        } catch (FileNotFoundException e) {
            log.error("The XML file was not found");
            return false;
        } catch (javax.xml.bind.UnmarshalException e) {
            log.error("Error when unmarshaling the XML file. Make sure it conforms with the XML schema: " + e.getLinkedException().getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unknown error when reading the XML file: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    protected static <K extends PSGenericContentGenerator> void runMainMethod(String[] args, Class<K> generatorClass) {
        org.apache.commons.lang.Validate.notNull(args, "arguments must not be null");
        org.apache.commons.lang.Validate.isTrue(args.length == 4, "some arguments were not specified");

        var url = args[0];
        var uid = args[1];
        var pw = args[2];
        var defFileName = args[3];

        org.apache.commons.lang.Validate.notEmpty(url, "Server URL must be specified");
        org.apache.commons.lang.Validate.notEmpty(defFileName, "XML file name must be specified");

        try {
            var ctor = generatorClass.getConstructor(String.class, InputStream.class, String.class, String.class);
            try (var fs = new FileInputStream(defFileName)) {
                var contentGenerator = ctor.newInstance(url, fs, uid, pw);
                contentGenerator.cleanup();
                contentGenerator.generateContent();
            }
        } catch (IOException | IllegalAccessException | InstantiationException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
            log.error(com.percussion.error.PSExceptionUtils.getMessageForLog(e));
            log.debug(com.percussion.error.PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    @SuppressWarnings("rawtypes")
    protected static <K extends PSGenericContentGenerator> void runMainMethodSecure(String[] args, Class<K> generatorClass) {
        org.apache.commons.lang.Validate.notNull(args, "arguments must not be null");
        org.apache.commons.lang.Validate.isTrue(args.length == 6, "some arguments were not specified");

        var url = args[0];
        var adminUser = args[1];
        var adminPassword = args[2];
        var xmlDefFileName = args[3];
        var secureUrl = args[4];
        var allowSelfSignedCertificate = Boolean.parseBoolean(args[5]);

        org.apache.commons.lang.Validate.notEmpty(url, "Server URL must be specified");
        org.apache.commons.lang.Validate.notEmpty(xmlDefFileName, "XML file name must be specified");

        try {
            var ctor = generatorClass.getConstructor(String.class, String.class, Boolean.class, InputStream.class, String.class, String.class);
            try (var fi = new FileInputStream(xmlDefFileName)) {
                var contentGenerator = ctor.newInstance(url, secureUrl, allowSelfSignedCertificate, fi, adminUser, adminPassword);
                contentGenerator.cleanup();
                contentGenerator.generateContent();
            }
        } catch (Exception e) {
            log.error(com.percussion.error.PSExceptionUtils.getMessageForLog(e));
            log.debug(com.percussion.error.PSExceptionUtils.getDebugMessageForLog(e));
            throw new RuntimeException(e);
        }
    }

    public void cleanup() {
        if (!loadXmlData()) {
            return;
        }
        log.info("Cleaning up content for all defined elements in the data source");
        try {
            cleanupAllContent();
        } catch (Exception e) {
            log.error("Error when cleaning up content", e);
            throw new RuntimeException(e);
        }
    }

    public void generateContent() {
        if (!loadXmlData()) {
            return;
        }
        log.info("Generating content for all defined elements in the data source");
        try {
            generateAllContent();
        } catch (Exception e) {
            log.error("Error when generating content", e);
            throw new RuntimeException(e);
        }
    }

    protected abstract void cleanupAllContent();

    protected abstract void generateAllContent();

    protected abstract Class<T> getRootDataType();
}
