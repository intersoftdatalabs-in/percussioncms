/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11
package com.percussion.share.extension;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSStartupProcess;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.PSServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Startup process that auto-imports customer trusted certificates
 * into the central cacerts keystore when the server starts.
 *
 * @author Santosh Dhariwal
 */
public class PSImportCustCertificates implements IPSStartupProcess {

    private static final Logger log = LogManager.getLogger(PSImportCustCertificates.class);

    public PSImportCustCertificates() {}

    /**
     * Allow for running from the command line.
     */
    public static void main(String[] args) {
        var props = new Properties();
        props.setProperty(PSImportCustCertificates.class.getSimpleName(), "true");
        var run = new PSImportCustCertificates();
        try {
            run.doStartupWork(props);
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    @Override
    public void doStartupWork(Properties startupProps) {
        var propName = getPropName();
        if (!"true".equalsIgnoreCase(startupProps.getProperty(propName))) {
            log.info("{} is set to false or missing from startup properties file. Nothing to run.", propName);
            return;
        }

        var password = "changeit".toCharArray();
        var certificatePath = System.getProperty("java.home") + "/lib/security/cacerts";
        var file = new File(certificatePath);
        try (InputStream localCertIn = new FileInputStream(file)) {
            var keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(localCertIn, password);

            var custCertificateDir = new File(PSServer.getRxDir(), "rxconfig/trusted_certificates");
            var certificates = custCertificateDir.listFiles();
            if (certificates != null && certificates.length > 0) {
                for (var cert : certificates) {
                    if (!cert.isDirectory()) {
                        appendCertKey(cert, keystore);
                    }
                }
                try (FileOutputStream out = new FileOutputStream(file)) {
                    keystore.store(out, password);
                }
            } else {
                log.info("No Certificate Files found in : {}", custCertificateDir.getPath());
            }
        } catch (Exception e) {
            log.error("Error while importing customer trusted certificates into the central cacerts keystore.", e);
        }

        log.info("{} has completed.", propName);
    }

    private static void appendCertKey(File file, KeyStore keystore) {
        var fname = file.getPath();
        var sName = file.getName();
        try (FileInputStream fis = new FileInputStream(fname)) {
            var alias = sName + " : " + fis.getChannel().size();
            if (keystore.containsAlias(alias)) {
                return;
            }
            var aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                var str = aliases.nextElement();
                if (str.contains(sName)) {
                    keystore.deleteEntry(str);
                    break;
                }
            }
            try (DataInputStream dis = new DataInputStream(fis)) {
                var bytes = new byte[dis.available()];
                dis.readFully(bytes);
                try (ByteArrayInputStream certIn = new ByteArrayInputStream(bytes);
                     BufferedInputStream bis = new BufferedInputStream(certIn)) {
                    var cf = CertificateFactory.getInstance("X.509");
                    Certificate cert = cf.generateCertificate(bis);
                    keystore.setCertificateEntry(alias, cert);
                }
            }
        } catch (Exception e) {
            log.error("Error while importing customer trusted certificate File Name : {}", file.getName(), e);
        }
    }

    @Override
    public void setStartupProcessManager(IPSStartupProcessManager mgr) {
        mgr.addStartupProcess(this);
    }

    static String getPropName() {
        return PSImportCustCertificates.class.getSimpleName();
    }
}
